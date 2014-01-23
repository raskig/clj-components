(ns clj-components.components.web-server
  (:use [clj-components.protocols.component])
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io])
  (:import [ch.qos.logback.access.jetty RequestLogImpl]
           [org.eclipse.jetty.server NCSARequestLog]
           [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.server Server Request]
           (org.eclipse.jetty.server.handler HandlerCollection ContextHandlerCollection
                                             RequestLogHandler)))

(defn- set-buffer-sizes [server]
  (doseq [connector (.getConnectors server)]
    (let [header-size 1048576]
      (.setRequestHeaderSize connector
                             header-size)
      (.setResponseHeaderSize connector
                              header-size))))

;; See https://groups.google.com/d/topic/ring-clojure/07VoXVHrQ_M/discussion
(defn ^Server run-jetty-hacked [options & handlers]
  (let [handler-collection (HandlerCollection.)
        ^Server s (#'jetty/create-server (dissoc options :configurator))
        ^QueuedThreadPool p (QueuedThreadPool. ^Integer (options :max-threads 50))
        dirty-shutdown (options :dirty-shutdown false)
        graaceful-timeout (if dirty-shutdown
                            1
                            10000)]
    (doseq [h (remove nil? handlers)]
      (.addHandler handler-collection (if (or (fn? h) (var? h)) (#'jetty/proxy-handler h) h)))
    (doto s
      (.setHandler handler-collection)
      (.setThreadPool p)
      (.setGracefulShutdown graaceful-timeout)
      (.setStopAtShutdown (not dirty-shutdown))
      (set-buffer-sizes)
      (.start))))

(defn- request-log-handler []
  (doto (RequestLogHandler.)
    (.setRequestLog
     (doto (RequestLogImpl.)
       (.setQuiet true)
       (.setResource "/logback-access.xml")))))

(defrecord WebServerComponent []
  SystemComponent
  (registry-key [this] :web-server)

  (init [this _ {:keys [http-port http-handler http-request-logs? dirty-shutdown]}]
    (assert http-port)
    (assert http-handler)

    (let [server (run-jetty-hacked {:port http-port
                                    :dirty-shutdown dirty-shutdown}
                                   http-handler
                                   (when http-request-logs? (request-log-handler)))
          hostname (.getHostName (java.net.InetAddress/getLocalHost))]

      (log/info (format "Ring started on %s:%s. Watch out for stones." hostname http-port))
      (assoc this :server server)))

  ShutdownComponent
  (shutdown [{:keys [server]}]
    (.stop server)))
