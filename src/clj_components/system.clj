(ns clj-components.system
  (:use [clj-components.protocols.system])
  (:require [clojure.tools.logging :as log]
            [clj-components.config]
            [clj-components.protocols.config-supplier :as config-supplier]
            [clj-components.component-management :as components-manager]))

;;-----------------------------------------------------------
;; Welcome to the ComponentSystem
;;
;; A ComponentSystem manages components. It makes use of a
;; pluggable config-supplier so source config for components.
;;
;; The config-supplier may also call back to the ComponentSystem,
;; to do things like bounce the odd component when appropiate.
;;
;; Examples might be when a piece of config data underpinning a
;; component changes, of if components need bouncing due to
;; a temporal disconnect from the config source.
;;-----------------------------------------------------------

(defn update-components!
  "Execute function f against an atom of components."
  [f components]
  (swap! components #(zipmap (keys %) (map f (vals %)))))

(defrecord ComponentSystem [config-supplier bootstrap-args components]
  ComponentSystemProtocol

  (init! [this]
    (log/info "System starting up.")

    (config-supplier/init! config-supplier (fn [] (handle-settings-reconnect! this)))

    (update-components! (partial components-manager/init-component! this) components)

    (log/info "System started."))

  (shutdown! [this]
    (log/info "System shutting down.")

    (update-components! components-manager/shutdown-component! components)

    (config-supplier/close! config-supplier)

    (log/info "System shut down."))

  (bounce-component! [this k]
    (swap! components assoc k (components-manager/bounce-component! this (@components k))))

  (handle-settings-reconnect! [this]
    (log/info "Bouncing relevant components owing to settings disconnect.")

    (update-components! (partial components-manager/handle-settings-reconnect! this) components)

    (log/info "Finished bouncing relevant components.")))

(defn make-system [component-constructors config-supplier bootstrap-args]
  (ComponentSystem. config-supplier bootstrap-args
                    (components-manager/components-atom component-constructors)))


;; todo we need a proper migration, should live in the avout space?
;; prob a mix of admin code and component code
