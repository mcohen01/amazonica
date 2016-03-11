(ns amazonica.aws.cloudwatchevents
  (:require [amazonica.core :as amz])
  (:import (com.amazonaws.services.cloudwatchevents AmazonCloudWatchEventsClient)))

(amz/set-client AmazonCloudWatchEventsClient *ns*)
