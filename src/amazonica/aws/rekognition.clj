(ns amazonica.aws.rekognition
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.rekognition AmazonRekognitionClient]))

(amz/set-client AmazonRekognitionClient *ns*)