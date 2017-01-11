(ns amazonica.aws.stepfunctions
  (:use [amazonica.core :only (kw->str parse-args)]
        [clojure.walk]
        [robert.hooke :only (add-hook)])
  (:import [com.amazonaws.services.stepfunctions AWSStepFunctionsClient]))

(amazonica.core/set-client AWSStepFunctionsClient *ns*)

(defn get-activity-task-result [arn]
  (get-activity-task {:activityArn arn})
  )

(defn mark-task-success [output token]
  (send-task-success {:output output
                      :taskToken token})
  )

(defn mark-task-failure [token]
  (send-task-failure {:taskToken token})
  )

(defn send-heartbeat [token]
  (send-task-heartbeat {:taskToken token})
  )

(defn start-state-machine [input state-machine-arn]
  (start-execution {:stateMachineArn state-machine-arn
                    :input input})
  )