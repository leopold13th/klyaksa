(ns ^:figwheel-no-load klyaksa.app
  (:require [klyaksa.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
