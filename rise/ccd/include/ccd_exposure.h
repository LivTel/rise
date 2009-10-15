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
/* ccd_exposure.h
** $Header: /space/home/eng/cjm/cvs/rise/ccd/include/ccd_exposure.h,v 1.1 2009-10-15 10:16:27 cjm Exp $
*/
#ifndef CCD_EXPOSURE_H
#define CCD_EXPOSURE_H
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
 * Return value from CCD_DSP_Get_Exposure_Status. 
 * <ul>
 * <li>CCD_EXPOSURE_STATUS_NONE means the library is not currently performing an exposure.
 * <li>CCD_EXPOSURE_STATUS_WAIT_START means the library is waiting for the correct moment to open the shutter.
 * <li>CCD_EXPOSURE_STATUS_CLEAR means the library is currently clearing the ccd.
 * <li>CCD_EXPOSURE_STATUS_EXPOSE means the library is currently performing an exposure.
 * <li>CCD_EXPOSURE_STATUS_PRE_READOUT means the library is currently exposing, but is about
 * 	to start reading out data from the ccd (so don't start any commands that won't work in readout). 
 * <li>CCD_EXPOSURE_STATUS_READOUT means the library is currently reading out data from the ccd.
 * <li>CCD_EXPOSURE_STATUS_POST_READOUT means the library has finished reading out, but is post processing
 * 	the data (byte swap/de-interlacing/saving to disk).
 * </ul>
 * If these values are changed, the relevant values in ngat.ccd.CCDLibrary should be updated to match.
 * @see #CCD_DSP_Get_Exposure_Status
 */
enum CCD_EXPOSURE_STATUS
{
	CCD_EXPOSURE_STATUS_NONE,CCD_EXPOSURE_STATUS_WAIT_START,CCD_EXPOSURE_STATUS_CLEAR,
	CCD_EXPOSURE_STATUS_EXPOSE,CCD_EXPOSURE_STATUS_PRE_READOUT,CCD_EXPOSURE_STATUS_READOUT,
	CCD_EXPOSURE_STATUS_POST_READOUT
};

/**
 * Macro to check whether the exposure status is a legal value.
 * @see #CCD_EXPOSURE_STATUS
 */
#define CCD_EXPOSURE_IS_STATUS(status)	(((status) == CCD_EXPOSURE_STATUS_NONE)|| \
        ((status) == CCD_EXPOSURE_STATUS_WAIT_START)|| \
	((status) == CCD_EXPOSURE_STATUS_CLEAR)||((status) == CCD_EXPOSURE_STATUS_EXPOSE)|| \
        ((status) == CCD_EXPOSURE_STATUS_READOUT)||((status) == CCD_EXPOSURE_STATUS_POST_READOUT))

extern void CCD_Exposure_Initialise(void);
extern int CCD_Exposure_Expose(int clear_array,int open_shutter,struct timespec start_time,int exposure_time,
			       char **filename_list,int filename_count);
extern int CCD_Exposure_Bias(char *filename);
extern int CCD_Exposure_Open_Shutter(void);
extern int CCD_Exposure_Close_Shutter(void);
extern int CCD_Exposure_Pause(void);
extern int CCD_Exposure_Resume(void);
extern int CCD_Exposure_Abort(void);
extern int CCD_Exposure_Read_Out_CCD(char *filename);

extern int CCD_Exposure_Set_Exposure_Status(enum CCD_EXPOSURE_STATUS status);
extern enum CCD_EXPOSURE_STATUS CCD_Exposure_Get_Exposure_Status(void);
extern struct timespec CCD_Exposure_Get_Exposure_Start_Time(void);
extern int CCD_Exposure_Get_Exposure_Length(void);
extern void CCD_Exposure_Set_Start_Exposure_Clear_Time(int time);
extern int CCD_Exposure_Get_Start_Exposure_Clear_Time(void);
extern void CCD_Exposure_Set_Start_Exposure_Offset_Time(int time);
extern int CCD_Exposure_Get_Start_Exposure_Offset_Time(void);
extern void CCD_Exposure_Set_Readout_Remaining_Time(int time);
extern int CCD_Exposure_Get_Readout_Remaining_Time(void);
extern void CCD_Exposure_Set_Exposure_Start_Time(void);

extern int CCD_Exposure_Get_Error_Number(void);
extern void CCD_Exposure_Error(void);
extern void CCD_Exposure_Error_String(char *error_string);
extern void CCD_Exposure_Warning(void);


#endif
