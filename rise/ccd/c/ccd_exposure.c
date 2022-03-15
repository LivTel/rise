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
/* ccd_exposure.c
** low level ccd library
** $Header: /space/home/eng/cjm/cvs/rise/ccd/c/ccd_exposure.c,v 1.5 2022-03-15 16:14:12 cjm Exp $
*/
/**
 * ccd_exposure.c contains routines for performing an exposure with an Andor camera. There is a
 * routine that does the whole job in one go, or several routines can be called to do parts of an exposure.
 * An exposure can be paused and resumed, or it can be stopped or aborted.
 * @author Chris Mottram
 * @version $Revision: 1.5 $
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
/**
 * This hash define is needed before including unistd.h to get usleep.
 */
#define _XOPEN_SOURCE 500
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#ifndef _POSIX_TIMERS
#include <sys/time.h> 
#endif
#include <time.h>
#include "log_udp.h"
#include "ccd_exposure.h"
#include "ccd_setup.h"
#ifdef CFITSIO
#include "fitsio.h"
#endif
#ifdef SLALIB
#include "slalib.h"
#endif /* SLALIB */
#ifdef NGATASTRO
#include "ngat_astro.h"
#include "ngat_astro_mjd.h"
#endif /* NGATASTRO */
#include "atmcdLXd.h"

/* hash definitions */
/**
 * Number used to determine how long we keep getting the same number of readout pixels
 * returned before we timeout. This number depeends on the sleep in the loop.
 * This is by default 1 second, which makes this number the number of seconds
 * we don't read out more pixels before we time out.
 */
#define EXPOSURE_READ_TIMEOUT                           (0x5)
/**
 * The number of milliseconds before the controller stops exposing and starts reading out,
 * that we switch the exposure status from EXPOSING to READOUT. This is done early as we 
 * only check the HSTR status every second, and RDM/TDL/RET/WRM check the exposure status
 * to determine whether it is safe. It is not safe to call RDM/TDl/RET/WRM when
 * the HSTR is in readout mode, so we change exposure state early. Note the value
 * of this define should be greater than the sleep in the exposure loop.
 */
#define EXPOSURE_DEFAULT_READOUT_REMAINING_TIME       	(1500)
/**
 * The default amount of time before we are due to start an exposure, that a CLEAR_ARRAY command should be sent to
 * the controller. This time is in seconds, and must be greater than the time the CLEAR_ARRAY command takes to
 * clock all accumulated charge off the CCD (approx 5 seconds for a 2kx2k EEV42-40).
 */
#define EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME	(10)
/**
 * The default amount of time, in milliseconds, before the desired start of exposure that we should send the
 * START_EXPOSURE command, to allow for transmission delay.
 */
#define EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME	(2)

/* structure */
/**
 * Structure used to hold local data to ccd_exposure.
 * <dl>
 * <dt>Exposure_Status</dt> <dd>Whether an operation is being performed to CLEAR, EXPOSE or READOUT the CCD.</dd>
 * <dt>Start_Exposure_Clear_Time</dt> <dd>The amount of time before we are due to start an exposure, 
 * 	that a CLEAR_ARRAY command should be sent to the controller. This time is in seconds, 
 * 	and must be greater than the time the CLEAR_ARRAY command takes to clock all accumulated charge off the CCD 
 * 	(approx 5 seconds for a 2kx2k EEV42-40).</dd>
 * <dt>Start_Exposure_Offset_Time</dt> <dd>The amount of time, in milliseconds, before the desired start of 
 * 	exposure that we should send the START_EXPOSURE command, to allow for transmission delay.</dd>
 * <dt>Readout_Remaining_Time</dt> <dd>Amount of time, in milleseconds,
 * 	remaining for an exposure when we change status to READOUT, to stop RDM/TDL/WRMs affecting the readout.</dd>
 * <dt>Exposure_Length</dt> <dd>The last exposure length to be set.</dd>
 * <dt>Exposure_Start_Time</dt> <dd>The time stamp when the START_EXPOSURE command was sent to the controller.</dd>
 * <dt>Abort</dt> <dd>Whether it has been requested to abort the current operation.</dd>
 * </dl>
 * @see ccd_exposure.html#CCD_EXPOSURE_STATUS
 */
struct Exposure_Struct
{
	enum CCD_EXPOSURE_STATUS Exposure_Status;
	int Start_Exposure_Clear_Time;
	int Start_Exposure_Offset_Time;
	int Readout_Remaining_Time;
	int Exposure_Length;
	struct timespec Exposure_Start_Time;
	volatile int Abort; /* This is volatile as a different thread may change this variable. */
};

/* external variables */

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: ccd_exposure.c,v 1.5 2022-03-15 16:14:12 cjm Exp $";

/**
 * Variable holding error code of last operation performed by ccd_exposure.
 */
static int Exposure_Error_Number = 0;
/**
 * Local variable holding description of the last error that occured.
 */
static char Exposure_Error_String[CCD_GLOBAL_ERROR_STRING_LENGTH] = "";
/**
 * Data holding the current status of ccd_exposure. This is statically initialised to the following:
 * <dl>
 * <dt>Exposure_Status</dt> <dd>CCD_EXPOSURE_STATUS_NONE</dd>
 * <dt>Start_Exposure_Clear_Time</dt> <dd>EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME</dd>
 * <dt>Start_Exposure_Offset_Time</dt> <dd>EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME</dd>
 * <dt>Readout_Remaining_Time</dt> <dd>EXPOSURE_DEFAULT_READOUT_REMAINING_TIME</dd>
 * <dt>Exposure_Length</dt> <dd>0</dd>
 * <dt>Exposure_Start_Time</dt> <dd>{0L,0L}</dd>
 * <dt>Abort</dt> <dd>FALSE</dd>
 * </dl>
 * @see #Exposure_Struct
 * @see #CCD_EXPOSURE_STATUS
 * @see #EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME
 * @see #EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME
 * @see #EXPOSURE_DEFAULT_READOUT_REMAINING_TIME
 */
static struct Exposure_Struct Exposure_Data = 
{
	CCD_EXPOSURE_STATUS_NONE,
	EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME,
	EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME,
	EXPOSURE_DEFAULT_READOUT_REMAINING_TIME,
	0,
	{0L,0L},
	FALSE
};

