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

(defn connect
  "Returns a ZooKeeper client, and initializes the STM if it doesn't already exist."
  ([& args]
     ))

(defn- connection-watcher [config-supplier reconnect-fn e]
  (log/warn "Zookeeper connection event:" e)
  (when (= :Expired (:keeper-state e))

    (log/warn "Zookeeper session expired, reconnecting.")

    (close! config-supplier)
    (init! config-supplier reconnect-fn)

    (log/info "Zookeeper reconnecting complete.")

    (reconnect-fn)))

(defn- atom-path [path]
  (clojure.string/join "/" (map name (concat [*cfg-node* (config/zk-root)] path))))

(defn- add-persistent-watch!
  "ZK Watches are fired one time only (except for ZK connect/disconnect events)."
  [client path callback]
  (log/info "Registering ZK watcher for " path)

  (let [watch-count (atom 0)
        watcher (fn watcher-fn [event]
                  (when (= :NodeDataChanged (:event-type event))
                    (swap! watch-count inc)
                    (log/info (format "Configuration change detected for atom %s in session %s (%s times)."
                                      path (.getSessionId client) @watch-count))
                    (callback)

                    ;; Re-add the watcher
                    (zk/exists client path :watcher watcher-fn)))]

    (zk/exists client (atom-path path) :watcher watcher)))

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

    (when-not (zk/exists @client (atom-path path))
      (log/info (format "Creating %s for first time" (atom-path path)))
      (zk/create-all @client (atom-path path) :persistent? true))

    (let [settings-atom (atom (deserialize-form (:data (zk/data @client (atom-path path)))))]
      (when watcher-fn
        (add-persistent-watch! @client path watcher-fn))

      settings-atom)))

(defn supplier []
  (ZkConfigSupplier. (atom nil)))
