(ns clj-components.system
  (:require [clj-components.settings]
            [clj-components.config]
            [clj-components.manifest]))

(declare components)

(defn configure! [components]
  (def components components))

(defn summary []
  (into (sorted-map)
        (assoc @clj-components.settings/settings
          :zk_cluster (clj-components.config/zk-ips)
          :zk_root (clj-components.config/zk-root)
          :manifest (clj-components.manifest/fetch)
          :status "OK")))
