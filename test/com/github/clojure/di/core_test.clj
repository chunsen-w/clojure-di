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


(deftest test-param-meta
  (testing "the :di/vec"
    (let [c1 (di [{:keys [^:clj-di/vec vec1
                          ^:clj-di/vec vec2]}]
                 {:r1 vec1
                  :r2 vec2})
          p1 (di [_] {:vec1 "p1-v1" :vec2 "p1-v2"})
          p2 (di [_] {:vec2 "p2-v2"})]
      (is (= (select-keys (execute [c1 p1 p2]) [:r1 :r2])
             {:r1  ["p1-v1"]
              :r2 ["p1-v2" "p2-v2"]})))))

(deftest test-component-param-check
  (testing "check required params"
    (let [miss-deps (di [{:keys [the-missing-key]}] {})
          p (di [_] {})]
      (is (thrown-with-msg?
           Exception
           #"\[:com.github.clojure.di.core-test/di-\d+\] missing required param \[:the-missing-key\]"
           (execute [miss-deps p]))))))


(defdi test-comp [_] {})

(deftest test-defdi
  (testing "component name"
    (is (= (::di/name (meta test-comp)) :com.github.clojure.di.core-test/test-comp)))
  (testing "symbol meta"
    (is (= (::di/di (meta #'test-comp)) true))))