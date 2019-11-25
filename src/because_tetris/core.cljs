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
   27 :escape
   82 :resume
   13 :enter})

(def tetrominos
  {:I [[-1  0] [ 0  0] [ 1  0] [ 2  0]]
   :L [[ 1 -1] [-1  0] [ 0  0] [ 1  0]]
   :J [[-1 -1] [-1  0] [ 0  0] [ 1  0]]
   :S [[ 0 -1] [ 1 -1] [-1  0] [ 0  0]]
   :Z [[-1 -1] [ 0 -1] [ 0  0] [ 1  0]]
   :O [[ 0 -1] [ 1 -1] [ 0  0] [ 1  0]]
   :T [[ 0 -1] [-1  0] [ 0  0] [ 1  0]]})

;; gravity speed depending on current game level. As described here https://tetris.wiki/Tetris_(NES,_Nintendo)
(def gravity-intervals
  (mapv
   (fn [speed] (* 1000 (/ speed 60.0988)))
   [48 43 38 33 28 23 18 13 8 6 5 5 5 4 4 4 3 3 3 2 2 2 2 2 2 2 2 2 2 1]))


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
                    :mode :welcome
                    :level 0
                    :score 0
                    :lines-completed 0
                    :last-render-ts 0})
;; state
(defonce app (atom initial-state))

;; canvas
(def game-canvas (.getElementById js/document "game-canvas"))
(def game-ctx (.getContext game-canvas "2d"))
(def cell-size (quot 600 rows)) ;; quot rounds to the nearest integer
(def next-canvas (.getElementById js/document "next-canvas"))
(def next-ctx (.getContext next-canvas "2d"))
(def score-text (.getElementById js/document "score"))
(def level-text (.getElementById js/document "level"))
(def splash-panel (.getElementById js/document "splash"))
(def message-div (.getElementById js/document "message"))
(def welcome-msg "Welcome to the world of Tetris")
(def gameover-msg "GAME OVER!")


(defn key-name [event]
  (-> event .-keyCode key-names))


;; tetromino rotation and movement
(defn rotate-cell [[x y]]
  [(- y) x])

(defn rotate [tetromino]
  (mapv rotate-cell tetromino))

(defn move-left [[x y]]
  [(dec x) y])

(defn move-right [[x y]]
  [(inc x) y])


(defn hide-splash! []
  (set! (.-display (.-style splash-panel)) "none"))


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
  [state]
  (let [{:keys [board piece position tetromino-name]} state]
    (if (fits-in? board piece position)
      (assoc state :board (cells-to-write board piece position tetromino-name))
      state)))


(defn try-move [dx state]
  (let [{:keys [board piece position]} state
        [x y] position
        new-position [(+ dx x) y]]
    (if (fits-in? board piece new-position)
      (assoc state :position new-position)
      state)))


(defn try-rotate [state]
  (let [{:keys [board piece position]} state
        rotated (rotate piece)]
    (if (fits-in? board rotated position)
      (assoc state :piece rotated)
      state)))


(defn start-new-game! []
  (let [next-name (rand-nth (keys tetrominos))]
    (hide-splash!)
    (merge initial-state {:mode :running
                          :tetromino-name next-name
                          :piece (next-name tetrominos)
                          })))


(defn game-over! [state]
  (assoc state :mode :game-over))


(defn launch-next! [{:keys [next-tetromino tetromino-name] :as state}]
  (merge state {:tetromino-name next-tetromino
                :position initial-pos
                :piece (next-tetromino tetrominos)
                :next-tetromino (rand-nth (keys tetrominos))
                }))


