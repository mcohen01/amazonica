(ns amazonica.aws.groundstation
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.groundstation.AWSGroundStationClient))

(amz/set-client AWSGroundStationClient *ns*)
