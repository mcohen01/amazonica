(ns amazonica.test.cloudwatch
  (:import org.joda.time.DateTime
           java.text.SimpleDateFormat
           java.util.Date)
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.cloudwatch]))

(deftest cloudwatch []

  (clojure.pprint/pprint
    (list-metrics      
      :metric-name "ThrottledRequests"
      :namespace "AWS/DynamoDB"))

  (set-date-format! "MM-dd-yyyy")

  (clojure.pprint/pprint
    (let [date-string (.. (SimpleDateFormat. "MM-dd-yyyy")
                          (format (Date.)))]
      (get-metric-statistics      
        :metric-name "ThrottledRequests"
        :namespace "AWS/DynamoDB"
        :start-time (.minusDays (DateTime.) 1)
        :end-time date-string
        :period 60
        :statistics ["Sum" "Maximum" "Minimum" 
                    "SampleCount" "Average"])))

  (clojure.pprint/pprint
    (describe-alarms :max-records 100))

  (clojure.pprint/pprint
    (describe-alarm-history :max-records 100))
)