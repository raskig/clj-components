(ns clj-components.components.db
  (:use [clj-components.protocols.component]))

(defrecord DbComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :db)

  (init [this settings]
    (assoc this :db {:classname "oracle.jdbc.driver.OracleDriver"
                     :subprotocol "oracle:thin"
                     :subname (-> settings :db-url)
                     :user (-> settings :db-user)
                     :password (-> settings :db-password)})))
