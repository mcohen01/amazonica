(ns amazonica.aws.augmentedairuntime
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.augmentedairuntime.AmazonAugmentedAIRuntimeClient))

(amz/set-client AmazonAugmentedAIRuntimeClient *ns*)
