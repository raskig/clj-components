(ns clj-components.components.logging
  (:use [clj-components.protocols.component])
  (:require [clojure.tools.logging :as log])
  (:import [ch.qos.logback.classic LoggerContext Level]
           [org.slf4j LoggerFactory]
           [ch.qos.logback.classic.joran JoranConfigurator]))

(defn set-level! [ns level]
  (let [context (LoggerFactory/getILoggerFactory)
        l (.getLogger context (name ns))
        level (.toUpperCase (name level))]
    (log/info "Setting log level" ns level)
    (.setLevel l (eval (read-string (format "ch.qos.logback.classic.Level/%s" level))))))

(defrecord LoggingComponent []
  SystemComponent
  (registry-key [this] :logging)

  (init [this _ {:keys [loggers]}]
    (doseq [{:keys [ns level]} loggers]
      (set-level! ns level))))
