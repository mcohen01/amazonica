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
        [amazonica.aws.autoscaling      :exclude [delete-tags
                                                  describe-tags
                                                  get-service-abbreviation]]
        [amazonica.aws.datapipeline     :exclude [adjust-client-configuration]]
        [amazonica.aws.dynamodbv2       :exclude [adjust-client-configuration]]
        [amazonica.aws.elasticache      :exclude [copy-snapshot
                                                  create-snapshot
                                                  delete-snapshot
                                                  describe-events
                                                  describe-snapshots]]
        [amazonica.aws.elasticbeanstalk :exclude [describe-events]]
        [amazonica.aws.rds              :exclude [describe-engine-default-parameters
                                                  describe-event-categories
                                                  create-event-subscription
                                                  delete-event-subscription
                                                  describe-event-subscriptions
                                                  list-tags-for-resource
                                                  modify-event-subscription]]
        [amazonica.aws.redshift         :exclude [create-tags
                                                  delete-tags
                                                  describe-events
                                                  describe-tags]]
        [amazonica.aws.simpledb         :exclude [create-domain
                                                  delete-domain]]
        [amazonica.aws.sns              :exclude [add-permission
                                                  remove-permission]]
        [amazonica.aws.storagegateway   :exclude [adjust-client-configuration
                                                  create-snapshot
                                                  delete-volume]]
        [amazonica.aws.glacier          :exclude [abort-multipart-upload
                                                  adjust-client-configuration
                                                  complete-multipart-upload
                                                  initiate-multipart-upload
                                                  list-multipart-uploads
                                                  list-parts]]
        [amazonica.aws.opsworks         :exclude [adjust-client-configuration
                                                  create-stack
                                                  delete-stack
                                                  describe-instances
                                                  describe-stacks
                                                  describe-volumes
                                                  update-stack]]
        [amazonica.aws.elasticmapreduce :exclude [add-tags
                                                  adjust-client-configuration
                                                  remove-tags]]
        [amazonica.aws.ec2              :exclude [describe-tags]]
        [amazonica.aws
          cloudformation
          cloudfront
          cloudsearch
          cloudwatch
          dynamodbv2
          elasticloadbalancing
          identitymanagement
          route53
          s3
          securitytoken
          simpleemail
          sqs]))

; ;; just load the namespace to confirm we don't have
; ;; any naming conflicts with interned Vars