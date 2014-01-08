(ns clj-components.avout-config
  (:use [avout.core :as avout]
        [clj-components.protocols.config-supplier])
  (:require [clojure.tools.logging :as log]
            [clj-components.config :as config]
            [clj-components.protocols.system :as system]
            [zookeeper :as zk]))

;;-----------------------------------------------------------
;; Avout config supplier
;;
;; Supplies config to the system.
;;
;; Also deals with the situation where a SESSION_EXPIRED event
;; is raised from ZK. In this case we want to bounce the client,
;; bounce components, and re-watch atoms for changes.
;;-----------------------------------------------------------

(defn- connection-watcher [config-supplier reconnect-fn e]
  (log/warn "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")

    (close! config-supplier)
    (init! config-supplier reconnect-fn)

    (reconnect-fn)

    (log/info "Finished bouncing relevant components.")))

(defn- atom-watcher [system client path bounce-count e]
  (swap! bounce-count inc)

  (log/info (format "Configuration change detected for atom %s in session %s (%s times)."
                    path (.getSessionId client) bounce-count))

  (system/bounce-component! system path))

(defrecord AvoutConfigSupplier [client]
  ConfigSupplier

  (init! [this reconnect-fn]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips) (config/zk-root)))

    (reset! client
            (avout/connect (config/zk-ips)
                           :timeout-msec 10000
                           :watcher (partial connection-watcher reconnect-fn))))

  (close! [this]
    (zk/close))

  (fetch [this system path]
    (let [settings-atom (avout/zk-atom @client
                                       (str "/" (clojure.string/join "/" (map name (cons (config/zk-root) path)))))
          bounce-count (atom 0)]

      ;; Add a watch through Clojures STM
      (add-watch settings-atom path (partial atom-watcher system client path (atom 0))))))

(defn supplier []
  (AvoutConfigSupplier. (atom nil)))
