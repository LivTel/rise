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
/* ccd_multrun.c
** Functions to perform multruns
** $Header: /space/home/eng/cjm/cvs/rise/ccd/c/ccd_multrun.c,v 1.2 2009-12-18 10:54:38 cjm Exp $
*/
/**
 * ccd_multrun.c includes a rewrite of the ccd_exposure.c code with andor function calls. It had to incorporate 
 * a number of changes to the way in whic an exposure sequence is taken. Normally the looping responsible for a
 * multrun is conducted in the java layer. However, to work in frame transfer mode, we must start an acquisition
 * and grab the images from a circular buffer in memory. Thus the writing of the image sequence and the fits image
 * naming convention must be worked out here, rather than the java layer.
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
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include<math.h>
#ifndef _POSIX_TIMERS
#include <sys/time.h>
#endif
#include <time.h>
#include "ccd_global.h"
#include "ccd_exposure.h"
#include "ccd_multrun.h"
#include "ccd_dsp.h"
#include "ccd_setup.h"
#include "ccd_temperature.h"
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
#include "estar_config.h"

#define EXPOSURE_READ_TIMEOUT                           30
#define EXPOSURE_DEFAULT_READOUT_REMAINING_TIME       	(1500)
#define EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME	(10)
#define EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME	(2)
/* Header values if not filled */
#define DUMHEADERSTRING "UNKNOWN" 
#define DUMHEADERFLOAT -999.9 

/* Revision Control System identifier.
 */
static char rcsid[] = "$Id: ccd_multrun.c,v 1.2 2009-12-18 10:54:38 cjm Exp $";

/**
 * Variable holding error code of last operation performed by ccd_multrun.
 */
static int Multrun_Error_Number = 0;
/**
 * Local variable holding description of the last error that occured.
 */
static char Multrun_Error_String[CCD_GLOBAL_ERROR_STRING_LENGTH] = "";
/**
 * Data holding the current status of ccd_multrun. This is statically initialised to the following:
 * <dl>
 * <dt>Exposure_Status</dt> <dd>CCD_EXPOSURE_STATUS_NONE</dd>
 * <dt>Start_Exposure_Clear_Time</dt> <dd>EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME</dd>
 * <dt>Start_Exposure_Offset_Time</dt> <dd>EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME</dd>
 * <dt>Readout_Remaining_Time</dt> <dd>EXPOSURE_DEFAULT_READOUT_REMAINING_TIME</dd>
 * <dt>Exposure_Length</dt> <dd>0</dd>
 * <dt>Exposure_Start_Time</dt> <dd>{0L,0L}</dd>
 * </dl>
 * @see #Multrun_Struct
 * @see #CCD_EXPOSURE_STATUS
 * @see #EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME
 * @see #EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME
 * @see #EXPOSURE_DEFAULT_READOUT_REMAINING_TIME
 */
static struct Multrun_Struct Multrun_Data = 
{
	CCD_EXPOSURE_STATUS_NONE,
	EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME,
	EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME,
	EXPOSURE_DEFAULT_READOUT_REMAINING_TIME,
	0,
	0.0,
	0.0,
	0,
	{0L,0L},
	{0L,0L},
	{0L,0L},
	{0L,0L},
	0.0,
	0.0,
	0.0,
	-1.0,
  	"undefined",
	"none defined",
	999.0,
	0,
	0,
};

/**
 * FITS filename data.
 * @see #FitsFilename
 */
struct FitsFilename ff; 
/**
 * FITS header data.
 * @see #Header
 */
struct Header fileHeaders;

/* internal functions */
static unsigned int expose(float exposure, int width, int height,long nimages,int *recalculate_exposure_length); 
static void Exposure_TimeSpec_To_Date_String(struct timespec time,char *time_string);
static void Exposure_TimeSpec_To_Date_Obs_String(struct timespec time,char *time_string);
static void Exposure_TimeSpec_To_UtStart_String(struct timespec time,char *time_string);
static int Exposure_TimeSpec_To_Mjd(struct timespec time,int leap_second_correction,double *mjd); 

void GetParameterFileValues (void);
int ExpiredStatus ( time_t start, long length );
int getSquareRegion(const long *inArray, double *sqrArray, int x, int y, int R);
int getNtpDriftFile (char *file);
static int getNextFilename (char *NewFileName, int NewMultRun);
static char *ConstructNextFilename (struct FitsFilename *ff, int MMR, int MR, int startMR, char *NFN);

/**
 * Do a normal multrun.
 * @param open_shutter Boolean, whether to open the shutter or not.
 * @param startTime Unused.
 * @param exposure_time Exposure length in milliseconds.
 * @param exposures Number of exposures.
 * @param headers An array of character strings holding the FITS headers.
 * @return The routine returns TRUE on success and FALSE on failure.
 */
int CCD_Multrun_Expose(int open_shutter, long startTime, int exposure_time, long exposures, char **headers)
{
	int error,recalculate_exposure_length;
	float expose_exposure_time = (float)exposure_time/1000;
	int expose_exposures = (int)exposures; 

#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
            "CCD_Multrun_Expose:Started(open_shutter=%d,start_time=%ld,exposure_length=%d,number of exposures=%d).",
			      open_shutter,startTime,exposure_time,exposures);
#endif
	Multrun_Data.isMultFlat = 0;

	GetParameterFileValues();	
	/* Dump out the headers into the structure. These come in ordered RA,DEC,LATITUDE,LONGITUD,OBSTYPE,AIRMASS
	 * from the java layer. See /home/dev/src/ccs/java/MULTRUNImplementation.java   */
	/* NOT GOOD - works, but better using strncpy!*/
	strcpy(fileHeaders.ra,headers[0]);
	strcpy(fileHeaders.dec,headers[1]);
	strcpy(fileHeaders.latitude,headers[2]);
	strcpy(fileHeaders.longitude,headers[3]);
	strcpy(fileHeaders.obstype,headers[4]);
	strcpy(fileHeaders.airmass,headers[5]);
	strcpy(fileHeaders.telfocus,headers[6]);
	strcpy(fileHeaders.origin,headers[7]);
	strcpy(fileHeaders.instatus,headers[8]);
	strcpy(fileHeaders.configid,headers[9]);
	strcpy(fileHeaders.telescop,headers[10]);
	strcpy(fileHeaders.telmode,headers[11]);
	strcpy(fileHeaders.lst,headers[12]);
	strcpy(fileHeaders.catra,headers[13]);
	strcpy(fileHeaders.catdec,headers[14]);
	strcpy(fileHeaders.telstat,headers[15]);
	strcpy(fileHeaders.autoguid,headers[16]);
	strcpy(fileHeaders.rotmode,headers[17]);
	strcpy(fileHeaders.rotskypa,headers[18]);
	strcpy(fileHeaders.windspee,headers[19]);
	strcpy(fileHeaders.wmstemp,headers[20]);
	strcpy(fileHeaders.wmshumid,headers[21]);
	strcpy(fileHeaders.object,headers[22]);

	strcpy(fileHeaders.instrument,headers[23]);
	strcpy(fileHeaders.confname,headers[24]);
	strcpy(fileHeaders.detector,headers[25]);
	strcpy(fileHeaders.gain,headers[26]);
	strcpy(fileHeaders.readnoise,headers[27]);

	strcpy(fileHeaders.tagid,headers[28]);
	strcpy(fileHeaders.userid,headers[29]);
	strcpy(fileHeaders.propid,headers[30]);
	strcpy(fileHeaders.groupid,headers[31]);
	strcpy(fileHeaders.obsid,headers[32]);

	strcpy(fileHeaders.exptotal,headers[33]);
	strcpy(fileHeaders.prescan,headers[34]);
	strcpy(fileHeaders.postscan,headers[35]);
	strcpy(fileHeaders.rotcentx,headers[36]);
	strcpy(fileHeaders.rotcenty,headers[37]);
	strcpy(fileHeaders.poicentx,headers[38]);
	strcpy(fileHeaders.poicenty,headers[39]);
	strcpy(fileHeaders.filteri1,headers[40]);

	strcpy(fileHeaders.ccdscale,	headers[41]);
	strcpy(fileHeaders.radecsys,	headers[42]);
	strcpy(fileHeaders.equinox,	headers[43]);
	strcpy(fileHeaders.grouptimng,headers[44]);
	strcpy(fileHeaders.groupnumob,headers[45]);
	strcpy(fileHeaders.groupuid,	headers[46]);
	strcpy(fileHeaders.groupnomex,headers[47]);
	strcpy(fileHeaders.groupmonp,	headers[48]);
	strcpy(fileHeaders.filter1,	headers[49]);
	strcpy(fileHeaders.rotangle,	headers[50]);

	error=expose(expose_exposure_time, CCD_Setup_Get_NCols(), CCD_Setup_Get_NRows(), 
		      expose_exposures,&recalculate_exposure_length);
#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multrun_Expose:Finished with return value %d.",error);
#endif
	return (error);
}

void GetParameterFileValues (void)
{
	/* Get all the values from the parameter file ccs.properties */	
	/* Each one of these SHOULD be checked and filled with sane values should there be an error */

	char *tempString; /* This variable is malloc'd outside this scope*/
	long tempLong;

	eSTAR_Config_Get_Long(&rProperties,"multrun.flat.counts.target",&tempLong);
	mrParams.flatTarget = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"multrun.flat.counts.save.min",&tempLong);
	mrParams.minFlatCounts = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"multrun.flat.counts.save.max",&tempLong);
	mrParams.maxFlatCounts = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"multrun.flat.counts.recalc.min",&tempLong);
	mrParams.minFlatCountsRecalc = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"multrun.flat.counts.recalc.max",&tempLong);
	mrParams.maxFlatCountsRecalc = (unsigned int)tempLong;
	eSTAR_Config_Get_Int(&rProperties,"multrun.flat.median.HalfBoxSize",&(mrParams.halfBoxSize));
	eSTAR_Config_Get_Int(&rProperties,"multrun.flat.median.centre.x",&(mrParams.posBoxX));
	eSTAR_Config_Get_Int(&rProperties,"multrun.flat.median.centre.y",&(mrParams.posBoxY));
	eSTAR_Config_Get_Long(&rProperties,"multrun.bias.counts.mean",&tempLong);
	mrParams.biasLevel = (unsigned int)tempLong;
	/* ccs.libccd.[vh]sspeed not implemented in code yet*/
	eSTAR_Config_Get_Long(&rProperties,"ccs.libccd.hsspeed",&tempLong);
	mrParams.HSIndex = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"ccs.libccd.vsspeed",&tempLong);
	mrParams.VSIndex = (unsigned int)tempLong;
	eSTAR_Config_Get_Long(&rProperties,"ccs.twilight_calibrate.min_exposure_time",&tempLong);
	mrParams.minExposure = (float) tempLong/1000; /* Convert the time from milliseconds to seconds */
	eSTAR_Config_Get_Long(&rProperties,"ccs.twilight_calibrate.max_exposure_time",&tempLong);
	mrParams.maxExposure = (float) tempLong/1000; /* Convert the time from milliseconds to seconds */

	eSTAR_Config_Get_String(&rProperties,"ntp.datafile",&tempString);
	strncpy(mrParams.ntpDriftFile,tempString,63);

	free(tempString);
}

