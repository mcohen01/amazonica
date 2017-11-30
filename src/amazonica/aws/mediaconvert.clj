(ns amazonica.aws.mediaconvert
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mediaconvert AWSMediaConvertClient]))

(amz/set-client AWSMediaConvertClient *ns*)
