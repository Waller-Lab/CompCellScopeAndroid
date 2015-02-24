CompCellScope Reciever - v4, 2/23/15
Zack Phillips, UC Berkeley
zkphil@berkeley.edu

Hardware Requirements:
Arduino Micro
TLC5926 Controller-based LED array
(Optional) Bluetooth Controller

Features:
Bluetooth Array Control (Serial)
USB Array Control (Serial)
Arbitrary pattern generation
Basic triggering capabilities

Files:
- TLC5926 - library for controlling TLC5926 chips, copy to Program Files (x86)\Arduino\Libraries
- CompCellScopeReciever_v3.ino - Main File
- dpcmaps_bit.h - Array of Integers (bit patterns) corresponding to different array patterns generated with included MATLAB script, used for speed.
- arrayCoordinates.h - contains the hole position to serial position lookup table, also LED position in theta_x and theta_y relative to center of the array. Generated in MATLAB




