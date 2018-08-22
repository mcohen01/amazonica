(ns amazonica.aws.macie
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.macie AmazonMacieClient]))

(amz/set-client AmazonMacieClient *ns*)
