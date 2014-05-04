(ns om-spreadsheet.repository
  (:require [datascript :as d]
            [om.core :as om]
            [om-spreadsheet.persistence :as persist]))

(defn get-conn [owner]
  (om/get-shared owner :conn))

(defn update-cell-value! [owner id value]
  (d/transact! (get-conn owner)
               [[:db/add id :cell/value value]]))

(defn set-cell-state! [owner id state]
  (d/transact! (get-conn owner)
               (persist/get-cells-state-update-facts id state)))

(defn set-focus-to-cell-at-location! [db owner new-location]
  (let [new-id (persist/get-cell-by-location db new-location)]
    (when new-id
      (d/transact! (get-conn owner)
                   (persist/get-cells-state-update-facts new-id :focused)))))
