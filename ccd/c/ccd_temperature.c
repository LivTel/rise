/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Rise.

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
/* ccd_temperature.c
** low level ccd library
** $Header: /space/home/eng/cjm/cvs/rise/ccd/c/ccd_temperature.c,v 1.4 2022-03-15 16:14:12 cjm Exp $
*/

/**
 * ccd_temperature holds the routines for calulating the current CCD temperature and setting the CCDs
 * temperature.
 * @author Chris Mottram
 * @version $Revision: 1.4 $
 */
/**
 * This hash define is needed before including source files give us POSIX.4/IEEE1003.1b-1993 prototypes.
 */
#define _POSIX_SOURCE 1
/**
 * This hash define is needed before including source files give us POSIX.4/IEEE1003.1b-1993 prototypes.
 */
#define _POSIX_C_SOURCE 199309L
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include "log_udp.h"
#include "ccd_global.h"
#include "ccd_multrun.h"
#include "ccd_temperature.h"
#include "atmcdLXd.h"


/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: ccd_temperature.c,v 1.4 2022-03-15 16:14:12 cjm Exp $";

/* data types */

/* external variables */

/* internal variables */
/**
 * Variable holding error code of last operation performed by ccd_temperature.
 */
static int Temperature_Error_Number = 0;
/**
 * Local variable holding description of the last error that occured.
 */
static char Temperature_Error_String[CCD_GLOBAL_ERROR_STRING_LENGTH] = "";

/* internal function definitions */

/* external functions */
/**
 * This routine gets the current temperature of the CCD. 
 * GetStatus is called first, and if the status is not DRV_IDLE we assume a MULTRUN is in progress
 * and return the last cached temperature (CCD_Multrun_Get_Cached_Temperature).
 * GetTemperatureF is called to retrieve the temperature. The current temperature and status are logged.
 * @param temperature The address of a variable to hold the calculated temperature to be returned.
 * 	The returned temperature is in degrees centigrade.
 * @return TRUE if the operation was successfull and the temperature returned was sensible, FALSE
 * 	if a failure occured or the temperature returned was not sensible.
 * @see #Temperature_Error_Number
 * @see #Temperature_Error_String
 * @see ccd_multrun.html#CCD_Multrun_Get_Cached_Temperature
 * @see ccd_global.html#CCD_Global_ErrorCode_To_String
 * @see ccd_global.html#CCD_Global_Log_Format
 */
int CCD_Temperature_Get(double *temperature)
{
	double cached_temperature;
	float temp;
	int error;

	/* First get status. We must not be active! */
	GetStatus(&error);
	if(error!=DRV_IDLE) 
	{
		/* we are probably doing a multrun (ccd_multrun.c:Expose).
		** Lets return the ccd temperature cached at the start of Expose */
		cached_temperature = CCD_Multrun_Get_Cached_Temperature();
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,
				      "CCD_Temperature_Get: CCD_Multrun_Get_Cached_Temperature returned temperature %f degC.",cached_temperature);
#endif
		(*temperature) = cached_temperature;
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,
				      "CCD_Temperature_Get: Using Multrun cached Temperature %f degC.",(*temperature));
#endif
		return TRUE;
		/*
		(*temperature) = 0.0;
		Temperature_Error_Number = 1;
		sprintf(Temperature_Error_String,"CCD_Temperature_Get:GetStatus not DRV_IDLE:%d (%s).",error,
			CCD_Global_ErrorCode_To_String(error));
		return FALSE;
		*/
	}

	error=GetTemperatureF(&temp);
	(*temperature) = (double)temp;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Temperature_Get: Temperature %f degC %d",
			      *temperature,error);
#endif
	GetStatus(&error);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Temperature_Get: Current Status %d (%s).",error,
			CCD_Global_ErrorCode_To_String(error));
#endif
	return TRUE;

}

/**
 * Routine to set the target temperature the CCD Controller.
 * @param target_temperature The temperature we want the CCD cooled to, in degrees centigrade.
 * @return TRUE if the target temperature was set, FALSE if an error occured.
 */
