(ns clj-components.bootstrap
  (:require [clojure.tools.logging :as log]
            [clj-components.component :as component]
            [clj-components.manifest]
            [clj-components.system]
            [clj-components.settings]
            [clj-components.config]))

(declare init!)
(declare shutdown!)

(defn- init-component! [settings c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (component/init c settings))

(defn- bounce-on-config!
  "When some config changes, this is the function to run."
  []
  (log/info "Configuration change detected, bouncing relevant components.")
  (clj-components.system/configure!
   (into clj-components.system/components
         (for [c (vals clj-components.system/components)
               :when (satisfies? component/BounceOnConfigChange c)]
           [(component/registry-key c) (init-component! @clj-components.settings/settings c)]))))

(defn- zk-connection-watcher [e]
  (when (= :Expired (:keeper-state e))
    (println "Zookeeper expired, restarting.")
    (shutdown!)
    (init!)))

(defn init!
  "Load and instantiate system components."
  [bootstrap-args component-constructors]
  (println "Intialising.")
  (log/info "Manifest:" (clj-components.manifest/fetch))

  (let [config (clj-components.config/fetch! zk-connection-watcher bounce-on-config!)
        init-settings (merge @(:settings config) bootstrap-args)]
    (clj-components.settings/configure! config)
    (clj-components.system/configure!
     (into {}
           (for [c-c component-constructors :let [c (c-c)]]
             [(component/registry-key c) (init-component! init-settings c)]))))

  (log/info "Components loaded."))

(defn shutdown! []
  (println "Shutting down.")
  (when (bound? #'clj-components.system/components)
    (doseq [c (vals clj-components.system/components)
            :when (satisfies? component/ShutdownComponent c)]
      (log/info (format "Shutting down %s" (component/registry-key c)))
      (component/shutdown c)))
  (when (bound? #'clj-components.settings/config)
    (clj-components.config/disconnect! clj-components.settings/config))
  (println "Shut down complete."))
