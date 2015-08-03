(ns amazonica.aws.codecommit
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.codecommit AWSCodeCommitClient]))

(amz/set-client AWSCodeCommitClient *ns*)
