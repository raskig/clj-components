(ns clj-components.bootstrap
  (:require [clojure.tools.logging :as log]
            [clj-components.component :as component]
            [clj-components.manifest]
            [clj-components.system]
            [clj-components.settings]))

(defn- init-component! [settings c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (component/init c settings))

(defn- bounce-on-config!
  "When some config changes, this is the function to run."
  [settings]
  (log/info "Configuration change detected, bouncing relevant components.")
  (clj-components.settings/configure! settings)
  (clj-components.system/configure!
   (into clj-components.system/components
         (for [c (vals clj-components.system/components)
               :when (satisfies? component/BounceOnConfigChange c)]
           [(component/registry-key c) (init-component! @settings c)]))))

(defn- zk-connection-watcher [e]
  (println "TODO - Bounce Components" e))

(defn init!
  "Load and instantiate system components."
  [bootstrap-args component-constructors]
  (log/info "Manifest:" (clj-components.manifest/fetch))

  (let [settings (clj-components.config/fetch-settings #'bounce-on-config! #'zk-connection-watcher)
        init-settings (merge @settings bootstrap-args)]
    (clj-components.settings/configure! settings)
    (clj-components.system/configure!
     (into {}
           (for [c-c component-constructors :let [c (c-c)]]
             [(component/registry-key c) (init-component! init-settings c)]))))

  (log/info "Components loaded."))

(defn shutdown! []
  (when (bound? #'clj-components.system/components)
    (doseq [c (vals clj-components.system/components)
            :when (satisfies? component/ShutdownComponent c)]
      (log/info (format "Shutting down %s" (component/registry-key c)))
      (component/shutdown c))))
