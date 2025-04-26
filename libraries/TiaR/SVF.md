# Discretized "Zero" Delay State Variable Filters

## Introduction
my bsvf filters are a series of svf filters with different input output configurations
that can be useful in many cases (stereo processing, multiple sends, etc).

These svf filters are based on delay feedback reductions by matrix exponentiation.

The following details are extracted from an old Reaper discussion

https://forum.cockos.com/showpost.php?p=1574927&postcount=11

## Chamberlin filter

The Chamberlin filter is a simplistic yet efficient digital implementation
of the svf.
The main drawbacks are intabilities with low Qs (as discussed dy [Dattorro (see Section 3)](https://ccrma.stanford.edu/~dattorro/EffectDesignPart1.pdf))
This is caused by the use of a backward and a forward Euler integrator.

So, my starting point is the Chamberlin filter:

For every new input sample x, the state variables `lp` and `bp` are updated this way :

    n       n-1
    lp  <-              lp +              F * bp
    bp  <-  F * x - F * lp + (1 -F*D - F*F) * bp


`F` being related to the normalized frequency and `D` being a resonance factor (`1/2Q`).

## The Chamberlin Filter as a transition matrix 

The Chamberlin filter can be decribed as a transition matrix M:

         x     lp    bp
    x    1     0     0               constant input  output x is 1 times input x
    lp   0     1     F               bp feeds the lp integrator lp <- 1 lp + F bp 
    bp   F    -F     1 - F*D - F*F   double feedback on bp

## Matrix power is virtual upsampling

If you use `M^k` (the `k`th power of the matrix), instead of `M^1`,
it's just like if you iterated the filter `k` times...
its just like if you upsampled the filter `k` times with a constant input.

When you exponentiate `M` to `k=2^i` by hand (or with Maxima)
you quickly see that the form of matrix `M^k` remains

          x     lp    bp
    x     1     0     0
    lp    a     1-a   b
    bp    b     -b    c

## Simplification for control rate parameter evaluation

an iteration of the matrix exponentiation can be reduced to:

    i+1      i
    a   <-   b^2 + a * (2 - a)
    b   <-   b * (1 + c - a)
    c   <-   c^2 - b^2

So it's easy to get `M^2^i`.

With only 7 iterations on coefficients `a`,`b` and `c`, you get `M^128`.
If you use `M^128` instead of `M` you've got the equivalent of a x128
upsampling (with a constant input). You get better bandpass and lowpass
filter response and the filter is stable (which is not the case
of the Chamberlin filter).


## Audio rate

The audio rate processing is still quite simple:
Code:

    n       n-1
    lp  <-  a * x + (1 - a) * lp + b * bp;
    bp  <-  b * x       - b * lp + c * bp;



