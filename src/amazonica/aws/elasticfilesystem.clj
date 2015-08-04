(ns amazonica.aws.elasticfilesystem
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.elasticfilesystem AmazonElasticFileSystemClient]))

(amz/set-client AmazonElasticFileSystemClient *ns*)
