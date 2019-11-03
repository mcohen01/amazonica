(ns amazonica.aws.personalizeruntime
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.personalizeruntime.AmazonPersonalizeRuntimeClient))

(amz/set-client AmazonPersonalizeRuntimeClient *ns*)
