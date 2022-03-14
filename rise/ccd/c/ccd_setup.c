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
/* ccd_setup.c
** low level ccd library
** $Header: /space/home/eng/cjm/cvs/rise/ccd/c/ccd_setup.c,v 1.4 2022-03-14 15:23:03 cjm Exp $
*/
/**
 * ccd_setup.c contains routines to perform the setting of the SDSU CCD Controller, prior to performing
 * exposures.
 * @author SDSU, Chris Mottram
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
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <math.h>
#include <time.h>
#include <unistd.h>
#ifndef _POSIX_TIMERS
#include <sys/time.h>
#endif
#include "log_udp.h"
#include "ccd_exposure.h"
#include "ccd_global.h"
#include "ccd_temperature.h"
#include "ccd_setup.h"
#include "atmcdLXd.h"

/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: ccd_setup.c,v 1.4 2022-03-14 15:23:03 cjm Exp $";

/* #defines */
/**
 * The width of bias strip to use when windowing.
 */
#define SETUP_WINDOW_BIAS_WIDTH		(53)

/* data types */
/**
 * Data type used to hold local data to ccd_setup. Fields are:
 * <dl>
 * <dt>NCols</dt> <dd>The number of columns that will be used on the CCD.</dd>
 * <dt>NRows</dt> <dd>The number of rows that will be used on the CCD.</dd>
 * <dt>NSBin</dt> <dd>The amount of binning of columns on the CCD.</dd>
 * <dt>NPBin</dt> <dd>The amount of binning of rows on the CCD.</dd>
 * <dt>Window_Flags</dt> <dd>The window flags for this setup. Determines which of the four possible windows
 * 	are in use for this setup.</dd>
 * <dt>Window_List</dt> <dd>A list of window positions on the CCD. Theere are a maximum of CCD_SETUP_WINDOW_COUNT
 * 	windows. The windows should not overlap in either dimension.</dd>
 * <dt>Dimension_Complete</dt> <dd>A boolean value indicating whether the dimension setup was completed
 * 	successfully.</dd>
 * <dt>Setup_In_Progress</dt> <dd>A boolean value indicating whether the setup operation is in progress.</dd>
 * </dl>
 */
struct Setup_Struct
{
	int NCols;
	int NRows;
	int NSBin;
	int NPBin;
	int Window_Flags;
	struct CCD_Setup_Window_Struct Window_List[CCD_SETUP_WINDOW_COUNT];
	int Dimension_Complete;
	int Setup_In_Progress;
};

/* external variables */

/* local variables */
/**
 * Variable holding error code of last operation performed by ccd_setup.
 */
static int Setup_Error_Number = 0;
/**
 * Local variable holding description of the last error that occured.
 */
static char Setup_Error_String[CCD_GLOBAL_ERROR_STRING_LENGTH] = "";
/**
 * Data holding the current status of ccd_setup.
 * @see #Setup_Struct
 */
static struct Setup_Struct Setup_Data;

/* local function definitions */
static int Setup_Binning(int nsbin,int npbin);
static int Setup_Dimensions(int ncols,int nrows);
static int Setup_Window_List(int window_flags,struct CCD_Setup_Window_Struct window_list[]);
static int Setup_Controller_Windows(void);


/* external functions */
/**
 * This routine sets up ccd_setup internal variables.
 * It should be called at startup.
 */
void CCD_Setup_Initialise(void)
{
	int i;

	Setup_Error_Number = 0;
	Setup_Data.NCols = 0;
	Setup_Data.NRows = 0;
	Setup_Data.NSBin = 0;
	Setup_Data.NPBin = 0;
	Setup_Data.Window_Flags = 0;
	for(i=0;i<CCD_SETUP_WINDOW_COUNT;i++)
	{
		Setup_Data.Window_List[i].X_Start = -1;
		Setup_Data.Window_List[i].Y_Start = -1;
		Setup_Data.Window_List[i].X_End = -1;
		Setup_Data.Window_List[i].Y_End = -1;
	}
	Setup_Data.Dimension_Complete = FALSE;
/* print some compile time information to stdout */
	fprintf(stdout,"CCD_Setup_Initialise:%s.\n",rcsid);
}

/**
 * The routine that sets up the CCD Controller. This routine does the following:
 * <ul>
 * <li>Sets the arrays target temperature.</li>
 * </ul>
 * Array dimension information also needs to be setup before the controller can take exposures 
 * (see CCD_Setup_Dimensions).
 * This routine can be aborted with CCD_Setup_Abort.
 * @param target_temperature Specifies the target temperature the CCD is meant to run at. 
 * @return Returns TRUE if the setup is successfully completed, FALSE if the setup fails or is aborted.
 * @see #CCD_Temperature_Set
 * @see #CCD_Setup_Dimensions
 * @see #CCD_Setup_Abort
 */
int CCD_Setup_Startup(double target_temperature)
{
/* ANDOR STUFF .....  */
	long lNumCameras;
	int iSelectedCamera  = 0; 
	unsigned long error;
	int andorTargetTemp=(int)target_temperature; /* ANDOR function takes an int! */
	long lCameraHandle;

	Setup_Error_Number = 0;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup(temperature=%.2f) started.",
			      target_temperature);
