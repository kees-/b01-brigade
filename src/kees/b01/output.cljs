(ns kees.b01.output
  (:require [reagent.dom :as rdom]
            ["print-js" :as printer]
            [goog.date]))

;; ========== LOGGING ==========================================================
(defn now
  []
  (-> (goog.date.DateTime.)
      .toIsoTimeString))

;; ========== PRINTING OUTPUT ==================================================
(defn- ingredient->hiccup
  [{:keys [name quantity-string scaling-string]}]
  [:div.fr.ingredient.w100
   [:div.ing-name name]
   [:div.quantity quantity-string]
   [:div.scaling scaling-string]])

(defn- procedure->hiccup [text]
  [:div.fr.w100.procedure
   [:div.step-number "①"]
   [:div.step-text text]])

(defn- section->hiccup
  [{:keys [ingredients procedures]}]
  [:div.fr.w100
   (into [:div.ingredients.fc]
         (for [[iid ingredient] ingredients]
           ^{:key iid}
           [ingredient->hiccup ingredient]))
   (into [:div.procedures.fc]
         (for [[pid procedure] procedures]
           ^{:key pid}
           [procedure->hiccup procedure]))])

(defn- data->html
  [{:keys [meta sections]}]
  [:<>
   [:div.flex-root.fc
    [:div.recipe-header.fg1
     [:div.recipe-title (:title meta)]
     [:div.yield (:yield meta)]]
    [:div.recipe-body.fc.fg1.w100
     [:div.col-labels.w100
      [:div.header-ing "INGREDIENT"]
      [:div.header-qua "QUANTITY"]
      [:div.header-sca "SCALING"]
      [:div.header-art "PROCEDURE"]]
     [:hr]
     (->> sections
          vals
          (map section->hiccup)
          (interpose [:hr])
          (into [:div.fc.sections]))
     [:hr]]
    [:div.recipe-footer
     [:div.recipe-source (:source meta)]]]])

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
