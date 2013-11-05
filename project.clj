(defproject clj-components "0.1.3-beta21"
  :description "Component lifecycle management lib based off Avout/Zookeeper"
  :url "https://github.com/MailOnline/clj-components"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Shared
                 [com.fasterxml.jackson.core/jackson-core "2.2.1"]

                 ;; Config
                 [zookeeper-clj "0.9.5" :exclusions [org.slf4j/slf4j-log4j12
                                                     log4j]]
                 [avout "0.5.3"]
                 [environ "0.4.0"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13" :exclusions [org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-access "1.0.13"]
                 [ch.qos.logback/logback-core "1.0.13"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [net.logstash.logback/logstash-logback-encoder "1.2" :exclusions [com.fasterxml.jackson.core/jackson-core]]

                 ;; ElasticSearch Component
                 [clojurewerkz/elastisch "1.3.0-beta1" :exclusions [com.fasterxml.jackson.core/jackson-core]]

                 ;; Riemann Component
                 [riemann-clojure-client "0.2.6"]

                 ;; Redis Component
                 [com.taoensso/carmine "1.7.0"]

                 ;; WebServer Component
                 [ring "1.2.0"]

                 ;; NRepl Component
                 [org.clojure/tools.nrepl "0.2.2"]

                 ;; Quartz Component
                 [clojurewerkz/quartzite "1.0.1"]

                 ;; DB Component
                 [org.clojure/java.jdbc "0.2.3"]
                 [com.oracle/ojdbc14 "10.2.0.4.0"]

                 ;; Memcached Component
                 [clojurewerkz/spyglass "1.1.0"]]
  :profiles {:dev {:plugins [[lein-environ "0.4.0"]]}}
  :repositories {"snapshots" {:url "http://10.251.76.32:8081/nexus/content/repositories/snapshots"
                              :username "admin" :password "admin123"}
                 "releases" {:url "http://10.251.76.32:8081/nexus/content/repositories/releases"
                             :username "admin" :password "admin123" }
                 "thirdparty" {:url "http://10.251.76.32:8081/nexus/content/repositories/thirdparty"}})
