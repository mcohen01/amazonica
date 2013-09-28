(ns amazonica.test.glacier
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.aws.glacier]))

(deftest glacier []

  (def upload-file (java.io.File. "upload.txt"))
  
  (create-vault :vault-name "my-vault")
  
  (clojure.pprint/pprint
    (describe-vault :vault-name "my-vault"))
  
  (clojure.pprint/pprint
    (list-vaults :limit 10))

  (let [upload (upload-archive :vault-name "my-vault"
                               :body "upload.txt")]
    (delete-archive :account-id "-"
                    :vault-name "my-vault"
                    :archive-id (:archive-id upload)))

  ;; fails with "Vault not empty or recently written to"
  #_(delete-vault :account-id "-"
                :vault-name "my-vault")
  
)