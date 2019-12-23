(ns amazonica.aws.quicksight
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.quicksight.AmazonQuickSightClient))

(amz/set-client AmazonQuickSightClient *ns*)

