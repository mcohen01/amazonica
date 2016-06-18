(ns amazonica.test.simplesystemsmanagement
  (import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest)
  (:use [clojure.test]
        [amazonica.aws.simplesystemsmanagement]))

(deftest simplesystemsmanagement []

  ;; test for marshalling map values 
  ;; see https://github.com/mcohen01/amazonica/issues/219
  (let [pojo (SendCommandRequest.)]
    (amazonica.core/set-fields pojo {:parameters {"k" ["one" "two"]}})
    (is (= {"k" ["one" "two"]}
           (.getParameters pojo))))

)