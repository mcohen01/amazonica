(ns amazonica.aws.s3
  (:use [amazonica.core :only (IMarshall coerce-value marshall register-coercions 
                               set-fields to-date kw->str)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.s3
              AmazonS3Client]
           [com.amazonaws.regions Region Regions]
           [com.amazonaws.services.s3.model
              AccessControlList
              BucketNotificationConfiguration
              BucketTaggingConfiguration
              CanonicalGrantee
              CORSRule
              CORSRule$AllowedMethods
              DeleteObjectsRequest$KeyVersion
              EmailAddressGrantee
              Filter
              FilterRule
              Grant
              Grantee
              GroupGrantee
              LambdaConfiguration
              ObjectMetadata
              Owner
              Permission
              QueueConfiguration
              S3KeyFilter
              S3Object
              TagSet
              TopicConfiguration]))

(def email-pattern #"^[_A-Za-z0-9-\\+]+(?:\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(?:\.[A-Za-z0-9]+)*(?:\.[A-Za-z]{2,})$")

(defn- notification-configuration-instance
  [value]
  (let [ks (->> value keys (reduce str))]
    (cond
      (.contains ks "queue")
      (QueueConfiguration. (or (:queue-ARN value) (:queue value))
                           (into-array (:events value)))
      (.contains ks "topic")
      (TopicConfiguration. (or (:topic-ARN value) (:topic value))
                           (into-array (:events value)))
      (.contains ks "function")
      (LambdaConfiguration. (or (:function-ARN value) (:function value))
                            (into-array (:events value))))))

(defn- as-bucket-notification-config
  [value]
  (let [bnc (BucketNotificationConfiguration.)]
    (.setConfigurations bnc
      (reduce
        #(assoc %
           (name (first %2))
           (set-fields (notification-configuration-instance (last %2))
                       (last %2)))
        {}
        (:configurations value)))
    bnc))

(defn- as-filter [value]
  (let [fltr (Filter.)
        s3ft (S3KeyFilter.)
        _ (.setS3KeyFilter fltr s3ft)
        f (fn [pair]
            (let [fr (FilterRule.)]
              (if (map? pair)
                  (do (.setName fr (-> pair keys first name))
                      (.setValue fr (-> pair vals last)))
                  (do (.setName fr (first pair))
                      (.setValue fr (last pair))))
              fr))]
    (.setFilterRules s3ft (map f value))
    fltr))

(defn- set-account-owner [acl]
  (let [s3ns (find-ns (symbol "amazonica.aws.s3"))
        sym  (symbol "get-s3account-owner")
        own  (ns-resolve s3ns sym)]
    (try
      (.setOwner acl (coerce-value (marshall (own)) Owner))
      (catch Throwable e
        (println "[WARN] Unable to set account owner for ACL: "
                 (.getMessage e))))))


(extend-protocol IMarshall
  CORSRule$AllowedMethods
  (marshall [obj]
    (.toString obj))
  ObjectMetadata
  (marshall [obj]
    {:cache-control           (.getCacheControl obj)
     :content-disposition     (.getContentDisposition obj)
     :content-encoding        (.getContentEncoding obj)
     :content-length          (.getContentLength obj)
     :content-md5             (.getContentMD5 obj)
     :content-type            (.getContentType obj)
     :etag                    (.getETag obj)
     :expiration-time         (.getExpirationTime obj)
     :expiration-time-rule-id (.getExpirationTimeRuleId obj)
     :http-expires-date       (marshall (.getHttpExpiresDate obj))
     :instance-length         (.getInstanceLength obj)
     :last-modified           (marshall (.getLastModified obj))
     :ongoing-restore         (marshall (.getOngoingRestore obj))
     ;; :raw-metadata            (.getRawMetadata obj)
     :restore-expiration-time (marshall (.getRestoreExpirationTime obj))
     :server-side-encryption  (.getServerSideEncryption obj)
     :user-metadata           (marshall (.getUserMetadata obj))
     :version-id              (.getVersionId obj)})
  S3Object
  (marshall [obj]
    {:bucket-name       (.getBucketName obj)
     :key               (.getKey obj)
     :input-stream      (.getObjectContent obj)
     :object-content    (.getObjectContent obj)
     :redirect-location (.getRedirectLocation obj)
     :object-metadata   (marshall (.getObjectMetadata obj))})
  BucketTaggingConfiguration
  (marshall [obj]
    {:tag-sets (map marshall (.getAllTagSets obj))})
  TagSet
  (marshall [obj]
    {:tags (.getAllTags obj)}))

