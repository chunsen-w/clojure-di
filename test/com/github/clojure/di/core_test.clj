(ns com.github.clojure.di.core-test
  (:use [clojure.test])
  (:require [com.github.clojure.di.core :refer [defdi di execute] :as di]))

(deftest test-di-macro
  (let [comp (di [{:keys [key-1 key-2]
                   ::keys [qualified-key1]
                   key3 :the-key-3
                   _ :anonymous-key
                   :or {key3 ""
                        qualified-key1 ""
                        key-1 ""}}]
                 {:out-1 ""
                  ::out-2 ""})]
    (testing "component parameter"
      (is (= (-> comp meta ::di/in)
             #{:key-1 :key-2 ::qualified-key1 :the-key-3 :anonymous-key})))
    (testing "optinal keys"
      (is (= (-> comp meta ::di/optional)
             #{:key-1 :the-key-3 ::qualified-key1})))
    (testing "component return keys"
      (is (= (-> comp meta ::di/out)
             #{:out-1 ::out-2})))))

(defn ctx-values [ctx key]
  (->> (ctx key)
       (map second)))

(deftest test-param-meta
  (testing "the :clj-di/vec"
    (let [c1 (di [{:keys [^:clj-di/vec vec1
                          ^:clj-di/vec vec2]}]
                 {:r1 vec1
                  :r2 vec2})
          p1 (di [_] {:vec1 "p1-v1" :vec2 "p1-v2"})
          p2 (di [_] {:vec2 "p2-v2"})
          rs (execute [p1 c1 p2])]
      (are [x y] (= x y)
        (ctx-values rs :r1) [["p1-v1"]]
        (ctx-values rs :r2) [["p1-v2" "p2-v2"]]))))

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
    (is (= (::di/name (meta test-comp)) :com.github.clojure.di.core-test/test-comp)))
  (testing "symbol meta"
    (is (= (::di/di (meta #'test-comp)) true))))

(defdi b1 [_] {:pb1 "pb1"})
(defdi b2 [{:keys [pb1]}] {:pb2 (str "pb2_" pb1)})
(defdi b3 [{:keys [pb2]}] {:pb3 (str "pb3_" pb2)})
((deftest execute-test
   (testing "execute component"
     (is (= (execute [b1 b2 b3]) {:pb1 '([::b1 "pb1"])
                                  :pb2 '([::b2 "pb2_pb1"])
                                  :pb3 '([::b3 "pb3_pb2_pb1"])})))))