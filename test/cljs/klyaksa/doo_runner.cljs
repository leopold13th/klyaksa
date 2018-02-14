(ns klyaksa.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [klyaksa.core-test]))

(doo-tests 'klyaksa.core-test)

