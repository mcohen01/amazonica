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
             iot
             iotdata
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

(deftest camel->keyword2-tests
  (let [c->k #'c/camel->keyword2]
    (is (= :abc-def (c->k "abcDef")))
    (is (= :abc-def (c->k "AbcDef")))

    (is (= :abc-def (c->k "ABCDef")))))

(def changed-camel->keyword-vars
  "These are vars that were converted to kebab-case in a way that didn't deal
  with acronyms (any sequence of capital letters, but also some things like
  iSCSI and OpenID) correctly. This exists so that we can verify that we didn't
  accidentally break backwards compatibility.

  This is a collection of strings and symbols and not just vars so we can verify
  other information about the two resulting vars; and that you get a nice test
  failure instead of an ugly compilation error when it fails."
  {"identitymanagement" [['list-mfadevices
                          'list-mfa-devices]
                         ['list-open-idconnect-providers
                          'list-openid-connect-providers]
                         ['list-samlproviders
                          'list-saml-providers]
                         ['list-sshpublic-keys
                          'list-ssh-public-keys]
                         ['list-virtual-mfadevices
                          'list-virtual-mfa-devices]]
   "iot" [['describe-cacertificate
           'describe-ca-certificate]
          ['list-cacertificates
           'list-ca-certificates]]
   "rds" [['describe-dbcluster-parameter-groups
           'describe-db-cluster-parameter-groups]
          ['describe-dbcluster-snapshots
           'describe-db-cluster-snapshots]
          ['describe-dbclusters
           'describe-db-clusters]
          ['describe-dbengine-versions
           'describe-db-engine-versions]
          ['describe-dbinstances
           'describe-db-instances]
          ['describe-dbparameter-groups
           'describe-db-parameter-groups]
          ['describe-dbsecurity-groups
           'describe-db-security-groups]
          ['describe-dbsnapshots
           'describe-db-snapshots]
          ['describe-dbsubnet-groups
           'describe-db-subnet-groups]]
   "storagegateway" [['describe-storedi-scsivolumes
                      'describe-stored-iscsi-volumes]
                     ['describe-cachedi-scsivolumes
                      'describe-cached-iscsivolumes]]})
