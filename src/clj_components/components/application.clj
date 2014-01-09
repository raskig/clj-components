(ns clj-components.components.application
  (:use [clj-components.protocols.component]))

(defrecord ApplicationComponent [k]
  BounceOnConfigChange
  SystemComponent
  (registry-key [_] [:applications k])

  SpecifySettingsPath
  (settings-path [_] [:applications k])

  (init [_ _ _])

  ShutdownComponent
  (shutdown [_]))
