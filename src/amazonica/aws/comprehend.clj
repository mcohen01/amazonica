(ns amazonica.aws.comprehend
  (:require [amazonica.core :as amz])
  (:import [ com.amazonaws.services.comprehend AmazonComprehend AmazonComprehendClient]))

(amz/set-client AmazonComprehendClient *ns*)
