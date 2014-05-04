(ns om-spreadsheet.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout]]
            [clojure.string :as str]
            [datascript :as d]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om :include-macros true]
            [om-spreadsheet.state :as state]
            [om-spreadsheet.repository :as repo]
            [sablono.core :as s :include-macros :refer [html]]))

(enable-console-print!)

(fw/defonce conn (d/create-conn state/schema))
(fw/defonce initialize
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

(defn set-cell-state! [owner id state]
  (d/transact! (get-conn owner)
               [[:db/add id :cell/state state]]))

(defn get-loc-str-value-tuple [db loc-str]
  (let [cell-loc (keyword (str/replace-first loc-str "$" ""))]
    [loc-str
     (:cell/value
      (d/entity db (repo/get-cell-by-location db cell-loc)))]))

(defn eval-function [db value]
  (let [value (str/replace-first value "=" "")]
    (->> value
         (re-seq #"\$[A-Za-z]+[0-9]+")
         (map (partial get-loc-str-value-tuple db))
         (reduce
           (fn [input [loc-str v]]
             (str/replace input loc-str v))
           value)
         ;; da da da DANGEROUS
         js/eval)))

(defn display-value [db id]
  (let [cell (d/entity db id)
        value (:cell/value cell)]
    (cond
      (= :focused (:cell/state cell)) value
      (= \= (first value)) (eval-function db value)
      :else
      value)))

(defn cell [db owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [id]}]
      (html
        [:input {:type "text"
                 :on-focus #(set-cell-state! owner id :focused)
                 :on-blur #(set-cell-state! owner id :unfocused)
                 :on-change (partial update-cell-value! owner id)
                 :value (display-value db id)}]))))

(defn spreadsheet-app [db _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:h1 (repo/get-header-text db)]
         [:div
          (for [id (repo/get-sorted-cells db)]
            (om/build cell db {:init-state {:id id}}))]]))))

(om/root
  spreadsheet-app conn
  {:shared {:conn conn}
   :target (js/document.getElementById "app")})

(fw/watch-and-reload)
