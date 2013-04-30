(ns amazonica.test.datapipeline
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.datapipeline]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest datapipeline []
  
  (let [pid (:pipeline-id
              (create-pipeline
                cred
                :name "my-pipeline"
                :unique-id "mp"))]
    (clojure.pprint/pprint
      (describe-pipelines cred :pipeline-ids [pid]))
    (delete-pipeline cred :pipeline-id pid))

  (list-pipelines cred)

)