(ns amazonica.aws.machinelearning
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.machinelearning AmazonMachineLearningClient]))

(amz/set-client AmazonMachineLearningClient *ns*)
