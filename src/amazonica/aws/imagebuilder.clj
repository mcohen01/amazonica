(ns amazonica.aws.imagebuilder
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.imagebuilder.AWSimagebuilderClient))

(amz/set-client AWSimagebuilderClient *ns*)
