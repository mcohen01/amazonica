(ns amazonica.core
  "Amazon AWS functions."
  (:use [clojure.algo.generic.functor :only (fmap)])
  (:require [clojure.string :as str])
  (:import clojure.lang.Reflector
           com.amazonaws.AmazonServiceException
           com.amazonaws.services.dynamodb.model.AttributeValue
           [com.amazonaws.auth
             AWSCredentials
             AWSCredentialsProvider
             BasicAWSCredentials
             BasicSessionCredentials
             DefaultAWSCredentialsProviderChain]
           [com.amazonaws.regions
             Region
             Regions]
           [com.amazonaws.services.s3
             AmazonS3Client
             AmazonS3EncryptionClient]
           [com.amazonaws.services.s3.model
             CryptoConfiguration
             EncryptionMaterials
             StaticEncryptionMaterialsProvider]
           com.amazonaws.services.s3.transfer.TransferManager           
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
           java.text.ParsePosition
           java.text.SimpleDateFormat
           java.util.Date))

(defonce ^:private credential (atom {}))

(def ^:dynamic ^:private *credentials* nil)

(def ^:dynamic ^:private *client-class* nil)

(def ^:private date-format (atom "yyyy-MM-dd"))

(def ^:private root-unwrapping (atom false))

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
  #{:invoke
    :init
    :set-endpoint
    :get-cached-response-metadata
    :get-service-abbreviation})
    ; addRequestHandler???


(defn- keys->cred
  [access-key secret-key endpoint]
  (let [credential {:access-key access-key
                    :secret-key secret-key}]
    (if-not (empty? endpoint)
      (merge credential {:endpoint (first endpoint)})
      credential)))

(defn defcredential
  "Specify the AWS access key, secret key and optional
  endpoint to use on subsequent requests."
  [access-key secret-key & endpoint]
  (reset!
    credential
    (keys->cred access-key secret-key endpoint)))
        
