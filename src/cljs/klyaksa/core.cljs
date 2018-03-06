(ns klyaksa.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [klyaksa.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [goog.string :as gstring])
  (:import goog.History))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link {:href uri} title]])

(defn navbar []
  [:nav.navbar.navbar-light.navbar-expand-md
   {:role "navigation" :style {:background-color "#d8e3d8"}}
   [:button.navbar-toggler.hidden-sm-up
    {:type "button"
     :data-toggle "collapse"
     :data-target "#collapsing-navbar"}
    [:span.navbar-toggler-icon]]
   [:a.navbar-brand {:href "#/"} [:img {:src (str js/context "/img/klyaksa.png") :width "45" :height "45"}] " Klяksa!"]
   [:div#collapsing-navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav.mr-auto
     [nav-link "#/" "Users" :home]
     [nav-link "#/aliases" "Aliases" :aliases]
     [nav-link "#/about" "About" :about]]]])

(declare fetch-docs!)

(defn find-in-arr [id]
  (loop [i 0 users (session/get :users)]
    (if (= (:id (nth users i)) id)
      i
      (recur (inc i) users))))

(defn update-active-user [id]
  (let [x (find-in-arr id)]
    (session/put! :users (update-in (session/get :users) [x :active] #(if (= % 0) 1 0)))))

(defn update-delete-user [id]
  (session/put! :users (filter #(not= (:id %) id) (session/get :users))))

(defn change-password [id]
  (POST "/pw" {:handler #(js/alert (str "New password " (:result %)))
               :error-handler #(js/alert "Error.")
               :params {:id id, :pw (js/prompt "New pw")}
               :response-format :json
               :keywords? true}))

(defn enable-disable-user [id status]
  (POST "/status" {:handler #(if (= (:result %) 1) (update-active-user id) (js/alert "Error occurred while saving to DB"))
                   :error-handler #(js/alert "Error.")
                   :params {:id id, :status (if status 0 1)}
                   :response-format :json
                   :keywords? true}))

(defn create-user []
  (let [email (js/prompt "Enter email" "@ks-bank.ru")
        c (count email)]
    (if (> c 11)
      (POST "/create" {;:handler #(js/alert (str "Status " (:result %)))
                       :handler #(do (fetch-docs!) (session/put! :filter email))
                       :error-handler #(js/alert "Error.")
                       :params {:email email}
                       :response-format :json
                       :keywords? true}))))

(defn delete-user [id]
  (if (js/confirm "Do you really want to delete mail address?")
    (POST "/delete" {:handler (fn [r]
                                (if (= (:result r) 1)
                                  (update-delete-user id)
                                  (js/alert "Error occurred while deleting from DB")))
                     :error-handler #(js/alert "Error.")
                     :params {:id id}
                     :response-format :json
                     :keywords? true})))

(defn home-page []
  (let [users (session/get :users)]
    ; (js/console.log users)
    [:div.container
      [:div.row>div.col-sm-12
        [:input.form-control {:type "text"
                              ; :id "usr"
                              :value (session/get :filter)
                              :on-change #(session/put! :filter (-> % .-target .-value))}]
        [:p "count: " (count (filter #(gstring/contains (:email %) (session/get :filter)) users))]
        [:button.btn {:on-click #(create-user)} "+ Добавить новый ящик"] [:br] [:br]
        [:div
          [:table.table.table-hover.table-sm
            [:thead [:tr [:th "id"] [:th "email"] [:th "active"] [:th "quota"] [:th "quotac"] [:th]]]
            [:tbody
              (doall (for [x (filter #(gstring/contains (:email %) (session/get :filter)) users)]
                (let [active? (if (= (:active x) 0) false true)
                      tr-style (if active? "" "table-danger")
                      enable-disable-caption (if active? "disable" "enable")]
                  [:tr {:key (:id x), :class tr-style}
                    [:td (str "#" (:id x))]
                    [:td (:email x)]
                    [:td (:active x)]
                    [:td (:quota x)]
                    [:td (:quotac x)]
                    [:td
                      [:button.btn.btn-info {:on-click #(change-password (:id x))} "pass"] " "
                      [:button.btn {:on-click #(enable-disable-user (:id x) active?)} enable-disable-caption] " "
                      [:button.btn.btn-danger.pull-right {:on-click #(delete-user (:id x))} "del"]]])))]]]]]))

(defn del-alias [id]
  (if (js/confirm "Do you really want to delete alias?")
    (POST "/deletealias" {:handler #(if (= (:result %) 1) (fetch-docs!) (js/alert "Error occurred while deleting from DB"))
                          :error-handler #(js/alert "Error.")
                          :params {:id id}
                          :response-format :json
                          :keywords? true})))

(defn create-alias []
  (POST "/createalias" {:handler #(fetch-docs!)
                        :error-handler #(js/alert "Error.")
                        :params {:src (session/get :src) :dst (session/get :dst)}
                        :response-format :json
                        :keywords? true}))

(defn aliases-page []
  [:div.container
    [:div.row>div.col-sm-8
      [:form
        [:div.form-group.row
          [:label.col-2 {:for "ssss"} "source"]
          [:input.form-control.col-6 {:type "text"
                                :id "ssss"
                                :value (session/get :src)
                                :on-change #(session/put! :src (-> % .-target .-value))}]]
        [:div.form-group.row
          [:label.col-2 {:for "dddd"} "destination"]
          [:input.form-control.col-6 {:type "text"
                                :id "dddd"
                                :value (session/get :dst)
                                :on-change #(session/put! :dst (-> % .-target .-value))}]]
        [:button.btn {:on-click #(create-alias)} "+ Добавить алиас"] [:br] [:br]]
      [:table.table.table-hover.table-sm
          [:thead [:tr [:th "id"] [:th "source"] [:th "destination"] [:th]]]
          [:tbody
            (doall
              (for [x (session/get :aliases)]
                [:tr {:key (:id x)}
                  [:td (:id x)]
                  [:td (:source x)]
                  [:td (:destination x)]
                  [:td [:button.btn.btn-danger {:on-click #(del-alias (:id x))} "Delete"]]]))]]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

;(def pass "trez")
(def pass "trez18kha")

(defn redirect! [loc]
  (set! (.-location js/window) loc))

; (defn set-hash! [loc]
;   (set! (.-hash js/window.location) loc))

(defn change-route [url]
  (if (= (session/get :password) pass)
    (session/put! :page url)
    (redirect! "/#/login")))    ;(session/put! :page :login)

(defn check-password []
  (if (= (session/get :password) pass)
    (do
      (.remove (.-classList (.getElementById js/document "body")) "bg")
      (change-route :home))
    (session/put! :error true)))

(defn login-page2 []
  (.add (.-classList (.getElementById js/document "body")) "bg")
  [:div.container
    (if (= (session/get :error) true)
      [:div.justify-content-md-center
          [:div.alert.alert-danger {:role "alert"}
            [:img {:src (str js/context "/img/oops.png")}]
            [:label.col-form-label [:strong " Неверный пароль! Давай еще раз."]]
            [:button.btn.btn-success {:on-click #(session/put! :error false)} "Понял"]]])
    [:div.row.justify-content-md-center
      [:h4 "А какой у тебя пароль, a?"]]
    [:div.row.justify-content-md-center  ;.jumbotron
      [:label.col-form-label "Password:"]
      [:div.col-3
        [:input.form-control {:name "myPass"
               :type "password"
               :on-change #(do (session/put! :error false) (session/put! :password (-> % .-target .-value)))}]]
      [:button.btn.btn-success {:on-click #(check-password)} "Go!"]]])

(defn login-page []
  (.add (.-classList (.getElementById js/document "body")) "bg")
  [:div.container
    (if (= (session/get :error) true)
      [:div.justify-content-md-center
          [:div.alert.alert-danger {:role "alert"}
            [:img {:src (str js/context "/img/oops.png")}]
            [:label.col-form-label [:strong " Неверный пароль! Давай еще раз."]]
            ;[:button.btn.btn-success {:on-click #(session/put! :error false)} "Понял"]
            ]])
    [:div.row.justify-content-md-center
      [:h4 "А какой у тебя пароль, a?"]]
    [:div.row.justify-content-md-center.middle  ;.jumbotron
      [:label.col-form-label "Password:"]
      [:div.col-3
        [:input.form-control {:name "myPass"
               :type "password"
               :on-focus #(session/put! :error false)
               :on-change #(do (session/put! :error false) (session/put! :password (-> % .-target .-value)))}]]
      [:button.btn.btn-success {:on-click #(check-password)} "Go!"]]])

(def pages
  {:home #'home-page
   :aliases #'aliases-page
   :about #'about-page
   :login #'login-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (change-route :home))

(secretary/defroute "/aliases" []
  (change-route :aliases))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/login" []
  (session/put! :page :login))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/users"
    {:handler #(session/put! :users %)
     :response-format :json
     :keywords? true})
  (GET "/aliases"
    {:handler #(session/put! :aliases %)
     :response-format :json
     :keywords? true}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (session/put! :filter "@")
  (session/put! :src "@ks-bank.ru")
  (session/put! :dst "@ks-bank.ru")
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components)
  (change-route :login))