#endif
/* we are in a setup routine */
	Setup_Data.Setup_In_Progress = TRUE;
/* reset completion flags - even dimension flag is reset, as the controller itself is reset */
	Setup_Data.Dimension_Complete = FALSE;

	/* Load the parameter file */
	eSTAR_Config_Parse_File("ccs.properties",&rProperties);	
	eSTAR_Config_Print_Error();
	eSTAR_Config_Get_Int(&rProperties,"ccs.libccd.cooling",&(mrParams.ccdCool));
	
	GetAvailableCameras(&lNumCameras);
 
	if (iSelectedCamera < lNumCameras && iSelectedCamera >= 0) 
	{ 
		GetCameraHandle(iSelectedCamera, &lCameraHandle);
		SetCurrentCamera(lCameraHandle); 
	}

	sleep(1);
	error = Initialize("/usr/local/etc/andor");
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup:Andor Camera %d selected",iSelectedCamera);
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup:ANDOR initialise %d",error);
#endif
	sleep(5);

	if(error != DRV_SUCCESS)
	{
		Setup_Error_Number = 2;
		sprintf(Setup_Error_String,"CCD_Setup_Startup:Andor Camera Initalise failure(%lu)...exiting",error);
		return FALSE;
	}
	error=SetReadMode(4);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup:ANDOR SetReadMode IMAGE %lu",error);
#endif
	error=SetAcquisitionMode(1);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup:ANDOR SetAquisitionMode Single Scan %lu",
			      error);
#endif
	error = SetTemperature(andorTargetTemp);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: Temperature target set at %d",
			      andorTargetTemp);
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: ANDOR SetTemperature returned %lu",error);
#endif
	if(mrParams.ccdCool==1) 
	{
		error=CoolerON();
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: CoolerON called %lu",error);
#endif
	} 
	else 
	{
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"Cooling disabled...");
#endif
	}
#ifdef FTMODE
#if FTMODE > 0
	error=SetFrameTransferMode(1);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: SetFrameTransferMode ON %lu",error);
#endif
#endif
#else
	error=SetFrameTransferMode(0);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: SetFrameTransferMode OFF %lu",error);
#endif
#endif
	/*
	while (GetTemperature(&cTemp)!=DRV_TEMPERATURE_STABILIZED && mrParams.ccdCool==1)
	{
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: Cooling (%d degC)...please wait",
				      cTemp);
#endif
		sleep (10);
	}
	*/
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Startup: Initialisation complete...");
#endif
	Setup_Data.Setup_In_Progress = FALSE;
	return TRUE;
}

/**
 * Routine to shut down the SDSU CCD Controller board. This consists of:
 * <ul>
 * <li>Reseting the setup completion flags.
 * <li>Performing a power off command to switch off analogue voltages.
 * </ul>
 * It then just remains to close the connection to the astro device driver.
 * @see #CCD_Setup_Startup
 */
int CCD_Setup_Shutdown(void)
{
	/* Send a shutdown command to the ANDOR ccd */
	unsigned int andor_error = 0;
	static int shutdown_temp = 0;
	int cTemp = 999;
 
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Stutdown() started.");
#endif       
        andor_error=SetTemperature(shutdown_temp);
        andor_error=GetTemperature(&cTemp);
        while ((cTemp < shutdown_temp || cTemp==-999) && mrParams.ccdCool==1) 
	{ 
		andor_error=GetTemperature(&cTemp);
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,
				      "CCD_Setup_Shutdown: Shutting down...raising to %d (%d)",
				      shutdown_temp,cTemp);
#endif
		sleep (5);
	}

        CoolerOFF();
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Shutdown: Sent Cooler off to CCD at temp %d",cTemp);
#endif        
        ShutDown();     
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Shutdown: Sent ShutDown() to CCD");
#endif
	eSTAR_Config_Destroy_Properties(&rProperties);

	return TRUE;
}

/**
 * Routine to setup dimension information in the controller. This needs to be setup before an exposure
 * can take place. This routine must be called <b>after</b> the CCD_Setup_Startup routine.
 * This routine can be aborted with CCD_Setup_Abort.
 * @param ncols The number of columns in the image.
 * @param nrows The number of rows in the image to be readout from the CCD.
 * @param nsbin The amount of binning applied to pixels in columns. This parameter will change internally
 *	ncols.
 * @param npbin The amount of binning applied to pixels in rows.This parameter will change internally
 *	nrows.
 * @param window_flags Information on which of the sets of window positions supplied contain windows to be used.
 * @param window_list A list of CCD_Setup_Window_Structs defining the position of the windows. The list should
 * 	<b>always</b> contain <b>four</b> entries, one for each possible window. The window_flags parameter
 * 	determines which items in the list are used.
 * @return The routine returns TRUE on success and FALSE if an error occured.
 * @see #CCD_Setup_Startup
 * @see #Setup_Data
 * @see #Setup_Binning
 * @see #Setup_Dimensions
 * @see #Setup_Window_List
 * @see #CCD_Setup_Abort
 * @see #CCD_Setup_Window_Struct
 */
