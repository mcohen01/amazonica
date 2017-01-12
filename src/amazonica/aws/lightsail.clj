(ns amazonica.aws.lightsail
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.lightsail AmazonLightsailClient]))

(amz/set-client AmazonLightsailClient *ns*)