(ns clj-components.components.logging
  (:use [clj-components.component])
  (:require [clojure.tools.logging :as log])
  (:import [ch.qos.logback.classic LoggerContext Level]
           [org.slf4j LoggerFactory]
           [ch.qos.logback.classic.joran JoranConfigurator]))

(defrecord LoggingComponent []
  SystemComponent
  (registry-key [this] :logging)

  (init [this {:keys [loggers]}]
    (let [context (LoggerFactory/getILoggerFactory)]
      (doseq [{:keys [ns level]} loggers
              :let [l (.getLogger context (name ns))
                    level (.toUpperCase (name level))]]
        (log/info "Setting log level" ns level)
        (.setLevel l (eval (read-string (format "ch.qos.logback.classic.Level/%s" level))))))))
