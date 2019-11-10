(ns because-tetris.core
    (:require ))

;; colors
(def dark-green "#143")
(def light-green "#175")
(def dark-purple "#449")
(def light-purple "#6ad")

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


(defn draw-cell
  [ctx [x y]]
  (let [rx (* cell-size x)
        ry (* cell-size y)
        rs cell-size]
    (.fillRect ctx rx ry rs rs)
    (.strokeRect ctx rx ry rs rs)))


(defn render
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
