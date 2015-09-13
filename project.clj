(defproject fetch-citations "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [enlive "1.1.6"]
                 [riemann-clojure-client "0.4.1"]]
  :profiles {:dev {:java-cmd "/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java"
                   :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]]}}
)
