(ns clj-components.config
  (:require [environ.core :as environ]
            [zookeeper :as zk]))

(defn zk-ips
  "Zookeeper IPs."
  [] (or (environ/env :clj-fe-zk-conn-str) "10.251.76.40:2181,10.251.76.52:2181"))

(defn ^:dynamic zk-root
  "Zookeeper Root."
  [] (keyword (or (environ/env :clj-fe-zk-root) :clj-fe)))

(defn- field-on-object [o f]
  (let [f (.getDeclaredField (.getClass o) f)]
    (.setAccessible f true)
    (.get f o)))

(defn zk-client-watchers [client]
  (let [watch-manager (field-on-object client "watchManager")]
    (into {}
          (for [watch-type [:exist :data :child]]
            [watch-type (field-on-object watch-manager (str (name watch-type) "Watches"))]))))

(defn zk-watcher-totals [client]
  (let [watches (zk-client-watchers client)]
    (zipmap (keys watches)
            (map #(reduce + (map count (vals %))) (vals watches)))))

;; ZK Notes -------------------------------------------------------------

;; ZK Connection Watcher Events:

;; Connect:
;; the connection watcher is called with {:event-type :None, :keeper-state :SyncConnected, :path nil}

;; Disconnect:
;; the connection watcher is called with {:event-type :None, :keeper-state :Disconnected, :path nil}
;; the connection watcher is called with {:event-type :None, :keeper-state :Expired, :path nil}

;; Client is usually accessible via:
;; (def client (-> fe.system.system/system :config deref :client))
