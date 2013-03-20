(ns amazonica.aws.glacier
  (:import com.amazonaws.services.glacier.AmazonGlacierClient))

(amazonica.core/set-client AmazonGlacierClient *ns*)