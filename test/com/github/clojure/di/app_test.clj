(ns com.github.clojure.di.app-test
  (:use [clojure.test])
  (:require [com.github.clojure.di.app :refer [scan-components]]
            [com.github.clojure.di.core :refer [defdi] :as di]))

(defdi comp1 [_] {})
(defdi comp2 [_] {:p2 "p2"})

(deftest scan-components-test
  (testing "get di components"
    (let [components (scan-components 'com.github.clojure.di.app-test)]
      (are [x y] (= x y)
        (count components) 2
        (some #(not (fn? %)) components) nil
        (set (map #(-> % meta ::di/name) components)) #{::comp1 ::comp2}))))