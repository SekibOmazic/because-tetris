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

(def next-piece
  {:I :T
   :T :O
   :O :J
   :J :L
   :L :S
   :S :Z
   :Z :I})


;; board 
(def rows 20)
(def cols 10)
(def empty-row (vec (repeat cols 0)))
(def empty-board (vec (repeat rows empty-row)))

(def initial-pos [4 6])

;; state
(defonce app (atom {:piece :J
                    :position initial-pos}))


;; canvas
(def game-canvas (.getElementById js/document "game-canvas"))
(def game-ctx (.getContext game-canvas "2d"))
(def cell-size (quot 600 rows)) ;; quot rounds to the nearest integer
(def next-canvas (.getElementById js/document "next-canvas"))
(def next-ctx (.getContext next-canvas "2d"))


(defn mousemove-handler
  "updates row and col based on mouse position"
  [e]
  (let [rect (.getBoundingClientRect game-canvas) ;; rect = canvas.getBoundingClientRect()
        x (- (.-clientX e) (.-left rect))         ;; x = e.clientX - rect.left
        y (- (.-clientY e) (.-top rect))          ;; y = e.clientY - rect.top
        col (quot x cell-size)                    ;; calculate col
        row (quot y cell-size)]
    (swap! app assoc :position [col row])))


(defn mouseleave-handler
  [_]
  (swap! app assoc :position initial-pos))


(defn click-handler
  [_]
  (swap! app update :piece next-piece))


(defn get-absolute-coords
  "for the given piece key (name) get a vector of absolute positions (using position from app state and pieces map)"
  [piece-key piece-pos]
  (let [[cx cy] piece-pos]
    (mapv (fn [[x y]] [(+ cx x) (+ cy y)]) (pieces piece-key))))


(defn draw-cell
  "render the cell with given coordinates on the given canvas"
  [ctx [x y]]
  
  (let [rx (* cell-size x)
        ry (* cell-size y)
        rs cell-size]
    (set! (.-fillStyle ctx) dark-purple)
    (set! (.-strokeStyle ctx) light-purple)
    (.fillRect ctx rx ry rs rs)
    (.strokeRect ctx rx ry rs rs)))


(defn draw-piece
  [ctx piece-name piece-pos]
  (let [piece-cells (get-absolute-coords piece-name piece-pos)]
    (doseq [cell piece-cells]
      (draw-cell ctx cell))))


(defn draw-current-piece
  [ctx]
  (set! (.-lineWidth ctx) 2)
  (draw-piece ctx (:piece @app) (:position @app)))


(defn draw-next-piece
  [ctx]
  (set! (.-lineWidth ctx) 2)
  (draw-piece ctx ((:piece @app) next-piece) [1 2]))


(defn render
  []
  (.requestAnimationFrame js/window render)
  (.clearRect game-ctx 0 0 (* cell-size cols) (* cell-size rows))
  (.clearRect next-ctx 0 0 (* cell-size 4) (* cell-size 4))
  (draw-current-piece game-ctx)
  (draw-next-piece next-ctx))


(.addEventListener game-canvas "mousemove" mousemove-handler)
(.addEventListener game-canvas "mouseleave" mouseleave-handler)
(.addEventListener next-canvas "mousedown" click-handler)


;; start
(render)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
