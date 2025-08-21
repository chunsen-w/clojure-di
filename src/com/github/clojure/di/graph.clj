(ns com.github.clojure.di.graph
  (:require [clojure.java.shell :as shell])
  (:import (org.jgrapht.graph DefaultDirectedGraph DefaultEdge)
           (org.jgrapht Graphs)
           (org.jgrapht.nio.dot DOTExporter)
           (java.io StringWriter)))

(set! *warn-on-reflection* true)

(defn build-graph
  "Build a graph from the dege or vertex seq, 
   if the element is a vector [source target], then add it as edge
   else add it as vertex"
  [edges-or-vertices]
  (let [graph (DefaultDirectedGraph.  DefaultEdge)]
    (doseq [e edges-or-vertices]
      (if (vector? e)
        (Graphs/addEdgeWithVertices graph (first e) (second e))
        (.addVertex graph e)))
    graph))

(defn reverse-graph [^DefaultDirectedGraph graph]
  (let [vertices (.vertexSet graph)
        new-graph (DefaultDirectedGraph. DefaultEdge)]
    (Graphs/addAllVertices new-graph vertices)
    (doseq [e (.edgeSet graph)]
      (.addEdge new-graph (.getEdgeTarget graph e) (.getEdgeSource graph e)))
    new-graph))

(defn show-graph
  ([graph] (show-graph graph str))
  ([^DefaultDirectedGraph graph label-fn]
   (let [writer (StringWriter.)
         export (DOTExporter. label-fn)]
     (.exportGraph export graph writer)
     (str writer))))

(defn topological-sort [^DefaultDirectedGraph graph]
  (let [iterator (org.jgrapht.traverse.TopologicalOrderIterator. graph)]
    (iterator-seq iterator)))

(defn dfs-visit [^DefaultDirectedGraph graph start]
  (let [visitor (org.jgrapht.traverse.DepthFirstIterator. graph start)]
    (iterator-seq visitor)))

(defn detect-cycle [^DefaultDirectedGraph graph]
  (let [detector (org.jgrapht.alg.cycle.CycleDetector. graph)]
    (.findCycles detector)))

(defn view-graph [graph]
  (let [dot-str (show-graph graph)
        encoded-str (.replace (java.net.URLEncoder/encode dot-str) "+" "%20")]
    (shell/sh "open" (str "https://dreampuf.github.io/GraphvizOnline/?engine=dot#" encoded-str))))