(defn not-filled? [row]
  (some #(= 0 %) row))


(defn next-level [level]
  (* 5 (/ (* level (inc level)) 2)))


(defn update-score! [state1]
  (let [{:keys [board lines-completed level score] :as state} state1
        filtered (filter #(not-filled? %) board)
        collapsed (- (count board) (count filtered))
        new-board (into (vec (repeat collapsed empty-row)) filtered)]
    (merge state
           {:board new-board
            :lines-completed (+ collapsed lines-completed)
            :level (if (>= lines-completed (next-level (inc level)))
                     (inc level)
                     level)
            :score (+ score
                      (* (inc level)
                         (case collapsed
                           0 0
                           1 100
                           2 300
                           3 500
                           4 800)))
            })))


(defn try-game-over! [{:keys [board piece position] :as state}]
  (if-not (fits-in? board piece position)
    (game-over! state)
    state))

(defn finish-tetromino [state]
  (-> state
   (write-to-board!) 
   (update-score!)  
   (launch-next!)    
   (try-game-over!))) 


(defn move-down [state1]
  (let [{:keys [board piece position] :as state} state1
        [x y] position
        new-pos [x (inc y)]]
    (if (fits-in? board piece new-pos)
      (assoc state :position new-pos)
      (finish-tetromino state))))


(defn hard-drop! [state]
  (let [{:keys [board piece position]} state
        [x y] position
        dy (get-drop-y board piece position)]
    (finish-tetromino (assoc state :position [x dy]))))


(defn keydown-handler [event]
  (let [keyname (key-name event)
        mode (:mode @app)
        state @app]
    (reset! app  
            (cond
              (= mode :running)
              (case keyname
                :left (try-move -1 state)
                :right (try-move 1 state)
                :up (try-rotate state)
                :down (move-down state)
                :space (hard-drop! state)
                :ghost (assoc state :ghost (not (:ghost state)))
                :escape (assoc state :mode :pause)
                state)
              (= mode :pause)
                (if (= keyname :resume) (assoc state :mode :running) state)
              (or (= mode :game-over) (= mode :welcome))
                (if (= keyname :enter) (start-new-game!) state)))))


(defn process-gravity [now state]
  (let [{:keys [last-render-ts level]} state
        gravity-interval (nth gravity-intervals level 1)]
    (if (> (- now last-render-ts) gravity-interval)
      (move-down (assoc state :last-render-ts now))
      state)))


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
  [ctx {:keys [next-tetromino] :as state}]
  (let [next-cells (tetrominos next-tetromino)]
    (.clearRect ctx 0 0 (* cell-size 4) (* cell-size 4))
    (set! (.-fillStyle ctx) (next-tetromino colors))
    (draw-tetromino! ctx next-cells [1 2])
    state))


(defn draw-ghost!
  [ctx {:keys [board piece position ghost]}]
  (set! (.-fillStyle ctx) "#555")
  (let [[x y] position
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


(defn render-status! [{:keys [score level] :as state}]
  (aset score-text "innerText" score)
  (aset level-text "innerText" level)
  state)


(defn render-splash! [msg]
  (aset message-div "innerText" msg)
  (set! (.-display (.-style splash-panel)) "flex"))


(defn draw-game! [ctx state]
  (draw-board! ctx state)
  (draw-ghost! ctx state)
  (draw-current! ctx state)
  state)


(defn game-loop [now]
  (let [{:keys [mode] :as state} @app]
    (cond
      (= mode :welcome) (render-splash! welcome-msg)
      (= mode :game-over) (render-splash! gameover-msg)
      (= mode :running) 
      (->> state
           (process-gravity now)
           (draw-game! game-ctx)
           (draw-next! next-ctx)
           (render-status!)
           (reset! app)))
    (.requestAnimationFrame js/window game-loop)))


(defn start-game []
  (.addEventListener js/window "keydown" keydown-handler)
  (set! (.-lineWidth game-ctx) 2)
  (set! (.-lineWidth next-ctx) 2)
  (set! (.-strokeStyle game-ctx) "#333")
  (set! (.-strokeStyle next-ctx) "#2c2c2c")
  (.requestAnimationFrame js/window game-loop))

;; start
(defonce launch (start-game))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
