(ns amazonica.aws.lambda
  (:use [robert.hooke :only (add-hook)])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.lambda.AWSLambdaClient))

(amz/set-client AWSLambdaClient *ns*)

(defn byte-buffer-zip-file [function-name body]
  (let [baos (java.io.ByteArrayOutputStream.)
        zos  (java.util.zip.ZipOutputStream. baos)]
    (.putNextEntry zos (java.util.zip.ZipEntry. (str function-name ".js")))
    (.write zos (.getBytes body))
    (.closeEntry zos)
    (.finish zos)
    (java.nio.ByteBuffer/wrap (.toByteArray baos))))

(defn function-name [node-fn]
  (-> (re-find #"exports\..+=" node-fn)
      (.replaceFirst "exports." "")
      (.replaceFirst "=" "")
      .trim))

(defn- parse-function
  [f cred & args]
  (let [arg-map (amz/parse-args cred args)
        attrs   (if (even? (count (:args arg-map)))
                    (apply hash-map (:args arg-map))
                    (first (:args arg-map)))
        attrs   (merge {:timeout 10
                        :memory-size 256
                        :mode "event"
                        :runtime "nodejs"
                        :description "uploaded via amazonica"} attrs)
        fn-name (or (:function-name attrs) (function-name (:function attrs)))
        attrs   (if (:function attrs)
                    (merge {:function-name fn-name
                            :code {:zip-file (byte-buffer-zip-file fn-name (:function attrs))}
                            :handler (str fn-name "." fn-name)} attrs)
                    attrs)
        func    (if (contains? arg-map :cred)
                    (partial f cred)
                    f)]
    (func (dissoc attrs :function))))


(add-hook #'create-function #'parse-function)