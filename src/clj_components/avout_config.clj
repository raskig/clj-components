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
;; let the system know, and re-watch atoms for changes.
;;-----------------------------------------------------------

(defn- connection-watcher [config-supplier reconnect-fn e]
  (log/warn "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting.")

    (close! config-supplier)
    (init! config-supplier reconnect-fn)

    (log/info "Zookeeper reconnecting complete.")

    (reconnect-fn)

    (log/info "Re-registering watchers.")

    ;; Re-apply watchers
    (doseq [[path watcher-fn] @(:watchers config-supplier)]
      (register-watcher config-supplier path watcher-fn))

    (log/info "Re-registering watchers complete.")))

(defn- atom-watcher [system client path watch-count f e]
  (swap! watch-count inc)

  (log/info (format "Configuration change detected for atom %s in session %s (%s times)."
                    path (.getSessionId client) watch-count))

  (f))

(defrecord AvoutConfigSupplier [client watchers]
  ConfigSupplier

  (init! [this reconnect-fn]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips) (config/zk-root)))

    (reset! client
            (avout/connect (config/zk-ips)
                           :timeout-msec 10000
                           :watcher (partial connection-watcher reconnect-fn))))

  (close! [this]
    (zk/close))

  (fetch [this path]
    (avout/zk-atom @client (str "/" (clojure.string/join "/" (map name (cons (config/zk-root) path))))))

  (register-watcher [this path watcher-fn]
    (let [settings-atom (fetch this path)]
      ;; Add a watch through Clojures STM
      (add-watch settings-atom path (partial atom-watcher client path watcher-fn (atom 0)))
      ;; Register watcher with this config supplier
      (swap! watchers path watcher-fn)

      (log/info "Registered watcher for path" path))))

(defn supplier []
  (AvoutConfigSupplier. (atom nil) (atom {})))

;; Migration
;;  Do a simple story, like migrate ES from shared ref to singular atom
;;  Be good to leave current migration stuff in place, like it's backwards compatible (somehow)

(defn migrate-component! [system k]
  (let [config-supplier (:config-supplier system)
        client (-> config-supplier :client deref)
        original-settings (fetch config-supplier [])
        new-settings (fetch config-supplier k)]
    (avout/reset!! new-settings @original-settings)))
