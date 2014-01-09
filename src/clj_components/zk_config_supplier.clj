(ns clj-components.zk-config-supplier
  (:use [clj-components.protocols.config-supplier])
  (:require [clojure.tools.logging :as log]
            [clj-components.config :as config]
            [zookeeper :as zk]
            [zookeeper.data :as zk-data]))

;;-----------------------------------------------------------
;; ZK config supplier
;;
;; Supplies config to the system.
;;
;; Also deals with the situation where a SESSION_EXPIRED event
;; is raised from ZK. In this case we want to bounce the client,
;; let the system know.
;;-----------------------------------------------------------

(def ^:dynamic *cfg-node* "/stoic")

(defn serialize-form
  "Serializes a Clojure form to a byte-array."
  ([form]
     (zk-data/to-bytes (pr-str form))))

(defn deserialize-form
  "Deserializes a byte-array to a Clojure form."
  ([form]
     (when form (read-string (zk-data/to-string form)))))

(defn- atom-path [path]
  (clojure.string/join "/" (map name (concat [*cfg-node* (config/zk-root)] path))))

(defn- connection-watcher [config-supplier reconnect-fn e]
  (log/debug "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting.")

    (close! config-supplier)
    (init! config-supplier reconnect-fn)

    (log/info "Zookeeper reconnecting complete.")

    (reconnect-fn)))

(defn- add-persistent-watcher!
  "ZK Watches are fired one time only (except for ZK connect/disconnect events)."
  [client path callback]
  (log/info "Registering ZK watcher for path:" path)

  (let [watch-count (atom 0)
        watcher (fn watcher-fn [event]
                  (when (= :NodeDataChanged (:event-type event))
                    (swap! watch-count inc)
                    (log/info (format "Watcher fired for path %s in session %s (%s times)."
                                      path (.getSessionId client) @watch-count))
                    (callback)

                    ;; Re-add the watcher
                    (zk/exists client path :watcher watcher-fn)))]

    (zk/exists client path :watcher watcher)))

(defrecord ZkConfigSupplier [client]
  ConfigSupplier

  (init! [this reconnect-fn]
    (log/info (format "Connecting to ZK %s with root %s" (config/zk-ips) (config/zk-root)))

    (reset! client (zk/connect (config/zk-ips)
                               :timeout-msec 10000
                               :watcher (partial connection-watcher this reconnect-fn))))

  (close! [this]
    (zk/close @client))

  (fetch [this path watcher-fn]
    (log/info (format "Fetching %s located at %s" path (atom-path path)))

    (let [path (atom-path path)]

      (when-not (zk/exists @client path)
        (log/info (format "Creating %s for first time" path))
        (zk/create-all @client path :persistent? true))

      (let [settings-atom (atom (deserialize-form (:data (zk/data @client path))))]

        ;; Add a watcher to keep the atom updated and trigger custom watcher
        (add-persistent-watcher! @client path
                                 (fn []
                                   (reset! settings-atom (deserialize-form (:data (zk/data @client path))))
                                   (when watcher-fn (watcher-fn))))

        settings-atom))))

(defn supplier []
  (ZkConfigSupplier. (atom nil)))


;;--------------------
;; Helpful Stuff:
;;--------------------

(defn update! [client path form]
  (let [path (atom-path path)]
    (when-not (zk/exists client path)
      (log/info (format "Creating %s for first time" path))
      (zk/create-all client path :persistent? true))

    (let [v (:version (zk/exists client path))]
      (zk/set-data client path (serialize-form form) v))))
