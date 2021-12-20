(ns amazonica.aws.timestreamquery
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.timestreamquery AmazonTimestreamQueryClient]))

(amz/set-client AmazonTimestreamQueryClient *ns*)
