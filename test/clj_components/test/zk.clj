(ns clj-components.test.zk
  (:use [clojure.test]
        [clj-components.component]
        [avout.core])
  (:require [clj-components.bootstrap :as bootstrap]
            [clj-components.system :as system]
            [clj-components.settings :as settings]))

;; test the bootstrapping

(def ^:dynamic some-val :a-val)

(defn fixture [f]
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
  SystemComponent
  BounceOnConfigChange
  (registry-key [this] :test-bouncy)
  (init [this settings]
    (assoc this :some-val some-val)))

(deftest can-boot-up-with-bootstrap-args
  (bootstrap/init! {:bootstrap-foo :bootstrap-bar} [clj-components.test.zk/->TestComponent])
  (is (= :bootstrap-bar (-> system/components :test :booted-with-settings :bootstrap-foo))))

(deftest can-bounce-components-based-off-zk-config-change
  (alter-var-root #'some-val (constantly :first-pass))

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
