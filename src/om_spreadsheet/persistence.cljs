(ns om-spreadsheet.persistence
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
      '[:find ?i
        :where
        [?i :cell/focused? true]]
      db)))

(defn get-cell-focused-facts [db id]
  (let [current-focused (get-focused-cell db)]
    (concat
      (when (and current-focused (not= current-focused id))
        [[:db/add current-focused :cell/focused? false]])
      [[:db/add id :cell/focused? true]])))

(defn get-cells-focus-update-facts [id focused?]
  (if focused?
    [[:db.fn/call get-cell-focused-facts id]]
    [[:db/add id :cell/focused? false]]))