/* internal functions */
static int Exposure_Expose_Post_Readout_Full_Frame(unsigned long *exposure_data,char *filename);
/* static int Exposure_Save(char *filename,unsigned short *exposure_data,int ncols,int nrows); */
static int Exposure_Save(char *filename,unsigned long *exposure_data,int ncols,int nrows); 
static void Exposure_TimeSpec_To_Date_String(struct timespec time,char *time_string);
static void Exposure_TimeSpec_To_Date_Obs_String(struct timespec time,char *time_string);
static void Exposure_TimeSpec_To_UtStart_String(struct timespec time,char *time_string);
static int Exposure_TimeSpec_To_Mjd(struct timespec time,int leap_second_correction,double *mjd);
static int Exposure_Expose_Delete_Fits_Images(char **filename_list,int filename_count);
static int fexist(char *filename);

/* external functions */
/**
 * This routine sets up ccd_exposure internal variables.
 * It should be called at startup.
 * @see #Exposure_Data
 */
void CCD_Exposure_Initialise(void)
{
	Exposure_Error_Number = 0;
	Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
	Exposure_Data.Start_Exposure_Clear_Time = EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME;
	Exposure_Data.Start_Exposure_Offset_Time = EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME;
	Exposure_Data.Readout_Remaining_Time = EXPOSURE_DEFAULT_READOUT_REMAINING_TIME;
	Exposure_Data.Exposure_Length = 0;
	Exposure_Data.Exposure_Start_Time.tv_sec = 0;
	Exposure_Data.Exposure_Start_Time.tv_nsec = 0;
	Exposure_Data.Abort = FALSE;
/* print some compile time information to stdout */
	fprintf(stdout,"CCD_Exposure_Initialise:%s.\n",rcsid);
#ifdef CCD_EXPOSURE_BYTE_SWAP
	fprintf(stdout,"CCD_Exposure_Initialise:Image data is byte swapped by the application.\n");
#else
	fprintf(stdout,"CCD_Exposure_Initialise:Image data is byte swapped by the device driver.\n");
#endif
#ifdef CFITSIO
	fprintf(stdout,"CCD_Exposure_Initialise:Using CFITSIO.\n");
#else
	fprintf(stdout,"CCD_Exposure_Initialise:NOT Using CFITSIO.\n");
#endif
}

/**
 * Routine to perform an exposure.
 * <ul>
 * <li>It checks to ensure CCD Setup has been successfully completed using CCD_Setup_Get_Setup_Complete.
 * <li>The controller is told whether to open the shutter or not during the exposure, depending on the value
 * 	of the open_shutter parameter.
 * <li>The length of exposure is sent to the controller using CCD_DSP_Command_SET.
 * <li>A sleep is executed until it is nearly (Exposure_Data.Start_Exposure_Clear_Time) time to start the exposure.
 * <li>The array is cleared using CCD_DSP_Command_CLR.
 * <li>The exposure is started by calling CCD_DSP_Command_SEX.
 * <li>Enter a loop, until the readout is completed:
 * 	<ul>
 * 	<li>Get the Host Status Transfer Register value, using CCD_DSP_Command_Get_HSTR.
 * 	<li>If we are not reading out, and have more than Exposure_Data.Readout_Remaining_Time milliseconds 
 * 		left of exposure, use CCD_DSP_Command_RET to get the current elapsed exposure time.
 * 	<li>If the exposure length minus the current elapsed exposure time is less than 
 * 		Exposure_Data.Readout_Remaining_Time milliseconds, switch exposure status to READOUT.
 * 	<li>If we are in readout mode, use CCD_DSP_Command_Get_Readout_Progress to get how many pixels
 * 		we have read out.
 * 	<li>Check to see if we have finished reading out.
 * 	<li>Check to see whether we have been aborted.
 *	</ul>
 * <li>If we are reading out a full frame, call Exposure_Expose_Post_Readout_Full_Frame.
 * </ul>
 * The Exposure_Data.Exposure_Status is changed to reflect the operation being performed on the CCD.
 * If the exposure is aborted at any stage the routine returns. Exposure_Expose_Delete_Fits_Images is
 * called to attempt to delete the blank FITS files, if the routine fails or is aborted.
 * @param clear_array An integer representing a boolean. This should be set to TRUE if we wish to
 * 	manually clear the array before the exposure starts, FALSE if we do not. This is usually TRUE.
 * @param open_shutter TRUE if the shutter is to be opened over the duration of the exposure, FALSE if the
 * 	shutter should remain closed. The shutter may not want to be opened if a calibration image is
 * 	being taken.
 * @param start_time The time to start the exposure. If both the fields in the <i>struct timespec</i> are zero,
 * 	the exposure can be started at any convenient time.
 * @param exposure_time The length of time to open the shutter for in milliseconds. This must be greater than zero,
 * 	and less than the maximum exposure length CCD_DSP_EXPOSURE_MAX_LENGTH.
 * @param filename_list A list of filenames to save the exposure into. This is normally of length 1,unless 
 *        we are windowing, in which case there will be one filename for each window.
 * @param filename_count The number of filenames in the filename_list.
 * @return Returns TRUE if the exposure succeeds and the file is saved, returns FALSE if an error
 *	occurs or the exposure is aborted.
 * @see #EXPOSURE_HSTR_HTF_BITS
 * @see #CCD_EXPOSURE_HSTR_READOUT
 * @see #CCD_EXPOSURE_HSTR_BIT_SHIFT
 * @see #EXPOSURE_READ_TIMEOUT
 * @see #Exposure_Data
 * @see #Exposure_Expose_Post_Readout_Full_Frame
 * @see #Exposure_Expose_Delete_Fits_Images
 * @see ccd_setup.html#CCD_Setup_Get_Setup_Complete
 * @see ccd_setup.html#CCD_Setup_Get_Window_Flags
 * @see ccd_setup.html#CCD_Setup_Get_Readout_Pixel_Count
 */
int CCD_Exposure_Expose(int clear_array,int open_shutter,struct timespec start_time,int exposure_time,
			char **filename_list,int filename_count)
{
	struct timespec sleep_time,current_time;
#ifndef _POSIX_TIMERS
	struct timeval gtod_current_time;
#endif
	long *exposure_data = NULL; 
	unsigned long andor_error = 0;
	long elapsed_exposure_time = 0;
	int done;
	int status,window_flags;
	int expected_pixel_count;

	Exposure_Error_Number = 0;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose(clear_array=%d,open_shutter=%d,"
			      "start_time_sec=%ld,exposure_time=%d,filename_count=%d) started.",
			      clear_array,open_shutter,start_time.tv_sec,exposure_time,filename_count);
#endif
/* reset abort flag */
	CCD_Exposure_Set_Abort(FALSE);
/* we shouldn't be able to expose until setup has been successfully completed - check this */
	if(!CCD_Setup_Get_Setup_Complete())
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 1;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Exposure failed:Setup was not complete");
		return FALSE;
	}
