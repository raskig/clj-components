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

(defn- atom-watcher
  "We plug this watcher into Clojures std STM.
  The STM model expects to invoke a watcher-fn with 4 args:
  The key, the reference, its old-state, its new-state."
  [session-id watch-count f k _ _ _]
  (swap! watch-count inc)

  (log/info (format "Configuration change detected for atom %s in session %s (%s times)." k session-id @watch-count))

  (f))

(defrecord AvoutConfigSupplier [client watchers]
  ConfigSupplier

  (init! [this reconnect-fn]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips) (config/zk-root)))

    (reset! client
            (avout/connect (config/zk-ips)
                           :timeout-msec 10000
                           :watcher (partial connection-watcher this reconnect-fn))))

  (close! [this]
    (zk/close @client))

  (fetch [this path]
    (avout/zk-atom @client (str "/" (clojure.string/join "/" (map name (cons (config/zk-root) path))))))

  (register-watcher [this path watcher-fn]
    (let [settings-atom (fetch this path)]
      ;; Add a watch through Clojures STM
      (add-watch settings-atom path (partial atom-watcher (.getSessionId @client) (atom 0) watcher-fn))
      ;; Register watcher with this config supplier
      (swap! watchers assoc path watcher-fn)

      (log/info "Registered watcher for path" path))))

(defn supplier []
  (AvoutConfigSupplier. (atom nil) (atom {})))

;; Migration
;;  Do a simple story, like migrate ES from shared ref to singular atom
;;  Be good to leave current migration stuff in place, like it's backwards compatible (somehow)

(defn fetch-old [{:keys [config-supplier]}]
  (avout/zk-ref @(:client config-supplier) (str "/" (name (config/zk-root)))))

(defn migrate-component! [{:keys [config-supplier] :as system} k]
  (avout/reset!! (fetch config-supplier [:components k])
                 (-> clj-components.bootstrap/s fetch-old deref :components k)))