(defmacro with-credential
  "Per invocation binding of credentials for ad-hoc
  service calls using alternate user/password combos
  (and endpoints)."
  [[a b c] & body]
  `(binding [*credentials* ~(keys->cred a b c)]
    (do ~@body)))

(declare new-instance)

(defn- create-client
  [aws-client credentials]
  (if (nil? credentials)
    (new-instance aws-client)
    ; TransferManager is the only client to date that doesn't
    ; accept AWSCredentialsProviders
    (let [credentials (if (= aws-client TransferManager)
                        (AmazonS3Client. credentials)    
                        credentials)]
      (Reflector/invokeConstructor
        aws-client
        (into-array [credentials])))))

(defn get-credentials
  [credentials]
  (cond
    (or (instance? AWSCredentialsProvider credentials)
        (instance? AWSCredentials credentials))
    credentials
    (contains? credentials :session-token)
    (BasicSessionCredentials.
        (:access-key credentials)
        (:secret-key credentials)
        (:session-token credentials))
    (contains? credentials :access-key)
    (BasicAWSCredentials.
        (:access-key credentials)
        (:secret-key credentials))
    :else
    (DefaultAWSCredentialsProviderChain.)))

(defn- encryption-client*
  [materials credentials]
  (let [creds    (get-credentials credentials)
        key      (or (:secret-key materials)
                     (:key-pair materials))
        crypto   (CryptoConfiguration.)
        em       (EncryptionMaterials. key)
        enc-mat  (StaticEncryptionMaterialsProvider. em)]
    (if-let [provider (:provider materials)]
        (.withCryptoProvider crypto provider))
    (AmazonS3EncryptionClient. creds enc-mat crypto)))
      
(defn- amazon-client*
  [clazz credentials]
  (let [aws-creds (get-credentials credentials)
        client    (create-client clazz aws-creds)]
    (when-let [endpoint (:endpoint credentials)]
      (try
        (.getDeclaredMethod clazz "setRegion" (make-array Class 0))
        (->> (-> (str/upper-case endpoint)
                 (.replaceAll "-" "_"))
             Regions/valueOf
             Region/getRegion
             (.setRegion client))
        (catch NoSuchMethodException e)))
    client))

(def ^:private encryption-client
  (memoize encryption-client*))

(def ^:private amazon-client
  (memoize amazon-client*))


(defn- camel->keyword
  "from Emerick, Grande, Carper 2012 p.70"
  [s]
  (->> (str/split s #"(?<=[a-z])(?=[A-Z])")
       (map str/lower-case)
       (interpose \-)
       str/join
       keyword))

(defn- keyword->camel
  [kw]
  (let [n (name kw)
        m (.replace n "?" "")]
    (->> (str/split m #"\-")
         (fmap str/capitalize)
         str/join)))


(defn- aws-package?
  [clazz]
  (re-find #"com\.amazonaws\.services" (.getName clazz)))

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
(def ^:private coercions (atom
  {String     str
   Integer    int
   Long       long
   Boolean    boolean
   Double     double
   Float      float
   BigDecimal bigdec
   BigInteger bigint
   Date       to-date
   File       to-file
   "int"      int
   "long"     long
   "double"   double
   "float"    float
   "boolean"  boolean}))

(defn register-coercions
  "Accepts key/value pairs of class/function, which defines
  how data will be converted to the appropriate type 
  required by the AWS Amazon*Client Java method."
  [& {:as coercion}]
  (swap! coercions merge coercion))

(defn coerce-value
  "Coerces the supplied value to the required type as
  defined by the AWS method signature. String conversion
  to Enum types (e.g. via valueOf()) is supported."
  [value type]
  (if-not (instance? type val)
    (if (= java.lang.Enum (.getSuperclass type))
      (to-enum type value)
      (if-let [coercion (@coercions (if (.isPrimitive type)
                                      (str type)
                                      type))]
        (coercion value)
        (throw (IllegalArgumentException.
                 (format "No coercion is available to turn %s into an object of type %s"
                         value type)))))
    val))

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
  (let [ctors (.getConstructors clazz)]
    (or
      (some 
        #(if (= 0 (count (.getParameterTypes %)))
          %
          nil)
        ctors)
      (first ctors))))

(defn- new-instance
  "Create a new instance of a Java bean. S3 neccessitates
  the check for contructor args here, as the rest of the
  AWS api contains strictly no-arg ctor JavaBeans."
  [clazz]
  (let [ctor (best-constructor clazz)
        arr  (constructor-args ctor)]
    (.newInstance ctor arr)))
    
(defn- unwind-types
  [param]
  (if (instance? ParameterizedType param)
    (-> param
        (.getActualTypeArguments)
        last
        unwind-types)
    [param]))

(defn- paramter-types
  [method]
  (let [types (seq (.getGenericParameterTypes method))
        param (first types)
        rval  {:generic types}]
    (if (instance? ParameterizedType param)
      (merge rval
        {:actual (unwind-types param)
         :raw    (.getRawType param)})
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
  [method name getter?]
  (let [args (.getParameterTypes method)]
    (and (= name (normalized-name (.getName method)))
         (case getter?
           true  (= 0 (count args))
           false (and (< 0 (count args))
                      (not (and (= 2 (count args))
                                (every? (partial = java.util.Map$Entry) 
                                        args))))))))
                      
(defn- accessor-methods
  [class-methods name getter?]
  (reduce
    #(if (matches? %2 name getter?)
      (conj %1 %2)
      %1)
    []
    class-methods))

(defn- find-methods
  [pojo k & v]
  (-> (.getClass pojo)
      (.getDeclaredMethods)
      (accessor-methods
        (.toLowerCase (keyword->camel k))
        (empty? v))))

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
            (= AmazonS3Client *client-class*))
        [(to-java-coll v)]
        [v])))
  true)
  
(declare set-fields)

(defn kw->str [k]
  (if (keyword? k) (name k) k))

(defn- populate
  [types key props]
  (let [type (-> types key last)]
    (if (contains? @coercions type)
      (coerce-value props type)
      (set-fields (new-instance type) props))))

(defn- unmarshall
  "Transform Clojure data to the required Java objects."
  [types col]
  (let [type (last (or (:actual types)
                       (:generic types)))
        pp   (partial populate types :actual)]
    (if (aws-package? type)
      (if (map? col)
        (if (contains? types :actual)
          (apply assoc {}
            (interleave
              (fmap kw->str (apply vector (keys col)))
              (fmap pp (apply vector (vals col)))))
          (populate types :generic col))
        (fmap pp col))
      (if (and (contains? types :actual)
               (aws-package? type))
        (fmap pp col)
        (fmap #(coerce-value % type) col)))))

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
    (->> (find-methods pojo k v)
         (some (partial invoke-method pojo v))))
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
  (-> (.getParameterTypes method)
      first
      (create-bean args)))


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
    (or (.startsWith name "get")
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
  (marshall [obj]
    nil)
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
    (marshall [obj]
      (DateTime. (.getTime obj)))
  ; `false` boolean objects (i.e. (Boolean. false)) come out of e.g.
  ; .doesBucketExist, which wreak havoc on Clojure truthiness
  Boolean
    (marshall [obj] (.booleanValue obj))
  Object
    (marshall [obj]
      (if (aws-package? (class obj))
        (get-fields obj)
        obj)))

(defn- use-aws-request-bean?
  [method args]
  (let [types (.getParameterTypes method)]
    (and (< 1 (count args))
         (< 0 (count types))
         (and
            (or (and
                  (even? (count args))
                  (not= java.io.File (last types)))
                (and
                  (odd? (count args))
                  (= java.io.File (last types)))) ; s3 getObject() support
            (some keyword? args))
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
                  (create-bean %2 %)
                  (coerce-value % %2))
               (vec args) 
               types))
        (if (use-aws-request-bean? method args)
          (if (= 1 num)
            (into-array Object 
              [(create-request-bean
                  method 
                  (seq (apply hash-map args)))])
            ; note: AWS api only ever uses custom bean types
            ; as the first or last argument, as of v1.4.0
            ))))))


(defn- args-from
  "Function arguments take an optional first parameter map
  of AWS credentials. Addtional parameters are either a map,
  or seq of keys and values."
  [args]
  (let [a (first args)]
    (cond
      (or (and (map? (first a))
               (contains? (first a) :access-key))
          (instance? AWSCredentialsProvider (first a))
          (instance? AWSCredentials (first a)))
      {:args (rest a) :credential (first a)}
      (map? (first a))
      {:args (interleave (keys (first a)) 
                         (vals (first a)))}
      :default {:args a})))

(defn- candidate-client
  [clazz args]
  (if (and (= clazz AmazonS3Client)
           (even? (count (:args args)))
           (contains? (apply hash-map (:args args)) :encryption))
      (encryption-client (:encryption (apply hash-map (:args args)))
                         (or (:credential args) @credential))
      (amazon-client
        clazz
        (or (:credential args) @credential))))

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
          (let [c (if (thread-bound? #'*credentials*)
                      (amazon-client clazz *credentials*)
                      @client)
                java (.invoke method c arg-arr)
                cloj (marshall java)]
            (if (and
                  @root-unwrapping
                  (map? cloj)
                  (= 1 (count (keys cloj))))
              (-> cloj first second)
              cloj))
          (catch InvocationTargetException ite          
            (throw (.getTargetException ite))))))))

(defn- best-method
  "Finds the appropriate method to invoke in cases where
  the Amazon*Client has overloaded methods by arity or type."
  [methods & arg]
  (let [args (:args (args-from arg))
        methods (filter #(not (Modifier/isPrivate (.getModifiers %))) methods)]
    (some
      (fn [method]
        (let [types (.getParameterTypes method)
              num   (count types)]
          (if (or
                (and (empty? args) (= 0 num))
                (use-aws-request-bean? method args)
                (and
                  (= num (count args))
                  (not (aws-package? (first types)))))
            method
            false)))
      methods)))

(defn- intern-function
  "Interns into ns, the symbol mapped to a Clojure function
   derived from the java.lang.reflect.Method(s). Overloaded
   methods will yield a variadic Clojure function."
  [client ns fname methods]
  (intern ns (symbol (name fname))
    (fn [& args]
      (if-let [method (best-method methods args)]
        (if-not args
          ((fn-call client method))
          ((fn-call client method args)))
        (throw (IllegalArgumentException.
                 (format "Could not determine best method to invoke for %s using arguments %s"
                         (name fname) args)))))))

(defn- client-methods
  "Returns a map with keys of idiomatic Clojure hyphenated
   keywords corresponding to the public Java method names of
   the class argument, vals are vectors of
   java.lang.reflect.Methods."
  [client]
  (reduce
    (fn [col method]
      (let [fname (camel->keyword (.getName method))]
        (if (contains? excluded fname)
          col
          (if (contains? col fname)
            (update-in col [fname] conj method)
            (assoc col fname [method])))))
    {}
    (.getDeclaredMethods client)))

(defn set-client
  "Intern into the specified namespace all public methods
   from the Amazon*Client class as Clojure functions."
  [client ns]
  (doseq [[k v] (client-methods client)]
    (intern-function client ns k v)))
