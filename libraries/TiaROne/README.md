# Build instructions

## 1) 96kHz

Edit the `AudioStream.h` file under
C:\Users\\[user]\AppData\Local\Arduino15\packages\teensy\hardware\avr\1.59.0\cores\teensy4

    #ifndef AUDIO_BLOCK_SAMPLES
    #define AUDIO_BLOCK_SAMPLES  32
    #endif

    #ifndef AUDIO_SAMPLE_RATE_EXACT
    #define AUDIO_SAMPLE_RATE_EXACT 96000.0f
    #endif

## 2) Non blocking I2C driver

You will also need to install the great library teensy4_i2c from Richard-Gemmell
https://github.com/Richard-Gemmell/teensy4_i2c

## 3) copy the TiaR library

In order to compile the code you will need to put the TiaR directory into
your Arduino libraries directory.

This library contains objects that will be common to various synthesizers.

## 4) copy the TiaROne library

In order to compile the code you will need to put the TiaROne directory into
your Arduino libraries directory.

This library contains the specific code for the TiaROne project


