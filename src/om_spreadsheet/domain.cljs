(ns om-spreadsheet.domain
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
       (sort-by
         (fn [[_id loc]]
           (let [s (name loc)
                 row (re-find #"[0-9]+" s)
                 column (re-find #"[a-z]+" s)]
             [row column])))
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

(defn get-row-count [db]
  (ffirst
    (d/q
      '[:find ?r
        :where
        [?i :table/rows ?r]]
      db)))

(defn get-column-count [db]
  (ffirst
    (d/q
      '[:find ?c
        :where
        [?i :table/columns ?c]]
      db)))

(defn get-focused-cell [db]
  (ffirst
    (d/q
      '[:find ?c
        :where
        [?i :focused-cell ?c]]
      db)))

(defn get-focused-attr [db]
  (ffirst
    (d/q
      '[:find ?i
        :where
        [?i :focused-cell]]
      db)))

(defn get-cells-focus-update-facts [db id focused?]
  (let [focused-attr-id (if-let [id (get-focused-attr db)] id -1)]
    (if focused?
      [[:db/add focused-attr-id :focused-cell id]]
      [[:db/retract focused-attr-id :focused-cell id]])))