/**
 * Do a series a flat exposures.
 * @param open_shutter Boolean, whether to open the shutter or not.
 * @param startTime Unused.
 * @param exposure_time Exposure length in milliseconds.
 * @param exposures Maximum time to attempt a multflat run (in milliseconds).
 * @param headers An array of character strings holding the FITS headers.
 * @return Returns TRUE on success and FALSE on failure.
 * @see #expose
 */
int CCD_Multflat_Expose(int open_shutter,long startTime,int exposure_time,long exposures,char **headers)
{
	/* Variable "long exposures" is now the maximum time to attempt a multflat run (in milliseconds).
	   First, we timestamp the code now. Accuracy should be to around one second. The code should 
	   loop and take exposures while there is time enough remaining */
	
	int i,error;
	float expose_exposure_time = (float) exposure_time/1000;
	float initialExp = expose_exposure_time;
	long remainingExposures=4000;  /* Take this many exposures at most */
	int bin = CCD_Setup_Get_NSBin();
	int expired = 0;    /* Expired flag, true = 1 */
	int recalculate_exposure_length;
	Multrun_Data.maxTime = exposures/1000; /* Noted above */
	Multrun_Data.isMultFlat = 1;

#if LOGGING > 1
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multflat_Expose:Started.");
#endif
	/* Get the start time for the multflat run in seconds since the epoch */
	Multrun_Data.timeStart = time(NULL);

	/* Clear Abort Status */
	CCD_DSP_Set_Abort(FALSE);

	GetParameterFileValues();	
	/* Dump out the headers into the structure. These come in ordered RA,DEC,LATITUDE,LONGITUD,OBSTYPE 
	 * from the java layer. See /home/dev/src/ccs/java/MULTRUNImplementation.java   */
	strcpy(fileHeaders.ra,headers[0]);
	strcpy(fileHeaders.dec,headers[1]);
	strcpy(fileHeaders.latitude,headers[2]);
	strcpy(fileHeaders.longitude,headers[3]);
	strcpy(fileHeaders.obstype,"SKYFLAT");
	strcpy(fileHeaders.airmass,headers[5]);
	strcpy(fileHeaders.telfocus,headers[6]);
	strcpy(fileHeaders.origin,headers[7]);
	strcpy(fileHeaders.instatus,headers[8]);
	strcpy(fileHeaders.configid,headers[9]);
	strcpy(fileHeaders.telescop,headers[10]);
	strcpy(fileHeaders.telmode,headers[11]);
	strcpy(fileHeaders.lst,headers[12]);
	strcpy(fileHeaders.catra,headers[13]);
	strcpy(fileHeaders.catdec,headers[14]);
	strcpy(fileHeaders.telstat,headers[15]);
	strcpy(fileHeaders.autoguid,headers[16]);
	strcpy(fileHeaders.rotmode,headers[17]);
	strcpy(fileHeaders.rotskypa,headers[18]);
	strcpy(fileHeaders.windspee,headers[19]);
	strcpy(fileHeaders.wmstemp,headers[20]);
	strcpy(fileHeaders.wmshumid,headers[21]);
	strcpy(fileHeaders.object,"FLAT");
	strcpy(fileHeaders.instrument,headers[23]);
	strcpy(fileHeaders.confname,headers[24]);
	strcpy(fileHeaders.detector,headers[25]);
	strcpy(fileHeaders.gain,headers[26]);
	strcpy(fileHeaders.readnoise,headers[27]);
	strcpy(fileHeaders.tagid,headers[28]);
	strcpy(fileHeaders.userid,headers[29]);
	strcpy(fileHeaders.propid,headers[30]);
	strcpy(fileHeaders.groupid,headers[31]);
	strcpy(fileHeaders.obsid,headers[32]);

	strcpy(fileHeaders.exptotal,headers[33]);
	strcpy(fileHeaders.prescan,headers[34]);
	strcpy(fileHeaders.postscan,headers[35]);
	strcpy(fileHeaders.rotcentx,headers[36]);
	strcpy(fileHeaders.rotcenty,headers[37]);
	strcpy(fileHeaders.poicentx,headers[38]);
	strcpy(fileHeaders.poicenty,headers[39]);
	strcpy(fileHeaders.filteri1,headers[40]);

	strcpy(fileHeaders.ccdscale,	headers[41]);
	strcpy(fileHeaders.radecsys,	headers[42]);
	strcpy(fileHeaders.equinox,	headers[43]);
	strcpy(fileHeaders.grouptimng,headers[44]);
	strcpy(fileHeaders.groupnumob,headers[45]);
	strcpy(fileHeaders.groupuid,	headers[46]);
	strcpy(fileHeaders.groupnomex,headers[47]);
	strcpy(fileHeaders.groupmonp,	headers[48]);
	strcpy(fileHeaders.filter1,headers[49]);
	strcpy(fileHeaders.rotangle,	headers[50]);

#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multflat_Expose:exptime: %f  flat_run_time: %ld sec",
			      expose_exposure_time,Multrun_Data.maxTime);
#endif

	/* Take a test exposure */	
	expose(expose_exposure_time, CCD_Setup_Get_NCols(), CCD_Setup_Get_NRows(), 1,&recalculate_exposure_length);
	expose_exposure_time = getNewExposureTime(Multrun_Data.median_value,expose_exposure_time);

	/* Loop taking adaptive exposures until they have all been taken */
	while (	expired  == 0 )
	{
		/* Check abort status */
		if(CCD_DSP_Get_Abort())
		{
			Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
			Multrun_Error_Number = 24;
			error=AbortAcquisition();
			sprintf(Multrun_Error_String,"CCD_Multflat_Expose:Aborted.");
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multflat_Expose:Aborted. RC %d",
					      error);
#endif
			return FALSE; 
		}   

		/* Check that we haven't overran our time. */
		if (ExpiredStatus(Multrun_Data.timeStart,Multrun_Data.maxTime)==1) 
		{
			expired = 1;
			continue;
		}
	 
		/* if the exposure time is out of range, wait for a bit to try again */
		if(expose_exposure_time < (mrParams.minExposure/bin) || expose_exposure_time > mrParams.maxExposure) 
		{
#if LOGGING > 3
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				     "CCD_Multflat_Expose:Calculating %.3f sec required for target in range [%ld,%ld]",
				       expose_exposure_time,mrParams.minExposure/bin,mrParams.maxExposure); 
			CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				       "CCD_Multflat_Expose:Waiting 15 seconds to try again");
#endif
			/* loop for 15 seconds, but test for abort or timeout every 1 sec */
			for(i=0;i<15;i++)
			{
				sleep (1);
				/* Check abort status */
				if(CCD_DSP_Get_Abort())
				{
					Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
					Multrun_Error_Number = 25;
					error=AbortAcquisition();
					sprintf(Multrun_Error_String,"CCD_Exposure_Expose:Aborted.");
#if LOGGING > 1
					CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
							      "CCD_Multflat_Expose:Aborted. RC %d",error);
#endif
					return FALSE;
				}   
				/* Check that we haven't overran our time. */
				if (ExpiredStatus(Multrun_Data.timeStart,Multrun_Data.maxTime)==1) 
				{
					expired = 1;
					continue;
				}
			} 
			/* Re-test with the inital exposure time */
			expose_exposure_time = initialExp;
		}

		/* Test all the return conditions */
#if LOGGING > 1
		CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multflat_Expose:Acquisition restarted...");
#endif
		error = expose(expose_exposure_time,CCD_Setup_Get_NCols(),CCD_Setup_Get_NRows(),
			       remainingExposures,&recalculate_exposure_length);
	        if(recalculate_exposure_length) 
		{
			expose_exposure_time = getNewExposureTime(Multrun_Data.median_value,
								  Multrun_Data.Exposure_Length);
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				 "CCD_Multflat_Expose:Counts out of range (%.2f)... %.3f sec required for target",
					      Multrun_Data.median_value,expose_exposure_time); 
#endif
		}
		remainingExposures = remainingExposures - Multrun_Data.lastMultrunExposures;

		/* Check abort status */
		if(CCD_DSP_Get_Abort())
		{
			Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
			Multrun_Error_Number = 26;
			error=AbortAcquisition();
			sprintf(Multrun_Error_String,"CCD_Exposure_Expose:Aborted.");
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
					      "CCD_Multflat_Expose:Aborted. RC %d",error);
#endif
			return FALSE; 
		} 
	} /* End of while loop ~line 209 */
#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			   "CCD_Multflat_Expose: --- Finished %ld mult-flat in %d sec, last exp time: %.4f sec ---",
			      Multrun_Data.lastMultrunExposures,(int)(time(NULL)-Multrun_Data.timeStart),
			      Multrun_Data.Exposure_Length);
#endif
#if LOGGING > 1
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"CCD_Multflat_Expose:Finished.");
#endif

	return (TRUE);
}

/**
 * Internal expose routine. Used for both MULTRUN and MULTFLAT exposures.
 * @param exposure The exposure length.
 * @param width Width of image to read out.
 * @param height Height of image to read out.
 * @param nimages Number of images to take.
 * @param recalculate_exposure_length Address of an integer. Set on return to TRUE if we need to recalculate
 *        the exposure length, and FALSE if we do not.
 * @return Returns TRUE on success and FALSE on failure.
 */
