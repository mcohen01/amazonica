(ns amazonica.test.apigateway
  (:refer-clojure :exclude [get-method])
  (:use [clojure.test]
        [clojure.set]
        [amazonica.aws.apigateway]))

(def cred
  (let [access "aws_access_key_id"
        secret "aws_secret_access_key"
        file   "/.aws/credentials"
        creds  (-> "user.home"
                   System/getProperty
                   (str file)
                   ^String (slurp)
                   (.split "\n"))]
    (clojure.set/rename-keys
      (reduce
        (fn [m e]
          (let [pair (.split ^String e "=")]
            (if (some #{access secret} [(first pair)])
                (apply assoc m pair)
                m)))
        {}
        creds)
      {access :access-key secret :secret-key})))

(deftest apigateway

  (get-rest-apis cred)

)
