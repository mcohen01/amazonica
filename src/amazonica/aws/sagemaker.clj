(ns amazonica.aws.sagemaker
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.sagemaker AmazonSageMakerClient]))

(amz/set-client AmazonSageMakerClient *ns*)