unsigned int expose(float exposure, int width, int height,long nimages,int *recalculate_exposure_length)
{	
	char *pcft,current_filetime[64];
	char outfile[64],*poutfile=outfile;	
	char full_filename[128];
	char *pcomment,comment[80];
	int status,error;
	int bin = CCD_Setup_Get_NSBin();
	long pixels=width*height;
	long medianPixels = pow((2*mrParams.halfBoxSize+1),2);
	unsigned long *longarray = NULL;  
	double *median_array = NULL;
	int j;
	struct timespec waittime;
	long lastseries=-1,series=0;
	long first,last;
	long images_remaining = nimages-series;
	long buffer_images_remaining = 0,buffer_images_retrieved=0;
	float KinExposure,KinAccumulate,KinKineticCT;
	float TimeSinceLastImage=0;
	char exposure_start_time_string[64];
	float speeds[3],tempSpeed; /* Array to store the shift speeds */
	int maxVShiftIndex=0,maxHShiftIndex=0;

	/* Set the timers for writing the headers */
	struct timespec mr_current_time;

#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose started: %d ms for %d images.",
			      exposure,nimages);
#endif
	if(recalculate_exposure_length == NULL)
	{
		Multrun_Error_Number = 1;
		sprintf(Multrun_Error_String,"expose:recalculate_exposure_length is NULL.");
#if LOGGING > 1
		CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:recalculate_exposure_length is NULL.");
#endif
		return FALSE;
	}
	(*recalculate_exposure_length) = FALSE;
	/* Reset the number of images taken this cycle */
	Multrun_Data.lastMultrunExposures = 0; 
	
	/* Allocate the memory for the arrays */ 
	longarray=(unsigned long*)malloc(pixels*sizeof(unsigned long));	
	median_array=(double *)malloc(medianPixels*sizeof(double));	
	
	/* Check the Allocated memory */
	if(longarray==NULL || median_array==NULL)
	{
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "expose:ERROR: Memory allocation error in expose(%d,%p,%d,%p)",
				      pixels,longarray,medianPixels,median_array);
#endif
		Multrun_Error_Number = 2;
		sprintf(Multrun_Error_String,"expose:ERROR: Memory allocation error in expose(%ld,%p,%ld,%p)",
			pixels,(void*)longarray,medianPixels,(void*)median_array);
		return FALSE; 
	}

	pcft = current_filetime;
	pcomment=comment;
	/* Get comment for header */
	sprintf(pcomment,"Test");
	pcomment=chomp(comment);

	/* Reset the abort flag */
	CCD_DSP_Set_Abort(FALSE);
	Multrun_Error_Number = 0; /* LT extern variable? */ 

	/* Set up exposure */
	SetAcquisitionMode(5); /* Run til abort */
        SetFrameTransferMode(1);
	Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_WAIT_START;
	SetExposureTime(exposure);

	/* Store the REQUESTED exposure time in the header */
	Multrun_Data.requestedExposureTime = exposure;
	SetNumberAccumulations(1); /* Don't add images together */

	/* Get a list of the possible speeds */
#if LOGGING > 3
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Possible H-Shift speeds  ");
#endif
	j = 0;
	while (GetHSSpeed(0,0,j,&tempSpeed) == DRV_SUCCESS && j<10)
	{
#if LOGGING > 3
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:%d:%.2f ",j,tempSpeed);
#endif
		j++;
	}
	maxHShiftIndex = j;
#if LOGGING > 3
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Possible V-Shift speeds  ");
#endif
	j = 0;
	while (GetVSSpeed(j,&tempSpeed) == DRV_SUCCESS && j<10)
	{
#if LOGGING > 3
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:%d:%.2f ",j,tempSpeed);
#endif
		j++;
	}
	maxVShiftIndex = j;

	/* Get the verical shift speeds */
	GetVSSpeed(0,&speeds[0]);
	GetHSSpeed(0,0,0,&speeds[1]);
#if LOGGING > 3
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			      "expose:Two fastest shift speeds are V: %.2f and H: %.2f",
			      speeds[0],speeds[1]);
#endif
	/* Set the shift speeds to the fastest */
	SetVSSpeed(0);
	SetHSSpeed(1,0);
	/* Send the shift speeds to the Multrun_Data structure so that they can be used elsewhere */
	Multrun_Data.VSspeed = speeds[0];
	Multrun_Data.HSspeed = speeds[1];


	/* Get the driver set acquisition timimgs */
	GetAcquisitionTimings(&KinExposure,&KinAccumulate,&KinKineticCT);
#if LOGGING > 3
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			      "expose:GetAcquisitionTimings  EXP:%.3f ACC:%.3f KCT:%.3f",
			      KinExposure,KinAccumulate,KinKineticCT);
#endif
	/* Work out the correction made to the image epoch time */	
	Multrun_Data.TimeCorrection = start_time_correction(KinExposure);

	/* Export value to local structure for other functions */
	Multrun_Data.Exposure_Length = KinExposure; 

#if LOGGING > 3
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Image WxH:  %d %d  Binning %dx%d",
			      width,height,bin,bin);
  	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			      "expose:Imaging for %.2f secs (adjusted to %f secs)",
			      exposure,Multrun_Data.Exposure_Length);
  	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Acquiring %ld images",nimages);
#endif
	Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_PRE_READOUT;

	/* Get the current high-res time for start of acquistion */
	clock_gettime(CLOCK_REALTIME,&(Multrun_Data.LastImageTime));
	Exposure_TimeSpec_To_UtStart_String(Multrun_Data.LastImageTime,exposure_start_time_string);

	/* Temp for header; will be approx correct if writeout is close to acquisition */
	/* Temperature cannot be probed during an Andor acquisition */
	CCD_Temperature_Get(&(Multrun_Data.temperature));

        GetStatus(&status);
#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Current Status: %d",status);
#endif
	/* Grab the NTP drift stats */
	error = getNtpDriftFile(mrParams.ntpDriftFile);
#if LOGGING > 3
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			      "expose:NTP Date: %s  server: %s  uncertainty: %.0f ms, error %d",
			      Multrun_Data.ntpTime,Multrun_Data.ntpServer,Multrun_Data.ntpDrift,error);
#endif

	/* Start the acquisition */
	error = StartAcquisition();

	/* Wait 0.50 seconds */
	waittime.tv_sec = 0;
	waittime.tv_nsec = 500000000; 
	nanosleep(&waittime, NULL); /* Sleep for a bit */
#if LOGGING > 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Acquisition started %s UT  RC: %d",
			      exposure_start_time_string,error);
#endif

	/*Loop while the driver is working, or theere are buffer stored images or images still to be taken */

        GetStatus(&status);
	/* set Wait 0.05 seconds */
	waittime.tv_sec = 0;
	waittime.tv_nsec = 50000000; 
	/* May cause the driver to acquire more imageg that requested if DRV_ACQUIRING stcs for a second or two 
	   while(status==DRV_ACQUIRING || buffer_images_remaining>0 || images_remaining >0){ */
	while( (status==DRV_ACQUIRING && images_remaining >0) || buffer_images_remaining>0 )
	{
		nanosleep(&waittime, NULL); /* Sleep for a bit to prevent system hogging */
		clock_gettime(CLOCK_REALTIME,&mr_current_time);
		TimeSinceLastImage = (mr_current_time.tv_sec + mr_current_time.tv_nsec/1e9) 
			- (Multrun_Data.LastImageTime.tv_sec + Multrun_Data.LastImageTime.tv_nsec/1e9);

		/* Check tht we should be taking images. If not, abort */
		if (images_remaining == 0 )
		{
			error=AbortAcquisition();
		}


		/* Check for a driver time-out. This happens if the last image was taken too long ago */ 
		/* if ( status==DRV_ACQUIRING && ( TimeSinceLastImage > EXPOSURE_READ_TIMEOUT + KinExposure) ){ */
		if ( ( TimeSinceLastImage > EXPOSURE_READ_TIMEOUT + KinExposure) )
		{
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Pre-Abort Status: %d",
					      status);
#endif
			error=AbortAcquisition();
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
					      "expose:WARNING: Acquisition timed out, AbortAcquisition RC: %d",
					      error);
#endif
			Multrun_Error_Number = 3;
			sprintf(Multrun_Error_String,"expose:ERROR: Acquisition timed out, "
				"%.2f > %d + %.2f, error = %d",
				TimeSinceLastImage,EXPOSURE_READ_TIMEOUT,KinExposure,error);
			return FALSE;
		}

		GetTotalNumberImagesAcquired(&series); 

		if(CCD_DSP_Get_Abort())
		{
			Multrun_Data.Exposure_Status = CCD_EXPOSURE_STATUS_NONE;
			error=AbortAcquisition();
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose:Aborted. RC %d",
					      error);
#endif
			FreeInternalMemory();
			Multrun_Error_Number = 27;
			sprintf(Multrun_Error_String,"expose:Aborted: return code %d.",error);
			return FALSE;
		}

		if( GetNumberNewImages(&first,&last)==DRV_SUCCESS)
		{
			buffer_images_remaining = last-first; 
			images_remaining = nimages-series;
#if LOGGING > 1
			CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			    "expose:--- Image: %ld of %ld  Left: %ld  Buff: %ld TSLI: %.3f EXP: %.3f ---",
					      series,nimages,images_remaining,buffer_images_remaining,
					      TimeSinceLastImage,KinExposure);
#endif
			/* Get the image data and write to file	  */
			error=GetOldestImage((long*)longarray, pixels);
			if(error==20067)
			{
				AbortAcquisition();
#if LOGGING > 1
				CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
						      "expose: Array size not valid(%d).",pixels);
