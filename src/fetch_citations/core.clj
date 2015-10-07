(ns fetch-citations.core
  (:use net.cgrand.enlive-html)
  (require [riemann.client :as riemann])
  (require [clojure.data.json :as json])
  (require [clojure.edn :as edn]))
(import [java.net URLEncoder])

(def test-doi "10.1039/C0SM00164Cy")

(defn scholar-query-url [doi]
  (str "http://scholar.google.com/scholar?q=" 
       (URLEncoder/encode doi "UTF-8")))

(defn url-resource [doi]
  "Given a DOI string, download the scholar search and return as an enlive resource"
  (with-open [inputstream (-> (java.net.URL. 
                               (scholar-query-url doi))
                              .openConnection
                              (doto (.setRequestProperty "User-Agent"
                                                         "Mozilla/5.0 ..."))
                              .getContent)]
    (html-resource inputstream)))

(defn citation-text [urlres]
  "Given an enlive url resource, grab the scholar div with id 'gs_ab_md'"
  (let [text (select urlres [:div.gs_ri :div.gs_fl :a :> text-node])]
    (apply str (filter #(.contains % "Cited by") text))))

(defn citations [cit-div]
  "Given a text string such as 'Cited by 2' retrieves the 2"
  (edn/read-string (last (re-find #"Cited by (\d+)" cit-div))))

(defn citations-from-doi [doi]
  (-> doi
      url-resource
      citation-text
      citations))

(defn send-riemann-report [doi fetcher-host riemann-host riemann-port]
  (let [c (riemann/tcp-client {:host riemann-host :port riemann-port})
        cites (citations-from-doi doi)]
    (-> c (riemann/send-event 
           {:host fetcher-host :service (str "cites-" doi) :metric cites})
        (deref 5000 ::timeout))))
    
(defn read-config [filename]
  (json/read-str (slurp filename)))

(defn doi-pairs-from-config [config]
  "Reads a map of keys->[dois] and returns pairs of (key, doi)"
  (for [kv (get config "dois")
        y (second kv)]
    [(first kv) y]))

(defn fetch-results [config-filename]
  (let [config (read-config config-filename) 
        fetcher-host ((config "config") "fetcher-host")
        riemann-host ((config "config") "riemann-host")
        riemann-port ((config "config") "riemann-port")
        doi-pairs (doi-pairs-from-config config)]
    (map #(send-riemann-report (second %) fetcher-host riemann-host riemann-port) doi-pairs))
  )
