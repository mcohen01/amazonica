(ns amazonica.test.dynamodbv2
  (:import org.joda.time.DateTime
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID)
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]])
  (:use [clojure.test]
        amazonica.core
        [amazonica.aws
          dynamodbv2]))

(def table "TestTable")

(deftest dynamodbv2 []  

  (create-table 
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
  (doseq [table (:table-names (list-tables))]
    (loop [status (get-in (describe-table :table-name table)
                          [:table :table-status])]
      (if-not (= "ACTIVE" status)
        (do 
          (println "waiting for status" status "to be active")
          (Thread/sleep 1000)
          (recur (get-in (describe-table :table-name table)
                          [:table :table-status]))))))
 
  (set-root-unwrapping! true)

  (is 
    (= "id" 
      (get-in 
        (describe-table :table-name table)
        [:key-schema 0 :attribute-name])))

  (set-root-unwrapping! false)

  (is 
    (= "id" 
      (get-in 
        (describe-table :table-name table)
        [:table :key-schema 0 :attribute-name])))
  
  (list-tables)
  (list-tables :limit 1)

  (let [item {:id "foo"
              :date 123456
              :text "barbaz"
              :column1 "first name"
              :column2 "last name"
              :bytes (.getBytes "some bytes")
              :someMap {:text "bar" :number 7}
              :someList ["one" "two" 3]
              :mapInMap {:foo {:bar "yes"}}
              :setInMap {:mySet #{"one" "two"}}
              :nullAttr nil
              :boolAttr true
              :listInMap {:myList ["one" "two"]}
              :complex {:mySet #{"one" "two"} :foo {:bar "it works"} :answer 42}
              :someSet #{17 42}}]
    (put-item      
      :table-name table
      :return-consumed-capacity "TOTAL"
      :return-item-collection-metrics "SIZE"
      :item item)
    (let [ret-item (:item (get-item                            
                            :table-name table
                            :key 
                            {:id {:s "foo"}
                             :date {:n 123456}}))]
      (is (= (dissoc item :bytes) (dissoc ret-item :bytes)))
      (is (= (-> item :bytes String.)
             (-> ret-item :bytes .array String.)))))
  
    
  (query :table-name table
         :limit 1
         :index-name "column1_idx"
         :select "ALL_ATTRIBUTES"
         :scan-index-forward true
         :key-conditions 
           {:id      {:attribute-value-list ["foo"]      :comparison-operator "EQ"}
            :column1 {:attribute-value-list ["first na"] :comparison-operator "BEGINS_WITH"}})

  (clojure.pprint/pprint
    (scan :table-name "TestTable"))

  (set-root-unwrapping! false)

(let [item (batch-get-item 
             :return-consumed-capacity "TOTAL"
             :request-items {
             "TestTable" {:keys [{"id"   {:s "foobar"}
                                  "date" {:n 3172671}}
                                 {"id"   {:s "foo"}
                                  "date" {:n 123456}}]
                          :consistent-read true
                          :attributes-to-get ["id" "text" "column1"]}})]
  (is (= "barbaz" (-> item :responses :TestTable first :text)))
  (is (= "foo"    (-> item :responses :TestTable first :id))))

(batch-write-item
  :return-consumed-capacity "TOTAL"
  :return-item-collection-metrics "SIZE"
  :request-items 
    {"TestTable"
      [{:delete-request 
         {:key {:id "foo"
                :date 123456}}}
       {:put-request
         {:item {:id "foobar"
                 :date 3172671
                 :text "bunny"
                 :column1 "funky"}}}]})
  
  (clojure.pprint/pprint 
    (describe-table :table-name "TestTable"))

  (delete-table :table-name "TestTable")

)