int CCD_Temperature_Set(double target_temperature)
{
	unsigned long andor_error_num;

	Temperature_Error_Number = 0;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Temperature_Set(temperature=%.2f) started.",
		target_temperature);
#endif
	andor_error_num = SetTemperature(target_temperature);
	if(andor_error_num != DRV_SUCCESS)
	{
		Temperature_Error_Number = 2;
		sprintf(Temperature_Error_String,"CCD_Temperature_Set:Andor SetTemperature failure(%lu).",
			andor_error_num);
		return FALSE;
	}
	andor_error_num=CoolerON();
	if(andor_error_num != DRV_SUCCESS)
	{
		Temperature_Error_Number = 3;
		sprintf(Temperature_Error_String,"CCD_Temperature_Set:Andor CoolerON failure(%lu).",andor_error_num);
		return FALSE;
	}
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Temperature_Set() returned TRUE.");
#endif
	return TRUE;
}

/**
 * Get the current value of the error number.
 * @return The current value of the error number.
 */
int CCD_Temperature_Get_Error_Number(void)
{
	return Temperature_Error_Number;
}

/**
 * The error routine that reports any errors occuring in ccd_temperature in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Temperature_Error(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Temperature_Error_Number == 0)
		sprintf(Temperature_Error_String,"Logic Error:No Error defined");
	fprintf(stderr,"%s CCD_Temperature:Error(%d) : %s\n",time_string,
		Temperature_Error_Number,Temperature_Error_String);
}

/**
 * The error routine that reports any errors occuring in ccd_temperature in a standard way. This routine places the
 * generated error string at the end of a passed in string argument.
 * @param error_string A string to put the generated error in. This string should be initialised before
 * being passed to this routine. The routine will try to concatenate it's error string onto the end
 * of any string already in existance.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Temperature_Error_String(char *error_string)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Temperature_Error_Number == 0)
		sprintf(Temperature_Error_String,"Logic Error:No Error defined");
	sprintf(error_string+strlen(error_string),"%s CCD_Temperature:Error(%d) : %s\n",time_string,
		Temperature_Error_Number,Temperature_Error_String);
}

/* -----------------------------------------------------------------------------
** 	internal functions 
** ----------------------------------------------------------------------------- */

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2022/03/14 15:23:03  cjm
** Removed SDSU specific temperature code.
**
** Revision 1.2  2010/03/26 14:39:49  cjm
** Changed from bitwise to absolute logging levels.
**
** Revision 1.1  2009/10/15 10:16:23  cjm
** Initial revision
**
** Revision 0.12  2006/05/16 14:14:08  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.11  2004/05/16 14:28:18  cjm
** Re-wrote abort code.
**
** Revision 0.10  2004/03/03 15:43:49  cjm
** Added comments amount units of temperature.
**
** Revision 0.9  2002/12/16 16:49:36  cjm
** Removed Error routines resetting error number to zero.
**
** Revision 0.8  2002/11/07 19:13:39  cjm
** Changes to make library work with SDSU version 1.7 DSP code.
**
** Revision 0.7  2001/07/13 09:48:48  cjm
** Added CCD_Temperature_Get_Heater_ADU.
**
** Revision 0.6  2001/06/04 14:42:44  cjm
** Added LOGGING code.
**
** Revision 0.5  2000/09/25 09:51:28  cjm
** Changes to use with v1.4 SDSU DSP code.
**
** Revision 0.4  2000/04/13 13:09:30  cjm
** Added current time to error routines.
**
** Revision 0.4  2000/04/13 13:03:14  cjm
** Changed error routine to print current time.
**
** Revision 0.3  2000/03/01 15:44:41  cjm
** Backup.
**
** Revision 0.2  2000/02/22 16:07:55  cjm
** Added calls to CCD_DSP_Set_Abort.
**
** Revision 0.1  2000/01/25 14:57:27  cjm
** initial revision (PCI version).
**
*/
