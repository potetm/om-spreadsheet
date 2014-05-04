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

(defn eval-function [value]
  (-> (str/replace-first value "=" "")))

(defn display-value [db id]
  (let [value (:cell/value (d/entity db id))]
    (cond
      (= \= (first value)) (eval-function value)
      :else
      value)))

(defn cell [db owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [id]}]
      (html
        [:input {:type "text"
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
