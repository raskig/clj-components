(ns clj-components.test.zk
  (:use [clojure.test]
        [clj-components.component])
  (:require [clj-components.bootstrap :as bootstrap]
            [clj-components.system :as system]))

;; test the bootstrapping

(defrecord TestComponent []
  SystemComponent
  (registry-key [this] :test)
  (init [this settings]
    (assoc this :booted-with-settings settings)))

(deftest can-boot-up-with-bootstrap-args
  (binding [clj-components.config/zk-root (constantly :clj-components-test)]
    (bootstrap/init! {:bootstrap-foo :bootstrap-bar} [clj-components.test.zk/->TestComponent])
    (is (= {:bootstrap-foo :bootstrap-bar} (-> system/components :test :booted-with-settings) ))))
