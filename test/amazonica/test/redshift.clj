(ns amazonica.test.redshift
  (:use [clojure.test]
        [amazonica.core]
        [amazonica.aws.redshift]))

(deftest redshift []

  (println (describe-cluster-versions))
  (println (describe-clusters))


  (try
    (create-cluster-subnet-group :cluster-subnet-group-name "my-subnet"
                                 :description "some desc"
                                 :subnet-ids ["1" "2" "3" "4"])
    (throw (Exception. "create-cluster-subnet-group did not throw exception"))
    (catch Exception e
      (is (.contains 
            (:message (ex->map e))
            "Some input subnets in :[1, 2, 3, 4] are invalid."))))
    
 (amazonica.aws.redshift/describe-events :source-type "cluster")

 (create-cluster-parameter-group :parameter-group-name "foobar"
                                 :description "foobar"
                                 :parameter-group-family "redshift-1.0")
  (try
    (modify-cluster-parameter-group
      :parameter-group-name "foobar"
      :parameters [
       {:source          "user"
        :parameter-name  "my_new_param"
        :parameter-value "some value"
        :data-type       "String"
        :description     "some generic param"}
       {:source          "user"
        :parameter-name  "my_new_param-2"
        :parameter-value 42
        :data-type       "Number"
        :description     "some integer param"}])
    (throw (Exception. "modify-cluster-parameter-group did not throw exception"))
    (catch Exception e
      (println (:message (ex->map e)))
      (is (.contains
            (:message (ex->map e))
            "Could not find parameter with name: my_new_param"))))
  
  (delete-cluster-parameter-group :parameter-group-name "foobar")

)