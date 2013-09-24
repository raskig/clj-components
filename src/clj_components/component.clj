(ns clj-components.component)

(defprotocol SystemComponent
  [registry-key [this]]

  (init [this settings]))

(defprotocol ShutdownComponent
  (shutdown [this]))

(defprotocol BounceOnConfigChange)
