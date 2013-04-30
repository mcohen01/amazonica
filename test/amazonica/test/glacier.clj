(ns amazonica.test.glacier
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.glacier]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest glacier []

  (def upload-file (java.io.File. "upload.txt"))
  
  (create-vault cred :vault-name "my-vault")
  
  (clojure.pprint/pprint
    (describe-vault cred :vault-name "my-vault"))
  
  (clojure.pprint/pprint
    (list-vaults cred :limit 10))

  (upload-archive
    cred
    :vault-name "my-vault"
    :body "upload.txt")
  
  (delete-archive
    cred
    :account-id "-"
    :vault-name "my-vault"
    :archive-id "pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg", :checksum "83a05fd1ce759e401b44fff8f34d40e17236bbdd24d771ec2ca4886b875430f9", :location "/676820690883/vaults/my-vault/archives/pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg")    

  (.delete upload-file)

)