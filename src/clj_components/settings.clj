(ns clj-components.settings)

(declare config)
(declare settings)

(defn configure! [config]
  (def config config)
  (def settings (:settings config))
  config)
