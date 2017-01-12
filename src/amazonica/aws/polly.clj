(ns amazonica.aws.polly
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.polly AmazonPollyClient]))

(amz/set-client AmazonPollyClient *ns*)