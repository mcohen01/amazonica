(ns amazonica.test.timestream
  (:use [clojure.test]
        [amazonica.aws.timestreamwrite :as w :exclude [shutdown]]
        [amazonica.aws.timestreamquery :only [query]]))

(def table-name "amazonica-test-table")
(def db-name "amazonica-test-db")

(defn transform-request [response]
  (map #(reduce into {} (for [i (range (count (:column-info response)))]
            (assoc {} (keyword (:name (get (:column-info response) i)))
                   (:scalar-value (get (:data %) i)))))
       (:rows response)))

(deftest timestream-write-query
  (w/create-database {:database-name db-name})
  (w/create-table {:database-name db-name :table-name table-name})
  (let [now (System/currentTimeMillis)]
    (w/write-records
     {:database-name db-name,
      :table-name table-name,
      :records [{:dimensions [{:name "dimensionX" :value "1"}]
                 :measureName "measurement1" :measureValue "1.1" :measureValueType "DOUBLE" :time now}]})

    (try (let [measurements (query {:query-string (str "SELECT * from \"" db-name "\".\"" table-name "\"")})]
           (is (= [{:data [{:scalar-value "1"}
                           {:scalar-value "measurement1"}
                           {:scalar-value (.format (.withZone
                                                         (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS000000")
                                                         (java.time.ZoneOffset/UTC))
                                                        (java.time.Instant/ofEpochMilli now))}
                           {:scalar-value "1.1"}]}]
                  (:rows measurements))))))
  (w/delete-table {:database-name db-name :table-name table-name})
  (w/delete-database {:database-name db-name}))

(deftest timestream-write-multi-measure-records-query
  (w/create-database {:database-name db-name})
  (w/create-table {:database-name db-name :table-name table-name})
  (let [now (System/currentTimeMillis)]
    (w/write-records
     {:database-name db-name,
      :table-name table-name,
      :records [{:dimensions [{:name "dimensionX" :value "1"}]
                 :measureName "measurement1"
                 :measureValueType "MULTI"
                 :measureValues [
                                 {:name "measurement2" :value "1.1" :type "DOUBLE"}
                                 {:name "measurement3" :value "1.2" :type "DOUBLE"}
                                 {:name "measurement4" :value "0.1" :type "DOUBLE"}
                                 {:name "measurement5" :value "0.7" :type "DOUBLE"}
                                 ]
                 :time now}]})
    (try (let [measurements (query {:query-string (str "SELECT * from \"" db-name "\".\"" table-name "\"")})]
           (is (= [{:dimensionX "1",
                    :measure_name "measurement1",
                    :time (.format (.withZone
                                    (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS000000")
                                    (java.time.ZoneOffset/UTC))
                                   (java.time.Instant/ofEpochMilli now)),
                    :measurement2 "1.1",
                    :measurement3 "1.2",
                    :measurement4 "0.1",
                    :measurement5 "0.7"}]
                  (transform-request measurements))))))
  (w/delete-table {:database-name db-name :table-name table-name})
  (w/delete-database {:database-name db-name}))
