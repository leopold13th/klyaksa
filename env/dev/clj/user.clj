(ns user
  (:require 
            [mount.core :as mount]
            [klyaksa.figwheel :refer [start-fw stop-fw cljs]]
            [klyaksa.core :refer [start-app]]))

(defn start []
  (mount/start-without #'klyaksa.core/repl-server))

(defn stop []
  (mount/stop-except #'klyaksa.core/repl-server))

(defn restart []
  (stop)
  (start))


