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


  (let [user-arn (get-in (get-user) [:user :arn])
        policy (str "{\"Version\": \"2012-10-17\",
                      \"Statement\": {
                        \"Effect\": \"Allow\",
                        \"Principal\": {\"AWS\": [\""user-arn"\"]},
                        \"Action\": \"sts:AssumeRole\"
                      }
                    }")
        role-arn (get-in (create-role :role-name "foobar"
                                      :assume-role-policy-document policy)
                         [:role :arn])]

    (assume-role :role-arn role-arn :role-session-name "baz")

    (delete-role :role-name "foobar"))


  (get-account-summary)

  (list-access-keys)

  (list-instance-profiles)

)
