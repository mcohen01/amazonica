(ns amazonica.aws.machinelearning
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.machinelearning
             AmazonMachineLearningClient]
           [com.amazonaws.services.machinelearning.model
              PredictRequest]))

(amz/set-client AmazonMachineLearningClient *ns*)

(amz/register-coercions
  PredictRequest
  (fn [value]
    (doto (PredictRequest.)
      (.setMLModelId (or (:mLModelId value)
                         (:mlmodel-id value)))
      (.setPredictEndpoint (:predict-endpoint value))
      (.setRecord (clojure.walk/stringify-keys (:record value))))))