(ns amazonica.aws.machinelearning
  (:use [clojure.walk :only (stringify-keys)])
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
      (.setRecord (stringify-keys (:record value))))))
