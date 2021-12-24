# AFMLuminescence

## Goal

This software aim to reproduce the luminescence of a quantum dot (QD) layer. It does so by simulating a QD layer with electrons, and simulate their recombinations.

This software has several execution mode:
- simulation of the luminescence, either under continuous light  or one shot illumination, of a sample with a randomly generated distribution of quantum dots from materials chosen by the user
- simulation of the luminescence, either under continuous light  or one shot illumination, of a sample generated from an AFM image entered by the user
- adjusting a distribution of quantum dot by comparing the simulated luminescence to an experimental, and try to deduce a more accurate QD distribution

## v1.0-beta

This version includes all the simulation goal: the software can generate the luminescence of a sample, either by generating a gaussian distribution of quantum or by generating it from a file entered by the user. It can simulate the luminescence either under single shot or under continuous excitation.

A tentative fitting algorithm has also be implemented, but the results are not satisfactory at the moment. Try it at your risks.

The alpha only has graphical interface for the ongoing simulation. All the information beforehand has to been entered in properties files in the /ressources folder, as described below.

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
