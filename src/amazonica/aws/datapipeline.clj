(ns amazonica.aws.datapipeline
  (:import com.amazonaws.services.datapipeline.DataPipelineClient))

(amazonica.core/set-client DataPipelineClient *ns*)