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
/* ccd_multrun.h
** $Header: /space/home/eng/cjm/cvs/rise/ccd/include/ccd_multrun.h,v 1.2 2010-01-14 16:13:06 cjm Exp $
*/
#ifndef CCD_MULTRUN_H
#define CCD_MULTRUN_H
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
#include <time.h>
#include "ccd_global.h"

/* These #define/enum definitions should match with those in CCDLibrary.java */
/**
 * When bits 3 and 5 in the HSTR are set, the SDSU controllers are reading out.
 */
#define CCD_EXPOSURE_HSTR_READOUT				(0x5)
/**
 * How many bits to shift the HSTR register to get the READOUT status bits.
 */
#define CCD_EXPOSURE_HSTR_BIT_SHIFT				(0x3)

/**
 * Macro to check whether the exposure status is a legal value.
 * @see #CCD_EXPOSURE_STATUS
 */
#define CCD_EXPOSURE_IS_STATUS(status)	(((status) == CCD_EXPOSURE_STATUS_NONE)|| \
        ((status) == CCD_EXPOSURE_STATUS_WAIT_START)|| \
	((status) == CCD_EXPOSURE_STATUS_CLEAR)||((status) == CCD_EXPOSURE_STATUS_EXPOSE)|| \
        ((status) == CCD_EXPOSURE_STATUS_READOUT)||((status) == CCD_EXPOSURE_STATUS_POST_READOUT))

/* FitsFilename.h stuff */
#include<dirent.h>
#define GZIP 1 /* Gzip the fits files */
#define HEADERS 1
#define MAX_FILENAME 64 /* Maximum filename size */
#define FF_DEBUG 0
#define INSTRUMENT_CODE_CCD_CAMERA "q"
#define PIPELINE_PROCESSING_FLAG_NONE "0"
#define PIPELINE_PROCESSING_FLAG_REAL_TIME "1"
#define PIPELINE_PROCESSING_FLAG_OFF_LINE "2"
#define INSTRUMENT_TOKEN_NUMBER "1"
#define EXPOSE_CODE_TOKEN_NUMBER "2"
#define DATE_TOKEN_NUMBER "3"
#define MULTRUN_TOKEN_NUMBER "4"
#define RUN_TOKEN_NUMBER "5"
#define WINDOW_TOKEN_NUMBER "6"
#define PIPELINE_PROCESSING_TOKEN_NUMBER "7"
#define FILE_EXTENSION_TOKEN_NUMBER "8"
#define EXPOSURE_CODE_EXPOSURE "e"
#define EXPOSURE_CODE_BIAS "b"
#define EXPOSURE_CODE_STANDARD "s"
#define EXPOSURE_CODE_SKY_FLAT "f"
#define EXPOSURE_CODE_LAMP_FLAT "w"
#define EXPOSURE_CODE_ARC "a"
#define EXPOSURE_CODE_DARK "d"
#define MAXLIST_SIZE 100000 /* Maximum number of directory entries */
#define TRUE 1			
#define FALSE 0
#define IMAGEDIR "/home/dev/tmp"
#define EXPOSURE_READ_TIMEOUT                           30
#define EXPOSURE_DEFAULT_READOUT_REMAINING_TIME       	(1500)
#define EXPOSURE_DEFAULT_START_EXPOSURE_CLEAR_TIME	(10)
#define EXPOSURE_DEFAULT_START_EXPOSURE_OFFSET_TIME	(2)

/* external structure declarations */

struct FitsFilename {
	char directory[64];
	char instrumentCode[2];
	char exposureCode[2];
	char date[64];
	int multRunNumber;
	int runNumber;
	int windowNumber;
	char pipelineProcessing[2];
	char fileExtension[8];
	int  isTelfocus;
	int  isTwilightCalibrate;
};

struct DirList {
   	char file[MAX_FILENAME]; 
	int fnlength; };

