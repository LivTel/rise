/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Rise.

    NGAT is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    NGAT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NGAT; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
// CCDLibrary.java
// $Header: /space/home/eng/cjm/cvs/rise/ngat/rise/ccd/CCDLibrary.java,v 1.3 2022-03-14 16:10:35 cjm Exp $
package ngat.rise.ccd;

import java.lang.*;
import java.util.List;
import java.util.Vector;
import ngat.util.logging.*;

/**
 * This class supports an interface to the SDSU CCD Controller library, for controlling CCDs.
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class CCDLibrary
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: CCDLibrary.java,v 1.3 2022-03-14 16:10:35 cjm Exp $");

// ccd_exposure.h
	/* These constants should be the same as those in ccd_exposure.h */
	/**
	 * Exposure status number, showing that no exposure is underway at the present moment.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_NONE =  		0;
	/**
	 * Exposure status number, showing that the library is waiting for the right moment to open the shutter.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_WAIT_START = 	1;
	/**
	 * Exposure status number, showing that the CCD is being cleared at the moment.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_CLEAR = 		2;
	/**
	 * Exposure status number, showing that an exposure is underway at the present moment.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_EXPOSE = 		3;
	/**
	 * Exposure status number, showing that a readout is about to start, and we should
	 * stop sending commands to the controller that don't work during a readout.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_PRE_READOUT = 	4;
	/**
	 * Exposure status number, showing that a readout is underway at the present moment.
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_READOUT = 		5;
	/**
	 * Exposure status number, showing that the readout of the exposure has been completed,
	 * and the data is being post-processed (byte swapped/de-interlaced/saved to disc).
	 * @see #CCDExposureGetExposureStatus
	 */
	public final static int CCD_EXPOSURE_STATUS_POST_READOUT = 	6;

// ccd_setup.h 
	/* These constants should be the same as those in ccd_setup.h */
	/**
	 * The number of windows the controller can put on the CCD.
	 */
	public final static int CCD_SETUP_WINDOW_COUNT = 		4;
	/**
	 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
	 * first window position is to be used.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_SETUP_WINDOW_ONE =			(1<<0);
	/**
	 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
	 * second window position is to be used.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_SETUP_WINDOW_TWO =			(1<<1);
	/**
	 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
	 * third window position is to be used.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_SETUP_WINDOW_THREE =		(1<<2);
	/**
	 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
	 * fourth window position is to be used.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_SETUP_WINDOW_FOUR =			(1<<3);
	/**
	 * Window flag used as part of the window_flags bit-field parameter of CCDSetupDimensions to specify all the
	 * window positions are to be used.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_SETUP_WINDOW_ALL =			(CCD_SETUP_WINDOW_ONE|CCD_SETUP_WINDOW_TWO|
								CCD_SETUP_WINDOW_THREE|CCD_SETUP_WINDOW_FOUR);

// ccd_exposure.h
	private native void CCD_Exposure_Expose(boolean open_shutter,
		long startTime,int exposureTime,List filenameList) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that takes a bias frame.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Exposure_Bias(String filename) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does a readout of the CCD.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Exposure_Read_Out_CCD(String filename) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that aborts an exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Exposure_Abort() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to librise_ccd routine thats returns whether an exposure is currently in progress.
	 */
	private native int CCD_Exposure_Get_Exposure_Status();
	/**
	 * Native wrapper to libccd routine thats returns the length of the last exposure length set.
c	 */
	private native int CCD_Exposure_Get_Exposure_Length();
	/**
	 * Native wrapper to libccd routine thats returns the number of milliseconds since the EPOCH of
	 * the exposure start time.
	 */
	private native long CCD_Exposure_Get_Exposure_Start_Time();
	/**
	 * Native wrapper to libccd routine that allows us to set how many seconds before the exposure
	 * is due to start we send the CLR (clear array) command to the controller.
	 * @param time The time in seconds. This should be greater than the time the CLR command takes to
	 * 	clock all accumulated charge off the CCD.
	 */
	private native void CCD_Exposure_Set_Start_Exposure_Clear_Time(int time);
	/**
	 * Native wrapper to libccd routine to set the amount of time, in milliseconds, 
	 * before the desired start of exposure that we should send the
	 * SEX (start exposure) command, to allow for transmission delay.
	 * @param time The time, in milliseconds.
	 */
	private native void CCD_Exposure_Set_Start_Exposure_Offset_Time(int time);
	/**
	 * Native wrapper to libccd routine to set the amount of time, in milleseconds, 
	 * remaining for an exposure when we stop sleeping and tell the
	 * interface to enter readout mode. 
	 * @param time The time, in milliseconds. Note, because the exposure time is read every second, it is best
	 * 	not have have this constant an exact multiple of 1000.
	 */
	private native void CCD_Exposure_Set_Readout_Remaining_Time(int time);
	/**
	 * Native wrapper to return ccd_exposure's error number.
	 */
	private native int CCD_Exposure_Get_Error_Number();

