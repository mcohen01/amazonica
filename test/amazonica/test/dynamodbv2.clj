(ns amazonica.test.dynamodbv2
  (:import org.joda.time.DateTime
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID)
  (:require [clojure.string :as str])
  (:use [clojure.test]
        amazonica.core
        [amazonica.aws
          dynamodbv2]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(def table "TestTable")

(deftest dynamodbv2 []  

  (create-table 
    cred 
    :table-name table
    :key-schema 
      [{:attribute-name "id"   :key-type "HASH"}
       {:attribute-name "date" :key-type "RANGE"}]
    :attribute-definitions 
      [{:attribute-name "id"      :attribute-type "S"}
       {:attribute-name "date"    :attribute-type "N"}
       {:attribute-name "column1" :attribute-type "S"}
       {:attribute-name "column2" :attribute-type "S"}]
    :local-secondary-indexes
      [{:index-name "column1_idx"
        :key-schema
         [{:attribute-name "id"   :key-type "HASH"}
          {:attribute-name "column1" :key-type "RANGE"}]
       :projection
         {:projection-type "INCLUDE"
          :non-key-attributes ["id" "date" "column1"]}}
       {:index-name "column2_idx"
        :key-schema
         [{:attribute-name "id"   :key-type "HASH"}
          {:attribute-name "column2" :key-type "RANGE"}]
       :projection {:projection-type "ALL"}}]
    :provisioned-throughput
      {:read-capacity-units 1
       :write-capacity-units 1})
  
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
 
  (set-root-unwrapping! true)

  (is 
    (= "id" 
      (get-in 
        (describe-table cred :table-name table)
        [:key-schema :hash-key-element :attribute-name])))

  (set-root-unwrapping! false)

  (is 
    (= "id" 
      (get-in 
        (describe-table cred :table-name table)
        [:table :key-schema :hash-key-element :attribute-name])))
  
  (list-tables cred)
  (list-tables cred :limit 1)

  (let [item {:id "foo"
              :date 123456
              :text "barbaz"
              :column1 "first name"
              :column2 "last name"
              :bytes (.getBytes "some bytes")}]
    (put-item
      cred
      :table-name table
      :return-consumed-capacity "TOTAL"
      :return-item-collection-metrics "SIZE"
      :item item)
    (let [ret-item (:item (get-item
                            cred
                            :table-name table
                            :key 
                            {:id {:s "foo"}
                             :date {:n 123456}}))]
      (is (= (dissoc item :bytes) (dissoc ret-item :bytes)))
      (is (= (-> item :bytes String.)
             (-> ret-item :bytes .array String.)))))
    
  (query
    cred
    :table-name table
    :limit 1
    :index-name "column1_idx"
    :select "ALL_ATTRIBUTES"
    :scan-index-forward true
    :key-conditions 
     {:id      {:attribute-value-list ["foo"]      :comparison-operator "EQ"}
      :column1 {:attribute-value-list ["first na"] :comparison-operator "BEGINS_WITH"}})

  (clojure.pprint/pprint
    (scan cred :table-name "TestTable"))

  (set-root-unwrapping! false)

(try
  (clojure.pprint/pprint 
    (batch-get-item
      cred 
      :return-consumed-capacity "TOTAL"
      :request-items {
      "TestTable" {:keys
                    [{"id" {:s "foo"}
                     "date" {:n 12345}}]
                   :consistent-read true
                   :attributes-to-get ["id" "text"]}}))
(catch Exception e
  (.printStackTrace e)))
  
  (clojure.pprint/pprint 
    (describe-table cred :table-name "TestTable"))

  (delete-table cred :table-name "TestTable")

)
