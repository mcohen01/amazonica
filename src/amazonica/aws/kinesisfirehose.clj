(ns amazonica.aws.kinesisfirehose
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient))

(amz/set-client AmazonKinesisFirehoseClient *ns*)