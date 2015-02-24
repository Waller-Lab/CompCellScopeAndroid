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
import java.io.FileFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import com.wallerlab.processing.datasets.Dataset;
import com.wallerlab.processing.tasks.ComputeRefocusTask;
import com.wallerlab.compcellscope.AcquireActivity;
import com.wallerlab.compcellscope.MultiModeViewActivity;
import com.wallerlab.compcellscope.bluetooth.BluetoothDeviceListActivity;
import com.wallerlab.compcellscope.bluetooth.BluetoothService;
import com.wallerlab.compcellscope.dialogs.DirectoryChooserDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
 
public class ComputationalCellScopeMain extends Activity{
  private static final String TAG = "cCS_main";
  private static final boolean Debug = true;
  String defaultAddress = "00:06:66:66:23:53";
  
  // Message types sent from the BluetoothChatService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;
  private static final int ACTIVITY_CHOOSE_FILE = 3;

  // Key names received from the BluetoothChatService Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String DEVICE_ADDRESS = "device_address";
  public static final String TOAST = "toast";

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;
  
  // Name of the connected device
  private String mConnectedDeviceName = null;
  private String mConnectedDeviceMACAddress = null;
  // Array adapter for the conversation thread
  private ArrayAdapter<String> mConversationArrayAdapter;
  // String buffer for outgoing messages
  private StringBuffer mOutStringBuffer;
  
  // Local Bluetooth adapter
  private BluetoothAdapter mBluetoothAdapter = null;
  // Member object for the chat services
  private BluetoothService mBluetoothService = null;
  
  Intent serverIntent = null;
  public boolean btConnection;
  
  public Dataset mDataset =  new Dataset();
  
  boolean on = false;
  
  final static String PREFS_NAME = "settings";
  final static String location_name = "location";

  Button btnConnectBluetooth, btnSettings, btnAcquireFullScan,btnAcquireBFScan, btnAcquireMultiMode, btnMultiModeViewer, btnGallery, btnComputeRefocus;
  TextView connStatusTextView, connDeviceNameTextView, connMACAddressTextView;
  Spinner ledArraySpinner;
  
