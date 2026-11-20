# Third-party notices

## Stepsy tracking engine

StepsMore's sensor-tracking service has been adapted from the sensor-processing
architecture in **Stepsy**:

- Project: https://github.com/nvllz/stepsy
- License: GNU General Public License, version 3 (GPL-3.0)
- License text: `LICENSE`

The StepsMore integration is modified. Changes include adapting the engine to
StepsMore's package, Room database, DataStore preferences, notification system,
and UI; processing sensor callbacks on a dedicated HandlerThread; serializing
samples through a single-consumer queue; and persisting the hardware counter
baseline with Android boot-count detection.

The Stepsy README credits **Motionmate** by 0xf4b1:
https://github.com/0xf4b1/motionmate

No claim is made that the upstream Stepsy project endorses StepsMore.

## GPL-3.0 distribution note

This combined derivative project is distributed under GPL-3.0. When conveying
the app or modified source, retain this notice and the full `LICENSE`, provide
the Corresponding Source as required by GPL-3.0, and identify modifications.
