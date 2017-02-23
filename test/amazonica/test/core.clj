(ns amazonica.test.core
  (:require [clojure.test :refer [deftest is]]
            [amazonica.core :as c]
            [amazonica.aws
             autoscaling
             cloudformation
             cloudfront
             cloudsearchv2
             cloudsearchdomain
             cloudtrail
             cloudwatch
             codedeploy
             cognitoidentity
             cognitosync
             datapipeline
             directconnect
             dynamodbv2
             ec2
             ecs
             elasticache
             elasticbeanstalk
             elasticloadbalancing
             elasticmapreduce
             elastictranscoder
             glacier
             identitymanagement
             kinesis
             kms
             lambda
             logs
             opsworks
             rds
             redshift
             route53
             s3
             securitytoken
             simpledb
             simpleemail
             simpleworkflow
             sns
             sqs
             storagegateway
             ]))

(deftest camel->keyword-tests
  (let [c->k #'c/camel->keyword]
    (is (= :abc-def (c->k "abcDef")))
    (is (= :abc-def (c->k "AbcDef")))

    (is (= :abcdef (c->k "ABCDef")))))