/* check parameter ranges */
	if(!CCD_GLOBAL_IS_BOOLEAN(clear_array))
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 6;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Illegal value:clear_array = %d.",
			clear_array);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: clear_array = %d",clear_array);
#endif
	if(!CCD_GLOBAL_IS_BOOLEAN(open_shutter))
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 2;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Illegal value:open_shutter = %d.",
			open_shutter);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: open_shutter = %d",open_shutter);
#endif
	if(exposure_time < 0)
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 3;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Illegal value:exposure_time = %d",exposure_time);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: exposure_time = %d msec",
			      exposure_time);
#endif
	if(filename_count < 0)
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 7;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Illegal value:filename_count = %d",filename_count);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: filename_count = %d",filename_count);
#endif
	window_flags = CCD_Setup_Get_Window_Flags();
	if((window_flags == 0)&&(filename_count > 1))
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 8;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Too many filenames for window_flags %d:"
			"filename_count = %d",window_flags,filename_count);
		return FALSE;
	}
/* get information from setup that we need to do an exposure */
	expected_pixel_count = CCD_Setup_Get_Readout_Pixel_Count();
	if(expected_pixel_count <= 0)
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Error_Number = 9;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Illegal expected pixel count '%d'.",
			expected_pixel_count);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: expected_pixel_count =  %d",
			      expected_pixel_count);
#endif

/* setup the shutter control bit - which determines whether the SEX command has
** control to open and close the shutter at the appropriate times */
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose():Setting shutter control(%d).",
			      open_shutter);
#endif
/* write the time to memory so that SEX can read it */
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose():Setting exposure length(%d).",
			      exposure_time);
#endif

	/* Set the ANDOR exposure time  IT */

	andor_error = SetExposureTime((float)(exposure_time/1000));
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: Andor SetExposureTime %lu",
			      andor_error);
#endif
/* initialise variables */
/* We will use the start_time parameter to determine when to start the exposure IF 
** it's seconds are greater then zero */ 
/* do the clear array a few seconds before the exposure is due to start */
	if(start_time.tv_sec > 0)
	{
		Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_WAIT_START;
		done = FALSE;
		while(done == FALSE)
		{
#ifdef _POSIX_TIMERS
			clock_gettime(CLOCK_REALTIME,&current_time);
#else
			gettimeofday(&gtod_current_time,NULL);
			current_time.tv_sec = gtod_current_time.tv_sec;
			current_time.tv_nsec = gtod_current_time.tv_usec*CCD_GLOBAL_ONE_MICROSECOND_NS;
#endif
#if LOGGING > 4
			CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,
				       "CCD_Exposure_Expose():Waiting for exposure start time (%ld,%ld).",
				       current_time.tv_sec,start_time.tv_sec);
#endif
		/* if we've time, sleep for a second */
			if((start_time.tv_sec - current_time.tv_sec) > Exposure_Data.Start_Exposure_Clear_Time)
			{
				sleep_time.tv_sec = 1;
				sleep_time.tv_nsec = 0;
				nanosleep(&sleep_time,NULL);
			}
			else
				done = TRUE;
		/* check - have we been aborted? */
			/*if(CCD_Exposure_Get_Abort())
			{
				Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
				Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
				Exposure_Error_Number = 37;
				sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Aborted.");
				return FALSE;
			} */
		}/* end while */
	}
/* clear the array */
	if(clear_array)
	{
#if LOGGING > 4
		CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose():Clearing CCD array.");
#endif
		Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_CLEAR;
	}
/* check - have we been aborted? */
	if(CCD_Exposure_Get_Abort())
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
		Exposure_Error_Number = 20;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Aborted.");
		return FALSE;
	} 
/* Send the command to start the exposure, and monitor for completion. */
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:Starting Exposure.");
#endif
	/* Exposure status is set in CCD_DSP_Command_SEX, as this routine sleeps before starting
	** the exposure. */
	CCD_Exposure_Set_Abort(FALSE);
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:Waiting to Expose");
#endif
	while(status!=DRV_IDLE) 
		GetStatus(&status);
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:Starting Exposure  %d msec",
			      exposure_time);
#endif
	Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_PRE_READOUT;
	StartAcquisition();
	GetStatus(&status);

	fflush(stdout);	
	while (status==DRV_ACQUIRING)
	{
		GetStatus(&status);
		usleep(500000); /* Sleep for 0.5 sec */
		elapsed_exposure_time=elapsed_exposure_time+500;
		if (elapsed_exposure_time>(exposure_time+30000)) 
		{
			CCD_Exposure_Abort(); 
			AbortAcquisition();
		}
		if(CCD_Exposure_Get_Abort())
		{
			Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
			Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
			Exposure_Error_Number = 24;
			CCD_Exposure_Abort(); 
			AbortAcquisition();
			sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Aborted.");
			return TRUE;
		}
		fflush(stdout);	
	}
	  
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:Finished Exposure...Andor Status %d",
			      status);
#endif
	if(CCD_Exposure_Get_Abort())
	{
		Exposure_Expose_Delete_Fits_Images(filename_list,filename_count);
		Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
		Exposure_Error_Number = 10;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Aborted.");
		return TRUE;
	}
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose():Getting reply data.");
#endif

	/* get data */
	exposure_data=(long*)malloc(expected_pixel_count*sizeof(long));
	if (exposure_data==NULL) 
	{
		Exposure_Error_Number = 4;
		sprintf(Exposure_Error_String,"CCD_Exposure_Expose:Unable to malloc() memory!");
		return FALSE; 
	} 

#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:malloc'd in array %p",
		       (void *)exposure_data); 
#endif
	andor_error=GetAcquiredData(exposure_data,expected_pixel_count);
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose:GetAcquiredData returned %lu",
			      andor_error);
#endif
	Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_POST_READOUT;
/* post-readout processing depends on whether we are windowing or not. */
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose: window_flags==%d",window_flags);
#endif
	
	if(window_flags == 0)
	{
		if(Exposure_Expose_Post_Readout_Full_Frame((unsigned long *)exposure_data,filename_list[0]) == FALSE)
		{
			/* Do not call Exposure_Expose_Delete_Fits_Images here - we may have saved to disk */
			Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
			return FALSE;
		}
	}
/* reset exposure status */
	Exposure_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Expose() returned TRUE.");