// ccd_global.h
	/**
	 * Native wrapper to libccd routine that sets up the CCD library for use.
	 */
	private native void CCD_Global_Initialise();
	/**
	 * Native wrapper to libccd routine that prints error values.
	 */
	private native void CCD_Global_Error();
	/**
	 * Native wrapper to libccd routine that gets error values into a string.
	 */
	private native String CCD_Global_Error_String();

// ccd_multrun.h
	/**
	 * Native wrapper to librise_ccd routine that does an exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
  	private native void CCD_Multrun_Expose(boolean open_shutter,long startTime, 
		int exposureTime,long exposures, List headers) throws CCDLibraryNativeException;
  	private native void CCD_Multflat_Expose(boolean open_shutter,long startTime, 
		int exposureTime,long exposures, List headers) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to librise_ccd routine thats returns whether an exposure is currently in progress.
	 */
	private native int CCD_Multrun_Get_Exposure_Status();
	/**
	 * Native wrapper to librise_ccd routine thats returns the elapsed exposure time in milliseconds.
	 */
	private native int CCD_Multrun_Get_Elapsed_Exposure_Time();

// ccd_setup.h
	/**
	 * Native wrapper to librise_ccd routine that does the CCD setup.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Startup(double target_temperature) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does the CCD shutdown.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Shutdown() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does the CCD dimensions setup.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Dimensions(int ncols,int nrows,int nsbin,int npbin,int window_flags,
		CCDLibrarySetupWindow window_list[]) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that aborts the CCD setup.
	 */
	private native void CCD_Setup_Abort();
	/**
	 * Native wrapper to libccd routine that gets the number of columns.
	 */
	private native int CCD_Setup_Get_NCols();
	/**
	 * Native wrapper to libccd routine that gets the number of Rows.
	 */
	private native int CCD_Setup_Get_NRows();
	/**
	 * Native wrapper to libccd routine that gets the column binning.
	 */
	private native int CCD_Setup_Get_NSBin();
	/**
	 * Native wrapper to libccd routine that gets the row binning.
	 */
	private native int CCD_Setup_Get_NPBin();
	/**
	 * Native wrapper to libccd routine that gets the setup window flags.
	 */
	private native int CCD_Setup_Get_Window_Flags();
	/**
	 * Native wrapper to libccd routine that gets a setup CCD window.
	 */
	private native CCDLibrarySetupWindow CCD_Setup_Get_Window(int windowIndex) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the width (inclusive of bias strips) of the specified window.
	 */
	private native int CCD_Setup_Get_Window_Width(int window_index);
	/**
	 * Native wrapper to libccd routine that gets the height of the specified window.
	 */
	private native int CCD_Setup_Get_Window_Height(int window_index);
	/**
	 * Native wrapper to libccd routine that gets whether a setup operation has been completed successfully.
	 */
	private native boolean CCD_Setup_Get_Setup_Complete();
	/**
	 * Native wrapper to libccd routine that gets whether a setup operation is in progress.
	 */
	private native boolean CCD_Setup_Get_Setup_In_Progress();
	/**
	 * Native wrapper to return ccd_setup's error number.
	 */
	private native int CCD_Setup_Get_Error_Number();

// ccd_temperature.h
	/**
	 * Native wrapper to libccd routine that gets the current temperature of the CCD.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Temperature_Get(CCDLibraryDouble temperature) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that sets the current temperature of the CCD.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Temperature_Set(double target_temperature) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the ADU counts from the utility board temperature sensor.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Temperature_Get_Utility_Board_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the current heater ADU counts.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Temperature_Get_Heater_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to return ccd_temperature's error number.
	 */
	private native int CCD_Temperature_Get_Error_Number();

