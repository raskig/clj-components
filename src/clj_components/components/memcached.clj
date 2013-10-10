(ns clj-components.components.memcached
  (:use [clj-components.component])
  (:require [clojurewerkz.spyglass.client :as c]
            [clojure.tools.logging :as log])
  (:import [net.spy.memcached ConnectionObserver]))

(defrecord MemcachedComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :memcached)

  (init [this settings]
    (let [settings (-> settings :components :memcached)]
      (if (not-empty (:url settings))
        (let [conn (c/bin-connection (:url settings))
              is-connected? (atom false)]
          (assert (.addObserver conn
                                (proxy [ConnectionObserver] []
                                  (connectionEstablished [_ _]
                                    (log/info "Connection established for memcached.")
                                    (reset! is-connected? true))

                                  (connectionLost [_]
                                    (log/warn "Connection lost for memcached.")
                                    (reset! is-connected? false)))))
          (assoc this :connection conn :is-connected? is-connected?))
        (dissoc this :connection :is-connected?))))

  ShutdownComponent
  (shutdown [this]
    (when (:connection this)
      (c/shutdown (:connection this)))))

(defn conn [system]
  (let [conn (-> system :components deref :memcached :connection)
        is-connected? (-> system :components deref :memcached :is-connected?)]
    (and is-connected? @is-connected? conn)))
