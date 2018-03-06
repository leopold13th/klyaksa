(ns klyaksa.routes.home
  (:require [klyaksa.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))
(use '[clojure.java.shell :only [sh]])
(use 'clojure.pprint)

(def mysql-db {
      :subprotocol "mysql"
      :subname "//127.0.0.1:3306/mail?serverTimezone=UTC&useSSL=false"
      :user "mailuser"
      :password "zaxoh9eeku7K"})

(defn home-page []
  (layout/render "home.html"))

(defn users-all []
  (let [res (sql/query mysql-db ["select * from virtual_users limit 10000"])]
    (seq res)))

(defn aliases-all []
  (let [res (sql/query mysql-db ["select * from virtual_aliases order by source limit 10000"])]
    (seq res)))

(defroutes home-routes
  (GET "/" []
    (home-page))
  (GET "/users" []
    (-> (response/ok (users-all))
        (response/header "Content-Type" "application/json; charset=utf-8")))
  (GET "/aliases" []
    (-> (response/ok (aliases-all))
        (response/header "Content-Type" "application/json; charset=utf-8")))
  (POST "/pw" [id pw]
    (let [hash (:out (sh "dovecot" "pw" "-s" "SHA256-CRYPT" "-p" pw))
          res (first (sql/update! mysql-db :virtual_users {:password hash} ["id = ?" id]))]
      (-> (response/ok {:result 1})
          (response/header "Content-Type" "application/json; charset=utf-8"))))
  (POST "/status" [id status]
    (let [res (first (sql/update! mysql-db :virtual_users {:active status} ["id = ?" id]))]
      (if (= res 1)
        (-> (response/ok {:result 1})        ; "Changed"
            (response/header "Content-Type" "application/json; charset=utf-8"))
        (-> (response/ok {:result 0})        ; "Failed"
            (response/header "Content-Type" "application/json; charset=utf-8")))))
  (POST "/create" [email]
    (let [res (first (sql/insert! mysql-db :virtual_users {:email email :domain_id 1 :password "!" :active 1}))]
      (if (> (:generated_key res) 0)
        (-> (response/ok {:result 1})        ; "Created"
            (response/header "Content-Type" "application/json; charset=utf-8"))
        (-> (response/ok {:result 0})        ; "Failed"
            (response/header "Content-Type" "application/json; charset=utf-8")))))
  (POST "/createalias" [src dst]
    (let [res (first (sql/insert! mysql-db :virtual_aliases {:domain_id 1 :source src :destination dst}))]
      (if (> (:generated_key res) 0)
        (-> (response/ok {:result 1})        ; "Created"
            (response/header "Content-Type" "application/json; charset=utf-8"))
        (-> (response/ok {:result 0})        ; "Failed"
            (response/header "Content-Type" "application/json; charset=utf-8")))))
  (POST "/delete" [id]
    (let [res (first (sql/delete! mysql-db :virtual_users ["id = ?" id]))]
      (if (= res 1)
        (-> (response/ok {:result 1})        ; "Deleted"
            (response/header "Content-Type" "application/json; charset=utf-8"))
        (-> (response/ok {:result 0})        ; "Failed"
            (response/header "Content-Type" "application/json; charset=utf-8")))))
  (POST "/deletealias" [id]
    (let [res (first (sql/delete! mysql-db :virtual_aliases ["id = ?" id]))]
      (if (= res 1)
        (-> (response/ok {:result 1})        ; "Deleted"
            (response/header "Content-Type" "application/json; charset=utf-8"))
        (-> (response/ok {:result 0})        ; "Failed"
            (response/header "Content-Type" "application/json; charset=utf-8")))))
  (GET "/docs" []
    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
        (response/header "Content-Type" "text/plain; charset=utf-8"))))
