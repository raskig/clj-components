(ns clj-components.test.zk
  (:use [clojure.test]
        [clj-components.component])
  (:require [clj-components.bootstrap :as bootstrap]))

;; test the bootstrapping

(defrecord TestComponent []
  SystemComponent
  (registry-key [this] :test)
  (init [this settings]
    (assoc this :booted-with-settings settings)))

(deftest can-boot-up-with-bootstrap-args
  (bootstrap/init! {} [clj-components.test.zk/->TestComponent])
  (is true))
