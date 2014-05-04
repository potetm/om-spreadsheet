(ns om-spreadsheet.state)

(def schema {})
(def initial-facts
  (concat
    [[:db/add -1 :header/text "Hello, World!"]]
    (mapcat
      identity
      (for [i (take 5 (range 97 123))
            :let [c (char i)]]
        (mapcat
          identity
          (for [j (range 1 6)
                :let [id (- -100 (+ i (* i j)))]]
            [[:db/add id :cell/value ""]
             [:db/add id :cell/location (keyword (str c j))]
             [:db/add id :cell/state :unfocused]]))))))
