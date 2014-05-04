(ns om-spreadsheet.state)

(def schema {})
(def initial-facts
  (concat
    [[:db/add -1 :header/text "Hello, World!"]]
    [[:db/add -2 :cell/value ""]]))