#endif
				return FALSE; 
			} 

			if (error==DRV_SUCCESS)
			{
				/* The first image is the Multrun start time - Need to correct for readout */
				if(series==1)
				{
					clock_gettime(CLOCK_REALTIME,&(Multrun_Data.Multrun_Start_Time));
					correct_start_time(&(Multrun_Data.Multrun_Start_Time));   
				}

				clock_gettime(CLOCK_REALTIME,&(Multrun_Data.LastImageTime));
				clock_gettime(CLOCK_REALTIME,&(Multrun_Data.Exposure_Epoch_Time));
				clock_gettime(CLOCK_REALTIME,&(Multrun_Data.Exposure_Start_Time));
				correct_start_time(&(Multrun_Data.Exposure_Start_Time));
				Exposure_TimeSpec_To_Date_Obs_String(Multrun_Data.Exposure_Start_Time,exposure_start_time_string);

				/* Grab the median value from central pixels.  */
				Multrun_Data.median_value=-1.0;

				getSquareRegion((long*)longarray,median_array,floor(mrParams.posBoxX/bin),floor(mrParams.posBoxY/bin),mrParams.halfBoxSize);
				Multrun_Data.median_value=median(median_array,medianPixels);

				if (strcmp(fileHeaders.obstype,"SKYFLAT")==0) 
				{   /* If we are doing a flat ...*/
#if LOGGING > 3
					CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
						      "expose:%ld pixel image median: %.2f (target %d)",
						      medianPixels,Multrun_Data.median_value,mrParams.flatTarget*bin);
#endif
					/* Check that the exposure median is within limits. If not, set
					** recalculate flag and return. */   
					if(Multrun_Data.median_value< mrParams.minFlatCountsRecalc*bin || 
					   Multrun_Data.median_value>mrParams.maxFlatCountsRecalc*bin)
					{
#if LOGGING > 3
						CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
								      "expose:Median:"
								      " (%.2f) outside RECALC range %d < MEDIAN < %d",
								      Multrun_Data.median_value,
								      mrParams.minFlatCountsRecalc*bin,
								      mrParams.maxFlatCountsRecalc*bin); 
#endif
						error=AbortAcquisition(); 
#if LOGGING > 3
						CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
								      "expose:"
						      "Median %.2f outside SAVE range %d < MEDIAN < %d, NOT saving",
								      Multrun_Data.median_value,
							    mrParams.minFlatCounts*bin,mrParams.maxFlatCounts*bin); 
#endif
						(*recalculate_exposure_length) = TRUE;
						return TRUE;
					}
				}
				else /* Else assume normal exposure */
				{
#if LOGGING > 3
					CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
							      "expose:%ld pixel image median: %.2f",
							      medianPixels,Multrun_Data.median_value);
#endif
				}
				buffer_images_retrieved++;
	
				/* Check if this is the start of a multrun. If it is, get the next MR number */	
				if(buffer_images_retrieved==1)
				{
					if(!getNextFilename(poutfile,1))
					{
#if LOGGING > 1
						CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
							 "expose:getNextFilename failed:Error(%d):%s",
							 Multrun_Error_Number,Multrun_Error_String);
#endif
						return FALSE;
					}
				}
				else 
				{
					if(!getNextFilename(poutfile,0))
					{
#if LOGGING > 1
						CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
							 "expose:getNextFilename failed:Error(%d):%s",
							 Multrun_Error_Number,Multrun_Error_String);
#endif
						return FALSE;
					}
				}
	
				/* generate the filename */
				sprintf(full_filename,"%s/%s",IMAGEDIR,poutfile);
#if LOGGING > 1
				CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
						      "expose:Writing out %s  %s to disk",
					 exposure_start_time_string,full_filename);
#endif
				if(!Multrun_Exposure_Save(full_filename,longarray,width,height))
				{
#if LOGGING > 1
					CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				       "expose:Multrun_Exposure_Save failed to save %s of dimensions (%d,%d) : "
				       "Error(%d): %s.",full_filename,width,height,
							      Multrun_Error_Number,Multrun_Error_String);
#endif
					return FALSE;
				}

				/* Let other funcs know how many were caught */
				Multrun_Data.lastMultrunExposures = series; 

				/* Check that we haven't overran our time. */
				if (ExpiredStatus(Multrun_Data.timeStart,Multrun_Data.maxTime)==1) 
				{
#if LOGGING > 1
					CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,
						       "expose:Multrun Completed in requested time");
#endif
					return TRUE;
				}
			} /* successful check for new image in buffer */

			lastseries=series;  
		} /* Conditional check for new images */

		/* Abort after full number of images taken */
		if(series>=nimages) 
			AbortAcquisition();
		GetStatus(&status);      
	} 

	nanosleep(&waittime,NULL); /* Wait a bit at end */	
	free (longarray); 
	free (median_array); 

#if LOGGING > 1
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"expose finished.");
#endif
	return TRUE;
}


int ExpiredStatus ( time_t start, long length )
{
	/* Checks that the current time against the start time. If it is greater than `length' seconds we return true, 
	   otherwise we return false. Also, return expired if taking a flat at the last exposure time +100% will
	   cause overrun. */

	time_t now = time(NULL);
	if (Multrun_Data.isMultFlat == 0) return 0;    /* Not applicable if not multflat */
	if ( (now-start) > (length-2*Multrun_Data.Exposure_Length) ) return 1;
	else return 0; 

}


int getNtpDriftFile (char *file){
	/* Gets the value of the NTP error from a system file call.
	   Preformatted file method. This uses a file with the format
	   DATE - NTPSERVER - ERROR  
	   which is generated from a Perl file running through cron. This prevents
	   this code stopping should the system ntpstat command stall, or 
	   recompiling should the format change (we just change the perl file). 
	   NOTE:  The output is written to the Multrun_Data structure */
	/* Exposure_data variables:  ntpTime, ntpServer and ntpDrift */      
    
	FILE *fp = NULL;
	char line[256];
	char *pline = NULL;
	char *delim = "-"; /* The ntp file delimiters */

	if(!(fp=fopen(file,"r")))
	{
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNtpDriftFile : WARNING : cannot open %s",file);
#endif
		return 0;
	}

	fgets(line,256,fp); /* Grab the first line from the file */
	/* Within that line, grab each string before the delimiter, 
	   i.e. the string 'date', ntp server and ntp error */
	pline=strtok(line,delim);
	if(pline!=NULL)strncpy(Multrun_Data.ntpTime,pline,256);
	pline=strtok(NULL,delim);
	if(pline!=NULL)strncpy(Multrun_Data.ntpServer,pline,256);
	pline=strtok(NULL,delim);
	if(pline!=NULL)Multrun_Data.ntpDrift=atof(pline);
	return 0;
}


int getNtpDriftInternal(char *server, float *drift)
{
	/* Gets the value of the estimated drift from the NTP daemon by
	   passing the output through popen. Not currently implemented
	   as a timeout here cannot be implemented without threads. */

	FILE *fpipe;
	char *command = "ntpstat";
	int from=0, to=0;
	char line[256];
	int length = 0;
	char *pto,*pfrom;
	char driftstring[16];

	if(! (fpipe = (FILE*)popen(command,"r")) )
	{
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "getNtpDriftInternal:Unable to open ntpstat for ntp time!"); 
#endif
		return (-1); 
	}

	while ( fgets(line, sizeof(line),fpipe) )
	{
		/* do stuff with the line */

		if ( strstr(line,"unsynchronised")!=NULL ) 
		{
			strncpy(server,"unsynchronised",32);
			*drift = -999.0; 
		}
		else if ( strstr(line,"NTP server")!=NULL )
		{
			from = (int)strcspn(line,"(");
			to   = (int)strcspn(line,")");
			if (to>=from)
			{
				strncpy(server,line+from+1,(to-from-1)); 
				server[to-from]='\0'; 
			}
			else 
			{
				strcpy(server,"unknown");
				*drift = -999.0; 
			}
		}
		else if ( strstr(line,"time correct to within")!=NULL ) 
		{
			pfrom = strstr(line,"within");
			pfrom+=6; /* get the end of within length(within)=6 */
			pto = strstr(line," ms");
			length=strlen(pfrom)-strlen(pto);
			if (length > 0 ) 
			{
				strncpy(driftstring,pfrom,length); 
				driftstring[length] = '\0';
				*drift = atof(driftstring); 
			}
		}
	}
	pclose(fpipe);
	return 0;
}


