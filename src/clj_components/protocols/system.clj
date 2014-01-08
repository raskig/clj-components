(ns clj-components.protocols.system)

(defprotocol ComponentSystemProtocol
  (init! [this])
  (shutdown! [this])
  (bounce-component! [this k])
  (handle-settings-reconnect! [this]))
