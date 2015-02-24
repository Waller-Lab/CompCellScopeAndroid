/*
 *    Computational CellScope Project
 *     CellScope LED Dome reciever
 *       Version 2.02, 2/23/15
 *  Zack Phillips, zkphil@berkeley.edu
 *
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

#include <TLC5926.h>
#include "dpcmaps_bit.h"
#include "arrayCoordinates.h"
#define LED_COUNT 508
#define TRIG_PIN 7
#define CMD_LENGTH 2
#define OPT_MAX_LENGTH 5

// Arrange for /OE to be on a PWM
const int SDI_pin = 12;
const int CLK_pin = 11;
const int LE_pin = 10;
const int OE_pin = -1; // should be a pwm pin
const int CHAIN_CT = 36;

boolean debug = 1;
float objectiveNA = 0.25;
int trigDelay = 300;

// Single global variable used to hold data to shift out
unsigned int patternArray[CHAIN_CT];
TLC5926 tlc;

void setup()
{
  Serial.begin(115200);
  Serial1.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  pinMode(9,OUTPUT);
  pinMode(10,OUTPUT);
  pinMode(11,OUTPUT);
  pinMode(12,OUTPUT);
  digitalWrite(9,HIGH);
  pinMode(TRIG_PIN,OUTPUT);

  // Set up the tlc
  tlc.debug(1)
    ->attach(CHAIN_CT, SDI_pin, CLK_pin, LE_pin, OE_pin )
      ->off()->reset();
  //tlc.all(HIGH);
  clearArray();
}

void drawSinglePixel(int holeNum, int on_off)
{
  tlc.on();
  holeNum = holeNum-1; // Zero indexing array

  int hole = (int)pgm_read_word(&(ledMap[holeNum][0]));
  int channel = (int)pgm_read_word(&(ledMap[holeNum][1]));
  int phi = (int)pgm_read_word(&(ledMap[holeNum][2]));
  int theta = (int)pgm_read_word(&(ledMap[holeNum][3]));
  int chip = (unsigned int)floor(channel/16);
  if (!on_off)
    memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  if (channel != -1)
  {
    patternArray[chip] |= 1 << (unsigned int)(channel-16*chip); //Turn that LED on
    sendData();
    Serial1.println(F("Drawing LED Pixel (Hole) #"));
    Serial1.println(hole);
  }
  else
  {
    Serial1.println(F("ERROR - no LED present according to arrayCoordinates.h"));
    sendData();
  }
}

void drawChannel(int channel)
{
  tlc.on();
  int chip = (unsigned int)floor(channel/16);
  memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  patternArray[chip] |= 1 << (unsigned int)(channel-16*chip); //Turn that LED on
  sendData();
  Serial1.println(F("Drawing LED Channel # "));
  Serial1.println(channel);
}

void sendTriggerPulse()
{
  digitalWrite(TRIG_PIN,LOW);
  delay(10);
  digitalWrite(TRIG_PIN,HIGH);
  delay(trigDelay);
}

void setTriggerDelay(int newDelay)
{
  trigDelay = newDelay;
}

void loadPattern(int type)
{
  /*
   * type 0 is top
   * type 1 is bottom
   * type 2 is left
   * type 3 is right
   * type 4 is brightfield
   * type 5 is darkfield
   * type 6 is annulus
   * type 7 is alignment 
   (generated in MATLAB)
   */
  memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  for (int chip=0; chip<CHAIN_CT; chip++)
  {
    switch ((int)(objectiveNA*100))
    {
    case 10:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles10[chip][type]));
        break;
      }
    case 15:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles15[chip][type]));
        break;
      }
    case 20:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles20[chip][type]));
        //Serial1.println(patternArray[chip]);
        break;
      }
    case 25:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles25[chip][type]));
        break;
      }
    case 30:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles30[chip][type]));
        //   Serial1.println(patternArray[chip]);
        break;
      }
    case 35:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles35[chip][type]));
        break;
      }
    case 40:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles40[chip][type]));
        break;
      }
    case 45:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles45[chip][type]));
        break;
      }
    case 50:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles50[chip][type]));
        break;
      }
    case 55:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles55[chip][type]));
        break;
      }
    case 60:
      {
        patternArray[chip] = (unsigned int)pgm_read_word(&(naHoles60[chip][type]));
        break;
      }
    default:
      Serial1.println(F("ERROR - NA not supported!"));
    }
  }
}

