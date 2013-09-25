(ns clj-components.components.elasticsearch
  (:use [clj-components.component])
  (:require [clojurewerkz.elastisch.native :as native]
            [clojure.tools.logging :as log]))

(defrecord ElasticSearchComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :es)

  (init [this settings]
    (when (and (:es-url settings) (:es-cluster settings))
      (log/info (format "Connecting to %s on cluster %s" (:es-url settings) (:es-cluster settings)))
      (let [pairs (->> (clojure.string/split (:es-url settings) #",")
                       (map #(clojure.string/split % #":"))
                       (map #(vector(first %) (Integer/parseInt (last %)))))]

        (native/connect! pairs {"cluster.name" (:es-cluster settings)})))
    this))
