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
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import com.wallerlab.compcellscope.bluetooth.BluetoothService;
import com.wallerlab.compcellscope.dialogs.AcquireSettings.NoticeDialogListener;
import com.wallerlab.compcellscope.surfaceviews.AcquireSurfaceView;
import com.wallerlab.compcellscope.MultiModeView;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MultiModeViewActivity extends Activity implements OnTouchListener, NoticeDialogListener, CvCameraViewListener2 {
    
    private static final String TAG = "cCS_MultimodeView";
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
    public double brightfieldNA = 0.25; // Account for LED size to be sure we completly cover NA .025
    
    public int ledCount = 508;
    public int centerLED = 249;
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
    public String fileName = "default";
    public boolean cameraReady = true;
    public int mmCount = 5;
    public float mmDelay = 0.0f;
    public String datasetName = "Dataset";
    
    private int frameNum = 1;
    private boolean dpcSwitch = false;
    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat 				   dpcLeft;
    private Mat                    dpcRight;
    private Mat                    dpcTop;
    private Mat                    dpcBottom;
    private Mat                    mmGrid;
    private Mat                    dpcLRImg;
    private Mat                    dpcTBImg;
    private Mat                    bfImg;
    private Mat                    dfImg;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;
    
    public int                     horzCrop = 0;
    public int                     vertCrop = 300;
    
    private Rect                   TLRect;
    private Rect                   TRRect;
    private Rect                   BLRect;
    private Rect                   BRRect;
    private int                    viewMode = 0;
    private boolean				   updateTrig = false;
    
	private Size 				   sz;

    private MultiModeView mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MultiModeViewActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MultiModeViewActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.multimode_liveview_layout);

        mOpenCvCameraView = (MultiModeView) findViewById(R.id.mmCameraView);
        
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
    	GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
    	mBluetoothService = BTAppClass.getBluetoothService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
  
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        sz = new Size(width,height); //960x1280 (5) 480x800 (4)
        mIntermediateMat = new Mat(height, width, CvType.CV_16UC4);

        dpcLeft = new Mat(height, width, CvType.CV_8UC4);
        dpcRight = new Mat(height, width, CvType.CV_8UC4);
        dpcTop = new Mat(height, width, CvType.CV_8UC4);
        dpcBottom = new Mat(height, width, CvType.CV_8UC4);

        mmGrid = new Mat(height,width,CvType.CV_8UC4);
        
        bfImg = new Mat(height,width,CvType.CV_8UC4);
        dfImg = new Mat(height,width,CvType.CV_8UC4);
        dpcLRImg = new Mat(height, width, CvType.CV_8UC4);
        dpcTBImg = new Mat(height, width, CvType.CV_8UC4);
        
        TLRect = new Rect(0,0,width/2,height/2);
        TRRect = new Rect(width/2,0,width/2,height/2);
        BLRect = new Rect(0,height/2,width/2,height/2);
        BRRect = new Rect(width/2,height/2,width/2,height/2);
        
        mCamera = mOpenCvCameraView.getCameraObject();
        //Set Exposure
        sendData("bf");
        Camera.Parameters camParams;
        camParams = mCamera.getParameters();
        camParams.setAutoExposureLock(false);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        //camParams.setAutoExposureLock(true);
        mCamera.setParameters(camParams);
        
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        sendData("xx");
        }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    
        mCamera = mOpenCvCameraView.getCameraObject();
    	mRgba = inputFrame.rgba();

    	Camera.Parameters camParams;
    	camParams = mCamera.getParameters();
    	
    	int bfCompensation = 0;
    	int dfCompensation = 0*camParams.getMaxExposureCompensation();
    	
    	if (viewMode == 0)
    	{
	    	switch (frameNum)
	    	{
		    	case 1: // Left
		    	{
		    		mRgba.copyTo(dpcLeft);
		    		sendData("dr");
		    		frameNum = 2;
		    		break;
		    	}
		    	case 2: // Right
		    	{   
		    		mRgba.copyTo(dpcRight);
		    		sendData("dt");
		    		frameNum = 3;
		    		break;
		    	}
		    	case 3: // Top
		    	{
		    		mRgba.copyTo(dpcTop);
		    		sendData("db");
		    		frameNum = 4;
		    		break;
		    	}
		    	case 4: // Bottom
		    	{
		    		mRgba.copyTo(dpcBottom);
		    		sendData("bf");
		    		camParams.setExposureCompensation(bfCompensation);
		    		mCamera.setParameters(camParams);
		    		frameNum = 5;
		    		break;
		    	}
		    	case 5: // Brightfield
		    	{
		    		camParams.setExposureCompensation(dfCompensation);
		    		mCamera.setParameters(camParams);
		    		mRgba.copyTo(bfImg);
		    		sendData("an");
	    			
		    		frameNum = 6;
		    		break;
		    	}
		    	case 6: // Darkfield
		    	{
		    		mRgba.copyTo(dfImg);
		    		sendData("dl");

		    		frameNum = 1;
		    		break;
		    	}
	    	}
	    	//dpcLRImg = calcDPC(dpcLeft, dpcRight,dpcLRImg);
	    	new calcDPCTask().execute(dpcLeft, dpcRight, dpcLRImg);
	    	new calcDPCTask().execute(dpcTop, dpcBottom, dpcTBImg);
    	}
    	else
    	{
    		switch(viewMode)
    		{
    		case 1:
    		{
    			dpcSwitch = !dpcSwitch;
    			if (dpcSwitch)
    			{
		    		sendData("dl");
		    		mRgba.copyTo(dpcLeft);
    			}else{
		    		sendData("dr");
		    		mRgba.copyTo(dpcRight);
    			}
    			new calcDPCTask().execute(dpcLeft, dpcRight, dpcLRImg);
    			try {
    				Thread.sleep(50);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    	    	//dpcLRImg = calcDPC(dpcLeft, dpcRight,dpcLRImg);
    			break;
    		}
    		case 3:
    		{
    			dpcSwitch = !dpcSwitch;
    			if (dpcSwitch)
    			{
		    		sendData("db");
		    		mRgba.copyTo(dpcTop);
    			}else{
		    		sendData("dt");
		    		mRgba.copyTo(dpcBottom);
    			}
    	    	new calcDPCTask().execute(dpcTop, dpcBottom, dpcTBImg);
    			try {
    				Thread.sleep(50);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    	    	//dpcTBImg = calcDPC(dpcTop, dpcBottom,dpcTBImg);
    			break;
    		}
    		case 4:
    		{
    
				sendData("bf");
    			mRgba.copyTo(bfImg);
    			//Log.d(TAG,"Displayed bf img");
    			break;
    		}
    		case 5:
    		{
    			if (updateTrig){
    				sendData("an");
    				updateTrig = false;
    			}
    			mRgba.copyTo(dfImg);
    			//Log.d(TAG,"Displayed df img");
    			break;
    		}
    		}
    	}
        mmGrid = Mat.zeros(mmGrid.size(), mmGrid.type());
    	if (viewMode == 0)
    		mmGrid = generateMMFrame(mmGrid,bfImg,dfImg,dpcLRImg,dpcTBImg);
    	else if (viewMode == 1)
    		mmGrid = dpcLRImg;
    	else if (viewMode == 3)
    		mmGrid = dpcTBImg;
    	else if (viewMode == 4)
    		mmGrid = bfImg;
    	else if (viewMode == 5)
    		mmGrid = dfImg;
    	
        return mmGrid;
    }
    
    public Mat calcDPC(Mat in1, Mat in2, Mat out)
    {
        Mat Mat1 = new Mat(in1.width(),in1.height(), in1.type());
        Mat Mat2 = new Mat(in2.width(),in2.height(), in2.type());
        in1.copyTo(Mat1);
        in2.copyTo(Mat2);
        
        Imgproc.cvtColor(Mat1, Mat1, Imgproc.COLOR_RGBA2GRAY, 1);
        Imgproc.cvtColor(Mat2, Mat2, Imgproc.COLOR_RGBA2GRAY, 1);
        
        Mat output = new Mat(Mat1.width(),Mat1.height(), CvType.CV_8UC4);
        Mat dpcSum = new Mat(Mat1.width(), Mat1.height(), CvType.CV_32FC1);
        Mat dpcDifference = new Mat(Mat1.width(), Mat1.height(), CvType.CV_32FC1);
        Mat dpcImgF = new Mat(Mat1.width(), Mat1.height(), CvType.CV_32FC1);
        
        /*
	    Log.d(TAG,String.format("Mat1 format is %.1f-%.1f, type: %d",Mat1.size().width,Mat1.size().height,Mat1.type()));
    	Log.d(TAG,String.format("Mat2 format is %.1f-%.1f, type: %d",Mat2.size().width,Mat2.size().height,Mat2.type()));
		*/
        
    	// Convert to Floats
    	Mat1.convertTo(Mat1, CvType.CV_32FC1);
    	Mat2.convertTo(Mat2, CvType.CV_32FC1);
    	Core.add(Mat1, Mat2, dpcSum);
    	Core.subtract(Mat1, Mat2, dpcDifference);
    	Core.divide(dpcDifference, dpcSum, dpcImgF);
		Core.add(dpcImgF, new Scalar(1.0), dpcImgF); // Normalize to 0-2.0
		Core.multiply(dpcImgF, new Scalar(110), dpcImgF); // Normalize to 0-255
		dpcImgF.convertTo(output, CvType.CV_8UC1); // Convert back into RGB
        Imgproc.cvtColor(output, output, Imgproc.COLOR_GRAY2RGBA, 4);
        
        dpcSum.release();
        dpcDifference.release();
        dpcImgF.release();
        Mat1.release();
        Mat2.release();
        
        Mat maskedImg = Mat.zeros(output.rows(), output.cols(), CvType.CV_8UC4);
        int radius = maskedImg.width()/2+25;
        Core.circle(maskedImg, new Point (maskedImg.width()/2,maskedImg.height()/2),radius, new Scalar (255,255,255),-1, 8, 0);
        output.copyTo(out,maskedImg);
        output.release();
        maskedImg.release();
		return out;
    }
    
    public Mat generateMMFrame(Mat gridOut, Mat MatTL, Mat MatTR, Mat MatBL, Mat MatBR)
    {
    	//gridOut = new Mat(100, 100, gridOut.type(), new Scalar(0,0,0));
    	Mat Mat1 = new Mat(MatTL.size(),MatTL.type());
    	Mat Mat2 = new Mat(MatTR.size(),MatTR.type());
    	Mat Mat3 = new Mat(MatBL.size(),MatBL.type());
    	Mat Mat4 = new Mat(MatBR.size(),MatBR.type());
    	
    	// Ensure all of the mats are of the correct size since pyramid operation resizes
    	Imgproc.resize( MatTL, MatTL, sz );
    	Imgproc.resize( MatTR, MatTR, sz );
    	Imgproc.resize( MatBL, MatBL, sz );
    	Imgproc.resize( MatBR, MatBR, sz );
    	
    	// Downsample by 2 for 2x2 grid
    	Imgproc.pyrDown(MatBL, Mat1);
    	Imgproc.pyrDown(MatBR, Mat2);
    	Imgproc.pyrDown(MatTL, Mat3);
    	Imgproc.pyrDown(MatTR, Mat4);
    	
    	/*
	    Log.d(TAG,String.format("TLRect format is %.1f-%.1f",TLRect.size().width,TLRect.size().height));
    	Log.d(TAG,String.format("TRRect format is %.1f-%.1f",TRRect.size().width,TRRect.size().height));

	    Log.d(TAG,String.format("BLRect format is %.1f-%.1f",BLRect.size().width,BLRect.size().height));
    	Log.d(TAG,String.format("BRRect format is %.1f-%.1f",BRRect.size().width,BRRect.size().height));
       
	    Log.d(TAG,String.format("MatTL format is %.1f-%.1f",MatTL.size().width,MatTL.size().height));
    	Log.d(TAG,String.format("MatTR format is %.1f-%.1f",MatTR.size().width,MatTR.size().height));

	    Log.d(TAG,String.format("MatBL format is %.1f-%.1f",MatBL.size().width,MatBL.size().height));
    	Log.d(TAG,String.format("MatBR format is %.1f-%.1f",MatBR.size().width,MatBR.size().height));
        */
    	
    	Core.putText(Mat1, "DPC-LR", new Point(43,40), Core.FONT_ITALIC, 1, new Scalar(255,255,0));
    	Core.putText(Mat2, "DPC-TB", new Point(43,40), Core.FONT_ITALIC, 1, new Scalar(255,255,0));
    	Core.putText(Mat3, "BrightField", new Point(33,40), Core.FONT_ITALIC, 1, new Scalar(255,255,0));
    	Core.putText(Mat4, "DarkField", new Point(37,40), Core.FONT_ITALIC, 1, new Scalar(255,255,0));
    	
    	Mat1.copyTo(gridOut.submat(BLRect));
    	Mat2.copyTo(gridOut.submat(BRRect));
    	Mat3.copyTo(gridOut.submat(TLRect));
    	Mat4.copyTo(gridOut.submat(TRRect));
    	
    	Mat1.release();
    	Mat2.release();
    	Mat3.release();
    	Mat4.release();

    	return gridOut;
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        /*
        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        }
        */
        return true;
    }

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
		{
			if (viewMode == 0)
			{
				updateTrig = true;
				// Determine set new view based on where the user touched
				if ((int)event.getY() < v.getHeight()/2 )
				{
					if ((int)event.getX() > v.getWidth()/2)
					{
						viewMode = 5;
					    Camera.Parameters camParams = mCamera.getParameters();
					    camParams.setExposureCompensation(-6);
					    mCamera.setParameters(camParams);
					}
					else
					{
						viewMode = 4;
					    Camera.Parameters camParams = mCamera.getParameters();
					    camParams.setExposureCompensation(0);
					    mCamera.setParameters(camParams);
					}
				}else{
				    Camera.Parameters camParams = mCamera.getParameters();
				    camParams.setExposureCompensation(0);
				    mCamera.setParameters(camParams);
					if ((int)event.getX() > v.getWidth()/2)
						viewMode = 3;
					else
						viewMode = 1;
				}
			}
			else
			{
			    Camera.Parameters camParams = mCamera.getParameters();
			    camParams.setExposureCompensation(0);
			    mCamera.setParameters(camParams);
				viewMode = 0; // Set back to multimode display
			}
		}
		return false;
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
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
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
     MediaScannerConnection.scanFile(MultiModeViewActivity.this,
              fileListString, null,
              new MediaScannerConnection.OnScanCompletedListener() {
                  public void onScanCompleted(String path, Uri uri) {
                      //Log.i("TAG", "Finished scanning " + path);
                  }
              });
	}
  
  public void openSettingsDialog()
  {
	    //settingsDialogFragment.show(getFragmentManager(), "acquireSettings");
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
	  sendData("df");
      Camera.Parameters camParams = mCamera.getParameters();
      camParams.setAutoExposureLock(false);
      mCamera.setParameters(camParams);
  }
  
  public void toggleAlignment()
  {
	  // TODO - add alignment routine
	  sendData("xx");
  }
}
