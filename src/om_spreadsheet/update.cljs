(ns om-spreadsheet.update
  (:require [datascript :as d]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om]
            [om-spreadsheet.domain :as domain]
            [om-spreadsheet.state :as state]))

(fw/defonce conn (d/create-conn state/schema))

(defn update-cell-value! [id value]
  (d/transact! conn [[:db/add id :cell/value value]]))

(defn set-cell-focused! [id focused?]
  (d/transact! conn (domain/get-cells-focus-update-facts id focused?)))

(defn set-focus-to-cell-at-location! [db new-location]
  (let [new-id (domain/get-cell-by-location db new-location)]
    (when new-id
      (d/transact! conn (domain/get-cells-focus-update-facts new-id true)))))
