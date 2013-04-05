(ns amazonica.aws.glacier
  (:use [amazonica.core :only (set-client to-file)]
        [robert.hooke :only (add-hook)])
  (:import [com.amazonaws.services.glacier
              AmazonGlacierClient]
           [amazonica TreeHash]
           [java.io
              BufferedInputStream
              FileInputStream]))

(set-client AmazonGlacierClient *ns*)

(defn hasher
  [f cred & args]
  (let [m    (apply hash-map args)
        file (to-file (:body m))
        mm (merge-with
             #(do {% %2} %2)
             m
             {:content-length (.length file)
              :checksum (TreeHash/toHex (TreeHash/computeSHA256TreeHash file))
              :body (-> file (FileInputStream.) (BufferedInputStream.))})
        rval (interleave (keys mm) (vals mm))]
    (apply (partial f cred) rval)))

(add-hook #'upload-archive #'hasher)