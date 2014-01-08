(ns clj-components.component-management
  (:require [clojure.tools.logging :as log]
            [clj-components.protocols.component :as component]
            [clj-components.protocols.system :as system]
            [clj-components.protocols.config-supplier :as config-supplier]))

(defn components-atom [component-constructors]
  (atom (into {}
              (for [c-c component-constructors :let [c (c-c)]]
                [(component/registry-key c) c]))))

(defn- settings-path [c]
  (if (satisfies? component/SpecifySettingsPath c)
    (component/settings-path c)
    [:components (component/registry-key c)]))

(defn init-component!

  "Initialise component for the given system.

   We assoc in the settings for that component in as :settings.

   When we fetch the settings, we add a watcher to bounce the
   component if the settings change."

  [{:keys [config-supplier bootstrap-args] :as system} c]
  (log/info (format "Loading %s" (component/registry-key c)))

  (let [settings (or (:settings c)
                     (config-supplier/fetch config-supplier (settings-path c)
                                            (fn [] (system/bounce-component! system (component/registry-key c))) ))]

    (assoc (component/init c settings bootstrap-args) :settings settings)))

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

(defn handle-settings-reconnect!
  "Similar to bounce-component! except we follow a different protocol
   and remove existing settings, forcing a retrieval of the latest."
  [system component]
  (if (satisfies? component/BounceOnConfigChange component)
    (do
      (shutdown-component! component)
      (init-component! system (dissoc component :settings)))
    component))
