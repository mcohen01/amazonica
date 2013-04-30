(ns amazonica.test.sqs
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.sqs]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest sqs []
  
  (create-queue
    cred
    :queue-name "my-queue"
    :attributes
      {:VisibilityTimeout 30 ; sec
       :MaximumMessageSize 65536 ; bytes
       :MessageRetentionPeriod 1209600 ; sec
       :ReceiveMessageWaitTimeSeconds 10}) ; sec

  (def q (get (:queue-urls (list-queues cred)) 0))

  (send-message
    cred
    :queue-url q
    :delay-seconds 0
    :message-body (str "test" (java.util.Date.)))
  
  (clojure.pprint/pprint
  (receive-message
    cred
    :queue-url q
    :wait-time-seconds 6
    :max-number-of-messages 10
    :delete true
    :attribute-names ["SenderId" "ApproximateFirstReceiveTimestamp" "ApproximateReceiveCount" "SentTimestamp"]))

  (delete-queue
    cred
    :queue-url q)
)