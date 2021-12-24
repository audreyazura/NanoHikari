# AFMLuminescence

## Goal

This software aim to reproduce the luminscence of a quantum dot (QD) layer. It does so by simulating a QD layer with electrons, and simulate their recombinations. The obtained spectra can be compared to one taken from an experiment, and the QD density can then be adjusted in order to extract the QD distribution from the luminescence.

## Alpha version

The software is in alpha at the moment. Most functionnality are there, and it can generate luminescence from an inputed Quantum Dot distribution. However, there is still a problem in the fitting function, and after a few fitting passes, the fit deviate strongly from the experimental luminescence. The reasons are still under investigation.

At the moment, the input in the software works with configuration files, all situated in the /src/ressources folder.

At the start, the software loads configuration/default.conf. This file must contain the adress to a luminescence file, as well as a metamaterial. Only the QD distribution file is optionnal. The material is refered to by its ID.

A metamaterial file describe the compound used (for instance, InAs QD in GaAs barrier in the given example). A metamaterial contains a list of the material used and their conduction band offset. Each material are refered to by their ID. The condution band offset property is labelled offset_\[material1\]\[material2\]. A metamaterial should be placed in the folder src/ressources/metamaterials with the extension \*.metamat. More information are given in the README file in the folder.

Each material composing the metamaterial (for instance, InAs and GaAs) should be describe in there own file. Such a file has the extension \*.mat and is placed in the folder /src/ressources/materials/. If the material describe the QD material, it should contain the values of the capture times, escape times and recombination times for different size of quantum dots, either as file or as a number (in case of constant time). More information are given in the README file in the folder.

Capture times, escape times and recombination times file goes respectively to the folder /src/ressources/capturetimes/, /src/ressources/escapetimes/ and /src/ressources/recombinationtimes/. The column in those file are separated by ";".

## Dependency

* JDK 17
* Java FX 17
* [audreyazura/PhysicsTools](https://github.com/audreyazura/PhysicsTools)
* [BigDecimalMath (by Dr. Richard J. Mathard)](https://arxiv.org/abs/0908.3030v4)
* [PCG (java implementation by Kilian Brachtendorf)](https://github.com/KilianB/pcg-java)
