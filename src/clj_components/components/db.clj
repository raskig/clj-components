(ns clj-components.components.db
  (:use [clj-components.protocols.component]))

(defrecord DbComponent []
  BounceOnConfigChange
  SystemComponent
  (registry-key [this] :db)

  (init [this settings _]
    (assoc this :db {:classname "oracle.jdbc.driver.OracleDriver"
                     :subprotocol "oracle:thin"
                     :subname (-> settings deref :db-url)
                     :user (-> settings deref :db-user)
                     :password (-> settings deref :db-password)})))
