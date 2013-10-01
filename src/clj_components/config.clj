(ns clj-components.config
  (:use [avout.core]
        [clj-components.component])
  (:require [environ.core :as environ]
            [zookeeper :as zk]))

(defn zk-ips
  "Zookeeper IPs."
  [] (or (environ/env :clj-fe-zk-conn-str) "10.251.76.40:2181,10.251.76.52:2181"))

(defn ^:dynamic zk-root
  "Zookeeper Root."
  [] (keyword (or (environ/env :clj-fe-zk-root) :clj-fe)))

(defrecord ConfigComponent [client settings])

(defn fetch! [zk-connection-watcher ref-watcher]
  (let [client (connect (zk-ips) :timeout-msec 10000 :watcher zk-connection-watcher)
        settings (zk-ref client (str "/" (name (zk-root))))]
    (add-watch settings nil (fn [& args] (ref-watcher)))
    (ConfigComponent. client settings)))

(defn disconnect! [config]
  (zk/close (:client config)))

;; ZK Notes -------------------------------------------------------------

;; ZK Connection Watcher Events:

;; Connect:
;; the connection watcher is called with {:event-type :None, :keeper-state :SyncConnected, :path nil}

;; Disconnect:
;; the connection watcher is called with {:event-type :None, :keeper-state :Disconnected, :path nil}
;; the connection watcher is called with {:event-type :None, :keeper-state :Expired, :path nil}
