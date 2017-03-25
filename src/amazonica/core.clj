(ns amazonica.core
  "Amazon AWS functions."
  (:use [clojure.algo.generic.functor :only (fmap)])
  (:require [clojure.string :as str])
  (:import clojure.lang.Reflector
           [com.amazonaws
             AmazonServiceException
             ClientConfiguration]
           [com.amazonaws.auth
             AWSCredentials
             AWSCredentialsProvider
             BasicAWSCredentials
             BasicSessionCredentials
             DefaultAWSCredentialsProviderChain]
           com.amazonaws.auth.profile.ProfileCredentialsProvider
           [com.amazonaws.regions
             Region
             Regions]
           org.joda.time.DateTime
           org.joda.time.base.AbstractInstant
           java.io.File
           java.io.PrintWriter
           java.io.StringWriter
           java.lang.reflect.InvocationTargetException
           java.lang.reflect.ParameterizedType
           java.lang.reflect.Modifier
           java.math.BigDecimal
           java.math.BigInteger
           java.nio.ByteBuffer
           java.text.ParsePosition
           java.text.SimpleDateFormat
           java.util.Date))

(defonce ^:private credential (atom {}))

(defonce ^:private client-config (atom {}))

(def ^:dynamic ^:private *credentials* nil)

(def ^:dynamic ^:private *client-config* nil)

(def ^:dynamic ^:private *client-class* nil)

(def ^:private date-format (atom "yyyy-MM-dd"))

(def ^:private root-unwrapping (atom false))

(defn- invoke-constructor
  [class-name arg-vec]
  (Reflector/invokeConstructor
    (Class/forName class-name)
    (into-array Object arg-vec)))

(defn set-root-unwrapping!
  "Enables JSON-like root unwrapping of singly keyed
  top level maps.
    {:root {:key 'foo' :name 'bar'}}
  would become
    {:key 'foo' :name 'bar'}"
  [b]
  (reset! root-unwrapping b))

(defn set-date-format!
  "Sets the java.text.SimpleDateFormat pattern to use
  for transparent coercion of Strings passed as
  arguments where java.util.Dates are required by the
  AWS api."
  [df]
  (reset! date-format df))


(defn stack->string
  "Converts a Java stacktrace to String representation."
  [ex]
  (let [sw (StringWriter.)
        pw (PrintWriter. sw)
        _  (.printStackTrace ex pw)]
    (.toString sw)))

(defn ex->map
  "Converts a com.amazonaws.AmazonServiceException to a
  Clojure map with keys:
  :error-code
  :error-type
  :status-code
  :request-id
  :service-name
  :message
  :stack-trace"
  [e]
  {:error-code   (.getErrorCode e)
   :error-type   (.toString (.getErrorType e))
   :status-code  (.getStatusCode e)
   :request-id   (.getRequestId e)
   :service-name (.getServiceName e)
   :message      (.getMessage e)
   :stack-trace  (stack->string e)})

; Java methods on the AWS*Client class which won't be exposed
(def ^:private excluded
  #{:anonymous-invoke
    :do-invoke
    :invoke
    :init
    :set-endpoint
    :get-cached-response-metadata
    :get-service-abbreviation})
    ; addRequestHandler???


(defn keys->cred
  [access-key secret-key & [endpoint]]
  (let [credential {:access-key access-key
                    :secret-key secret-key}]
    (if-not (empty? endpoint)
      (merge credential {:endpoint endpoint})
      credential)))

(defn defcredential
  "Specify the AWS access key, secret key and optional
  endpoint to use on subsequent requests."
  ([cred]
   (reset! credential cred))
  ([access-key secret-key & [endpoint]]
   (defcredential (keys->cred access-key secret-key endpoint))))

(defn defclientconfig
  "Specify the default Client configuration to use on
  subsequent requests."
  [config]
  (reset! client-config config))

(defmacro with-credential
  "Per invocation binding of credentials for ad-hoc
  service calls using alternate user/password combos
  (and endpoints)."
  [cred & body]
  `(binding [*credentials*
             (let [cred# ~cred]
               (if (sequential? cred#)
                 (apply keys->cred cred#)
                 cred#))]
    (do ~@body)))

(defmacro with-client-config
  "Per invocation binding of client-config for ad-hoc
  service calls using alternate client configuration."
  [config & body]
  `(binding [*client-config* config]
     (do ~@body)))

