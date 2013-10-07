(ns clj-components.bootstrap
  (:require [clojure.tools.logging :as log]
            [clj-components.component :as component]
            [clj-components.config]))

(declare init-config!)
(declare shutdown!)

(defn- init-component! [config bootstrap-args c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (component/init c (merge @(:settings @config) bootstrap-args)))

(defn- bounce-on-config!
  "When some config changes, this is the function to run."
  [components config]
  (log/info "Configuration change detected, bouncing relevant components.")
  (reset! components
          (into @components
                (for [c (vals @components)
                      :when (satisfies? component/BounceOnConfigChange c)]
                  (do
                    (when (satisfies? component/ShutdownComponent c)
                      (log/info (format "Shutting down %s" (component/registry-key c)))
                      (component/shutdown c))
                    [(component/registry-key c) (init-component! config {} c)]))))
  (log/info "Finished bouncing relevant components."))

(defn- zk-connection-watcher [components config e]
  (log/warn "Zookeeper connection event." e)
  (when (= :Expired (:keeper-state e))
    (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")
    (reset! config (init-config! components config))
    (bounce-on-config! components config)))

(defn- init-config! [components config]
  (clj-components.config/fetch! (partial zk-connection-watcher components config) (partial bounce-on-config! components config)))

(defn init!
  "Load and instantiate system components."
  [old-system bootstrap-args component-constructors]
  (assert (not old-system))

  (let [components (atom (into {}
                               (for [c-c component-constructors :let [c (c-c)]]
                                 [(component/registry-key c) c])))
        config (atom nil)]
    (reset! config (init-config! components config))
    (reset! components (zipmap (keys @components) (map (partial init-component! config bootstrap-args) (vals @components))))
    (log/info "Components loaded.")
    {:components components :config config}))

(defn shutdown! [system]
  (log/info "Shutting down.")
  (when system
    (doseq [c (vals @(:components system))
            :when (satisfies? component/ShutdownComponent c)]
      (log/info (format "Shutting down %s" (component/registry-key c)))
      (component/shutdown c))
    (when-let [config @(:config system)]
      (clj-components.config/disconnect! config)))
  (log/info "Shut down complete."))
