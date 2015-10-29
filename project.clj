(defproject graci "0.1.0-SNAPSHOT"
  :description "A Clojure command line tool to pull Graphite raw metric data into a collection
  of hashmaps. This allows for programmatic evaluation of graphite data outside of Graphite.
  This might be useful for applying formulas that are not included with Graphite, or evaluating
  data ranges in a different ways.

  NOTE: a better solution would be to grab this data directly from a datasource (Storm spout, other) rather
  than relaying the data through Graphite."
  :url "https://github.com/dmillett/graci"
  :scm {:name "git" :url "https://github.com/dmillett/graci" }
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ; Increase as necessary pending size/count of graphite metric data
  :jvm-opts ["-Xms1024m" "-Xmx1024m"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [incanter "1.5.6"]]

  :repl-options {:init (do
                         (load-file "src/graci/core.clj")
                         (use 'graci.core)
                         (require '[incanter.core :as icore])
                         (require '[incanter.stats :as istats])
                         (require '[incanter.charts :as icharts])
                         (require '[incanter.io :as iio])
                         (println "Dependencies loaded. 'graci' is now ready for use."))})
