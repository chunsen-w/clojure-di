(ns com.github.clojure.di.impl-test
  (:use [clojure.test])
  (:require [com.github.clojure.di.impl :as impl]))



(deftest sort-test
  (testing "sort init order"
    (is (=  (impl/init-order '([:i1 #{}]
                               [:i2 #{:i1 :i4}]
                               [:i3 #{:i2}]
                               [:i4 #{}]))
            [:i1 :i4 :i2 :i3]))))