struct LtFilename {
 /* c_e_20070830_11_10_1_0.fits    */ 
	char InstCode[2];
	char ExposureType[2];
	char date[8];
	int multRunNumber;
	int runNumber;
	int windowNumber;
	int plProcessing;				
};

struct Header {
 /* Structure to hold the various headers */
 char ra[80];
 char dec[80];
 char latitude[80];
 char longitude[80];
 char obstype[80];
 char airmass[80];
 char telfocus[80];
 char origin[80];
 char instatus[80];
 char configid[80];
 char telescop[80];
 char telmode[80];
 char lst[80];
 char catra[80];
 char catdec[80];
 char telstat[80];
 char autoguid[80];
 char rotmode[80];
 char rotskypa[80];
 char windspee[80];
 char wmstemp[80];
 char wmshumid[80];
 char object[80];
 char instrument[80];
 char confname[80];
 char detector[80];
 char gain[80];
 char readnoise[80];
 char tagid[80];
 char userid[80];
 char progid[80];
 char propid[80];
 char groupid[80];
 char obsid[80];
 
 char exptotal[80];
 char prescan[80];
 char postscan[80];
 char rotcentx[80];
 char rotcenty[80];
 char poicentx[80];
 char poicenty[80];
 char filteri1[80];
 char filter1[80];
 char ccdscale[80];
 char radecsys[80];
 char equinox[80];
 char grouptimng[80];
 char groupnumob[80];
 char groupuid[80];
 char groupnomex[80];
 char groupmonp[80];
 char rotangle[80];
};

/**
 * Structure used to hold local data to ccd_multrun.
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
 * </dl>
 * @see ccd_dsp.html#CCD_EXPOSURE_STATUS
 */

struct Multrun_Struct {
	enum CCD_EXPOSURE_STATUS Exposure_Status;
	int Start_Exposure_Clear_Time;
	int Start_Exposure_Offset_Time;
	int Readout_Remaining_Time;
	long lastMultrunExposures;
	float Exposure_Length;
	float requestedExposureTime;
	double temperature;
	struct timespec Exposure_Start_Time;
	struct timespec Exposure_Epoch_Time;
	struct timespec Multrun_Start_Time;
	struct timespec LastImageTime;
	float HSspeed;
	float VSspeed;
	float TimeCorrection;
	double median_value;
	char ntpTime[256];
	char ntpServer[256];
	float ntpDrift; 
	time_t timeStart;
	long maxTime;
	int isMultFlat;
};

/* external function declarations */

extern int CCD_Multrun_Expose (int open_shutter, long startTime, int exposure_time, long exposures, char **headers);
extern int CCD_Multflat_Expose (int open_shutter, long startTime, int exposure_time, long exposures, char **headers);
extern int Multrun_Exposure_Save(char *filename, unsigned long *exposure_data,int ncols,int nrows);
extern void FitsFilename_init(struct FitsFilename *f);
extern void getDateString(char *p);
extern void load_dir(char *dir, struct DirList *f, int *count);
extern void FilterFilename (struct DirList *src, struct DirList *dest, int SrcSize, int *DestSize, char * string);
extern void ParseFilename(char *filename, struct LtFilename *ltfn);
extern int getLargestMultrunNumber (struct DirList *dl, int size_dl);
extern int getLargestRunNumber (struct DirList *dl, int size_dl, int multrun);

extern float start_time_correction (float exposure);
extern int correct_start_time (struct timespec *t);
extern void sorter(double *a,int n);
extern void swap(int i, int j,double *a);
extern double median(double *a,int n);
extern float getNewExposureTime( double oldCounts, float oldExposure);
extern int getNtpDrift(char *server, float *drift);
extern char *chomp (char *string); 

extern int CCD_Multrun_Get_Error_Number(void);
extern void CCD_Multrun_Error(void);
extern void CCD_Multrun_Error_String(char *error_string);
#endif
