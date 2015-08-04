(ns amazonica.test.sns
  (:import java.util.UUID)
  (:use [clojure.test]
        [amazonica.aws.sns]))

(deftest sns []

  (def topic-name (.. (UUID/randomUUID) toString))
  
  (let [topic (:topic-arn (create-topic :name topic-name))]
  
    (clojure.pprint/pprint (list-topics))

    (subscribe :protocol "http"
               :topic-arn topic
               :endpoint "http://www.example.com")

    (clojure.pprint/pprint (list-subscriptions))

    (publish :topic-arn topic
             :subject "test"
             :message (str "Todays is " (java.util.Date.)))

    (delete-topic :topic-arn topic))
  
)
