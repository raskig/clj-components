(ns clj-components.protocols.config-supplier)

(defprotocol ConfigSupplier
  (init! [this reconnect-fn])
  (close! [this])
  (fetch [this path])
  (register-watcher [this path watch-fn]))
