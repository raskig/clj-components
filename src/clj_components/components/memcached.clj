(ns clj-components.components.memcached
  (:use [clj-components.protocols.component])
  (:require [clojure.tools.logging :as log])
  (:import [net.spy.memcached ConnectionObserver MemcachedClient AddrUtil
            FailureMode ConnectionFactoryBuilder ConnectionFactoryBuilder$Protocol]))

(defn- custom-cf []
  (.build
   (doto (ConnectionFactoryBuilder.)
     (.setFailureMode FailureMode/Retry)
     (.setProtocol ConnectionFactoryBuilder$Protocol/BINARY))))

(defrecord MemcachedComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :memcached)

  (init [this settings _]
    (if (not-empty (:url @settings))
      (let [server-list (AddrUtil/getAddresses (:url @settings))
            client (MemcachedClient. (custom-cf) server-list)
            is-connected? (atom false)]
        (assert (.addObserver client
                              (proxy [ConnectionObserver] []
                                (connectionEstablished [_ _]
                                  (log/info "Connection established for memcached.")
                                  (reset! is-connected? true))

                                (connectionLost [_]
                                  (log/warn "Connection lost for memcached.")
                                  (reset! is-connected? false)))))
        (assoc this :client client :is-connected? is-connected?))
      (dissoc this :client :is-connected?)))

  ShutdownComponent
  (shutdown [this]
    (when (:client this)
      (.shutdown (:client this)))))

(defn client [system]
  (let [client (-> system :components deref :memcached :client)
        is-connected? (-> system :components deref :memcached :is-connected?)]
    (and is-connected? @is-connected? client)))
