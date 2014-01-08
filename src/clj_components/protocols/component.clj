(ns clj-components.protocols.component)

(defprotocol SystemComponent
  [registry-key [this]]

  (init [this settings bootstrap-args]))

(defprotocol SpecifySettingsPath
  (settings-path [this]))

(defprotocol ShutdownComponent
  (shutdown [this]))

(defprotocol BounceOnConfigChange)
