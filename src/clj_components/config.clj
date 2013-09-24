(ns clj-components.config
  (:use [avout.core])
  (:require [environ.core :as environ]))

(defn zk-ips
  "Zookeeper IPs."
  [] (or (environ/env :clj-fe-zk-conn-str) "10.251.76.40:2181,10.251.76.52:2181"))

(defn zk-root
  "Zookeeper Root."
  [] (keyword (or (environ/env :clj-fe-zk-root) :clj-fe)))

(defn fetch-settings
  "Retrieve settings from Zookeeper."
  [watcher]
  (let [client (connect (zk-ips))
        settings (zk-ref client (str "/" (name (zk-root))))]
    (add-watch settings nil (fn [& args] (watcher settings)))
    settings))