// TODO: does not properly calculate darkfield and brightfield, need to test when full dome is populated.
void drawCircle(int type)
{
  loadPattern(type);
  Serial1.println(F("Drew Pattern"));
  sendData();
}

void scanAlignment()
{
  loadPattern(7); // Load brightfield pattern
  unsigned int patternArray2[CHAIN_CT];
  memcpy( patternArray2, patternArray, CHAIN_CT*16 );
  memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  while(1)
  {
  for (int ch=0; ch<(CHAIN_CT*16); ch++)
  {
    int chip = (unsigned int)floor(ch/16);
    if(patternArray2[chip] & (1 << (unsigned int)(ch-16*chip))) //Turn that LED on
    {
    drawChannel(ch);
    delay(500);
    }
  }
    if(Serial1.available() > 0)
      {
        if (Serial1.read() == 'x')
        {
          clearArray();
          break;
        }
      }
      if(Serial.available() > 0)
      {
        if (Serial.read() == 'x')
        {
          clearArray();
          break;
        }
      }
  }
}

void drawWholeChip(int chipNum)
{
  memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  patternArray[chipNum-1] = (unsigned int)pow(2,16);
  Serial.println(patternArray[chipNum-1]);
  sendData();
}

void sendData()
{
  digitalWrite(9,LOW);
  for (int i=0; i<CHAIN_CT; i++) {
    tlc.send(patternArray[i]); // no flicker here
  }
  digitalWrite(9,HIGH);
  delay(10);
  tlc.latch_pulse();
}

void clearArray()
{
  memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
  sendData();
}

void printSystemInformation(int serialChan)
{
  if (serialChan == 1)
  {
    Serial1.println(F("BEGIN_INFO"));
    Serial1.println(F("NAME:CELLSCOPEDOME"));
    Serial1.println(F("TYPE:DOME"));
    Serial1.println(F("LEDCOUNT:508"));
    Serial1.println(F("COLOR:NO"));
  }
  else
  {
    Serial.println(F("BEGIN_INFO"));
    Serial.println(F("NAME:CELLSCOPEDOME"));
    Serial.println(F("TYPE:DOME"));
    Serial.println(F("LEDCOUNT:508"));
    Serial.println(F("COLOR:NO"));
  }
}

void printHelp( int ch)
{
  Serial1.println(F("USAGE:"));
  Serial1.println(F(" px,XXX   : Turns on LED XXX\n"));
  Serial1.println(F(" px,249   : Turns on LED 249 (center)\n"));
  Serial1.println(F(" na,XX    : sets the NA to 0.XX\n"));
  Serial1.println(F(" na,25    : sets the NA to 0.25\n"));
  Serial1.println(F(" xx       : Clears array\n"));
  Serial1.println(F(" ff       : Fills array\n"));
  Serial1.println(F(" bf       : Turns on Brightfield\n"));
  Serial1.println(F(" df       : Turns on Darkfield\n"));
  Serial1.println(F(" an       : Turns on Annulus\n"));
  Serial1.println(F(" fs       : Scan all LEDs\n"));
  Serial1.println(F(" sb       : Scans Brightfield LEDs\n"));
}

