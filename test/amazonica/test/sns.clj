(ns amazonica.test.sns
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.sns]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest sns []

  (create-topic cred :name "my-topic")
  
  (list-topics cred)

  (subscribe
    cred
    :protocol "email"
    :topic-arn "arn:aws:sns:us-east-1:676820690883:my-topic"
    :endpoint "mcohen01@gmail.com")

  (clojure.pprint/pprint
    (list-subscriptions cred))

  (publish
    cred
    :topic-arn "arn:aws:sns:us-east-1:676820690883:my-topic"
    :subject "test"
    :message (str "Todays is " (java.util.Date.)))

  (unsubscribe
    cred
    :subscription-arn
    "arn:aws:sns:us-east-1:676820690883:my-topic:33fb2721-b639-419f-9cc3-b4adec0f4eda")

  (delete-topic
    cred
    :topic-arn "arn:aws:sns:us-east-1:676820690883:my-topic")
)
