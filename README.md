# Thierry-Rochebois-I
Teensy4.1 based Transdimensional Synthesizer number one.

**You only need a Teensy4.1 and a I2S audio DAC to try it.**

## Transdimensional ?
_Transdimensional_ is a fancy name for a way to simplify the use of very complicated synthesis algorithm.
It is a kind of morphing that takes care of navigating through the synthesis parameter hyperspace.
_Thierry-Rochebois-I_ navigates in a 54 dimension hyperspace.

## Thierry-Rochebois-I
Yes, it's my real name. It can seem weird to name a synth after myself... but that's an easy way to avoid
troubles with trademarks. I had many funny names in mind but most of them would have been a cause of conflict
with existing products.

## Thierry-Rochebois-I-alpha000
It is my first github publication for almost 5 years (I was pretty active with the Axoloti then). I discovered the
Teensy4.1 a year ago.
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

## Presets (alpha000)
|PC|Preset|
|--|------|
|0|bass|
|1|china|
|2|chordéon (wheel)|
|3|west (wheel)|
|4|sync (wheel)|
|5|sync harp(wheel)|
|6|sync dream(wheel)|


## CC

|  CC  |    Description   | CC Values | Notes                    |
|------|------------------|-----------|--------------------------|
|   `7`|Volume            |  0..127   |                          |
|  `91`|Reverb            |  0..7     |                          |
|  `93`|Chorus Type       |  0..3     |                          |
|  `74`|Filter Cutoff     |  0..127   |                          |
|  `73`|Filter Mod        |  0..127   | scales to -64..63 semitones |
|  `72`|Filter Decay time |  0..127   |                          |
|  `71`|Filter Resonance  |  0..5     |                          |
|  `16`|Volume enveloppe  |  0..127   | 0 Gate -> 40 longer release -> 64 smooth AR -> Brass |
|   `1`|ModWheel = MORPH  |  0..127   | The Morph parameter      |
|  `18`|Morph Mod Depth   |  0..127   | if 127 the modulation takes full control and the modwheel has no effect|
|  `19`|Morph Mod Rate    |  0..127   | The rate of mod modulation |
|  `77`|Morph Function    |  0..7     | The morph modulator type... LFOs or enveloppes... see table below |
|  `78`|A1 half hyperspace point 1 |  0..31    | 0..11 available on alpha000 |
|  `79`|B1 half hyperspace point 1 |  0..32    | 0..11 + 32 available on alpha000 |
|  `75`|A0 half hyperspace point 1 |  0..31    | 0..11 available on alpha000 |
|  `76`|B0 half hyperspace point 1 |  0..32    | 0..11 + 32 available on alpha000 |
|  `94`|Wave symetry ratio | 0..3 | for more or less squarish sounds |

## Morph Function
Without morph modulation, the morphing from [A0,B0] to [A1,B1] is controlled by the modwheel.
With Morph Mod Depth `CC18` part of the morphing will be controlled by the Morph Function Generator which provides 8 different functions selected by `CC77`

|Value|Description|Note|
|-----|-----------|----|
|0|Decay|The most used mode|
|1|Note triggered LFO|starts like the decay and then goes back in the other direction...|
|2|Decay + Note triggered LFO |Useful to have the sound still "alive" after the decay|
|3|Decay + fast LFO ||
|4|Common LFO | A common LFO is great for Phaser like modulation of the sound when playing chords|
|5|Combination of LFOs| Vibrations... vibrations...|
|6|Velocity modulated Decay|Expressive|
|7|Random S&H|Let robots have a talk!|


