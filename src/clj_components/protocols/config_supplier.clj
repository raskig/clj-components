(ns clj-components.protocols.config-supplier)

(defprotocol ConfigSupplier
  (init! [this system])
  (close! [this])
  (fetch [this system path]))
