(ns clj-components.components.web-server
  (:use [clj-components.component])
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]))

(defn- graceful-restart [jetty]
    (.setGracefulShutdown jetty 10000)
    (.setStopAtShutdown jetty true))

(defrecord WebServerComponent []
  SystemComponent
  (registry-key [this] :web-server)

  (init [this {:keys [http-port web-handler]}]
  (let [server (jetty/run-jetty web-handler
                                {:port http-port :configurator graceful-restart :join? false})
        hostname (.getHostName (java.net.InetAddress/getLocalHost))]
    (log/info (format "Clojure frontend started on %s:%s. Watch out for stones." hostname http-port))
    (assoc this :server server)))

  ShutdownComponent
  (shutdown [{:keys [server]}]
    (.stop server)))
