(ns amazonica.aws.eks
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.eks AmazonEKSClient]))

(amz/set-client AmazonEKSClient *ns*)
