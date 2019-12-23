(ns amazonica.aws.connectparticipant
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient))

(amz/set-client AmazonConnectParticipantClient *ns*)
