# because-tetris

Learning clojure(script) by programming the cult game of tetris

## Overview


- Step 1: create a basic page with 2 canvas elements, one for the game and one to display game status. Register a mouse listener and render a cell based on mouse position



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
