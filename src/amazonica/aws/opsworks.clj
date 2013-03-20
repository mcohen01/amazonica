(ns amazonica.aws.opsworks
  (:import com.amazonaws.services.opsworks.AWSOpsWorksClient))

(amazonica.core/set-client AWSOpsWorksClient *ns*)