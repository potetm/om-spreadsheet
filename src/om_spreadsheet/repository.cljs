(ns om-spreadsheet.repository
  (:require [datascript :as d]))

(defn get-header-text [db]
  (ffirst
    (d/q
      '[:find ?t
        :where
        [?i :header/text ?t]]
      db)))

(defn first-id-for-attr [db attr]
  (ffirst
    (d/q
      '[:find ?i
        :in $ ?attr
        :where
        [?i ?attr]]
      db
      attr)))
