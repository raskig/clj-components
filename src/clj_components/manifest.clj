(ns clj-components.manifest
  (:require [manifest.core :as m]))

(defn fetch [] (m/manifest "fe.system.app"))
