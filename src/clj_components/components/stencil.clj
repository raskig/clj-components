(ns clj-components.components.stencil
  (:use [clj-components.protocols.component])
  (:require [clojure.tools.logging :as log]
            [stencil.loader]))

(defrecord StencilComponent []
  SystemComponent
  (registry-key [this] :stencil)

  (init [this settings bootstrap-args]
    (let [ttl (or (:template-cache-ttl @settings)
                  (:template-cache-ttl bootstrap-args)
                  300000)]
      (log/info "Init set up template cache for stencil, ttl:" ttl "ms")
      (stencil.loader/set-cache (clojure.core.cache/ttl-cache-factory {} :ttl ttl)))
    this)

  SpecifySettingsPath
  (settings-path [this] [:applications :fe]))
