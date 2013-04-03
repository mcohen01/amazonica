(ns amazonica.aws.s3
  (:use [amazonica.core :only (IMarshall coerce-value marshall register-coercions)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.s3
              AmazonS3Client]
           [com.amazonaws.services.s3.model
              AccessControlList
              CanonicalGrantee
              EmailAddressGrantee
              Grant
              Grantee
              GroupGrantee
              Permission
              S3Object]))

(def email-pattern #"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$;")

(extend-protocol IMarshall
  S3Object
  (marshall [obj]
    {:bucket-name       (.getBucketName obj)
     :key               (.getKey obj)
     :input-stream      (.getObjectContent obj)
     :object-content    (.getObjectContent obj)
     :redirect-location (.getRedirectLocation obj)
     :object-metadata   (marshall
                          (.getObjectMetadata obj))}))

(register-coercions  
  AccessControlList
  (fn [col]
    (let [acl (AccessControlList.)]
      (if-let [revoked (:revoke-all-permissions col)]
        (.revokeAllPermissions acl (coerce-value revoked Grantee)))
      (if-let [grant-all (:grant-all col)]
        (.grantAllPermissions acl
          (into-array
            (fmap #(coerce-value % Grant)
              grant-all))))
      (if-let [grant (:grant-permission col)]
        (.grantPermission
          acl
          (coerce-value (first grant) Grantee)
          (coerce-value (second grant) Permission)
      acl))))
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
    (Permission/valueOf value)))

(amazonica.core/set-client AmazonS3Client *ns*)