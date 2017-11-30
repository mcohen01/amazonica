(ns amazonica.aws.dax
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.dax AmazonDaxClient]))

(amz/set-client AmazonDaxClient *ns*)
