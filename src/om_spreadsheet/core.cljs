(ns om-spreadsheet.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! <! >! chan timeout]]
            [clojure.string :as str]
            [datascript :as d]
            [figwheel.client :as fw :include-macros true]
            [om.core :as om :include-macros true]
            [om-spreadsheet.state :as state]
            [om-spreadsheet.domain :as domain]
            [om-spreadsheet.update :as update]
            [sablono.core :as s :include-macros :refer [html]]))

(enable-console-print!)

(fw/defonce initialize
            (d/transact! update/conn state/initial-facts))

;; prevent cursor-ification
(extend-type d/DB
  om/IToCursor
  (-to-cursor
    ([this _] this)
    ([this _ _] this)))

(declare calculate-value)

(defn get-loc-str-value-tuple [db loc-str]
  (let [cell-loc (keyword (str/replace-first loc-str "$" ""))
        cell (d/entity db (domain/get-cell-by-location db cell-loc))]
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

(defn handle-cell-key-press [db id e]
  (when (= 13 (.-keyCode e))
    (if (.-shiftKey e)
      (update/set-focus-to-cell-at-location! db (decrement-row db id))
      (update/set-focus-to-cell-at-location! db (increment-row db id)))))

(defn cell [db owner]
  (reify
    om/IDidUpdate
    (did-update [this _ {:keys [id]}]
      (when (= id (domain/get-focused-cell db))
        (println "focused-cell=" id)
        (.focus (om/get-node owner))))
    om/IRenderState
    (render-state [this {:keys [id]}]
      (html
        [:input
         {:type "text"
          :on-focus #(update/set-cell-focused! db id true)
          :on-blur #(update/set-cell-focused! db id false)
          :on-key-press (partial handle-cell-key-press db id)
          :on-change #(update/update-cell-value! id (-> % .-target .-value))
          :value (display-value db id)}]))))

(defn table-header-row [db _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:thead
         [:tr
          [:th]
          (for [i (range (domain/get-column-count db))]
            [:th (char (+ 97 i))])]]))))

(defn table-rows [db _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:tbody
         (for [[i ids] (->> (domain/get-sorted-cells db)
                            (partition (domain/get-column-count db))
                            (map-indexed vector))]
           [:tr
            [:th (inc i)]
            (for [id ids]
              [:td (om/build cell db {:init-state {:id id}})])])]))))

(defn spreadsheet-app [db _owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:h1 (domain/get-header-text db)]
         [:table
          (om/build table-header-row db)
          (om/build table-rows db)]]))))

(om/root
  spreadsheet-app update/conn
  {:target (js/document.getElementById "app")})

(fw/watch-and-reload)
