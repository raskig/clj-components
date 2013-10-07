(ns clj-components.system
  (:require [clj-components.component :as component]
            [clojure.tools.logging :as log]
            [clj-components.config]))

(defprotocol ComponentSystemProtocol
  (init-config! [this])
  (init-components! [this bootstrap-args])
  (shutdown-components! [this])
  (bounce-components-on-config! [this])
  (shutdown! [this]))

(defn- on-components! [f components]
  (swap! components #(zipmap (keys %) (map f (vals %)))))

(defn- init-component! [system bootstrap-args c]
  (log/info (format "Loading %s" (component/registry-key c)))
  (component/init c (merge @(:settings @(:config system)) bootstrap-args)))

(defn- shutdown-component! [component]
  (when (satisfies? component/ShutdownComponent component)
    (log/info (format "Shutting down %s" (component/registry-key component)))
    (component/shutdown component))
  component)

(defn- bounce-component! [system component]
  (if (satisfies? component/BounceOnConfigChange component)
    (->> component shutdown-component! (init-component! system {}))
    component))

(defn- handle-reconnect! [system e]
    (log/warn "Zookeeper connection event:" e)
    (when (= :Expired (:keeper-state e))
      (log/warn "Zookeeper session expired, reconnecting and bouncing relevant components.")
      (init-config! system)
      (bounce-components-on-config! system)))

(defrecord ComponentSystem [config components]
  ComponentSystemProtocol
  (init-config! [this]
    (reset! config
            (clj-components.config/fetch! (partial handle-reconnect! this)
                                          #(bounce-components-on-config! this))))

  (init-components! [this bootstrap-args]
    (on-components! (partial init-component! this bootstrap-args) components)

    (log/info "Components loaded."))

  (shutdown-components! [this]
    (on-components! shutdown-component! components))

  (bounce-components-on-config! [this]
    (log/info "Configuration change detected, bouncing relevant components.")

    (on-components! (partial bounce-component! this) components)

    (log/info "Finished bouncing relevant components."))

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
