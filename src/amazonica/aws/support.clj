(ns amazonica.aws.support
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.support.AWSSupportClient))

(amz/set-client AWSSupportClient *ns*)
