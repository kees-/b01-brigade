(ns kees.b01.output
  (:require [reagent.dom :as rdom]
            [clojure.string :as s]
            [goog.date]
            ["print-js" :as printer]))

;; ========== SHAPING DATA =====================================================
; Unicode values for circled numbers 1-50
(def ^:private steps
  [9450 ; Unused 0 for ease of indexing
   9312  9313  9314  9315  9316  9317  9318  9319  9320  9321
   9322  9323  9324  9325  9326  9327  9328  9329  9330  9331
   12881 12882 12883 12884 12885 12886 12887 12888 12889 12890
   12891 12892 12893 12894 12895 12977 12978 12979 12980 12981
   12982 12983 12984 12985 12986 12987 12988 12989 12990 12991])

(defn state->data
  [data]
  (let [renumber-sids (fn [m]
                        (->> m
                             vals
                             (#(zipmap (take (count %) (iterate inc 1)) %))
                             (into (sorted-map))))
        renumber-pids (fn [m]
                        (merge m
                               (loop [out m
                                      step 0
                                      in (seq m)]
                                 (if (empty? in)
                                   out
                                   (let [[sid value] (first in)
                                         c (-> value :procedures count)
                                         cs (take c (iterate inc (inc step)))
                                         pals (-> value :procedures vals)
                                         news (zipmap cs pals)
                                         boop (println news)]
                                     (recur
                                      (assoc-in out [sid :procedures] news)
                                      (+ step c)
                                      (rest in)))))))]
    (-> data
        (update
         :sections
         ; Change :sections with this fn:
         (fn [sections]
           (-> sections
               renumber-sids
               renumber-pids
               (update-vals
            ; Change EACH value of :sections with this fn:
                (fn [section]
                  (-> section
                      (update
                       :ingredients
                   ; Change :ingredients with this fn:
                       (fn [ingredients]
                         (update-vals
                          ingredients
                      ; Change EACH value of :ingredients with this fn:
                          (fn [ingredient]
                            (-> ingredient
                            ; Parse :quantity and :unit of ONE ingredient to :quantity-string
                                (assoc
                                 :quantity-string
                                 (-> (str (when-not (= "none" (:unit ingredient))
                                            (:quantity ingredient))
                                          " "
                                          (case (:unit ingredient)
                                            "no unit" nil
                                            "none" nil
                                            "other" (:other ingredient)
                                            (name (:unit ingredient))))
                                     s/trim))
                            ; Parse :scaling and :scalar? of ONE ingredient to :scaling-string
                                (assoc
                                 :scaling-string
                                 (if (:scalar? ingredient)
                                   "100%"
                                   (case (:scaling ingredient)
                                     :auto "x%"
                                     :override (str (:custom-scale ingredient) "%")
                                     "")))
                            ; Remove unneeded keys
                                (dissoc :iid :scalar? :scaling :custom-scale :quantity :unit :other))))))
                      (update
                       :procedures
                   ; Change :procedures with this fn:
                       (fn [procedures]
                         (update-vals
                          procedures
                      ; Change EACH value of :procedures with this fn:
                          (fn [procedure]
                            (:value procedure)))))
                      (dissoc :sid))))))))))

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