float getNewExposureTime( double oldCounts, float oldExposure){
	/* This function calculates the approximate exposure time required for a target value of counts. */
 
	int bin = CCD_Setup_Get_NSBin();
	float newtime =  oldExposure*(mrParams.flatTarget*bin - (mrParams.biasLevel))/(oldCounts - (mrParams.biasLevel) );

	/* Times allowed ito prevent buffer overrun */ 
	if (bin == 1 && newtime < 1.5 ) newtime = 1.5;
	if (bin == 2 && newtime < 0.8 ) newtime = 0.8;

	if (bin == 1 && oldCounts > 23500.0) 
	{
		newtime = 1.50;
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "getNewExposureTime:Near saturation limit, trying 1.50 seconds");
#endif
	}
 
	else if (bin == 2 && oldCounts > 65500.0) {
		newtime = 0.8;
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "getNewExposureTime:Near saturation limit, trying 0.8 seconds");
#endif
	}
 
	else if (bin == 1 && oldCounts < 800.0) {
		newtime = 15.0;
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "getNewExposureTime:Close to bias level, trying 15.0 seconds");
#endif
	}
 
	else if (bin == 2 && oldCounts < 800.0) {
		newtime = 10.0;
#if LOGGING > 1
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "getNewExposureTime:Close to bias level, trying 10.0 seconds");
#endif
	}
 
	return (newtime);
}


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
int Multrun_Exposure_Save(char *filename, unsigned long *exposure_data,int ncols,int nrows)
{
	fitsfile *fp = NULL;
	int retval=0,status=0;
	char buff[32]; /* fits_get_errstatus returns 30 chars max */
	long naxes[2];
	char exposure_start_time_string[64];
	char exposure_epoch_time_string[64];
	double mjd;

#if LOGGING > 4
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"Exposure_Save:Started.");
#endif
	
	naxes[0] = (long)ncols; naxes[1] = (long)nrows;

	/* try to open file */

	retval = fits_create_file(&fp, filename, &status);
	retval = fits_create_img(fp, LONG_IMG, 2, naxes, &status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		Multrun_Error_Number = 53;
		sprintf(Multrun_Error_String,"Exposure_Save: File open failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
	
	/* write the data */
	retval = fits_write_img(fp,TULONG,1,ncols*nrows,exposure_data,&status); 
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 54;
		sprintf(Multrun_Error_String,"Exposure_Save: File write failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
	
#if HEADERS > 0
	/*  Write out the headers... this will be LONG! */

	/* Add comments noting that most headers will be static at start of multrun  */

	/* update COMMENT keyword  */
	retval = fits_update_key(fp,TSTRING,"COMMENT1","Most headers are only updated at the start of the multrun",
				 "",&status);
	retval = fits_update_key(fp,TSTRING,"COMMENT2","MR based headers and EXPOSED are updated per exposure",
				 "",&status);
	retval = fits_update_key(fp,TSTRING,"COMMENT2","Telescope pointing, status and CCDATEMP etc are static at MULTRUN start.", "",&status);


	/* update DATE keyword */
	if(exposure_start_time_string==NULL) strncpy(exposure_start_time_string,"UNKNOWN-NULL",80);
	Exposure_TimeSpec_To_Date_String(Multrun_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"DATE",exposure_start_time_string,"Exposure start",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 55;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating DATE failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}

	/* update DATE-OBS keyword */
	if(exposure_start_time_string==NULL) strncpy (exposure_start_time_string,"UNKNOWN-NULL",80);
	Exposure_TimeSpec_To_Date_Obs_String(Multrun_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"DATE-OBS",exposure_start_time_string,"Date of observation",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 56;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating DATE-OBS failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}
	/* update UTSTART keyword */
	if(exposure_start_time_string==NULL) strncpy(exposure_start_time_string,"UNKNOWN-NULL",80);
	Exposure_TimeSpec_To_UtStart_String(Multrun_Data.Exposure_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"UTSTART",exposure_start_time_string,"Start of observation",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 57;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating UTSTART failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}
	/* update MJD keyword 
	** note leap second correction not implemented yet (always FALSE). */
	if(exposure_start_time_string==NULL) strncpy(exposure_start_time_string,"UNKNOWN-NULL",80);
	Exposure_TimeSpec_To_Mjd(Multrun_Data.Exposure_Start_Time,FALSE,&mjd);
	retval = fits_update_key_fixdbl(fp,"MJD",mjd,6,NULL,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 58;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating MJD failed(%.2f,%s,%d,%s).",mjd,filename,
			status,buff);
		return FALSE;
	}

	/* update MRSTART keyword, defining the start time of the multrun cycle */
	if(exposure_start_time_string==NULL) strncpy(exposure_start_time_string,"UNKNOWN-NULL",80);
	Exposure_TimeSpec_To_Date_Obs_String(Multrun_Data.Multrun_Start_Time,exposure_start_time_string);
	retval = fits_update_key(fp,TSTRING,"MRSTART",exposure_start_time_string,"Time of start of Multrun",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 28;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating MRSTART failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update CCDATEMP keyword  */
	if(&(Multrun_Data.temperature)==NULL) Multrun_Data.temperature = -9998.0;
	retval = fits_update_key_fixdbl(fp,"CCDATEMP",Multrun_Data.temperature,3,"CCD Temperature at START of multrun",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 62;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CCDATEMP failed(%.2f,%s,%d,%s).",
			Multrun_Data.temperature,filename, status,buff);
		return FALSE;
	}

	/* update FILENAME keyword  */
	if(filename==NULL) strncpy(filename,"UNKNOWN-NULL",80);;
	retval = fits_update_key(fp,TSTRING,"FILENAME",filename,"Current filename",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 63;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating FILENAME failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update EXPTIME keyword  */
	if(&(Multrun_Data.Exposure_Length)==NULL) Multrun_Data.Exposure_Length = -9998.0;
	retval = fits_update_key_fixdbl(fp,"EXPTIME",Multrun_Data.Exposure_Length,4,"Andor Corrected (true) exposure time ",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 29;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EXPTIME failed(%.2f,%s,%d,%s).",
			Multrun_Data.Exposure_Length,filename, status,buff);
		return FALSE;
	}

	/* update REQEXP keyword  */
	if(&(Multrun_Data.requestedExposureTime)==NULL) Multrun_Data.requestedExposureTime = -9998.0;
	retval = fits_update_key_fixdbl(fp,"REQEXP",Multrun_Data.requestedExposureTime,4,"Exposure time requested by user",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 30;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EXPTIME failed(%.2f,%s,%d,%s).",
			Multrun_Data.Exposure_Length,filename, status,buff);
		return FALSE;
	}

	/* update MEDIAN keyword  */
	if(&(Multrun_Data.median_value)==NULL) Multrun_Data.median_value = -9998.0;
	retval = fits_update_key_fixdbl(fp,"MEDIAN",Multrun_Data.median_value,6,"The approx median of the centre values",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 64;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating MEDIAN failed(%.2f,%s,%d,%s).",
			Multrun_Data.median_value,filename, status,buff);
		return FALSE;
	}

	/* update TIMECORR keyword  */
	if(&(Multrun_Data.TimeCorrection)==NULL) Multrun_Data.TimeCorrection = -9998.0;
	retval = fits_update_key_fixdbl(fp,"TIMECORR",Multrun_Data.TimeCorrection,0,"Time correction in ns for readout, FT and exposure",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 65;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating TIMECORR failed(%.2f,%s,%d,%s).",
			Multrun_Data.TimeCorrection,filename, status,buff);
		return FALSE;
	}

	/* update EXPEPOCH keyword, defining the start time of the multrun cycle */
	/*if(&(Multrun_Data.Exposure_Epoch_Time)==NULL) Multrun_Data.Exposure_Epoch_Time = 0; */
	Exposure_TimeSpec_To_Date_Obs_String(Multrun_Data.Exposure_Epoch_Time,exposure_epoch_time_string);
	retval = fits_update_key(fp,TSTRING,"EXPEPOCH",exposure_epoch_time_string,"Actual end of exposure time",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 60;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EXPEPOCH failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update CCDXBIN keyword  */
	retval = fits_update_key_fixdbl(fp,"CCDXBIN",(float)CCD_Setup_Get_NSBin(),0,"Column binning",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 31;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CCDXBIN failed(%.2f,%s,%d,%s).",
			(float)CCD_Setup_Get_NSBin(),filename, status,buff);
		return FALSE;
	}

	/* update CCDYBIN keyword  */
	retval = fits_update_key_fixdbl(fp,"CCDYBIN",(float)CCD_Setup_Get_NPBin(),0,"Row binning",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 32;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CCDYBIN failed(%.2f,%s,%d,%s).",
			(float)CCD_Setup_Get_NSBin(),filename, status,buff);
		return FALSE;
	}

	/* update OBSTYPE keyword  */
	if(fileHeaders.obstype==NULL) strncpy(fileHeaders.obstype, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"OBSTYPE",fileHeaders.obstype,"Observation type",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 33;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating OBSTYPE failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update RUNNUM keyword  */
	if(&(ff.multRunNumber)==NULL) ff.multRunNumber = -9998.0;
	retval = fits_update_key_fixdbl(fp,"RUNNUM",(float)(ff.multRunNumber),0,"Multrun Number",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 34;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating RUNNUM failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update EXPNUM keyword  */
	if(&(ff.runNumber)==NULL) ff.runNumber = -9998.0;
	retval = fits_update_key_fixdbl(fp,"EXPNUM",(float)(ff.runNumber),0,"Number of exposure in Multrun",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 35;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EXPNUM failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update RA keyword  */
	if(fileHeaders.ra==NULL) strncpy(fileHeaders.ra, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"RA",fileHeaders.ra,"Telescope returned RA",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 36;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating RA failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update DEC keyword  */
	if(fileHeaders.dec==NULL) strncpy(fileHeaders.dec, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"DEC",fileHeaders.dec,"Telescope returned DEC",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 37;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating DEC failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update LATITUDE keyword  */
	if(fileHeaders.latitude==NULL) strncpy(fileHeaders.latitude, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"LATITUDE",atof(fileHeaders.latitude),4,"Latitude of telescope",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 38;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating LATITUDE failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.latitude),filename, status,buff);
		return FALSE;
	}


	/* update LONGITUDE keyword  */
	if(fileHeaders.longitude==NULL) strncpy(fileHeaders.longitude, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"LONGITUD",atof(fileHeaders.longitude),4,"Longitude of telescope",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 39;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating LONGITUD failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.longitude),filename, status,buff);
		return FALSE;
	}

	/* update NTPTIME keyword  */
	if(Multrun_Data.ntpTime==NULL) strncpy(Multrun_Data.ntpTime, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"NTPTIME",Multrun_Data.ntpTime,"Last time NTP status was checked",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 40;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating NTPTIME failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update NTPSERVE keyword  */
	if(Multrun_Data.ntpServer==NULL) strncpy(Multrun_Data.ntpServer, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"NTPSERVE",Multrun_Data.ntpServer,"Address of ntp server",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 41;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating NTPSERVE failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}
	/* update NTPERROR keyword  */
	if(&(Multrun_Data.ntpDrift)==NULL) Multrun_Data.ntpDrift = -9998.0;
	retval = fits_update_key_fixdbl(fp,"NTPERROR",Multrun_Data.ntpDrift,3,"Uncertainty in ntp time in msec",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 42;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating NTPERROR failed(%.2f,%s,%d,%s).",
			Multrun_Data.temperature,filename, status,buff);
		return FALSE;
	}
	/* update AIRMASS keyword  */
	if(fileHeaders.airmass==NULL) strncpy(fileHeaders.airmass, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"AIRMASS",atof(fileHeaders.airmass),4,"The airmass",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 43;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating AIRMASS failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.airmass),filename, status,buff);
		return FALSE;
	}
	/* update TELFOCUS keyword  */
	if(fileHeaders.telfocus==NULL) strncpy(fileHeaders.telfocus, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"TELFOCUS",atof(fileHeaders.telfocus),4,"The focus position of telescope in m",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 44;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating TELFOCUS failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.telfocus),filename, status,buff);
		return FALSE;
	}
	/* update VSSPEED keyword  */
	if(&(Multrun_Data.VSspeed)==NULL) Multrun_Data.VSspeed = -9998.0;
	retval = fits_update_key_fixdbl(fp,"VSSPEED",Multrun_Data.VSspeed,1,"Andor Verical Shift speed in us per pixel",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 45;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating VSSPEED failed(%.2f,%s,%d,%s).",
			Multrun_Data.VSspeed,filename, status,buff);
		return FALSE;
	}
	/* update HSSPEED keyword  */
	if(&(Multrun_Data.HSspeed)==NULL) Multrun_Data.HSspeed =-9998;
	retval = fits_update_key_fixdbl(fp,"HSSPEED",Multrun_Data.HSspeed,1,"Andor Horizontal Shift speed in us per pixel",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 46;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating HSSPEED failed(%.2f,%s,%d,%s).",
			Multrun_Data.HSspeed,filename, status,buff);
		return FALSE;
	}
	/* update CONFIGID keyword  */
	if(fileHeaders.configid==NULL) strncpy(fileHeaders.configid, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"CONFIGID",atoi(fileHeaders.configid),0,"Unique configuration ID.",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 66;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CONFIGID failed(%d,%s,%d,%s).",
			atoi(fileHeaders.configid),filename, status,buff);
		return FALSE;
	}

	/* update ORIGIN keyword  */
	if(fileHeaders.origin==NULL) strncpy(fileHeaders.origin, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"ORIGIN",fileHeaders.origin,"",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 47;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ORIGIN failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update INSTATUS keyword  */
	if(fileHeaders.instatus==NULL) strncpy(fileHeaders.instatus, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"INSTATUS",fileHeaders.instatus,"The instrument status.",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 48;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ORIGIN failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update TELESCOP keyword  */
	if(fileHeaders.telescop==NULL) strncpy(fileHeaders.telescop, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"TELESCOP",fileHeaders.telescop,"",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 49;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating TELESCOP failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update TELMODE keyword  */
	if(fileHeaders.telmode==NULL) strncpy(fileHeaders.telmode, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"TELMODE",fileHeaders.telmode,"",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 50;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating TELMODE failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update LST keyword  */
	if(fileHeaders.lst==NULL) strncpy(fileHeaders.lst, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"LST",fileHeaders.lst,"[hours] As retrieved from the TCS",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 51;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating LST failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update CAT-RA keyword  */
	if(fileHeaders.catra==NULL) strncpy(fileHeaders.catra, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"CAT-RA",fileHeaders.catra,"[hours] Source catalogue position",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 52;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CAT-RA failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update CAT-DEC keyword  */
	if(fileHeaders.catdec==NULL) strncpy(fileHeaders.catdec, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"CAT-DEC",fileHeaders.catdec,"[hours] Source catalogue position",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 61;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CAT-DEC failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update TELSTAT keyword  */
	if(fileHeaders.telstat==NULL) strncpy(fileHeaders.telstat, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"TELSTAT",fileHeaders.telstat,"Status of telescope",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 67;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CAT-DEC failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update AUTOGUID keyword  */
	if(fileHeaders.autoguid==NULL) strncpy(fileHeaders.autoguid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"AUTOGUID",fileHeaders.autoguid,"",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 68;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating AUTOGUID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update ROTMODE keyword  */
	if(fileHeaders.rotmode==NULL) strncpy(fileHeaders.rotmode, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"ROTMODE",fileHeaders.rotmode,"Rotator mode",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 69;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ROTMODE failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update ROTSKYPA keyword  */
	if(fileHeaders.rotskypa==NULL) strncpy(fileHeaders.rotskypa, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"ROTSKYPA",atof(fileHeaders.rotskypa),7,"[degrees] Turntable position angle",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 70;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ROTSKYPA failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.rotskypa),filename, status,buff);
		return FALSE;
	}


	/* update WINDSPEE keyword  */
	if(fileHeaders.windspee==NULL) strncpy(fileHeaders.windspee, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"WINDSPEE",atof(fileHeaders.windspee),7,
					"[m/s] Recorded by WMS, at start of exposure",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 71;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating WINDSPEE failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.windspee),filename, status,buff);
		return FALSE;
	}

	/* update REFTEMP keyword  */
	if(fileHeaders.wmstemp==NULL) strncpy(fileHeaders.wmstemp,"UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"WMSTEMP",atof(fileHeaders.wmstemp),7,
					"[Kelvin] Current external temperature",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 72;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating WMSTEMP failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.wmstemp),filename, status,buff);
		return FALSE;
	}

	/* update WMSHUMID keyword  */
	if(fileHeaders.wmshumid==NULL) strncpy(fileHeaders.wmshumid, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"WMSHUMID",atof(fileHeaders.wmshumid),7,
					"[percent] Current percentage humidity",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 73;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating WMSHUMID failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.wmshumid),filename, status,buff);
		return FALSE;
	}

	/* update OBJECT keyword  */
	if(fileHeaders.object==NULL) strncpy(fileHeaders.object, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"OBJECT",fileHeaders.object,"Object Name",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 74;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating OBJECT failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update INSTRUME keyword  */
	if(fileHeaders.instrument==NULL) strncpy(fileHeaders.instrument, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"INSTRUME",fileHeaders.instrument,"Instrument",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 75;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating INSTRUME failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update CONFNAME keyword  */
	if(fileHeaders.confname==NULL) strncpy(fileHeaders.confname, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"CONFNAME",fileHeaders.confname,"Config in use",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 76;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CONFNAME failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update DETECTOR keyword  */
	if(fileHeaders.detector==NULL) strncpy(fileHeaders.detector, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"DETECTOR",fileHeaders.detector,"Detector",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 77;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CONFNAME failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update GAIN keyword  */
	if(fileHeaders.gain==NULL) strncpy(fileHeaders.gain, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"GAIN",atof(fileHeaders.gain),7,
					"[unknown]",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 78;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GAIN failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.gain),filename, status,buff);
		return FALSE;
	}

	/* update READNOIS keyword  */
	if(fileHeaders.readnoise==NULL) strncpy(fileHeaders.readnoise, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"READNOIS",atof(fileHeaders.readnoise),7,
					"[unknown]",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 79;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating READNOIS failed(%.2f,%s,%d,%s).",
			atof(fileHeaders.readnoise),filename, status,buff);
		return FALSE;
	}

	/* update TAGID keyword  */
	if(fileHeaders.tagid==NULL) strncpy(fileHeaders.tagid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"TAGID",fileHeaders.tagid,"TAG ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 80;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating TAGID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update USERID keyword  */
	if(fileHeaders.userid==NULL) strncpy(fileHeaders.userid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"USERID",fileHeaders.userid,"USER ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 81;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating USERID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update PROPID keyword  */
	if(fileHeaders.propid==NULL) strncpy(fileHeaders.propid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"PROPID",fileHeaders.propid,"Proposal ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 82;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating PROPID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update GROUPID keyword  */
	if(fileHeaders.groupid==NULL) strncpy(fileHeaders.groupid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"GROUPID",fileHeaders.groupid,"Group ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 83;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GROUPID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update OBSID keyword  */
	if(fileHeaders.obsid==NULL) strncpy(fileHeaders.obsid, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"OBSID",fileHeaders.obsid,"Obs ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 84;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating OBSID failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* Post 2008-11 header corrections */

	/* update EXPTOTAL keyword  */
	if(fileHeaders.exptotal==NULL) strncpy(fileHeaders.exptotal, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"EXPTOTAL",atoi(fileHeaders.exptotal),0,"Total number of exposures requested for this multrun",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 85;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EXPTOTAL failed(%d,%s,%d,%s).",
			atoi(fileHeaders.exptotal),filename, status,buff);
		return FALSE;
	}


	/* update PRESCAN keyword  */
	if(fileHeaders.prescan==NULL) strncpy(fileHeaders.prescan, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"PRESCAN",atoi(fileHeaders.prescan),0,"Columns of prescan",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 86;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating PRESCAN failed(%d,%s,%d,%s).",
			atoi(fileHeaders.prescan),filename, status,buff);
		return FALSE;
	}

	/* update POSTSCAN keyword  */
	if(fileHeaders.postscan==NULL) strncpy(fileHeaders.postscan, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"POSTSCAN",atoi(fileHeaders.postscan),0,"Columns of postscan",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 87;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating POSTSCAN failed(%d,%s,%d,%s).",
			atoi(fileHeaders.postscan),filename, status,buff);
		return FALSE;
	}

	/* update ROTCENTX keyword  */
	if(fileHeaders.rotcentx==NULL) strncpy(fileHeaders.rotcentx, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"ROTCENTX",atoi(fileHeaders.rotcentx),0,"Pixel Coord of mechanical rotator centre",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 88;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ROTCENTX failed(%d,%s,%d,%s).",
			atoi(fileHeaders.rotcentx),filename, status,buff);
		return FALSE;
	}


	/* update ROTCENTY keyword  */
	if(fileHeaders.rotcenty==NULL) strncpy(fileHeaders.rotcenty, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"ROTCENTY",atoi(fileHeaders.rotcenty),0,"Pixel Coord of mechanical rotator centre",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 89;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ROTCENTY failed(%d,%s,%d,%s).",
			atoi(fileHeaders.rotcenty),filename, status,buff);
		return FALSE;
	}


	/* update POICENTX keyword  */
	if(fileHeaders.poicentx==NULL) strncpy(fileHeaders.poicentx, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"POICENTX",atoi(fileHeaders.poicentx),0,"Pixel of pointing model centre after APERTURE command",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 90;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating POICENTX failed(%d,%s,%d,%s).",
			atoi(fileHeaders.poicentx),filename, status,buff);
		return FALSE;
	}

	/* update POICENTY keyword  */
	if(fileHeaders.poicenty==NULL) strncpy(fileHeaders.poicenty, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"POICENTY",atoi(fileHeaders.poicenty),0,"Pixel of pointing model centre after APERTURE command",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 91;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating POICENTY failed(%d,%s,%d,%s).",
			atoi(fileHeaders.poicenty),filename, status,buff);
		return FALSE;
	}

	/* update FILTER1 keyword  */
	if(fileHeaders.filter1==NULL) strncpy(fileHeaders.filter1, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"FILTER1",fileHeaders.filter1,"Name of filter type",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 92;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating FILTER1 failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}

	/* update FILTERI1 keyword  */
	if(fileHeaders.filteri1==NULL) strncpy(fileHeaders.filteri1, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"FILTERI1",fileHeaders.filteri1,"Filter ID",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 93;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating FILTERI1 failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update CCDSCALE keyword  */
	if(fileHeaders.ccdscale==NULL) strncpy(fileHeaders.ccdscale, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"CCDSCALE",atof(fileHeaders.ccdscale),5,"arcsec/pix unbinned",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 94;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating CCDSCALE failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.ccdscale),filename, status,buff);
		return FALSE;
	}


	/* update RADECSYS keyword  */
	if(fileHeaders.radecsys==NULL) strncpy(fileHeaders.radecsys, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"RADECSYS",fileHeaders.radecsys,"RADEC System",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 95;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating RADECSYS failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}


	/* update EQUINOX keyword  */
	if(fileHeaders.equinox==NULL) strncpy(fileHeaders.equinox, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"EQUINOX",atof(fileHeaders.equinox),1,"Coordinate system date",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 96;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating EQUINOX failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.equinox),filename, status,buff);
		return FALSE;
	}


	/* update GRPTIMNG keyword  */
	if(fileHeaders.grouptimng==NULL) strncpy(fileHeaders.grouptimng, "UNKNOWN-NULL",80);
	retval = fits_update_key(fp,TSTRING,"GRPTIMNG",fileHeaders.grouptimng,"Group timing constraint class",&status);

	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 97;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GRPTIMNG failed(%s,%d,%s).",filename,
			status,buff);
		return FALSE;
	}



	/* update GRPNUMOB keyword  */
	if(fileHeaders.groupnumob==NULL) strncpy(fileHeaders.groupnumob, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"GRPNUMOB",atof(fileHeaders.groupnumob),0,"Number of Observations in group",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 98;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GRPNUMOB failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.groupnumob),filename, status,buff);
		return FALSE;
	}




	/* update GRPUID keyword  */
	if(fileHeaders.groupuid==NULL) strncpy(fileHeaders.groupuid, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"GRPUID",atof(fileHeaders.groupuid),0,"Group unique ID",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 99;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GRPUID failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.groupuid),filename, status,buff);
		return FALSE;
	}



	/* update GRPNOMEX keyword  */
	if(fileHeaders.groupnomex==NULL) strncpy(fileHeaders.groupnomex, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"GRPNOMEX",atof(fileHeaders.groupnomex),6,"Group nominal exec time",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 100;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GRPNOMEX failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.groupnomex),filename, status,buff);
		return FALSE;
	}



	/* update GRPMONP keyword  */
	if(fileHeaders.groupmonp==NULL) strncpy(fileHeaders.groupmonp, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"GRPMONP",atof(fileHeaders.groupmonp),6,"Group monitor period",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 101;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating GRPMONP failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.groupmonp),filename, status,buff);
		return FALSE;
	}


	/* update ROTANGLE keyword  */
	if(fileHeaders.rotangle==NULL) strncpy(fileHeaders.rotangle, "UNKNOWN-NULL",80);
	retval = fits_update_key_fixdbl(fp,"ROTANGLE",atof(fileHeaders.rotangle),6,"Mount angle at start of Multrun",&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		fits_close_file(fp,&status);
		Multrun_Error_Number = 102;
		sprintf(Multrun_Error_String,"Exposure_Save: Updating ROTANGLE failed(%.5f,%s,%d,%s).",
			atof(fileHeaders.rotangle),filename, status,buff);
		return FALSE;
	}




	/* Finished writing headers! */

