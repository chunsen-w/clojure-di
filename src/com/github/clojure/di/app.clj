(ns com.github.clojure.di.app
  (:require [com.github.clojure.di.core :refer [execute] :as di]
            [com.github.clojure.di.util :as util]
            [com.github.clojure.di.ns :as dns]
            [clojure.tools.logging :as log]))

(defn scan-components
  "scan the namespace, return all the di component defined by defdi in this namespace"
  [the-ns]
  (when-let [the-ns (find-ns (symbol the-ns))]
    (->> (ns-interns the-ns)
         vals
         (filter (partial util/has-meta :clj-di/di))
         (map var-get))))

(defn bootstrap
  "bootstrap the components in the namespace  
   args:  
     - ns-prefix-or-patterns - a vector of namespace prefix or pattern  
   
   opts:
    - :additional-comps - a list of var representing the additional components beside auto scan added
    - :init-roots see execute"
  ([ns-prefix-or-patterns] (bootstrap ns-prefix-or-patterns {} {}))
  ([ns-prefix-or-patterns init-ctx] (bootstrap ns-prefix-or-patterns init-ctx {}))
  ([ns-prefix-or-patterns init-ctx opts]
   (let [namespaces (dns/scan-ns (if (or (seq? ns-prefix-or-patterns)
                                         (vector? ns-prefix-or-patterns))
                                   ns-prefix-or-patterns
                                   [ns-prefix-or-patterns]))]
     (doseq [n namespaces]
       (log/info "loading ns " n " ...")
       (require (symbol n)))
     (let [components (concat
                       (mapcat scan-components namespaces)
                       (map var-get (:additional-comps opts)))]
       (execute components init-ctx opts)))))

(comment
  (macroexpand
   '(def-di  aaa
      "this is a commnet"
      [{:keys [^::vec age port]
        :or {port 8080}}] {:user-name ""}))
  ;
  )