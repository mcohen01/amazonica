(ns amazonica.aws.appstream
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.appstream AmazonAppStreamClient]))

(amz/set-client AmazonAppStreamClient *ns*)