(ns amazonica.aws.sagemaker-runtime
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.sagemakerruntime AmazonSageMakerRuntimeClient]))

(amz/set-client AmazonSageMakerRuntimeClient *ns*)
