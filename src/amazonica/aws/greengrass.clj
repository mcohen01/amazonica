(ns amazonica.aws.greengrass
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.greengrass AWSGreengrassClient]))

(amz/set-client AWSGreengrassClient *ns*)
