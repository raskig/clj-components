(ns clj-components.components.riemann
  (:use [clj-components.protocols.component])
  (:require [riemann.client :as r]
            [clojure.tools.logging :as log]
            [clj-components.utils.bounded-executor :as executor]))

(defrecord RiemannComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :riemann)

  (init [this settings _]
    (let [riemann-host (:riemann-host @settings)]
      (assoc this :client (when (not-empty riemann-host)
                            (r/tcp-client :host riemann-host)))))

  ShutdownComponent
  (shutdown [this]
    (when-let [client (:client this)]
      (r/close-client client))))

(defn- local-host-name* []
  (try
    (.getHostName (java.net.InetAddress/getLocalHost))
    (catch Exception e "unknown")))

(def local-host-name (memoize local-host-name*))

(def default-riemann-event
  {:state "normal"
   :metric 0
   :ttl 300
   :tags []
   :host (local-host-name)})

(defn- client [system]
  (-> system :components deref :riemann :client))

(defn send-event
  "Sends an event to Riemann"
  [system event]
  (try
    (executor/run-bounded (fn [] (r/send-event (client system) event)))
    (catch Exception e
      (log/error e (format "Can't send event %s to riemann" (keys event))))))

;; Test your riemann:
(comment
  (let [ex (Exception.)]
    (r/send-event
     {:state "critical"
      :service "clj_fe"
      :description (.getName (.getClass ex))
      :tags ["exception" (.getSimpleName (.getClass ex))]})))
