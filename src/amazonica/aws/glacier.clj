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

(defn- file-hash->map
  [file]
  {:content-length (.length file)
   :checksum (-> file
                 TreeHash/computeSHA256TreeHash
                 TreeHash/toHex)
   :body (-> file
             (FileInputStream.) 
             (BufferedInputStream.))})

(defn tree-hash
  [f cred & args]
  (let [m    (apply hash-map args)
        file (to-file (:body m))        
        mm   (merge-with               
               (fn [_ e] e)
               m
               (file-hash->map file))
        rval (interleave (keys mm) (vals mm))]
    (apply (partial f cred) rval)))

(add-hook #'upload-archive #'tree-hash)