(ns clj-components.components.riemann
  (:use [clj-components.component])
  (:require [riemann.client :as r]
            [clojure.tools.logging :as log]))

(defrecord RiemannComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :riemann)

  (init [this settings]
    (assoc this :client (when (not-empty (:riemann-host settings))
                          (r/tcp-client :host (:riemann-host settings))))))
