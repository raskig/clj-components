(ns clj-components.test.zk
  (:use [clojure.test]
        [clj-components.component]
        [avout.core])
  (:require [clj-components.bootstrap :as bootstrap]
            [clj-components.system :as system]
            [clj-components.settings :as settings]
            [zookeeper :as zk])
  (:import [org.apache.zookeeper ZooKeeper]))

;; test the bootstrapping

(declare ^:dynamic some-val)
(declare ^:dynamic var-to-prove-shutdown)

(defn fixture [f]
  (alter-var-root #'var-to-prove-shutdown (constantly false))
  (alter-var-root #'some-val (constantly :first-pass))

  (binding [clj-components.config/zk-root (constantly :clj-components-test)]
    (f)
    (bootstrap/shutdown!)))

(use-fixtures :each fixture)

(defrecord TestComponent []
  SystemComponent
  (registry-key [this] :test)
  (init [this settings]
    (assoc this
      :booted-with-settings settings
      :some-val some-val)))

(defrecord TestBounceableComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :test-bouncy)
  (init [this settings]
    (assoc this :some-val some-val))

  ShutdownComponent
  (shutdown [this]
    (alter-var-root #'var-to-prove-shutdown (constantly true))))

(deftest can-boot-up-with-bootstrap-args
  (bootstrap/init! {:bootstrap-foo :bootstrap-bar} [clj-components.test.zk/->TestComponent])
  (is (= :bootstrap-bar (-> system/components :test :booted-with-settings :bootstrap-foo))))

(deftest can-bounce-components-based-off-zk-config-change
  (bootstrap/init! {} [clj-components.test.zk/->TestComponent
                       clj-components.test.zk/->TestBounceableComponent])

  (is (= :first-pass (-> system/components :test :some-val)))
  (is (= :first-pass (-> system/components :test-bouncy :some-val)))

  (alter-var-root #'some-val (constantly :second-pass))

  (let [client (:client settings/config)]
    (dosync!! client
              (alter!! settings/settings assoc :poke-settings :poked)))

  (Thread/sleep 1000)

  (is (= :first-pass (-> system/components :test :some-val)))
  (is (= :second-pass (-> system/components :test-bouncy :some-val))))

(deftest can-shutdown
  (bootstrap/init! {} [clj-components.test.zk/->TestBounceableComponent])

  (is (= false var-to-prove-shutdown))

  (bootstrap/shutdown!)

  (Thread/sleep 1000)

  (is var-to-prove-shutdown)
  (is (nil? clj-components.system/components)))

(deftest can-shutdown-and-re-init
  (bootstrap/init! {} [clj-components.test.zk/->TestComponent])

  (bootstrap/shutdown!)

  (bootstrap/init! {:bootstrap-foo :bootstrap-bar} [clj-components.test.zk/->TestComponent])

  (Thread/sleep 1000)

  (is (= :bootstrap-bar (-> system/components :test :booted-with-settings :bootstrap-foo))))

(defn- expire-zk-conn [conn]
  (let [clone (ZooKeeper. (clj-components.config/zk-ips) 60000, nil, (zk/session-id conn), (zk/session-password conn))]
    (println  "Closing" (zk/session-id conn), (zk/session-password conn))
    (zk/close clone)
    (println "Connection state of original connection is now:" (zk/state conn))))

(deftest can-recover-from-severed-zk-connection
  (bootstrap/init! {} [clj-components.test.zk/->TestComponent
                       clj-components.test.zk/->TestBounceableComponent])

  (alter-var-root #'some-val (constantly :second-pass))

  (expire-zk-conn (:client settings/config))

  (Thread/sleep 2000)

  (is (= :first-pass (-> system/components :test :some-val)))
  (is (= :second-pass (-> system/components :test-bouncy :some-val)))

  (alter-var-root #'some-val (constantly :third-pass))

  (let [client (:client settings/config)]
    (dosync!! client
              (alter!! settings/settings assoc :poke-settings :poked)))

  (Thread/sleep 1000)

  (is (= :first-pass (-> system/components :test :some-val)))
  (is (= :third-pass (-> system/components :test-bouncy :some-val))))
