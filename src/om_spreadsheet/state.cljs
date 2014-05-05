(ns om-spreadsheet.state)

(def schema {})
(def ^:private rows 6)
(def ^:private columns 6)
(def initial-facts
  (concat
    [[:db/add -1 :header/text "Hello, World!"]]
    [[:db/add -2 :table/rows rows]
     [:db/add -3 :table/columns columns]]
    (mapcat
      identity
      (for [i (take rows (range 97 123))
            :let [c (char i)]]
        (mapcat
          identity
          (for [j (range 1 (inc columns))
                :let [id (- -100 (+ i (* i j)))]]
            [[:db/add id :cell/value ""]
             [:db/add id :cell/location (keyword (str c j))]
             [:db/add id :cell/focused? false]]))))))
