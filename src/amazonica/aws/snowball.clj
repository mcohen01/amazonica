(ns amazonica.aws.snowball
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.snowball AmazonSnowballClient]))

(amz/set-client AmazonSnowballClient *ns*)