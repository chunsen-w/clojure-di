(ns com.github.clojure.di.core-test
  (:use [clojure.test])
  (:require [com.github.clojure.di.core :refer [defdi di execute] :as di]))

(deftest test-di-macro
  (let [comp (di [{:keys [key-1 key-2]
                   :com.github.clojure.di.core-test/keys [qualified-key1]
                   key3 :the-key-3
                   _ :anonymous-key
                   :or {key3 ""
                        qualified-key1 ""
                        key-1 ""}}]
                 {:out-1 ""
                  :com.github.clojure.di.core-test/out-2 ""})]
    (testing "component parameter"
      (is (= (-> comp meta :clj-di/in)
             #{[:key-1 #{:optional}]
               [:key-2 #{}]
               [:com.github.clojure.di.core-test/qualified-key1 #{:optional}]
               [:the-key-3 #{:optional}]
               [:anonymous-key #{}]})))
    (testing "component return keys"
      (is (= (-> comp meta :clj-di/out)
             #{:out-1 :com.github.clojure.di.core-test/out-2})))))

(testing "di with no argument"
  (let [comp (di [] {:out ""})]
    (is (= (-> comp meta :clj-di/in) #{}))))

(testing "di-with-name"
  (let [comp (di test-di [] {:out ""})]
    (is (true? (.contains (str (-> comp meta :clj-di/name) :com.github.clojure.di.core-test/test-di) "test-di")))
    (is (= (-> comp meta :clj-di/out) #{:out}))))

(defn ctx-values [ctx key]
  (->> (ctx key)
       (map second)))

(deftest test-param-meta
  (testing "the :clj-di/list"
    (let [c1 (di [{:keys [^:clj-di/list list1
                          ^:clj-di/list list2]}]
                 {:r1 list1
                  :r2 list2})
          p1 (di [_] {:list1 "p1-v1" :list2 "p1-v2"})
          p2 (di [_] {:list2 "p2-v2"})
          rs (execute [p1 c1 p2])]
      (are [x y] (= x y)
        (ctx-values rs :r1) [(list "p1-v1")]
        (ctx-values rs :r2) [(list "p1-v2" "p2-v2")]))))

(deftest test-component-param-check
  (testing "check required params"
    (let [miss-deps (di [{:keys [the-missing-key]}] {})
          p (di [_] {})]
      (is (thrown-with-msg?
           Exception
           #"\[:com.github.clojure.di.core-test/di-\d+\] missing required input \[:the-missing-key\]"
           (execute [miss-deps p]))))))


(defdi test-comp [_] {})

(deftest test-defdi
  (testing "component name"
    (is (= (:clj-di/name (meta test-comp)) :com.github.clojure.di.core-test/test-comp)))
  (testing "symbol meta"
    (is (= (:clj-di/di (meta #'test-comp)) true))))

(defdi b1 [_] {:pb1 "pb1"})
(defdi b2 [{:keys [pb1]}] {:pb2 (str "pb2_" pb1)})
(defdi b3 [{:keys [pb2]}] {:pb3 (str "pb3_" pb2)})
(defdi b4 [{:keys [pb2 pb1]}] {:pb4 (str "pb4_" pb2 "_" pb1)})
(deftest execute-test
  (testing "execute component"
    (is (= (execute [b1 b2 b3])
           {:pb1 '([:com.github.clojure.di.core-test/b1 "pb1"])
            :pb2 '([:com.github.clojure.di.core-test/b2 "pb2_pb1"])
            :pb3 '([:com.github.clojure.di.core-test/b3 "pb3_pb2_pb1"])})))

  (testing "init roots"
    (is (= (execute [b1 b2 b3 b4] {} {:init-roots [:pb4]})
           {:pb1 '([:com.github.clojure.di.core-test/b1 "pb1"])
            :pb2 '([:com.github.clojure.di.core-test/b2 "pb2_pb1"])
            :pb4 '([:com.github.clojure.di.core-test/b4 "pb4_pb2_pb1_pb1"])}))))