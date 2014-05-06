(ns om-spreadsheet.state)

(def ^:private rows 6)
(def ^:private columns 6)

(def initial-state
  {:header-text "Hello, World!"
   :table-rows rows
   :table-columns columns
   :cells
   (->> (for [i (take rows (range 97 123))
              :let [c (char i)]]
          (for [j (range 1 (inc columns))]
            {:value ""
             :location (keyword (str c j))
             :focused? false}))
        flatten
        (sort-by
          (fn [{:keys [location]}]
            (let [loc-str (name location)
                  row (re-find #"[0-9]+" loc-str)
                  column (re-find #"[a-z]+" loc-str)]
              [row column]))))})
