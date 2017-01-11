/*
 * Developed as part of the Computational CellScope Project
 * Waller Lab, EECS Dept., The University of California at Berkeley
 *
 * Licensed under the 3-Clause BSD License:
 *
 * Copyright © 2015 Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the owner nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.wallerlab.compcellscope;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wallerlab.compcellscope.bluetooth.BluetoothService;
import com.wallerlab.compcellscope.dialogs.AcquireSettings;
import com.wallerlab.compcellscope.dialogs.AcquireSettings.NoticeDialogListener;
import com.wallerlab.compcellscope.surfaceviews.AcquireSurfaceView;
import com.wallerlab.processing.datasets.Dataset;

public class AcquireActivity extends Activity implements OnTouchListener, NoticeDialogListener  {
	
    private static final String TAG = "cCS_Acquire";
    private static final boolean D = true;
    private Camera mCamera;
    private AcquireSurfaceView mPreview;
    private BluetoothService mBluetoothService = null;
    private String acquireType = "MultiMode";
    Button btnSetup, btnAcquire;
    private TextView acquireTextView;
    private TextView acquireTextView2;
    private TextView timeLeftTextView;
    private ProgressBar acquireProgressBar;
    
    public double objectiveNA = 0.3;
    public double brightfieldNA = 0.4; // Account for LED size to be sure we completly cover NA .025
    
    public int ledCount = 508;
    public int centerLED = 249;
    
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
    public String fileName = "default";
    public boolean cameraReady = true;
    public int mmCount = 1;
    public float mmDelay = 0.0f;
    public int aecCompensation = 0;
    public String datasetName = "Dataset";
    public boolean usingHDR = false;
    public boolean darkfieldAnnulus = true;
    public Dataset mDataset;
    
    public DialogFragment settingsDialogFragment;
    
    // FORMAT: hole number,, channel, 1000*Theta_x, 1000*Theta_y
    static final int[][] domeCoordinates = new int[][]{
    /*  1*/ {1, 320, -113, -726 }, 
    /*  2*/ {2, 257, -38, -726 }, 
    /*  3*/ {3, 256, 38, -726 }, 
    /*  4*/ {4, 200, 113, -726 }, 
    /*  5*/ {5, 384, -298, -676 }, 
    /*  6*/ {6, 392, -223, -676 }, 
    /*  7*/ {7, 321, -149, -676 }, 
    /*  8*/ {8, 328, -74, -676 }, 
    /*  9*/ {9, 264, 0, -676 }, 
    /* 10*/ {10, 193, 74, -676 }, 
    /* 11*/ {11, 201, 149, -676 }, 
    /* 12*/ {12, 136, 223, -676 }, 
    /* 13*/ {13, 128, 298, -676 }, 
    /* 14*/ {14, 456, -405, -626 }, 
    /* 15*/ {15, 386, -331, -626 }, 
    /* 16*/ {16, 393, -258, -626 }, 
    /* 17*/ {17, 322, -184, -626 }, 
    /* 18*/ {18, 329, -110, -626 }, 
    /* 19*/ {19, 258, -37, -626 }, 
    /* 20*/ {20, 265, 37, -626 }, 
    /* 21*/ {21, 203, 110, -626 }, 
    /* 22*/ {22, 192, 184, -626 }, 
    /* 23*/ {23, 137, 258, -626 }, 
    /* 24*/ {24, 130, 331, -626 }, 
    /* 25*/ {25, 73, 405, -626 }, 
    /* 26*/ {26, 457, -437, -576 }, 
    /* 27*/ {27, 387, -364, -576 }, 
    /* 28*/ {28, 385, -291, -576 }, 
    /* 29*/ {29, 394, -218, -576 }, 
    /* 30*/ {30, 330, -146, -576 }, 
    /* 31*/ {31, 259, -73, -576 }, 
    /* 32*/ {32, 266, 0, -576 }, 
    /* 33*/ {33, 195, 73, -576 }, 
    /* 34*/ {34, 202, 146, -576 }, 
    /* 35*/ {35, 138, 218, -576 }, 
    /* 36*/ {36, 131, 291, -576 }, 
    /* 37*/ {37, 129, 364, -576 }, 
    /* 38*/ {38, 74, 437, -576 }, 
    /* 39*/ {39, 449, -540, -526 }, 
    /* 40*/ {40, 451, -468, -526 }, 
    /* 41*/ {41, 515, -396, -526 }, 
    /* 42*/ {42, 395, -324, -526 }, 
    /* 43*/ {43, 396, -252, -526 }, 
    /* 44*/ {44, 323, -180, -526 }, 
    /* 45*/ {45, 331, -108, -526 }, 
    /* 46*/ {46, 267, -36, -526 }, 
    /* 47*/ {47, 268, 36, -526 }, 
    /* 48*/ {48, 204, 108, -526 }, 
    /* 49*/ {49, 194, 180, -526 }, 
    /* 50*/ {50, 141, 252, -526 }, 
    /* 51*/ {51, 132, 324, -526 }, 
    /* 52*/ {52, 75, 396, -526 }, 
    /* 53*/ {53, 68, 468, -526 }, 
    /* 54*/ {54, 64, 540, -526 }, 
    /* 55*/ {55, 518, -570, -476 }, 
    /* 56*/ {56, 523, -498, -476 }, 
    /* 57*/ {57, 460, -427, -476 }, 
    /* 58*/ {58, 388, -356, -476 }, 
    /* 59*/ {59, 397, -285, -476 }, 
    /* 60*/ {60, 324, -214, -476 }, 
    /* 61*/ {61, 512, -142, -476 }, 
    /* 62*/ {62, 260, -71, -476 }, 
    /* 63*/ {63, 269, 0, -476 }, 
    /* 64*/ {64, 196, 71, -476 }, 
    /* 65*/ {65, 3, 142, -476 }, 
    /* 66*/ {66, 142, 214, -476 }, 
    /* 67*/ {67, 140, 285, -476 }, 
    /* 68*/ {68, 76, 356, -476 }, 
    /* 69*/ {69, 77, 427, -476 }, 
    /* 70*/ {70, 12, 498, -476 }, 
    /* 71*/ {71, 65, 570, -476 }, 
    /* 72*/ {72, 453, -598, -426 }, 
    /* 73*/ {73, 461, -528, -426 }, 
    /* 74*/ {74, 462, -458, -426 }, 
    /* 75*/ {75, 452, -387, -426 }, 
    /* 76*/ {76, 389, -317, -426 }, 
    /* 77*/ {77, 325, -246, -426 }, 
    /* 78*/ {78, 334, -176, -426 }, 
    /* 79*/ {79, 333, -106, -426 }, 
    /* 80*/ {80, 270, -35, -426 }, 
    /* 81*/ {81, 271, 35, -426 }, 
    /* 82*/ {82, 205, 106, -426 }, 
    /* 83*/ {83, 199, 176, -426 }, 
    /* 84*/ {84, 143, 246, -426 }, 
    /* 85*/ {85, 133, 317, -426 }, 
    /* 86*/ {86, 78, 387, -426 }, 
    /* 87*/ {87, 70, 458, -426 }, 
    /* 88*/ {88, 69, 528, -426 }, 
    /* 89*/ {89, 66, 598, -426 }, 
    /* 90*/ {90, 454, -626, -376 }, 
    /* 91*/ {91, 455, -557, -376 }, 
    /* 92*/ {92, 463, -487, -376 }, 
    /* 93*/ {93, 400, -418, -376 }, 
    /* 94*/ {94, 390, -348, -376 }, 
    /* 95*/ {95, 398, -278, -376 }, 
    /* 96*/ {96, 326, -209, -376 }, 
    /* 97*/ {97, 335, -139, -376 }, 
    /* 98*/ {98, 262, -70, -376 }, 
    /* 99*/ {99, 280, 0, -376 }, 
    /*100*/ {100, 198, 70, -376 }, 
    /*101*/ {101, 206, 139, -376 }, 
    /*102*/ {102, 209, 209, -376 }, 
    /*103*/ {103, 152, 278, -376 }, 
    /*104*/ {104, 135, 348, -376 }, 
    /*105*/ {105, 79, 418, -376 }, 
    /*106*/ {106, 71, 487, -376 }, 
    /*107*/ {107, 67, 557, -376 }, 
    /*108*/ {108, 13, 626, -376 }, 
    /*109*/ {109, 527, -654, -325 }, 
    /*110*/ {110, 464, -585, -325 }, 
    /*111*/ {111, 472, -516, -325 }, 
    /*112*/ {112, 473, -447, -325 }, 
    /*113*/ {113, 391, -378, -325 }, 
    /*114*/ {114, 399, -310, -325 }, 
    /*115*/ {115, 327, -241, -325 }, 
    /*116*/ {116, 336, -172, -325 }, 
    /*117*/ {117, 263, -103, -325 }, 
    /*118*/ {118, 261, -34, -325 }, 
    /*119*/ {119, 281, 34, -325 }, 
    /*120*/ {120, 207, 103, -325 }, 
    /*121*/ {121, 217, 172, -325 }, 
    /*122*/ {122, 154, 241, -325 }, 
    /*123*/ {123, 134, 310, -325 }, 
    /*124*/ {124, 9, 378, -325 }, 
    /*125*/ {125, 81, 447, -325 }, 
    /*126*/ {126, 80, 516, -325 }, 
    /*127*/ {127, 15, 585, -325 }, 
    /*128*/ {128, 14, 654, -325 }, 
    /*129*/ {129, 528, -680, -275 }, 
    /*130*/ {130, 537, -612, -275 }, 
    /*131*/ {131, 466, -544, -275 }, 
    /*132*/ {132, 474, -476, -275 }, 
    /*133*/ {133, 402, -408, -275 }, 
    /*134*/ {134, 409, -340, -275 }, 
    /*135*/ {135, 408, -272, -275 }, 
    /*136*/ {136, 337, -204, -275 }, 
    /*137*/ {137, 344, -136, -275 }, 
    /*138*/ {138, 272, -68, -275 }, 
    /*139*/ {139, 282, 0, -275 }, 
    /*140*/ {140, 208, 68, -275 }, 
    /*141*/ {141, 219, 136, -275 }, 
    /*142*/ {142, 216, 204, -275 }, 
    /*143*/ {143, 153, 272, -275 }, 
    /*144*/ {144, 144, 340, -275 }, 
    /*145*/ {145, 11, 408, -275 }, 
    /*146*/ {146, 82, 476, -275 }, 
    /*147*/ {147, 4, 544, -275 }, 
    /*148*/ {148, 24, 612, -275 }, 
    /*149*/ {149, 5, 680, -275 }, 
    /*150*/ {150, 532, -706, -225 }, 
    /*151*/ {151, 538, -638, -225 }, 
    /*152*/ {152, 465, -571, -225 }, 
    /*153*/ {153, 475, -504, -225 }, 
    /*154*/ {154, 401, -437, -225 }, 
    /*155*/ {155, 403, -370, -225 }, 
    /*156*/ {156, 411, -302, -225 }, 
    /*157*/ {157, 338, -235, -225 }, 
    /*158*/ {158, 346, -168, -225 }, 
    /*159*/ {159, 347, -101, -225 }, 
    /*160*/ {160, 273, -34, -225 }, 
    /*161*/ {161, 283, 34, -225 }, 
    /*162*/ {162, 221, 101, -225 }, 
    /*163*/ {163, 218, 168, -225 }, 
    /*164*/ {164, 210, 235, -225 }, 
    /*165*/ {165, 155, 302, -225 }, 
    /*166*/ {166, 145, 370, -225 }, 
    /*167*/ {167, 89, 437, -225 }, 
    /*168*/ {168, 83, 504, -225 }, 
    /*169*/ {169, 2, 571, -225 }, 
    /*170*/ {170, 17, 638, -225 }, 
    /*171*/ {171, 6, 706, -225 }, 
    /*172*/ {172, 530, -664, -175 }, 
    /*173*/ {173, 539, -598, -175 }, 
    /*174*/ {174, 467, -531, -175 }, 
    /*175*/ {175, 476, -465, -175 }, 
    /*176*/ {176, 404, -398, -175 }, 
    /*177*/ {177, 412, -332, -175 }, 
    /*178*/ {178, 513, -266, -175 }, 
    /*179*/ {179, 341, -199, -175 }, 
    /*180*/ {180, 348, -133, -175 }, 
    /*181*/ {181, 275, -66, -175 }, 
    /*182*/ {182, 284, 0, -175 }, 
    /*183*/ {183, 212, 66, -175 }, 
    /*184*/ {184, 220, 133, -175 }, 
    /*185*/ {185, 211, 199, -175 }, 
    /*186*/ {186, 156, 266, -175 }, 
    /*187*/ {187, 146, 332, -175 }, 
    /*188*/ {188, 58, 398, -175 }, 
    /*189*/ {189, 92, 465, -175 }, 
    /*190*/ {190, 84, 531, -175 }, 
    /*191*/ {191, 28, 598, -175 }, 
    /*192*/ {192, 16, 664, -175 }, 
    /*193*/ {193, 534, -689, -125 }, 
    /*194*/ {194, 540, -623, -125 }, 
    /*195*/ {195, 468, -558, -125 }, 
    /*196*/ {196, 469, -492, -125 }, 
    /*197*/ {197, 477, -426, -125 }, 
    /*198*/ {198, 406, -361, -125 }, 
    /*199*/ {199, 413, -295, -125 }, 
    /*200*/ {200, 339, -230, -125 }, 
    /*201*/ {201, 349, -164, -125 }, 
    /*202*/ {202, 277, -98, -125 }, 
    /*203*/ {203, 274, -33, -125 }, 
    /*204*/ {204, 286, 33, -125 }, 
    /*205*/ {205, 222, 98, -125 }, 
    /*206*/ {206, 213, 164, -125 }, 
    /*207*/ {207, 149, 230, -125 }, 
    /*208*/ {208, 158, 295, -125 }, 
    /*209*/ {209, 147, 361, -125 }, 
    /*210*/ {210, 94, 426, -125 }, 
    /*211*/ {211, 85, 492, -125 }, 
    /*212*/ {212, 27, 558, -125 }, 
    /*213*/ {213, 19, 623, -125 }, 
    /*214*/ {214, 7, 689, -125 }, 
    /*215*/ {215, 535, -713, -75 }, 
    /*216*/ {216, 541, -648, -75 }, 
    /*217*/ {217, 471, -583, -75 }, 
    /*218*/ {218, 470, -518, -75 }, 
    /*219*/ {219, 478, -454, -75 }, 
    /*220*/ {220, 405, -389, -75 }, 
    /*221*/ {221, 570, -324, -75 }, 
    /*222*/ {222, 414, -259, -75 }, 
    /*223*/ {223, 342, -194, -75 }, 
    /*224*/ {224, 448, -130, -75 }, 
    /*225*/ {225, 276, -65, -75 }, 
    /*226*/ {226, 287, 0, -75 }, 
    /*227*/ {227, 285, 65, -75 }, 
    /*228*/ {228, 223, 130, -75 }, 
    /*229*/ {229, 214, 194, -75 }, 
    /*230*/ {230, 159, 259, -75 }, 
    /*231*/ {231, 150, 324, -75 }, 
    /*232*/ {232, 148, 389, -75 }, 
    /*233*/ {233, 93, 454, -75 }, 
    /*234*/ {234, 86, 518, -75 }, 
    /*235*/ {235, 30, 583, -75 }, 
    /*236*/ {236, 18, 648, -75 }, 
    /*237*/ {237, 20, 713, -75 }, 
    /*238*/ {238, 552, -736, -25 }, 
    /*239*/ {239, 542, -672, -25 }, 
    /*240*/ {240, 488, -608, -25 }, 
    /*241*/ {241, 490, -544, -25 }, 
    /*242*/ {242, 480, -480, -25 }, 
    /*243*/ {243, 479, -416, -25 }, 
    /*244*/ {244, 424, -352, -25 }, 
    /*245*/ {245, 415, -288, -25 }, 
    /*246*/ {246, 360, -224, -25 }, 
    /*247*/ {247, 351, -160, -25 }, 
    /*248*/ {248, 278, -96, -25 }, 
    /*249*/ {249, 296, -32, -25 }, 
    /*250*/ {250, 288, 32, -25 }, 
    /*251*/ {251, 224, 96, -25 }, 
    /*252*/ {252, 53, 160, -25 }, 
    /*253*/ {253, 161, 224, -25 }, 
    /*254*/ {254, 151, 288, -25 }, 
    /*255*/ {255, 97, 352, -25 }, 
    /*256*/ {256, 95, 416, -25 }, 
    /*257*/ {257, 104, 480, -25 }, 
    /*258*/ {258, 31, 544, -25 }, 
    /*259*/ {259, 22, 608, -25 }, 
    /*260*/ {260, 23, 672, -25 }, 
    /*261*/ {261, 21, 736, -25 }, 
    /*262*/ {262, 553, -704, 26 }, 
    /*263*/ {263, 544, -640, 26 }, 
    /*264*/ {264, 489, -576, 26 }, 
    /*265*/ {265, 491, -512, 26 }, 
    /*266*/ {266, 481, -448, 26 }, 
    /*267*/ {267, 563, -384, 26 }, 
    /*268*/ {268, 416, -320, 26 }, 
    /*269*/ {269, 417, -256, 26 }, 
    /*270*/ {270, 343, -192, 26 }, 
    /*271*/ {271, 279, -128, 26 }, 
    /*272*/ {272, 297, -64, 26 }, 
    /*273*/ {273, 289, 0, 26 }, 
    /*274*/ {274, 215, 64, 26 }, 
    /*275*/ {275, 226, 128, 26 }, 
    /*276*/ {276, 8, 192, 26 }, 
    /*277*/ {277, 160, 256, 26 }, 
    /*278*/ {278, 168, 320, 26 }, 
    /*279*/ {279, 96, 384, 26 }, 
    /*280*/ {280, 105, 448, 26 }, 
    /*281*/ {281, 87, 512, 26 }, 
    /*282*/ {282, 33, 576, 26 }, 
    /*283*/ {283, 40, 640, 26 }, 
    /*284*/ {284, 41, 704, 26 }, 
    /*285*/ {285, 555, -680, 77 }, 
    /*286*/ {286, 545, -616, 77 }, 
    /*287*/ {287, 492, -551, 77 }, 
    /*288*/ {288, 482, -486, 77 }, 
    /*289*/ {289, 426, -421, 77 }, 
    /*290*/ {290, 419, -356, 77 }, 
    /*291*/ {291, 362, -292, 77 }, 
    /*292*/ {292, 361, -227, 77 }, 
    /*293*/ {293, 353, -162, 77 }, 
    /*294*/ {294, 352, -97, 77 }, 
    /*295*/ {295, 298, -32, 77 }, 
    /*296*/ {296, 290, 32, 77 }, 
    /*297*/ {297, 227, 97, 77 }, 
    /*298*/ {298, 225, 162, 77 }, 
    /*299*/ {299, 162, 227, 77 }, 
    /*300*/ {300, 169, 292, 77 }, 
    /*301*/ {301, 172, 356, 77 }, 
    /*302*/ {302, 98, 421, 77 }, 
    /*303*/ {303, 107, 486, 77 }, 
    /*304*/ {304, 106, 551, 77 }, 
    /*305*/ {305, 42, 616, 77 }, 
    /*306*/ {306, 43, 680, 77 }, 
    /*307*/ {307, 554, -722, 129 }, 
    /*308*/ {308, 556, -656, 129 }, 
    /*309*/ {309, 546, -590, 129 }, 
    /*310*/ {310, 493, -525, 129 }, 
    /*311*/ {311, 483, -459, 129 }, 
    /*312*/ {312, 420, -394, 129 }, 
    /*313*/ {313, 418, -328, 129 }, 
    /*314*/ {314, 364, -262, 129 }, 
    /*315*/ {315, 354, -197, 129 }, 
    /*316*/ {316, 355, -131, 129 }, 
    /*317*/ {317, 300, -66, 129 }, 
    /*318*/ {318, 291, 0, 129 }, 
    /*319*/ {319, 234, 66, 129 }, 
    /*320*/ {320, 50, 131, 129 }, 
    /*321*/ {321, 163, 197, 129 }, 
    /*322*/ {322, 170, 262, 129 }, 
    /*323*/ {323, 171, 328, 129 }, 
    /*324*/ {324, 99, 394, 129 }, 
    /*325*/ {325, 100, 459, 129 }, 
    /*326*/ {326, 108, 525, 129 }, 
    /*327*/ {327, 34, 590, 129 }, 
    /*328*/ {328, 44, 656, 129 }, 
    /*329*/ {329, 45, 722, 129 }, 
    /*330*/ {330, 557, -697, 180 }, 
    /*331*/ {331, 549, -631, 180 }, 
    /*332*/ {332, 547, -564, 180 }, 
    /*333*/ {333, 484, -498, 180 }, 
    /*334*/ {334, 427, -432, 180 }, 
    /*335*/ {335, 421, -365, 180 }, 
    /*336*/ {336, 363, -299, 180 }, 
    /*337*/ {337, 365, -232, 180 }, 
    /*338*/ {338, 357, -166, 180 }, 
    /*339*/ {339, 356, -100, 180 }, 
    /*340*/ {340, 299, -33, 180 }, 
    /*341*/ {341, 292, 33, 180 }, 
    /*342*/ {342, 228, 100, 180 }, 
    /*343*/ {343, 237, 166, 180 }, 
    /*344*/ {344, 239, 232, 180 }, 
    /*345*/ {345, 164, 299, 180 }, 
    /*346*/ {346, 174, 365, 180 }, 
    /*347*/ {347, 101, 432, 180 }, 
    /*348*/ {348, 109, 498, 180 }, 
    /*349*/ {349, 60, 564, 180 }, 
    /*350*/ {350, 36, 631, 180 }, 
    /*351*/ {351, 46, 697, 180 }, 
    /*352*/ {352, 558, -672, 232 }, 
    /*353*/ {353, 548, -605, 232 }, 
    /*354*/ {354, 494, -538, 232 }, 
    /*355*/ {355, 485, -470, 232 }, 
    /*356*/ {356, 429, -403, 232 }, 
    /*357*/ {357, 422, -336, 232 }, 
    /*358*/ {358, 367, -269, 232 }, 
    /*359*/ {359, 366, -202, 232 }, 
    /*360*/ {360, 358, -134, 232 }, 
    /*361*/ {361, 301, -67, 232 }, 
    /*362*/ {362, 293, 0, 232 }, 
    /*363*/ {363, 236, 67, 232 }, 
    /*364*/ {364, 231, 134, 232 }, 
    /*365*/ {365, 238, 202, 232 }, 
    /*366*/ {366, 165, 269, 232 }, 
    /*367*/ {367, 173, 336, 232 }, 
    /*368*/ {368, 102, 403, 232 }, 
    /*369*/ {369, 110, 470, 232 }, 
    /*370*/ {370, 38, 538, 232 }, 
    /*371*/ {371, 37, 605, 232 }, 
    /*372*/ {372, 63, 672, 232 }, 
    /*373*/ {373, 559, -646, 283 }, 
    /*374*/ {374, 495, -578, 283 }, 
    /*375*/ {375, 487, -510, 283 }, 
    /*376*/ {376, 486, -442, 283 }, 
    /*377*/ {377, 430, -374, 283 }, 
    /*378*/ {378, 428, -306, 283 }, 
    /*379*/ {379, 383, -238, 283 }, 
    /*380*/ {380, 359, -170, 283 }, 
    /*381*/ {381, 319, -102, 283 }, 
    /*382*/ {382, 302, -34, 283 }, 
    /*383*/ {383, 567, 34, 283 }, 
    /*384*/ {384, 229, 102, 283 }, 
    /*385*/ {385, 230, 170, 283 }, 
    /*386*/ {386, 166, 238, 283 }, 
    /*387*/ {387, 167, 306, 283 }, 
    /*388*/ {388, 175, 374, 283 }, 
    /*389*/ {389, 103, 442, 283 }, 
    /*390*/ {390, 111, 510, 283 }, 
    /*391*/ {391, 39, 578, 283 }, 
    /*392*/ {392, 62, 646, 283 }, 
    /*393*/ {393, 511, -619, 335 }, 
    /*394*/ {394, 509, -550, 335 }, 
    /*395*/ {395, 496, -482, 335 }, 
    /*396*/ {396, 446, -413, 335 }, 
    /*397*/ {397, 431, -344, 335 }, 
    /*398*/ {398, 423, -275, 335 }, 
    /*399*/ {399, 382, -206, 335 }, 
    /*400*/ {400, 368, -138, 335 }, 
    /*401*/ {401, 303, -69, 335 }, 
    /*402*/ {402, 295, 0, 335 }, 
    /*403*/ {403, 565, 69, 335 }, 
    /*404*/ {404, 240, 138, 335 }, 
    /*405*/ {405, 254, 206, 335 }, 
    /*406*/ {406, 176, 275, 335 }, 
    /*407*/ {407, 191, 344, 335 }, 
    /*408*/ {408, 61, 413, 335 }, 
    /*409*/ {409, 126, 482, 335 }, 
    /*410*/ {410, 127, 550, 335 }, 
    /*411*/ {411, 49, 619, 335 }, 
    /*412*/ {412, 510, -592, 386 }, 
    /*413*/ {413, 508, -522, 386 }, 
    /*414*/ {414, 497, -452, 386 }, 
    /*415*/ {415, 566, -383, 386 }, 
    /*416*/ {416, 447, -313, 386 }, 
    /*417*/ {417, 432, -244, 386 }, 
    /*418*/ {418, 381, -174, 386 }, 
    /*419*/ {419, 369, -104, 386 }, 
    /*420*/ {420, 305, -35, 386 }, 
    /*421*/ {421, 569, 35, 386 }, 
    /*422*/ {422, 568, 104, 386 }, 
    /*423*/ {423, 253, 174, 386 }, 
    /*424*/ {424, 177, 244, 386 }, 
    /*425*/ {425, 190, 313, 386 }, 
    /*426*/ {426, 47, 383, 386 }, 
    /*427*/ {427, 125, 452, 386 }, 
    /*428*/ {428, 48, 522, 386 }, 
    /*429*/ {429, 51, 592, 386 }, 
    /*430*/ {430, 507, -563, 438 }, 
    /*431*/ {431, 506, -493, 438 }, 
    /*432*/ {432, 498, -422, 438 }, 
    /*433*/ {433, 564, -352, 438 }, 
    /*434*/ {434, 434, -282, 438 }, 
    /*435*/ {435, 380, -211, 438 }, 
    /*436*/ {436, 370, -141, 438 }, 
    /*437*/ {437, 318, -70, 438 }, 
    /*438*/ {438, 306, 0, 438 }, 
    /*439*/ {439, 252, 70, 438 }, 
    /*440*/ {440, 242, 141, 438 }, 
    /*441*/ {441, 179, 211, 438 }, 
    /*442*/ {442, 187, 282, 438 }, 
    /*443*/ {443, 189, 352, 438 }, 
    /*444*/ {444, 124, 422, 438 }, 
    /*445*/ {445, 123, 493, 438 }, 
    /*446*/ {446, 52, 563, 438 }, 
    /*447*/ {447, 505, -534, 489 }, 
    /*448*/ {448, 501, -463, 489 }, 
    /*449*/ {449, 499, -392, 489 }, 
    /*450*/ {450, 443, -320, 489 }, 
    /*451*/ {451, 436, -249, 489 }, 
    /*452*/ {452, 371, -178, 489 }, 
    /*453*/ {453, 316, -107, 489 }, 
    /*454*/ {454, 317, -36, 489 }, 
    /*455*/ {455, 307, 36, 489 }, 
    /*456*/ {456, 244, 107, 489 }, 
    /*457*/ {457, 251, 178, 489 }, 
    /*458*/ {458, 178, 249, 489 }, 
    /*459*/ {459, 188, 320, 489 }, 
    /*460*/ {460, 114, 392, 489 }, 
    /*461*/ {461, 122, 463, 489 }, 
    /*462*/ {462, 54, 534, 489 }, 
    /*463*/ {463, 504, -504, 541 }, 
    /*464*/ {464, 502, -432, 541 }, 
    /*465*/ {465, 500, -360, 541 }, 
    /*466*/ {466, 442, -288, 541 }, 
    /*467*/ {467, 379, -216, 541 }, 
    /*468*/ {468, 372, -144, 541 }, 
    /*469*/ {469, 315, -72, 541 }, 
    /*470*/ {470, 308, 0, 541 }, 
    /*471*/ {471, 249, 72, 541 }, 
    /*472*/ {472, 243, 144, 541 }, 
    /*473*/ {473, 180, 216, 541 }, 
    /*474*/ {474, 186, 288, 541 }, 
    /*475*/ {475, 115, 360, 541 }, 
    /*476*/ {476, 120, 432, 541 }, 
    /*477*/ {477, 121, 504, 541 }, 
    /*478*/ {478, 503, -400, 592 }, 
    /*479*/ {479, 441, -328, 592 }, 
    /*480*/ {480, 437, -255, 592 }, 
    /*481*/ {481, 378, -182, 592 }, 
    /*482*/ {482, 373, -109, 592 }, 
    /*483*/ {483, 314, -36, 592 }, 
    /*484*/ {484, 309, 36, 592 }, 
    /*485*/ {485, 245, 109, 592 }, 
    /*486*/ {486, 181, 182, 592 }, 
    /*487*/ {487, 185, 255, 592 }, 
    /*488*/ {488, 116, 328, 592 }, 
    /*489*/ {489, 57, 400, 592 }, 
    /*490*/ {490, 440, -368, 644 }, 
    /*491*/ {491, 438, -294, 644 }, 
    /*492*/ {492, 377, -221, 644 }, 
    /*493*/ {493, 374, -147, 644 }, 
    /*494*/ {494, 313, -74, 644 }, 
    /*495*/ {495, 310, 0, 644 }, 
    /*496*/ {496, 248, 74, 644 }, 
    /*497*/ {497, 250, 147, 644 }, 
    /*498*/ {498, 182, 221, 644 }, 
    /*499*/ {499, 184, 294, 644 }, 
    /*500*/ {500, 119, 368, 644 }, 
    /*501*/ {501, 439, -260, 695 }, 
    /*502*/ {502, 376, -186, 695 }, 
    /*503*/ {503, 375, -112, 695 }, 
    /*504*/ {504, 312, -37, 695 }, 
    /*505*/ {505, 311, 37, 695 }, 
    /*506*/ {506, 247, 112, 695 }, 
    /*507*/ {507, 246, 186, 695 }, 
    /*508*/ {508, 183, 260, 695 } 
    };
    
    @SuppressLint("ClickableViewAccessibility")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acquire_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Bundle b = getIntent().getExtras();
        if(b!=null)
        {
            acquireType =(String) b.get("type");
        }
        
    	GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
    	mBluetoothService = BTAppClass.getBluetoothService();
    	mDataset = BTAppClass.getDataset();
  
        btnSetup = (Button) findViewById(R.id.btnSetup);
        btnAcquire = (Button) findViewById(R.id.btnSaveFrame);
        
        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        acquireTextView2 = (TextView) findViewById(R.id.acquireStatusTextView2);
        timeLeftTextView = (TextView) findViewById(R.id.timeLeftTextView);
        acquireProgressBar = (ProgressBar) findViewById(R.id.acquireProgressBar);
        
        acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up
        if (mBluetoothService != null)
        	acquireTextView.setText(String.format("MODE: %s, ARRAY: Connected", acquireType));
        else
        	acquireTextView.setText(String.format("MODE: %s ARRAY: Not Connected", acquireType));
        acquireTextView.setTextColor(Color.YELLOW);
        acquireTextView2.setTextColor(Color.YELLOW);
        timeLeftTextView.setTextColor(Color.YELLOW);
        
	    settingsDialogFragment = new AcquireSettings();
        
        btnAcquire.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if (mBluetoothService != null){
            		if (acquireType.equals("MultiMode"))
            		{
            			objectiveNA=brightfieldNA;
            			new runMultiMode().execute();
            		}
            		else if (acquireType.equals("Brightfield_Scan"))
            		{
            			objectiveNA=brightfieldNA;
            			new runScanningMode().execute();
            		}
            		else if (acquireType.equals("Full_Scan"))
            		{
            			objectiveNA=1.0;
            			new runScanningMode().execute();     
            		}
            		else if (acquireType.equals("DPC_Stack"))
            		{
            			objectiveNA=brightfieldNA;
            			new runScanningMode().execute();     
            		}
            	}
            }
          });
        
        btnSetup.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	openSettingsDialog();
            }
          });
        
        btnSetup.setTextColor(Color.parseColor("yellow")); //SET CUSTOM COLOR 
        btnAcquire.setTextColor(Color.parseColor("yellow")); //SET CUSTOM COLOR 
        
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the instance of Camera
        mCamera = getCameraInstance();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR | ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // access camera Parameters
        final Camera.Parameters camParams = mCamera.getParameters();
        
        //set color effects to none
        camParams.setColorEffect(Camera.Parameters.EFFECT_NONE);

         //set antibanding to none
        if (camParams.getAntibanding() != null) {
        	camParams.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
        }

        // set white balance
        if (camParams.getWhiteBalance() != null) {
        	camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }
        
        // set images to maximum resolution
        List<Size> sizes = camParams.getSupportedPictureSizes();
        Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {
        	//String msg = String.format("%dh,%dw",sizes.get(i).height,sizes.get(i).width);
        	//Log.d(TAG, msg);
            if (sizes.get(i).width > size.width)
                size = sizes.get(i);
            
        }
        camParams.setPictureSize(size.width, size.height);
        acquireTextView2.setText(String.format("%dx%d", size.width,size.height));
        
        // Turn on AEC
        camParams.setAutoExposureLock(false);
        
        // Set parameters
        mCamera.setParameters(camParams);
        
        // Get camera ID of rear camera
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
          CameraInfo info = new CameraInfo();
          Camera.getCameraInfo(i, info);
          if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            cameraId = i;
            break;
          }
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.canDisableShutterSound) {
            mCamera.enableShutterSound(false);
        }
        
        
        
        // Callbacks for camera acquires
        shutterCallback = new ShutterCallback() {
            public void onShutter() {
                //Log.i("Log", "onShutter'd");
            }
        };
        
        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) 
            {
                FileOutputStream outStream = null;
	                String imageFileName = fileName + ".jpeg";
	                File storageDir = Environment.getExternalStorageDirectory();
	                String path = storageDir+imageFileName;
                   try {

	                   outStream = new FileOutputStream(String.format(path));
                       outStream.write(data);
                       outStream.close();
                       Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to path: " + path);
                   } catch (FileNotFoundException e) {
                	   Log.d(TAG, "onPictureTaken - jpeg - directory not found");
                       e.printStackTrace();
                   } catch (IOException e) {
                       e.printStackTrace();
                       Log.d(TAG, "onPictureTaken - jpeg - IO Exception");
                   } finally {
                   }
                   camera.startPreview();
                   cameraReady = true;
                   // Add file to the mediaStore
                   //MediaScannerConnection.scanFile(AcquireActivity.this,new String[] { path }, null,null);
            }
        };

        // Create our Preview view and set it as the content of our activity.
        mPreview = new AcquireSurfaceView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_view);
        preview.addView(mPreview);
        
        mPreview.setOnTouchListener(new OnTouchListener() 
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) 
            {
            	final Camera.Parameters myParams = mCamera.getParameters();
            	myParams.setAutoExposureLock(true);
            	myParams.setExposureCompensation(aecCompensation);
                mCamera.autoFocus(null);
                mCamera.setParameters(myParams);
                return false;
            }
        });
        // Set the NA of the objective on the arduino
        sendData(String.format("na,%d", (int) Math.round(brightfieldNA*100)));
        
        // turn on center LED to start
        // String cmd = String.format("p%d", centerLED);
        // sendData(cmd);
		sendData("an");
        
        
    }
    
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    
    private class runMultiMode extends AsyncTask<Void, Void, Void>
    {
        int n = 0;
        long t = 0;
        Camera.Parameters camParams;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS",Locale.US).format(new Date());
        String path = "/CellScope/"+"multimode_"+datasetName+"_"+timestamp +"/";
        File myDir = new File(Environment.getExternalStorageDirectory()+path);
        //params.setExposureCompensation(params.getMinExposureCompensation());
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            timeLeftTextView.setText("Time left:");
            acquireTextView.setText(String.format("Acquiring - MODE: %s", acquireType));
            acquireProgressBar.setVisibility(View.VISIBLE); // Make invisible at first, then have it pop up
        	acquireProgressBar.setMax(5*mmCount);
        	
        	camParams = mCamera.getParameters();
    		camParams.setExposureCompensation(aecCompensation);
        	camParams.setAutoExposureLock(false);
    		mCamera.setParameters(camParams);
    	
    		sendData("an");
    		try { 
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		
    		//Log.d(TAG,String.format("!!!AEC Compensation: %d max, %d min", camParams.getMaxExposureCompensation(), camParams.getMinExposureCompensation()));
    		//Log.d(TAG,mCamera.getParameters().flatten());
    		//params.setExposureCompensation(params.getMinExposureCompensation());
    		camParams.setAutoExposureLock(true);
    		//params.setExposureCompensation(params.getMinExposureCompensation());
    		mCamera.setParameters(camParams);
            myDir.mkdirs();
        }
        
        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(n);
    		long elapsed = SystemClock.elapsedRealtime() - t;
    		t = SystemClock.elapsedRealtime();
    		float timeLeft = (float)(((long)(mmCount*5 - n)*elapsed)/1000.0);
    		timeLeftTextView.setText(String.format("Time left: %.2f seconds, %d/%d images saved", timeLeft,n,5*mmCount));
    		Log.d(TAG,String.format("Time left: %.2f seconds", timeLeft));
        }
        
        void mSleep(int sleepVal)
        {
    		try {
				Thread.sleep(sleepVal);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        
        @Override
        protected Void doInBackground(Void... params) {

            sendData("xx"); // Clear the array first
            // Wait for the data to propigate down the chain
        	t = SystemClock.elapsedRealtime();
        	long startTime = SystemClock.elapsedRealtime();
        	n=0;
        	for (int i = 0; i < mmCount; i++) // one count i per cycle
        	{
        		// Top
        		sendData("dt");
        		mSleep(200); //Let AEC stabalize if it's on
        		
        		cameraReady = false;
        		captureImage(path + String.format("top_%d_",i+1)+ String.format("%3d", SystemClock.elapsedRealtime()-startTime));
        		while(!cameraReady)
        		{
        			mSleep(1);
        		}
        		n++;
                publishProgress();
                
        		// Bottom
        		sendData("db");
        		cameraReady = false;
        		captureImage(path + String.format("bottom_%d_",i+1)+ String.format("%3d", SystemClock.elapsedRealtime()-startTime));
        		while(!cameraReady)
        		{
        			mSleep(1);
        		}
        		n++;
        		publishProgress();

        		// Left Side
        	    sendData("dl");
        	    cameraReady = false;
        	    captureImage(path + String.format("left_%d_",i+1)+ String.format("%3d", SystemClock.elapsedRealtime()-startTime));
        		while(!cameraReady)
        		{
        			mSleep(1);
        		}
        		n++;
        		publishProgress();
        		
        		// Right Side
        	    sendData("dr");
        	    cameraReady = false;
        	    captureImage(path + String.format("right_%d_",i+1)+ String.format("%3d", SystemClock.elapsedRealtime()-startTime));
        		while(!cameraReady)
        		{
        			mSleep(1);
        		}
        		n++;
        		publishProgress();
        		
        		/*
        		// Brightfield
        	    sendData("bf");
        	    camParams.setExposureCompensation(camParams.getMinExposureCompensation());
        		mCamera.setParameters(camParams);
        		
        	    cameraReady = false;
        	    captureImage(path+String.format("%3d", SystemClock.elapsedRealtime()-startTime) + "_btfd_" + String.format("%d",i+1));
        		while(!cameraReady)
        		{
        			mSleep(1);
        		}
        		n++;
        		publishProgress();
        		*/

        		// Darkfield
        		//camParams.setAutoExposureLock(false);
        		if (usingHDR)
        		{
        			camParams.setSceneMode("hdr");
    			    mCamera.setParameters(camParams);
        		}
        	
        	    if (darkfieldAnnulus)
        	    	sendData("an");
        	    else
        	    	sendData("df");
        	    
        	    camParams = mCamera.getParameters();
    			camParams.setExposureCompensation(aecCompensation);
			    mCamera.setParameters(camParams);
        	    
        	    cameraReady = false;
        	    mSleep(500);
        	    captureImage(path + String.format("darkfield_%d_",i+1)+ String.format("%3d", SystemClock.elapsedRealtime()-startTime));
        		while(!cameraReady)
        		{
        			mSleep(10);
        		}
        		
        		n++;
        		publishProgress();
        		
        	    camParams = mCamera.getParameters();
    			camParams.setExposureCompensation(aecCompensation);
			    mCamera.setParameters(camParams);
        		
        		// Undo HDR and make sure AEC is locked
        		if(usingHDR)
        		{
	        		camParams.setSceneMode("auto");     
	        		camParams.setAutoExposureLock(true);
	        		mCamera.setParameters(camParams);
        		}
        		
        		// User-defined delay between captures for a time-series
        		if (mmDelay != 0)
        		{	        		camParams.setSceneMode("auto");     
        		camParams.setAutoExposureLock(true);
        		mCamera.setParameters(camParams);
        			sendData("x"); // Clear the Array
        			Log.d(TAG,String.format("Sleeping for %d ms", (int)(Math.round(mmDelay*1000f))));
        			mSleep((int)(Math.round(mmDelay*1000f)));
        		}
        	}
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up

            //String cmd = String.format("p%d", centerLED);
            String cmd = "df";
            sendData(cmd);
            timeLeftTextView.setText(" ");
            
    		Camera.Parameters params = mCamera.getParameters();
    		params.setAutoExposureLock(false);
    		params.setExposureCompensation(aecCompensation);
    		mCamera.setParameters(params);
    		updateFileStructure(myDir.getAbsolutePath());
            mDataset.DATASET_PATH = Environment.getExternalStorageDirectory()+path;
            mDataset.DATASET_TYPE = acquireType;
    		
        }
    }
   
    private class runScanningMode extends AsyncTask<Void, Void, Void>
    {
        //ProgressDialog pdLoading = new ProgressDialog(AsyncExample.this);
        int centerCount = 0;
        long t = 0;
        int n = 0;
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS",Locale.US).format(new Date());
        String path = "/CellScope/" + acquireType + "_" + datasetName + "_" + timestamp;
        File myDir = new File(Environment.getExternalStorageDirectory()+ path);
        
        void mSleep(int sleepVal)
        {
    		try {
				Thread.sleep(sleepVal);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        
        @Override
        protected void onPreExecute() 
        {
            super.onPreExecute();
            
            acquireTextView.setText(String.format("Acquiring - MODE: %s", acquireType));
            acquireProgressBar.setVisibility(View.VISIBLE); // Make invisible at first, then have it pop up
            
     	    // Count how many LEDs there are for progress bar - this only happens once and should be fairly fast, but isn't optimal.
            for (int index=0; index<ledCount; index++)
        	{
            	if (Math.sqrt(Math.sin((double)(domeCoordinates[index][2])/1000.0)*Math.sin((double)(domeCoordinates[index][2])/1000.0)+ Math.sin((double)(domeCoordinates[index][3])/1000.0)*Math.sin((double)(domeCoordinates[index][3])/1000.0)) < objectiveNA)
        			centerCount++;
        	}
        	acquireProgressBar.setMax(centerCount);
        	myDir.mkdirs();
        	
            Camera.Parameters camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(false);
            camParams.setExposureCompensation(aecCompensation);
            mCamera.setParameters(camParams);
        	
        	int edgeLED = centerLED; // For Now
        	String cmd;
			if (acquireType.contains("Full"))
			{
        		 cmd = String.format("dh,%d", edgeLED);
			}
			else
			{
				cmd = String.format("dh,%d", centerLED);
			}
        	sendData(cmd);
    		try { 
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            
            // Lock exposure - here we assume that the center LED is turned on when AcquireActivity class instance is created
            camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(true);
            camParams.setExposureCompensation(0);
            mCamera.setParameters(camParams);
            mDataset.DATASET_PATH = Environment.getExternalStorageDirectory()+path;
            mDataset.DATASET_TYPE = acquireType;
        }
        
        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(n);
    		long elapsed = SystemClock.elapsedRealtime() - t;
    		t = SystemClock.elapsedRealtime();
    		float timeLeft = (float)(((long)(centerCount - n)*elapsed)/1000.0);
    		timeLeftTextView.setText(String.format("Time left: %.2f seconds, %d/%d images saved", timeLeft,n,centerCount));
    		//Log.d(TAG,String.format("Time left: %.2f seconds", timeLeft));
        }
        
        @Override
        protected Void doInBackground(Void... params) {
        	t = SystemClock.elapsedRealtime();
        	for (int index=1; index<=ledCount; index++)
        	{
        			if (Math.sqrt(Math.sin((double)(domeCoordinates[index-1][2])/1000.0)*Math.sin((double)(domeCoordinates[index-1][2])/1000.0)+ Math.sin((double)(domeCoordinates[index-1][3])/1000.0)*Math.sin((double)(domeCoordinates[index-1][3])/1000.0)) < objectiveNA)
        			{
        			   n++;
            		   String cmd = String.format("dh,%d",index);
            		   sendData(cmd);
            		   mSleep(100);
            		   cameraReady = false;
               		   
            		   captureImage(path + "/"+timestamp + "_scanning_" + String.format("%d",index));
            		   publishProgress();
            		   while (!cameraReady)
            		   {
	            		   try {
							  Thread.sleep(10);
						   } catch (InterruptedException e) {
							e.printStackTrace();
						   }
            		   }
        		}
        	}

            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up
            // Turn on the center LED
            //String cmd = String.format("p%d", centerLED);
            String cmd = "bf";
            sendData(cmd);
            timeLeftTextView.setText(" ");
            
            // Unlock Exposure
            Camera.Parameters camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(false);
            mCamera.setParameters(camParams);
            updateFileStructure(myDir.getAbsolutePath());
        }
    }
  
    public void captureImage(String fileHeader)
    {
    	fileName = fileHeader;
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }
    
    public void sendData(String message) {
    	message = message + "\n";
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
        	if (D) Log.d(TAG, message);
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
        
        
         /*
		   try {
			  Thread.sleep(2000);
		   } catch (InterruptedException e) {
			e.printStackTrace();
		   }
		   */
		   
    }

  //fire intent to start activity with proper configuration for acquire type
  protected void startGalleryActivity() {
	  Intent intent = new Intent();
	  intent.setAction(Intent.ACTION_VIEW);
	  intent.setDataAndType(Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/CellScope/20140815_163448496/"), "image/*");
	  startActivity(intent);
  }
  
  public void updateFileStructure(String currPath) { 
	  File f = new File(currPath);
	  File[] fileList = f.listFiles();
	  ArrayList<String> arrayFiles = new ArrayList<String>();
	     if (!(fileList.length == 0))
	     {
	            for (int i=0; i<fileList.length; i++) 
	                arrayFiles.add(currPath+"/"+fileList[i].getName());
	     }
	     
     String[] fileListString = new String[arrayFiles.size()];
     fileListString = arrayFiles.toArray(fileListString);
     MediaScannerConnection.scanFile(AcquireActivity.this,
              fileListString, null,
              new MediaScannerConnection.OnScanCompletedListener() {
                  public void onScanCompleted(String path, Uri uri) {
                      //Log.i("TAG", "Finished scanning " + path);
                  }
              });
	}
  
  public void openSettingsDialog()
  {
	    settingsDialogFragment.show(getFragmentManager(), "acquireSettings");

  }
  public void setMultiModeCount(int count) 
  {
	  mmCount = count;
  }
  
  public void setNA(float na) 
  {
	  brightfieldNA = na;
      sendData(String.format("na,%d", (int) Math.round(na*100)));
  }
  
  public void setMultiModeDelay(float delay) 
  {
      mmDelay = delay;

  }
  
  public void setDatasetName(String name)
  {
	  datasetName = name;
  }
  
  public void toggleBrightfield()
  {
	  sendData("bf");
      Camera.Parameters camParams = mCamera.getParameters();
      camParams.setAutoExposureLock(false);
      mCamera.setParameters(camParams);
  }
  
  public void toggleDarkfield()
  {
	  if(darkfieldAnnulus)
		  sendData("an");
	  else
		  sendData("df");
	  
      Camera.Parameters camParams = mCamera.getParameters();
      camParams.setAutoExposureLock(false);
      mCamera.setParameters(camParams);
  }
  
  public void setAECCompensation( int aecVal)
  {
	  aecCompensation = aecVal;
      Camera.Parameters camParams = mCamera.getParameters();
      camParams.setAutoExposureLock(false);
      camParams.setExposureCompensation(aecCompensation);
      mCamera.setParameters(camParams);
  }
  
  
  public void toggleAlignment()
  {
	  // TODO - add alignment routine
	  sendData("xx");
  }
  public void setHDR(boolean state)
  {
	  if (state)
		  usingHDR = true;
	  else
		  usingHDR = false;
	  
  }
  
@SuppressLint("ClickableViewAccessibility")
@Override
public boolean onTouch(View v, MotionEvent event) {
	return false;
}

@Override
public void onDialogPositiveClick(DialogFragment dialog) {
	
}

@Override
public void onDialogNegativeClick(DialogFragment dialog) {
	
}
}