(ns clj-components.component-management
  (:require [clojure.tools.logging :as log]
            [clj-components.protocols.component :as component]
            [clj-components.protocols.config-supplier :as config-supplier]))

(defn components-atom [component-constructors]
  (atom (into {}
              (for [c-c component-constructors :let [c (c-c)]]
                [(component/registry-key c) c]))))

(defn on-components!
  "Execute function f against an atom of components."
  [f components]
  (swap! components #(zipmap (keys %) (map f (vals %)))))

(defn settings-path [c]
  (if (satisfies? component/SpecifySettingsPath c)
    (component/settings-path c)
    [:components (component/registry-key c)]))

(defn init-component!
  "Initialise component for the given system"
  [{:keys [config-supplier bootstrap-args]} c]
  (log/info (format "Loading %s" (component/registry-key c)))

  (let [path (settings-path c)
        component-settings (config-supplier/fetch config-supplier path)]

    (component/init c (merge @component-settings bootstrap-args))))

(defn shutdown-component! [component]
  (when (satisfies? component/ShutdownComponent component)
    (log/info (format "Shutting down %s" (component/registry-key component)))
    (try
      (component/shutdown component)
      (catch Throwable t
        (log/warn t "Could not shutdown component" (component/registry-key component))
        component)))
  component)

(defn bounce-component! [system component]
  (if (satisfies? component/BounceOnConfigChange component)
    (do
      (shutdown-component! component)
      (init-component! system component))
    component))
