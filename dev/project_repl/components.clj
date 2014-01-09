(ns project-repl.components
  (:require [clj-components.components.elasticsearch]
            [clj-components.bootstrap]))

(defn init []
  (clj-components.bootstrap/init! nil {} [clj-components.components.elasticsearch/->ElasticSearchComponent]))

;; (avout.core/dosync!! c (avout.core/alter!! (:settings @(:config s)) assoc :foo :bar))
;;  (def c (:client @(:config s)))
