(ns clj-components.bootstrap
  (:require [clj-components.system]
            [clj-components.avout-config]
            [clj-components.protocols.system :as system]
            [clojure.tools.logging :as log]))

(defn shutdown! [system]
  (when system
    (system/shutdown! system)))

(defn init!
  "Load and instantiate system components."
  [old-system bootstrap-args component-constructors]
  (assert (not old-system))

  (let [system (clj-components.system/make-system
                component-constructors
                (clj-components.avout-config/supplier)
                bootstrap-args)]

    (system/init! system)

    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (println "Caught shutdown, shutting down...")
                (shutdown! system))))

    system))
