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
        (let [pairs (->> (clojure.string/split (:es-url settings) #",")
                         (map #(clojure.string/split % #":"))
                         (map #(vector(first %) (Integer/parseInt (last %)))))]
          (log/info (format "Connecting to %s on cluster %s" es-url es-cluster))
          (assoc this
            :url es-url
            :cluster es-cluster
            :client (native/connect! pairs {"cluster.name" es-cluster
                                            "client.transport.ping_timeout" (:es-client-ping-timout settings "10s")})))
        this)))

  ShutdownComponent
  (shutdown [this]
    (.close native/*client*)))
