(ns clj-components.components.nrepl
  (:use [clj-components.component])
  (:require [clojure.tools.nrepl.server :as nrserver]
            [clojure.tools.logging :as log]))

(defrecord NReplComponent []
  SystemComponent
  (registry-key [this] :nrepl)

  (init [this {:keys [nrepl-port]}]
    (when nrepl-port
      (nrserver/start-server :port nrepl-port)
      (log/info (format "nRepl server started on %s" nrepl-port)))
    this))
