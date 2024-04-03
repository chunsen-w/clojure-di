(ns com.github.clojure.di.core
  (:require [com.github.clojure.di.impl :as impl]))


(def ^:private opt-map
  {:clj-di/vec {:merge-fn
                (fn [_ values] (->> values (map second) reverse (into [])))}
   :clj-di/list {:merge-fn (fn [_ values] (map second values))}})

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
                              (throw (ex-info (str "illegal component " %) {:comp %})))
                            (assoc metadata ::fn %))
                         components)
         name-map (into
                   {}
                   (map (fn [c] [(::name c) c]) components))

         init-order (impl/init-order components)]
     (reduce (fn [ctx name]
               (let [comp (get name-map name)
                     input  (impl/select-input comp ctx opts)
                     res ((::fn comp) input)]
                 (impl/merge-ctx ctx res name)))
             (impl/merge-ctx {} init-ctx :init)
             init-order))))