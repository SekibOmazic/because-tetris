(ns because-tetris.core
    (:require ))

;; colors
(def dark-purple "#449")
(def light-purple "#6ad")
(def dark-green "#143")
(def light-green "#175")

(def pieces
  {:I [[-1  0] [ 0  0] [ 1  0] [ 2  0]]
   :L [[ 1 -1] [-1  0] [ 0  0] [ 1  0]]
   :J [[-1 -1] [-1  0] [ 0  0] [ 1  0]]
   :S [[ 0 -1] [ 1 -1] [-1  0] [ 0  0]]
   :Z [[-1 -1] [ 0 -1] [ 0  0] [ 1  0]]
   :O [[ 0 -1] [ 1 -1] [ 0  0] [ 1  0]]
   :T [[ 0 -1] [-1  0] [ 0  0] [ 1  0]]})

(def positions
  {:I [4 1]
   :T [4 4]
   :O [4 7]
   :J [4 10]
   :L [4 13]
   :S [4 16]
   :Z [4 19]})

;; state
(defonce app (atom {:row nil :col nil}))

;; board 
(def rows 20)
(def cols 10)
(def empty-row (vec (repeat cols 0)))
(def empty-board (vec (repeat rows empty-row)))

;; canvas
(def game-canvas (.getElementById js/document "game-canvas"))
(def game-ctx (.getContext game-canvas "2d"))
(def cell-size (quot 600 rows)) ;; quot rounds to the nearest integer


(defn canvas-mouse-listener
  "updates row and col based on mouse position"
  [e]
  (let [rect (.getBoundingClientRect game-canvas) ;; rect = canvas.getBoundingClientRect()
        x (- (.-clientX e) (.-left rect))         ;; x = e.clientX - rect.left
        y (- (.-clientY e) (.-top rect))          ;; y = e.clientY - rect.top
        col (quot x cell-size)                    ;; calculate col
        row (quot y cell-size)]
    (swap! app assoc :row row)
    (swap! app assoc :col col)))


(defn canvas-leave-mouse-listener
  [_]
  (do
    (swap! app assoc :row nil)
    (swap! app assoc :col nil)))


(defn get-absolute-coords
  [piece-key]
  (let [[cx cy] (positions piece-key)]
    (mapv (fn [[x y]] [(+ cx x) (+ cy y)]) (pieces piece-key))))


(defn center?
  "for the given cell [x y] check if this is the center cell of the tetris piece"
  [[x y]]
  (let [mx (:col @app)
        my (:row @app)]
    (= [0 0] [(- mx x) (- my y)])))


(defn draw-cell
  "takes canvas ctx, cell coordinates and the flag if the cell is in selected (mouse over) tetris piece"
  [ctx [x y] active]
  
  (let [rx (* cell-size x)
        ry (* cell-size y)
        rs cell-size]
    (set! (.-fillStyle ctx)
          (cond
            (center? [x y]) dark-green
            active dark-purple
            :else "transparent"))
    (set! (.-strokeStyle ctx)
          (cond
            ;;(center? [x y]) light-green
            active light-purple
            :else "#888"))
    (.fillRect ctx rx ry rs rs)
    (.strokeRect ctx rx ry rs rs)))


(defn active-piece?
  [piece-cells]
  (let [x (:col @app)
        y (:row @app)]
    (some #(= [x y] %) piece-cells)))


(defn draw-piece
  [ctx piece-key]
  (let [piece-cells (get-absolute-coords piece-key)
        active (active-piece? piece-cells)]
    (doseq [cell piece-cells]
      (draw-cell ctx cell active))))


(defn draw-board
  [ctx pieces]
  (set! (.-lineWidth ctx) 2)
  (doseq [p (keys pieces)]
    (draw-piece ctx p)))


(defn render
  []
  (.requestAnimationFrame js/window render)
  (.clearRect game-ctx 0 0 (* cell-size cols) (* cell-size rows))
  (draw-board game-ctx pieces))



#_(defn render
  []
  (.requestAnimationFrame js/window render)
  (let [x (:col @app)
        y (:row @app)]
    (.clearRect game-ctx 0 0 (* cell-size cols) (* cell-size rows))
    (set! (.-lineWidth game-ctx) 2)
    (set! (.-fillStyle game-ctx) dark-purple)
    (set! (.-strokeStyle game-ctx) light-purple)
    (when y
      (draw-cell game-ctx [x y]))))


(.addEventListener game-canvas "mousemove" canvas-mouse-listener)
(.addEventListener game-canvas "mouseleave" canvas-leave-mouse-listener)


;; start
(render)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
