(ns clj-components.full-registry
  (:require [clj-components.components.redis]
            [clj-components.components.riemann]
            [clj-components.components.web-server]
            [clj-components.components.nrepl]
            [clj-components.components.elasticsearch]))

;; An doesn't have to use the full registry, it can pick and choose and use it's own

(def constructors
  [clj-components.components.elasticsearch/->ElasticSearchComponent
   ;; clj-components.components.redis/->RedisComponent
   ;; clj-components.components.riemann/->RiemannComponent
   ;; clj-components.components.web-server/->WebServerComponent
   ;; clj-components.components.nrepl/->NReplComponent
])