void parseCommands(int serialChan, char* cmdHeader, char* optionalParam)
{
  if ((strcmp(cmdHeader, "x") == 0) || (strcmp(cmdHeader, "xx") == 0)) // clear the array
  {
    clearArray();
    if (debug)
      if (serialChan == 1)
        Serial1.println(F("Cleared Array"));
      else
        Serial.println(F("Cleared Array"));

  }
  else if (strcmp(cmdHeader, "dh") == 0) // draw a single pixel
  {
    int pxNum = atoi(optionalParam);
    if (pxNum <=0)
      drawSinglePixel(249,0);
    else
      drawSinglePixel(pxNum,0);
    if (debug)
    {
      if (serialChan)
      {
        Serial1.println(F("Drew Pixel # "));
        Serial1.println(pxNum);
      }
      else
      {
        Serial.println(F("Drew Pixel # "));
        Serial.println(pxNum);
      }
    }
  }
  else if (strcmp(cmdHeader, "dc") == 0) // draw a single pixel
  {
    int ch = atoi(optionalParam);
    drawChannel(ch);
    if (debug)
    {
      if (serialChan)
      {
        Serial1.println(F("Drew Ch # "));
        Serial1.println(ch);
      }
      else
      {
        Serial.println(F("Drew Ch # "));
        Serial.println(ch);
      }
    }
  }
  else if (strcmp(cmdHeader, "do") == 0) // draw a single pixel
  {
    int pxNum = atoi(optionalParam);
    drawSinglePixel(pxNum,1);
    if (debug)
    {
      if (serialChan)
      {
        Serial1.println(F("Drew Pixel (No Clearing) # "));
        Serial1.println(pxNum);
      }
      else
      {
        Serial.println(F("Drew Pixel (No Clearing) # "));
        Serial.println(pxNum);
      }
    }
  }
  else if (strcmp(cmdHeader, "bg") == 0) // toggle debugging output
  {
    if (serialChan == 1)
    {
      Serial1.println(F("Toggled debug output. state:"));
      Serial1.println(debug);
    }
    else{
      Serial.println(F("Toggled debug output. state:"));
      Serial.println(debug);
    }
    debug = !debug;
  }
  else if (strcmp(cmdHeader, "ff") == 0) // Fill the Array
  {
    if (debug)
      if(serialChan)
        Serial1.println(F("Filled Array (WARNING - do not leave on for more than 5 seconds!)"));
      else
        Serial.println(F("Filled Array (WARNING - do not leave on for more than 5 seconds!)"));
    tlc.all(HIGH);
  }
  else if (strcmp(cmdHeader, "ca") == 0)
  {
    for (int hole=0; hole <152; hole++)
    {
      int myHole = (int)pgm_read_word(&(calLogoHoles[hole]));
      myHole = myHole-1; // Zero indexing array
      int channel = (int)pgm_read_word(&(ledMap[myHole][1]));
      int chip = (unsigned int)floor(channel/16);
      patternArray[chip] |= 1 << (unsigned int)(channel-16*chip); //Turn that LED on
    }
    sendData();
    if (serialChan == 1)
      Serial1.println(F("Finished drawing cal logo"));
    else
      Serial.println(F("Finished drawing cal logo"));
  }
  // Send useful information to the host
  else if (strcmp(cmdHeader, "hp") == 0)
  {
    printSystemInformation(serialChan);
  }
  else if (strcmp(cmdHeader, "na") == 0)
  {
    int newNA = atoi(optionalParam);
    objectiveNA = (float)newNA/100.0;
    if (serialChan == 1)
    {
      Serial1.println(F("New NA set to: "));
      Serial1.println(objectiveNA);
    }
    else
    {
      Serial.println(F("New NA set to: "));
      Serial.println(objectiveNA);
    }
  }
  else if (strcmp(cmdHeader, "dt") == 0)
    drawCircle(0);
  else if (strcmp(cmdHeader, "db") == 0)
    drawCircle(1);
  else if (strcmp(cmdHeader, "dl") == 0)
    drawCircle(2);
  else if (strcmp(cmdHeader, "dr") == 0)
    drawCircle(3);
  else if (strcmp(cmdHeader, "bf") == 0)
    drawCircle(4);
  else if (strcmp(cmdHeader, "df") == 0)
    drawCircle(5);
  else if (strcmp(cmdHeader, "an") == 0)
    drawCircle(6);
  else if (strcmp(cmdHeader, "al") == 0)
    drawCircle(7);
  else if (strcmp(cmdHeader, "as") == 0)
    scanAlignment();
  else if (strcmp(cmdHeader, "sd") == 0)
    setTriggerDelay(atoi(optionalParam));
    
  // Scan the center line of LEDs
  else if (strcmp(cmdHeader, "sl") == 0)
  {
   
   int startHole = 238;
   int endHole = 261;
    for (int hole=startHole; hole <=endHole; hole++)
    {
      drawSinglePixel(hole,0);
      sendTriggerPulse();
      delay(200);
    }
    if (serialChan == 1)
      Serial1.println(F("Finished drawing cal logo"));
    else
      Serial.println(F("Finished drawing cal logo"));
  }
  // Scan the Brightfield LEDs - scan will be out of order, order is defined in Separate .mat file
  else if (strcmp(cmdHeader, "sb") == 0)
  {
    loadPattern(4); // Load brightfield pattern
    unsigned int patternArray2[CHAIN_CT];
    memcpy( patternArray2, patternArray, CHAIN_CT*16 );
    memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
    for (int ch=0; ch<(CHAIN_CT*16); ch++)
    {
      int chip = (unsigned int)floor(ch/16);
      if(patternArray2[chip] & (1 << (unsigned int)(ch-16*chip))) //Turn that LED on
      {
      tlc.on();
      memset(patternArray,0,sizeof(patternArray)); // Set all values to zero
      patternArray[chip] |= 1 << (unsigned int)(ch-16*chip); //Turn that LED on
      sendData();
      sendTriggerPulse();
      if(Serial1.available() > 0)
      {
        if (Serial1.read() == 'x')
        {
          clearArray();
          break;
        }
      }
      if(Serial.available() > 0)
      {
        if (Serial.read() == 'x')
        {
          clearArray();
          break;
        }
      }
      }
    }
  }
  // Full LED Scan
  else if (strcmp(cmdHeader, "sf") == 0)
  {
    if (debug)
      Serial1.println(F("Beginning LED Scan. Send x to quit"));
    digitalWrite(TRIG_PIN,HIGH);
    delay(200);
    for (int idx=1; idx<=LED_COUNT; idx++)
    {
      if(Serial1.available() > 0)
        if (Serial1.read() == 'x')
        {
          clearArray();
          break;
        }
      if(Serial.available() > 0)
        if (Serial.read() == 'x')
        {
          clearArray();
          break;
        }
      drawSinglePixel(idx,0);
      sendTriggerPulse();
    }
  }
}

