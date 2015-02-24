Computational CellScope CAD/MATLAB Scripts
Zack Phillips, UC Berkeley
zkphil@berkeley.edu

Last Modified 2/23/15

The included files are licensed under the BSD Open Source License, as described in the LICENSE.txt file.

These files were written in MATLAB r2015b. They should also be compatible with GNU Octave, though this is untested.

--- File List ---
MATLAB Scripts:
compCellScope_genLEDCoordinates.m : Used to generate led position coordinates using hexagonal close packing, as well as PCB mounting angles.
compCellScope_genArduinoHeaders.m : Used to generate header files for the included Arduino sketch including DPC patterns and channel mapping.

Other Files:
ccs_domeHoleCoordinates.mat   : MATLAB variables containing spherical and cartesian coordinates of the dome holes
BrightfieldScanImageOrders.mat : contains list of brightfield images for each NA in the correct order, used when acquiring using the "sb" command on the Arduino interface
channelMap.xlsx : contains the hole-channel mappings for our device, as a sample. Yours will likley vary based on how you assemble the dome.
BrightfieldScanImageOrders.mat : The "sb" command scans brightfield images out of order due to the way they are stored in memory - this file contains the mapping of this arbitrary order to actual LED poitions for each NA.