int CCD_Setup_Dimensions(int ncols,int nrows,int nsbin,int npbin,
			 int window_flags,struct CCD_Setup_Window_Struct window_list[])
{
  int error;

	Setup_Error_Number = 0;
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Dimensions(ncols=%d,nrows=%d,nsbin=%d,npbin=%d,"
		"window_flags=%d) started.",ncols,nrows,nsbin,npbin,
		window_flags);
#endif
/* we are in a setup routine */
	Setup_Data.Setup_In_Progress = TRUE;
/* reset abort flag - we havn't aborted yet! */
	CCD_Exposure_Set_Abort(FALSE); 
/* reset dimension flag */
	Setup_Data.Dimension_Complete = FALSE;
/* first do the binning */
	/*fprintf(stdout,"CCD_Setup_Dimensions: Dim currently set to COLSxROWS %dx%d\n",ncols,nrows); */
	if(nrows <= 0)
	{
		Setup_Data.Setup_In_Progress = FALSE;
		Setup_Error_Number = 24;
		sprintf(Setup_Error_String,"CCD_Setup_Dimensions:Illegal value:Number of Rows '%d'",
			nrows);
		return FALSE;
	}
	Setup_Data.NRows = nrows;
	if(ncols <= 0)
	{
		Setup_Data.Setup_In_Progress = FALSE;
		Setup_Error_Number = 25;
		sprintf(Setup_Error_String,"CCD_Setup_Dimensions:Illegal value:Number of Columns '%d'",
			ncols);
		return FALSE;
	}
	Setup_Data.NCols = ncols;
	if(!Setup_Binning(nsbin,npbin))
	{
		Setup_Data.Setup_In_Progress = FALSE;
		return FALSE; 
	}
/* if we have aborted - stop here */
	if(CCD_Exposure_Get_Abort())
	{
		Setup_Data.Setup_In_Progress = FALSE;
		Setup_Error_Number = 78;
		sprintf(Setup_Error_String,"CCD_Setup_Dimensions:Aborted");
		return FALSE;
	}
/* if we have aborted - stop here */
	if(CCD_Exposure_Get_Abort())
	{
		Setup_Data.Setup_In_Progress = FALSE;
		Setup_Error_Number = 79;
		sprintf(Setup_Error_String,"CCD_Setup_Dimensions:Aborted");
		return FALSE;
	}
/* setup final calculated dimensions */
	if(!Setup_Dimensions(Setup_Data.NCols,Setup_Data.NRows))
	{
		Setup_Data.Setup_In_Progress = FALSE;
		return FALSE;
	}
	else /*acknowlege dimensions complete*/ 
		Setup_Data.Dimension_Complete = TRUE;
	
	error = SetImage(npbin,nsbin,1,ncols,1,nrows);
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Dimensions: binning COLSxROWS %dx%d",npbin,nsbin);
        CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Dimensions: SetImage %dx%d  %d",
			      ncols/npbin,nrows/nsbin,error);
#endif


/* if we have aborted - stop here */
	if(CCD_Exposure_Get_Abort())
	{
		Setup_Data.Setup_In_Progress = FALSE;
		Setup_Error_Number = 80;
		sprintf(Setup_Error_String,"CCD_Setup_Dimensions:Aborted");
		return FALSE;
	}
/* setup windowing data */
	if(!Setup_Window_List(window_flags,window_list))
	{
		Setup_Data.Setup_In_Progress = FALSE;
		return FALSE;
	}
/* reset in progress information */
	Setup_Data.Setup_In_Progress = FALSE;
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Dimensions() returned TRUE.");
#endif
	return TRUE;
}

/**
 * Routine to abort a setup that is underway. This will cause CCD_Setup_Startup and CCD_Setup_Dimensions
 * to return FALSE as it will fail to complete the setup.
 * @see #CCD_Setup_Startup
 * @see #CCD_Setup_Dimensions
 */
