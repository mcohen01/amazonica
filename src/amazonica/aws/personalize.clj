(ns amazonica.aws.personalize
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.personalize.AmazonPersonalizeClient))

(amz/set-client AmazonPersonalizeClient *ns*)
