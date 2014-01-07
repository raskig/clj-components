(ns clj-components.components.nrepl
  (:use [clj-components.protocols.component])
  (:require [clojure.tools.nrepl.server :as nrserver]
            [clojure.tools.logging :as log]))

(defrecord NReplComponent []
  SystemComponent
  (registry-key [this] :nrepl)

  (init [this {:keys [nrepl-port]}]
    (if nrepl-port
      (let [server (nrserver/start-server :port nrepl-port)]
        (log/info (format "nRepl server started on %s" nrepl-port))
        (assoc this :server server))
      this))

  ShutdownComponent
  (shutdown [{:keys [server]}]
    (when server
      (.close server))))
