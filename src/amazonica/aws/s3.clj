(ns amazonica.aws.s3
  (:use [amazonica.core :only (IMarshall coerce-value marshall register-coercions 
                               to-date kw->str)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.s3
              AmazonS3Client]
           [com.amazonaws.services.s3.model
              AccessControlList
              CanonicalGrantee
              CORSRule
              CORSRule$AllowedMethods
              DeleteObjectsRequest$KeyVersion
              EmailAddressGrantee
              Grant
              Grantee
              GroupGrantee
              ObjectMetadata
              Permission
              S3Object]))

(def email-pattern #"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$;")

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
     :object-metadata   (marshall (.getObjectMetadata obj))}))

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
      acl))
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