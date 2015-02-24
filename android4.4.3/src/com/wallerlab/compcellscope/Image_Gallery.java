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

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


public class Image_Gallery extends Activity {

    SharedPreferences settings;
    final String LOG = "Image_Gallery";
    private int counter;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image__gallery);
        counter = 0;
        final ImageView currPic = new ImageView(this);

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        final int screen_width = metrics.widthPixels;

        SharedPreferences settings = getSharedPreferences(Folder_Chooser.PREFS_NAME, 0);
        //SharedPreferences.Editor edit = settings.edit();
        String path = settings.getString(Folder_Chooser.location_name, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());

        //Log.d(LOG, "  |  " + path + " |  ");
        TextView text1 = (TextView)findViewById(R.id.text1);
        final TextView text2 = (TextView)findViewById(R.id.text2);

        text1.setText(path);

        File directory = new File(path);

        // get all the files from a directory
        File[] dump_files = directory.listFiles();
        Log.d(LOG, dump_files.length + " ");

        final File [] fList = removeElements(dump_files, "info.json");

        Log.d(LOG, "Filtered Length: " + fList.length);

        Arrays.sort(fList, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                String one = file.toString();
                String two = file2.toString();
                //Log.d(LOG, "one: " + one);
                //Log.d(LOG, "two: " + two);
                int num_one = Integer.parseInt(one.substring(one.lastIndexOf("(") + 1, one.lastIndexOf(")")));
                int num_two = Integer.parseInt(two.substring(two.lastIndexOf("(") + 1, two.lastIndexOf(")")));
                return num_one - num_two;
            }
        });
        
        try {
            writeJsonFile(fList);
        } catch (JSONException e){
            Log.d(LOG, "JSON WRITE FAILED");
        }
        //List names programattically
        LinearLayout myLinearLayout = (LinearLayout) findViewById(R.id.linear_table);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                screen_width + 4);

        myLinearLayout.setOrientation(LinearLayout.VERTICAL);


        if(fList != null) {
            File cur_pic = fList[0];

            BitmapFactory.Options opts = new BitmapFactory.Options();
            Log.d(LOG, "\n File Location: " + cur_pic.getAbsolutePath() + " \n"
             + " Parent: " + cur_pic.getParent().toString() + "\n");

            Bitmap myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);

            currPic.setLayoutParams(params);
            currPic.setImageBitmap(myImage);
            currPic.setId(View.generateViewId());
            text2.setText(cur_pic.getName());
        }

        myLinearLayout.addView(currPic);


        //Seekbar
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setEnabled(true);
        seekBar.setMax(fList.length - 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            int length = fList.length;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setCounter(i);
                if(getCounter() <= fList.length){
                    File cur_pic = fList[getCounter()];
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    Bitmap myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);
                    int image_width = opts.outWidth;
                    int image_height = opts.outHeight;
                    int sampleSize = image_width/screen_width;
                    opts.inSampleSize = sampleSize;

                    text2.setText(cur_pic.getName());

                    opts.inJustDecodeBounds = false;
                    myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);

                    currPic.setImageBitmap(myImage);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        //Make Button Layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            
        LinearLayout.LayoutParams LLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonLayout.setLayoutParams(LLParams);
        buttonLayout.setId(View.generateViewId());


        //Button Layout Params
        LinearLayout.LayoutParams param_button = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);


        //Prev Pic
        Button prevPic = new Button(this);
        prevPic.setText("Previous");
        prevPic.setId(View.generateViewId());
        prevPic.setLayoutParams(param_button);

        prevPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fList != null){
                    decrementCounter();
                    if(getCounter() >= 0){
                        File cur_pic = fList[getCounter()];
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        Bitmap myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);
                        int image_width = opts.outWidth;
                        int image_height = opts.outHeight;
                        int sampleSize = image_width/screen_width;
                        opts.inSampleSize = sampleSize;

                        text2.setText(cur_pic.getName());

                        opts.inJustDecodeBounds = false;
                        myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);
                        //Log.d(LOG, getCounter() + "");
                        seekBar.setProgress(getCounter());
                        currPic.setImageBitmap(myImage);

                    }else{
                        setCounter(0);
                    }

                } else{
                    Toast.makeText(Image_Gallery.this, "There are no pictures in this folder", Toast.LENGTH_SHORT);
                    setCounter(getCounter() - 1);
                }
            }});

        buttonLayout.addView(prevPic);

        // Next Picture  Button
        Button nextPic = new Button(this);
        nextPic.setText("Next");
        nextPic.setId(View.generateViewId());
        nextPic.setLayoutParams(param_button);

        buttonLayout.addView(nextPic);
        nextPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fList != null){
                    incrementCounter();
                    if(getCounter() < fList.length){
                        File cur_pic = fList[getCounter()];
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        Bitmap myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);
                        int image_width = opts.outWidth;
                        int image_height = opts.outHeight;
                        int sampleSize = image_width/screen_width;
                        opts.inSampleSize = sampleSize;

                        text2.setText(cur_pic.getName());

                        opts.inJustDecodeBounds = false;
                        myImage = BitmapFactory.decodeFile(cur_pic.getAbsolutePath(), opts);
                        //Log.d(LOG, getCounter() + "");
                        seekBar.setProgress(getCounter());

                        currPic.setImageBitmap(myImage);
                    }else{
                        setCounter(getCounter() - 1);
                    }

                } else{
                    Toast.makeText(Image_Gallery.this, "There are no pictures in this folder", Toast.LENGTH_SHORT);
                    setCounter(getCounter() - 1);
                }
            }});

        myLinearLayout.addView(buttonLayout);
    }



    private void incrementCounter(){
        counter++;
    }

    private void setCounter(int i){
        counter = i;
    }

    private int getCounter(){
        return counter;
    }

    private void decrementCounter(){
        counter--;
    }

    private void writeJsonFile(File[] files) throws JSONException {

        ExifInterface inter;

        //Can write meta deta to the JSON File
        JSONArray image_files = new JSONArray();
        for(int i = 0; i < files.length; i++)
        {
            File file = files[i];
            JSONObject image = new JSONObject();
            String one = file.getName().toString();

            Log.d(LOG, "Fileyyyyyy: " + file.getPath());
            //Get Information about picture hidden in tags
            try {
                inter = new ExifInterface(file.getPath());
                Log.d(LOG, "Date Taken: " + inter.getAttribute(ExifInterface.TAG_DATETIME));
                Log.d(LOG, "GPSTimeStamp Taken: " + inter.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
                Log.d(LOG, "Make Taken: " + inter.getAttribute(ExifInterface.TAG_MAKE));
            }catch (IOException e){
                e.printStackTrace();
            }



            Integer.parseInt(one.substring(one.lastIndexOf("(") + 1, one.lastIndexOf(")")));
            Integer.parseInt(one.substring(one.lastIndexOf("(") + 1, one.lastIndexOf(")")));
            image.put("name", one);
            image.put("focus", Integer.parseInt(one.substring(one.lastIndexOf("(") + 1, one.lastIndexOf(")"))));

            image_files.put(image);
        }

        try{
            FileWriter file = new FileWriter(files[0].getParent().toString() + File.separator + "info.json");
            file.write("test");
            file.flush();
            file.close();
        } catch(IOException e) {
            e.printStackTrace();
        }


    }

    public static File[] removeElements(File[] input, String deleteMe) {
        ArrayList<File> result = new ArrayList<File>();
        Log.d("Image_Gallery", "Got here: " + input.length);
        for(File item : input)
            if(!item.toString().contains(deleteMe))
                result.add(item);

        Log.d("Image_Gallery", "Got here: " + result.size());


        return result.toArray(new File[result.size()]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image__gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}