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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import com.wallerlab.compcellscope.dialogs.DirectoryChooserDialog;


public class Folder_Chooser extends Activity {

    Button getPictures, selectDirectory;
    EditText location;
    final static String PREFS_NAME = "settings";
    final static String location_name = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder__chooser);
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        //File Stuff
        File default_loc = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator
                + "Research_Pics/");

        location = (EditText) findViewById(R.id.mod_Location);
        selectDirectory = (Button) findViewById(R.id.pick_directory);
        getPictures = (Button) findViewById(R.id.get_pictures);

        location.setText(settings.getString(location_name, default_loc.toString()));
        
        selectDirectory.setOnClickListener(new View.OnClickListener() {
        	private String m_chosenDir = "";
            private boolean m_newFolderEnabled = true;
            
			@Override
			public void onClick(View v) {

                // Create DirectoryChooserDialog and register a callback 
                DirectoryChooserDialog directoryChooserDialog = 
                new DirectoryChooserDialog(Folder_Chooser.this, 
                    new DirectoryChooserDialog.ChosenDirectoryListener() 
                {
                    @Override
                    public void onChosenDir(String chosenDir) 
                    {
                        m_chosenDir = chosenDir;
                        File file = new File(m_chosenDir);
                        location.setText(file.toString());
                        if(file.exists()) {
                            SharedPreferences.Editor edit = settings.edit();
                            edit.putString(location_name, location.getText().toString());
                            edit.commit();
                            //startImageGalleryActivity();
                        }
                        else{
                            Toast.makeText(getPictures.getContext(), "This file does not exist", Toast.LENGTH_LONG).show();
                        }
                        
                        Toast.makeText(
                        Folder_Chooser.this, "Chosen directory: " + 
                          chosenDir, Toast.LENGTH_SHORT).show();
                    }
                }); 
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(m_newFolderEnabled);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(m_chosenDir);
                m_newFolderEnabled = ! m_newFolderEnabled;
				
			}
		});
        
        getPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file = new File(location.getText().toString());
                if(file.exists()) {
                    SharedPreferences.Editor edit = settings.edit();
                    edit.putString(location_name, location.getText().toString());
                    edit.commit();
                    startImageGalleryActivity();
                }
                else{
                    Toast.makeText(getPictures.getContext(), "This file does not exist", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.folder__chooser, menu);
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

    protected void startImageGalleryActivity(){
        Intent intent = new Intent(this, Image_Gallery.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


}