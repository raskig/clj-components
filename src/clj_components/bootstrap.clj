(ns clj-components.bootstrap
  (:require [clj-components.system :as system]
            [clojure.tools.logging :as log]
            [clj-components.components.logging]))

(defn shutdown! [system]
  (when system
    (system/shutdown! system)))

(defn init!
  "Load and instantiate system components."
  [old-system bootstrap-args component-constructors]
  (assert (not old-system))

  (clj-components.components.logging/set-level! 'avout :debug)

  (let [system (system/make-system component-constructors)]
    (system/init-config! system)
    (system/init-components! system bootstrap-args)

    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (println "Caught shutdown, shutting down...")
                (shutdown! system))))

    system))
