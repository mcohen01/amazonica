(ns amazonica.aws.pricing
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.pricing AWSPricingClient]))

(amz/set-client AWSPricingClient *ns*)
