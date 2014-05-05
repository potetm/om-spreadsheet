(ns om-spreadsheet.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout]]
            [clojure.string :as str]
            [datascript :as d]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om :include-macros true]
            [om-spreadsheet.state :as state]
            [om-spreadsheet.persistence :as persist]
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

(declare calculate-value)

(defn get-loc-str-value-tuple [db loc-str]
  (let [cell-loc (keyword (str/replace-first loc-str "$" ""))
        cell (d/entity db (repo/get-cell-by-location db cell-loc))]
    [loc-str
     (calculate-value db (:cell/value cell))]))

(defn eval-expr [v]
  (if (re-matches #".*[-+*/].*" v)
    (js/eval v) ;; da da da DANGEROUS
    v))

(defn eval-function [db value]
  (let [value (str/replace-first value "=" "")]
    (->> value
         (re-seq #"\$[A-Za-z]+[0-9]+")
         (map (partial get-loc-str-value-tuple db))
         (reduce
           (fn [input [loc-str v]]
             (str/replace input loc-str v))
           value)
         eval-expr)))

(defn calculate-value [db value]
  (if (= \= (first value))
    (eval-function db value)
    value))

(defn display-value [db id]
  (let [cell (d/entity db id)
        value (:cell/value cell)]
    (if (= :focused (:cell/state cell))
      value
      (calculate-value db value))))

(defn get-column-row-tuple [db id]
  (->> (d/entity db id)
       :cell/location
       name
       (re-seq #"[a-z]+|[0-9]+")))

(defn decrement-row [db id]
  (let [[c r] (get-column-row-tuple db id)]
    (keyword (str c (dec (js/parseInt r))))))

(defn increment-row [db id]
  (let [[c r] (get-column-row-tuple db id)]
    (keyword (str c (inc (js/parseInt r))))))

(defn handle-cell-key-press [db owner id e]
  (when (= 13 (.-keyCode e))
    (if (.-shiftKey e)
      (repo/set-focus-to-cell-at-location! db owner (decrement-row db id))
      (repo/set-focus-to-cell-at-location! db owner (increment-row db id)))))

(defn cell [db owner]
  (reify
    om/IDidUpdate
    (did-update [this _ {:keys [id]}]
      (when (= id (persist/get-focused-cell db))
        (.focus (om/get-node owner))))
    om/IRenderState
    (render-state [this {:keys [id]}]
      (html
        [:input
         {:type "text"
          :on-focus #(repo/set-cell-state! owner id :focused)
          :on-blur #(repo/set-cell-state! owner id :unfocused)
          :on-key-press (partial handle-cell-key-press db owner id)
          :on-change #(repo/update-cell-value! owner id (-> % .-target .-value))
          :value (display-value db id)}]))))

(defn table-header-row [_db _owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [col-count]}]
      (html
        [:thead
         [:tr
          [:th]
          (for [i (range col-count)]
            [:th (char (+ 97 i))])]]))))

(defn table-rows [db _owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [col-count]}]
      (html
        [:tbody
         (for [[i ids] (->> (persist/get-sorted-cells db)
                            (partition col-count)
                            (map-indexed vector))]
           [:tr
            [:th (inc i)]
            (for [id ids]
              [:td (om/build cell db {:init-state {:id id}})])])]))))

(defn spreadsheet-app [db _owner]
  (reify
    om/IRender
    (render [_]
      (let [col-count (persist/get-column-count db)]
        (html
          [:div
           [:h1 (persist/get-header-text db)]
           [:table
            (om/build table-header-row db {:init-state {:col-count col-count}})
            (om/build table-rows db {:init-state {:col-count col-count}})]])))))

(om/root
  spreadsheet-app conn
  {:shared {:conn conn}
   :target (js/document.getElementById "app")})

(fw/watch-and-reload)
