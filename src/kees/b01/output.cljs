(ns kees.b01.output
  (:require [reagent.dom :as rdom]
            [cljs.pprint :refer [pprint]]
            ["print-js" :as printer]
            [goog.date]))

;; ========== LOGGING ==========================================================
(defn now
  []
  (-> (goog.date.DateTime.)
      .toIsoTimeString))

;; ========== PRINTING OUTPUT ==================================================
(defn- section->hiccup
  [{:keys [sid ingredients procedures]}]
  ; USELESS PLACEHOLDER
  [:p (-> procedures vals first)])

(defn- data->html
  [{:keys [meta sections] :as content}]
  [:<>
   [:div#flex-root
    [:div#recipe-header
     [:article (:title meta)]
     [:article (:servings meta)]]
    [:div#recipe-body
     [:div#col-labels
      [:article#header-ing "INGREDIENT"]
      [:article#header-qua "QUANTITY"]
      [:article#header-sca "SCALING"]
      [:article#header-art "PROCEDURE"]]
     [:hr]
     (->> sections
          vals
          (map section->hiccup)
          (into [:div#sections]))
     [:hr]]
    [:div#recipe-footer
     [:article (:source meta)]]]
   [:pre#data-sent
    {:style {:margin-top "4rem"}}
    (-> content pprint with-out-str)]])

(defn render-to-output
  [root data]
  (let [el (.getElementById js/document root)]
    (rdom/unmount-component-at-node el)
    (-> data
        data->html
        (rdom/render el))))

(defn save
  []
  (printer
   #js{:printable "output"
       :type "html"
       :css #js["/_css/reset.css" "/_css/printer.css"]
       :scanStyles false}))
