(ns clj-components.avout-config
  (:use [avout.core :as avout]
        [clj-components.protocols.config-supplier])
  (:require [clojure.tools.logging :as log]
            [clj-components.config :as config]
            [clj-components.protocols.system :as system]
            [zookeeper :as zk]))

;;-----------------------------------------------------------
;;
;; AVOUT IS NOT SAFE - SEE https://github.com/liebke/avout/issues/10
;;
;; Avout config supplier
;;
;; Supplies config to the system.
;;
;; Also deals with the situation where a SESSION_EXPIRED event
;; is raised from ZK. In this case we want to bounce the client,
;; let the system know, and re-watch atoms for changes.
;;-----------------------------------------------------------

(defn- connection-watcher [config-supplier reconnect-fn e]
  (log/warn "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting.")

    (close! config-supplier)
    (init! config-supplier reconnect-fn)

    (log/info "Zookeeper reconnecting complete.")

    (reconnect-fn)))

(defn- atom-watcher
  "We plug this watcher into Clojures std STM.
  The STM model expects to invoke a watcher-fn with 4 args:
  The key, the reference, its old-state, its new-state."
  [session-id watch-count f k _ _ _]
  (swap! watch-count inc)

  (log/info (format "Configuration change detected for atom %s in session %s (%s times)." k session-id @watch-count))

  (f))

(defn- atom-path [path]
  (str "/" (clojure.string/join "/" (map name (cons (config/zk-root) path)))))

(defrecord AvoutConfigSupplier [client]
  ConfigSupplier

  (init! [this reconnect-fn]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips) (config/zk-root)))

    (reset! client
            (avout/connect (config/zk-ips)
                           :timeout-msec 10000
                           :watcher (partial connection-watcher this reconnect-fn))))

  (close! [this]
    (zk/close @client))

  (fetch [this path watcher-fn]
    (log/info (format "Fetching %s" path))

    (let [settings-atom (avout/zk-atom @client (atom-path path))]

      (when watcher-fn

        ;; Add a watch through Clojures STM
        (add-watch settings-atom path (partial atom-watcher (.getSessionId @client) (atom 0) watcher-fn))

        (log/info "Registered watcher for path" path))

      settings-atom)))

(defn supplier []
  (AvoutConfigSupplier. (atom nil)))

(defn fetch-old [config-supplier]
  (avout/zk-ref @(:client config-supplier) (str "/" (name (config/zk-root)))))
