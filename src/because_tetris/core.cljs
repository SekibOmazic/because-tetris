(ns because-tetris.core
    (:require ))

(def colors
  {:I "#72fafd"
   :T "#ea3ff7"
   :O "#fffd55"
   :J "#0023f5"
   :L "#f08633"
   :S "#75fa4c"
   :Z "#eb3323"})

(def key-names
  {37 :left
   38 :up
   39 :right
   40 :down
   32 :space
   71 :ghost
   80 :pause
   82 :rresume
   13 :enter ;; new game
   })

(def tetrominos
  {:I [[-1  0] [ 0  0] [ 1  0] [ 2  0]]
   :L [[ 1 -1] [-1  0] [ 0  0] [ 1  0]]
   :J [[-1 -1] [-1  0] [ 0  0] [ 1  0]]
   :S [[ 0 -1] [ 1 -1] [-1  0] [ 0  0]]
   :Z [[-1 -1] [ 0 -1] [ 0  0] [ 1  0]]
   :O [[ 0 -1] [ 1 -1] [ 0  0] [ 1  0]]
   :T [[ 0 -1] [-1  0] [ 0  0] [ 1  0]]})

;; board 
(def rows 20)
(def cols 10)
(def empty-row (vec (repeat cols 0)))
(def empty-board (vec (repeat rows empty-row)))

(def initial-pos [4 2])
(def random-tetromino (rand-nth (keys tetrominos)))
(def initial-state {:board empty-board
                    :tetromino-name random-tetromino
                    :piece (random-tetromino tetrominos)
                    :position initial-pos
                    :next-tetromino (rand-nth (keys tetrominos))
                    :ghost true
                    :mode :running})
;; state
(defonce app (atom initial-state))

;; canvas
(def game-canvas (.getElementById js/document "game-canvas"))
(def game-ctx (.getContext game-canvas "2d"))
(def cell-size (quot 600 rows)) ;; quot rounds to the nearest integer
(def next-canvas (.getElementById js/document "next-canvas"))
(def next-ctx (.getContext next-canvas "2d"))


(defn key-name
  [event]
  (-> event .-keyCode key-names))


;; tetromino rotation and movement
(defn rotate-cell
  [[x y]]
  [(- y) x])

(defn rotate
  [tetromino]
  (mapv rotate-cell tetromino))

(defn move-left
  [[x y]]
  [(dec x) y])

(defn move-right
  [[x y]]
  [(inc x) y])


(defn cells-to-write
  "take the current tetromino as a vector of cells, the current position, tetromino name and write it on board"
  [board tetromino [cx cy] name]
  (reduce (fn [board [x y]]
            (try
              (assoc-in board [(+ cy y) (+ cx x)] name) ;; note: querying board coords with [row, col]
              (catch js/Error _ board)))
          board
          tetromino))


(defn fits-in?
  "check if the given tetromino fits into the board (no overlapping with the tetrominos on the board)"
  [board tetromino [cx cy]]
  (every?
   (fn [[x y]]
     (zero? (get-in board [(+ cy y) (+ cx x)])))
   tetromino))


(defn find-first
  "find the first element in the collection that meets requirement defined by function f"
  [f coll]
  (first (filter f coll)))


(defn get-drop-y
  "find first colliding y coordinate for the given board, tetromino and tetromino's current position"
  [board piece [x y]]
  (let [collide? (fn [ny] (not (fits-in? board piece [x ny])))
        gy (find-first collide? (iterate inc y))]
    (max y (dec gy))))


(defn write-to-board!
  "write tetromino on the board"
  []
  (let [{:keys [board piece position tetromino-name]} @app]
    (when (fits-in? board piece position)
      (swap! app
             update-in [:board]
             cells-to-write piece position tetromino-name))))


(defn try-move
  [dx]
  (let [{:keys [board piece position]} @app
        [x y] position
        new-position [(+ dx x) y]]
    (when (fits-in? board piece new-position)
      (swap! app assoc :position new-position))))


(defn try-rotate
  []
  (let [{:keys [board piece position]} @app
        rotated (rotate piece)]
    (when (fits-in? board rotated position)
      (swap! app assoc :piece rotated))))


(defn start-new-game!
  []
  (let [next-name (rand-nth (keys tetrominos))]
    (reset! app initial-state)
    (swap! app assoc :tetromino-name next-name)
    (swap! app assoc :piece (next-name tetrominos))))


(defn game-over!
  []
  (swap! app assoc :mode :game-over))


(defn launch-next!
  []
  (swap! app assoc :tetromino-name (:next-tetromino @app))
  (swap! app assoc :position initial-pos)
  (swap! app assoc :piece ((:tetromino-name @app) tetrominos))
  (swap! app assoc :next-tetromino (rand-nth (keys tetrominos))))


