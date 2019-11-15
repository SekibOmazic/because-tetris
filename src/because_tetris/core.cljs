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


;; keys
(def key-names
  {37 :left
   38 :up
   39 :right
   40 :down
   32 :space})

(defn key-name
  [event]
  (-> event .-keyCode key-names))



;; board 
(def rows 20)
(def cols 10)
(def empty-row (vec (repeat cols 0)))
(def empty-board (vec (repeat rows empty-row)))

(def initial-pos [4 6])

;; state
(defonce app (atom {:board empty-board
                    :piece-name :J
                    :piece (:J pieces)
                    :position initial-pos}))

;; canvas
(def game-canvas (.getElementById js/document "game-canvas"))
(def game-ctx (.getContext game-canvas "2d"))
(def cell-size (quot 600 rows)) ;; quot rounds to the nearest integer
(def next-canvas (.getElementById js/document "next-canvas"))
(def next-ctx (.getContext next-canvas "2d"))

;; piece rotation and movement
(defn rotate-cell
  [[x y]]
  [(- y) x])

(defn rotate
  [piece]
  (mapv rotate-cell piece))

(defn move-left
  [[x y]]
  [(dec x) y])

(defn move-right
  [[x y]]
  [(inc x) y])

(defn move-down
  [x y]
  [x (inc y)])


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


(defn next-piece-click-handler
  [_]
  (let [next-name ((:piece-name @app) next-piece)
        next-cells (pieces next-name)]
    (swap! app assoc :piece-name next-name)
    (swap! app assoc :piece next-cells)))


(defn cells-to-write
  "take the current piece as a vector of cells, the current position and write it on board"
  [board piece [cx cy]]
  (reduce (fn [board [x y]]
            (try
              (assoc-in board [(+ cy y) (+ cx x)] 1)
              (catch js/Error _ board )))
          board
          piece))


(defn write-to-board!
  []
  (let [{:keys [piece position]} @app]
    (swap! app
           update-in [:board]
           cells-to-write piece position)))


(defn game-click-handler
  [_]
  (write-to-board!))


(defn try-rotate
  []
  (let [{:keys [piece position]} @app
        rotated (rotate piece)]
    (swap! app assoc :piece rotated)))


(defn keydown-handler
  [event]
  (let [keyname (key-name event)]
    (case keyname
      ;;:left (try-move -1)
      ;;:right (try-move 1)
      :up (try-rotate)
      nil)
    (when (#{:down :left :right :up :space} keyname)
      (.preventDefault event))))


(defn get-absolute-coords
  "for the given piece (vector of cells) and it's current position get a vector of absolute positions"
  [piece pos]
  (let [[cx cy] pos]
    (mapv (fn [[x y]] [(+ cx x) (+ cy y)]) piece)))


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
  "calculate absoulute position of the current piece and draw it"
  [ctx piece piece-pos]
  (let [piece-cells (get-absolute-coords piece piece-pos)]
    (doseq [cell piece-cells]
      (draw-cell ctx cell))))


(defn draw-current-piece
  [ctx]
  (set! (.-lineWidth ctx) 2)
  (draw-piece ctx (:piece @app) (:position @app)))


(defn draw-next-piece
  [ctx]
  (set! (.-lineWidth ctx) 2)
  (let [next-name ((:piece-name @app) next-piece)
        next-cells (pieces next-name)]
    (draw-piece ctx next-cells [1 2])))


(defn draw-board
  [ctx board]
  (doseq [y (range rows)
          x (range cols)]
    (let [cell-value (get-in board [y x])] ;; query board with [row coll]
      (when-not (zero? cell-value)
        (draw-cell ctx [x y])))))


(defn render
  []
  (.requestAnimationFrame js/window render)
  (.clearRect game-ctx 0 0 (* cell-size cols) (* cell-size rows))
  (.clearRect next-ctx 0 0 (* cell-size 4) (* cell-size 4))
  (draw-board game-ctx (:board @app))
  (draw-current-piece game-ctx)
  (draw-next-piece next-ctx))


(.addEventListener game-canvas "mousemove" mousemove-handler)
(.addEventListener game-canvas "mouseleave" mouseleave-handler)
(.addEventListener next-canvas "mousedown" next-piece-click-handler)
(.addEventListener js/window "keydown" keydown-handler)

(.addEventListener game-canvas "mousedown" game-click-handler)


;; start
(render)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
