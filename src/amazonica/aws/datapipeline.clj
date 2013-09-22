(ns amazonica.aws.datapipeline
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.datapipeline.DataPipelineClient))

(amz/set-client DataPipelineClient *ns*)