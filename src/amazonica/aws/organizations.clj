(ns amazonica.aws.organizations
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.organizations AWSOrganizationsClient]))

(amz/set-client AWSOrganizationsClient *ns*)
