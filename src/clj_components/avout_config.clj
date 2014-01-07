(ns clj-components.avout-config
  (:use [avout.core :as avout]
        [clj-components.protocols.config-supplier])
  (:require [clojure.tools.logging :as log]
            [clj-components.config :as config]
            [clj-components.protocols.system :as system]
            [zookeeper :as zk]))

(defn- connection-watcher [system e]
  (log/warn "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")

    (close! (:config-supplier system))
    (init! (:config-supplier system) system)

    (system/bounce-components! system)

    (log/info "Finished bouncing relevant components.")))

(defn- atom-watcher [system client path bounce-count]
  (swap! bounce-count inc)

  (log/info (format "Configuration change detected for atom %s in session %s (%s times)."
                    path (.getSessionId client) bounce-count))

  (system/bounce-component! system path))

(defrecord AvoutConfigSupplier [client]
  ConfigSupplier

  (init! [this system]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips)))

    (reset! client
            (avout/connect (config/zk-ips)
                           :timeout-msec 10000
                           :watcher (partial connection-watcher system))))

  (close! [this]
    (zk/close))

  (fetch [this system path]
    (let [atom (avout/zk-atom client (str "/" (clojure.string/join "/" (map name path))))
          bounce-count (atom 0)]

      ;; Add a watch through Clojures STM
      (add-watch atom path (partial atom-watcher system client path (atom 0))))))

(defn supplier []
  (AvoutConfigSupplier. (atom nil)))
