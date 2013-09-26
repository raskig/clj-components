(ns clj-components.bootstrap
  (:require [clojure.tools.logging :as log]
            [clj-components.component :as component]
            [clj-components.manifest]
            [clj-components.system]
            [clj-components.settings]
            [clj-components.config]))

(declare init-config!)
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
           [(component/registry-key c) (init-component! @clj-components.settings/settings c)])))
  (log/info "Finished bouncing relevant components."))

(defn- zk-connection-watcher [e]
  (when (= :Expired (:keeper-state e))
    (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")
    (init-config!)
    (bounce-on-config!)))

(defn- init-config! []
  (clj-components.settings/configure!
   (clj-components.config/fetch! zk-connection-watcher bounce-on-config!)))

(defn init!
  "Load and instantiate system components."
  [bootstrap-args component-constructors]
  (assert (not (and (bound? #'clj-components.system/components) clj-components.system/components)))
  (log/info "Manifest:" (clj-components.manifest/fetch))

  (init-config!)

  (let [init-settings (merge @(:settings clj-components.settings/config) bootstrap-args)]
    (clj-components.system/configure!
     (into {}
           (for [c-c component-constructors :let [c (c-c)]]
             [(component/registry-key c) (init-component! init-settings c)]))))

  (log/info "Components loaded."))

(defn shutdown! []
  (log/info "Shutting down.")
  (when (bound? #'clj-components.system/components)
    (doseq [c (vals clj-components.system/components)
            :when (satisfies? component/ShutdownComponent c)]
      (log/info (format "Shutting down %s" (component/registry-key c)))
      (component/shutdown c))
    (clj-components.system/configure! nil))
  (when (bound? #'clj-components.settings/config)
    (clj-components.config/disconnect! clj-components.settings/config))
  (log/info "Shut down complete."))