#endif
  	/* nanosleep(&waittime,NULL);	*/
	/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_get_errstatus(status,buff);
		fits_report_error(stderr,status);
		Multrun_Error_Number = 59;
		sprintf(Multrun_Error_String,"Exposure_Save: File close failed(%s,%d,%s).",filename,status,buff);
		return FALSE;
	}
#if LOGGING > 4
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"Exposure_Save:Finished, CFITSIO status %d",status);
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"Exposure_Save: File %s saved.",filename);
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"Exposure_Save:Completed.");
#endif
	return TRUE;
}


float start_time_correction (float exposure){
	/* This function corrects the multrun epoch time so that the time of the start
	 * of image acquisition is taken. It subtracts the readout time and the frame 
	 * transfer time, derived from the horizontal and vertical shift speeds. These
	 * speeds are in microseconds per pixel. Note tv_nsec is in nanoseconds so that
	 * 1 microsec = 1000 nanosec! 
	 *
	 * For a single Multrun, the correction will be the same for each image, as it is a 
	 * function of VSspeed, HSspeed and the exposure time. Use in conjunction with 
	 * correct_start_time() to get the UTSTART struct timespec. 
	 * @return Returns the value in nanoseconds of the correction */

	int image_rows = CCD_Setup_Get_NRows(); 
	int image_cols = CCD_Setup_Get_NCols(); 
	float readout_time, ft_time;

#if LOGGING > 4
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"start_time_correction: %d %d  H:%.2f V:%.2f",
			      image_rows,image_cols,Multrun_Data.HSspeed,Multrun_Data.VSspeed);
