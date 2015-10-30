;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns ^{:author "dmillett"} graci.core_test
  (:require [clojure.test :refer :all]
            [graci.core :refer :all])
  (:import (java.time LocalDateTime ZoneOffset)))

; Each group of metric data is returned as 1 line. Multiple metrics are delimited by a newline
(def latency "am.smash.avgLatency,1445868060,1445868240,60|12709.547,11976.233,None")
(def success "am.smash.count,1445868060,1445868240,60|53.0,60.0,None")
(def failed "am.smash.countFailed,1445868060,1445838240,60|0.0,2.0,None")

(deftest test-metric-header-date
  (is (= "20151026_19:01" (metric-header-date "1445868060" 5))))

(deftest test-format-java-datetime
  (is (= "19:01_20151026" (format-java-datetime (LocalDateTime/ofEpochSecond 1445868060 0 (ZoneOffset/ofHours 5))) ) ) )

(deftest test-graphite-date
  (are [x y] (= x y)
    20 (. (graphite-date "20") getHour)
    42 (. (graphite-date "20:42") getMinute)
    20 (. (graphite-date "20:42") getHour)
    2015 (. (graphite-date "20151027") getYear)
    10 (. (graphite-date "20151027 20") getMonthValue)
    27 (. (graphite-date "20151027 20:42") getDayOfMonth)
    ) )

(deftest test-graphite-urls
  (let [urls1 (graphite-urls "http://graphite.hulk" "am.search")
        urls2 (graphite-urls "http://graphite.hulk" "am.strong" :from "20151027")
        urls3 (graphite-urls "http://graphite.hulk" "am.smash" :from "20151027 18" :until "20151027 19:30")
        urls4 (graphite-urls "http://graphite.hulk" "smash.puny.metric" :metrics ["countFailed"])]

    (are [x y] (= x y)
      3 (count urls1)
      "http://graphite.hulk/render/?&target=am.search.avgLatency&rawData=true" (first urls1)
      "http://graphite.hulk/render/?&from=00%3A00_20151027&target=am.strong.avgLatency&rawData=true" (first urls2)
      "http://graphite.hulk/render/?&from=18%3A00_20151027&until=19%3A30_20151027&target=am.smash.avgLatency&rawData=true" (first urls3)
      1 (count urls4)
      "http://graphite.hulk/render/?&target=smash.puny.metric.countFailed&rawData=true" (first urls4)
      ) ) )

(deftest test-parse-metric-data
  (let [r1 (parse-metric-data latency)]
    (are [x y] (= x y)
      1445868060 (:start (first (vals r1)))
      1445868240 (:stop (first (vals r1)))
      60 (:step (first (vals r1)))
      '(12709.547 11976.233 nil) (:results (first (vals r1)))
      ) ) )

(deftest test-parse-graphite-metrics
  (let [data (str latency "\n" success "\n" failed)
        results (parse-graphite-metrics data)]
    (are [x y] (= x y)
      3 (count results)
      12709.547 (first (:results (get results "am.smash.avgLatency")))
      53.0 (first (:results (get results "am.smash.count")))
      0.0 (first (:results (get results "am.smash.countFailed")))
      ) ) )

(deftest test-metric-time-series
  (let [metric1 (parse-metric-data latency)
        result1 (metric-time-series metric1)
        result2 (apply metric-time-series metric1)]
    (are [x y] (= x y)
      {"am.smash.avgLatency" {1445868060 12709.547, 1445868120 11976.233, 1445868180 nil}} result1
      result2 result1
      )
    ) )

(deftest test-plusx
  (are [x y] (= x y)
    nil (plusx)
    1 (plusx 1 "a")
    42 (plusx nil 42)
    ) )

(deftest test-average
  (is (= 12342.89 (average (:results (first (vals (parse-metric-data latency))))))))
