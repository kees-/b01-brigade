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
   [:span.f.ing-name name]
   [:span.f.quantity quantity-string]
   [:span.f.scaling scaling-string]])

(defn- procedure->hiccup [text]
  [:div.fr.w100.procedure
   [:span.step-number "①"]
   [:span.step-text text]])

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
     [:span.recipe-title (:title meta)]
     [:article (:servings meta)]]
    [:div.recipe-body.fc.fg1.w100
     [:div.col-labels.w100
      [:article.header-ing "INGREDIENT"]
      [:article.header-qua "QUANTITY"]
      [:article.header-sca "SCALING"]
      [:article.header-art "PROCEDURE"]]
     [:hr]
     (->> sections
          vals
          (map section->hiccup)
          (interpose [:hr])
          (into [:div.fc.sections]))
     [:hr]]
    [:div#recipe-footer
     [:article (:source meta)]]]])

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
