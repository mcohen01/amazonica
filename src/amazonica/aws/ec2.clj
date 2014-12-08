(ns amazonica.aws.ec2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.ec2.AmazonEC2Client
           [com.amazonaws.services.ec2.model PortRange Tag]))

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
      tag))
  PortRange
  (fn [value]
    (let [ports (PortRange.)]
      (cond
        (string? value)
        (do
          (.setFrom ports (Integer. value))
          (.setTo ports (Integer. value)))
        (number? value)
        (do
          (.setFrom ports (int value))
          (.setTo ports (int value)))
        (and (map? value) (:from value) (:to value))
        (do
          (.setFrom ports (int (:from value)))
          (.setTo ports (int (:to value))))
        (and (sequential? value) (or (= 1 (count value)) (= 2 (count value))))
        (do
          (.setFrom ports (int (first value)))
          (.setTo ports (int (last value))))
        :default
        (throw (IllegalArgumentException.
                 (format "Don't know how to create PortRange from %s" value))))
      ports)))

(amz/set-client AmazonEC2Client *ns*)