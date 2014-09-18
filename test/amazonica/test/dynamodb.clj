(ns amazonica.test.dynamodb
  (:require [amazonica.core :as amz])
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.aws.dynamodb]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest dynamodb []  

  (create-table cred 
                :table-name "TestTable"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput {
                  :read-capacity-units 1
                  :write-capacity-units 1
                })

  (create-table cred
                :table-name "TestTable2"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                  :range-key-element {
                    :attribute-name "range"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput {
                  :read-capacity-units 1
                  :write-capacity-units 1
                })

  (create-table cred
                :table-name "TestTable3"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput [1 1])

  ; wait for the tables to be created
  (doseq [table (:table-names (list-tables cred))]
    (loop [status (get-in (describe-table cred :table-name table)
                          [:table :table-status])]
      (if-not (= "ACTIVE" status)
        (do 
          (println "waiting for status" status "to be active")
          (Thread/sleep 1000)
          (recur (get-in (describe-table cred :table-name table)
                          [:table :table-status]))))))
 

  (amz/set-root-unwrapping! true)

  (is (= "id" 
         (get-in 
           (describe-table cred :table-name "TestTable")
           [:key-schema :hash-key-element :attribute-name])))

  (amz/set-root-unwrapping! false)

  (is (= "id" 
         (get-in 
           (describe-table cred :table-name "TestTable")
           [:table :key-schema :hash-key-element :attribute-name])))
  
  (list-tables cred)
  (list-tables cred :limit 1)

  (dotimes [x 10] 
    (let [m {:id (str "1234" x) :text "joey t"}]
      (put-item cred 
                :table-name "TestTable" 
                :item m)))

  (try
    (put-item cred
            :table-name "TestTable"
            :item {
              :id "foo" 
              :text "barbaz"
            })
    (catch Exception e
      (.printStackTrace e)))

  (put-item cred
            :table-name "TestTable2"
            :item {
              :id { :s "foo" }
              :range { :s "foo" } 
              :text { :s "zonica" }
            })
  (try
    (get-item cred
              :table-name "TestTableXXX"
              :key "foo")
  (catch Exception e
    (let [error-map (ex->map e)]
      (is (contains? error-map :error-code))
      (is (contains? error-map :status-code))
      (is (contains? error-map :service-name))
      (is (contains? error-map :message)))))

  (query cred
         :table-name "TestTable2"
         :limit 1
         :hash-key-value "mofo"
         :range-key-condition {
           :attribute-value-list ["f"]
           :comparison-operator "BEGINS_WITH"
          })

  (clojure.pprint/pprint
    (scan cred :table-name "TestTable"))

  (amz/set-root-unwrapping! false)

  (clojure.pprint/pprint (batch-get-item cred :request-items {
     "TestTable" { :keys [
                     {:hash-key-element {:s "foo"}}
                     {:hash-key-element {:s "1234"}}
                   ]
                   :consistent-read true
                   :attributes-to-get ["id" "text"]}}))

  
  #_(try
    (batch-write-item cred :request-items {
    "TestTable" [
      {:put-request {
        :item {
          :id "1234"
          :text "mofo"}}}
      {:put-request {
        :item {
          :id "foo"
          :text "barbarbanks"}}}]})
    (catch Exception e
      (println (.printStackTrace e))))

  (clojure.pprint/pprint 
    (describe-table cred :table-name "TestTable"))

  (delete-table cred :table-name "TestTable")
  (delete-table cred :table-name "TestTable2")
  (delete-table cred :table-name "TestTable3")

)