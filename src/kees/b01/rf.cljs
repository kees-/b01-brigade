(ns kees.b01.rf
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx path]]
            [clojure.string :as s]
            [kees.b01.output :as output]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def <sub-lazy re-frame/subscribe)
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)

(def default-db
  {:ui
   {:text-output ""
    :console (vector)}
   :active-recipe
   {:meta {:title ""
           :servings ""
           :source ""}
    :sections (sorted-map)}})

(defn default-ingredient
  [iid]
  {:iid iid
   :name ""
   :quantity ""
   :unit "g"
   :other ""
   :scaling :auto
   :custom-scale ""
   :scalar? false})

;; ========== ASSISTANTS =======================================================
;; Taken from re-frame/todomvc
(defn next-id
  "Returns the next todo id.
  Assumes todos are sorted.
  Returns one more than the current largest id."
  [todos]
  ((fnil inc 0) (last (keys todos))))

(reg-fx
 :save
 (fn [] (output/save)))

(reg-fx
 :render-output
 (fn [[id data]]
   (output/render-to-output id data)))

;; ========== EVENTS ===========================================================
(reg-event-db
 ::boot
 (fn [_ _]
   default-db))

(reg-event-db
 ::recipe-metadata
 (path [:active-recipe :meta])
 (fn [meta [_ metadata value]]
   (assoc meta metadata value)))

(reg-event-db
 ::add-section
 (path [:active-recipe :sections])
 (fn [sections _]
   (let [sid (next-id sections)]
     (assoc sections sid {:sid sid
                          :ingredients (sorted-map)
                          :procedures (sorted-map)}))))

(reg-event-db
 ::remove-section
 (path [:active-recipe :sections])
 (fn [sections [_ sid]]
   (dissoc sections sid)))

(reg-event-db
 ::add-procedure
 (path [:active-recipe :sections])
 (fn [sections [_ sid]]
   (let [pid (next-id (get-in sections [sid :procedures]))]
     (assoc-in sections [sid :procedures pid] {:pid pid :value nil}))))

(reg-event-db
 ::edit-procedure
 (path [:active-recipe :sections])
 (fn [sections [_ sid pid val]]
   (assoc-in sections [sid :procedures pid :value] val)))

(reg-event-db
 ::remove-procedure
 (path [:active-recipe :sections])
 (fn [sections [_ sid pid]]
   (update-in sections [sid :procedures] dissoc pid)))

(reg-event-db
 ::add-ingredient
 (path [:active-recipe :sections])
 (fn [sections [_ sid]]
   (let [iid (next-id (get-in sections [sid :ingredients]))]
     (assoc-in sections [sid :ingredients iid] (default-ingredient iid)))))

(reg-event-db
 ::edit-ingredient
 (path [:active-recipe :sections])
 (fn [sections [_ sid iid key val]]
   (assoc-in sections [sid :ingredients iid key] val)))

(reg-event-db
 ::remove-ingredient
 (path [:active-recipe :sections])
 (fn [sections [_ sid iid]]
   (update-in sections [sid :ingredients] dissoc iid)))

(reg-event-db
 ::set-as-scalar
 (path [:active-recipe :sections])
 (fn [sections [_ sid iid]]
   (-> sections
       (update-vals
        (fn [section]
          (update
           section
           :ingredients
           (fn [ingredients]
             (update-vals
              ingredients
              (fn [ingredient] (assoc ingredient :scalar? false)))))))
       (assoc-in [sid :ingredients iid :scalar?] true))))

(reg-event-db
 ::send-to-text-output
 (path [:ui])
 (fn [ui [_ value]]
   (assoc ui :text-output (str value))))

(reg-event-db
 ::log
 (path [:ui])
 (fn [console [_ message]]
   (let [message (str "> [" (output/now) "] " message)]
     (update console :console conj message))))

(reg-event-db
 ::clear-console
 (path [:ui])
 (fn [ui _]
   (assoc ui :console (vector))))

(reg-event-fx
 ::recipe->pdf
 (fn [_ [_ data]]
   {:fx [[:render-output ["output" data]]
         [:save nil]
         [:dispatch [::log "Rendering for saving or printing"]]]}))

;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::recipe-metadata
 (fn [db]
   (-> db :active-recipe :meta)))

(reg-sub
 ::sections
 (fn [db]
   (-> db :active-recipe :sections)))

(reg-sub
 ::procedure-value
 (fn [db [_ sid pid]]
   (get-in db [:active-recipe :sections sid :procedures pid :value])))

(reg-sub
 ::ingredient-values
 (fn [db [_ sid iid]]
   (get-in db [:active-recipe :sections sid :ingredients iid])))

(reg-sub
 ::is-scalar?
 (fn [db [_ sid iid]]
   (get-in db [:active-recipe :sections sid :ingredients iid :scalar?])))

(reg-sub
 ::text-output
 (fn [db _]
   (-> db :ui :text-output)))

(reg-sub
 ::ingredient-unit-other?
 (fn [db [_ sid iid]]
   (= "other"
      (get-in db [:active-recipe :sections sid :ingredients iid :unit]))))

(reg-sub
 ::ingredient-unit-none?
 (fn [db [_ sid iid]]
   (= "none"
      (get-in db [:active-recipe :sections sid :ingredients iid :unit]))))

(reg-sub
 ::console
 (fn [{{:keys [console]} :ui} _]
   (->> console
        (s/join \newline))))

;; ========== DATA OUTPUT ======================================================
(reg-sub
 ::form->data
 (fn [db _]
   (let [data (:active-recipe db)]
     (-> data
         (update
          :sections
          (fn [sections]
            (update-vals
             sections
             (fn [section]
               (-> section
                   (update
                    :ingredients
                    (fn [ingredients]
                      (update-vals
                       ingredients
                       (fn [ingredient]
                         (-> ingredient
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
                             (assoc
                              :scaling-string
                              (if (:scalar? ingredient)
                                "100%"
                                (case (:scaling ingredient)
                                  :auto "x%"
                                  :override (str (:custom-scale ingredient) "%")
                                  "")))
                             (dissoc :iid :scalar? :scaling :custom-scale :quantity :unit :other))))))
                   (update
                    :procedures
                    (fn [procedures]
                      (update-vals
                       procedures
                       (fn [procedure]
                         (:value procedure)))))
                   (dissoc :sid))))))))))
