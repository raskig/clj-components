(ns clj-components.components.riemann
  (:use [clj-components.protocols.component])
  (:require [riemann.client :as r]
            [clojure.tools.logging :as log]))

(defrecord RiemannComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :riemann)

  (init [this settings]
    (let [riemann-host (:riemann-host settings)]
      (assoc this :client (when (not-empty riemann-host)
                            (r/udp-client :host riemann-host)))))

  ShutdownComponent
  (shutdown [this]
    (when-let [client (:client this)]
      (r/close-client client))))
