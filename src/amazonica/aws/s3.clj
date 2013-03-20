(ns amazonica.aws.s3
  (:import com.amazonaws.services.s3.AmazonS3Client))

(amazonica.core/set-client AmazonS3Client *ns*)