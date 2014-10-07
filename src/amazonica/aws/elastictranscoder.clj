(ns amazonica.aws.elastictranscoder
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClient))

(amz/set-client AmazonElasticTranscoderClient *ns*)
