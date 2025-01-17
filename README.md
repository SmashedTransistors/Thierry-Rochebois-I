# Thierry-Rochebois-I
Teensy4.1 based Transdimensional Synthesizer number one.

## Transdimensional ?
Transdimensional is a fancy name for a way to simplify the use of very complicated synthesis algorithm.
It is a kind of morphing that takes care of navigating through the synthesis parameter hyperspace.
Thierry-Rochebois-I navigates in a 54 dimension hyperspace.

## Thierry-Rochebois-I
Yes, it's my real name. It can seem weird to name a synth after myself... but that's an easy way to avoid
troubles with trademarks. I had many funny names in mind but most of them would have been a cause of conflict
with existing products.

## Thierry-Rochebois-I-alpha000
It is my first github publication for almost 5 years (I was pretty active with the Axoloti then).
With alpha000 you already have 7 presets available (through midi program changes). 
They are based on a recreation of classic waveforms and hardsync.

I will add more details about the hardware options and the various Midi controls this week end.

## Configurations
### Base
1 Teensy and 1 I2S audio DAC
### Options
- OLED SH1106 (for oscilloscope and parameter display) 
- Midi Din (on RX1)
- Akai APC25 Mk2 on USBHost for direct sound editing.
- Secondary I2S audio DAC for symetrical outputs.

## Features

- 8 voice polyphony
- 96kHz sample rate (for a brighter sound and controlled aliasing)
- Reverberation
- Chorus
- CC control of synthesis parameters
- Presets through Midi Program Changes ( 7 presets already available on version alpha000).
