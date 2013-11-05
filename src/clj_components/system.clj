(ns clj-components.system
  (:require [clj-components.component :as component]
            [clojure.tools.logging :as log]
            [clj-components.config]))

(defprotocol ComponentSystemProtocol
  (init-config! [this])
  (init-components! [this bootstrap-args])
  (shutdown-components! [this])
  (shutdown! [this]))

(defn- on-components! [f components]
  (swap! components #(zipmap (keys %) (map f (vals %)))))

(defn- init-component! [settings bootstrap-args c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (let [component-settings (get-in settings
                                   (if (satisfies? component/SpecifySettingsPath c)
                                     (component/settings-path c)
                                     [:components (component/registry-key c)]))]
    (component/init c (merge component-settings bootstrap-args))))

(defn- shutdown-component! [component]
  (when (satisfies? component/ShutdownComponent component)
    (log/info (format "Shutting down %s" (component/registry-key component)))
    (component/shutdown component))
  component)

(defn- bounce-component! [settings component]
  (if (satisfies? component/BounceOnConfigChange component)
    (->> component shutdown-component! (init-component! settings {}))
    component))

(defn- bounce-components-on-config! [system new-settings session-id bounce-count]
  (log/info (format "Configuration change detected for session %s, bouncing relevant components (%s times)."
                    session-id bounce-count))

  (on-components! (partial bounce-component! new-settings) (:components system))

  (log/info "Finished bouncing relevant components."))

(defn- handle-reconnect! [system session-id e]
    (log/warn "Zookeeper connection event:" e)
    (when (= :Expired (:keeper-state e))
      (log/warn (format "Zookeeper session %s expired, reconnecting and bouncing relevant components." session-id))
      (init-config! system)
      (bounce-components-on-config! system @(:settings @(:config system)) (:session-id @(:config system)) 0)))

(defrecord ComponentSystem [config components]
  ComponentSystemProtocol
  (init-config! [this]
    (swap! config
           clj-components.config/fetch!
           (partial handle-reconnect! this)
           (partial bounce-components-on-config! this)))

  (init-components! [this bootstrap-args]
    (on-components! (partial init-component! @(:settings @config) bootstrap-args) components)

    (log/info "Components loaded."))

  (shutdown-components! [this]
    (on-components! shutdown-component! components))

  (shutdown! [this]
    (log/info "Shutting down.")

    (shutdown-components! this)
    (when @config
      (clj-components.config/disconnect! @config))

    (log/info "Shut down complete.")))

(defn make-system [component-constructors]
  (ComponentSystem. (atom nil) (atom (into {}
                                           (for [c-c component-constructors :let [c (c-c)]]
                                             [(component/registry-key c) c])))))
