(ns amazonica.aws.timestreamwrite
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.timestreamwrite AmazonTimestreamWriteClient]))

(amz/set-client AmazonTimestreamWriteClient *ns*)