#endif

	/* Get the readout time in microseconds */
	readout_time = (image_rows * Multrun_Data.VSspeed) + (image_cols * image_rows * Multrun_Data.HSspeed);
	ft_time = image_rows * Multrun_Data.VSspeed;

#if LOGGING > 4
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
			      "start_time_correction: readout time %.2f ms : FT time: %.2f ms",
			      readout_time/1000,ft_time/1000);
#endif

	/* Return time correction in nanoseconds */
	return (readout_time*1e3 + ft_time*1e3 + exposure*1e9 );
}


int correct_start_time (struct timespec *t){
	/* This function applies the time correction derived in start_time_correction() */
	int seconds = (int)(floor(Multrun_Data.TimeCorrection/1e9));  
	float nseconds = Multrun_Data.TimeCorrection - seconds*1e9;
	t->tv_sec-=seconds;
	t->tv_nsec-=nseconds;
	if (t->tv_nsec <0){
		t->tv_sec-=1;
		t->tv_nsec+=1e9; }
	return 0;
}



/* Start of FitsFilename.cpp */

static int getNextFilename (char *NewFileName, int NewMultRun)
{
	/* Define this externally! */
	/* struct FitsFilename ff; */
	struct DirList *srclist=NULL;
	struct DirList *FilteredList=NULL;

	int srclistLength, FilteredListSize;
	int MaxRun,MaxMultRun;

	/* Allocate enough memory for the dir listing */
	srclist = (struct DirList*) malloc(MAXLIST_SIZE*sizeof(*srclist));	
	if (srclist == NULL) 
	{
		Multrun_Error_Number = 105;
		sprintf(Multrun_Error_String,"getNextFilename:Malloc error on line %d",(__LINE__-2)); 
		return FALSE; 
	}

	/* Initialise with sane values */
	FitsFilename_init(&ff);

	/* Get today's date, and load the image dir */
	getDateString(ff.date);   
	load_dir(ff.directory,srclist,&srclistLength);
	/* Shrink to fit */
	srclist = (struct DirList*) realloc(srclist,(srclistLength+1)*sizeof(*srclist));	
	if (srclist == NULL) 
	{
		Multrun_Error_Number = 106;
		sprintf(Multrun_Error_String,"getNextFilename:Realloc error on line %d",(__LINE__-2)); 
		return FALSE; 
	}
	/* Create a new structured array for the filtered data, at least as large as
 	 * the unfiltered one. */
	FilteredList = (struct DirList*) realloc(FilteredList,(srclistLength+1)*sizeof(*FilteredList));	
	if (FilteredList == NULL) 
	{
		Multrun_Error_Number = 107;
		sprintf(Multrun_Error_String,"getNextFilename:Realloc error on line %d",(__LINE__-2)); 
		return FALSE; 
	}

#if FF_DEBUG == 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:date %s",ff.date);
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:directory %s",ff.directory);
#endif
	/* Filter the dates based on today's date */
    	FilterFilename (srclist, FilteredList, srclistLength, &FilteredListSize, ff.date);

#if FF_DEBUG == 1
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:Unfiltered:");
	for(i=0;i<srclistLength;i++)
	{
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:%s \t\t%d",
				      srclist[i].file,srclist[i].fnlength);
	}
	CCD_Global_Log(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:Filtered:");
	for(i=0;i<FilteredListSize;i++) 
	{
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:%s \t\t%d",
				      FilteredList[i].file,FilteredList[i].fnlength);
	}
#endif
	MaxMultRun=getLargestMultrunNumber (FilteredList, FilteredListSize);
	MaxRun= getLargestRunNumber(FilteredList, FilteredListSize, MaxMultRun);
	ConstructNextFilename (&ff, MaxMultRun, MaxRun, NewMultRun, NewFileName); 
#if FF_DEBUG == 1
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:Largest Multrun: %d",MaxMultRun);
	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename: |__ Largest run: %d",MaxRun);
 	CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,"getNextFilename:Next_Filename: %s",NewFileName);
#endif
	free(srclist); 
	free(FilteredList);
	return TRUE;
}



static char *ConstructNextFilename (struct FitsFilename *ff, int MMR, int MR, int startMR, char *NFN) 
{
	/* This function constructs the next filename for a multrun */
 
	/* Check if the startMR flag is set. This means 'start a new multrun' If it is
	 * not set, then there is no new multrun, and the previous one continues */

	switch (startMR) {
		case 0: /* Flag not set, continue multrun */
			MMR = MMR;
			MR++;
			break;
		case 1:
			MMR++;
			MR=1;
			break;
	} 
	sprintf(NFN,"%s_%s_%s_%d_%d_%d_%s.%s",ff->instrumentCode,ff->exposureCode,ff->date,MMR,MR,
		ff->windowNumber,ff->pipelineProcessing,ff->fileExtension);
	ff->multRunNumber=MMR;
	ff->runNumber=MR;
	return(NFN);
}

int getLargestRunNumber (struct DirList *dl, int size_dl, int multrun) {
	/* This function generates the largest run number from a dirlist */
	int rn_max = 0; /* Initally set to zero */
	int i;
	struct LtFilename file_breakdown;

	for (i=0;i<size_dl;i++)
	{
		ParseFilename(dl[i].file,&file_breakdown);
		if(file_breakdown.multRunNumber==multrun && file_breakdown.runNumber > rn_max) 
			rn_max=file_breakdown.runNumber; 
	}
	return (rn_max);
}


