(ns project-repl.components
  (:require [clj-components.components.elasticsearch]
            [clj-components.bootstrap]
            [clj-components.config]))

(defn init []
  (clj-components.bootstrap/init! nil {} [clj-components.components.elasticsearch/->ElasticSearchComponent]))

;; (avout.core/dosync!! c (avout.core/alter!! (:settings @(:config s)) assoc :foo :bar))
;;  (def c (:client @(:config s)))

(defn watcher-totals [s]
  (clj-components.config/zk-watcher-totals (:client @(:config s))))
