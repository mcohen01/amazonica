(ns amazonica.aws.codepipeline
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.codepipeline AWSCodePipelineClient]))

(amz/set-client AWSCodePipelineClient *ns*)
