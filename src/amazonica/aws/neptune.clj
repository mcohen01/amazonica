(ns amazonica.aws.neptune
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.neptune AmazonNeptuneClient]))

(amz/set-client AmazonNeptuneClient *ns*)
