(ns clj-components.components.db
  (:use [clj-components.component]))

(defrecord DbComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :db)

  (init [this settings]
    (let [settings (-> settings :components :db)]
      (assoc this :db {:classname "oracle.jdbc.driver.OracleDriver"
                       :subprotocol "oracle:thin"
                       :subname (-> settings :db-url)
                       :user (-> settings :db-user)
                       :password (-> settings :db-password)}))))
