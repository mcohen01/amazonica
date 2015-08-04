(ns amazonica.test.datapipeline
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.datapipeline]))

(deftest datapipeline []
  
  (let [pid (:pipeline-id
              (create-pipeline
                :name "my-pipeline"
                :unique-id "mp"))]
    (clojure.pprint/pprint
      (describe-pipelines :pipeline-ids [pid]))
    (delete-pipeline :pipeline-id pid))

  (list-pipelines)

)