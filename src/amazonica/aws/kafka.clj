(ns amazonica.aws.kafka
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.kafka AWSKafkaClient]))

(amz/set-client AWSKafkaClient *ns*)
