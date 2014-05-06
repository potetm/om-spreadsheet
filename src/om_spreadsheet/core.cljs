(ns om-spreadsheet.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout]]
            [clojure.string :as str]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om :include-macros true]
            [om-spreadsheet.state :as state]
            [sablono.core :as s :include-macros :refer [html]]))

(enable-console-print!)

(def app-state (atom state/initial-state))
(declare calculate-value)

(defn get-loc-str-value-tuple [db loc-str]
  (let [cell-loc (keyword (str/replace-first loc-str "$" ""))
        cell (d/entity db (persist/get-cell-by-location db cell-loc))]
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
    (if (:cell/focused? cell)
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

(defn cell [cell owner]
  (reify
    ;;    om/IDidUpdate
    #_   (did-update [this _ {:keys [id]}]
         #_(when (= id (persist/get-focused-cell db))
           (.focus (om/get-node owner))))
    om/IRender
    (render [_]
      (html
        [:input
         {:type "text"
          ;; :on-focus #(repo/set-cell-focused! owner id true)
          ;; :on-blur #(repo/set-cell-focused! owner id false)
          ;; :on-key-press (partial handle-cell-key-press db owner id)
          ;; :on-change #(repo/update-cell-value! owner id (-> % .-target .-value))
          :value (:value cell)}]))))

(defn table-header-row [column-count _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:thead
         [:tr
          [:th]
          (for [i (range column-count)]
            [:th (char (+ 97 i))])]]))))

(defn table-rows [{:keys [cells table-columns]} _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:tbody
         (for [[i cells] (map-indexed vector (partition table-columns cells))]
           [:tr
            [:th (inc i)]
            (for [c cells]
              [:td (om/build cell c)])])]))))

(defn spreadsheet-app [app _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:h1 (:header-text app)]
         [:table
          (om/build table-header-row (:table-columns app))
          (om/build table-rows {:table-columns (:table-columns app)
                                :cells (:cells app)})]]))))

(om/root
  spreadsheet-app app-state
  {:target (js/document.getElementById "app")})

(fw/watch-and-reload)
