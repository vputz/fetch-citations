(ns fetch-citations.core
  (:use net.cgrand.enlive-html)
  (require [riemann.client :as riemann])
  (require [clojure.edn :as edn]))
(import [java.net URLEncoder])

(def test-doi "10.1039/C0SM00164C")

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

(defn citation-div [urlres]
  "Given an enlive url resource, grab the scholar div with id 'gs_ab_md'"
  (let [text (select urlres [:div#gs_ab_md :> text-node])]
    (apply str text)))

(defn citations [cit-div]
  "Given a text string such as '2 results ' retrieves the 2"
  (edn/read-string (last (re-find #"(\d+) results" cit-div))))

(defn citations-from-doi [url]
  (-> url
      url-resource
      citation-div
      citations))

(defn send-riemann-report [doi host port]
  (let [c (riemann/tcp-client {:host host :port port})
        cites (citations-from-doi doi)]
    (-> c (riemann/send-event 
           {:host "vagrant" :service (str "cites-" doi) :metric cites})
        (deref 5000 ::timeout))))
    
