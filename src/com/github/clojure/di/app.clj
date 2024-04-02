(ns com.github.clojure.di.app
  (:require [com.github.clojure.di.core :refer [execute]
             :as di]
            [com.github.clojure.di.util :as util]
            [com.github.clojure.di.ns :as dns]))

(defn scan-components
  "scan the namespace, return all the di component defined by defdi in this namespace"
  [the-ns]
  (when-let [the-ns (find-ns (symbol the-ns))]
    (->> (ns-interns the-ns)
         vals
         (filter (partial util/has-meta ::di/di)))))

(defn bootstrap [ns-prefix init-ctx opts]
  (let [namespaces (dns/scan-ns ns-prefix)]
    (doseq [n namespaces]
      (println "loading " n " ......")
      (require (symbol n)))
    (let [components (mapcat scan-components namespaces)]
      (execute components init-ctx opts))))

(comment
  (macroexpand
   '(def-di  aaa
      "this is a commnet"
      [{:keys [^::vec age port]
        :or {port 8080}}] {:user-name ""}))
  ;
  )