(ns amazonica.aws.ec2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.ec2.AmazonEC2Client
           com.amazonaws.services.ec2.model.Tag))

(defn- key->str [kw]
  (if (keyword? kw) 
      (name kw)
      (str kw)))

(amz/register-coercions
  Tag
  (fn [value]
    (let [tag (Tag.)]
      (if (map? value)
          (if (= 1 (count value))
              (do (.setKey tag (-> value keys first key->str))
                  (.setValue tag (-> value vals first str)))
              (do (.setKey tag (:key value))
                  (.setValue tag (:value value)))))
      (if (sequential? value)
          (do (.setKey tag (-> value first key->str))
              (.setValue tag (-> value second str))))
      tag)))
              

(amz/set-client AmazonEC2Client *ns*)