void CCD_Setup_Abort(void)
{
#if LOGGING > 0
	CCD_Global_Log(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Abort() started.");
#endif
	/* diddly */
}

/**
 * Routine that returns the number of columns setup has set the SDSU CCD Controller to readout. This is the
 * number passed into CCD_Setup_Dimensions, however, binning will have
 * reduced the value (ncols = ncols passed in / nsbin), and some deinterlacing options require an even
 * number of columns.
 * @return The number of columns.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
int CCD_Setup_Get_NCols(void)
{
	return Setup_Data.NCols;
}

/**
 * Routine that returns the number of rows setup has set the SDSU CCD Controller to readout. This is the
 * number passed into CCD_Setup_Dimensions, however, binning will have
 * reduced the value (nrows = nrows passed in / npbin), and some deinterlacing options require an even
 * number of rows.
 * @return The number of rows.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
int CCD_Setup_Get_NRows(void)
{
	return Setup_Data.NRows;
}

/**
 * Routine that returns the column binning factor the last dimension setup has set the SDSU CCD Controller to. 
 * This is the number passed into CCD_Setup_Dimensions.
 * @return The columns binning number.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
int CCD_Setup_Get_NSBin(void)
{
	return Setup_Data.NSBin;
}

/**
 * Routine that returns the row binning factor the last dimension setup has set the SDSU CCD Controller to. 
 * This is the number passed into CCD_Setup_Dimensions.
 * @return The row binning number.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
int CCD_Setup_Get_NPBin(void)
{
	return Setup_Data.NPBin;
}

/**
 * Routine to return the number of pixels that will be read out from the CCD. This is the number of
 * columns x the number of rows (post binning) for full array images, and something more comlicated for
 * windowed readouts.
 * @return The number of pixels.
 * @see #Setup_Data
 * @see #CCD_SETUP_WINDOW_COUNT
 * @see #SETUP_WINDOW_BIAS_WIDTH
 */
int CCD_Setup_Get_Readout_Pixel_Count(void)
{
	int pixel_count,i,bias_width,box_width,box_height;

	if(Setup_Data.Window_Flags == 0)
	{
		/* the NCols and NRows variables should already have been adjusted for binning. */
		pixel_count = Setup_Data.NCols*Setup_Data.NRows;
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,
				      "CCD_Setup_Get_Readout_Pixel_Count: Rows: %d Cols: %d",
				      Setup_Data.NRows,Setup_Data.NCols);
#endif
	}
	else
	{
		pixel_count = 0;
		for(i=0;i<CCD_SETUP_WINDOW_COUNT;i++)
		{
			/* Note, relies on CCD_SETUP_WINDOW_ONE == (1<<0),
			** CCD_SETUP_WINDOW_TWO	== (1<<1),
			** CCD_SETUP_WINDOW_THREE == (1<<2) and
			** CCD_SETUP_WINDOW_FOUR == (1<<3) */
			if(Setup_Data.Window_Flags&(1<<i))
			{
				/* These next lines  must agree with Setup_Controller_Windows for this to work */
				bias_width = SETUP_WINDOW_BIAS_WIDTH;/* diddly - get this from parameters? */
				box_width = Setup_Data.Window_List[i].X_End-Setup_Data.Window_List[i].X_Start;
				box_height = Setup_Data.Window_List[i].Y_End-Setup_Data.Window_List[i].Y_Start;
				pixel_count += (box_width+bias_width)*box_height;
			}
		}/* end for */
	}
	return pixel_count;
}

/**
 * Routine to return the number of pixels in the specified window. 
 * @param window_index This is the index in the window list to return. The first window is at index zero
 * 	and the last at (CCD_SETUP_WINDOW_COUNT-1). This index must be within this range.
 * @return The number of pixels, or -1 if this window is not in use.
 * @see #Setup_Data
 * @see #CCD_SETUP_WINDOW_COUNT
 * @see #SETUP_WINDOW_BIAS_WIDTH
 */
int CCD_Setup_Get_Window_Pixel_Count(int window_index)
{
	int pixel_count,bias_width,box_width,box_height;

	if((window_index < 0) || (window_index >= CCD_SETUP_WINDOW_COUNT))
	{
		Setup_Error_Number = 61;
		sprintf(Setup_Error_String,"CCD_Setup_Get_Window_Pixel_Count:Window Index '%d' out of range:"
			"['%d' to '%d'] inclusive.",window_index,0,CCD_SETUP_WINDOW_COUNT-1);
		return -1;
	}
	/* Note, relies on CCD_SETUP_WINDOW_ONE == (1<<0),
	** CCD_SETUP_WINDOW_TWO	== (1<<1),
	** CCD_SETUP_WINDOW_THREE == (1<<2) and
	** CCD_SETUP_WINDOW_FOUR == (1<<3) */
	if(Setup_Data.Window_Flags&(1<<window_index))
	{
		/* These next lines  must agree with Setup_Controller_Windows for this to work */
		bias_width = SETUP_WINDOW_BIAS_WIDTH;/* diddly - get this from parameters? */
		box_width = Setup_Data.Window_List[window_index].X_End-Setup_Data.Window_List[window_index].X_Start;
		box_height = Setup_Data.Window_List[window_index].Y_End-Setup_Data.Window_List[window_index].Y_Start;
		pixel_count = (box_width+bias_width)*box_height;
	}
	else
		pixel_count = -1;
	return pixel_count;
}

/**
 * Routine to return the width of the specified window. 
 * @param window_index This is the index in the window list to return. The first window is at index zero
 * 	and the last at (CCD_SETUP_WINDOW_COUNT-1). This index must be within this range.
 * @return The width of the window (including any added bias strips), or -1 if this window is not in use.
 * @see #Setup_Data
 * @see #CCD_SETUP_WINDOW_COUNT
 * @see #SETUP_WINDOW_BIAS_WIDTH
 */
