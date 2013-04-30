(ns amazonica.test.cloudwatch
  (:import org.joda.time.DateTime
           java.text.SimpleDateFormat
           java.util.Date)
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.cloudwatch]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest cloudwatch []

  (clojure.pprint/pprint
    (list-metrics
      cred
      :metric-name "ThrottledRequests"
      :namespace "AWS/DynamoDB"))

  (set-date-format! "MM-dd-yyyy")

  (clojure.pprint/pprint
    (let [date-string (.. (SimpleDateFormat. "MM-dd-yyyy")
                          (format (Date.)))]
      (get-metric-statistics 
        cred
        :metric-name "ThrottledRequests"
        :namespace "AWS/DynamoDB"
        :start-time (.minusDays (DateTime.) 1)
        :end-time date-string
        :period 60
        :statistics ["Sum" "Maximum" "Minimum" 
                    "SampleCount" "Average"])))

  (clojure.pprint/pprint
    (describe-alarms cred :max-records 100))

  (clojure.pprint/pprint
    (describe-alarm-history cred :max-records 100))
)