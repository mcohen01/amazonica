(ns amazonica.aws.fsx
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.fsx.AmazonFSxClient))

(amz/set-client AmazonFSxClient *ns*)