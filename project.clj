(defproject amazonica "0.1.22-SNAPSHOT"
  :description "A comprehensive Clojure client for the entire Amazon AWS api."
  :url "https://github.com/mcohen01/amazonica"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["src/main/java"]
  :target-path "target"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.amazonaws/aws-java-sdk "1.5.5"]
                 [org.clojure/algo.generic "0.1.0"]
                 [joda-time "2.1"]
                 [robert/hooke "1.3.0"]])