int getLargestMultrunNumber (struct DirList *dl, int size_dl)
{
	/* Gets the largest multrun number for the current date string */
	int mrn_max = 0; /* Initally set to zero */
	int i;
	struct LtFilename file_breakdown;

	for (i=0;i<size_dl;i++)
	{
		ParseFilename(dl[i].file,&file_breakdown);
		if(file_breakdown.multRunNumber > mrn_max) 
			mrn_max=file_breakdown.multRunNumber; 
	}
	return (mrn_max);
}
 

void getDateString(char *p)
{
	time_t now;
	struct tm *ptm;
	time (&now); /* Get the current time */

	ptm = gmtime(&now);
	/* The date string needs to be the date AT THE START OF THE NIGHT. */
	/* If the hour of day is less than midday, then the date at the start of the night */
	/* is the day before the current day so roll back the date by one day. */
	if(ptm->tm_hour < 12)
	{ 
		now = now - (time_t)(24*60*60); 
		ptm = gmtime(&now);
	}

	sprintf(p,"%04d%02d%02d",ptm->tm_year+1900,ptm->tm_mon+1, ptm->tm_mday); 
	return;
}


void ParseFilename(char *filename, struct LtFilename *ltfn)
{
	/* This function will take a filename, and will generate the different
	 * components for use.  c_e_20070830_11_10_1_0.fits     
	 * if a value is not available, it is relaced by X */

	char *temp;
	char filename1[64];
	char delim[] = "_";

	/* Copy the variable, because strtok will destroy it! */
	strncpy(filename1,filename,64); 
	/* Grab the first element, the intrument code */
	temp=strtok(filename1,delim);
	if(temp!=NULL)strncpy(ltfn->InstCode,temp,1);
	else strncpy(ltfn->InstCode,"X",1);
	temp=strtok(NULL,delim);
	if(temp!=NULL)strncpy(ltfn->ExposureType,temp,2);
	else strncpy(ltfn->ExposureType,"X",1);
	temp=strtok(NULL,delim);
	if(temp!=NULL)strncpy(ltfn->date,temp,8);
	else strncpy(ltfn->date,"XXXXXXXX",8);
	temp=strtok(NULL,delim);
	if(temp!=NULL)ltfn->multRunNumber = atoi(temp);
	else ltfn->multRunNumber = 0;
	temp=strtok(NULL,delim);
	if(temp!=NULL)ltfn->runNumber = atoi(temp);
	else ltfn->runNumber = 0;
	temp=strtok(NULL,delim);
	if(temp!=NULL)ltfn->windowNumber = atoi(temp);
	else ltfn->windowNumber = 0;
	temp=strtok(NULL,delim);
	if(temp!=NULL)ltfn->plProcessing = atoi(temp);
	else ltfn->plProcessing = 0;
	/* Clean out null characters */
}


void FitsFilename_init(struct FitsFilename *f){
	strcpy(f->directory,IMAGEDIR);
	strcpy(f->instrumentCode,INSTRUMENT_CODE_CCD_CAMERA);
	if(strcmp(fileHeaders.obstype,"EXPOSE")==0)
		strcpy(f->exposureCode,EXPOSURE_CODE_EXPOSURE); 
	else if(strcmp(fileHeaders.obstype,"SKYFLAT")==0)
		strcpy(f->exposureCode,EXPOSURE_CODE_SKY_FLAT);
	else strcpy(f->exposureCode,"U");
	f->multRunNumber = 0;
	f->runNumber = 0;
	f->windowNumber = 1; 	
	strcpy(f->pipelineProcessing,PIPELINE_PROCESSING_FLAG_NONE);
	if(GZIP == 1) strcpy(f->fileExtension,"fits.gz");
	else strcpy(f->fileExtension,"fits");
	strcpy(f->date,"19700101");
	f->isTelfocus=FALSE;
	f->isTwilightCalibrate=FALSE;
} 


void FilterFilename (struct DirList *src, struct DirList *dest, int SrcSize, int *DestSize, char * string)
{
	/* Filters a source dir list to a dest dirlist based on the presence of a
	   string, e.g. a date */ 
	int i,DestCount=0;

	for (i=0;i<SrcSize;i++)
	{
		if (strstr(src[i].file,string)!=NULL)
		{
			strncpy(dest[DestCount].file,src[i].file,MAX_FILENAME);
			dest[DestCount].fnlength=src[i].fnlength;
			DestCount++; 
		} 
	}
	*DestSize=DestCount;
}


void load_dir(char *dir, struct DirList *f, int *count)
{
	DIR *dirp; /* Pointer to dir */
	struct dirent *dp; /* Pointer to structure holding data */
	char temp[MAX_FILENAME];
	int i=0;
		
	dirp=opendir(dir);
	if(dirp==NULL)
	{
		CCD_Global_Log_Format(CCD_GLOBAL_LOG_BIT_EXPOSURE,
				      "load_dir:Cannot use assigned out dir %s, using /tmp",dir);
		dirp=opendir("/tmp");
	}

	/*Each call to readdir reads new file in dir */
	/*Files beginning with a dot are ignored */
	while((dp=readdir(dirp)) != NULL)
	{ 
		strncpy(temp,dp->d_name,MAX_FILENAME);
		if(strncmp(temp,".",1)==0) 
			i--;
		else 
		{
			strncpy(f[i].file,temp,MAX_FILENAME);
			f[i].fnlength = (int)strlen(temp); 
		}
		i++;
	}
	
	/* Close the pointer to the file */
	closedir(dirp);
	
	*count=i;

	return;
}

char *chomp (char *string)
{
	/* Similar to CHOMP in Perl. Pass a string, a string is returned. */
	int size = strlen(string);
	int i;
	for(i=0;i<size;i++)
	{
		if(string[i] == '\n') 
			string[i]='\0';
	}
	return(string);
} 

/* End of FitsFilename.cpp  */



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
		Multrun_Error_Number = 103;
		sprintf(Multrun_Error_String,"Exposure_TimeSpec_To_Mjd:slaCldj(%d,%d,%d) failed(%d).",year,month,
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
		Multrun_Error_Number = 104;
		sprintf(Multrun_Error_String,"Exposure_TimeSpec_To_Mjd:NGAT_Astro_Timespec_To_MJD failed.\n");
		/* concatenate NGAT Astro library error onto Multrun_Error_String */
		NGAT_Astro_Error_String(Multrun_Error_String+strlen(Multrun_Error_String));
		return FALSE;
	}
#else
#error Neither NGATASTRO or SLALIB are defined: No library defined for MJD calculation.
#endif
#endif
	return TRUE;
}


/* Median functions to determine the brightness of the image */
void sorter(double *a,int n)
{
	int i,j;

	for(i=0; i<n-1; i++) {
		for (j=i+1;j<n;j++) {
			if (a[i] < a[j]) swap(i,j,a);
		}
	}
	return; 
}

void swap(int i, int j,double *a)
{
	double temp;
	temp = a[i]; 
	a[i] = a[j]; 
	a[j] = temp; 
	return; 
}

double median(double *a,int n)
{
	sorter(a,n);
	if (n != 2*(n/2)) return a[n/2]; 
	return (a[(n-1)/2] + a[n/2])/2;
}


/* End of median functions */


int getSquareRegion(const long *inArray, double *sqrArray, int x, int y, int R){ 
	/* This function will grab a square region from the single line array 
	   and will return an appropriately sized array filled with those values
	   this is useful for calculating the median of a small region on an image. 
	   The value of R is the 'radius' of the box e.g. it is 5 for a 5x5 box.
	   NOTE: The returned array must be allocated enough space! Also, no check is made 
	   to check for the value of R running off the side of the image. It is assumed that
	   the user will know the physical layout and prevent this.

	   Req: The size of the image frame is retrieved from CCD_Setup_Get_NCols() etc. 
	   The amount of space required for sqrArray is (2R+1)^2 long.      */
    
	long i,j,arrayCount=0;
	long XcPixelNumber=0;
	int ncols = CCD_Setup_Get_NCols();

	/* Now loop over the required rows. The following procedure should work:
	   1. Step through -R to +R and get the pixel number of the center column Xc in each row
	   2. Loop from the start of each row to the end. If the pixel is +/- R from Xc add
	   it to the array.   */

	for(i=-R; i<=R; i++){
		XcPixelNumber = ( (y+i) * ncols ) + x;
		/*  printf("CenterPixel: %d \n",XcPixelNumber); */
		for(j=(XcPixelNumber-R); j<=(XcPixelNumber+R); j++){
			sqrArray[arrayCount]=(double)inArray[j];
			arrayCount++; 
		}
	}

	return(0);
}


/**
 * Get the current value of the ccd_multrun error number.
 * @return The current value of the ccd_multrun error number.
 */
int CCD_Multrun_Get_Error_Number(void)
{
	return Multrun_Error_Number;
}

/**
 * The error routine that reports any errors occuring in ccd_multrun in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Multrun_Error(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Multrun_Error_Number == 0)
		sprintf(Multrun_Error_String,"Logic Error:No Error defined");
	fprintf(stderr,"%s CCD_Multrun:Error(%d) : %s\n",time_string,Multrun_Error_Number,Multrun_Error_String);
}

/**
 * The error routine that reports any errors occuring in ccd_multrun in a standard way. This routine places the
 * generated error string at the end of a passed in string argument.
 * @param error_string A string to put the generated error in. This string should be initialised before
 * being passed to this routine. The routine will try to concatenate it's error string onto the end
 * of any string already in existance.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Multrun_Error_String(char *error_string)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Multrun_Error_Number == 0)
		sprintf(Multrun_Error_String,"Logic Error:No Error defined");
	sprintf(error_string+strlen(error_string),"%s CCD_Multrun:Error(%d) : %s\n",time_string,
		Multrun_Error_Number,Multrun_Error_String);
}


/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:16:23  cjm
** Initial revision
**
** Revision 1.1  2009/10/15 10:06:52  cjm
** Initial revision
**
** Revision 1.4  2009/01/06 14:15:20  wasp
** Last update submitted to RISE camera to add some header information.
**
** Revision 1.3  2008/05/13 10:57:19  wasp
** Fixed polling of circular buffer issue that quantized the time to 0.5 second chunks.
**
** Revision 1.2  2008/04/17 07:38:02  wasp
** Working
**
** Revision 1.1.1.1  2008/03/11 13:37:01  wasp
** Start
** 
*/







