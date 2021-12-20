(ns amazonica.test.timestream
  (:use [clojure.test]
        [amazonica.aws.timestreamwrite :as w :exclude [shutdown]]
        [amazonica.aws.timestreamquery :only [query]]))

(def table-name "amazonica-test-table")
(def db-name "amazonica-test-db")

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
