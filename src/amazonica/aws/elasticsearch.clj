(ns amazonica.aws.elasticsearch
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticsearch.AWSElasticsearchClient))

(amz/set-client AWSElasticsearchClient *ns*)
