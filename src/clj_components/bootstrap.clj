(ns clj-components.bootstrap
  (:require [clj-components.system :as system]))

(defn init!
  "Load and instantiate system components."
  [old-system bootstrap-args component-constructors]
  (assert (not old-system))

  (let [system (system/make-system component-constructors)]
    (system/init-config! system)
    (system/init-components! system bootstrap-args)
    system))

(defn shutdown! [system]
  (when system
    (shutdown! system)))
