(ns amazonica.test.sqs
  (:use [clojure.test]
        [amazonica.aws.sqs])
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.amazonaws.services.sqs.model.QueueDoesNotExistException))

(deftest sqs []

  (list-queues)
  
  (list-queues (DefaultAWSCredentialsProviderChain.) "my-queue")

  (create-queue :queue-name "my-queue"
                :attributes
                  {:VisibilityTimeout 30 ; sec
                   :MaximumMessageSize 65536 ; bytes
                   :MessageRetentionPeriod 1209600 ; sec
                   :ReceiveMessageWaitTimeSeconds 10}) ; sec
  
  (while (nil? (find-queue "my-queue")))

  (def q (find-queue "my-queue"))

  (get-queue-attributes q)
  (get-queue-attributes q ["All"])

  (create-queue "DLQ")
  (while (nil? (find-queue "DLQ")))
  (assign-dead-letter-queue q
                            (find-queue "DLQ")
                            10)

  (send-message :queue-url q
                :delay-seconds 0
                :message-body (str "test" (java.util.Date.)))
  
  (send-message q "hello world")  
  
  (def msgs (receive-message q))
  
  (delete-message (-> msgs 
                      :messages 
                      first
                      (assoc :queue-url q)))

  (let [msgs (receive-message (DefaultAWSCredentialsProviderChain.)
                              :queue-url q
                              :wait-time-seconds 6
                              :max-number-of-messages 10
                              :delete true
                              :attribute-names ["All"])]
    (is (= 1 (count (:messages msgs)))))

  (let [msgs (receive-message :queue-url q
                              :wait-time-seconds 6
                              :max-number-of-messages 10)]
    (is (= 0 (count (:messages msgs)))))

  (delete-queue :queue-url q)
  (try
    (delete-queue q)
    (catch QueueDoesNotExistException e))

  (-> "DLQ" find-queue delete-queue)

)