#endif
	CCD_Exposure_Set_Abort(FALSE);
	free(exposure_data); 
	return TRUE;
}

/**
 * Routine to take a bias frame. Calls CCD_Exposure_Expose with clear_array TRUE, open_shutter FALSE and 
 * zero exposure length. Note assumes single readout filename, will not work if setup is windowed.
 * @param filename The filename to save the resultant data (in FITS format) to.
 * @return The routine returns TRUE if the operation was completed successfully, FALSE if it failed.
 */
int CCD_Exposure_Bias(char *filename)
{
	struct timespec start_time;
	char *filename_list[1];

	start_time.tv_sec = 0;
	start_time.tv_nsec = 0;
	filename_list[0] = filename;
	return CCD_Exposure_Expose(TRUE,FALSE,start_time,0,filename_list,1);
}

/**
 * This routine aborts an exposure currenly underway, whether it is reading out or not.
 * This routine sets the Abort flag to true by calling CCD_Exposure_Set_Abort(TRUE).
 * @return Returns TRUE if the abort succeeds  returns FALSE if an error occurs.
 * @see #Exposure_Data
 * @see #CCD_Exposure_Expose
 * @see #CCD_Exposure_Get_Exposure_Status
 * @see #CCD_Exposure_Set_Abort
 */
int CCD_Exposure_Abort(void)
{
	Exposure_Error_Number = 0;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Abort() started with exposure status %d.",
		       Exposure_Data.Exposure_Status);
#endif
	CCD_Exposure_Set_Abort(TRUE);
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"CCD_Exposure_Abort() finished.");
#endif
	return TRUE;
}


/**
 * This routine is not called as part of the normal exposure sequence. It is used to read out a ccd exposure
 * under manual control or to read out an aborted exposure. 
 * If the readout is aborted at any stage the routine returns.
 * Note assumes single readout filename, will not work if setup is windowed.
 * This routine just calls CCD_Exposure_Expose, with clear_array FALSE (to prevent destruction of the image),
 * open_shutter FALSE, and an exposure length of zero.
 * @param filename The filename to save the exposure into.
 * @return Returns TRUE if the readout succeeds and the file is saved, returns FALSE if an error
 *	occurs or the readout is aborted.
 * @see #CCD_Exposure_Expose
 */
int CCD_Exposure_Read_Out_CCD(char *filename)
{
	struct timespec start_time;
	char *filename_list[1];

	start_time.tv_sec = 0;
	start_time.tv_nsec = 0;
	filename_list[0] = filename;
	return CCD_Exposure_Expose(FALSE,FALSE,start_time,0,filename_list,1);
}

/**
 * Routine to set the current value of the exposure status.
 * @param status The exposure status.
 * @see #Exposure_Data
 * @see #CCD_EXPOSURE_STATUS
 * @see #CCD_EXPOSURE_IS_STATUS
 */
int CCD_Exposure_Set_Exposure_Status(enum CCD_EXPOSURE_STATUS status)
{
	if(!CCD_EXPOSURE_IS_STATUS(status))
	{
		Exposure_Error_Number = 72;
		sprintf(Exposure_Error_String,"CCD_Exposure_Set_Exposure_Status:Status illegal value (%d).",status);
		return FALSE;
	}
	Exposure_Data.Exposure_Status = status;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,
			      "CCD_Exposure_Set_Exposure_Status: Exposure_Data.Exposure_Status = %d\n",
			      Exposure_Data.Exposure_Status);
#endif
	return TRUE;
}


/**
 * This routine gets the current value of Exposure Status.
 * Exposure_Status is defined in Exposure_Data.
 * @return The current status of exposure.
 * @see #CCD_EXPOSURE_STATUS
 * @see #Exposure_Data
 */
enum CCD_EXPOSURE_STATUS CCD_Exposure_Get_Exposure_Status(void)
{
	return Exposure_Data.Exposure_Status;
}

/**
 * This routine gets the current value of Exposure Length.
 * @return The last exposure length.
 * @see #Exposure_Data
 */
int CCD_Exposure_Get_Exposure_Length(void)
{
	return Exposure_Data.Exposure_Length;
}

/**
 * This routine gets the time stamp for the start of the exposure.
 * @return The time stamp for the start of the exposure.
 * @see #Exposure_Data
 */
struct timespec CCD_Exposure_Get_Exposure_Start_Time(void)
{
	return Exposure_Data.Exposure_Start_Time;
}

/**
 * Routine to set how many seconds before an exposure is due to start we wish to send the CLEAR_ARRAY
 * command to the controller.
 * @param time The time in seconds. This should be greater than the time the CLEAR_ARRAY command takes to
 * 	clock all accumulated charge off the CCD (approx 5 seconds for a 2kx2k EEV42-40).
 * @see #Exposure_Data
 */
void CCD_Exposure_Set_Start_Exposure_Clear_Time(int time)
{
	Exposure_Data.Start_Exposure_Clear_Time = time;
}

/**
 * Routine to get the current setting for how many seconds before an exposure is due to start we wish 
 * to send the CLEAR_ARRAY command to the controller.
 * @return The time, in seconds.
 * @see #Exposure_Data
 */
int CCD_Exposure_Get_Start_Exposure_Clear_Time(void)
{
	return Exposure_Data.Start_Exposure_Clear_Time;
}

/**
 * Routine to set the amount of time, in milliseconds, before the desired start of exposure that we should send the
 * START_EXPOSURE command, to allow for transmission delay.
 * @param time The time, in milliseconds.
 * @see #Exposure_Data
 */
void CCD_Exposure_Set_Start_Exposure_Offset_Time(int time)
{
	Exposure_Data.Start_Exposure_Offset_Time = time;
}

/**
 * Routine to get the amount of time, in milliseconds, before the desired start of exposure that we should send the
 * START_EXPOSURE command, to allow for transmission delay.
 * @return The time, in milliseconds.
 * @see #Exposure_Data
 */
int CCD_Exposure_Get_Start_Exposure_Offset_Time(void)
{
	return Exposure_Data.Start_Exposure_Offset_Time;
}

/**
 * Routine to set the amount of time, in milleseconds, remaining for an exposure when we change status to READOUT, 
 * to stop RDM/TDL/WRMs affecting the readout.
 * @param time The time, in milliseconds. Note, because the exposure time is read every second, it is best
 * 	not have have this constant an exact multiple of 1000.
 * @see #Exposure_Data
 */
void CCD_Exposure_Set_Readout_Remaining_Time(int time)
{
	Exposure_Data.Readout_Remaining_Time = time;
}

