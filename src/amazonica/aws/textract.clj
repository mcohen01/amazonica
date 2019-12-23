(ns amazonica.aws.textract
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.textract.AmazonTextractClient))

(amz/set-client AmazonTextractClient *ns*)
