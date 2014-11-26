(ns amazonica.test.kms
  (:use [clojure.test]
        [amazonica.core]
        [amazonica.aws.kms]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred
  (apply
    hash-map
      (interleave
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest kms []
  (list-keys cred)

  (def test-kms-key (create-key cred))
  (disable-key cred
               :key-id (:key-id (:key-metadata test-kms-key)))
)
