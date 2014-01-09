(ns project-repl.components
  (:require [clj-components.full-registry]
            [clj-components.bootstrap]))

(defn init []
  (clj-components.bootstrap/init! nil {} clj-components.full-registry/constructors))

;; (avout.core/dosync!! c (avout.core/alter!! (:settings @(:config s)) assoc :foo :bar))
;;  (def c (:client @(:config s)))