void loop() {
  // Loop until we recieve a command, then parse it.

  // Bluetooth Serial
  if (Serial1.available())
  {
    while(Serial1.available() > 0)
    {
      const byte inByte = Serial1.read();
      static char cmd [CMD_LENGTH+1];
      static char opt [OPT_MAX_LENGTH+1];
      static unsigned int input_pos = 0;
      static unsigned int opt_input_pos = 0;

      switch (inByte)
      {
      case '\n':   // end of text
        cmd [input_pos] = 0;     // terminating null byte
        opt [opt_input_pos] = 0; // terminating null byte
        parseCommands(0, cmd, opt);
        Serial1.println(cmd);
        Serial1.println(opt);

        // reset buffer for next time
        input_pos = 0;
        opt_input_pos = 0;
        break;

      case '\r':   // discard carriage return
        break;

      case ',':   // discard comma
        break;
        
      case '?':
        printHelp(1);
        break;

      default:
        // keep adding if not full ... allow for terminating null byte
        if (input_pos < (CMD_LENGTH))
        {
          cmd [input_pos++] = inByte;
          Serial1.println(cmd);
        }
        else
          opt [opt_input_pos++] = inByte;
        break;

      }  // end of switch
    }
  }
  else if (Serial.available())
  {
    while(Serial.available() > 0)
    {
      const byte inByte = Serial.read();
      static char cmd [CMD_LENGTH+1];
      static char opt [OPT_MAX_LENGTH+1];
      static unsigned int input_pos = 0;
      static unsigned int opt_input_pos = 0;

      switch (inByte)
      {
      case '\n':   // end of text
        cmd [input_pos] = 0;     // terminating null byte
        opt [opt_input_pos] = 0; // terminating null byte
        parseCommands(0, cmd, opt);
        Serial.println(cmd);
        Serial.println(opt);
        // reset buffer for next time
        input_pos = 0;
        opt_input_pos = 0;
        break;

      case '\r':   // discard carriage return
        break;

      case ',':   // discard comma
        break;
        
      default:
        // keep adding if not full ... allow for terminating null byte
        if (input_pos < (CMD_LENGTH))
        {
          cmd [input_pos++] = inByte;
        }
        else
          opt [opt_input_pos++] = inByte;
        break;

      }  // end of switch
    }
  }

}


