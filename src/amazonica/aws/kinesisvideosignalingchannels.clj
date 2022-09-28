(ns amazonica.aws.kinesisvideosignalingchannels
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kinesisvideosignalingchannels.AmazonKinesisVideoSignalingChannelsClient))

(amz/set-client AmazonKinesisVideoSignalingChannelsClient *ns*)
