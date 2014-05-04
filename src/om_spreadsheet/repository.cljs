(ns om-spreadsheet.repository
  (:require [datascript :as d]))

(defn get-header-text [db]
  (ffirst
    (d/q
      '[:find ?t
        :where
        [?i :header/text ?t]]
      db)))

(defn get-sorted-cells [db]
  (->> (d/q
         '[:find ?i ?l
           :where
           [?i :cell/location ?l]]
         db)
       (sort-by second)
       (map first)))

(defn get-cell-by-location [db location]
  (ffirst
    (d/q
      '[:find ?i
        :in $ ?l
        :where
        [?i :cell/location ?l]]
      db
      location)))
