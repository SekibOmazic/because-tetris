# because-tetris

Learning clojure(script) by programming the cult game of tetris

## Overview

You can check out each of the following branches to see the progress:


- Step 1: create a basic page with 2 canvas elements, one for the game and one to display next piece. Register a mouse listener and render a cell based on mouse position
- Step 2: draw all 7 pieces. Color of the active piece (mouse is over the piece) should be purple. In this case the color of the center cell should be green.
- Step 3: draw one piece on the main canvas based on the current mouse position. On the next piece canvas add a click handler to change the next piece on each mouse click
- Step 4: add a keydown handler, bind it to Arrow Up key and rotate the current piece on keydown event
- Step 5: write the current piece on the board when clicked
- Step 6: add colors and collision detection. No tetromino should be written to the board if it doesn't fit on the board (no cells out of the board and no overlapping with already stored tetrominons)
- Step 7: use keyboard for tetromino movements (left, right, down, up for rotation). Render the ghost tetromino which can be toggled when pressing the key "g"
- Step 8: collapse filled rows
- Step 9: add gravity, add scoring and a better layout of the page.
- Step 10: add splash screens

## To do

- [x] ~~Add splash pane on game over and before starting the game.~~
- [ ] Refactor code to be more functional. Currently the state update is scattered all over the code base. 
- [ ] Improve rotation system. User Super Rotation System as described [here](https://strategywiki.org/wiki/Tetris/Rotation_systems)
- [ ] Add wall kicks as described [here](https://strategywiki.org/wiki/Tetris/Rotation_systems)
- [ ] Use "valid" scoring system? Check [here] (https://tetris.wiki/Tetris_(NES,_Nintendo) )


## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
