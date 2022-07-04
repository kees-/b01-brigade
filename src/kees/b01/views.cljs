(ns kees.b01.views
  (:require
   [clojure.string :as s]
   [kees.b01.rf :as rf :refer [<sub <sub-lazy >evt]]))

;; ========== UTILITIES ========================================================
(defn blur
  [e]
  (-> e .-target .blur))

(def unit-opts
  ["g" "oz" "lb" "fl oz" "ml" "other" "none"])

(def scaling-opts
  ["auto" "override" "none"])

;; ========== DOM ==============================================================
(defn recipe-head
  []
  (let [{:keys [title servings]} (<sub [::rf/recipe-metadata])]
    [:div#recipe-head.fit
     [:input
      {:type "text"
       :value title
       :on-change #(>evt [::rf/recipe-metadata :title (-> % .-target .-value)])
       :placeholder "Recipe name"}]
     [:input
      {:type "text"
       :value servings
       :on-change #(>evt [::rf/recipe-metadata :servings (-> % .-target .-value)])
       :placeholder "Servings"}]]))

(defn recipe-ingredient
  [sid {:keys [iid]}]
  (let [{:keys [name quantity unit]} (<sub [::rf/ingredient-values sid iid])]
    [:li
     [:input
      {:type "text"
       :placeholder "Name"
       :value name
       :on-change #(>evt [::rf/edit-ingredient sid iid :name (-> % .-target .-value)])
       :on-key-down #(case (.-keyCode %)
                       nil)}]
     [:input.quantity-input
      {:type "text"
       :placeholder "Quantity"
       :value quantity
       :on-change #(>evt [::rf/edit-ingredient sid iid :quantity (-> % .-target .-value)])
       :on-key-down #(case (.-keyCode %)
                       nil)}]
     [:select
      {:on-change #(>evt [::rf/edit-ingredient sid iid :unit (-> % .-target .-value (s/replace \space \-) keyword)])}
      (for [unit-opt unit-opts]
        ^{:key unit-opt} [:option {:value unit-opt} unit-opt])]
     [:select
      {:on-change #(>evt [::rf/edit-ingredient sid iid :scaling (-> % .-target .-value keyword)])}
      (for [scaling-opt scaling-opts]
        ^{:key scaling-opt} [:option {:value scaling-opt} scaling-opt])]
     [:button.remove-ingredient
      {:on-click #(>evt [::rf/remove-ingredient sid iid])}
      "x"]]))

(defn recipe-procedure
  [sid {:keys [pid]}]
  (let [val (<sub-lazy [::rf/procedure-value sid pid])
        final (<sub [::rf/final-procedure-value sid])
        add-if-val (fn [e] (when final (>evt [::rf/add-procedure sid pid])))]
    [:li
     [:input
      {:type "text"
       :placeholder "Next procedure..."
       :value @val
       :on-change #(>evt [::rf/edit-procedure sid pid (-> % .-target .-value)])
       :on-key-down #(case (.-keyCode %)
                       13 (add-if-val %)
                       27 (blur %)
                       nil)}]
     [:button.remove-procedure
      {:on-click #(>evt [::rf/remove-procedure sid pid])}
      "x"]]))

(defn recipe-section
  []
  (let []
    (fn [{:keys [sid ingredients procedures]}]
      (when-not (seq procedures)
        (>evt [::rf/add-procedure sid]))
      (when-not (seq ingredients)
        (>evt [::rf/add-ingredient sid]))
      [:tr
       [:td>ul.ingredients
        (for [ingredient ingredients]
          ^{:key (first ingredient)} [recipe-ingredient sid (last ingredient)])
        [:button.add-ingredient
         {:on-click #(>evt [::rf/add-ingredient sid])}
         "+"]]
       [:td>ul.procedures
        (for [procedure procedures]
          ^{:key (first procedure)} [recipe-procedure sid (last procedure)])
        [:button.add-procedure
         {:on-click #(>evt [::rf/add-procedure sid])}
         "+"]]
       [:td>button.remove-section
        {:on-click #(>evt [::rf/remove-section sid])}
        "x"]])))

(defn recipe-foot
  []
  (let [{:keys [source]} (<sub [::rf/recipe-metadata])]
    [:div#recipe-foot
     [:input
      {:type "text"
       :value source
       :on-change #(>evt [::rf/recipe-metadata :source (-> % .-target .-value)])
       :placeholder "Recipe source"}]]))

(defn active-recipe
  []
  (let [sections (<sub [::rf/sections])]
    (when-not (seq sections)
      (>evt [::rf/add-section]))
    [:div#active-recipe.bor.fit
     [recipe-head]
     [:hr]
     (when (seq sections)
       [:table#sections
        [:thead>tr [:th "Ingredients"] [:th "Procedures"] [:th]]
        [:tbody
         (for [section sections]
           ^{:key (first section)} [recipe-section (last section)])]])
     [:button#add-section
      {:on-click #(>evt [::rf/add-section])}
      "Add section"]
     [:hr]
     [recipe-foot]]))

(defn main-panel []
  (let [title "re-frame in July"]
    [:<>
     [:header
      [:h1 title]
      [:hr]]
     [:main
      [active-recipe]
      [:div
       [:button
        {:on-click #()}
        "Form->data"]
       [:button
        {:on-click #(js/console.log "Placeholder for downloading PDF")}
        "Download PDF"]]]
     [:footer
      [:hr]
      [:ul
       [:li "Incorporate re-frame " [:code "path"] " logic"]
       [:li "Fix lagging text inputs"]
       [:li "Do the async startup flow"]
       [:li "Test " [:code "case"] " vs " [:code "cond"]]
       [:li "Add conditional logic to not delete single line item"]
       [:li "Deal with input list focuses (make an event to focus " [:code "(inc id)"] " e.g)"]]]]))
