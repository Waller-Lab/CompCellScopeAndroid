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

package com.wallerlab.compcellscope.dialogs;

import com.wallerlab.compcellscope.AcquireActivity;
import com.wallerlab.compcellscope.R;
import com.wallerlab.compcellscope.R.id;
import com.wallerlab.compcellscope.R.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class AcquireSettings extends DialogFragment{
	public static String TAG = "Settings Dialog";
	  
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private Button acqSettingsDarkfieldButton;
    private Button acqSettingsBrightfieldButton;
    private Button acqSettingsAlignmentButton;
    private TextView acquireSettingsSetDatasetName;
    private TextView acquireSettingsMultiModeCountTextView;
    private TextView acquireSettingsSetNAEditText;
    private TextView acquireSettingsSetMultiModeDelayEditText;
    private TextView acquireSettingsAECCompensationEditText;
    private CheckBox acquireSettingsHDRCheckbox;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
    
    public static interface OnCompleteListener {
        public abstract void onComplete(String time);
    }
    
    
	  @Override
	  public Dialog onCreateDialog(Bundle savedInstanceState) {
	      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	      // Get the layout inflater
	      LayoutInflater inflater = getActivity().getLayoutInflater();
	      View content = inflater.inflate(R.layout.acquire_settings_layout, null);
	      

  
	      // Inflate and set the layout for the dialog
	      // Pass null as the parent view because its going in the dialog layout
	      builder.setView(content);
	      // Add action buttons
          builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int id) {
     	        String mmCountValue = acquireSettingsMultiModeCountTextView.getText().toString();
    	        String naValue = acquireSettingsSetNAEditText.getText().toString();
    	        String mmDelayValue = acquireSettingsSetMultiModeDelayEditText.getText().toString();
    	        String aecCompensationVal = acquireSettingsAECCompensationEditText.getText().toString();
    	        String datasetName = acquireSettingsSetDatasetName.getText().toString();
    	        Log.d(TAG,String.format("mmCount: %s", mmCountValue));
    	        Log.d(TAG,String.format("mmDelay: %s", mmDelayValue));
    	        Log.d(TAG,String.format("new na: %s", naValue));
    	        AcquireActivity callingActivity = (AcquireActivity) getActivity();
    	        callingActivity.setMultiModeCount(Integer.parseInt(mmCountValue));
    	        callingActivity.setMultiModeDelay(Float.parseFloat(mmDelayValue));
    	        callingActivity.setNA(Float.parseFloat(naValue));
    	        callingActivity.setDatasetName(datasetName);
    	        callingActivity.setAECCompensation(Integer.parseInt(aecCompensationVal));
    	        callingActivity.setHDR(acquireSettingsHDRCheckbox.isChecked());
             }
         })
         .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
            	 dialog.dismiss();
             }
         });  
          
          AcquireActivity callingActivity = (AcquireActivity) getActivity();
          
          acquireSettingsMultiModeCountTextView = (TextView) content.findViewById(R.id.acquireSettingsMultiModeCountTextView);
          acquireSettingsMultiModeCountTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
          acquireSettingsMultiModeCountTextView.setText(String.format("%d", callingActivity.mmCount));
          
          acquireSettingsSetNAEditText = (TextView) content.findViewById(R.id.acquireSettingsSetNAEditText);
          acquireSettingsSetNAEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
          acquireSettingsSetNAEditText.setText(String.format("%.2f", callingActivity.brightfieldNA));
          
          acquireSettingsAECCompensationEditText = (TextView) content.findViewById(R.id.acquireSettingsAECCompensationEditText);
          acquireSettingsAECCompensationEditText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
          acquireSettingsAECCompensationEditText.setText(String.format("%d", callingActivity.aecCompensation));
          
          acquireSettingsSetMultiModeDelayEditText = (TextView) content.findViewById(R.id.acquireSettingsSetMultiModeDelayEditText);
          acquireSettingsSetMultiModeDelayEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
          acquireSettingsSetMultiModeDelayEditText.setText(String.format("%.2f", callingActivity.mmDelay));
          
          acquireSettingsSetDatasetName = (TextView) content.findViewById(R.id.acquireSettingsSetDatasetName);
          acquireSettingsSetDatasetName.setInputType(InputType.TYPE_CLASS_TEXT);
          acquireSettingsSetDatasetName.setText(callingActivity.datasetName);
          
          acquireSettingsHDRCheckbox = (CheckBox) content.findViewById(R.id.acquireSettingsHDRCheckbox);
    	  acquireSettingsHDRCheckbox.setChecked(callingActivity.usingHDR);
          
          acqSettingsBrightfieldButton = (Button) content.findViewById(R.id.acqSettingsBrightfieldButton);
          acqSettingsBrightfieldButton.setOnClickListener(new View.OnClickListener() {
              public void onClick(View view) {
      	        AcquireActivity callingActivity = (AcquireActivity) getActivity();
      	        callingActivity.toggleBrightfield();

              }
            });
          
          acqSettingsDarkfieldButton = (Button) content.findViewById(R.id.acqSettingsDarkfieldButton);
          acqSettingsDarkfieldButton.setOnClickListener(new View.OnClickListener() {
              public void onClick(View view) {
      	        AcquireActivity callingActivity = (AcquireActivity) getActivity();
      	        callingActivity.toggleDarkfield();
              }
            });
          acqSettingsAlignmentButton = (Button) content.findViewById(R.id.acqSettingsAlignmentButton); // CLEARS THE ARRAY FOR NOW
          acqSettingsAlignmentButton.setOnClickListener(new View.OnClickListener() {
              public void onClick(View view) {
      	        AcquireActivity callingActivity = (AcquireActivity) getActivity();
      	        callingActivity.toggleAlignment();
              }
            });

	      return builder.create();
	  }
	  
	  
	}