int CCD_Setup_Get_Window_Width(int window_index)
{
	int box_width,bias_width;

	if((window_index < 0) || (window_index >= CCD_SETUP_WINDOW_COUNT))
	{
		Setup_Error_Number = 62;
		sprintf(Setup_Error_String,"CCD_Setup_Get_Window_Width:Window Index '%d' out of range:"
			"['%d' to '%d'] inclusive.",window_index,0,CCD_SETUP_WINDOW_COUNT-1);
		return -1;
	}
	/* Note, relies on CCD_SETUP_WINDOW_ONE == (1<<0),
	** CCD_SETUP_WINDOW_TWO	== (1<<1),
	** CCD_SETUP_WINDOW_THREE == (1<<2) and
	** CCD_SETUP_WINDOW_FOUR == (1<<3) */
	if(Setup_Data.Window_Flags&(1<<window_index))
	{
		/* These next lines  must agree with Setup_Controller_Windows for this to work */
		bias_width = SETUP_WINDOW_BIAS_WIDTH;/* diddly - get this from parameters? */
		box_width = Setup_Data.Window_List[window_index].X_End-Setup_Data.Window_List[window_index].X_Start+
			bias_width;
	}
	else
		box_width = -1;
	return box_width;
}

/**
 * Routine to return the height of the specified window. 
 * @param window_index This is the index in the window list to return. The first window is at index zero
 * 	and the last at (CCD_SETUP_WINDOW_COUNT-1). This index must be within this range.
 * @return The height of the window, or -1 if this window is not in use.
 * @see #Setup_Data
 * @see #CCD_SETUP_WINDOW_COUNT
 */
int CCD_Setup_Get_Window_Height(int window_index)
{
	int box_height;

	if((window_index < 0) || (window_index >= CCD_SETUP_WINDOW_COUNT))
	{
		Setup_Error_Number = 63;
		sprintf(Setup_Error_String,"CCD_Setup_Get_Window_Height:Window Index '%d' out of range:"
			"['%d' to '%d'] inclusive.",window_index,0,CCD_SETUP_WINDOW_COUNT-1);
		return -1;
	}
	/* Note, relies on CCD_SETUP_WINDOW_ONE == (1<<0),
	** CCD_SETUP_WINDOW_TWO	== (1<<1),
	** CCD_SETUP_WINDOW_THREE == (1<<2) and
	** CCD_SETUP_WINDOW_FOUR == (1<<3) */
	if(Setup_Data.Window_Flags&(1<<window_index))
	{
		/* These next lines  must agree with Setup_Controller_Windows for this to work */
		box_height = Setup_Data.Window_List[window_index].Y_End-Setup_Data.Window_List[window_index].Y_Start;
	}
	else
		box_height = -1;
	return box_height;
}

/**
 * Routine that returns the window flags number of the last successful dimension setup.
 * @return The window flags.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
int CCD_Setup_Get_Window_Flags(void)
{
	return Setup_Data.Window_Flags;
}

/**
 * Routine to return one of the windows setup on the CCD chip. Use CCD_Setup_Get_Window_Flags to
 * determine whether the window is in use.
 * @param window_index This is the index in the window list to return. The first window is at index zero
 * 	and the last at (CCD_SETUP_WINDOW_COUNT-1). This index must be within this range.
 * @param window An address of a structure to hold the window data. This is filled with the
 * 	requested window data.
 * @return This routine returns TRUE if the window_index is in range and the window data is filled in,
 * 	FALSE if the window_index is out of range (an error is setup).
 * @see #CCD_SETUP_WINDOW_COUNT
 * @see #CCD_Setup_Window_Struct
 * @see #CCD_Setup_Get_Window_Flags
 * @see #Setup_Data
 */
int CCD_Setup_Get_Window(int window_index,struct CCD_Setup_Window_Struct *window)
{
	if((window_index < 0) || (window_index >= CCD_SETUP_WINDOW_COUNT))
	{
		Setup_Error_Number = 1;
		sprintf(Setup_Error_String,"CCD_Setup_Get_Window:Window Index '%d' out of range:"
			"['%d' to '%d'] inclusive.",window_index,0,CCD_SETUP_WINDOW_COUNT-1);
		return FALSE;
	}
	if(window == NULL)
	{
		Setup_Error_Number = 49;
		sprintf(Setup_Error_String,"CCD_Setup_Get_Window:Window Index '%d':Null pointer.",window_index);
		return FALSE;
	}
	(*window) = Setup_Data.Window_List[window_index];
	return TRUE;
}

/**
 * Routine to return whether CCD_Setup_Startup and CCD_Setup_Dimensions completed successfully,
 * and the controller is in a state suitable to do an exposure. This is determined by examining the
 * Completion flags in Setup_Data.
 * @return Returns TRUE if setup was completed, FALSE otherwise.
 * @see #Setup_Data
 * @see #CCD_Setup_Startup
 * @see #CCD_Setup_Dimensions
 */
int CCD_Setup_Get_Setup_Complete(void)
{
	int status = 0;
        GetStatus(&status);
	if (status==20001 || status==20073) 
	{
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"CCD_Setup_Get_Setup_Complete:ANDOR return ok %d",
				      status);
#endif
		return (TRUE); /* For andor  IT*/ 
  	}
        else 
	{ 
#if LOGGING > 0
		CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,
				      "CCD_Setup_Get_Setup_Complete:ANDOR *** Not ready *** %d",status);