(declare new-instance)

(defn- create-client
  [clazz credentials configuration]
  (if (every? nil? [credentials configuration])
    (new-instance (Class/forName clazz))
    ; TransferManager is the only client to date that doesn't accept AWSCredentialsProviders
    (if (= (.getSimpleName clazz) "TransferManager")
        (invoke-constructor
          "com.amazonaws.services.s3.transfer.TransferManager"
          [(create-client (Class/forName "com.amazonaws.services.s3.AmazonS3Client")
                          credentials
                          configuration)])
        (invoke-constructor (.getName clazz)
                            (->> [credentials configuration]
                                 (filter (comp not nil?))
                                 vec)))))

(defn get-credentials
  [credentials]
  (cond
    (or (instance? AWSCredentialsProvider credentials)
        (instance? AWSCredentials credentials))
    credentials
    (and (associative? credentials)
         (contains? credentials :session-token))
    (BasicSessionCredentials.
        (:access-key credentials)
        (:secret-key credentials)
        (:session-token credentials))
    (and (associative? credentials)
         (contains? credentials :access-key))
    (BasicAWSCredentials.
        (:access-key credentials)
        (:secret-key credentials))
    (and (associative? credentials)
         (contains? credentials :profile))
    (ProfileCredentialsProvider.
        (:profile credentials))
    (and (associative? credentials)
         (instance? AWSCredentialsProvider (:cred credentials)))
    (:cred credentials)
    :else
    (DefaultAWSCredentialsProviderChain.)))

(defn parse-args
  "Legacy support means credentials may or may not be passed
   as the first argument."
  [cred args]
  (if (and (instance? AWSCredentialsProvider (get-credentials cred))
           (not (instance? AWSCredentialsProvider cred))
           (nil? (:endpoint cred)))
    {:args (conj args cred)}
    {:args args :cred cred}))

(declare create-bean)

(defn- get-client-configuration
  [configuration]
  (when (associative? configuration)
    (create-bean ClientConfiguration configuration)))

(defn- set-endpoint!
  [client credentials]
  (when-let [endpoint (or (:endpoint credentials)
                          (System/getenv "AWS_DEFAULT_REGION"))]
    (if (contains? (fmap #(-> % str/upper-case (str/replace "_" ""))
                         (apply hash-set (seq (Regions/values))))
                   (-> (str/upper-case endpoint)
                       (str/replace "-" "")))
        (try
          (->> (-> (str/upper-case endpoint)
                   (str/replace "-" "_"))
               Regions/valueOf
               Region/getRegion
               (.setRegion client))
          (catch NoSuchMethodException e
            (println e)))
        (.setEndpoint client endpoint))))

(defn encryption-client*
  [encryption credentials configuration]
  (let [creds     (get-credentials credentials)
        config    (get-client-configuration configuration)
        key       (or (:secret-key encryption)
                      (:key-pair encryption))
        crypto    (invoke-constructor
                    "com.amazonaws.services.s3.model.CryptoConfiguration" [])
        em        (invoke-constructor
                    "com.amazonaws.services.s3.model.EncryptionMaterials"
                    [key])
        materials (invoke-constructor
                    "com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider"
                    [em])
        _         (if-let [provider (:provider encryption)]
                    (.withCryptoProvider crypto provider))
        client    (if config
                      (invoke-constructor
                        "com.amazonaws.services.s3.AmazonS3EncryptionClient"
                        [creds materials config crypto])
                      (invoke-constructor
                        "com.amazonaws.services.s3.AmazonS3EncryptionClient"
                        [creds materials crypto]))]
    (set-endpoint! client credentials)
    client))

(swap! client-config assoc :encryption-client-fn (memoize encryption-client*))

(defn amazon-client*
  [clazz credentials configuration]
  (let [aws-creds  (get-credentials credentials)
        aws-config (get-client-configuration configuration)
        client     (create-client clazz aws-creds aws-config)]
    (set-endpoint! client credentials)
    client))

(swap! client-config assoc :amazon-client-fn (memoize amazon-client*))

