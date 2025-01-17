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
You only need a Teensy4.1 and a I2S audio DAC to try it.
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

## CC

| CC |    Description   | CC Values | Parameter Range |
|----|------------------|-----------|-----------------|
|   7|Volume            |  0..127   |                 |
|  91|Reverb            |  0..7     |                 |
|  93|Chorus Type       |  0..3     |                 |
|  74|Filter Cutoff     |  0..127   |                 |
|  73|Filter Mod        |  0..127   | -64..63         |
|  72|Filter Decay time |  0..127   |                 |