(register-coercions
  ObjectMetadata
  (fn [col]
    (let [om (ObjectMetadata.)]
      (when-let [cc (:cache-control col)]
        (.setCacheControl om cc))
      (when-let [cd (:content-disposition col)]
        (.setContentDisposition om cd))
      (when-let [ce (:content-encoding col)]
        (.setContentEncoding om ce))
      (when-let [cl (:content-length col)]
        (.setContentLength om cl))
      (when-let [cm (:content-md5 col)]
        (.setContentMD5 om cm))
      (when-let [ct (:content-type col)]
        (.setContentType om ct))
      (when-let [et (:expiration-time col)]
        (.setExpirationTime om (to-date et)))
      (when-let [id (:expiration-time-rule-id col)]
        (.setExpirationTimeRuleId om id))
      (when-let [he (:http-expires-date col)]
        (.setHttpExpiresDate om he))
      (when-let [rt (:restore-expiration-time col)]
        (.setRestoreExpirationTime om (to-date rt)))
      (when-let [sse (:server-side-encryption col)]
        (.setServerSideEncryption om sse))
      (when-let [metadata (:user-metadata col)]
        (doseq [[k v] metadata]
          (.addUserMetadata om
            (kw->str k)
            (str v))))
      om))
  AccessControlList
  (fn [col]
    (let [acl (AccessControlList.)]
          ;; get-s3account-owner is not interned until runtime
      (if-let [revoked (:revoke-all-permissions col)]
        (.revokeAllPermissions acl
          (coerce-value revoked Grantee)))
      (if-let [grant-all (:grant-all col)]
        (.grantAllPermissions acl
          (into-array
            (fmap #(coerce-value % Grant)
              grant-all))))
      (if-let [grant (:grant-permission col)]
        (.grantPermission
          acl
          (coerce-value (first grant) Grantee)
          (coerce-value (second grant) Permission)))
      ;; s3 complains about ACLs without owners (even though docs say internal)
      (if-let [o (:owner col)]
        (.setOwner acl (coerce-value o Owner))
        (set-account-owner acl))
      acl))
  BucketNotificationConfiguration
  (fn [value]
    (as-bucket-notification-config value))
  Filter
  (fn [value]
    (as-filter value))
  Grant
  (fn [value]
    (Grant.
      (coerce-value (first value) Grantee)
      (coerce-value (second value) Permission)))
  Grantee
  (fn [value]
    (cond
      (= "AllUsers" value)
      (GroupGrantee/valueOf value)
      (= "AuthenticatedUsers" value)
      (GroupGrantee/valueOf value)
      (= "LogDelivery" value)
      (GroupGrantee/valueOf value)
      (re-find email-pattern value)
      (EmailAddressGrantee. value)
      true
      (CanonicalGrantee. value)))
  Owner
  (fn [col]
    (Owner. (:id col) (:displayName col)))
  TagSet
  (fn [value]
    (->> value
         (reduce #(assoc % (name (first %2)) (last %2)) {})
         (TagSet.)))
  Region
  (fn [value]
    (Region/getRegion (Regions/fromName value)))
  Permission
  (fn [value]
    (Permission/valueOf value))
  DeleteObjectsRequest$KeyVersion
  (fn [value]
    (if (coll? value)
        (DeleteObjectsRequest$KeyVersion. (first value) (second value))
        (DeleteObjectsRequest$KeyVersion. value)))
  CORSRule
  (fn [col]
    (let [cors (CORSRule.)]
      (when-let [id (:id col)]
        (.setId cors id))
      (when-let [max-age-seconds (:max-age-seconds col)]
        (.setMaxAgeSeconds cors max-age-seconds))
      (when-let [allowed-headers (:allowed-headers col)]
        (.setAllowedHeaders cors (into-array String allowed-headers)))
      (when-let [allowed-origins (:allowed-origins col)]
        (.setAllowedOrigins cors (into-array String allowed-origins)))
      (when-let [exposed-headers (:exposed-headers col)]
        (.setExposedHeaders cors (into-array String exposed-headers)))
      (when-let [allowed-methods (:allowed-methods col)]
        (.setAllowedMethods cors
                            (into-array CORSRule$AllowedMethods
                    (fmap #(coerce-value % CORSRule$AllowedMethods)
                          allowed-methods))))
      cors)))

(amazonica.core/set-client AmazonS3Client *ns*)
