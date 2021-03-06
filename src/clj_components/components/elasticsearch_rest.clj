(ns clj-components.components.elasticsearch-rest
  (:use [clj-components.protocols.component])
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojure.tools.logging :as log]))

(defrecord ElasticSearchRestfulComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :es-restful)

  (init [this settings]
    (let [es-url (:es-url settings)]
      (if es-url
        (log/info (format "Connecting to %s" es-url))
        (esr/connect! (format "http://%s" (clojure.string/replace es-url #":93" ":92")))))
    this))