(defn- keyword-converter
  "Given something that tokenizes a string into parts, turn it into
  a :kebab-case-keyword."
  [separator-regex]
  (fn [s]
    (->> (str/split s separator-regex)
         (map str/lower-case)
         (interpose \-)
         str/join
         keyword)))

(def ^:private camel->keyword
  "from Emerick, Grande, Carper 2012 p.70"
  (keyword-converter #"(?<=[a-z])(?=[A-Z])"))

(def ^:private camel->keyword2
  "Like [[camel->keyword]], but smarter about acronyms and concepts like iSCSI
  and OpenID which should be treated as a single concept."
  (comp
   (keyword-converter #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
   #(str/replace % #"(?i)iscsi" "ISCSI")
   #(str/replace % #"(?i)openid" "OPENID")))

(defn- keyword->camel
  [kw]
  (let [n (name kw)
        m (.replace n "?" "")]
    (->> (str/split m #"\-")
         (fmap str/capitalize)
         str/join)))

(defn- aws-package?
  [clazz]
  (->> (.getName clazz)
       (re-find #"com\.amazonaws\.services")
       some?))

(defn to-date
  [date]
  (cond
    (instance? java.util.Date date) date
    (instance? AbstractInstant date) (.toDate date)
    (integer? date) (java.util.Date. date)
    true (.. (SimpleDateFormat. @date-format)
           (parse (str date) (ParsePosition. 0)))))

(defn to-file
  [file]
  (if (instance? File file)
    file
    (if (string? file)
      (File. file))))

(defn to-enum
  "Case-insensitive resolution of Enum types by String."
  [type value]
  (some
    #(if (and
           (not (nil? (.toString %))) ; some aws enums return nil!
           (= (str/upper-case value)
             (str/upper-case (.toString %))))
      %)
    (-> type
        (.getDeclaredMethod "values" (make-array Class 0))
        (.invoke type (make-array Object 0)))))

; assoc java Class to Clojure cast functions
(defonce ^:private coercions
  (->> [:String :Integer :Long :Double :Float]
       (reduce
         (fn [m e]
           (let [arr (str "[Ljava.lang." (name e) ";")
                 clazz (Class/forName arr)]
             (assoc m clazz into-array))) {})
       (merge {
         String     str
         Integer    int
         Long       long
         Boolean    boolean
         Double     double
         Float      float
         BigDecimal bigdec
         BigInteger bigint
         Date       to-date
         File       to-file
         Region     #(Region/getRegion (Regions/fromName %))
         ByteBuffer #(-> % str .getBytes ByteBuffer/wrap)
         "int"      int
         "long"     long
         "double"   double
         "float"    float
         "boolean"  boolean})
       atom))

(defn register-coercions
  "Accepts key/value pairs of class/function, which defines
  how data will be converted to the appropriate type
  required by the AWS Amazon*Client Java method."
  [& {:as coercion}]
  (swap! coercions merge coercion))

(defn coerce-value
  "Coerces the supplied stringvalue to the required type as
  defined by the AWS method signature. String or keyword
  conversion to Enum types (e.g. via valueOf()) is supported."
  [value type]
  (let [value (if (keyword? value) (name value) value)]
    (if-not (instance? type value)
      (if (= java.lang.Enum (.getSuperclass type))
        (to-enum type value)
        (if-let [coercion (@coercions (if (.isPrimitive type)
                                        (str type)
                                        type))]
          (coercion value)
          (throw (IllegalArgumentException.
                   (format "No coercion is available to turn %s into an object of type %s"
                           value type)))))
      value)))

(defn- default-value
  [class-name]
  (get
    {"boolean" false
     "double" (double 0.0)
     "float" (float 0.0)
     "long" 0
     "int" (int 0)}
    class-name))

(defn- constructor-args
  [ctor]
  (let [types (.getParameterTypes ctor)]
    (if (= 0 (count types))
      (make-array Object 0)
      (into-array Object
        (map
          (comp default-value str)
          (seq types))))))

(defn best-constructor
  "Prefer no-arg ctor if one exists, else the first found."
  [clazz]
  {:pre [clazz]}
  (let [ctors (.getConstructors clazz)]
    (or
      (some
        #(if (= 0 (count (.getParameterTypes %)))
          %
          nil)
        ctors)
      (first (sort-by
               (fn [ctor]
                 (some aws-package? (.getParameterTypes ctor)))
               ctors)))))

(defn- new-instance
  "Create a new instance of a Java bean. S3 neccessitates
  the check for contructor args here, as the rest of the
  AWS api contains strictly no-arg ctor JavaBeans."
  [clazz]
  {:pre [clazz]}
  (let [ctor (best-constructor clazz)
        arr  (constructor-args ctor)]
    (.newInstance ctor arr)))

(defn- unwind-types
  [depth param]
  (if (instance? ParameterizedType param)
      (let [f (partial unwind-types (inc depth))
            types (-> param .getActualTypeArguments)
            t (-> types last f)]
        (if (and (instance? ParameterizedType (last types))
                 (.contains (-> types last str) "java.util")
                 (.contains (-> types last str) "java.lang"))
          {:type [(-> types last .getRawType)]
           :depth depth}
          t))
      {:type [param]
       :depth depth}))

(defn- paramter-types
  [method]
  (let [types (seq (.getGenericParameterTypes method))
        param (last types)
        rval  {:generic types}]
    (if (instance? ParameterizedType param)
        (let [t (unwind-types 1 param)]
          (merge rval
                 {:actual (:type t)
                  :depth  (:depth t)}))
        rval)))

(defn- normalized-name
  [method-name]
  (-> (name method-name)
      (.replaceFirst
        (case (.substring method-name 0 3)
          "get" "get"
          "set" "set"
          "default")
      "")
      (.toLowerCase)))

(defn- matches?
  "We exclude any mutators of the bean which
   expect a java.util.Map$Entry as the first
   argument, as we won't be dealing in these
   from Clojure. Specifically, this is meant
   to address the various setKey() methods
   in the DynamoDBV2Client.
   http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/DeleteItemRequest.html#setKey(java.util.Map.Entry, java.util.Map.Entry)
   http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/GetItemRequest.html#setKey(java.util.Map.Entry, java.util.Map.Entry)"
  [method name value]
  (let [args (.getParameterTypes method)]
    (and (= name (normalized-name (.getName method)))
         (if (empty? value)
             (= 0 (count args))
             (and (< 0 (count args))
                  (or (= (count args) (count (flatten value)))
                      (and (coll? value)
                           (= 1 (count args))
                           (or (contains? @coercions (first args))
                               (.isArray (first args))
                               (and (.getPackage (first args))
                                    (.startsWith
                                      (.getName (.getPackage (first args)))
                                      "java.util")))))
                  (not (and (= 2 (count args))
                            (every? (partial = java.util.Map$Entry)
                                    args))))))))

(defn- accessor-methods
  [class-methods name value]
  (reduce
    #(if (matches? %2 name value)
      (conj %1 %2)
      %1)
    []
    class-methods))

(defn- find-methods
  [pojo k & v]
  (-> (.getClass pojo)
      (.getMethods)
      (accessor-methods
        (.toLowerCase (keyword->camel k))
        v)))

(defn to-java-coll
  "Need this only because S3 methods actually try to
   mutate (e.g. sort) collections passed to them."
  [col]
  (cond
    (map? col)
      (doto
        (java.util.HashMap.)
        (.putAll col))
    (set? col)
      (java.util.HashSet. col)
    (or (list? col) (vector? col))
      (java.util.ArrayList. col)))

(defn- invoke
  [pojo method v]
  (.invoke method pojo
    (into-array
      ;; misbehaving S3Client mutates the coll
      (if (and (coll? v)
               (not (nil? *client-class*))
               (= "AmazonS3Client" (.getSimpleName *client-class*)))
        (if (and (> (count (.getParameterTypes method)) 1)
                 (sequential? v))
          (to-java-coll v)
          [(to-java-coll v)])
          [v])))
  true)

(declare set-fields)

(defn kw->str [k]
  (if (keyword? k) (name k) k))

(defn- populate
  [types key props]
  (let [type (-> types key last)]
    (assert type (str "Bad data type - there is no " key " in " types))
    (if (contains? @coercions type)
      (coerce-value props type)
      (set-fields (new-instance type) props))))

(defn- unmarshall
  "Transform Clojure data to the required Java objects."
  [types col]
  (let [type (last (or (:actual types)
                       (:generic types)))
        pp   (partial populate types :actual)]
    (try
      (if (aws-package? type)
       (if (map? col)
         (if (contains? types :actual)
           (if (< (:depth types) 3)
             (apply assoc {}
                    (interleave (fmap kw->str (apply vector (keys col)))
                                (fmap pp (apply vector (vals col)))))
             (apply assoc {}
                    (interleave (fmap kw->str (apply vector (keys col)))
                                [(fmap #(populate {:generic [type]}
                                                  :generic
                                                  %)
                                       (first (apply vector (vals col))))])))
           (populate types :generic col))
         (if (and (contains? types :actual)
                  (= (:depth types) 3))
           (fmap #(fmap pp %) col)
           (fmap pp col)))
       (if (and (contains? types :actual)
                (aws-package? type))
         (fmap pp col)
         (fmap #(coerce-value % type) col)))
      (catch Throwable e
        (throw (RuntimeException. (str
                                    "Failed to create an instance of "
                                    (.getName type)
                                    " from " col
                                    " due to " e
                                    ". Make sure the data matches an existing constructor and setters.")))))))

(defn- invoke-method
  [pojo v method]
  (let [f       (partial invoke pojo method)
        types   (paramter-types method)
        generic (last (:generic types))]
    (if (and
          (coll? v)
          (not (contains? @coercions generic)))
      (f (unmarshall types v))
      (if (instance? generic v)
        (f v)
        (f (coerce-value v generic))))))

(defn set-fields
  "Returns the populated AWS *Request bean with 'args' as
   the values. args is a map with keywords as keys and any
   type values. Complex values will be recursively resolved
   to the corresponding method calls on the object graph."
  [pojo args]
  (doseq [[k v] args]
    (try
      (->> (find-methods pojo k v)
           (some (partial invoke-method pojo v)))
      (catch Throwable e
        (throw (ex-info
                 (str "Error setting " k ": " (.getMessage e) ". Perhaps the value isn't compatible with the setter?")
                 {:property k, :value v}
                 e)))))
  pojo)

(defn- create-bean
  [clazz args]
  (-> clazz new-instance (set-fields args)))

(defn- create-request-bean
  "Create a new instance of an AWS *Request style Java
   bean passed as the argument to a method call on the
   Amazon*Client class. (Note that we assume all AWS
   service calls take at most a single argument.)"
  [method args]
  (let [clazz (first (.getParameterTypes method))]
    (if (contains? @coercions clazz)
        (coerce-value (into {} args) clazz)
        (create-bean clazz args))))


(defprotocol IMarshall
  "Defines the contract for converting Java types to Clojure
  data. All return values from AWS service calls are
  marshalled. As such, the AWS service-specific namespaces
  will frequently need to implement this protocol in order
  to provide convenient data representations. See also the
  register-coercions function for coercing Clojure data to
  Java types."
  (marshall [obj]))

(defn- getter?
  [method]
  (let [name (.getName method)
        type (.getName (.getReturnType method))]
    (or (and
          (.startsWith name "get")
          (= 0 (count (.getParameterTypes method))))
        (and
          (.startsWith name "is")
          (= "boolean" type)))))

(defn accessors
  "Returns a vector of getters or setters for the class."
  [clazz getters?]
  (reduce
    #(if (or
           (and getters? (getter? %2))
           (and (not getters?)
                (.startsWith (.getName %2) "set")))
      (conj % %2)
      %)
    []
    (.getDeclaredMethods clazz)))

(defn- prop->name
  [method]
  (let [name (.getName method)]
    (if (.startsWith name "is")
      (str (.substring name 2) "?")
      (.substring name 3))))

(defn get-fields
  "Returns a map of all non-null values returned by
  invoking all public getters on the specified object."
  [obj]
  (let [no-arg (make-array Object 0)]
    (into {}
      (for [m (accessors (class obj) true)]
        (let [r (marshall (.invoke m obj no-arg))]
          (if-not (nil? r)
            (hash-map
              (camel->keyword (prop->name m))
              r)))))))

(extend-protocol IMarshall
  nil
  (marshall [obj] nil)

  java.util.Map
  (marshall [obj]
    (if-not (empty? obj)
      (apply assoc {}
        (interleave
          (fmap #(if (string? %) (keyword %) %)
                (apply vector (keys obj)))
          (fmap marshall
                (apply vector (vals obj)))))))

  java.util.Collection
  (marshall [obj]
    (if (instance? clojure.lang.IPersistentSet obj)
      obj
      (fmap marshall (apply vector obj))))

  java.util.Date
  (marshall [obj] (DateTime. (.getTime obj)))

  ; `false` boolean objects (i.e. (Boolean. false)) come out of e.g.
  ; .doesBucketExist, which wreak havoc on Clojure truthiness
  Boolean
  (marshall [obj] (when-not (nil? obj) (.booleanValue obj)))

  Object
  (marshall [obj]
    (if (aws-package? (class obj))
        (get-fields obj)
        obj)))

(defn- use-aws-request-bean?
  [method args]
  (let [types (.getParameterTypes method)]
    (and (or (map? args) (< 1 (count args)))
         (< 0 (count types))
         (or (map? args)
             (and
                (or (and
                      (even? (count args))
                      (not= java.io.File (last types)))
                    (and
                      (odd? (count args))
                      (= java.io.File (last types)))) ; s3 getObject() support
                (some keyword? args)))
         (or (aws-package? (first types))
             (and (aws-package? (last types))
                  (not (< (count types) (count args))))))))

(defn- prepare-args
  [method args]
  (let [types (.getParameterTypes method)
        num   (count types)]
    (if (and (empty? args) (= 0 num))
      (into-array Object args)
      (if (= num (count args))
        (into-array Object
          (map #(if (and (aws-package? %2) (seq (.getConstructors %2)))
                  ; must be a concrete, instantiatable class
                  (if (contains? @coercions %2)
                      (coerce-value % %2)
                      (create-bean %2 %))
                  (coerce-value % %2))
               (vec args)
               types))
        (if (use-aws-request-bean? method args)
          (cond
            (= 1 num)
            (into-array Object
                        [(create-request-bean
                            method
                            (seq (apply hash-map args)))])
            (and (aws-package? (first types))
                 (= 2 num)
                 (= File (last types)))
            (into-array Object
                        [(create-request-bean
                            method
                            (seq (apply hash-map (butlast args))))
                         (last args)])))))))


(defn- args-from
  "Function arguments take an optional first parameter map
  of AWS credentials. Addtional parameters are either a map,
  or seq of keys and values."
  [arg-seq]
  (let [args (first arg-seq)]
    (cond
      (or (and (or (map? args)
                   (map? (first args)))
               (or (contains? (first args) :access-key)
                   (contains? (first args) :endpoint)
                   (contains? (first args) :profile)
                   (contains? (first args) :client-config)))
          (instance? AWSCredentialsProvider (first args))
          (instance? AWSCredentials (first args)))
      {:args (if (-> args rest first map?)
                 (mapcat identity (-> args rest args-from :args))
                 (rest args))
       :credential (if (map? (first args))
                       (dissoc (first args) :client-config)
                       (first args))
       :client-config (:client-config (first args))}
      (map? (first args))
      {:args (let [m (mapcat identity (first args))]
               (if (seq m) m {}))}
      :default {:args args})))

(defn transfer-manager*
  [credential client-config crypto]
  (let [encryption-client (:encryption-client-fn client-config)
        amazon-client (:amazon-client-fn client-config)]
    (invoke-constructor
      "com.amazonaws.services.s3.transfer.TransferManager"
      (if crypto
        [(encryption-client crypto credential client-config)]
        [(amazon-client (Class/forName "com.amazonaws.services.s3.AmazonS3Client")
                        credential
                        client-config)]))))

(swap! client-config assoc :transfer-manager-fn (memoize transfer-manager*))

(defn candidate-client
  [clazz args]
  (let [cred-bound (or *credentials* (:credential args))
        credential (if (map? cred-bound)
                             (merge @credential cred-bound)
                             (or cred-bound @credential))
        config-bound (or *client-config* (:client-config args))
        client-config (merge @client-config config-bound)
        encryption-client (:encryption-client-fn client-config)
        amazon-client (:amazon-client-fn client-config)
        transfer-manager (:transfer-manager-fn client-config)
        crypto (if (even? (count (:args args)))
                   (:encryption (apply hash-map (:args args))))
        client  (if (and crypto (or (= (.getSimpleName clazz) "AmazonS3Client")
                                    (= (.getSimpleName clazz) "TransferManager")))
                    (delay (encryption-client crypto credential client-config))
                    (delay (amazon-client clazz credential client-config)))]
        (if (= (.getSimpleName clazz) "TransferManager")
            (transfer-manager credential client-config crypto)
            @client)))


(defn- fn-call
  "Returns a function that reflectively invokes method on
   clazz with supplied args (if any). The 'method' here is
   the Java method on the Amazon*Client class."
  [clazz method & arg]
  (binding [*client-class* clazz]
    (let [args    (args-from arg)
          arg-arr (prepare-args method (:args args))
          client  (delay (candidate-client clazz args))]
      (fn []
        (try
          (let [java (.invoke method @client arg-arr)
                cloj (marshall java)]
            (if (and
                  @root-unwrapping
                  (map? cloj)
                  (= 1 (count (keys cloj))))
              (-> cloj first second)
              cloj))
          (catch InvocationTargetException ite
            (throw (.getTargetException ite))))))))

(defn- types-match-args
  [args method]
  (let [types (.getParameterTypes method)]
    (if (and (= (count types) (count args))
             (every? identity (map instance? types args)))
        method)))

(defn- coercible? [type]
  (and (contains? @coercions type)
       (not (re-find #"java\.lang" (str type)))))

(defn- choose-from [possible]
  (if (= 1 (count possible))
      (first possible)
      (first
        (sort-by
          (fn [method]
            (let [types (.getParameterTypes method)]
              (cond
                (some coercible? types) 1
                (some #(= java.lang.Enum (.getSuperclass %)) types) 2
                :else 3)))
          possible))))

(defn possible-methods
  [methods args]
  (filter
    (fn [method]
      (let [types (.getParameterTypes method)
            num   (count types)]
        (if (or
              (and (empty? args) (= 0 num))
              (use-aws-request-bean? method args)
              (and
                (= num (count args))
                (not (keyword? (first args)))
                (not (aws-package? (first types)))))
          method
          false)))
    methods))

(defn- best-method
  "Finds the appropriate method to invoke in cases where
  the Amazon*Client has overloaded methods by arity or type."
  [methods & arg]
  (let [args (:args (args-from arg))
        methods (filter #(not (Modifier/isPrivate (.getModifiers %))) methods)]
    (or (some (partial types-match-args args) methods)
        (choose-from (possible-methods methods args)))))

(defn intern-function
  "Interns into ns, the symbol mapped to a Clojure function
   derived from the java.lang.reflect.Method(s). Overloaded
   methods will yield a variadic Clojure function."
  [client ns fname methods]
  (intern ns (with-meta (symbol (name fname))
               {:amazonica/client client
                :amazonica/methods methods})
    (fn [& args]
      (if-let [method (best-method methods args)]
        (if-not args
          ((fn-call client method))
          ((fn-call client method args)))
        (throw (IllegalArgumentException.
                 (format "Could not determine best method to invoke for %s using arguments %s"
                         (name fname) args)))))))

(defn- client-methods
  "Returns a map with keys of idiomatic Clojure hyphenated keywords
  corresponding to the public Java method names of the class argument, vals are
  vectors of java.lang.reflect.Methods."
  [client]
  (let [methods (->> (.getDeclaredMethods client)
                     (remove #(.isSynthetic %))
                     (group-by #(camel->keyword (.getName %))))]
    (if (#{"AWSLambdaClient" "AmazonCloudSearchDomainClient"} (.getSimpleName client))
      methods
      (apply dissoc methods excluded))))

(defn- show-functions [ns]
  (intern ns (symbol "show-functions")
    (fn []
      (->> (ns-publics ns)
           sort
           (map (comp println first))))))

(defn set-client
  "Intern into the specified namespace all public methods
   from the Amazon*Client class as Clojure functions."
  [client ns]
  (show-functions ns)
  (intern ns 'client-class client)
  (doseq [[fname methods] (client-methods client)
          :let [the-var (intern-function client ns fname methods)
                fname2 (-> methods first .getName camel->keyword2)]]
    (when (not= fname fname2)
      (let [the-var2 (intern-function client ns fname2 methods)]
        (alter-meta! the-var assoc :amazonica/deprecated-in-favor-of the-var2)))))
