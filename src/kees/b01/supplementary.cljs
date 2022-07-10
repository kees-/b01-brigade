(ns kees.b01.supplementary)

(defn todo
  []
  [:<>
   [:hr]
   [:div#todo
    [:h2 "To do"]
    [:ul
     [:li
      [:h4 "Features"]
      [:ul
       [:li "Rearrange sections by grabbing"]
       [:li "Deal with input list focuses (make an event to focus " [:code "(inc id)"] " e.g)"]
       [:li "Cut off sections at 50"]]]
     [:li
      [:h4 "Fixes"]
      [:ul
       [:li.struck "Fix lagging text inputs"]
       [:li "Correct numbering"]
       [:li "Calculate scaling values"]
       [:li "Add conditional logic to not delete single line item (flashing before re-created)"]]]
     [:li
      [:h4 "Code organization / performance"]
      [:ul
       [:li.struck "Split " [:code "none"] " to " [:code "no unit"] " and " [:code "no quantity"]]
       [:li.struck "Incorporate re-frame " [:code "path"] " logic"]
       [:li "Implement re-com"]
       [:li "Do the async startup flow"]
       [:li.struck "Test " [:code "case"] " vs " [:code "cond"]]]]]]])
