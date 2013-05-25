(ns amazonica.test.securitytoken
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws 
          identitymanagement
          securitytoken]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest securitytoken []

  (let [session (:credentials (get-session-token cred))]
    (is (= true (contains? session :access-key-id)))
    (is (= true (contains? session :secret-access-key)))
    (is (= true (contains? session :session-token))))

  (assume-role 
    cred 
    :role-arn 
    (-> (get-role cred :role-name "my-role")
        :role
        :arn))

  (get-user cred)
  
  (get-account-summary cred)
  
  (list-access-keys cred)
  
  (list-instance-profiles cred)
  
)