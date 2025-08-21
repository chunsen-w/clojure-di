(ns com.github.clojure.di.impl
  (:require [com.github.clojure.di.util :as util]))

(defn parse-args [destruce-map]
  (cond
    (map? destruce-map)
    (let [sym-map (reduce (fn [acc [k v]]
                            (cond
                              (= :keys k) (into acc (map (fn [sym] [sym (-> sym name keyword)]) v))
                              (and (qualified-keyword? k)
                                   (= (name k) "keys")) (let [ns (namespace k)]
                                                          (into acc (map (fn [sym] [sym (->> sym name (keyword ns))]) v)))
                              (symbol? k) (assoc acc k v)
                              ;; (map? k) (parse-deps k)
                              :else acc))
                          {}
                          destruce-map)
          optional-keys (if-let [optionals (:or destruce-map)]
                          (set (map sym-map (keys optionals)))
                          #{})]
      {:clj-di/in
       (->> sym-map
            (map (fn [[sym kw]]
                   (-> [kw
                        (-> #{}
                            (into  (when (optional-keys kw) [:optional]))
                            (into (->> sym util/true-metas (filter (fn [meta-key] (= (namespace meta-key) "clj-di"))))))])))
            set)})
    (or (nil? destruce-map)
        (= (name destruce-map) "_")) {:clj-di/in #{}}
    :else (throw (IllegalArgumentException. "the argument of a di compoent must be a map or _"))))

(defn parse-resp [body]
  (let [resp (last body)]
    (cond
      (map? resp) {:clj-di/out (set (keys resp))}
      (seq? resp) (parse-resp resp)
      :else (throw (new IllegalArgumentException
                        "Di must return a map")))))

(defn calc-provider
  "translate the map for {name -> out [p1 p2]} to {p1 -> (name1 name2), p2-> (name)}"
  [components]
  (reduce (fn [acc [k n]] (update acc k conj n))
          {}
          (mapcat (fn [{:clj-di/keys [out name]}]
                    (map #(list % name) out))
                  components)))

(defn merge-ctx [ctx new provider-name]
  (reduce (fn [acc [k v]]
            (update acc k #(conj % [provider-name v])))
          ctx
          new))

(defn select-input
  "select the input value of a di component from the ctx"
  [{:clj-di/keys [name in]}
   ctx]
  (letfn [(select [key opts]
            (if-let [values (get ctx key)]
              (cond
                (opts :clj-di/list) (into '() (map second values))
                (= (count values) 1) (second (first values))
                :else (throw (Exception. (str "Multiple values found for key [" key "]"))))
              (when-not (opts :optional)
                (throw  (Exception. (str "[" name "] missing required input [" key "]"))))))]
    (reduce (fn [acc [key opts]]
              (if-let [input (select key opts)]
                (assoc acc key input)
                acc))
            {}
            in)))
