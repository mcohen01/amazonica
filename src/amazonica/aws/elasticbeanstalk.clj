(ns amazonica.aws.elasticbeanstalk
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient))

(amz/set-client AWSElasticBeanstalkClient *ns*)