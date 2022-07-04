(ns kees.b01.main
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [kees.b01.rf :as rf :refer [>evt-now]]
   [kees.b01.views :as views]
   [kees.b01.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (>evt-now [::rf/boot])
  (dev-setup)
  (mount-root))
