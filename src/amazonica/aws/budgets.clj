(ns amazonica.aws.budgets
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.budgets AWSBudgetsClient]))

(amz/set-client AWSBudgetsClient *ns*)
