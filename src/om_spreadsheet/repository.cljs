(ns om-spreadsheet.repository
  (:require [datascript :as d]
            [om.core :as om]
            [om-spreadsheet.persistence :as persist]))

(defn get-conn [owner]
  (om/get-shared owner :conn))

(defn update-cell-value! [owner id value]
  (d/transact! (get-conn owner)
               [[:db/add id :cell/value value]]))

(defn set-cell-focused! [owner id focused?]
  (d/transact! (get-conn owner)
               (persist/get-cells-focus-update-facts id focused?)))

(defn set-focus-to-cell-at-location! [db owner new-location]
  (let [new-id (persist/get-cell-by-location db new-location)]
    (when new-id
      (d/transact! (get-conn owner)
                   (persist/get-cells-focus-update-facts new-id true)))))
