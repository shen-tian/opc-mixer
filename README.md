# opcmixer

[![Build Status](https://travis-ci.org/shen-tian/opcmixer.svg?branch=master)](https://travis-ci.org/shen-tian/opcmixer)

An app for mixing of Open Pixel Control streams. Intended to be used upstream from
a Fadecandy or LEDScape server.

It gracefully handles disconnects both upstream (OPC sources) and downstream (OPC servers).

## Usage

Right now, it's hardcoded to:

 * Listen on port 7890
 * Conenct to an output on port 7891 (localhost)

To run, use:

    lein run

This accepts N incoming OPC streams, and outputs their average at ~60fps.

Will package better, and sort out configuration later. 

## Notes

The backing code is a bit heavyweight. It uses the `aleph` and `manifold` combo. It also
uses the `clj-opc` [library](https://github.com/shen-tian/clj-opc) for the OPC codec and
handling of output.

Future features:

 * Control (RESTful? WebSockets? TCP? MIDI?) to control channels/mixing
 * Web front end that acts as a (low FPS?) preview of each channel and its mixes. This
 also implies that the system will have to have some understanding about the physical
 layout of the pixels.
 
## License

Copyright © 2016 Shen Tian

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
