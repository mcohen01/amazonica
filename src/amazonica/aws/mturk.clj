(ns amazonica.aws.mturk
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mturk AmazonMTurkClient]))

(amz/set-client AmazonMTurkClient *ns*)
