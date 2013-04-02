(ns amazonica.aws.s3
  (:use [amazonica.core :only (coerce-value marshall IMarshall)])
  (:import [com.amazonaws.services.s3
              AmazonS3Client]
           [com.amazonaws.services.s3.model
              S3Object]))

(extend-protocol IMarshall
  S3Object
    (marshall [obj]
      {:bucket-name (.getBucketName obj)
       :key (.getKey obj)
       :input-stream (.getObjectContent obj)
       :object-content (.getObjectContent obj)
       :redirect-location (.getRedirectLocation obj)
       :object-metadata (marshall
                          (.getObjectMetadata obj))}))

(amazonica.core/set-client AmazonS3Client *ns*)