(ns clj-components.components.riemann
  (:use [clj-components.component])
  (:require [riemann.client :as r]
            [clojure.tools.logging :as log]))

(defrecord RiemannComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :riemann)

  (init [this settings]
    (let [riemann-host (-> settings :components :riemann :riemann-host)]
      (assoc this :client (when (not-empty riemann.host)
                            (r/tcp-client :host riemann.host)))))

  ShutdownComponent
  (shutdown [this]
    (when-let [client (:client this)]
      (r/close-client client))))
