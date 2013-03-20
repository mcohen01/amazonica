(ns amazonica.aws.elasticbeanstalk
  (:import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient))

(amazonica.core/set-client AWSElasticBeanstalkClient *ns*)