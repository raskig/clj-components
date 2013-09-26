(ns clj-components.components.elasticsearch
  (:use [clj-components.component])
  (:require [clojurewerkz.elastisch.native :as native]
            [clojure.tools.logging :as log]))

(defrecord ElasticSearchComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :es)

  (init [this settings]
    (let [es-url (:es-url settings)
          es-cluster (:es-cluster settings)]
      (if (and es-url es-cluster)
        (let [_ (log/info (format "Connecting to %s on cluster %s" es-url es-cluster))
              pairs (->> (clojure.string/split (:es-url settings) #",")
                         (map #(clojure.string/split % #":"))
                         (map #(vector(first %) (Integer/parseInt (last %)))))]

          (assoc this
            :es-url es-url
            :es-cluster es-cluster
            :es-client (native/connect! pairs {"cluster.name" (:es-cluster settings)})))
        this))))
