(ns amazonica.aws.cloud9
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloud9.AWSCloud9Client))

(amz/set-client AWSCloud9Client *ns*)
