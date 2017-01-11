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

import java.net.URL;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.os.AsyncTask;
import android.util.Log;



class calcDPCTask extends AsyncTask<Mat, Integer, Long> {
	private String TAG = "calcDPCTask";
    protected Long doInBackground(Mat... matrix_list) {
        //int count = urls.length;
    	Mat in1 = matrix_list[0];
    	Mat in2 = matrix_list[1];
    	Mat outputMat = matrix_list[2];

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
        output.copyTo(outputMat,maskedImg);
        output.release();
        maskedImg.release();
		return null; 
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(Long result) {
    	Log.d(TAG,"FINISHED CALCULATING DPC");
    }
}