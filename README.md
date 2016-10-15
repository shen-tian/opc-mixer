# opcmixer

[![Build Status](https://travis-ci.org/shen-tian/opcmixer.svg?branch=master)](https://travis-ci.org/shen-tian/opcmixer)

A Clojure app for mixing of Open Pixel Control streams. 

## Usage

Right now, it's hardcoded to:

 * Listen on port 7890
 * Conenct to an output on port 7891 (localhost)

To run, use:

    lein run

This accepts N incoming OPC streams, and outputs their average at ~60fps.

## License

Copyright © 2016 Shen Tian

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
