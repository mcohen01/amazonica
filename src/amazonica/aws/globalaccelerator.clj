(ns amazonica.aws.globalaccelerator
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.globalaccelerator.AWSGlobalAcceleratorClient))

(amz/set-client AWSGlobalAcceleratorClient *ns*)
