(ns amazonica.aws.codebuild
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.codebuild AWSCodeBuildClient]))

(amz/set-client AWSCodeBuildClient *ns*)