/**
 * Routine to get the amount of time, in milliseconds, remaining for an exposure when we change status to READOUT, 
 * to stop RDM/TDL/WRMs affecting the readout.
 * @return The time, in milliseconds.
 * @see #Exposure_Data
 */
int CCD_Exposure_Get_Readout_Remaining_Time(void)
{
	return Exposure_Data.Readout_Remaining_Time;
}

/**
 * Routine to set the Exposure_Start_Time of Exposure_Data, to the current time of the real time clock.
 * clock_gettime or gettimeofday is used, depending on whether _POSIX_TIMERS is defined.
 * @see #Exposure_Data
 */
void CCD_Exposure_Set_Exposure_Start_Time(void)
{
#ifndef _POSIX_TIMERS
	struct timeval gtod_current_time;
#endif

#ifdef _POSIX_TIMERS
	clock_gettime(CLOCK_REALTIME,&(Exposure_Data.Exposure_Start_Time));
#else
	gettimeofday(&gtod_current_time,NULL);
	Exposure_Data.Exposure_Start_Time.tv_sec = gtod_current_time.tv_sec;
	Exposure_Data.Exposure_Start_Time.tv_nsec = gtod_current_time.tv_usec*CCD_GLOBAL_ONE_MICROSECOND_NS;
#endif
}

/**
 * This routine returns the current stste of the Abort flag.
 * The Abort flag is defined in Exposure_Data and is set to true when
 * the user wants to stop execution mid-commend.
 * @return The current Abort status.
 * @see #CCD_Exposure_Set_Abort
 */
int CCD_Exposure_Get_Abort(void)
{
	return Exposure_Data.Abort;
}

/**
 * This routine allows the setting and reseting of the Abort flag.
 * The Abort flag is defined in Exposure_Data and is set to true when
 * the user wants to stop execution mid-commend.
 * @return Returns TRUE or FALSE to indicate success/failure.
 * @param value What to set the Abort flag to: either TRUE or FALSE.
 * @see #CCD_Exposure_Get_Abort
 * @see #Exposure_Data
 */
int CCD_Exposure_Set_Abort(int value)
{
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Exposure_Set_Abort(%d) started.",value);
#endif
	if(!CCD_GLOBAL_IS_BOOLEAN(value))
	{
		Exposure_Error_Number = 5;
		sprintf(Exposure_Error_String,"CCD_Exposure_Set_Abort:Illegal value '%d'.",value);
		return FALSE;
	}
	Exposure_Data.Abort = value;
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Exposure_Set_Abort(%d) finished.",value);
#endif
	return TRUE;
}

/**
 * Get the current value of the ccd_exposure error number.
 * @return The current value of the ccd_exposure error number.
 */
int CCD_Exposure_Get_Error_Number(void)
{
	return Exposure_Error_Number;
}

/**
 * The error routine that reports any errors occuring in ccd_exposure in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Exposure_Error(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Exposure_Error_Number == 0)
		sprintf(Exposure_Error_String,"Logic Error:No Error defined");
	fprintf(stderr,"%s CCD_Exposure:Error(%d) : %s\n",time_string,Exposure_Error_Number,Exposure_Error_String);
}

/**
 * The error routine that reports any errors occuring in ccd_exposure in a standard way. This routine places the
 * generated error string at the end of a passed in string argument.
 * @param error_string A string to put the generated error in. This string should be initialised before
 * being passed to this routine. The routine will try to concatenate it's error string onto the end
 * of any string already in existance.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Exposure_Error_String(char *error_string)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Exposure_Error_Number == 0)
		sprintf(Exposure_Error_String,"Logic Error:No Error defined");
	sprintf(error_string+strlen(error_string),"%s CCD_Exposure:Error(%d) : %s\n",time_string,
		Exposure_Error_Number,Exposure_Error_String);
}

/**
 * The warning routine that reports any warnings occuring in ccd_exposure in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Exposure_Warning(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an warning message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an warning to display */
	if(Exposure_Error_Number == 0)
		sprintf(Exposure_Error_String,"Logic Error:No Warning defined");
	fprintf(stderr,"%s CCD_Exposure:Warning(%d) : %s\n",time_string,Exposure_Error_Number,Exposure_Error_String);
}

/* ----------------------------------------------------------------
**	Internal routines
** ---------------------------------------------------------------- */
/**
 * Post-Readout operations on a full frame exposure,
 * <ul>
 * <li>The number of columns and rows are retrieved from setup.
 * <li>The data is saved to disc using Exposure_Save.
 * </ul>
 * If an error occurs BEFORE saving the read out frame to disk, Exposure_Expose_Delete_Fits_Images is called
 * to delete any 'blank' FITS files.
 * @param exposure_data The data read out from the CCD.
 * @param filename The FITS filename (which should already contain relevant headers), in which to write 
 *        the image data.
 * @return The routine returns TRUE if it suceeded, and FALSE if it fails.
 * @see #Exposure_Save
 * @see #Exposure_Expose_Delete_Fits_Images
 * @see ccd_setup.html#CCD_Setup_Get_NCols
 * @see ccd_setup.html#CCD_Setup_Get_NRows
 */
static int Exposure_Expose_Post_Readout_Full_Frame(unsigned long *exposure_data,char *filename)
{
	char *filename_list[1];
	int ncols,nrows;

/* get setup details */
	ncols = CCD_Setup_Get_NCols();
	nrows = CCD_Setup_Get_NRows();
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,
			      "Exposure_Expose_Post_Readout_Full_Frame: ncols nrows deinterlace %d %d",
			      ncols,nrows);
#endif
/* number of columns must be a positive number */
	if(ncols <= 0)
	{
		filename_list[0] = filename;
		Exposure_Expose_Delete_Fits_Images(filename_list,1);
		Exposure_Error_Number = 27;
		sprintf(Exposure_Error_String,"Exposure_Expose_Post_Readout_Full_Frame:Illegal ncols '%d'.",ncols);
		return FALSE;
	}
/* number of rows must be a positive number */
	if(nrows <= 0)
	{
		filename_list[0] = filename;
		Exposure_Expose_Delete_Fits_Images(filename_list,1);
		Exposure_Error_Number = 31;
		sprintf(Exposure_Error_String,"Exposure_Expose_Post_Readout_Full_Frame:Illegal nrows '%d'.",nrows);
		return FALSE;
	}
/* 
	DON'T do deinterlacing!! Andor  IT 
*/

/* save the resultant image to disk */
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Expose_Post_Readout_Full_Frame:"
			      "Saving to filename %s.",filename);