#endif
		return (FALSE); 
	}
}

/**
 * Routine to return whether a call to CCD_Setup_Startup or CCD_Setup_Dimensions is in progress. This is done
 * by examining Setup_In_Progress in Setup_Data.
 * @return Returns TRUE if the SDSU CCD Controller is in the process of being setup, FALSE otherwise.
 * @see #Setup_Data
 * @see #CCD_Setup_Startup
 * @see #CCD_Setup_Dimensions
 */
int CCD_Setup_Get_Setup_In_Progress(void)
{
	return Setup_Data.Setup_In_Progress;
}

/**
 * Get the current value of ccd_setup's error number.
 * @return The current value of ccd_setup's error number.
 */
int CCD_Setup_Get_Error_Number(void)
{
	return Setup_Error_Number;
}

/**
 * The error routine that reports any errors occuring in ccd_setup in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Setup_Error(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Setup_Error_Number == 0)
		sprintf(Setup_Error_String,"Logic Error:No Error defined");
	fprintf(stderr,"%s CCD_Setup:Error(%d) : %s\n",time_string,Setup_Error_Number,Setup_Error_String);
}

/**
 * The error routine that reports any errors occuring in ccd_setup in a standard way. This routine places the
 * generated error string at the end of a passed in string argument.
 * @param error_string A string to put the generated error in. This string should be initialised before
 * being passed to this routine. The routine will try to concatenate it's error string onto the end
 * of any string already in existance.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Setup_Error_String(char *error_string)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an error message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an error to display */
	if(Setup_Error_Number == 0)
		sprintf(Setup_Error_String,"Logic Error:No Error defined");
	sprintf(error_string+strlen(error_string),"%s CCD_Setup:Error(%d) : %s\n",time_string,
		Setup_Error_Number,Setup_Error_String);
}

/**
 * The warning routine that reports any warnings occuring in ccd_setup in a standard way.
 * @see ccd_global.html#CCD_Global_Get_Current_Time_String
 */
void CCD_Setup_Warning(void)
{
	char time_string[32];

	CCD_Global_Get_Current_Time_String(time_string,32);
	/* if the error number is zero an warning message has not been set up
	** This is in itself an error as we should not be calling this routine
	** without there being an warning to display */
	if(Setup_Error_Number == 0)
		sprintf(Setup_Error_String,"Logic Error:No Warning defined");
	fprintf(stderr,"%s CCD_Setup:Warning(%d) : %s\n",time_string,Setup_Error_Number,Setup_Error_String);
}

/* ------------------------------------------------------------------
**	Internal Functions
** ------------------------------------------------------------------ */
/**
 * Internal routine to set up the binning configuration for the SDSU CCD Controller. This routine checks
 * the binning values and saves them in Setup_Data, writes the
 * binning values to the controller boards, and re-calculates the stored columns and rows values to allow for
 * binning e.g. NCols = NCols/NSBin. This routine is called from CCD_Setup_Dimensions.
 * @param nsbin The amount of binning applied to pixels in columns. This parameter will change internally ncols.
 * @param npbin The amount of binning applied to pixels in rows.This parameter will change internally nrows.
 * @return Returns TRUE if the operation succeeds, FALSE if it fails.
 * @see #CCD_Setup_Dimensions
 * @see #Setup_Data
 */
static int Setup_Binning(int nsbin,int npbin)
{
	if(nsbin <= 0)
	{
		Setup_Error_Number = 26;
		sprintf(Setup_Error_String,"Setup_Binning:Illegal value:Horizontal Binning '%d'",nsbin);
		return FALSE;
	}
	Setup_Data.NSBin = nsbin;
	if(npbin <= 0)
	{
		Setup_Error_Number = 27;
		sprintf(Setup_Error_String,"Setup_Binning:Illegal value:Vertical Binning '%d'",npbin);
		return FALSE;
	}
	Setup_Data.NPBin = npbin;

/* will be sending the FINAL image size to the boards, so calculate them now */
	Setup_Data.NCols = Setup_Data.NCols/Setup_Data.NSBin;
	Setup_Data.NRows = Setup_Data.NRows/Setup_Data.NPBin;
	
	/* Andor stuff */
	return TRUE;
}

/**
 * Internal routine to set up the CCD dimensions for the SDSU CCD Controller. This routines writes the
 * dimension values to the controller boards using WRM.  This routine is called from CCD_Setup_Dimensions.
 * @param ncols The number of columns. This is usually Setup_Data.NCols, but will be different when
 *        windowing.
 * @param nrows The number of rows. This is usually Setup_Data.NRows, but will be different when
 *        windowing.
 * @return Returns TRUE if the operation succeeds, FALSE if it fails.
 * @see #CCD_Setup_Dimensions
 */
