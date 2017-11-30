(ns amazonica.aws.codestar
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.codestar AWSCodeStarClient]))

(amz/set-client AWSCodeStarClient *ns*)
