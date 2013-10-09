(ns clj-components.components.memcached
  (:use [clj-components.component])
  (:require [clojurewerkz.spyglass.client :as c]))

(defrecord MemcachedComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :memcached)

  (init [this settings]
    (let [settings (-> settings :components :memcached)]
      (if (not-empty (:url settings))
        (assoc this :connection (c/bin-connection (:url settings)))
        this)))

  ShutdownComponent
  (shutdown [this]
    (when (:connection this)
      (c/shutdown (:connection this)))))

(defn conn [system] (-> system :components deref :memcached :connection))
