(ns amazonica.aws.simpleworkflow
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient))

(amz/set-client AmazonSimpleWorkflowClient *ns*)