static int Setup_Dimensions(int ncols,int nrows)
{
	/* Andor set GetDimensions and return  IT*/
	int error = GetDetector(&ncols,&nrows);
	
#if LOGGING > 0
	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"Setup_Dimensions: Unbinned COLSxROWS %dx%d",ncols,nrows);
 	CCD_Global_Log_Format(LOG_VERBOSITY_VERBOSE,"Setup_Dimensions: GetDetector returned %d",error);	
#endif
	
	return TRUE;
}

/**
 * This routine sets the Setup_Data.Window_List from the passed in list of windows.
 * The windows are checked to ensure they don't overlap in the y (row) direction, and that sub-images are
 * all the same size. Only windows which are included in the window_flags parameter are checked.
 * If the windows are OK, Setup_Controller_Windows is called to write the windows to the SDSU controller.
 * @param window_flags Information on which of the sets of window positions supplied contain windows to be used.
 * @param window_list A list of CCD_Setup_Window_Structs defining the position of the windows. The list should
 * 	<b>always</b> contain <b>four</b> entries, one for each possible window. The window_flags parameter
 * 	determines which items in the list are used.
 * @return The routine returns TRUE on success and FALSE if an error occured.
 * @see #Setup_Data
 * @see #CCD_Setup_Window_Struct
 * @see #Setup_Controller_Windows
 */
static int Setup_Window_List(int window_flags,struct CCD_Setup_Window_Struct window_list[])
{
	int start_window_index,end_window_index,found;
	int start_x_size,start_y_size,end_x_size,end_y_size;

/* check non-overlapping in y (row) directions all sub-images are same size. */
	start_window_index = 0;
	end_window_index = 0;
	/* while there are active windows, keep checking */
	while((start_window_index < CCD_SETUP_WINDOW_COUNT)&&(end_window_index < CCD_SETUP_WINDOW_COUNT))
	{
	/* find a valid start window index */
		found = FALSE;
		while((found == FALSE)&&(start_window_index < CCD_SETUP_WINDOW_COUNT))
		{
			found = (window_flags&(1<<start_window_index));
			if(!found)
				start_window_index++;
		}
	/* find a valid end window index  after start window index */
		end_window_index = start_window_index+1;
		found = FALSE;
		while((found == FALSE)&&(end_window_index < CCD_SETUP_WINDOW_COUNT))
		{
			found = (window_flags&(1<<end_window_index));
			if(!found)
				end_window_index++;
		}
	/* if we found two valid windows, check the second does not overlap the first */
		if((start_window_index < CCD_SETUP_WINDOW_COUNT)&&(end_window_index < CCD_SETUP_WINDOW_COUNT))
		{
		/* is start window's Y_End greater or equal to end windows Y_Start? */
			if(window_list[start_window_index].Y_End >= window_list[end_window_index].Y_Start)
			{
				Setup_Error_Number = 46;
				sprintf(Setup_Error_String,"Setting Windows:Windows %d and %d overlap in Y (%d,%d)",
					start_window_index,end_window_index,window_list[start_window_index].Y_End,
					window_list[end_window_index].Y_Start);
				return FALSE;
			}
		/* check sub-images are the same size/are positive size */
			start_x_size = window_list[start_window_index].X_End-window_list[start_window_index].X_Start;
			start_y_size = window_list[start_window_index].Y_End-window_list[start_window_index].Y_Start;
			end_x_size = window_list[end_window_index].X_End-window_list[end_window_index].X_Start;
			end_y_size = window_list[end_window_index].Y_End-window_list[end_window_index].Y_Start;
			if((start_x_size != end_x_size)||(start_y_size != end_y_size))
			{
				Setup_Error_Number = 47;
				sprintf(Setup_Error_String,"Setting Windows:Windows are different sizes"
					"%d = (%d,%d),%d = (%d,%d).",
					start_window_index,start_x_size,start_y_size,
					end_window_index,end_x_size,end_y_size);
				return FALSE;
			}
		/* note both windows are same size, only need to check one for sensible size */
			if((start_x_size <= 0)||(start_y_size <= 0))
			{
				Setup_Error_Number = 48;
				sprintf(Setup_Error_String,"Setting Windows:Windows are too small(%d,%d).",
					start_x_size,start_y_size);
				return FALSE;
			}
		}
	/* check the next pair of windows, by setting the start point to the last end point in the list */
		start_window_index = end_window_index;
	}
/* copy parameters to Setup_Data.Window_List and Setup_Data.Window_Flags */
	if(window_flags&CCD_SETUP_WINDOW_ONE)
		Setup_Data.Window_List[0] = window_list[0];
	if(window_flags&CCD_SETUP_WINDOW_TWO)
		Setup_Data.Window_List[1] = window_list[1];
	if(window_flags&CCD_SETUP_WINDOW_THREE)
		Setup_Data.Window_List[2] = window_list[2];
	if(window_flags&CCD_SETUP_WINDOW_FOUR)
		Setup_Data.Window_List[3] = window_list[3];
	Setup_Data.Window_Flags = window_flags;
/* write parameters to window table on timing board */
	if(!Setup_Controller_Windows())
		return FALSE;
	return TRUE;
}

