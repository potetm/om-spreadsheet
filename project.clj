(defproject om-spreadsheet "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[datascript "0.1.4"]
                 [figwheel "0.1.0-SNAPSHOT"]
                 [om "0.6.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [sablono "0.2.16"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.1.0-SNAPSHOT"]]
  :source-paths ["src"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/om_spreadsheet.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :none
                           :source-map true
                           :externs ["react/externs/react.js"]}}]})
