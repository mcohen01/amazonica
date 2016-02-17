(ns amazonica.test.kms
  (:use [clojure.test]
        [amazonica.core]
        [amazonica.aws.kms]))

(deftest kms []
  (list-keys)

  (let [k (create-key)
        k (:key-metadata k)]
    (disable-key k)
    (schedule-key-deletion (merge k {:pending-window-in-days 7})))
)
