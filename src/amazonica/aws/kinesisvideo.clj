(ns amazonica.aws.kinesisvideo
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClient))

(amz/set-client AmazonKinesisVideoClient *ns*)
