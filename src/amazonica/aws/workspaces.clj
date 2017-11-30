(ns amazonica.aws.workspaces
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.workspaces AmazonWorkspacesClient]))

(amz/set-client AmazonWorkspacesClient *ns*)
