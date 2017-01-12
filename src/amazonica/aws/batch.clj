(ns amazonica.aws.batch
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.batch AWSBatchClient]))

(amz/set-client AWSBatchClient *ns*)