(defn not-filled?
  [row]
  (some #(= 0 %) row))


(defn collapse-filled-rows!
  []
  (let [old-board (:board @app)
        filtered (filter #(not-filled? %) old-board)
        collapsed (- (count old-board) (count filtered))
        new-board (into (vec (repeat collapsed empty-row)) filtered)]
    (swap! app assoc :board new-board)))


(defn finish-tetromino
  []
  (write-to-board!)
  (collapse-filled-rows!)
  (launch-next!)
  (when-not (fits-in? (:board @app) (:piece @app) (:position @app))
    (game-over!))
  )


(defn move-down
  []
  (let [{:keys [board piece position]} @app
        [x y] position
        new-pos [x (inc y)]]
    (if (fits-in? board piece new-pos)
      (swap! app assoc :position new-pos)
      (finish-tetromino))))


(defn hard-drop!
  []
  (let [{:keys [board piece position]} @app
        [x y] position
        dy (get-drop-y board piece position)]
    (swap! app assoc :position [x dy])
    (finish-tetromino)))


(defn toggle-ghost!
  []
  (swap! app assoc :ghost (not (:ghost @app))))


(defn keydown-handler
  [event]
  (let [keyname (key-name event)
        mode (:mode @app)]
    (if (= mode :running)
      (case keyname
        :left (try-move -1)
        :right (try-move 1)
        :up (try-rotate)
        :down (move-down)
        :space (hard-drop!)
        :ghost (toggle-ghost!)
        nil)
      (when (= keyname :enter)
        (start-new-game!)))
    
    (when (#{:down :left :right :up :space} keyname)
      (.preventDefault event))))


(defn get-absolute-coords
  "for the given tetromino (vector of cells) and it's current position get a vector of absolute positions"
  [tetromino pos]
  (let [[cx cy] pos]
    (mapv (fn [[x y]] [(+ cx x) (+ cy y)]) tetromino)))


(defn draw-cell
  "render the cell with given coordinates on the given canvas"
  [ctx [x y]]  
  (let [rx (* cell-size x)
        ry (* cell-size y)
        rs cell-size]
    (.fillRect ctx rx ry rs rs)
    (.strokeRect ctx rx ry rs rs)))


(defn draw-tetromino!
  "calculate absoulute position of the current tetromino and draw it"
  [ctx tetromino pos]
  (let [tetromino-cells (get-absolute-coords tetromino pos)]
    (doseq [cell tetromino-cells]
      (draw-cell ctx cell))))


(defn draw-current!
  [ctx {:keys [tetromino-name piece position]}]
  (set! (.-fillStyle ctx) (tetromino-name colors))
  (draw-tetromino! ctx piece position))


(defn draw-next!
  [ctx {:keys [next-tetromino]}]
  (let [next-cells (tetrominos next-tetromino)]
    (.clearRect ctx 0 0 (* cell-size 4) (* cell-size 4))
    (set! (.-fillStyle ctx) (next-tetromino colors))
    (draw-tetromino! ctx next-cells [1 2])))


(defn draw-ghost!
  [ctx {:keys [board piece position ghost]}]
  (set! (.-fillStyle ctx) "#555")
  (let [;;{:keys [board piece position]} @app
        [x y] position
        gy (get-drop-y board piece position)]
    (when ghost 
      (draw-tetromino! ctx piece [x gy]))))


(defn draw-board!
  [ctx {:keys [board]}]
  (.clearRect ctx 0 0 (* cell-size cols) (* cell-size rows))
  (doseq [y (range rows)
          x (range cols)]
    (let [cell-value (get-in board [y x])] ;; query board with [row coll]
      (when-not (zero? cell-value)
        (do
          (set! (.-fillStyle ctx) (cell-value colors))
          (draw-cell ctx [x y]))))))


(defn render
  []
  (let [state @app
        mode (:mode @app)]
    (when (= mode :running)
      (draw-board! game-ctx state)
      (draw-ghost! game-ctx state)
      (draw-current! game-ctx state)
      (draw-next! next-ctx state))
    (.requestAnimationFrame js/window render)))


#_(defn render
  []
  (let [state @app
        mode (:mode @app)]
    (cond->> state
      (= mode :running) (draw-board! game-ctx)
      (= mode :running) (draw-ghost! game-ctx)
      (= mode :running) (draw-current! game-ctx)
      (= mode :running) (draw-next! next-ctx))
    (.requestAnimationFrame js/window render)))


(.addEventListener js/window "keydown" keydown-handler)

(set! (.-lineWidth game-ctx) 2)
(set! (.-lineWidth next-ctx) 2)
(set! (.-strokeStyle game-ctx) "#333")
(set! (.-strokeStyle next-ctx) "#2c2c2c")

;; start
(render)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
