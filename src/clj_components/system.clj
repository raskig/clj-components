(ns clj-components.system
  (:use [clj-components.protocols.system])
  (:require [clj-components.component :as component]
            [clojure.tools.logging :as log]
            [clj-components.config]
            [clj-components.protocols.config-supplier :as config-supplier]
            [clj-components.component-management :as components-manager]))

(defn- bounce-components-on-config! [system new-settings session-id bounce-count]
  (log/info (format "Configuration change detected for session %s, bouncing relevant components (%s times)."
                    session-id bounce-count)))

(defrecord ComponentSystem [config-supplier bootstrap-args components]
  ComponentSystemProtocol

  (init! [this]
    (log/info "System starting up.")

    (config-supplier/init! config-supplier this)
    (components-manager/on-components!
     (partial components-manager/init-component! this config-supplier bootstrap-args) components)

    (log/info "System started."))

  (shutdown! [this]
    (log/info "System shutting down.")

    (components-manager/on-components! components-manager/shutdown-component! components)
    (config-supplier/close! (config-supplier))

    (log/info "System shut down."))

  (bounce-component! [this k]
    (swap! components update-in [k] (components-manager/bounce-component! (@components k))))

  (bounce-components! [this]
    (log/info "Bouncing relevant components.")

    (components-manager/on-components! (partial components-manager/bounce-component! this) components)

    (log/info "Finished bouncing relevant components.")))

(defn make-system [component-constructors config-supplier bootstrap-args]
  (ComponentSystem. config-supplier bootstrap-args
                    (components-manager/components-atom component-constructors)))
