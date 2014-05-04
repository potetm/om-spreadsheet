(ns om-spreadsheet.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout]]
            [datascript :as d]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om :include-macros true]
            [om-spreadsheet.state :as state]
            [om-spreadsheet.repository :as repo]
            [sablono.core :as s :include-macros :refer [html]]))

(enable-console-print!)

(def conn (d/create-conn state/schema))
(def initialize
  (d/transact! conn state/initial-facts))

;; prevent cursor-ification
(extend-type d/DB
  om/IToCursor
  (-to-cursor
    ([this _] this)
    ([this _ _] this)))

(defn get-conn [owner]
  (om/get-shared owner :conn))

(defn update-cell-value! [owner id e]
  (d/transact! (get-conn owner)
               [[:db/add id :cell/value (-> e .-target .-value)]]))

(defn cell [db owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [id]}]
      (html
        [:input {:type "text"
                 :on-change (partial update-cell-value! owner id)
                 :value (:cell/value (d/entity db id))}]))))

(defn spreadsheet-app [db owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:h1 (repo/get-header-text db)]
         [:h2 (:cell/value (d/entity db 2))]
         (om/build cell db {:init-state {:id 2}})
         (om/build cell db {:init-state {:id 2}})]))))

(om/root
  spreadsheet-app conn
  {:shared {:conn conn}
   :target (js/document.getElementById "app")})

(fw/watch-and-reload)
