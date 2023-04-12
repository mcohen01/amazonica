(ns amazonica.test.core
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
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
             machinelearning
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
             ]
            [clojure.string :as string]))

(deftest camel->keyword-tests
  (let [c->k #'c/camel->keyword]
    (is (= :abc-def (c->k "abcDef")))
    (is (= :abc-def (c->k "AbcDef")))

    (is (= :abcdef (c->k "ABCDef")))

    (is (= :describe-storedi-scsivolumes (c->k "DescribeStorediSCSIVolumes")))
    (is (= :list-open-idconnect-providers (c->k "ListOpenIDConnectProviders")))))

(deftest camel->keyword2-tests
  (let [c->k #'c/camel->keyword2]
    (is (= :abc-def (c->k "abcDef")))
    (is (= :abc-def (c->k "AbcDef")))

    (is (= :abc-def (c->k "ABCDef")))

    (is (= :describe-stored-iscsi-volumes (c->k "DescribeStorediSCSIVolumes")))
    (is (= :list-openid-connect-providers (c->k "ListOpenIDConnectProviders")))))

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
                      'describe-cached-iscsi-volumes]
                     ['describe-nfsfile-shares
                      'describe-nfs-file-shares]]
   "machinelearning" [['describe-mlmodels
                       'describe-ml-models]]})

(defn relevant-keys [m]
  (->> m
       keys
       (remove #{:amazonica/source :amazonica/method-name})))

(deftest camel->keyword-changes-tests
  (testing "See https://github.com/mcohen01/amazonica/issues/256"
    (doseq [[service changed-fn-names] changed-camel->keyword-vars
            [old-name new-name] changed-fn-names
            :let [service-ns-sym (symbol (str "amazonica.aws." service))
                  old (ns-resolve service-ns-sym old-name)
                  new (ns-resolve service-ns-sym new-name)]]
      (is (not= old-name new-name))
      (is (some? old) (str "missing old var: " service " " old-name))
      (is (some? new) (str "missing new var: " service " " new-name))

      (is (= #{:arglists
               :ns
               :name
               :amazonica/client
               :amazonica/methods
               :amazonica/deprecated-in-favor-of}
             (set (relevant-keys (meta old)))))
      (is (= #{:arglists
               :ns
               :name
               :amazonica/client
               :amazonica/methods}
             (set (relevant-keys (meta new)))))
      (is (= new
             (get (meta old) :amazonica/deprecated-in-favor-of)))))

  ;; Make sure we don't accidentally attach new metadata keys to old, untouched
  ;; vars:
  (let [unrelated-var #'amazonica.aws.ec2/describe-addresses]
    (is (= (set (relevant-keys (meta unrelated-var)))
           #{:arglists
             :name
             :ns
             :amazonica/client
             :amazonica/methods}))))

(def non-auto-generated-public-vars
  "The set of public vars in this project that weren't generated by reflection."
  '#{amazonica.aws.cloudsearchdomain/set-endpoint
     amazonica.aws.dynamodbv2/marshall-allow-empty-maps
     amazonica.aws.glacier/tree-hash
     amazonica.aws.kinesis/marshall
     amazonica.aws.kinesis/unwrap
     amazonica.aws.kinesis/worker
     amazonica.aws.kinesis/worker!
     amazonica.aws.kinesisfirehose/*list-delivery-streams-default-limit*
     amazonica.aws.kinesisfirehose/->bytes
     amazonica.aws.kinesisfirehose/maybe-update-in
     amazonica.aws.lambda/byte-buffer-zip-file
     amazonica.aws.lambda/function-name
     amazonica.aws.s3/email-pattern
     amazonica.aws.s3transfer/add-listener
     amazonica.aws.s3transfer/transfer
     amazonica.aws.s3transfer/wait
     amazonica.aws.sqs/arn
     amazonica.aws.sqs/assign-dead-letter-queue
     amazonica.aws.sqs/find-queue
     amazonica.aws.stepfunctions/get-activity-task-result
     amazonica.aws.stepfunctions/mark-task-failure
     amazonica.aws.stepfunctions/mark-task-success
     amazonica.aws.stepfunctions/send-heartbeat
     amazonica.aws.stepfunctions/start-state-machine})

(def has-sources?
  (let [v (System/getProperty "amazonica.internal.test.using-sources")]
    (assert (#{"true" "false"} v))
    (read-string v)))

(defn var->sym [var-ref]
  (let [{var-ns :ns, var-name :name} (meta var-ref)]
    (symbol (str var-ns)
            (str var-name))))

(when has-sources?
  (deftest source-metadata-tests
    (testing ":amazonica/source and :amazonica/method-name metadata is added"
      (let [auto-generated-public-vars
            (->> (all-ns)
                 (filter (comp (fn [s]
                                 (and (string/starts-with? s "amazonica.")
                                      (not (string/starts-with? s "amazonica.core"))
                                      (not (string/starts-with? s "amazonica.test"))))
                               str
                               ns-name))
                 (mapcat (comp vals ns-publics))
                 (remove (comp #{'show-functions 'client-class} :name meta))
                 (remove (comp non-auto-generated-public-vars var->sym)))]
        (assert (> (count auto-generated-public-vars)
                   2000)
                (count auto-generated-public-vars))
        (doseq [public-var auto-generated-public-vars
                :let [{source      :amazonica/source
                       method-name :amazonica/method-name
                       :keys       [arglists]} (meta public-var)]]

          (assert (is (string/starts-with? source "jar:file:")))
          (is (-> source slurp seq)
              "The referenced file exists")

          (is (not (string/blank? method-name)))

          (doseq [[ampersand {arglist-keys :keys
                              arglist-as   :as
                              :as          arglist-map-or-vec} :as arglist] arglists]
            (when (seq arglist)
              (when (map? arglist-map-or-vec)
                (is (= (sort arglist-keys)
                       arglist-keys)
                    "The arglist :keys are sorted"))

              (if (= ampersand '&)
                (is (or (symbol? arglist-as)
                        (and (vector? arglist-map-or-vec)
                             (do
                               (doseq [item arglist-map-or-vec]
                                 (is (symbol? item)
                                     (pr-str arglist-map-or-vec)))
                               true)))
                    (pr-str arglist))
                (do
                  (is (vector? arglist))
                  (doseq [item arglist]
                    (is (symbol? item))))))))))))

(deftest ^:contains-should-not-consider-nil-values
  validate-contains?-doesnt-allow-nil-values
  (testing "map-contains? shouldn't consider nil values"
    (let [sample-value {:A "some-value" :B nil}]
      (is (true? (c/map-contains? sample-value :A)))
      (is (false? (c/map-contains? sample-value :B)))
      (is (true? (c/map-contains? sample-value :A :B))))))