  protected void startBluetooth() {
      if (!mBluetoothAdapter.isEnabled()) {
    	  Log.d(TAG, "Asking to enable BT");
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      } else {
          if (mBluetoothService == null) {
        	  startBTService();
          }
      }
  }
  private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
      @Override
      public void onManagerConnected(int status) {
          switch (status) {
              case LoaderCallbackInterface.SUCCESS: {
                  Log.i(TAG, "OpenCV loaded successfully");

                  // Load native libraries after(!) OpenCV initialization
                  postOpenCVLoad();
              } break;
              default: {
                  super.onManagerConnected(status);
              } break;
          }
      }
  };

  // override this to use opencv dependent libraries
  public void postOpenCVLoad() {
      Toast.makeText(this, "OpenCV initialized successfully", Toast.LENGTH_SHORT).show();
      return;
  }

  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
    
    btnMultiModeViewer = (Button) findViewById(R.id.btnMultiModeViewer);
    btnConnectBluetooth = (Button) findViewById(R.id.btnConnectBluetooth);
    btnAcquireBFScan = (Button) findViewById(R.id.btnAcquireBFScan);
    btnAcquireFullScan = (Button) findViewById(R.id.btnAcquireFullScan);
    btnAcquireMultiMode = (Button) findViewById(R.id.btnAcquireMuitimode);
    btnGallery = (Button) findViewById(R.id.btnGallery);
    
    connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
    connDeviceNameTextView = (TextView) findViewById(R.id.connDeviceNameTextView);
    connMACAddressTextView = (TextView) findViewById(R.id.connMACAddressTextView);
    
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
        Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        finish();
        return;
    }
    
    // Create the BT service object
	mBluetoothService = new BluetoothService(this, mHandler);
	
    // See if we've already got an instance of the bluetooth connection going, and if so update the UI to reflect this
	GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
	
	// Set the global Dataset object
	BTAppClass.setDataset(mDataset);
	if (BTAppClass.getBluetoothService() != null)
	{
		//Toast.makeText(this, "Bluetooth class is active!", Toast.LENGTH_LONG).show();
		mBluetoothService = BTAppClass.getBluetoothService();
		
        connDeviceNameTextView.setText(mBluetoothService.getDeviceName());
        connMACAddressTextView.setText(mBluetoothService.getDeviceAddress());
		
  	    connStatusTextView.setText("Connected to Array");
  	    btnConnectBluetooth.setText("Disconnect from Array");
  	    btConnection = true;
	} else {
		startBluetooth();
		// Try and connect to the default class
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(defaultAddress);
        // Attempt to connect to the device
        mBluetoothService.connect(device);
		BTAppClass.setBluetoothService(mBluetoothService);
	}
    
    btnConnectBluetooth.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	if (btConnection)
        	{
        		sendData("endConnection");
        		stopBTService();
        	}
        	else
        		startBTService();
        }
      });
    
    btnAcquireBFScan.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	startAcquireActivity("Brightfield_Scan");
        }
      });
    
    btnAcquireFullScan.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	startAcquireActivity("Full_Scan");
        }
      });
    
    btnAcquireMultiMode.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	startAcquireActivity("MultiMode");
        	
        }
    });
    
    btnMultiModeViewer.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	startMultiViewerActivity();
        	
        	
        }
    });
    
    btnGallery.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			startGallery();
		}
	});
    btnComputeRefocus = (Button) findViewById(R.id.btnComputeRefocus);
    btnComputeRefocus.setOnClickListener(new View.OnClickListener() {
    	
    	
        @Override
        public void onClick(View v) {

	        if (mDataset.DATASET_PATH != "")
	        	new ComputeRefocusTask(ComputationalCellScopeMain.this).execute(mDataset);
	        else
	        	chooseDatasetDirectory();
        }
    });
    
  	}

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "...onResume - try connect...");
  }
  
  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "...In onPause()...");
  }
  
  //ensure app is in portrait orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  //fire intent to start activity with proper configuration for acquire type
  protected void startAcquireActivity(String type) {
      Intent intent = new Intent(this, AcquireActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("type", type);
      startActivity(intent);
  }
  
  protected void startMultiViewerActivity() {
      Intent intent = new Intent(this, MultiModeViewActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
  }
  
  public void chooseDatasetDirectory()
  {
      final String m_chosenDir = "";
      boolean m_newFolderEnabled = true;
      final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
       
       // Create DirectoryChooserDialog and register a callback 
       DirectoryChooserDialog directoryChooserDialog = 
       new DirectoryChooserDialog(ComputationalCellScopeMain.this, 
           new DirectoryChooserDialog.ChosenDirectoryListener() 
       {
           @Override
           public void onChosenDir(String chosenDir) 
           {

               File file = new File(chosenDir);
               if(file.exists()) {
                   SharedPreferences.Editor edit = settings.edit();
                   edit.commit();
                   mDataset.DATASET_PATH = chosenDir+"/";
                   Log.d(TAG, "CHOSEN DIR: " + chosenDir);
                   mDataset.buildFileListFromPath(chosenDir+"/");
               }
               else{
                   Toast.makeText(getApplicationContext(), "This file does not exist", Toast.LENGTH_LONG).show();
               }
               
               Toast.makeText(
               		getApplicationContext(), "Chosen directory: " + 
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

  protected void startGallery(){
      final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      
	  if (mDataset.DATASET_PATH != "")
	  {
		  
	      AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setMessage("Use Current Dataset?");
		   // Add the buttons
		   builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		              public void onClick(DialogInterface dialog, int id) {
		        		  File file = new File(mDataset.DATASET_PATH);
		        		  if(file.exists()) {
		        		      SharedPreferences.Editor edit = settings.edit();
		        		      edit.putString(location_name, mDataset.DATASET_PATH+"Refocused/");
		        		      edit.commit();
		        		      Intent intent = new Intent(ComputationalCellScopeMain.this, Image_Gallery.class);
		        		      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        		      startActivity(intent);
		        		  }
		        		  else
		        		  {
		        		      Toast.makeText(getApplicationContext(), "This file does not exist: ", Toast.LENGTH_LONG).show();
		        		  }
		              }
		          });
		   builder.setNegativeButton("No - Choose new Dataset", new DialogInterface.OnClickListener() {
		              public void onClick(DialogInterface dialog, int id) {
		        		  chooseDatasetDirectory();
		              }
		          });


	   // Create the AlertDialog
	   AlertDialog dialog = builder.create();
	   dialog.show();
	   
	  }
	  else
		  chooseDatasetDirectory(); 
  }
  
  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
          switch (msg.what) {
          case MESSAGE_STATE_CHANGE:
              if(Debug) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
              switch (msg.arg1) {
              case BluetoothService.STATE_CONNECTED:
            	  connStatusTextView.setText("Connected to Array");
            	  btnConnectBluetooth.setText("Disconnect from Array");
            	  btConnection = true;
                  break;      
              case BluetoothService.STATE_CONNECTING:
            	  connStatusTextView.setText("Connecting to Array");
                  connDeviceNameTextView.setText("");
                  connMACAddressTextView.setText("");
            	  btConnection = false;
                  break;
              case BluetoothService.STATE_LISTEN:
            	  connStatusTextView.setText("Disconnected");
            	  btnConnectBluetooth.setText("Connect to Array");
                  connDeviceNameTextView.setText("");
                  connMACAddressTextView.setText("");
            	  btConnection = false;
            	  break;
              case BluetoothService.STATE_NONE:
            	  connStatusTextView.setText("Disconnected");
            	  btnConnectBluetooth.setText("Connect to Array");
                  connDeviceNameTextView.setText("");
                  connMACAddressTextView.setText("");
            	  btConnection = false;
                  break;
              }
              break;
          case MESSAGE_WRITE:
              byte[] writeBuf = (byte[]) msg.obj;
              // construct a string from the buffer
              String writeMessage = new String(writeBuf);
              mConversationArrayAdapter.add("Me:  " + writeMessage);
              break;
          case MESSAGE_READ:
              //byte[] readBuf = (byte[]) msg.obj;
              // construct a string from the valid bytes in the buffer
              //String readMessage = new String(readBuf, 0, msg.arg1);
              //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
              break;
          case MESSAGE_DEVICE_NAME:
              // save the connected device's name
              mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
              mConnectedDeviceMACAddress = msg.getData().getString(DEVICE_ADDRESS);
              Toast.makeText(getApplicationContext(), "Connected to "
                             + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
              connDeviceNameTextView.setText(mConnectedDeviceName);
              connMACAddressTextView.setText(mConnectedDeviceMACAddress);
              break;
          case MESSAGE_TOAST:
              Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                             Toast.LENGTH_SHORT).show();
              break;
          }
      }
  };
  
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if(Debug) Log.d(TAG, "onActivityResult " + resultCode);
      switch (requestCode) {
      case REQUEST_CONNECT_DEVICE:
          // When DeviceListActivity returns with a device to connect
          if (resultCode == Activity.RESULT_OK) {
              // Get the device MAC address
              String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
              // Show the address for debugging
              if(Debug)Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
              // Get the BLuetoothDevice object
              BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
              // Attempt to connect to the device
              mBluetoothService.connect(device);
          }
          break;
      case REQUEST_ENABLE_BT:
          // When the request to enable Bluetooth returns
          if (resultCode == Activity.RESULT_OK) {
              // Bluetooth is now enabled, so set up the Bluetooth service
        	  startBTService();
          } else {
              // User did not enable Bluetooth or an error occured
              Log.d(TAG, "BT not enabled");
              Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT).show();
              finish();
          }
      }
      if(requestCode == ACTIVITY_CHOOSE_FILE)
      {
            if (data != null)
            {
	    	    Uri uri = data.getData();
	            String FilePath = getRealPathFromURI(uri);
	            mDataset.buildFileListFromPath(FilePath);
            }
      }
      if (resultCode != RESULT_OK) return;
  }
  
  public String getRealPathFromURI(Uri contentUri) {
	    String [] proj      = {MediaColumns.DATA};
	    Cursor cursor       = getContentResolver().query( contentUri, proj, null, null,null); 
	    if (cursor == null) return null; 
	    int column_index    = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
  }
  
  private void startBTService() 
  {
	  Log.d(TAG, "Starting BT Service");
	
	  // Initialize the buffer for outgoing memBtAdapterssages
	  mOutStringBuffer = new StringBuffer("");
	  
      serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
      startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
      
	  // Tie the service to the global application context
	  GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
	  BTAppClass.setBluetoothService(mBluetoothService);
      
  }
  private void stopBTService() {
	  Log.d(TAG, "Stopping BT Service");
	  mBluetoothService.stop();
  }
  
  
  public void sendData(String message) {
      // Check that we're actually connected before trying anything
      if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
          Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
          return;
      }

      // Check that there's actually something to send
      if (message.length() > 0) {
          // Get the message bytes and tell the BluetoothChatService to write
          byte[] send = message.getBytes();
          mBluetoothService.write(send);
      }
  }
}



