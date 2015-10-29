# graci

A simple Clojure library designed to pull raw data from [Graphite](https://github.com/graphite-project) 
for analysis and redisplay with [incanter](https://github.com/incanter/incanter) This
should allow for closer examination of the underlying data and better correlation of 
multiple metrics. For example, identifying when slopes change for count/countFailed 
across multiple service points without decoding complicated plot overlays.

NOTE: It would be better to stream the raw data from an aggregation point below Graphite (Storm, Kafka, Riemann, etc),
but this should help provide some insight.

## Usage

Graci is intended to work from the REPL, so clone and point at your local, friendly Graphite server
and provide a target Graphite path. The dot notation is representative of Graphite's ~directory 
structure path to the underlying data.

```
; Assumes Leiningen is installed
git clone http://github.com/dmillett/graci
cd graci
lein repl
```

Create the urls for the Graphite server instance and service/event paths.
```clojure
; Build the graphite urls (server, service path)
user=> (def am_search (graphite-urls "http://graphite.hulk" "am.search"))

; Specify a single metric
user=> (def am_winning (graphite-urls "http://graphite.hulk" "am.smash" :metrics ["count"]))

; Specify a specific time range
user=> (def am_smash (graphite-urls "http://graphite.hulk" "am.smash" :from "20151027 18" :until "20151027 19:30"))
```

Fetch the raw Graphite data, which returns a collection of hashmaps
```clojure
; From urls above
user=> (def data (get-graphite-data am_smash))

; Graphite data is in the :body of the response
user=> (def parsed (:body (first data)))
{am.smash.avgLatency {:start 1445868060, :stop 1445868240, :step 60, :results (0.547 0.623 nil)}}
```

## License

Copyright © 2015 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
