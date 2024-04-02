(ns com.github.clojure.di.core
  (:require [com.github.clojure.di.impl :as impl]
            [com.github.clojure.di.util :as util]))


(def ^:private opt-map
  {:clj-di/vec {:cast-fn
                (fn [v] (if (util/has-meta ::merged-vector v)
                          v
                          (conj [] v)))}})

(def ^:private di-name
  (let [index (atom 0)]
    (fn [prefix]
      (keyword (-> *ns* ns-name name) (str prefix "-" (swap! index inc))))))

(defmacro di [& fdecl]
  (let [[maybe-name args] (if (symbol? (first fdecl))
                            [(first fdecl) (fnext fdecl)]
                            [nil (first fdecl)])
        name (or
              (::di-name (meta &form))
              (when (symbol? maybe-name) (di-name (name maybe-name)))
              (di-name "di"))
        _ (assert (keyword? name))
        _ (assert (vector? args))
        _ (assert (= (count args) 1) "Component shoul have only 1 argument")
        deps (impl/parse-args (first args) opt-map)
        provide (impl/parse-resp (next fdecl))
        meta-map (merge
                  {::name name}
                  deps
                  provide)]
    `(with-meta (fn ~@fdecl) '~meta-map)))


(defmacro defdi [fn-name & fdecl]
  (let [comp-name (keyword (-> *ns* ns-name name) (name fn-name))
        m {::di true}
        m (if (string? (first fdecl))
            (assoc m :doc (first fdecl))
            m)
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)]
    `(def ~(with-meta fn-name m)
       ~(with-meta `(di ~@fdecl) {::di-name comp-name}))))


(defn execute
  "execute the components in their dependency order"
  ([components] (execute components {} {}))
  ([components init-ctx] (execute components init-ctx {}))
  ([components init-ctx opts]
   (let [components (map #(let [{::keys [name] :as metadata} (meta %)]
                            (when-not name
                              (throw (ex-info "illegal component" {:comp %})))
                            (assoc metadata ::fn %))
                         components)
         name-map (into
                   {}
                   (map (fn [c] [(::name c) c]) components))

         init-order (impl/init-order components)]
     (reduce (fn [ctx name]
               (let [{the-fn ::fn
                      {:keys [cast-fn]} ::opts
                      :as comp} (get name-map name)]
                 (impl/check-deps comp ctx)
                 (let [input (if cast-fn
                               (reduce (fn [acc [k fun]]
                                         (update acc k fun))
                                       ctx
                                       cast-fn)
                               ctx)
                       res (the-fn input)]
                   (impl/merge-ctx ctx res (:merge-fn opts)))))
             init-ctx
             init-order))))