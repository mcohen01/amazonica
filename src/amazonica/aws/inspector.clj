(ns amazonica.aws.inspector
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.inspector AmazonInspectorClient]))

(amz/set-client AmazonInspectorClient *ns*)