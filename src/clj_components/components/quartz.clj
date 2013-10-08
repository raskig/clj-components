(ns clj-components.components.quartz
  (:use [clj-components.component])
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [clojure.tools.logging :as log])
  (:import org.quartz.impl.matchers.GroupMatcher
           org.quartz.Scheduler
           org.quartz.TriggerKey
           org.quartz.utils.Key))

(def job-group :clj-jobs)
(def trigger-group :clj-triggers)

(defmacro def-job [k trigger-cron job-fn]
  (let [job-name (apply str (map clojure.string/capitalize (-> k name (clojure.string/split #"\-"))))]
    `{:job (defrecord ~(symbol job-name) []
             org.quartz.StatefulJob
             (execute [this ctx]
               (~job-fn)))
      :job-name (name ~k)
      :trigger-desc (clojure.string/join " " (map clojure.string/capitalize (-> ~k name (clojure.string/split #"\-"))))
      :trigger-cron ~trigger-cron}))

(defn sched [{:keys [job-name job trigger-desc trigger-cron]}]
  ;; schedule takes a job and a trigger
  (qs/schedule (j/build
                (j/of-type job)
                (j/with-identity job-name (name job-group)))

               (t/build
                (t/with-identity (.toString (java.util.UUID/randomUUID)) (name trigger-group))
                (t/with-description trigger-desc)
                (t/with-schedule (cron/schedule
                                  (cron/cron-schedule trigger-cron))))))

(defn run-job-now [job-name]
  (log/info (str "Manually firing " job-name))
  (qs/trigger (.getKey (qs/get-job (name job-name) (name job-group)))))

(defn- trigger-state [t]
  (.getTriggerState @clojurewerkz.quartzite.scheduler/*scheduler*
                    (TriggerKey. t (name trigger-group))))

(defn triggers []
  "Making our own map to bypass clunkiness
    See http://quartz-scheduler.org/api/2.0.0/org/quartz/impl/triggers/CronTriggerImpl.html
    BLOCKED: means running
    NORMAL: means not running :-)"
  (for [t (qs/get-triggers (qs/get-trigger-keys (GroupMatcher/groupEquals (name trigger-group))))]
    {:description (.getDescription t)
     :nextFire (.getFireTimeAfter t (java.util.Date.))
     :previousFire (.getPreviousFireTime t)
     :cron (.getCronExpression t)
     :job (.getName (.getJobKey t))
     :state (.toString (trigger-state (.getName t)))
     }))

(defrecord QuartzComponent []
  SystemComponent
  (registry-key [this] :quartz)

  (init [this {:keys [quartz-jobs]}]
    (log/info "Starting up Quartz... ")
    (qs/initialize)
    (qs/start)
    (qs/clear!)
    (when quartz-jobs
      (doseq [j quartz-jobs]
        (sched j)))
    (log/info "Done.")
    this)

  ShutdownComponent
  (shutdown [this]
    (qs/shutdown)))
