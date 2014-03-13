(ns clj-components.components.redis
  (:use [clj-components.protocols.component])
  (:require [taoensso.carmine :as car]
            [clojure.tools.logging :as log]))

(defmacro wcar-db [system db & body]
  `(let [spec-server# (-> ~system :components deref :redis :spec-server (assoc :db ~db))
         pool# (-> ~system :components deref :redis :pool)]
     (car/with-conn pool# spec-server# ~@body)))

(defmacro wcar [system & body]
  `(wcar-db ~system 0 ~@body))

(defrecord RedisComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :redis)

  (init [this settings _]
    (let [settings @settings]
      (log/info (format "Connecting to %s" (-> settings :url)))
      (let [spec-server (car/make-conn-spec :uri (-> settings :url))]
        (assoc this
          :spec-server spec-server
          :pool (car/make-conn-pool))))))