#endif
	if(!Exposure_Save(filename,exposure_data,ncols,nrows))
	{
		/* Exposure_Save can fail but still have saved the exposure_data to disk OK */
		return FALSE;
	}
	return TRUE; 
}

/* 
** Exposure_Save uses a different implementation depending on whether CFITSIO define was defined at compile time.
** If it was we use CFITSIO routines, otherwise we don't.
*/

#ifdef CFITSIO
/**
 * This routine takes some image data and saves it in a file on disc. It also updates the 
 * DATE-OBS FITS keyword to the value saved just before the SEX command was sent to the controller.
 * @param filename The filename to save the data into.
 * @param exposure_data The data to save.
 * @param ncols The number of columns in the image data.
 * @param nrows The number of rows in the image data.
 * @return Returns TRUE if the image is saved successfully, FALSE if it fails.
 * @see #Exposure_TimeSpec_To_Date_String
 * @see #Exposure_TimeSpec_To_Date_Obs_String
 * @see #Exposure_TimeSpec_To_UtStart_String
 * @see #Exposure_TimeSpec_To_Mjd
 */
static int Exposure_Save(char *filename,unsigned long *exposure_data,int ncols,int nrows)
{
	fitsfile *fp = NULL;
	int retval=0,status=0;
	int ii;
	char buff[32]; /* fits_get_errstatus returns 30 chars max */
	char exposure_start_time_string[64];
	double mjd;

#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Started.");
#endif
	
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Printing first 5 data elements:");
	for(ii=0;ii<5;ii++) 
	{
		CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:exposure_data[%d]:%lu",
				      ii,exposure_data[ii]);
	}
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save: Ended printing");
#endif

	/* try to open file */
	retval = fits_open_file(&fp,filename,READWRITE,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		Exposure_Error_Number = 53;
		sprintf(Exposure_Error_String,"Exposure_Save: File open failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
	/* write the data */
	/* retval = fits_write_img(fp,TUSHORT,1,ncols*nrows,exposure_data,&status); */
	retval = fits_write_img(fp,TULONG,1,ncols*nrows,exposure_data,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Exposure_Error_Number = 54;
		sprintf(Exposure_Error_String,"Exposure_Save: File write failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
/* update DATE keyword */
	Exposure_TimeSpec_To_Date_String(Exposure_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"DATE",exposure_start_time_string,NULL,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Exposure_Error_Number = 55;
		sprintf(Exposure_Error_String,"Exposure_Save: Updating DATE failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
/* update DATE-OBS keyword */
	Exposure_TimeSpec_To_Date_Obs_String(Exposure_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"DATE-OBS",exposure_start_time_string,NULL,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Exposure_Error_Number = 56;
		sprintf(Exposure_Error_String,"Exposure_Save: Updating DATE-OBS failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}
/* update UTSTART keyword */
	Exposure_TimeSpec_To_UtStart_String(Exposure_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"UTSTART",exposure_start_time_string,NULL,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Exposure_Error_Number = 57;
		sprintf(Exposure_Error_String,"Exposure_Save: Updating UTSTART failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}
/* update MJD keyword */
/* note leap second correction not implemented yet (always FALSE). */
	if(!Exposure_TimeSpec_To_Mjd(Exposure_Data.Exposure_Start_Time,FALSE,&mjd))
		return FALSE;
	retval = fits_update_key_fixdbl(fp,"MJD",mjd,6,NULL,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Exposure_Error_Number = 58;
		sprintf(Exposure_Error_String,"Exposure_Save: Updating MJD failed(%.2f,%s,%d,%s).",mjd,filename,
			status,buff);
		return FALSE;
	}
/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		Exposure_Error_Number = 59;
		sprintf(Exposure_Error_String,"Exposure_Save: File close failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
#if LOGGING > 4
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Completed to file %s.",filename);
	CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Finished, CFITSIO status %d",status);
#endif
	return TRUE;
}
#else
/**
 * This routine takes some image data and saves it in a file on disc.
 * This routine does not update the DATE-OBS keyword, unlike the CFITSIO routine.
 * @param filename The filename to save the data into.
 * @param exposure_data The data to save.
 * @param ncols The number of columns in the image data.
 * @param nrows The number of rows in the image data.
 * @return Returns TRUE if the image is saved successfully, FALSE if it fails.
 */
static int Exposure_Save(char *filename,unsigned long *exposure_data,int ncols,int nrows)
{
	FILE *fp = NULL;
	int retval,error_number,nitems;

#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Started.");
#endif
	/* try to open file */
	fp = fopen(filename,"rb+");
	if(fp == NULL)
	{
		error_number = errno;
		Exposure_Error_Number = 60;
		sprintf(Exposure_Error_String,"Exposure_Save: File open failed(%s,%d).",filename,error_number);
		return FALSE;
	}
	/* move to end of file */
	retval = fseek(fp,0,SEEK_END);
	if(retval == -1)
	{
		fclose(fp);
		Exposure_Error_Number = 61;
		sprintf(Exposure_Error_String,"Exposure_Save: File seek failed(%s,%d,%s).",filename,errno,
			strerror(errno));
		return FALSE;
	}
	/* write the data */
	nitems = nrows*ncols;
	retval = fwrite(exposure_data,CCD_GLOBAL_BYTES_PER_PIXEL,nitems,fp);
	if(retval != nitems)
	{
		fclose(fp);
		Exposure_Error_Number = 62;
		sprintf(Exposure_Error_String,"Exposure_Save: File write failed(%s,%d,%d).",filename,retval,nitems);
		return FALSE;
	}
	fclose(fp);
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Save:Completed.");
#endif
	return TRUE;
}
#endif

/**
 * Routine to convert a timespec structure to a DATE sytle string to put into a FITS header.
 * This uses gmtime and strftime to format the string. The resultant string is of the form:
 * <b>CCYY-MM-DD</b>, which is equivalent to %Y-%m-%d passed to strftime.
 * @param time The time to convert.
 * @param time_string The string to put the time representation in. The string must be at least
 * 	12 characters long.
 */
static void Exposure_TimeSpec_To_Date_String(struct timespec time,char *time_string)
{
	struct tm *tm_time = NULL;

	tm_time = gmtime(&(time.tv_sec));
	strftime(time_string,12,"%Y-%m-%d",tm_time);
}

/**
 * Routine to convert a timespec structure to a DATE-OBS sytle string to put into a FITS header.
 * This uses gmtime and strftime to format most of the string, and tags the milliseconds on the end.
 * The resultant form of the string is <b>CCYY-MM-DDTHH:MM:SS.sss</b>.
 * @param time The time to convert.
 * @param time_string The string to put the time representation in. The string must be at least
 * 	24 characters long.
 * @see ccd_global.html#CCD_GLOBAL_ONE_MILLISECOND_NS
 */
static void Exposure_TimeSpec_To_Date_Obs_String(struct timespec time,char *time_string)
{
	struct tm *tm_time = NULL;
	char buff[32];
	int milliseconds;

	tm_time = gmtime(&(time.tv_sec));
	strftime(buff,32,"%Y-%m-%dT%H:%M:%S.",tm_time);
	milliseconds = (((double)time.tv_nsec)/((double)CCD_GLOBAL_ONE_MILLISECOND_NS));
	sprintf(time_string,"%s%03d",buff,milliseconds);
}

/**
 * Routine to convert a timespec structure to a UTSTART sytle string to put into a FITS header.
 * This uses gmtime and strftime to format most of the string, and tags the milliseconds on the end.
 * @param time The time to convert.
 * @param time_string The string to put the time representation in. The string must be at least
 * 	14 characters long.
 * @see ccd_global.html#CCD_GLOBAL_ONE_MILLISECOND_NS
 */
static void Exposure_TimeSpec_To_UtStart_String(struct timespec time,char *time_string)
{
	struct tm *tm_time = NULL;
	char buff[16];
	int milliseconds;

	tm_time = gmtime(&(time.tv_sec));
	strftime(buff,16,"%H:%M:%S.",tm_time);
	milliseconds = (((double)time.tv_nsec)/((double)CCD_GLOBAL_ONE_MILLISECOND_NS));
	sprintf(time_string,"%s%03d",buff,milliseconds);
}

/**
 * Routine to convert a timespec structure to a Modified Julian Date (decimal days) to put into a FITS header.
 * <p>If SLALIB is defined, this uses slaCldj to get the MJD for zero hours, 
 * and then adds hours/minutes/seconds/milliseconds on the end as a decimal.
 * <p>If NGATASTRO is defined, this uses NGAT_Astro_Timespec_To_MJD to get the MJD.
 * <p>If neither SLALIB or NGATASTRO are defined at compile time, this routine should throw an error
 * when compiling.
 * <p>This routine is still wrong for last second of the leap day, as gmtime will return 1st second of the next day.
 * Also note the passed in leap_second_correction should change at midnight, when the leap second occurs.
 * None of this should really matter, 1 second will not affect the MJD for several decimal places.
 * @param time The time to convert.
 * @param leap_second_correction A number representing whether a leap second will occur. This is normally zero,
 * 	which means no leap second will occur. It can be 1, which means the last minute of the day has 61 seconds,
 *	i.e. there are 86401 seconds in the day. It can be -1,which means the last minute of the day has 59 seconds,
 *	i.e. there are 86399 seconds in the day.
 * @param mjd The address of a double to store the calculated MJD.
 * @return The routine returns TRUE if it succeeded, FALSE if it fails. 
 *         slaCldj and NGAT_Astro_Timespec_To_MJD can fail.
 */
static int Exposure_TimeSpec_To_Mjd(struct timespec time,int leap_second_correction,double *mjd)
{
#ifdef SLALIB
	struct tm *tm_time = NULL;
	int year,month,day;
	double seconds_in_day = 86400.0;
	double elapsed_seconds;
	double day_fraction;
#endif
	int retval;

#ifdef SLALIB
/* check leap_second_correction in range */
/* convert time to ymdhms*/
	tm_time = gmtime(&(time.tv_sec));
/* convert tm_time data to format suitable for slaCldj */
	year = tm_time->tm_year+1900; /* tm_year is years since 1900 : slaCldj wants full year.*/
	month = tm_time->tm_mon+1;/* tm_mon is 0..11 : slaCldj wants 1..12 */
	day = tm_time->tm_mday;
/* call slaCldj to get MJD for 0hr */
	slaCldj(year,month,day,mjd,&retval);
	if(retval != 0)
	{
		Exposure_Error_Number = 63;
		sprintf(Exposure_Error_String,"Exposure_TimeSpec_To_Mjd:slaCldj(%d,%d,%d) failed(%d).",year,month,
			day,retval);
		return FALSE;
	}
/* how many seconds were in the day */
	seconds_in_day = 86400.0;
	seconds_in_day += (double)leap_second_correction;
/* calculate the number of elapsed seconds in the day */
	elapsed_seconds = (double)tm_time->tm_sec + (((double)time.tv_nsec) / 1.0E+09);
	elapsed_seconds += ((double)tm_time->tm_min) * 60.0;
	elapsed_seconds += ((double)tm_time->tm_hour) * 3600.0;
/* calculate day fraction */
	day_fraction = elapsed_seconds / seconds_in_day;
/* add day_fraction to mjd */
	(*mjd) += day_fraction;
#else
#ifdef NGATASTRO
	retval = NGAT_Astro_Timespec_To_MJD(time,leap_second_correction,mjd);
	if(retval == FALSE)
	{
		Exposure_Error_Number = 64;
		sprintf(Exposure_Error_String,"Exposure_TimeSpec_To_Mjd:NGAT_Astro_Timespec_To_MJD failed.\n");
		/* concatenate NGAT Astro library error onto Exposure_Error_String */
		NGAT_Astro_Error_String(Exposure_Error_String+strlen(Exposure_Error_String));
		return FALSE;
	}
#else
#error Neither NGATASTRO or SLALIB are defined: No library defined for MJD calculation.
#endif
#endif
	return TRUE;
}

/**
 * Routine used to delete any of the filenames specified in filename_list, if they exist on disk.
 * This is done as part of aborting or when an error occurs during an exposure sequence.
 * This stops FITS images being left on disk with blank image data within them, which the data pipeline
 * does not like.
 * @param filename_list A list of strings, containing filenames to delete. These filenames should be
 *        a list of FITS images passed to the CCD_Exposure_Expose routine (a list of windows to readout to).
 * @param filename_count The number of filenames in filename_list.
 * @return The routine returns TRUE if it succeeded, FALSE if it fails. 
 * @see #CCD_Exposure_Expose
 * @see #fexist
 */
static int Exposure_Expose_Delete_Fits_Images(char **filename_list,int filename_count)
{
	int i,retval,local_errno;

#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Expose_Delete_Fits_Images:Started.");
#endif
	for(i=0;i<filename_count; i++)
	{
		if(fexist(filename_list[i]))
		{
#if LOGGING > 4
			CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Expose_Delete_Fits_Images:"
					      "Removing file %s (index %d).",filename_list[i],i);
#endif
			retval = remove(filename_list[i]);
			local_errno = errno;
			if(retval != 0)
			{
				Exposure_Error_Number = 17;
				sprintf(Exposure_Error_String,"Exposure_Expose_Delete_Fits_Images: "
					"remove failed(%s,%d,%d,%s).",filename_list[i],retval,local_errno,
					strerror(local_errno));
				return FALSE;
			}
		}/* end if exist */
		else
		{
#if LOGGING > 4
			CCD_Global_Log_Format(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Expose_Delete_Fits_Images:"
					      "file %s (index %d) does not exist?",filename_list[i],i);
#endif
		}
	}/* end for */
#if LOGGING > 4
	CCD_Global_Log(LOG_VERBOSITY_INTERMEDIATE,"Exposure_Expose_Delete_Fits_Images:Finished.");
#endif
	return TRUE;
}

/**
 * Return whether the specified filename exists or not.
 * @param filename A string representing the filename to test.
 * @return The routine returns TRUE if the filename exists, and FALSE if it does not exist. 
 */
static int fexist(char *filename)
{
	FILE *fptr = NULL;

	fptr = fopen(filename,"r");
	if(fptr == NULL )
		return FALSE;
	fclose(fptr);
	return TRUE;
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.4  2022/03/14 15:23:03  cjm
** Removed SDSU specific exposure code.
**
** Revision 1.3  2010/03/26 14:39:49  cjm
** Changed from bitwise to absolute logging levels.
**
** Revision 1.2  2010/02/09 11:52:40  cjm
** No change.
**
** Revision 1.1  2009/10/15 10:16:23  cjm
** Initial revision
**
** Revision 0.33  2006/05/17 18:06:20  cjm
** Fixed unused variables and mismatches sprintfs.
**
** Revision 0.32  2006/05/16 14:14:02  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.31  2005/02/04 17:46:49  cjm
** Added Exposure_Expose_Delete_Fits_Images.
** Most of the time, FITS filename passed into CCD_Exposure_Expose that
** do not subsequently get filled with read out data get deleted.
** There are some places (e.g. Exposure_Save) where they do not.
**
** Revision 0.30  2004/08/02 16:34:58  cjm
** Added CCD_DSP_DEINTERLACE_FLIP.
**
** Revision 0.29  2004/06/03 16:23:23  cjm
** Calling ABR when exposure status is READOUT seems to cause occasional lockups.
** So we now only abort when exposing.
** If we abort when in readout mode, the software will wait until it exits the
** exposure/readout loop to catch the abort.
**
** Revision 0.28  2004/05/16 15:34:11  cjm
** Added CCD_EXPOSURE_STATUS_NONE set if ABR failed.
**
** Revision 0.27  2004/05/16 14:28:18  cjm
** Re-wrote abort code.
**
** Revision 0.26  2003/12/08 15:04:00  cjm
** CCD_EXPOSURE_STATUS_WAIT_START added.
**
** Revision 0.25  2003/11/04 14:42:00  cjm
** Minor MJD fixes based on errors in Linux versions of the code.
**
** Revision 0.24  2003/03/26 15:44:48  cjm
** Added windowing code.
**
** Revision 0.23  2003/03/04 17:09:53  cjm
** Added NGAT_Astro call.
**
** Revision 0.22  2002/12/16 16:49:36  cjm
** Fixed problems with status during an exposure, so that it only goes into
** PRE_READOUT at the correct time.
** Removed Error routines resetting error number to zero.
**
** Revision 0.21  2002/11/08 12:13:19  cjm
** CCD_DSP_Command_SEX now changes the exposure status to READOUT immediately,
** if the exposure length is small enough.
**
** Revision 0.20  2002/11/07 19:13:39  cjm
** Changes to make library work with SDSU version 1.7 DSP code.
**
** Revision 0.19  2001/06/04 14:38:00  cjm
** Changed DEBUG to LOGGING.
** Added readout process priority changes and memory locking code.
**
** Revision 0.18  2001/02/16 09:55:18  cjm
** Added more detail to error messages.
**
** Revision 0.17  2001/02/09 18:30:40  cjm
** comment spelling.
**
** Revision 0.16  2001/02/05 14:30:09  cjm
** Added checks to CCD_Exposure_Bias to STP/IDL called
** only when Idling was configured at startup.
**
** Revision 0.15  2001/01/23 18:21:27  cjm
** Added check for maximum exposure length CCD_DSP_EXPOSURE_MAX_LENGTH.
**
** Revision 0.14  2000/09/25 09:51:28  cjm
** Changes to use with v1.4 SDSU DSP code.
**
** Revision 0.13  2000/07/14 16:25:44  cjm
** Backup.
**
** Revision 0.12  2000/07/11 10:42:24  cjm
** Removed CCD_Exposure_Flush_CCD.
**
** Revision 0.11  2000/06/20 12:53:07  cjm
** CCD_DSP_Command_Sex now automatically calls CCD_DSP_Command_RDI.
**
** Revision 0.10  2000/06/19 08:48:34  cjm
** Backup.
**
** Revision 0.9  2000/06/13 17:14:13  cjm
** Changes to make Ccs agree with voodoo.
**
** Revision 0.8  2000/05/23 10:34:46  cjm
** Added call to CCD_DSP_Set_Exposure_Start_Time in CCD_Exposure_Bias,
** so that bias frames now have an exposure start time set,
** which gives a sensible value for DATE, DATE-OBS and UTSTART in FITS files.
**
** Revision 0.7  2000/05/10 14:37:52  cjm
** Removed number of bytes parameter from CCD_DSP_Command_RDI.
**
** Revision 0.6  2000/04/13 13:17:36  cjm
** Added current time to error routines.
**
** Revision 0.5  2000/03/13 12:30:17  cjm
** Removed duplicate CCD_DSP_Set_Abort(FALSE) in CCD_Exposure_Bias.
**
** Revision 0.4  2000/02/28 19:13:01  cjm
** Backup.
**
** Revision 0.3  2000/02/22 16:05:21  cjm
** Changed call structure to CCD_DSP_Set_Abort.
**
** Revision 0.2  2000/02/01 17:50:01  cjm
** Changed references to CCD_Setup_Setup_CCD to CCD_Setup_Get_Setup_Complete.
**
** Revision 0.1  2000/01/25 14:57:27  cjm
** initial revision (PCI version).
**
*/
