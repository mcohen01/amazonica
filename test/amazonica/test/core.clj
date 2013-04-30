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
  (:use [clojure.test]
        [clojure.pprint]
        [clojure.java.shell]
        [amazonica.core]
        [amazonica.aws.autoscaling      :exclude (delete-tags
                                                  describe-tags
                                                  get-service-abbreviation)]
        [amazonica.aws.elasticache      :exclude (describe-events )]
        [amazonica.aws.elasticbeanstalk :exclude (describe-events)]
        [amazonica.aws.rds              :exclude (describe-engine-default-parameters)]
        [amazonica.aws.redshift         :exclude (describe-events)]
        [amazonica.aws.simpledb         :exclude (create-domain
                                                  delete-domain)]
        [amazonica.aws.sns              :exclude (add-permission
                                                  remove-permission)]
        [amazonica.aws.storagegateway   :exclude (create-snapshot
                                                  delete-volume)]
        [amazonica.aws.glacier          :exclude (abort-multipart-upload
                                                  complete-multipart-upload
                                                  initiate-multipart-upload
                                                  list-multipart-uploads
                                                  list-parts)]
        [amazonica.aws.opsworks         :exclude (create-stack
                                                  delete-stack
                                                  describe-instances
                                                  describe-stacks
                                                  describe-volumes
                                                  update-stack)]
        [amazonica.aws
          cloudformation
          cloudfront
          cloudsearch
          cloudwatch
          datapipeline
          directconnect
          dynamodb
          ec2
          elasticloadbalancing
          elasticmapreduce
          identitymanagement
          route53
          s3
          simpleemail
          sqs]))

;; just load the namespace to confirm we don't have
;; any naming conflicts with interned Vars