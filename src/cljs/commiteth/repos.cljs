(ns commiteth.repos
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [commiteth.svg :as svg]))


(defn repo-toggle-button [enabled busy on-click]
  (let [add-busy-styles (fn [x] (conj x (when busy {:class (str "busy loading")})))
        button (if enabled
                 [:div.ui.button.small.repo-added-button (add-busy-styles {})
                  [:i.icon.check]
                  "ADDED"]
                 [:div.ui.button.small.add-repo-button
                  (add-busy-styles {:on-click on-click})
                  "ADD"])]

    [:div.ui.two.column.container
     button
     (when enabled
       [:a.ui.item.remove-link {:on-click on-click} "REMOVE"])]))


(defn repo-card [repo]
  [:div.ui.card
   [:div.content
    [:div.repo-label [:a {:href (:html_url repo)} (:full_name repo)]
     (when  (:fork repo)
       [:span.fork-span [svg/github-fork-icon]])]
    [:div.repo-description (:description repo)]]

   [:div.repo-button-container
    [repo-toggle-button
     (:enabled repo)
     (:busy? repo)
     #(rf/dispatch [:toggle-repo repo])]]])

(defn repo-group-title [group login]
  [:h3
   (if (= group login)
     "Personal repositories"
     group)])


(defn show-forks-checkbox [show-atom]
  [:div.ui.container
   [:div.ui.checkbox.commiteth-toggle
    [:input (merge {:type :checkbox
                    :on-change #(swap! show-atom not)}
                   (when @show-atom
                     {:checked :checked}))]
    [:label "Show forks"]]])


(defn repos-list []
  (let [repos (rf/subscribe [:repos])
        user (rf/subscribe [:user])
        show-atom (r/atom false)]
    (fn []
      (let [repo-groups (sort-by identity (fn [a _] (= a (:login @user)))
                                 (keys @repos))
            filter-fn (if @show-atom
                        identity
                        (fn [repo]
                          (not (:fork repo))))]
        (into [:div
               [show-forks-checkbox show-atom]]
              (for [[group group-repos]
                    (map (fn [group] [group (get @repos group)])
                         repo-groups)]
                [:div.repo-group-title [repo-group-title group (:login @user)]
                 (let [filtered-group-repos (filter filter-fn group-repos)]
                   (if (empty? filtered-group-repos)
                     [:div.ui.text "No data"]
                     (into [:div.ui.cards]
                           (map repo-card
                                filtered-group-repos))))]))))))

(defn repos-page-token-ok []
  (println "repos-token-ok")
  (let [repos-loading? (rf/subscribe [:repos-loading?])]
    (fn []
      (if @repos-loading?
        [:div.view-loading-container
         [:div.ui.active.inverted.dimmer
          [:div.ui.text.loader.view-loading-label "Loading"]]]
        [repos-list]))))

(defn repos-page []
  (let [gh-admin-token (rf/subscribe [:gh-admin-token])]
    (fn []
      (println "gh-admin-token" @gh-admin-token)
      (if (empty? @gh-admin-token)
        [:div.ui.container.enable-github-account
         [:div.ui.center.aligned.segment.enable-github-account-title
          "Work on bounties that suit you"]
         [:div.ui.center.aligned.segment.enable-github-account-description
          [:p
           "Not all projects are created equally.  Choose the bounties that excite you, match your skill set or fit in with your schedule. No strings attached.  Enable your GitHub repositories to get started."]]
         [:div.ui.center.aligned.segment
          [:a.ui.button.small {:href js/authorizeUrlAdmin} "ENABLE GITHUB ACCOUNT"]]]
        (do
          (rf/dispatch [:load-user-repos])
          [repos-page-token-ok])))))
