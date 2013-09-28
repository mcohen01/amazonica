(ns amazonica.test.sqs
  (:use [clojure.test]
        [amazonica.aws.sqs]))

(deftest sqs []
  
  (create-queue :queue-name "my-queue"
                :attributes
                  {:VisibilityTimeout 30 ; sec
                   :MaximumMessageSize 65536 ; bytes
                   :MessageRetentionPeriod 1209600 ; sec
                   :ReceiveMessageWaitTimeSeconds 10}) ; sec

  (def q (get (:queue-urls (list-queues)) 0))

  (send-message :queue-url q
                :delay-seconds 0
                :message-body (str "test" (java.util.Date.)))
  
  (let [msgs (receive-message :queue-url q
                              :wait-time-seconds 6
                              :max-number-of-messages 10
                              :delete true
                              :attribute-names ["SenderId" "ApproximateFirstReceiveTimestamp" "ApproximateReceiveCount" "SentTimestamp"])]
    (is (= 1 (count (:messages msgs)))))

  (let [msgs (receive-message :queue-url q
                              :wait-time-seconds 6
                              :max-number-of-messages 10)]
    (is (= 0 (count (:messages msgs)))))
  
  (delete-queue :queue-url q)
)