;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns ^{:author "dmillett"} graci.core
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [clj-http.util :as util])
  (:import (java.time LocalDateTime ZoneOffset)
           (java.time.format DateTimeFormatter)))

(def dpipe #"\|")
(def dcomma #",")
(def dnewline #"\n")
(def dtf (DateTimeFormatter/ofPattern "yyyyMMdd_HH:mm"))

(def datetime-patterns
  "Currently accepted datetime patterns."
  '("yyyyMMdd" "yyyyMMdd HH" "yyyyMMdd HH:mm" "HH:mm" "HH"))

(defn format-java-datetime
  "Formats and url encodes a Java LocalDate to something graphite
  can manage. The indicator will be: 'from' or 'until' while the
  datetime is formatted to HH:mm_yyyyMMdd before encoding."
  [java_datetime]
  (.. java_datetime (format (DateTimeFormatter/ofPattern "HH:mm_yyyyMMdd"))) )

(defn graphite-date
  "Creates a date from string that is used in graphite. Supported date-time formats
  include: see (datetime-patterns). This seems a bit messy (todo: cleanup)."
  [date_time]
  (let [text (if (number? date_time) (apply str (take 2 (str date_time))) date_time)
        c (count text)
        ldtfx #(LocalDateTime/parse %1 (DateTimeFormatter/ofPattern %2))]
    (cond
      (or (= 1 c) (= 2 c)) (.. (LocalDateTime/now) (withMinute 0) (withHour (read-string text)))
      (= 5 c) (let [[h m] (map #(read-string %) (s/split text #":"))]
                           (.. (LocalDateTime/now) (withHour h) (withMinute m)))
      (= 8 c) (ldtfx (str text " 00") "yyyyMMdd HH")
      (= 11 c) (ldtfx text "yyyyMMdd HH")
      (= 14 c) (ldtfx text "yyyyMMdd HH:mm")
      :else (throw (RuntimeException. (str "'" date_time "' Is Invalid. See (datetime-patterns)")))
     ) ) )

(defn graphite-urls
  "Create a graphite url lookup (hint: graphite uses a directory type structure),
  to specify which data and metrics to retrieve. This is broken down by:
  :server - \"http://your.graphite.server\"
  :target - \"this.is.a.test.event \"
  optional:
  :metrics - '(\"avgLatency\") ;  default'(avgLatency, count, countFailed)
  :from    - \"2015-10-20\" or \"2015-10-20 10:00\"  ; defaults to current - 24 hours
  :until   - \"2015-10-21 09:00\" ; defaults to 'now'

  NOTE: :metrics '(\"*\") to retrieve all metrics for a target event"
  [server base & {:keys [metrics from until] :or {metrics '("avgLatency", "count", "countFailed") from nil until nil}}]
  ; Maybe a bit too much happening here
  (let [s (if (.endsWith server "/") (apply str (drop-last server)) server)
        start (if (nil? from) "" (str "&from=" (util/url-encode (format-java-datetime (graphite-date from))) ))
        stop (if (nil? until) "" (str "&until=" (util/url-encode (format-java-datetime (graphite-date until))) ))]
    (map #(str s "/render/?" start stop "&target=" base "." % "&rawData=true") metrics)
    ) )

(defn get-graphite-data
  "Pull the raw data for a group of URLs from Graphite (single threaded).
  It works, just test in production ;-)"
  [graphite_urls]
  (reduce (fn [result current] (conj result (client/get current))) [] graphite_urls) )

(defn metric-header-date
  "Determine the date-time for the start/stop fields in the graphite
  metric header. This should correspond to seconds from 1970-01-01."
  [milli_time offset_hours]
  (let [t (if (number? milli_time) milli_time (read-string milli_time))]
    (.. (LocalDateTime/ofEpochSecond t 0 (ZoneOffset/ofHours offset_hours)) (format dtf))
    ) )

(defn parse-metric-data
  "Parse the data for a single graphite path/metric. If there are multiple metrics,
   then split them by line and use this function. Start and Stop times are seconds
   since 1970-01-01. Use an offset of 5 for ('central/us'). Each data point is
   'n' steps away from start/stop where a step is in seconds (60 in many cases)."
  [data]
  ; Look at transducers to for map to correct 'None' and 'nil'
  (let [[header body] (s/split data dpipe)
        [metric start stop step] (s/split header dcomma)
        results (s/split body dcomma)
        filtered (map #(if (= "None" %) nil (read-string %)) results)]
    {metric {:start (read-string start) :stop (read-string stop) :step (read-string step) :results filtered}}
    ) )

(defn parse-graphite-metrics
  "Parse the data for multiple graphite metrics '(count, countFailed, avgLatency, etc)"
  [metric_group_data]
  (apply merge (map parse-metric-data (s/split metric_group_data dnewline))))

(defn plusx
  "Handles the addition of only numbers and will not throw an exception.
  For example, if (+ x y) and 'y' is a String, it returns x"
  ([] nil)
  ([x] x)
  ([x y] (cond (and (number? x) (number? y)) (+ x y) (number? x) x :else y)) )

(defn average
  [data]
  ; todo, look at transduce here instead of intermediate collection
  (let [fd (filter #(not (nil? %)) data)]
    (/ (reduce plusx fd) (count fd))
    ) )
