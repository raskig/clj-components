(ns clj-components.zk-util
  (:require [zookeeper :as zk]
            [clj-components.config :as config])
  (:import [org.apache.zookeeper ZooKeeper]))

(defn expire-zk-conn [conn]
  (let [clone (ZooKeeper. (config/zk-ips) 60000, nil, (zk/session-id conn), (zk/session-password conn))]
    (println  "Closing" (zk/session-id conn), (zk/session-password conn))
    (zk/close clone)
    (println "Connection state of original connection is now:" (zk/state conn))))
