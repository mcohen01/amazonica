(ns amazonica.aws.glue
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.glue AWSGlueClient]))

(amz/set-client AWSGlueClient *ns*)
