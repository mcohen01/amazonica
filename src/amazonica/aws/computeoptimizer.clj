(ns amazonica.aws.computeoptimizer
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.computeoptimizer.AWSComputeOptimizerClient))

(amz/set-client AWSComputeOptimizerClient *ns*)
