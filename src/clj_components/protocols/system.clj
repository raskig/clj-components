(ns clj-components.protocols.system)

(defprotocol ComponentSystemProtocol
  (init! [this])
  (shutdown! [this])
  (bounce-component! [this k])
  (bounce-components! [this]))