/**
 * Actually write the calculated Setup_Data windows to the SDSU controller, using SSS and SSP.
 * If no windowing is taking place, we use SSS to reset the window sizes to zero (turning them off in the DSP code).
 * We also call Setup_Dimensions to set NSR and NPR to an area equivalent to the total number of pixels
 * written back from the timing board to the PCI board.
 * @return The routine returns TRUE on success, and FALSE if something fails.
 * @see #Setup_Dimensions
 * @see #Setup_Data
 * @see #CCD_Setup_Window_Struct
 * @see #SETUP_WINDOW_BIAS_WIDTH
 */
static int Setup_Controller_Windows(void)
{
	/* No windowing to be used in Andor  */
	return TRUE;
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2010/03/26 14:39:49  cjm
** Changed from bitwise to absolute logging levels.
**
** Revision 1.2  2009/10/21 13:53:10  cjm
** Setup properties now read from rise.ccs.properties.
**
** Revision 1.1  2009/10/15 10:16:23  cjm
** Initial revision
**
** Revision 0.28  2006/05/17 18:01:59  cjm
** Fixed unused variables.
**
** Revision 0.27  2006/05/16 14:14:07  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.26  2004/11/04 15:59:35  cjm
** Added processing of CCD_DSP_DEINTERLACE_FLIP.
**
** Revision 0.25  2004/05/16 14:28:18  cjm
** Re-wrote abort code.
**
** Revision 0.24  2003/06/06 12:36:01  cjm
** CHanged vacuum gauge read implementation, now issues VON/VOF and
** averages multiple samples.
** Setup_Dimensions changed, and windowing code added (SSS/SSP).
**
** Revision 0.23  2003/03/26 15:44:48  cjm
** Added windowing implementation.
** Note Bias offsets etc hardcodeed at the moment.
**
** Revision 0.22  2002/12/16 16:49:36  cjm
** Removed Error routines resetting error number to zero.
**
** Revision 0.21  2002/12/03 17:13:19  cjm
** Added CCD_Setup_Get_Vacuum_Gauge_MBar, CCD_Setup_Get_Vacuum_Gauge_ADU routines.
**
** Revision 0.20  2002/11/08 10:35:43  cjm
** Reversed order of calls of CCD_Interface_Memory_Map and Setup_PCI_Board.
** CCD_Interface_Memory_Map calls mmap, which in the device driver calls a HCVR
** command. Normally this ordering makes no difference, but it does
** when the PCI rom is not correct.
**
** Revision 0.19  2002/11/07 19:13:39  cjm
** Changes to make library work with SDSU version 1.7 DSP code.
**
** Revision 0.18  2001/06/04 14:42:17  cjm
** Added LOGGING code.
**
** Revision 0.17  2001/02/05 17:04:48  cjm
** More work on windowing.
**
** Revision 0.16  2001/01/31 16:35:18  cjm
** Added tests for filename is NULL in DSP download code.
**
** Revision 0.15  2000/12/19 17:52:47  cjm
** New filter wheel code.
**
** Revision 0.14  2000/06/19 08:48:34  cjm
** Backup.
**
** Revision 0.13  2000/06/13 17:14:13  cjm
** Changes to make Ccs agree with voodoo.
**
** Revision 0.12  2000/05/26 08:56:09  cjm
** Added CCD_Setup_Get_Window.
**
** Revision 0.11  2000/05/25 08:44:46  cjm
** Gain settings now held in Setup_Data so that CCD_Setup_Get_Gain
** can return the current gain.
** Some changes to internal routines (parameter checking now in them where applicable).
**
** Revision 0.10  2000/04/13 13:06:59  cjm
** Added current time to error routines.
**
** Revision 0.10  2000/04/13 13:04:46  cjm
** Changed error routine to print out current time.
**
** Revision 0.9  2000/03/02 16:46:53  cjm
** Converted Setup_Hardware_Test from internal routine to external CCD_Setup_Hardware_Test.
**
** Revision 0.8  2000/03/01 15:44:41  cjm
** Backup.
**
** Revision 0.7  2000/02/23 11:54:00  cjm
** Removed setting reply buffer bit on startup/shutdown.
** This is now handled by the latest astropci driver.
**
** Revision 0.6  2000/02/14 17:09:35  cjm
** Added some get routines to get data from Setup_Data:
** CCD_Setup_Get_NSBin
** CCD_Setup_Get_NPBin
** CCD_Setup_Get_Window_Flags
** CCD_Setup_Get_Filter_Wheel_Position
** These are to be used for get status commands.
**
** Revision 0.5  2000/02/10 12:07:38  cjm
** Added interface board to hardware test.
**
** Revision 0.4  2000/02/10 12:01:19  cjm
** Modified CCD_Setup_Shutdown with call to CCD_DSP_Command_WRM_No_Reply to switch off controller replies.
**
** Revision 0.3  2000/02/02 15:58:32  cjm
** Changed SETUP_ATTR to struct Setup_Struct.
**
** Revision 0.2  2000/02/02 15:55:14  cjm
** Binning and windowing addded.
**
** Revision 0.1  2000/01/25 14:57:27  cjm
** initial revision (PCI version).
**
*/
