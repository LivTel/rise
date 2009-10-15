/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Ccs.

    Ccs is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Ccs is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Ccs; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
/* ngat_rise_ccd_CCDLibrary.c
** implementation of Java Class ngat.ccd.CCDLibrary native interfaces
** $Header: /space/home/eng/cjm/cvs/rise/ccd/c/ngat_rise_ccd_CCDLibrary.c,v 1.1 2009-10-15 10:16:23 cjm Exp $
*/
/**
 * ngat_rise_ccd_CCDLibrary.c is the 'glue' between librise_ccd, the C library version of the SDSU CCD Controller
 * software, and CCDLibrary.java, a Java Class to drive the controller. CCDLibrary specifically
 * contains all the native C routines corresponding to native methods in Java.
 * @author SDSU, Chris Mottram LJMU
 * @version $Revision: 1.1 $
 */
/**
 * This hash define is needed before including source files give us POSIX.4/IEEE1003.1b-1993 prototypes
 * for time.
 */
#define _POSIX_SOURCE 1
/**
 * This hash define is needed before including source files give us POSIX.4/IEEE1003.1b-1993 prototypes
 * for time.
 */
#define _POSIX_C_SOURCE 199309L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <time.h>
#include "ccd_global.h"
#include "ccd_dsp.h"
#include "ccd_exposure.h"
#include "ccd_multrun.h"
#include "ccd_filter_wheel.h"
#include "ccd_interface.h"
#include "ccd_pci.h"
#include "ccd_setup.h"
#include "ccd_temperature.h"
#include "ccd_text.h"
#include "ngat_rise_ccd_CCDLibrary.h"

/* hash definitions */
/**
 * Constant value for the size of buffer to use when getting the librise_ccd error string using CCD_Global_Error_String.
 * @see ccd_global.html#CCD_Global_Error_String
 */
#define CCD_LIBRARY_ERROR_STRING_LENGTH	4096

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: ngat_rise_ccd_CCDLibrary.c,v 1.1 2009-10-15 10:16:23 cjm Exp $";

/**
 * Copy of the java virtual machine pointer, used for logging back up to the Java layer from C.
 */
static JavaVM *java_vm = NULL;
/**
 * Cached global reference to the "ngat.ccd.CCDLibrary" logger, used to log back to the Java layer from
 * C routines.
 */
static jobject logger = NULL;
/**
 * Cached reference to the "ngat.util.logging.Logger" class's log(int level,String message) method.
 * Used to log C layer log messages, in conjunction with the logger's object reference logger.
 * @see #logger
 */
static jmethodID log_method_id = NULL;

/* internal routines */
static void CCDLibrary_Throw_Exception(JNIEnv *env,jobject obj,char *function_name);
static void CCDLibrary_Throw_Exception_String(JNIEnv *env,jobject obj,char *function_name,char *error_string);
static void CCDLibrary_Log_Handler(int level,char *string);
static int CCDLibrary_Java_String_List_To_C_List(JNIEnv *env,jobject obj,jobject java_list,
						 jstring **jni_jstring_list,int *jni_jstring_count,
						 char ***c_list,int *c_list_count);
static int CCDLibrary_Java_String_List_Free(JNIEnv *env,jobject obj,
					    jstring *jni_jstring_list,int jni_jstring_count,
					    char **c_list,int c_list_count);

/* ------------------------------------------------------------------------------
** 		External routines
** ------------------------------------------------------------------------------ */
/* ------------------------------------------------------------------------------
** 		ccd_dsp.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_DSP_Abort<br>
 * Signature: ()V<br>
 * Java Native Interface routine to abort a DSP command sent to the controller.
 * This is done by calling CCD_DSP_Set_Abort(TRUE).
 * @see  ccd_dsp.html#CCD_DSP_Set_Abort
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1DSP_1Abort(JNIEnv *env,jobject obj)
{
	CCD_DSP_Set_Abort(TRUE);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_DSP_Command_PEX<br>
 * Signature: ()V<br>
 * Java Native Interface routine to pause an exposure.
 * This is done by calling CCD_DSP_Command_PEX.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see  ccd_dsp.html#CCD_DSP_Command_PEX
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1DSP_1Command_1PEX(JNIEnv *env,jobject obj)
{
	int retval;

	retval = CCD_DSP_Command_PEX();
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_DSP_Command_PEX");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_DSP_Command_Read_Exposure_Time<br>
 * Signature: ()I<br>
 * Java Native Interface routine to read the length of time an exposure is underway.
 * This is done by calling CCD_DSP_Command_RET.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * If an exposure is not underway (the shutter is closed) zero is returned.
 * @see  ccd_dsp.html#CCD_DSP_Command_RET
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1DSP_1Command_1Read_1Exposure_1Time(JNIEnv *env,jobject obj)
{
	int retval;

	retval = CCD_DSP_Command_RET();
	if((retval == 0)&&(CCD_DSP_Get_Error_Number()))
		CCDLibrary_Throw_Exception(env,obj,"CCD_DSP_Command_Read_Exposure_Time");
	return retval;
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_DSP_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for the ccd_dsp part of the library.
 * @return The current error number of ccd_dsp. A zero error number means an error has not occured.
 * @see ccd_dsp.html#CCD_DSP_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1DSP_1Get_1Error_1Number(JNIEnv *env, jobject obj)
{
	return CCD_DSP_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_exposure.c
** ------------------------------------------------------------------------------ */

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Multrun_Expose<br>
 * Signature: (ZJILjava/lang/String;)V<br>
 * Java Native Interface routine to do multrun exposures.
 * <a href="ccd_exposure.html#CCD_Exposure_Expose">CCD_Exposure_Expose</a> is called to perform the exposure.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @param open_shutter Whether to open the shutter or not.
 * @param startTime What time to start the exposure. If -1, we pass a 0 timespec structure to the C code,
 *        which means start anytime. Otherwise the time to start the exposure, in milliseconds since 1970, which
 *        is parsed into a timespec struct and passed to the C layer.
 * @param exposureTime The length of exposure to do, in milliseconds.
 * @param exposures The number of exposures to carry out
 * @see ccd_exposure.html#CCD_Multrun_Exposure
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Multrun_1Expose
  (JNIEnv *env, jobject obj, jboolean open_shutter, jlong startTime, jint exposureTime, jlong exposures, jobject headers)
{
	int retval,jni_header_count,header_count;
	jstring *jni_header_list = NULL;
	struct timespec start_time;
	char **headers_list =NULL;

	retval = CCDLibrary_Java_String_List_To_C_List(env,obj,headers,
						&jni_header_list, &jni_header_count,
						&headers_list,&header_count);
	if(retval == FALSE) return; /* Throw exception */ 

	if(startTime > -1)
	{
		start_time.tv_sec = (time_t)(startTime/((jlong)1000L));
		start_time.tv_nsec = (long)((startTime%((jlong)1000L))*1000000L);
	}
	else
	{
		start_time.tv_sec = 0;
		start_time.tv_nsec = 0;
	}

	/* do exposure */
	/* retval = CCD_Multrun_Expose(open_shutter,-1,exposureTime,exposures, headers_list); */
	retval = CCD_Multrun_Expose(open_shutter,-1,exposureTime,exposures, headers_list);
	
	CCDLibrary_Java_String_List_Free(env,obj,jni_header_list,jni_header_count,
						headers_list, header_count);
	
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Multrun_Expose");
	}
}

JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Multflat_1Expose
  (JNIEnv *env, jobject obj, jboolean open_shutter, jlong startTime, jint exposureTime, jlong exposures, jobject headers)
{
	int retval,jni_header_count,header_count;
	jstring *jni_header_list = NULL;
	struct timespec start_time;
	char **headers_list =NULL;

	retval = CCDLibrary_Java_String_List_To_C_List(env,obj,headers,
						&jni_header_list, &jni_header_count,
						&headers_list,&header_count);
	if(retval == FALSE) return; /* Throw exception */ 

	if(startTime > -1)
	{
		start_time.tv_sec = (time_t)(startTime/((jlong)1000L));
		start_time.tv_nsec = (long)((startTime%((jlong)1000L))*1000000L);
	}
	else
	{
		start_time.tv_sec = 0;
		start_time.tv_nsec = 0;
	}

	/* do exposure */
	/*retval = CCD_Multrun_Expose(open_shutter,-1,exposureTime,exposures, headers_list); */
	retval = CCD_Multflat_Expose(open_shutter,-1,exposureTime,exposures, headers_list); 
	
	CCDLibrary_Java_String_List_Free(env,obj,jni_header_list,jni_header_count,
						headers_list, header_count);
	
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Multrun_Expose");
	}
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Expose<br>
 * Signature: (ZJILjava/lang/String;)V<br>
 * Java Native Interface routine to do an exposure.
 * <a href="ccd_exposure.html#CCD_Exposure_Expose">CCD_Exposure_Expose</a> is called to perform the exposure.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @param open_shutter Whether to open the shutter or not.
 * @param startTime What time to start the exposure. If -1, we pass a 0 timespec structure to the C code,
 *        which means start anytime. Otherwise the time to start the exposure, in milliseconds since 1970, which
 *        is parsed into a timespec struct and passed to the C layer.
 * @param exposureTime The length of exposure to do, in milliseconds.
 * @param filenameList This should be a jobject reference to to java.util.List (probably a java.util.Vector)
 *        containing a list of String's (java.util.String/jstring) containing filenames for each of the windows to
 *        readout.
 * @see ccd_exposure.html#CCD_Exposure_Expose
 * @see #CCDLibrary_Throw_Exception
 * @see #CCDLibrary_Java_String_List_To_C_List
 * @see #CCDLibrary_Java_String_List_Free
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Expose(JNIEnv *env,jobject obj,jboolean open_shutter, 
	jlong startTime,jint exposureTime,jobject filenameList)
{
	int retval,jni_filename_count,filename_count;
	struct timespec start_time;
	jstring *jni_filename_list = NULL;
	char **filename_list = NULL;

	/* convert filenameList to filename_list (Java to C) */
	retval = CCDLibrary_Java_String_List_To_C_List(env,obj,filenameList,
						 &jni_filename_list,&jni_filename_count,
						 &filename_list,&filename_count);
	if(retval == FALSE)
		return; /* CCDLibrary_Java_String_List_To_C_List throws exception */
	/* convert startTime to start_time */
	if(startTime > -1)
	{
		start_time.tv_sec = (time_t)(startTime/((jlong)1000L));
		start_time.tv_nsec = (long)((startTime%((jlong)1000L))*1000000L);
	}
	else
	{
		start_time.tv_sec = 0;
		start_time.tv_nsec = 0;
	}
	/* do exposure */
	retval = CCD_Exposure_Expose(TRUE,open_shutter,start_time,exposureTime,filename_list,filename_count);
	/* Free the generated C filename_list, and associated jstring list */
	CCDLibrary_Java_String_List_Free(env,obj,jni_filename_list,jni_filename_count,
		    filename_list,filename_count);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Exposure_Expose");
	}
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Bias<br>
 * Signature: (Ljava/lang/String;)V<br>
 * Java Native Interface routine to take a bias frame.
 * The filename parameter is translated from a Java to a C string and then 
 * <a href="ccd_exposure.html#CCD_Exposure_Bias">CCD_Exposure_Bias</a> is called to do the bias frame.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_exposure.html#CCD_Exposure_Bias
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Bias(JNIEnv *env, jobject obj, jstring filename)
{
	int retval;
	const char *cfilename = NULL;

	/* Get the filename froma java string to a c null terminated string
	** If the java String is null the cfilename should be null as well */
	if(filename != NULL)
		cfilename = (*env)->GetStringUTFChars(env,filename,0);
	retval = CCD_Exposure_Bias((char*)cfilename);
	/* If we created the cfilename string we need to free the memory it uses */
	if(filename != NULL)
		(*env)->ReleaseStringUTFChars(env,filename,cfilename);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Exposure_Bias");
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Read_Out_CCD<br>
 * Signature: (Ljava/lang/String;)V<br>
 * Java Native Interface implementation to readout the current data on the CCD to a file. 
 * The filename parameter is translated from a Java to a C string and then 
 * <a href="ccd_exposure.html#CCD_Exposure_Read_Out_CCD">CCD_Exposure_Read_Out_CCD</a> is called to do the readout.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_exposure.html#CCD_Exposure_Read_Out_CCD
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Read_1Out_1CCD(JNIEnv *env, jobject obj, jstring filename)
{
	int retval;
	const char *cfilename = NULL;

	/* Get the filename froma java string to a c null terminated string
	** If the java String is null the cfilename should be null as well */
	if(filename != NULL)
		cfilename = (*env)->GetStringUTFChars(env,filename,0);
	retval = CCD_Exposure_Read_Out_CCD((char*)cfilename);
	/* If we created the cfilename string we need to free the memory it uses */
	if(filename != NULL)
		(*env)->ReleaseStringUTFChars(env,filename,cfilename);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Exposure_Read_Out_CCD");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Abort<br>
 * Signature: ()V<br>
 * Java Native Interface implementation to abort an exposure. 
 * <a href="ccd_exposure.html#CCD_Exposure_Abort">CCD_Exposure_Abort</a> is called to abort the exposure.
 * @see ccd_exposure.html#CCD_Exposure_Abort
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Abort(JNIEnv *env, jobject obj)
{
	int retval;

	retval = CCD_Exposure_Abort();
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Exposure_Abort");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Get_Exposure_Status<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the current status of an exposure.
 * @return The current status of exposure, whether the ccd is not exposing,exposing or reading out.
 * @see ccd_exposure.html#CCD_Exposure_Get_Exposure_Status
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Get_1Exposure_1Status(JNIEnv *env, jobject obj)
{
	return CCD_Exposure_Get_Exposure_Status();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Get_Exposure_Length<br>
 * Signature: ()I<br>
 * Java native method to return the exposure length.
 * @return The exposure length in milliseconds.
 * @see ccd_exposure.html#CCD_Exposure_Get_Exposure_Length
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Get_1Exposure_1Length(JNIEnv *env,jobject obj)
{
	return CCD_Exposure_Get_Exposure_Length();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Get_Exposure_Start_Time<br>
 * Signature: ()J<br>
 * Java Native method to get the exposure start time.
 * @return A long, the time in milliseconds the exposure was started, since the EPOCH.
 * @see ccd_exposure.html#CCD_Exposure_Get_Exposure_Start_Time
 */
JNIEXPORT jlong JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Get_1Exposure_1Start_1Time(JNIEnv *env,jobject obj)
{
	struct timespec start_time;
	jlong retval;

	start_time = CCD_Exposure_Get_Exposure_Start_Time();
	retval = ((jlong)start_time.tv_sec)*((jlong)1000L);
	retval += ((jlong)start_time.tv_nsec)/((jlong)1000000L);
	return retval;
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Set_Readout_Remaining_Time<br>
 * Signature: (I)V<br>
 * Java Native method to set the amount of time, in milleseconds, 
 * remaining for an exposure when we change status to PRE_READOUT, to stop RDM/TDL/WRMs affecting the readout.
 * @param time The time, in milliseconds. Note, because the exposure time is read every second, it is best
 * 	not have have this constant an exact multiple of 1000.
 * @see ccd_exposure.html#CCD_Exposure_Set_Readout_Remaining_Time
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Set_1Readout_1Remaining_1Time(JNIEnv *env,jobject obj,
	jint time)
{
	CCD_Exposure_Set_Readout_Remaining_Time((int)time);
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Set_Start_Exposure_Clear_Time<br>
 * Signature: (I)V<br>
 * Java Native method to set how many seconds before an exposure is due to start we wish to send the CLR
 * command to the controller.
 * @param time The time in seconds. This should be greater than the time the CLR command takes to
 * 	clock all accumulated charge off the CCD.
 * @see ccd_exposure.html#CCD_Exposure_Set_Start_Exposure_Clear_Time
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Set_1Start_1Exposure_1Clear_1Time(JNIEnv *env,
	jobject obj,jint time)
{
	CCD_Exposure_Set_Start_Exposure_Clear_Time((int)time);
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Set_Start_Exposure_Offset_Time<br>
 * Signature: (I)V<br>
 * Java Native method to set the amount of time, in milliseconds, 
 * before the desired start of exposure that we should send the SEX command, to allow for transmission delay.
 * @param time The time, in milliseconds.
 * @see ccd_exposure.html#CCD_Exposure_Set_Start_Exposure_Offset_Time
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Set_1Start_1Exposure_1Offset_1Time(JNIEnv *env,
	jobject obj,jint time)
{
	CCD_Exposure_Set_Start_Exposure_Offset_Time((int)time);
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Exposure_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for the ccd_exposure part of the library.
 * @return The current error number of ccd_exposure. A zero error number means an error has not occured.
 * @see ccd_exposure.html#CCD_Exposure_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Exposure_1Get_1Error_1Number(JNIEnv *env, jobject obj)
{
	return CCD_Exposure_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_filter_wheel.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Set_Position_Count<br>
 * Signature: (I)V<br>
 * Java Native Interface routine to set the number of positions in each filter wheel.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Set_Position_Count
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Set_1Position_1Count(JNIEnv *env,jobject obj,
	jint position_count)
{
	int retval;
	
/*	retval = CCD_Filter_Wheel_Set_Position_Count((int)position_count); */
	retval = TRUE;
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Set_Position_Count");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Reset<br>
 * Signature: (I)V<br>
 * Java Native Interface routine to reset a filter wheel.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Reset
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Reset(JNIEnv *env,jobject obj,jint wheel_number)
{
	int retval;

	retval = CCD_Filter_Wheel_Reset((int)wheel_number);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Reset");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Move<br>
 * Signature: (II)V<br>
 * Java Native Interface routine to move a filter wheel.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Move
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Move(JNIEnv *env,jobject obj,jint wheel_number,
	jint position)
{
	int retval;

	retval = CCD_Filter_Wheel_Move((int)wheel_number,(int)position);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Move");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Abort<br>
 * Signature: ()V<br>
 * Java Native Interface routine to abort a filter wheel move/reset operation.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Abort
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Abort(JNIEnv *env, jobject obj)
{
	int retval;

	retval = CCD_Filter_Wheel_Abort();
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Abort");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for this module.
 * @return The current value of the error number for this module. A zero error number means an error has not occured.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Get_1Error_1Number(JNIEnv *env,jobject obj)
{
	return CCD_Filter_Wheel_Get_Error_Number();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Get_Status<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the current status of the filter wheel.
 * @return The current status of the filter wheel control, whether a filter wheel is moving, reseting or aborting.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Get_Status
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Get_1Status(JNIEnv *env,jobject obj)
{
	return CCD_Filter_Wheel_Get_Status();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Get_Position<br>
 * Signature: (I)I<br>
 * Java Native Interface routine to get a filter wheel's current position.
 * @return This routine returns the position if the call succeeded,
 * 	or returns -1 and throws a Java exception if the call returned FALSE (an error occured).
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Get_Position
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Get_1Position(JNIEnv *env, jobject obj,
	jint wheel_number)
{
	int retval,position;

	retval = CCD_Filter_Wheel_Get_Position((int)wheel_number,&position);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Get_Position");
		return -1;
	}
	return (jint)position;
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Filter_Wheel_Set_De_Bounce_Milliseconds<br>
 * Signature: (I)V<br>
 * Java Native Interface routine to set the the number of milliseconds before we start checking 
 * detent inputs.
 * @see ccd_filter_wheel.html#CCD_Filter_Wheel_Set_De_Bounce_Milliseconds
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Filter_1Wheel_1Set_1De_1Bounce_1Milliseconds(JNIEnv *env,
	jobject obj,jint ms)
{
	int retval;

	retval = CCD_Filter_Wheel_Set_De_Bounce_Milliseconds((int)ms);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Filter_Wheel_Set_De_Bounce_Milliseconds");
}

/* ------------------------------------------------------------------------------
** 		ccd_global.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Global_Initialise<br>
 * Signature: (I)V<br>
 * Java Native Interface implementation of <a href="ccd_global.html#CCD_Global_Initialise">CCD_Global_Initialise</a>,
 * which calls initialisation routines for various parts of the library.
 * It also sets the log handler to CCDLibrary_Log_Handler by calling CCD_Global_Set_Log_Handler_Function. This means
 * all logging messages get sent to the CCDLibrary's logger using a JNI call.
 * @see ccd_global.html#CCD_Global_Initialise
 * @see ccd_global.html#CCD_Global_Set_Log_Handler_Function
 * @see #CCDLibrary_Log_Handler
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Global_1Initialise(JNIEnv *env, jobject obj, jint interface_device)
{
	CCD_Global_Initialise(interface_device);
/* print some compile time information to stdout */
	fprintf(stdout,"ngat.ccd.CCDLibrary.CCD_Global_Initialise:%s.\n",rcsid);
	CCD_Global_Set_Log_Handler_Function(CCDLibrary_Log_Handler);
	fflush(stdout);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Global_Error<br>
 * Signature: ()V<br>
 * Java Native Interface implementation of <a href="ccd_global.html#CCD_Global_Error">CCD_Global_Error</a>, which
 * prints out to stderr any error messages outstanding in any of the modules that make up librise_ccd.
 * @see ccd_global.html#CCD_Global_Error
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Global_1Error(JNIEnv *env, jobject obj)
{
	CCD_Global_Error();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Global_Error_String<br>
 * Signature: ()Ljava/lang/String;<br>
 * Java Native Interface implementation of 
 * <a href="ccd_global.html#CCD_Global_Error_String">CCD_Global_Error_String</a>, which
 * returns any error messages outstanding in any of the modules that make up librise_ccd in a string.
 * Note this uses a hard coded error buffer, where it may seem more sensible to dynamically allocate
 * space for the string. However this routine is designed to work in situations where malloc is returning NULL
 * due to a memory error, so using malloc in an error routine is not desirable. This of course means an error
 * string greater than the array size will cause a memory overwrite, however this is deemed a smaller risk than
 * allocation occurring during the error routine.
 * @see ccd_global.html#CCD_Global_Error_String
 */
JNIEXPORT jstring JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Global_1Error_1String(JNIEnv *env , jobject obj)
{
	char error_string[CCD_LIBRARY_ERROR_STRING_LENGTH];

	CCD_Global_Error_String(error_string);
	return (*env)->NewStringUTF(env,error_string);
}

/* ------------------------------------------------------------------------------
** 		ccd_interface.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Interface_Open<br>
 * Signature: ()V<br>
 * Java Native Interface implementation of <a href="ccd_interface.html#CCD_Interface_Open">CCD_Interface_Open</a>,
 * which opens the selected interface.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_global.html#CCD_Global_Initialise
 * @see ccd_interface.html#CCD_Interface_Open
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Interface_1Open(JNIEnv *env, jobject obj)
{
	int retval;

	retval = CCD_Interface_Open();
	/* if an error occured throw an exception. */
/*	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Interface_Open"); */
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Interface_Close<br>
 * Signature: ()V<br>
 * Java Native Interface implementation of <a href="ccd_interface.html#CCD_Interface_Close">CCD_Interface_Close</a>,
 * which closes the selected interface.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_interface.html#CCD_Interface_Close
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Interface_1Close(JNIEnv *env, jobject obj)
{
	int retval;

	retval = CCD_Interface_Close();
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Interface_Close");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Interface_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for this module.
 * @return The current value of the error number for this module. A zero error number means an error has not occured.
 * @see ccd_interface.html#CCD_Interface_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Interface_1Get_1Error_1Number(JNIEnv *env,jobject obj)
{
	return CCD_Interface_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_pci.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_PCI_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for this module.
 * @return The current value of the error number for this module. A zero error number means an error has not occured.
 * @see ccd_interface.html#CCD_PCI_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1PCI_1Get_1Error_1Number(JNIEnv *env,jobject obj)
{
	return CCD_PCI_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_setup.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Startup<br>
 * Signature: (ILjava/lang/String;IILjava/lang/String;IILjava/lang/String;DIZZ)V<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Startup">CCD_Setup_Startup</a>,
 * a routine to setup the SDSU CCD COntroller for exposures. This routine translates the pci_filename_string, 
 * timing_filename_string and utility_filename_string parameters from Java Strings to C Strings.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_setup.html#CCD_Setup_Startup
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Startup(JNIEnv *env,jobject obj,
	jint pci_load_type, jstring pci_filename_string,
	jint timing_load_type, jint timing_application_number, jstring timing_filename_string,
	jint utility_load_type, jint utility_application_number, jstring utility_filename_string, 
	jdouble target_temperature, jint gain, jboolean gain_speed, jboolean idle)
{
	int retval;
	const char *pci_filename = NULL;
	const char *timing_filename = NULL;
	const char *utility_filename = NULL;

	/* Convert java strings to c null terminated strings.
	** If the java String is null the c filename should be null as well */
	if(pci_filename_string != NULL)
		pci_filename = (*env)->GetStringUTFChars(env,pci_filename_string,0);
	if(timing_filename_string != NULL)
		timing_filename = (*env)->GetStringUTFChars(env,timing_filename_string,0);
	if(utility_filename_string != NULL)
		utility_filename = (*env)->GetStringUTFChars(env,utility_filename_string,0);
	retval = CCD_Setup_Startup(pci_load_type,(char*)pci_filename,
		timing_load_type,timing_application_number,(char*)timing_filename,
		utility_load_type,utility_application_number,(char*)utility_filename,
		target_temperature,gain,gain_speed,idle);
	/* free any c strings allocated */
	if(pci_filename_string != NULL)
		(*env)->ReleaseStringUTFChars(env,pci_filename_string,pci_filename);
	if(timing_filename_string != NULL)
		(*env)->ReleaseStringUTFChars(env,timing_filename_string,timing_filename);
	if(utility_filename_string != NULL)
		(*env)->ReleaseStringUTFChars(env,utility_filename_string,utility_filename);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Startup");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Shutdown<br>
 * Signature: ()V<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Shutdown">CCD_Setup_Shutdown</a>,
 * a routine to shutdown the connection to a SDSU CCD Controller.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_setup.html#CCD_Setup_Shutdown
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Shutdown(JNIEnv *env,jobject obj)
{
	int retval;

	retval = CCD_Setup_Shutdown();
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Shutdown");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Dimensions<br>
 * Signature: (IIIIIII[Lngat/ccd/CCDLibrarySetupWindow;)V<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Dimensions">CCD_Setup_Dimensions</a>,
 * a routine to setup the SDSU CCD Controller dimensions.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * If the class cannot be found one of the following exception is thrown: 
 * ClassFormatError, ClassCircularityError, NoClassDefFoundError, OutOfMemoryError.
 * @see ccd_setup.html#CCD_Setup_Dimensions
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Dimensions(JNIEnv *env, jobject obj,
	jint ncols, jint nrows, jint nsbin, jint npbin, jint amplifier, jint deinterlace_setting, 
	jint window_flags, jobjectArray window_object_list)
{
	struct CCD_Setup_Window_Struct window_list[CCD_SETUP_WINDOW_COUNT];
	char error_string[256];
	int retval,i;
	jclass cls = NULL;
	jobject window_object = NULL;
	jmethodID get_x_start_method_id,get_y_start_method_id,get_x_end_method_id,get_y_end_method_id;
	jsize window_count;

/* convert window_object_list to window_list */
	/* check size of array */
	window_count = (*env)->GetArrayLength(env,(jarray)window_object_list);
	if(window_count != CCD_SETUP_WINDOW_COUNT)
	{
		/* N.B. This error occured in the JNI interface, not the librise_ccd - no error string set */
		sprintf(error_string,"CCD_Setup_Dimensions:window list has wrong number of elements(%d,%d).",
			window_count,CCD_SETUP_WINDOW_COUNT);
		CCDLibrary_Throw_Exception_String(env,obj,"CCD_Setup_Dimensions",error_string);
		return;
	}
/* get the class of CCDLibrarySetupWindow */
	cls = (*env)->FindClass(env,"ngat/ccd/CCDLibrarySetupWindow");
	/* if the class is null, one of the following exceptions occured:
	** ClassFormatError,ClassCircularityError,NoClassDefFoundError,OutOfMemoryError */
	if(cls == NULL)
		return;
/* get relevant method ids to call */
/* getXStart */
	get_x_start_method_id = (*env)->GetMethodID(env,cls,"getXStart","()I");
	if(get_x_start_method_id == NULL)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
/* getYStart */
	get_y_start_method_id = (*env)->GetMethodID(env,cls,"getYStart","()I");
	if(get_y_start_method_id == 0)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
/* getXEnd */
	get_x_end_method_id = (*env)->GetMethodID(env,cls,"getXEnd","()I");
	if(get_x_end_method_id == 0)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
/* getYEnd */
	get_y_end_method_id = (*env)->GetMethodID(env,cls,"getYEnd","()I");
	if(get_y_end_method_id == 0)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
/* for each window, get each position from the window_object_list into the window_list */
	for(i=0;i<CCD_SETUP_WINDOW_COUNT;i++)
	{
		/* Get the object at index i into window_object
		** throws ArrayIndexOutOfBoundsException: if index does not specify a valid index in the array. 
		** However, this should never occur, we have checked the size of the array earlier. */
		window_object = (*env)->GetObjectArrayElement(env,window_object_list,i);

		/* call the get method and put it into the list of window structures. */
		window_list[i].X_Start = (int)((*env)->CallIntMethod(env,window_object,get_x_start_method_id));
		window_list[i].Y_Start = (int)((*env)->CallIntMethod(env,window_object,get_y_start_method_id));
		window_list[i].X_End = (int)((*env)->CallIntMethod(env,window_object,get_x_end_method_id));
		window_list[i].Y_End = (int)((*env)->CallIntMethod(env,window_object,get_y_end_method_id));
	}
/* call dimension setup routine */
	retval = CCD_Setup_Dimensions(ncols,nrows,nsbin,npbin,amplifier,deinterlace_setting,window_flags,window_list);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Dimensions");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Hardware_Test<br>
 * Signature: (I)V<br>
 * Java Native Interface implementation of 
 * <a href="ccd_setup.html#CCD_Setup_Hardware_Test">CCD_Setup_Hardware_Test</a>,
 * which tests the hardware data links to the controller boards.
 * @see ccd_setup.html#CCD_Setup_Hardware_Test
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Hardware_1Test(JNIEnv *env,jobject obj,jint test_count)
{
	int retval;

	retval = CCD_Setup_Hardware_Test(test_count);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Hardware_Test");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Abort<br>
 * Signature: ()V<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Abort">CCD_Setup_Abort</a>,
 * which aborts a setup that is currently underway.
 * @see ccd_setup.html#CCD_Setup_Abort
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Abort(JNIEnv *env, jobject obj)
{
	CCD_Setup_Abort();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_NCols<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Get_NCols">CCD_Setup_Get_NCols</a>,
 * which gets the number of columns in the CCD chip.
 * @see ccd_setup.html#CCD_Setup_Get_NCols
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1NCols(JNIEnv *env, jobject obj)
{
	return (jint)CCD_Setup_Get_NCols();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_NRows<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Get_NRows">CCD_Setup_Get_NRows</a>,
 * which gets the number of rows in the CCD chip.
 * @see ccd_setup.html#CCD_Setup_Get_NRows
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1NRows(JNIEnv *env, jobject obj)
{
	return (jint)CCD_Setup_Get_NRows();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_NSBin<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Get_NSBin">CCD_Setup_Get_NSBin</a>,
 * which gets the column binning factor(serial binning).
 * @see ccd_setup.html#CCD_Setup_Get_NSBin
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1NSBin(JNIEnv *env,jobject obj)
{
	return (jint)CCD_Setup_Get_NSBin();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_NPBin<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of <a href="ccd_setup.html#CCD_Setup_Get_NPBin">CCD_Setup_Get_NPBin</a>,
 * which gets the row binning factor(parallel binning).
 * @see ccd_setup.html#CCD_Setup_Get_NPBin
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1NPBin(JNIEnv *env,jobject obj)
{
	return (jint)CCD_Setup_Get_NPBin();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Amplifier<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Setup_Get_Amplifier,
 * which gets the setup amplifier used to readout the image.
 * @see ccd_setup.html#CCD_Setup_Get_Amplifier
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Amplifier(JNIEnv *env, jobject obj)
{
	return (jint)CCD_Setup_Get_Amplifier();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_DeInterlace_Type<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of 
 * <a href="ccd_setup.html#CCD_Setup_Get_DeInterlace_Type">CCD_Setup_Get_DeInterlace_Type</a>,
 * which gets the type of de-interlacing done to the image.
 * @see ccd_setup.html#CCD_Setup_Get_DeInterlace_Type
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1DeInterlace_1Type(JNIEnv *env,jobject obj)
{
	return (jint)CCD_Setup_Get_DeInterlace_Type();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Gain<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of 
 * <a href="ccd_setup.html#CCD_Setup_Get_Gain">CCD_Setup_Get_Gain</a>,
 * which gets the current camera gain setting.
 * @see ccd_setup.html#CCD_Setup_Get_Gain
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Gain(JNIEnv *env,jobject obj)
{
	return (jint)CCD_Setup_Get_Gain();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Window_Flags<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of 
 * <a href="ccd_setup.html#CCD_Setup_Get_Window_Flags">CCD_Setup_Get_Window_Flags</a>,
 * which gets the current set of window flags (which windows are in use).
 * @see ccd_setup.html#CCD_Setup_Get_Window_Flags
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Window_1Flags(JNIEnv *env,jobject obj)
{
	return (jint)CCD_Setup_Get_Window_Flags();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Window<br>
 * Signature: (I)Lngat/ccd/CCDLibrarySetupWindow;<br>
 * Java Native Interface implementation of
 * <a href="ccd_setup.html#CCD_Setup_Get_Window">CCD_Setup_Get_Window</a>,
 * which gets the specified window parameters.
 * @see ccd_setup.html#CCD_Setup_Get_Window
 */
JNIEXPORT jobject JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Window(JNIEnv *env,jobject obj,jint window_index)
{
	struct CCD_Setup_Window_Struct window;
	jclass cls;
	jmethodID mid;
	jobject windowInstance;
	int retval;

	retval = CCD_Setup_Get_Window(window_index,&window);
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_Window");
		return NULL;
	}
/* get the class of CCDLibrarySetupWindow */
	cls = (*env)->FindClass(env,"ngat/ccd/CCDLibrarySetupWindow");
	/* if the class is null, one of the following exceptions occured:
	** ClassFormatError,ClassCircularityError,NoClassDefFoundError,OutOfMemoryError */
	if(cls == NULL)
		return NULL;
/* get CCDLibrarySetupWindow constructor */
	mid = (*env)->GetMethodID(env,cls,"<init>","(IIII)V");	
	if(mid == 0)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return NULL;
	}
/* call constructor */
	windowInstance = (*env)->NewObject(env,cls,mid,(jint)window.X_Start,(jint)window.Y_Start,
		(jint)window.X_End,(jint)window.Y_End);
	if(windowInstance == NULL)
	{
		/* One of the following exceptions has been thrown:
		** InstantiationException, OutOfMemoryError */
		return NULL;
	}
	return windowInstance;
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Window_Height<br>
 * Signature: (I)I<br>
 * @see ccd_setup.html#CCD_Setup_Get_Window_Height
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Window_1Height(JNIEnv *env,jobject obj,
										jint window_index)
{
	return (jint)CCD_Setup_Get_Window_Height((int)window_index);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Window_Width<br>
 * Signature: (I)I<br>
 * @see ccd_setup.html#CCD_Setup_Get_Window_Width
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Window_1Width(JNIEnv *env,jobject obj,
									       jint window_index)
{
	return (jint)CCD_Setup_Get_Window_Width((int)window_index);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Setup_Complete<br>
 * Signature: ()Z<br>
 * Java Native Interface routine to get whether a setup operation has been completed since the last reset.
 * The setup is complete when the DSP application have been loaded, the power is on and the dimension information has
 * been set.
 * @return True if the controller is setup for exposures, false if it is not.
 * @see ccd_setup.html#CCD_Setup_Get_Setup_Complete
 * @see ccd_setup.html#CCD_Setup_Startup
 * @see ccd_setup.html#CCD_Setup_Dimensions
 */
JNIEXPORT jboolean JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Setup_1Complete(JNIEnv *env,jobject obj)
{
	return (jboolean)CCD_Setup_Get_Setup_Complete();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Setup_In_Progress<br>
 * Signature: ()Z<br>
 * Java Native Interface routine to get whether a setup operation is currently underway. A setup
 * operation is currently underway if the setup routine is executing.
 * @return True if a setup operation is underway, false if it is not.
 * @see ccd_setup.html#CCD_Setup_Get_Setup_In_Progress
 * @see ccd_setup.html#CCD_Setup_Startup
 * @see ccd_setup.html#CCD_Setup_Dimensions
 */
JNIEXPORT jboolean JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Setup_1In_1Progress(JNIEnv *env, jobject obj)
{
	return (jboolean)CCD_Setup_Get_Setup_In_Progress();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_High_Voltage_Analogue_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Setup_Get_High_Voltage_Analogue_ADU,
 * which gets the ADU count of the current high voltage supply on the SDSU boards.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_setup.html#CCD_Setup_Get_High_Voltage_Analogue_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1High_1Voltage_1Analogue_1ADU(JNIEnv *env, jobject obj)
{
	int retval,v_adu = -1;

	retval = CCD_Setup_Get_High_Voltage_Analogue_ADU(&v_adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_High_Voltage_Analogue_ADU");
		return ((jint)v_adu);
	}
	return ((jint)v_adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Low_Voltage_Analogue_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Setup_Get_Low_Voltage_Analogue_ADU,
 * which gets the ADU count of the current low voltage supply on the SDSU boards.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_setup.html#CCD_Setup_Get_Low_Voltage_Analogue_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Low_1Voltage_1Analogue_1ADU(JNIEnv *env, jobject obj)
{
	int retval,v_adu = -1;

	retval = CCD_Setup_Get_Low_Voltage_Analogue_ADU(&v_adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_Low_Voltage_Analogue_ADU");
		return ((jint)v_adu);
	}
	return ((jint)v_adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU,
 * which gets the ADU count of the current negative low voltage supply on the SDSU boards.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_setup.html#CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Minus_1Low_1Voltage_1Analogue_1ADU(JNIEnv *env, 
												    jobject obj)
{
	int retval,v_adu = -1;

	retval = CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU(&v_adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU");
		return ((jint)v_adu);
	}
	return ((jint)v_adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Vacuum_Gauge_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Setup_Get_Vacuum_Gauge_ADU,
 * which gets the ADU count of the dewar vacuum gauge (if fitted).
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_setup.html#CCD_Setup_Get_Vacuum_Gauge_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Vacuum_1Gauge_1ADU(JNIEnv *env,jobject obj)
{
	int retval,adu = -1;

	retval = CCD_Setup_Get_Vacuum_Gauge_ADU(&adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_Vacuum_Gauge_ADU");
		return ((jint)adu);
	}
	return ((jint)adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Vacuum_Gauge_MBar<br>
 * Signature: ()D<br>
 * Java Native Interface implementation of CCD_Setup_Get_Vacuum_Gauge_MBar,
 * which gets pressure in the dewar, using the vacuum gauge (if fitted).
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @return The current pressure in the dewar, in millibar. If an error occurs an exception is thrown.
 * @see ccd_setup.html#CCD_Setup_Get_Vacuum_Gauge_MBar
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jdouble JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Vacuum_1Gauge_1MBar(JNIEnv *env,jobject obj)
{
	int retval;
	double mbar;

	retval = CCD_Setup_Get_Vacuum_Gauge_MBar(&mbar);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Setup_Get_Vacuum_Gauge_MBar");
		return ((jdouble)mbar);
	}
	return ((jdouble)mbar);
}


/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Setup_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for the ccd_setup part of the library.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @return The current error number of ccd_setup. A zero error number means an error has not occured.
 * @see ccd_setup.html#CCD_Setup_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Setup_1Get_1Error_1Number(JNIEnv *env, jobject obj)
{
	return CCD_Setup_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_temperature.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Temperature_Get<br>
 * Signature: (Lngat/ccd/CCDLibraryDouble;)V<br>
 * Java Native Interface implementation of <a href="ccd_temperature.html#CCD_Temperature_Get">CCD_Temperature_Get</a>,
 * which gets the current temperature of the CCD. The temperature is returned using the temperatureObj, which is of
 * class CCDLibraryDouble. The routine calls it's setValue method to set the temperature.
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_temperature.html#CCD_Temperature_Get
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Temperature_1Get(JNIEnv *env, jobject obj, jobject temperatureObj)
{
	int retval;
	double temperature;
	jclass cls;
	jmethodID mid;

	retval = CCD_Temperature_Get(&temperature);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Temperature_Get");
		return;
	}
	/* we are going to return the temperature in a CCDLibraryDouble - we need to call a method to do this */
	/* get the class of the object passed in */
	cls = (*env)->GetObjectClass(env,temperatureObj);
	/* get the method id of the setValue method in this class */
	mid = (*env)->GetMethodID(env,cls,"setValue","(D)V");
	/* did we find the method id? */
	if (mid == 0)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
	/* call the method with the temperature as a parameter to set the objects value */
	(*env)->CallVoidMethod(env,temperatureObj,mid,temperature);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Temperature_Set<br>
 * Signature: (D)V<br>
 * Java Native Interface implementation of <a href="ccd_temperature.html#CCD_Temperature_Set">CCD_Temperature_Set</a>,
 * which sets the temperature of the CCD. 
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @see ccd_temperature.html#CCD_Temperature_Set
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Temperature_1Set(JNIEnv *env, jobject obj, jdouble temperature)
{
	int retval;

	retval = CCD_Temperature_Set(temperature);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
		CCDLibrary_Throw_Exception(env,obj,"CCD_Temperature_Set");
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Temperature_Get_Utility_Board_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Temperature_Get_Utility_Board_ADU,
 * which gets the Analogue to Digital counts from the utility board mounted temperature sensor. 
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_temperature.html#CCD_Temperature_Get_Utility_Board_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Temperature_1Get_1Utility_1Board_1ADU(JNIEnv *env, jobject obj)
{
	int retval,adu = -1;

	retval = CCD_Temperature_Get_Utility_Board_ADU(&adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Temperature_Get_Utility_Board_ADU");
		return ((jint)adu);
	}
	return ((jint)adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Temperature_Get_Heater_ADU<br>
 * Signature: ()I<br>
 * Java Native Interface implementation of CCD_Temperature_Get_Heater_ADU,
 * which gets the current heater Analogue to Digital counts from the utility board. 
 * If an error occurs a CCDLibraryNativeException is thrown.
 * @return The current ADU count is returned. If an error occurs an exception is thrown, and this routine
 * 	returns -1.
 * @see ccd_temperature.html#CCD_Temperature_Get_Heater_ADU
 * @see #CCDLibrary_Throw_Exception
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Temperature_1Get_1Heater_1ADU(JNIEnv *env,jobject obj)
{
	int retval,adu = -1;

	retval = CCD_Temperature_Get_Heater_ADU(&adu);
	/* if an error occured throw an exception. */
	if(retval == FALSE)
	{
		CCDLibrary_Throw_Exception(env,obj,"CCD_Temperature_Get_Heater_ADU");
		return ((jint)adu);
	}
	return ((jint)adu);
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Temperature_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for the ccd_temperature part of the library.
 * @return The current error number of ccd_temperature. A zero error number means an error has not occured.
 * @see ccd_temperature.html#CCD_Temperature_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Temperature_1Get_1Error_1Number(JNIEnv *env, jobject obj)
{
	return CCD_Temperature_Get_Error_Number();
}

/* ------------------------------------------------------------------------------
** 		ccd_text.c
** ------------------------------------------------------------------------------ */
/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Text_Get_Error_Number<br>
 * Signature: ()I<br>
 * Java Native Interface routine to get the error number for this module.
 * @return The current value of the error number for this module. A zero error number means an error has not occured.
 * @see ccd_interface.html#CCD_Text_Get_Error_Number
 */
JNIEXPORT jint JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Text_1Get_1Error_1Number(JNIEnv *env,jobject obj)
{
	return CCD_Text_Get_Error_Number();
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    CCD_Text_Set_Print_Level<br>
 * Signature: (I)V<br>
 * Java Native Interface implementation of 
 * <a href="ccd_text.html#CCD_Text_Set_Print_Level">CCD_Text_Set_Print_Level</a>,
 * which sets the amount of information displayed when the text interface device is enabled. 
 * @see ccd_text.html#CCD_Text_Set_Print_Level
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_CCD_1Text_1Set_1Print_1Level(JNIEnv *env, jobject obj, jint level)
{
	CCD_Text_Set_Print_Level(level);
}

/* ------------------------------------------------------------------------------
** 		CCDLibrary C layer initialisation
** ------------------------------------------------------------------------------ */
/**
 * This routine gets called when the native library is loaded. We use this routine
 * to get a copy of the JavaVM pointer of the JVM we are running in. This is used to
 * get the correct per-thread JNIEnv context pointer in CCDLibrary_Log_Handler.
 * @see #java_vm
 * @see #CCDLibrary_Log_Handler
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	java_vm = vm;
	return JNI_VERSION_1_2;
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    initialiseLoggerReference<br>
 * Signature: (Lngat/util/logging/Logger;)V<br>
 * Java Native Interface implementation CCDLibrary's initialiseLoggerReference.
 * This takes the supplied logger object reference and stores it in the logger variable as a global reference.
 * The log method ID is also retrieved and stored.
 * @param l The CCDLibrary's "ngat.ccd.CCDLibrary" logger.
 * @see #logger
 * @see #log_method_id
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_initialiseLoggerReference(JNIEnv *env,jobject obj,jobject l)
{
	jclass cls = NULL;

/* save logger instance */
	logger = (*env)->NewGlobalRef(env,l);
/* get the ngat.util.logging.Logger class */
	cls = (*env)->FindClass(env,"ngat/util/logging/Logger");
	/* if the class is null, one of the following exceptions occured:
	** ClassFormatError,ClassCircularityError,NoClassDefFoundError,OutOfMemoryError */
	if(cls == NULL)
		return;
/* get relevant method id to call */
/* log(int level,java/lang/String message) */
	log_method_id = (*env)->GetMethodID(env,cls,"log","(ILjava/lang/String;)V");
	if(log_method_id == NULL)
	{
		/* One of the following exceptions has been thrown:
		** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
		return;
	}
}

/**
 * Class:     ngat_rise_ccd_CCDLibrary<br>
 * Method:    finaliseLoggerReference<br>
 * Signature: ()V<br>
 * This native method is called from CCDLibrary's finaliser method. It removes the global reference to
 * logger.
 * @see #logger
 */
JNIEXPORT void JNICALL Java_ngat_rise_ccd_CCDLibrary_finaliseLoggerReference(JNIEnv *env, jobject obj)
{
	(*env)->DeleteGlobalRef(env,logger);
}

/* ------------------------------------------------------------------------------
** 		Internal routines
** ------------------------------------------------------------------------------ */
/**
 * This routine throws an exception. The error generated is from the error codes in librise_ccd, it assumes
 * another routine has generated an error and this routine packs this error into an exception to return
 * to the Java code, using CCDLibrary_Throw_Exception_String. The total length of the error string should
 * not be longer than CCD_LIBRARY_ERROR_STRING_LENGTH. A new line is added to the start of the error string,
 * so that the error string returned from librise_ccd is formatted properly.
 * @param env The JNI environment pointer.
 * @param function_name The name of the function in which this exception is being generated for.
 * @param obj The instance of CCDLibrary that threw the error.
 * @see ccd_global.html#CCD_Global_Error_String
 * @see #CCDLibrary_Throw_Exception_String
 * @see #CCD_LIBRARY_ERROR_STRING_LENGTH
 */
static void CCDLibrary_Throw_Exception(JNIEnv *env,jobject obj,char *function_name)
{
	char error_string[CCD_LIBRARY_ERROR_STRING_LENGTH];

	strcpy(error_string,"\n");
	CCD_Global_Error_String(error_string+strlen(error_string));
	CCDLibrary_Throw_Exception_String(env,obj,function_name,error_string);
}

/**
 * This routine throws an exception of class ngat/ccd/CCDLibraryNativeException. This is used to report
 * all librise_ccd error messages back to the Java layer.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that threw the error.
 * @param function_name The name of the function in which this exception is being generated for.
 * @param error_string The string to pass to the constructor of the exception.
 */
static void CCDLibrary_Throw_Exception_String(JNIEnv *env,jobject obj,char *function_name,char *error_string)
{
	jclass exception_class = NULL;
	jobject exception_instance = NULL;
	jstring error_jstring = NULL;
	jmethodID mid;
	int retval;

	exception_class = (*env)->FindClass(env,"ngat/ccd/CCDLibraryNativeException");
	if(exception_class != NULL)
	{
	/* get CCDLibraryNativeException constructor */
		mid = (*env)->GetMethodID(env,exception_class,"<init>","(Ljava/lang/String;Lngat/ccd/CCDLibrary;)V");
		if(mid == 0)
		{
			/* One of the following exceptions has been thrown:
			** NoSuchMethodError, ExceptionInInitializerError, OutOfMemoryError */
			fprintf(stderr,"CCDLibrary_Throw_Exception_String:GetMethodID failed:%s:%s\n",function_name,
				error_string);
			return;
		}
	/* convert error_string to JString */
		error_jstring = (*env)->NewStringUTF(env,error_string);
	/* call constructor */
		exception_instance = (*env)->NewObject(env,exception_class,mid,error_jstring,obj);
		if(exception_instance == NULL)
		{
			/* One of the following exceptions has been thrown:
			** InstantiationException, OutOfMemoryError */
			fprintf(stderr,"CCDLibrary_Throw_Exception_String:NewObject failed %s:%s\n",
				function_name,error_string);
			return;
		}
	/* throw instance */
		retval = (*env)->Throw(env,(jthrowable)exception_instance);
		if(retval !=0)
		{
			fprintf(stderr,"CCDLibrary_Throw_Exception_String:Throw failed %d:%s:%s\n",retval,
				function_name,error_string);
		}
	}
	else
	{
		fprintf(stderr,"CCDLibrary_Throw_Exception_String:FindClass failed:%s:%s\n",function_name,
			error_string);
	}
}

/**
 * librise_ccd Log Handler for the Java layer interface. This calls the ngat.ccd.CCDLibrary logger's 
 * log(int level,String message) method with the parameters supplied to this routine.
 * If the logger instance is NULL, or the log_method_id is NULL the call is not made.
 * Otherwise, A java.lang.String instance is constructed from the string parameter,
 * and the JNI CallVoidMEthod routine called to call log().
 * @param level The log level of the message.
 * @param string The message to log.
 * @see #java_vm
 * @see #logger
 * @see #log_method_id
 */
static void CCDLibrary_Log_Handler(int level,char *string)
{
	JNIEnv *env = NULL;
	jstring java_string = NULL;

	if(logger == NULL)
	{
		fprintf(stderr,"CCDLibrary_Log_Handler:logger was NULL (%d,%s).\n",level,string);
		return;
	}
	if(log_method_id == NULL)
	{
		fprintf(stderr,"CCDLibrary_Log_Handler:log_method_id was NULL (%d,%s).\n",level,string);
		return;
	}
	if(java_vm == NULL)
	{
		fprintf(stderr,"CCDLibrary_Log_Handler:java_vm was NULL (%d,%s).\n",level,string);
		return;
	}
/* get java env for this thread */
	(*java_vm)->AttachCurrentThread(java_vm,(void**)&env,NULL);
	if(env == NULL)
	{
		fprintf(stderr,"CCDLibrary_Log_Handler:env was NULL (%d,%s).\n",level,string);
		return;
	}
	if(string == NULL)
	{
		fprintf(stderr,"CCDLibrary_Log_Handler:string (%d) was NULL.\n",level);
		return;
	}
/* convert C to Java String */
	java_string = (*env)->NewStringUTF(env,string);
/* call log method on logger instance */
	(*env)->CallVoidMethod(env,logger,log_method_id,(jint)level,java_string);
}

/**
 * This routine creates a re-allocatable c list of strings, from a jobject of class java.util.List
 * containing java.lang.String s. Note the c list of strings will need freeing in the same JNI routine.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @param java_list A jobject which implements the interface java.util.List. This list should contain
 *        jobjects that are jstrings. The string's characters are extracted and put into c_list
 * @param jni_jstring_list A pointer to a list a jstrings. This list is allocated by this routine,
 *        and filled with jstring's got from java_list. Each jstring in the returned list has an equivalent 
 *        C string in the c_list. This list is created to allow us to free the c_list correctly.
 * @param jni_jstring_count The address of an integer which, on exit, contains the number of jstrings in the 
 *       jni_jstring_list.
 * @param c_list The address of a reallocatable list of character pointers (char *). This will contain
 * 	a list a c strings on exit.
 * @param c_list_count The address of an integer to fill with the number of strings in the list.
 * @return True if the routine is successfull, false if it fails. If FALSE is returned, a JNI exception is thrown.
 * @see #CCDLibrary_Throw_Exception_String
 * @see #CCDLibrary_Java_String_List_Free
 */
static int CCDLibrary_Java_String_List_To_C_List(JNIEnv *env,jobject obj,jobject java_list,
						 jstring **jni_jstring_list,int *jni_jstring_count,
						 char ***c_list,int *c_list_count)
{
	jclass java_list_class = NULL;
	jmethodID list_size_mid = NULL;
	jmethodID list_get_mid = NULL;
	jstring java_string = NULL;
	int index;

	if(c_list == NULL)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List","C List was NULL.");
		return FALSE;
	}
	if(c_list_count == NULL)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
						  "C List count was NULL.");
		return FALSE;
	}
	/* get class and method id's */
	java_list_class = (*env)->GetObjectClass(env,java_list);
	list_size_mid = (*env)->GetMethodID(env,java_list_class, "size","()I");
	if(list_size_mid == 0)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
						  "List size Method ID was 0.");
		return FALSE;
	}
	list_get_mid = (*env)->GetMethodID(env,java_list_class, "get","(I)Ljava/lang/Object;");
	if(list_get_mid == 0)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
						  "List get Method ID was 0.");
		return FALSE;
	}

	/* get count of elements in java_list */
	(*c_list_count) = (int)((*env)->CallIntMethod(env,java_list,list_size_mid));
	(*jni_jstring_count) = (*c_list_count);
	/* allocate c_list to match number of elements in java_list */
	(*c_list) = (char **)malloc((*c_list_count)*sizeof(char*));
	if((*c_list) == NULL)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
					   "Memory Allocation Error(c_list).");
		return FALSE;
	}

	(*jni_jstring_list) = (jstring *)malloc((*jni_jstring_count)*sizeof(char*));
	if((*jni_jstring_list) == NULL)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
					   "Memory Allocation Error(jni_jstring_list).");
		return FALSE;
	}
	/* enter loop over elements in java_list */
	for(index = 0; index < (*c_list_count); index ++)
	{
		/* get the jstring at this index in the list */
		java_string = (jstring)((*env)->CallObjectMethod(env,java_list,list_get_mid,index));
		if(java_string == NULL)
		{
			/* free data elements already allocated. */
			CCDLibrary_Java_String_List_Free(env,obj,(*jni_jstring_list),(*jni_jstring_count),
							 (*c_list),(*c_list_count));
			fprintf(stdout,"::: THIS FAR 3:::\n"); fflush(stdout);
			(*jni_jstring_list) = NULL;
			(*jni_jstring_count) = 0;
			(*c_list) = NULL;
			(*c_list_count) = 0;
			CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
						   "Java String was NULL.");
			return FALSE;
		}
		(*jni_jstring_list)[index] = java_string;
		/* Get the filename from a java string to a c null terminated string */
		(*c_list)[index] = (char*)((*env)->GetStringUTFChars(env,java_string,0));
		/* fprintf(stdout,"::: c_list[%d] %s :::\n",index,(*c_list)[index]); fflush(stdout); */
		if((*c_list)[index] == NULL)
		{
			/* free data elements already allocated. */
			CCDLibrary_Java_String_List_Free(env,obj,(*jni_jstring_list),(*jni_jstring_count),
							 (*c_list),(*c_list_count));
			(*jni_jstring_list) = NULL;
			(*jni_jstring_count) = 0;
			(*c_list) = NULL;
			(*c_list_count) = 0;
			CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_To_C_List",
						   "C String was NULL.");
			return FALSE;
		}
	}/* end for on list */
	return TRUE;
}

/**
 * Routine to free data created by CCDLibrary_Java_String_List_To_C_List. This routine copes with
 * list in which half the elements have not been converted, i.e. it can be used to free during error recovery.
 * The passed in lists should have the same number of elements.
 * @param env The JNI environment pointer.
 * @param obj The instance of CCDLibrary that called this routine.
 * @param jni_jstring_list A list of jstrings. Each jstring in the list has an equivalent 
 *        C string in the c_list.
 * @param jni_jstring_count The number of jstrings in the jni_jstring_list.
 * @param c_list The list of character pointers (char *). This contains a list a c strings to free.
 * @param c_list_count The number of strings in the list.
 * @return True if the routine is successfull, false if it fails. If FALSE is returned, a JNI exception is thrown.
 * @see #CCDLibrary_Java_String_List_To_C_List
 * @see #CCDLibrary_Throw_Exception_String
 */
static int CCDLibrary_Java_String_List_Free(JNIEnv *env,jobject obj,
					    jstring *jni_jstring_list,int jni_jstring_count,
					    char **c_list,int c_list_count)
{
	int index;

	if(jni_jstring_count != c_list_count)
	{
		CCDLibrary_Throw_Exception_String(env,obj,"CCDLibrary_Java_String_List_Free",
					   "jni_jstring_count and c_list_count not equal.");
		return FALSE;
	}
	for(index = 0;index < jni_jstring_count;index ++)
	{
		/* This if test allows this routine to free half completed java string lists,
		** where an error occured during convertion and we only want to free what was converted.*/
		if((jni_jstring_list[index] != NULL) && (c_list[index] != NULL))
		{
			(*env)->ReleaseStringUTFChars(env,jni_jstring_list[index],c_list[index]);
		}
	}/* end for */
	if(jni_jstring_list != NULL)
		free(jni_jstring_list);
	if(c_list != NULL)
		free(c_list);
	return TRUE;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:06:52  cjm
** Initial revision
**
** Revision 0.32  2006/05/17 18:08:21  cjm
** Fixed assignment problems and unused variables.
**
** Revision 0.31  2006/05/16 14:14:10  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.30  2004/05/16 14:28:18  cjm
** Re-wrote abort code.
**
** Revision 0.29  2003/06/06 12:36:01  cjm
** Added setup routine to get amplifier.
**
** Revision 0.28  2003/03/26 15:44:48  cjm
** Added windowing JNI layer.
**
** Revision 0.27  2003/01/28 16:30:06  cjm
** Re-written filter wheel code to use ordinary motors, not
** stepper motors.
**
** Revision 0.26  2002/12/16 16:56:20  cjm
** Changed some CCD_DSP_ routines to CCD_Exposure routines.
** Aborts can now throw an exception.
**
** Revision 0.25  2002/12/03 17:46:43  cjm
** Added CCD_Setup_Get_Vacuum_Gauge_ADU and CCD_Setup_Get_Vacuum_Gauge_MBar
** native implementations.
**
** Revision 0.24  2002/11/07 19:13:39  cjm
** Changes to make library work with SDSU version 1.7 DSP code.
**
** Revision 0.23  2001/08/07 08:16:39  cjm
** Deleted spurious print.
**
** Revision 0.22  2001/07/13 10:09:54  cjm
** Added CCD_Temperature_Get_Heater_ADU.
**
** Revision 0.21  2001/06/04 14:43:15  cjm
** Added flushing of stdout compilation messages.
** Added extra CCDLibrary_Log_Handler error checking.
**
** Revision 0.20  2001/04/05 17:02:08  cjm
** New logging code, and more native implementations for getting
** error codes.
**
** Revision 0.19  2001/03/01 12:16:43  cjm
** Added CCD_Filter_Wheel_Set_De_Bounce_Step_Count.
**
** Revision 0.18  2001/02/09 18:29:14  cjm
** Added CCD_Filter_Wheel_Set_Milliseconds_Per_Step and fixed
** CCD_Filter_Wheel_Get_Status.
**
** Revision 0.17  2000/12/21 12:11:21  cjm
** Added CCD_DSP_Get_Filter_Wheel_Status JNI interface.
**
** Revision 0.16  2000/12/19 17:52:47  cjm
** New filter wheel code.
**
** Revision 0.15  2000/06/26 16:56:40  cjm
** Changed  CCDLibrary_Throw_Exception so that exception messages start with a newline
** for better formatting.
**
** Revision 0.14  2000/06/20 12:53:07  cjm
** CCD_DSP_Command_Sex now automatically calls CCD_DSP_Command_RDI.
**
** Revision 0.13  2000/06/13 17:14:13  cjm
** Changes to make Ccs agree with voodoo.
**
** Revision 0.12  2000/05/26 09:56:03  cjm
** Added CCD_Setup_Get_Window implementation.
**
** Revision 0.11  2000/05/25 08:54:31  cjm
** Added CCD_Setup_Get_Gain implementation.
**
** Revision 0.10  2000/03/09 16:34:53  cjm
** Added CCD_DSP_Command_Read_Exposure_Time implementation.
**
** Revision 0.9  2000/03/07 17:23:29  cjm
** Added pause exposure and resume exposure commands.
**
** Revision 0.8  2000/03/03 10:35:25  cjm
** Added CCD_DSP_Abort method implementation.
**
** Revision 0.7  2000/03/02 17:17:30  cjm
** Added implementation of CCD_Setup_Hardware_Test.
**
** Revision 0.6  2000/02/28 19:13:01  cjm
** Backup.
**
** Revision 0.5  2000/02/14 19:05:41  cjm
** Added JNI implementations for various setup methods.
**
** Revision 0.4  2000/02/03 16:55:01  cjm
** Fixed GetMethodID in CCD_Setup_Dimensions.
**
** Revision 0.3  2000/02/02 16:12:28  cjm
** Fixed GetArrayLength function pointer.
**
** Revision 0.2  2000/02/02 15:43:19  cjm
** Added filter wheel and windowing JNI code.
**
** Revision 0.1  2000/01/25 14:57:27  cjm
** initial revision (PCI version).
**
** Revision 0.12  1999/09/20 14:39:25  cjm
** Changed due to librise_ccd native routines throwing CCDLibraryNativeException when errors occur.
**
** Revision 0.11  1999/09/15 12:36:59  cjm
** Changed filename and function names to reflect the fact that CCDLibrary is now in the ngat.ccd package.
**
** Revision 0.10  1999/09/09 10:10:40  cjm
** Added JNI implementation of CCD_Exposure_Bias routine.
**
** Revision 0.9  1999/07/09 12:08:47  dev
** Changes to the JNI interface.
**
** Revision 0.8  1999/06/24 12:38:55  dev
** "Backup"
**
** Revision 0.7  1999/05/28 09:54:16  dev
** "Name
**
** Revision 0.6  1999/05/20 16:37:56  dev
** "Backup"
**
** Revision 0.5  1999/03/25 14:01:59  dev
** Backup
**
** Revision 0.4  1999/03/08 12:20:38  dev
** Backup
**
** Revision 0.3  1999/03/05 14:42:00  dev
** Backup
**
** Revision 0.2  1999/02/23 11:02:25  dev
** backup/transfer to ltccd1.
**
** Revision 0.1  1999/01/21 15:37:52  dev
** initial revision
**
*/
