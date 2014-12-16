(ns amazonica.test.kms
  (:use [clojure.test]
        [amazonica.core]
        [amazonica.aws.kms]))

(deftest kms []
  (list-keys)

  (def test-kms-key (create-key))
  (disable-key :key-id (:key-id (:key-metadata test-kms-key)))
)
