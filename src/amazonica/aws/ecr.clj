(ns amazonica.aws.ecr
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.ecr AmazonECRClient]))

(amz/set-client AmazonECRClient *ns*)
