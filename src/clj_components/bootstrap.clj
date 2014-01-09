(ns clj-components.bootstrap
  (:require [clj-components.system]
            [clj-components.zk-config-supplier]
            [clj-components.protocols.system :as system]
            [clj-components.protocols.config-supplier :as config-supplier]
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
                (clj-components.zk-config-supplier/supplier)
                bootstrap-args)]

    (system/init! system)

    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (println "Caught shutdown, shutting down...")
                (shutdown! system))))

    system))
