(defproject clj-components "0.1.0"
  :description "Component lifecycle management lib based off Avout/Zookeeper"
  :url "https://github.com/jonpither/clj-components"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Config
                 [clj_manifest "0.1.0"]
                 [zookeeper-clj "0.9.5"]
                 [avout "0.5.3"]
                 [environ "0.4.0"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.6.6"]
                 [log4j/log4j "1.2.16" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]

                 ;; ElasticSearch Component
                 [clojurewerkz/elastisch "1.3.0-beta1"]

                 ;; Riemann Component
                 [riemann-clojure-client "0.2.6"]

                 ;; Redis Component
                 [com.taoensso/carmine "1.7.0"]

                 ;; WebServer Component
                 [ring "1.2.0"]

                 ;; NRepl Component
                 [org.clojure/tools.nrepl "0.2.2"]]
  :repositories {"snapshots" {:url "http://10.251.76.32:8081/nexus/content/repositories/snapshots"
                              :username "admin" :password "admin123"}
                 "releases" {:url "http://10.251.76.32:8081/nexus/content/repositories/releases"
                             :username "admin" :password "admin123" }
                 "thirdparty" {:url "http://10.251.76.32:8081/nexus/content/repositories/thirdparty"}})
