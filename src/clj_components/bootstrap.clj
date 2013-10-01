(ns clj-components.bootstrap
  (:require [clojure.tools.logging :as log]
            [clj-components.component :as component]
            [clj-components.manifest]
            [clj-components.settings]
            [clj-components.config]))

(declare init-config!)
(declare shutdown!)

(defn- init-component! [settings c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (component/init c settings))

(defn- bounce-on-config!
  "When some config changes, this is the function to run."
  [components]
  (log/info "Configuration change detected, bouncing relevant components.")
  (into components
        (for [c (vals components)
              :when (satisfies? component/BounceOnConfigChange c)]
          [(component/registry-key c) (init-component! @clj-components.settings/settings c)]))
  (log/info "Finished bouncing relevant components."))

(defn- zk-connection-watcher [e]
  (when (= :Expired (:keeper-state e))
    (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")
    (bounce-on-config!)))

(defn- init-config! []
  (clj-components.settings/configure!
   (clj-components.config/fetch! zk-connection-watcher)))

(defn init!
  "Load and instantiate system components."
  [old-system bootstrap-args component-constructors]
  (assert (not old-system))
  (log/info "Manifest:" (clj-components.manifest/fetch))

  (let [settings (:settings (init-config!))
        init-settings (merge @settings bootstrap-args)
        components (into {}
                         (for [c-c component-constructors :let [c (c-c)]]
                           [(component/registry-key c) (init-component! init-settings c)]))]
    (clj-components.config/add-watcher settings bounce-on-config! components)
    (log/info "Components loaded.")
    (assoc components :settings settings)))

(defn shutdown! [system]
  (log/info "Shutting down.")
  (when system
    (doseq [c (vals system)
            :when (satisfies? component/ShutdownComponent c)]
      (log/info (format "Shutting down %s" (component/registry-key c)))
      (component/shutdown c)))
  (when (bound? #'clj-components.settings/config)
    (clj-components.config/disconnect! clj-components.settings/config))
  (log/info "Shut down complete."))