//internal C layer initialisation
	/**
	 * Native method that allows the JNI layer to store a reference to this Class's logger.
	 * @param logger The logger for this class.
	 */
	private native void initialiseLoggerReference(Logger logger);
	/**
	 * Native method that allows the JNI layer to release the global reference to this Class's logger.
	 */
	private native void finaliseLoggerReference();

// per instance variables
	/**
	 * The logger to log messages to.
	 */
	protected Logger logger = null;

// static code block
	/**
	 * Static code to load libccd, the shared C library that implements an interface to the
	 * SDSU CCD Controller.
	 */
	static
	{
		System.loadLibrary("rise_ccd");
	}

// constructor
	/**
	 * Constructor. Constructs the logger, and sets the C layers reference to it.
	 * @see #logger
	 * @see #initialiseLoggerReference
	 */
	public CCDLibrary()
	{
		super();
		logger = LogManager.getLogger(this);
		initialiseLoggerReference(logger);
	}

	/**
	 * Finalize method for this class, delete JNI global references.
	 * @see #finaliseLoggerReference
	 */
	protected void finalize() throws Throwable
	{
		super.finalize();
		finaliseLoggerReference();
	}

// ccd_exposure.h
	/**
	 * Routine to perform an exposure.
	 * @param open_shutter Determines whether the shutter should be opened to do the exposure. The shutter might
	 * 	be left closed to perform calibration images etc.
	 * @param startTime The start time, in milliseconds since the epoch (1st January 1970) to start the exposure.
	 * 	Passing the value -1 will start the exposure as soon as possible.
	 * @param exposureTime The number of milliseconds to expose the CCD.
	 * @param filename The filename to save the exposure into. This assumes the CCD is not configured to
	 *                 be windowed.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if 
	 * CCD_Exposure_Expose  failed.
	 * @see #CCD_Exposure_Expose
	 */
	public void CCDExposureExpose(boolean open_shutter,long startTime,int exposureTime,String filename) 
		throws CCDLibraryNativeException
	{
		List filenameList = null;

		filenameList = new Vector();
		filenameList.add(filename);
		CCD_Exposure_Expose(open_shutter,startTime,exposureTime,filenameList);
	}

	/**
	 * Routine to perform an exposure.
	 * @param open_shutter Determines whether the shutter should be opened to do the exposure. The shutter might
	 * 	be left closed to perform calibration images etc.
	 * @param startTime The start time, in milliseconds since the epoch (1st January 1970) to start the exposure.
	 * 	Passing the value -1 will start the exposure as soon as possible.
	 * @param exposureTime The number of milliseconds to expose the CCD.
	 * @param filenameList A list of filename strings (one per window) to save the exposure into.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if 
	 * CCD_Exposure_Expose  failed.
	 * @see #CCD_Exposure_Expose
	 */
	public void CCDExposureExpose(boolean open_shutter,long startTime,int exposureTime,List filenameList) 
		throws CCDLibraryNativeException
	{
		CCD_Exposure_Expose(open_shutter,startTime,exposureTime,filenameList);
	}

	/**
	 * Routine to take a bias frame and save the result to a file.
	 * A bias frame is taken by clearing the ccd array and then immediately reading it out to disk.
	 * This cannot be done with a call to CCDExposureExpose as this routine takes a <b>non-zero</b>
	 * exposure time.
	 * @param filename The filename to save the read out data into.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it fails.
	 * @see #CCD_Exposure_Bias
	 */
	public void CCDExposureBias(String filename) throws CCDLibraryNativeException
	{
		CCD_Exposure_Bias(filename);
	}

	/**
	 * Routine to readout data on the CCD to a file.
	 * @param filename The filename to save the read out data into.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it fails.
	 * @see #CCD_Exposure_Read_Out_CCD
	 */
	public void CCDExposureReadOutCCD(String filename) throws CCDLibraryNativeException
	{
		CCD_Exposure_Read_Out_CCD(filename);
	}

	/**
	 * Routine to abort an exposure that is underway. You can see if an exposure is in progress using 
	 * CCDExposureGetExposureStatus. 
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it fails.
	 * @see #CCDExposureGetExposureStatus 
	 * @see #CCDExposureExpose 
	 * @see #CCD_Exposure_Abort
	 */
	public void CCDExposureAbort() throws CCDLibraryNativeException
	{
		CCD_Exposure_Abort();
	}

	/**
	 * Returns whether an exposure is currently in progress. The library keeps track of whether a call to
	 * <a href="#CCDExposureExpose">CCDExposureExpose</a> is in progress, and whether it is exposing or reading out
	 * @return Returns the exposure status.
	 * @see #CCD_EXPOSURE_STATUS_NONE
	 * @see #CCD_EXPOSURE_STATUS_WAIT_START
	 * @see #CCD_EXPOSURE_STATUS_CLEAR
	 * @see #CCD_EXPOSURE_STATUS_EXPOSE
	 * @see #CCD_EXPOSURE_STATUS_PRE_READOUT
	 * @see #CCD_EXPOSURE_STATUS_READOUT
	 * @see #CCD_EXPOSURE_STATUS_POST_READOUT
	 * @see #CCDExposureExpose
	 * @see #CCD_Exposure_Get_Exposure_Status
	 */
	public int CCDExposureGetExposureStatus()
	{
		return CCD_Exposure_Get_Exposure_Status();
	}

	/**
	 * Method to get the exposure length the controller was last set to.
	 * @return The exposure length.
	 * @see #CCD_Exposure_Get_Exposure_Length
	 */
	public int CCDExposureGetExposureLength()
	{
		return CCD_Exposure_Get_Exposure_Length();
	}

	/**
	 * Method to get number of milliseconds since the EPOCH to the exposure start time.
	 * @return A long, in milliseconds.
	 * @see #CCD_Exposure_Get_Exposure_Start_Time
	 */
	public long CCDExposureGetExposureStartTime()
	{
		return CCD_Exposure_Get_Exposure_Start_Time();
	}

	/**
	 * Method to set how many seconds before the exposure
	 * is due to start we send the CLR command to the controller.
	 * @param time The time in seconds. This should be greater than the time the CLR command takes to
	 * 	clock all accumulated charge off the CCD.
	 * @see #CCD_Exposure_Set_Start_Exposure_Clear_Time
	 */
	public void CCDExposureSetStartExposureClearTime(int time)
	{
		CCD_Exposure_Set_Start_Exposure_Clear_Time(time);
	}

	/**
	 * Method to set the amount of time, in milliseconds, 
	 * before the desired start of exposure that we should send the SEX command, to allow for transmission delay.
	 * @param time The time, in milliseconds.
	 * @see #CCD_Exposure_Set_Start_Exposure_Offset_Time
	 */
	public void CCDExposureSetStartExposureOffsetTime(int time)
	{
		CCD_Exposure_Set_Start_Exposure_Offset_Time(time);
	}

	/**
	 * Method to set the amount of time, in milleseconds, 
	 * remaining for an exposure when we change status to PRE_READOUT, to stop RDM/TDL/WRMs affecting the readout.
	 * @param time The time, in milliseconds. Note, because the exposure time is read every second, it is best
	 * 	not have have this constant an exact multiple of 1000.
	 * @see #CCD_Exposure_Set_Readout_Remaining_Time
	 */
	public void CCDExposureSetReadoutRemainingTime(int time)
	{
		CCD_Exposure_Set_Readout_Remaining_Time(time);
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Exposure_Get_Error_Number
	 */
	public int CCDExposureGetErrorNumber()
	{
		return CCD_Exposure_Get_Error_Number();
	}

// ccd_global.h
	/**
	 * Routine that sets up all the parts of CCDLibrary  at the start of it's use. This routine should be
	 * called once at the start of each program. 
	 */
	public void CCDInitialise()
	{
		CCD_Global_Initialise();
	}

	/**
	 * Error routine. Should be called whenever another library routine has failed. 
	 * Prints out to stderr any error messages outstanding in any of the modules that
	 * make up libccd.
	 * <b>Note</b> you cannot call both CCDError and CCDErrorString to print the error string and 
	 * get a string copy of it, only one of the error routines can be called after libccd has generated an error.
	 * A second call to one of these methods will generate a libccd 'Error not found' error!.
	 * @see #CCD_Global_Error
	 */
	public void CCDError()
	{
		CCD_Global_Error();
	}

	/**
	 * Error routine. Should be called whenever another library routine has failed. 
	 * Returns in a string any error messages outstanding in any of the modules that
	 * make up libccd.
	 * <b>Note</b> you cannot call both CCDError and CCDErrorString to print the error string and 
	 * get a string copy of it, only one of the error routines can be called after libccd has generated an error.
	 * A second call to one of these methods will generate a libccd 'Error not found' error!.
	 * @return Returns the error string generated by libccd.
	 * @see #CCD_Global_Error_String
	 */
	public String CCDErrorString()
	{
		return CCD_Global_Error_String();
	}

// ccd_multrun.h
	/**
	 * Routine to perform a Multrun.
	 * @param open_shutter Determines whether the shutter should be opened to do the exposure. The shutter might
	 * 	be left closed to perform calibration images etc.
	 * @param startTime The start time, in milliseconds since the epoch (1st January 1970) to start the exposure.
	 * 	Passing the value -1 will start the exposure as soon as possible.
	 * @param exposureTime The number of milliseconds to expose the CCD.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if CCD_Multrun_Expose  
	 *            failed.
	 * @see #CCD_Multrun_Expose
	 */
	public void CCDMultrunExpose(boolean open_shutter,long startTime,int exposureTime,long exposures,List headers)
		throws CCDLibraryNativeException
	{
		CCD_Multrun_Expose(open_shutter,startTime,exposureTime,exposures, headers);
	}

	public void CCDMultflatExpose(boolean open_shutter,long startTime,int exposureTime,long exposures,List headers)
		throws CCDLibraryNativeException
	{
		CCD_Multflat_Expose(open_shutter,startTime,exposureTime,exposures, headers);
	}

	/**
	 * Returns whether an exposure is currently in progress. The library keeps track of whether a call to
	 * <a href="#CCDMultrunExpose">CCDMultrunExpose</a> is in progress, and whether it is exposing or reading out
	 * @return Returns the exposure status.
	 * @see #CCD_EXPOSURE_STATUS_NONE
	 * @see #CCD_EXPOSURE_STATUS_WAIT_START
	 * @see #CCD_EXPOSURE_STATUS_CLEAR
	 * @see #CCD_EXPOSURE_STATUS_EXPOSE
	 * @see #CCD_EXPOSURE_STATUS_PRE_READOUT
	 * @see #CCD_EXPOSURE_STATUS_READOUT
	 * @see #CCD_EXPOSURE_STATUS_POST_READOUT
	 * @see #CCDMultrunExpose
	 * @see #CCD_Multrun_Get_Exposure_Status
	 */
	public int CCDMultrunGetExposureStatus()
	{
		return CCD_Multrun_Get_Exposure_Status();
	}

	/**
	 * Returns how long since the last multrun exposure was started in milliseconds.
	 * @return The elapsed exposure time in milliseconds.
	 * @see #CCDMultrunExpose
	 * @see #CCD_Multrun_Get_Elapsed_Exposure_Time
	 */
	public int CCDMultrunGetElapsedExposureTime()
	{
		return CCD_Multrun_Get_Elapsed_Exposure_Time();
	}

// ccd_setup.h
	/**
	 * This routine sets up the Andor CCD Controller. 
	 * Array dimension information also needs to be setup before the controller can take exposures.
	 * This routine can be aborted with CCDSetupAbort.
	 * @param target_temperature Specifies the target temperature the CCD is meant to run at. 
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the setup failed.
	 * @see #CCDSetupAbort
	 * @see #CCD_Setup_Startup
	 */
	public void CCDSetupStartup(double target_temperature) throws CCDLibraryNativeException
	{
		CCD_Setup_Startup(target_temperature);
	}

	/**
	 * Routine to shut down the CCD Controller board.
	 * It then just remains to close the connection to the device driver.
	 * This routine can be aborted with CCDSetupAbort.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the shutdown failed.
	 * @see #CCDSetupStartup
	 * @see #CCDSetupAbort
	 * @see #CCD_Setup_Shutdown
	 */
	public void CCDSetupShutdown() throws CCDLibraryNativeException
	{
		CCD_Setup_Shutdown();
	}

	/**
	 * Routine to setup dimension information in the controller. This needs to be setup before an exposure
	 * can take place. This routine must be called <b>after</b> the CCDSetupStartup method.
	 * This routine can be aborted with CCDSetupAbort.
	 * @param ncols The number of columns in the image.
	 * @param nrows The number of rows in the image.
	 * @param nsbin The amount of binning applied to pixels in columns.This parameter will change internally
	 *	ncols.
	 * @param npbin The amount of binning applied to pixels in rows.This parameter will change internally
	 *	nrows.
	 * @param windowFlags Flags describing which windows are in use. A bit-field combination of:
	 * 	<a href="#CCD_SETUP_WINDOW_ONE">CCD_SETUP_WINDOW_ONE</a>,
	 * 	<a href="#CCD_SETUP_WINDOW_TWO">CCD_SETUP_WINDOW_TWO</a>,
	 * 	<a href="#CCD_SETUP_WINDOW_THREE">CCD_SETUP_WINDOW_THREE</a> and
	 * 	<a href="#CCD_SETUP_WINDOW_FOUR">CCD_SETUP_WINDOW_FOUR</a>.
	 * @param windowList A list of CCDLibrarySetupWindow objects describing the window dimensions.
	 * 	This list should have <b>four</b> items in it.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the setup failed.
	 * @see #CCDSetupAbort
	 * @see #CCD_Setup_Dimensions
	 */
	public void CCDSetupDimensions(int ncols,int nrows,int nsbin,int npbin,
				int windowFlags,CCDLibrarySetupWindow windowList[]) throws CCDLibraryNativeException
	{
		CCD_Setup_Dimensions(ncols,nrows,nsbin,npbin,windowFlags,windowList);
	}

	/**
	 * Routine to abort a setup that is underway.
	 * @see #CCDSetupStartup
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Abort
	 */
	public void CCDSetupAbort()
	{
		CCD_Setup_Abort();
	}

	/**
	 * Routine to get the number of columns on the CCD chip last passed into CCDSetupDimensions. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns an integer representing the number of columns.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_NCols
	 */
	public int CCDSetupGetNCols()
	{
		return CCD_Setup_Get_NCols();
	}

	/**
	 * Routine to get the number of rows on the CCD chip last passed into CCDSetupDimensions. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns an integer representing the number of rows.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_NRows
	 */
	public int CCDSetupGetNRows()
	{
		return CCD_Setup_Get_NRows();
	}

	/**
	 * Routine to get the column binning last passed into CCDSetupDimensions. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns an integer representing the column binning.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_NSBin
	 */
	public int CCDSetupGetNSBin()
	{
		return CCD_Setup_Get_NSBin();
	}

	/**
	 * Routine to get the row binning last passed into CCDSetupDimensions. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns an integer representing the row binning.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_NPBin
	 */
	public int CCDSetupGetNPBin()
	{
		return CCD_Setup_Get_NPBin();
	}

	/**
	 * Returns the window flags passed into the last setup. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns the window flags, a bit-field containing which window data is active. This consists of:
	 * 	<a href="#CCD_SETUP_WINDOW_ONE">CCD_SETUP_WINDOW_ONE</a>,
	 * 	<a href="#CCD_SETUP_WINDOW_TWO">CCD_SETUP_WINDOW_TWO</a>,
	 * 	<a href="#CCD_SETUP_WINDOW_THREE">CCD_SETUP_WINDOW_THREE</a> and
	 * 	<a href="#CCD_SETUP_WINDOW_FOUR">CCD_SETUP_WINDOW_FOUR</a>.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_Window_Flags
	 */
	public int CCDSetupGetWindowFlags()
	{
		return CCD_Setup_Get_Window_Flags();
	}

	/**
	 * Returns a new instance of CCDLibrarySetupWindow describing the CCD camera window at index windowIndex.
	 * Note this window is only used if the corresponding bit in CCDSetupGetWindowFlags() is set.
	 * @param windowIndex Which window to get information for. This should be from zero to the 
	 * 	number of windows minus one. Four windows are supported by the hardware, hence the maximum value
	 * 	the index can take is three.
	 * @return A new instance of CCDLibrarySetupWindow with the window paramaters.
	 * @exception CCDLibraryNativeException Thrown if the windowIndex is out of range.
	 * @see #CCDSetupGetWindowFlags
	 * @see #CCD_Setup_Get_Window
	 */
	public CCDLibrarySetupWindow CCDSetupGetWindow(int windowIndex) throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_Window(windowIndex);
	}

	/**
	 * Routine to get the window width of the specified window. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * Note this value is different from the width passed in the window list to CCDSetupDimensions,
	 * as it includes any bias strips added to the sides.
	 * @param windowIndex The index of the window.
	 * @return Returns an integer representing the width of the window.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_Window_Width
	 */
	public int CCDSetupGetWindowWidth(int windowIndex)
	{
		return CCD_Setup_Get_Window_Width(windowIndex);
	}

	/**
	 * Routine to get the window height of the specified window. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @param windowIndex The index of the window.
	 * @return Returns an integer representing the height of the window.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_Window_Height
	 */
	public int CCDSetupGetWindowHeight(int windowIndex)
	{
		return CCD_Setup_Get_Window_Height(windowIndex);
	}

	/**
	 * Routine to return whether a setup operation has been sucessfully completed since the last controller
	 * reset.
	 * @return Returns true if a setup has been completed otherwise false.
	 * @see #CCDSetupStartup
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_Setup_Complete
	 */
	public boolean CCDSetupGetSetupComplete()
	{
		return (boolean)CCD_Setup_Get_Setup_Complete();
	}

	/**
	 * Routine to detect whether a setup operation is underway.
	 * @return Returns true is a setup is in progress otherwise false.
	 * @see #CCDSetupStartup
	 * @see #CCD_Setup_Get_Setup_In_Progress
	 */
	public boolean CCDSetupGetSetupInProgress()
	{
		return (boolean)CCD_Setup_Get_Setup_In_Progress();
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Setup_Get_Error_Number
	 */
	public int CCDSetupGetErrorNumber()
	{
		return CCD_Setup_Get_Error_Number();
	}

// ccd_temperature.h
	/**
	 * Routine to get the current CCD temperature.
	 * @param temperature A double wrapper in which the current temperature is returned, in degrees centigrade.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see CCDLibraryDouble
	 * @see #CCD_Temperature_Get
	 */
	public void CCDTemperatureGet(CCDLibraryDouble temperature) throws CCDLibraryNativeException
	{
		CCD_Temperature_Get(temperature);
	}

	/**
	 * Routine to set the temperature of the CCD.
	 * @param target_temperature The temperature in degrees centigrade required for the CCD.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Temperature_Set
	 */
	public void CCDTemperatureSet(double target_temperature) throws CCDLibraryNativeException
	{
		CCD_Temperature_Set(target_temperature);
	}

	/**
	 * Routine to get the Analogue to Digital count from the utility board temperature sensor, 
	 * a measure of the temperature of the utility board electronics.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Temperature_Get_Utility_Board_ADU
	 */
	public int CCDTemperatureGetUtilityBoardADU() throws CCDLibraryNativeException
	{
		return CCD_Temperature_Get_Utility_Board_ADU();
	}

	/**
	 * Routine to get the current CCD heater Analogue to Digital count, a measure of how much
	 * heat is being put into the dewar to control the temperature.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Temperature_Get_Heater_ADU
	 */
	public int CCDTemperatureGetHeaterADU() throws CCDLibraryNativeException
	{
		return CCD_Temperature_Get_Heater_ADU();
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Temperature_Get_Error_Number
	 */
	public int CCDTemperatureGetErrorNumber()
	{
		return CCD_Temperature_Get_Error_Number();
	}
}
 
//
// $Log: not supported by cvs2svn $
// Revision 1.2  2009/10/21 13:49:21  cjm
// Changed library loaded to rise_ccd.
//
// Revision 1.1  2009/10/15 10:23:09  cjm
// Initial revision
//
// Revision 0.45  2006/05/16 17:41:33  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.44  2004/08/02 16:27:44  cjm
// Added CCD_DSP_DEINTERLACE_FLIP.
//
// Revision 0.43  2004/05/16 14:18:51  cjm
// Deleted CCDExposureAbortReadout. Allexposure aborts now handled by CCDExposureAbort.
//
// Revision 0.42  2004/03/03 15:45:19  cjm
// Added comments amount units of temperature returned in CCDTemperatureGet.
//
// Revision 0.41  2003/12/08 15:07:41  cjm
// Fixed references to CCDDSPGetExposureStatus, changed to CCDExposureGetExposureStatus.
//
// Revision 0.40  2003/12/08 15:03:47  cjm
// EXPOSURE_STATUS_WAIT_START added.
//
// Revision 0.39  2003/06/06 16:50:40  cjm
// Added windowing implementation.
//
// Revision 0.38  2003/01/28 16:25:35  cjm
// New filter wheel code.
//
// Revision 0.37  2002/12/16 19:50:49  cjm
// Changed Abort prototypes, thay can now return exceptions.
// Changed some CCDDSP methods to CCDExposure, to match libccd.
//
// Revision 0.36  2002/12/03 17:47:42  cjm
// Added CCDSetupGetVacuumGaugeADU and CCDSetupGetVacuumGaugeMBar.
//
// Revision 0.35  2002/09/23 15:11:45  cjm
// Comment change.
//
// Revision 0.34  2002/09/19 13:59:04  cjm
// Added CCDTemperatureGetUtilityBoardADU.
//
// Revision 0.33  2002/09/19 11:21:50  cjm
// Added voltage monitoring methods in ccd_setup.h.
//
// Revision 0.32  2001/07/13 10:10:23  cjm
// Added CCDTemperatureGetHeaterADU.
//
// Revision 0.31  2001/04/05 16:49:36  cjm
// Added logging back from C to Logger.
// Added more native interfaces for getting error numbers.
//
// Revision 0.30  2001/03/01 12:28:08  cjm
// Added CCDFilterWheelSetDeBounceStepCount.
//
// Revision 0.29  2001/02/09 18:29:48  cjm
// Added CCDFilterWheelSetMsPerStep and changed
// CCDFilterWheelGetStatus.
//
// Revision 0.28  2000/12/21 12:10:40  cjm
// Added CCDDSPGetFilterWheelStatus and relevant statuss.
//
// Revision 0.27  2000/06/20 12:54:27  cjm
// CCDExposureExpose now has ne readout_ccd parameter.
//
// Revision 0.26  2000/06/13 17:29:17  cjm
// JNI changes realating to updating the library to work the same way as voodoo.
// Includes PCI load code, amplifier setting,DSP internal var setting,
// etc...
//
// Revision 0.25  2000/05/26 10:05:29  cjm
// Added CCD_SETUP_WINDOW_COUNT constant.
//
// Revision 0.24  2000/05/26 09:56:48  cjm
// Added CCDSetupGetWindow method.
//
// Revision 0.23  2000/05/25 08:54:48  cjm
// Added CCDSetupGetGain method.
//
// Revision 0.22  2000/03/09 16:35:12  cjm
// Added CCDDSPCommandReadExposureTime implementation.
//
// Revision 0.21  2000/03/08 10:25:59  cjm
// Added CCDLibraryNativeException to pause and resumr methods.
//
// Revision 0.20  2000/03/07 17:03:05  cjm
// Added pause and resume methods.
//
// Revision 0.19  2000/03/03 10:32:03  cjm
// Added CCDDSPAbort method.
//
// Revision 0.18  2000/03/02 17:17:09  cjm
// Added CCDSetupHardwareTest.
//
// Revision 0.17  2000/02/28 19:13:47  cjm
// Backup.
//
// Revision 0.16  2000/02/22 17:30:48  cjm
// Added CCD_DSP_EXPOSURE_STATUS_CLEAR status.
//
// Revision 0.15  2000/02/14 19:06:07  cjm
// Added Java methods for accessing more setup data.
//
// Revision 0.14  2000/02/02 13:54:49  cjm
// Setup filter wheel and windowing paramater passing added.
//
// Revision 0.13  2000/01/24 14:08:57  cjm
// Methods and constants updated for new PCI version of libccd.
//
// Revision 0.12  1999/09/20 14:40:08  cjm
// Changed due to libccd native routines throwing CCDLibraryNativeException when errors occur.
//
// Revision 0.11  1999/09/13 13:54:54  cjm
// Class is now public.
//
// Revision 0.10  1999/09/10 15:33:42  cjm
// Changed package to ngat.ccd.
//
// Revision 0.9  1999/09/09 10:13:07  cjm
// Added CCDExposureBias call to library.
//
// Revision 0.8  1999/09/08 10:52:40  cjm
// Trying to fix file permissions of these files.
//
// Revision 0.7  1999/07/09 12:17:09  dev
// JNI Routines to get the number of rows/columns the CCD was setup to
// use.
//
// Revision 0.6  1999/06/07 16:56:41  dev
// String to Number parse routines
//
// Revision 0.5  1999/05/28 09:54:18  dev
// "Name
//
// Revision 0.4  1999/03/25 14:02:01  dev
// Backup
//
// Revision 0.3  1999/03/08 12:20:40  dev
// Backup
//
// Revision 0.2  1999/02/23 11:08:00  dev
// backup/transfer to ltccd1.
//
// Revision 0.1  1999/01/21 15:44:00  dev
// initial revision
//
//
