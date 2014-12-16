(ns amazonica.test.securitytoken
  (:use [clojure.test]
        [amazonica.aws 
          identitymanagement
          s3
          securitytoken]))

(deftest securitytoken []

  (let [session (:credentials (get-session-token))]
    (is (= true (contains? session :access-key)))
    (is (= true (contains? session :secret-key)))
    (is (= true (contains? session :session-token))))

  (assume-role 
    :role-arn 
    (-> (get-role :role-name "my-role")
        :role
        :arn))

  (get-user)
  
  (get-account-summary)
  
  (list-access-keys)
  
  (list-instance-profiles)
  
)