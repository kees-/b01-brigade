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
  {:active-recipe
   {:meta {:title ""
           :servings ""
           :source ""}
    :sections (sorted-map)}})

;; ========== ASSISTANTS =======================================================
;; Taken from re-frame/todomvc
(defn next-id
  "Returns the next todo id.
  Assumes todos are sorted.
  Returns one more than the current largest id."
  [todos]
  ((fnil inc 0) (last (keys todos))))

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
     (assoc-in
      sections
      [sid :ingredients iid]
      {:iid iid
       :name nil
       :quantity nil
       :unit "g"
       :other nil
       :scaling :auto
       :scalar? false}))))

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

;; ========== DATA OUTPUT ======================================================
(reg-sub
 ::form->data
 (fn [db _]
   #_{:clj-kondo/ignore [:redundant-let]}
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
                              (str (when-not (= "none" (:unit ingredient))
                                     (:quantity ingredient))
                                   (case (:unit ingredient)
                                     "no unit" nil
                                     "none" nil
                                     "other" (:other ingredient)
                                     (name (:unit ingredient)))))
                             (assoc
                              :scaling-string
                              (if (:scalar? ingredient)
                                "100%"
                                (case (:scaling ingredient)
                                  :auto "x%"
                                  :override "custom%"
                                  "")))
                             (dissoc :iid :scalar? :scaling :quantity :unit :other))))))
                   (update
                    :procedures
                    (fn [procedures]
                      (update-vals
                       procedures
                       (fn [procedure]
                         (:value procedure)))))
                   (dissoc :sid))))))))))
