(ns amazonica.test.core
  (:import amazonica.TreeHash
           org.joda.time.DateTime
           java.io.BufferedInputStream
           java.io.File
           java.io.FileInputStream
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID)
  (:require [clojure.string :as str])
  (:require [clojure.test]
            [clojure.pprint]
            [clojure.java.shell]
            [amazonica.core]
            [amazonica.aws
             autoscaling
             cloudformation
             cloudfront
             cloudsearch
             cloudwatch
             datapipeline
             dynamodbv2
             ec2
             elasticache
             elasticbeanstalk
             elasticloadbalancing
             elasticmapreduce
             glacier
             identitymanagement
             opsworks
             rds
             redshift
             route53
             s3
             securitytoken
             simpledb
             simpleemail
             sns
             sqs
             storagegateway
             ]))

; ;; just load the namespace to confirm we don't have
; ;; any naming conflicts with interned Vars
