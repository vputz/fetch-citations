(ns fetch-citations.wos-history
  (require [riemann.client :as riemann]
           [riemann.codec :as codec]
           [clojure.java.io :as io]
           [clj-time.core :as time]
           [clj-time.format :as tformat]
           [clj-time.coerce :as coerce]
           [semantic-csv.core :as sc :refer :all]
           [clojure.data.csv :as csv]
))

(def test-file "test/fetch_citations/sample_wos_citations.csv")

(defn as-rows [file]
  (process 
   (csv/read-csv file :separator \tab)))

(defn take-csvrows [n filename]
  (with-open [in-file (io/reader filename)]
    (let [csv-rows (as-rows in-file)]
      (doall (take n csv-rows)))))

(defn dates [filename]
  (with-open [in-file (io/reader filename)]
    (let [csv-rows (as-rows in-file)]
      (doall (map #(str (:PD %) " " (:PY %)) csv-rows)))))

(def multiparser
  (tformat/formatter (time/default-time-zone) "MMM dd YYYY" "MMM YYYY" "YYYY"))

(defn dates-to-datetimes
  "converts a list of dates in mm dd yyyy form to long" 
  [dates]
  (map #(tformat/parse multiparser (.trim %)) dates))

(defn dates-to-longs
  [dates]
  (map coerce/to-long dates))

(defn count-dates
  "converts a list of dates to an incremental (count, date) list"
  [dates]
  (map vector (iterate inc 0) dates))

(defn send-riemann-report [doi citations time fetcher-host riemann-host riemann-port]
  (let [c (riemann/tcp-client {:host riemann-host :port riemann-port})
        ]
    (-> c (riemann/send-event 
           {:host fetcher-host :service (str "cites-" doi) :metric citations :time (/ time 1000)})
        (deref 5000 ::timeout))))


(defn report-dates [doi csvfile fetcher-host riemann-host riemann-port]
  (let [dates (-> csvfile 
                  dates 
                  dates-to-datetimes 
                  dates-to-longs
                  sort 
                  count-dates)
        sendrep #(send-riemann-report doi (first %) (second %) fetcher-host riemann-host riemann-port)
] 
    (map sendrep dates)
    ))
