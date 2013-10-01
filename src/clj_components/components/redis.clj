(ns clj-components.components.redis
  (:use [clj-components.component])
  (:require [taoensso.carmine :as car]
            [clojure.tools.logging :as log]
            [clj-components.system :as system]))

(defrecord RedisComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :redis)

  (init [this settings]
    (log/info (format "Connecting to %s" (-> settings :components :redis :url)))
    (let [spec-server (car/make-conn-spec :uri (-> settings :components :redis :url))]
      (assoc this
        :spec-server spec-server
        :pool (car/make-conn-pool)))))

(defmacro wcar [& body]
  `(let [spec-server# (-> system/components :redis :spec-server)
         pool# (-> system/components :redis :pool)]
     (car/with-conn pool# spec-server# ~@body)))
