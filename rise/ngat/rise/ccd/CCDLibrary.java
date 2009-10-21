/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of NGAT.

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
// $Header: /space/home/eng/cjm/cvs/rise/ngat/rise/ccd/CCDLibrary.java,v 1.2 2009-10-21 13:49:21 cjm Exp $
package ngat.rise.ccd;

import java.lang.*;
import java.util.List;
import java.util.Vector;
import ngat.util.logging.*;

/**
 * This class supports an interface to the SDSU CCD Controller library, for controlling CCDs.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class CCDLibrary
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: CCDLibrary.java,v 1.2 2009-10-21 13:49:21 cjm Exp $");
// ccd_dsp.h
	/* These constants should be the same as those in ccd_dsp.h */
	/**
	 * Set Gain parameter, to set gain to 1.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_DSP_GAIN_ONE = 			0x1;
	/**
	 * Set Gain parameter, to set gain to 2.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_DSP_GAIN_TWO = 			0x2;
	/**
	 * Set Gain parameter, to set gain to 4.75.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_DSP_GAIN_FOUR = 			0x5;
	/**
	 * Set Gain parameter, to set gain to 9.5.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_DSP_GAIN_NINE = 			0xa;

	/* These constants should be the same as those in ccd_dsp.h */
	/**
	 * Set Output Source parameter, to make the controller read out images from the left amplifier.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_AMPLIFIER_LEFT 	=		0x5f5f4c;
	/**
	 * Set Output Source parameter, to make the controller read out images from the right amplifier.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_AMPLIFIER_RIGHT	=		0x5f5f52;
	/**
	 * Set Output Source parameter, to make the controller read out images from both amplifiers.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_AMPLIFIER_BOTH 	=		0x5f4c52;

	/* These constants should be the same as those in ccd_dsp.h */
	/**
	 * De-interlace type. This setting does no deinterlacing, as the CCD was read out from a single readout.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_DEINTERLACE_SINGLE = 		0;
	/**
	 * De-interlace type. This setting flips the output image in X, if the CCD was readout from the
	 * "wrong" amplifier, i.e. to ensure east is to the left.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_DEINTERLACE_FLIP = 		1;
	/**
	 * De-interlace type. This setting deinterlaces split parallel readout.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_DEINTERLACE_SPLIT_PARALLEL = 	2;
	/**
	 * De-interlace type. This setting deinterlaces split serial readout.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_DEINTERLACE_SPLIT_SERIAL =  	3;
	/**
	 * De-interlace type. This setting deinterlaces split quad readout.
	 * @see #CCDSetupDimensions
	 */
	public final static int CCD_DSP_DEINTERLACE_SPLIT_QUAD = 	4;

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

// ccd_filter_wheel.h
	/* These constants should be the same as those in ccd_filter_wheel.h */
	/**
	 * Filter wheel status number, showing that no filter wheel operation is underway at the present moment.
	 * @see #CCDFilterWheelGetStatus
	 */
	public final static int CCD_FILTER_WHEEL_STATUS_NONE = 		0;
	/**
	 * Filter wheel status number, showing that a filter wheel move operation is 
	 * underway at the present moment.
	 * @see #CCDFilterWheelGetStatus
	 */
	public final static int CCD_FILTER_WHEEL_STATUS_MOVING =  	1;
	/**
	 * Filter wheel status number, showing that a filter wheel reset operation is 
	 * underway at the present moment.
	 * @see #CCDFilterWheelGetStatus
	 */
	public final static int CCD_FILTER_WHEEL_STATUS_RESETING =   	2;
	/**
	 * Filter wheel status number, showing that a filter wheel abort operation is 
	 * pending at the present moment.
	 * @see #CCDFilterWheelGetStatus
	 */
	public final static int CCD_FILTER_WHEEL_STATUS_ABORTED =  	3;

// ccd_interface.h
	/* These constants should be the same as those in ccd_interface.h */
	/**
	 * Interface device number, showing that commands will currently be sent nowhere.
	 * @see #CCDInitialise
	 */
	public final static int CCD_INTERFACE_DEVICE_NONE = 		0;
	/**
	 * Interface device number, showing that commands will currently be sent to the text interface 
	 * to be printed out.
	 * @see #CCDInitialise
	 */
	public final static int CCD_INTERFACE_DEVICE_TEXT =		1;
	/**
	 * Interface device number, showing that commands will currently be sent to the PCI interface.
	 * @see #CCDInitialise
	 */
	public final static int CCD_INTERFACE_DEVICE_PCI = 		2;

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

	/* These constants should be the same as those in ccd_setup.h */
	/**
	 * Setup Load Type passed to CCDSetupStartup as a load_type parameter. This makes CCDSetupStartup do
	 * nothing for the DSP code for the relevant board, as it assumes the DSP code was loaded from ROM
	 * at bootup.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_SETUP_LOAD_ROM = 			0;
	/**
	 * Setup Load Type passed to CCDSetupStartup as a load_type parameter, to load DSP application code from 
	 * EEPROM.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_SETUP_LOAD_APPLICATION = 		1;
	/**
	 * Setup flag passed to CCDSetupStartup as a load_type parameter, to load DSP application code from a file.
	 * @see #CCDSetupStartup
	 */
	public final static int CCD_SETUP_LOAD_FILENAME = 		2;

// ccd_text.h
	/* These constants should be the same as those in ccd_text.h */
	/**
	 * Level number passed to CCDTextSetPrintLevel, to print out commands only.
	 * @see #CCDTextSetPrintLevel
	 */
	public final static int CCD_TEXT_PRINT_LEVEL_COMMANDS =   	0;
	/**
	 * Level number passed to CCDTextSetPrintLevel, to print out commands replies as well.
	 * @see #CCDTextSetPrintLevel
	 */
	public final static int CCD_TEXT_PRINT_LEVEL_REPLIES = 	  	1;
	/**
	 * Level number passed to CCDTextSetPrintLevel, to print out parameter value information as well.
	 * @see #CCDTextSetPrintLevel
	 */
	public final static int CCD_TEXT_PRINT_LEVEL_VALUES = 	     	2;
	/**
	 * Level number passed to CCDTextSetPrintLevel, to print out everything.
	 * @see #CCDTextSetPrintLevel
	 */
	public final static int CCD_TEXT_PRINT_LEVEL_ALL = 		3;

// ccd_dsp.h
	/**
	 * Native wrapper to libccd routine that aborts DSP commands.
	 */
	private native void CCD_DSP_Abort();
	/**
	 * Native wrapper to libccd routine that pauses an exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_DSP_Command_PEX() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that resumes an exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_DSP_Command_REX() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that get the amount of time an exposure has been underway.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_DSP_Command_Read_Exposure_Time() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to return this module's error number.
	 */
	private native int CCD_DSP_Get_Error_Number();

// ccd_exposure.h
	/**
	 * Native wrapper to libccd routine that does an exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 */
  	private native void CCD_Multrun_Expose(boolean open_shutter,long startTime, 
		int exposureTime,long exposures, List headers) throws CCDLibraryNativeException;

  	private native void CCD_Multflat_Expose(boolean open_shutter,long startTime, 
		int exposureTime,long exposures, List headers) throws CCDLibraryNativeException;

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
	 * Native wrapper to libccd routine thats returns whether an exposure is currently in progress.
	 */
	private native int CCD_Exposure_Get_Exposure_Status();
	/**
	 * Native wrapper to libccd routine thats returns the length of the last exposure length set.
	 */
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

// ccd_filter_wheel.h
	/**
	 * Native wrapper to libccd routine that sets the number of positions in each filter wheel.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Filter_Wheel_Set_Position_Count(int position_count) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does a filter wheel reset.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Filter_Wheel_Reset(int wheel_number) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does a filter wheel move.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Filter_Wheel_Move(int wheel_number,int position) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that aborts a filter wheel move or reset.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Filter_Wheel_Abort() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to return this module's error number.
	 */
	private native int CCD_Filter_Wheel_Get_Error_Number();
	/**
	 * Native wrapper to libccd routine thats returns whether an filter wheel operation is currently in progress.
	 */
	private native int CCD_Filter_Wheel_Get_Status();
	/**
	 * Native wrapper to libccd routine that gets the filter wheel position.
	 */
	private native int CCD_Filter_Wheel_Get_Position(int wheel_number) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that sets amount of time out of a filter wheel we turn on detent
	 * checking (filter wheel move de-bouncing).
	 */
	private native void CCD_Filter_Wheel_Set_De_Bounce_Milliseconds(int ms) throws CCDLibraryNativeException;

// ccd_global.h
	/**
	 * Native wrapper to libccd routine that sets up the CCD library for use.
	 */
	private native void CCD_Global_Initialise(int interface_device);
	/**
	 * Native wrapper to libccd routine that prints error values.
	 */
	private native void CCD_Global_Error();
	/**
	 * Native wrapper to libccd routine that gets error values into a string.
	 */
	private native String CCD_Global_Error_String();

// ccd_interface.h
	/**
	 * Native wrapper to libccd routine that opens the selected interface device.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Interface_Open() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that closes the selected interface device.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Interface_Close() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to return this module's error number.
	 */
	private native int CCD_Interface_Get_Error_Number();

// ccd_pci.h
	/**
	 * Native wrapper to return this module's error number.
	 */
	private native int CCD_PCI_Get_Error_Number();

// ccd_setup.h
	/**
	 * Native wrapper to libccd routine that does the CCD setup.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Startup(int pci_load_type, String pci_filename,
		int timing_load_type,int timing_application_number,String timing_filename,
		int utility_load_type,int utility_application_number,String utility_filename,
		double target_temperature,int gain,boolean gain_speed,boolean idle) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does the CCD shutdown.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Shutdown() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that does the CCD dimensions setup.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Dimensions(int ncols,int nrows,int nsbin,int npbin,
		int amplifier,int deinterlace_setting,int window_flags,
		CCDLibrarySetupWindow window_list[]) throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that performs a hardware test data link.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native void CCD_Setup_Hardware_Test(int test_count) throws CCDLibraryNativeException;
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
	 * Native wrapper to libccd routine that gets the setup amplifier type.
	 */
	private native int CCD_Setup_Get_Amplifier();
	/**
	 * Native wrapper to libccd routine that gets the setup de-interlace type.
	 */
	private native int CCD_Setup_Get_DeInterlace_Type();
	/**
	 * Native wrapper to libccd routine that gets the setup gain.
	 */
	private native int CCD_Setup_Get_Gain();
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
	 * Native wrapper to libccd routine that gets the current analogue high voltage ADU counts.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Setup_Get_High_Voltage_Analogue_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the current analogue low voltage ADU counts.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Setup_Get_Low_Voltage_Analogue_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the current analogue minus low voltage ADU counts.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the ADU counts from the dewar vacuum gauge.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native int CCD_Setup_Get_Vacuum_Gauge_ADU() throws CCDLibraryNativeException;
	/**
	 * Native wrapper to libccd routine that gets the dewar pressure from the vacuum gauge, in mbar.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 */
	private native double CCD_Setup_Get_Vacuum_Gauge_MBar() throws CCDLibraryNativeException;
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

// ccd_text.h
	/**
	 * Native wrapper to return this module's error number.
	 */
	private native int CCD_Text_Get_Error_Number();
	/**
	 * Native wrapper to libccd routine that sets the amount of output from the text interface.
	 */
	private native void CCD_Text_Set_Print_Level(int level);
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

// ccd_dsp.h
	/**
	 * Method to abort processing of a DSP command.
	 * @see #CCD_DSP_Abort
	 */
	public void CCDDSPAbort()
	{
		CCD_DSP_Abort();
	}

	/**
	 * Method to pause an exposure underway.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_DSP_Command_PEX
	 */
	public void CCDDSPCommandPEX() throws CCDLibraryNativeException
	{
		CCD_DSP_Command_PEX();
	}

	/**
	 * Method to resume a paused exposure.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_DSP_Command_REX
	 */
	public void CCDDSPCommandREX() throws CCDLibraryNativeException
	{
		CCD_DSP_Command_REX();
	}

	/**
	 * Method to return the amount of time an exposure has been underway (i.e. the length of time the
	 * shutter has been open for).
	 * @return The amount of time an exposure has been underway, in milliseconds. If an exposure
	 * 	is not underway (the shutter is not open) zero is returned.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_DSP_Command_Read_Exposure_Time
	 */
	public int CCDDSPCommandReadExposureTime() throws CCDLibraryNativeException
	{
		return CCD_DSP_Command_Read_Exposure_Time();
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_DSP_Get_Error_Number
	 */
	public int CCDDSPGetErrorNumber()
	{
		return CCD_DSP_Get_Error_Number();
	}

	/**
	 * Routine to parse a gain string and return a gain number suitable for input into
	 * <a href="#CCDSetupStartup">CCDSetupStartup</a>, or a DSP Set Gain (SGN) command. 
	 * @param s The string to parse.
	 * @return The gain number, one of:
	 * 	<ul>
	 * 	<li><a href="#CCD_DSP_GAIN_ONE">CCD_DSP_GAIN_ONE</a>
	 * 	<li><a href="#CCD_DSP_GAIN_TWO">CCD_DSP_GAIN_TWO</a>
	 * 	<li><a href="#CCD_DSP_GAIN_FOUR">CCD_DSP_GAIN_FOUR</a>
	 * 	<li><a href="#CCD_DSP_GAIN_NINE">CCD_DSP_GAIN_NINE</a>
	 * 	</ul>.
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDDSPGainFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_DSP_GAIN_ONE"))
			return CCD_DSP_GAIN_ONE;
		if(s.equals("CCD_DSP_GAIN_TWO"))
			return CCD_DSP_GAIN_TWO;
		if(s.equals("CCD_DSP_GAIN_FOUR"))
			return CCD_DSP_GAIN_FOUR;
		if(s.equals("CCD_DSP_GAIN_NINE"))
			return CCD_DSP_GAIN_NINE;
		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDDSPGainFromString",s);
	}

	/**
	 * Routine to parse an amplifier string and return a amplifier number suitable for input into
	 * <a href="#CCDSetupDimensions">CCDSetupDimensions</a>, or a DSP Set Output Source (SOS) command. 
	 * @param s The string to parse.
	 * @return The amplifier number, one of:
	 * 	<ul>
	 * 	<li><a href="#CCD_DSP_AMPLIFIER_LEFT">CCD_DSP_AMPLIFIER_LEFT</a>
	 * 	<li><a href="#CCD_DSP_AMPLIFIER_RIGHT">CCD_DSP_AMPLIFIER_RIGHT</a>
	 * 	<li><a href="#CCD_DSP_AMPLIFIER_BOTH">CCD_DSP_AMPLIFIER_BOTH</a>
	 * 	</ul>.
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDDSPAmplifierFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_DSP_AMPLIFIER_LEFT"))
			return CCD_DSP_AMPLIFIER_LEFT;
		if(s.equals("CCD_DSP_AMPLIFIER_RIGHT"))
			return CCD_DSP_AMPLIFIER_RIGHT;
		if(s.equals("CCD_DSP_AMPLIFIER_BOTH"))
			return CCD_DSP_AMPLIFIER_BOTH;
		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDDSPAmplifierFromString",s);
	}

	/**
	 * Routine to parse a string containing a representation of a valid deinterlace type and to return
	 * the numeric value of that type, suitable for passing into 
	 * <a href="#CCDSetupDimensions">CCDSetupDimensions</a> and other methods.
	 * @param s The string to parse.
	 * @return The deinterlace type number, one of:
	 * 	<ul>
	 * 	<li><a href="#CCD_DSP_DEINTERLACE_SINGLE">CCD_DSP_DEINTERLACE_SINGLE</a>
	 * 	<li><a href="#CCD_DSP_DEINTERLACE_FLIP">CCD_DSP_DEINTERLACE_FLIP</a>
	 * 	<li><a href="#CCD_DSP_DEINTERLACE_SPLIT_PARALLEL">CCD_DSP_DEINTERLACE_SPLIT_PARALLEL</a>
	 * 	<li><a href="#CCD_DSP_DEINTERLACE_SPLIT_SERIAL">CCD_DSP_DEINTERLACE_SPLIT_SERIAL</a>
	 * 	<li><a href="#CCD_DSP_DEINTERLACE_SPLIT_QUAD">CCD_DSP_DEINTERLACE_SPLIT_QUAD</a>
	 * 	</ul>.
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDDSPDeinterlaceFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_DSP_DEINTERLACE_SINGLE"))
			return CCD_DSP_DEINTERLACE_SINGLE;
		if(s.equals("CCD_DSP_DEINTERLACE_FLIP"))
			return CCD_DSP_DEINTERLACE_FLIP;
		if(s.equals("CCD_DSP_DEINTERLACE_SPLIT_PARALLEL"))
			return CCD_DSP_DEINTERLACE_SPLIT_PARALLEL;
		if(s.equals("CCD_DSP_DEINTERLACE_SPLIT_SERIAL"))
			return CCD_DSP_DEINTERLACE_SPLIT_SERIAL;
		if(s.equals("CCD_DSP_DEINTERLACE_SPLIT_QUAD"))
			return CCD_DSP_DEINTERLACE_SPLIT_QUAD;
		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDDSPDeinterlaceFromString",s);
	}

// ccd_exposure.h
	 /**
	 * Routine to perform a Multrun.
	 * @param open_shutter Determines whether the shutter should be opened to do the exposure. The shutter might
	 * 	be left closed to perform calibration images etc.
	 * @param readout_ccd Determines whether the CCD should be read out at the end of the exposure.
	 * @param startTime The start time, in milliseconds since the epoch (1st January 1970) to start the exposure.
	 * 	Passing the value -1 will start the exposure as soon as possible.
	 * @param exposureTime The number of milliseconds to expose the CCD.
	 * @param filename The filename to save the exposure into. This assumes the CCD is not configured to
	 *                 be windowed.
	 * @exception CCDLibraryNativeException This routine throws a CCDLibraryNativeException if 
	 * CCD_Exposure_Expose  failed.
	 * @see #CCD_Exposure_Expose
	 */
	public void CCDMultrunExpose(boolean open_shutter,long startTime,int exposureTime,long exposures, List headers) 
		throws CCDLibraryNativeException
	{

		CCD_Multrun_Expose(open_shutter,startTime,exposureTime,exposures, headers);
	}

	
	public void CCDMultflatExpose(boolean open_shutter,long startTime,int exposureTime,long exposures, List headers) 
		throws CCDLibraryNativeException
	{

		CCD_Multflat_Expose(open_shutter,startTime,exposureTime,exposures, headers);
	}

	/**
	 * Routine to perform an exposure.
	 * @param open_shutter Determines whether the shutter should be opened to do the exposure. The shutter might
	 * 	be left closed to perform calibration images etc.
	 * @param readout_ccd Determines whether the CCD should be read out at the end of the exposure.
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
	 * @param readout_ccd Determines whether the CCD should be read out at the end of the exposure.
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

// ccd_filter_wheel.h
	/**
	 * Method to setup the number of positions in each filter wheel.
	 * @param positionCount The number of positions in each filter wheel.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Filter_Wheel_Set_Position_Count
	 */
	public void CCDFilterWheelSetPositionCount(int positionCount) throws CCDLibraryNativeException
	{
		CCD_Filter_Wheel_Set_Position_Count(positionCount);
	}


	/**
	 * Method to reset a filter wheel to it's home position.
	 * This routine can be aborted with CCDFilterWheelAbort.
	 * @param wheel Which wheel to move. An integer, either zero or one.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCDFilterWheelAbort
	 * @see #CCD_Filter_Wheel_Reset
	 */
	public void CCDFilterWheelReset(int wheel) throws CCDLibraryNativeException
	{
		CCD_Filter_Wheel_Reset(wheel);
	}

	/**
	 * Method to move a filter wheel to the required position.
	 * This routine can be aborted with CCDFilterWheelAbort.
	 * @param wheel Which wheel to move. An integer, either zero or one.
	 * @param position The absolute position to move the specified wheel to.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCDFilterWheelAbort
	 * @see #CCD_Filter_Wheel_Move
	 */
	public void CCDFilterWheelMove(int wheel,int position) throws CCDLibraryNativeException
	{
		CCD_Filter_Wheel_Move(wheel,position);
	}

	/**
	 * Method to abort a filter wheel move or reset operation.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Filter_Wheel_Abort
	 */
	public void CCDFilterWheelAbort() throws CCDLibraryNativeException
	{
		CCD_Filter_Wheel_Abort();
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Filter_Wheel_Get_Error_Number
	 */
	public int CCDFilterWheelGetErrorNumber()
	{
		return CCD_Filter_Wheel_Get_Error_Number();
	}

	/**
	 * Returns the current status of the filter wheel control. The library keeps track of whether 
	 * a filter wheel is being moved,reset or aborted.
	 * @return Returns the filter wheel status, one of 
	 * 	CCD_FILTER_WHEEL_STATUS_NONE,CCD_FILTER_WHEEL_STATUS_MOVING,
	 * 	CCD_FILTER_WHEEL_STATUS_RESETING or CCD_FILTER_WHEEL_STATUS_ABORTED.
	 * @see #CCD_FILTER_WHEEL_STATUS_NONE
	 * @see #CCD_FILTER_WHEEL_STATUS_MOVING
	 * @see #CCD_FILTER_WHEEL_STATUS_RESETING
	 * @see #CCD_FILTER_WHEEL_STATUS_ABORTED
	 * @see #CCDFilterWheelMove
	 * @see #CCDFilterWheelReset
	 * @see #CCDFilterWheelAbort
	 * @see #CCD_Filter_Wheel_Get_Status
	 */
	public int CCDFilterWheelGetStatus()
	{
		return CCD_Filter_Wheel_Get_Status();
	}

	/**
	 * Routine to get the last position for the specified filter wheel. This value
	 * is got from the stored data.
	 * @param wheelNumber Which filter wheel to get the position for. An integer, either zero or one.
	 * @return Returns an integer representing the position of the filterwheel. This is an integer between
	 * 	zero (inclusive) and the number of positions in the wheel (exclusive). It can also be -1, which
	 * 	means the position is not currently known, or the wheel is moving.
	 * @exception CCDLibraryNativeException Thrown if an error occurs getting the position
	 * 	(the wheelNumber is out of range).
	 * @see #CCD_Filter_Wheel_Get_Position
	 */
	public int CCDFilterWheelGetPosition(int wheelNumber) throws CCDLibraryNativeException
	{
		return CCD_Filter_Wheel_Get_Position(wheelNumber);
	}

	/**
	 * A method to set the how long after driving the wheel out of
	 * a detent position we start checking detent inputs, during a filter wheel move. This 'de-bounces'
	 * the detent inputs when driving the filter wheel out of a detent.
	 * @param ms The time, in milliseconds, before we start detent checking. This must be less than
	 * 	about 1.3 seconds (1300) for 7 position filter wheels.
	 * @exception CCDLibraryNativeException Thrown if an error occurs (the parameter is out of range,
	 * 	or we could not write to the controller).
	 * @see #CCD_Filter_Wheel_Set_De_Bounce_Milliseconds
	 */
	public void CCDFilterWheelSetDeBounceMs(int ms) throws CCDLibraryNativeException
	{
		CCD_Filter_Wheel_Set_De_Bounce_Milliseconds(ms);
	}


// ccd_global.h
	/**
	 * Routine that sets up all the parts of CCDLibrary  at the start of it's use. This routine should be
	 * called once at the start of each program. 
	 * @param interface_device The interface device to use to communicate with the SDSU CCD Controller.
	 * 	One of:
	 * <a href="#CCD_INTERFACE_DEVICE_NONE">CCD_INTERFACE_DEVICE_NONE</a>,
	 * <a href="#CCD_INTERFACE_DEVICE_TEXT">CCD_INTERFACE_DEVICE_TEXT</a>,
	 * <a href="#CCD_INTERFACE_DEVICE_PCI">CCD_INTERFACE_DEVICE_PCI</a>.
	 * @see #CCD_Global_Initialise
	 */
	public void CCDInitialise(int interface_device)
	{
		CCD_Global_Initialise(interface_device);
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

// ccd_interface.h
	/**
	 * Routine to open the interface selected with <a href="#CCDInitialise">CCDInitialise</a> 
	 * or CCD_Interface_Set_Device 
	 * (a libccd routine, not implemented in CCDLibrary at the moment). 
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the device could
	 * 	not be opened.
	 * @see #CCDInitialise
	 * @see #CCDInterfaceClose
	 * @see #CCD_Interface_Open
	 */
	public void CCDInterfaceOpen() throws CCDLibraryNativeException
	{
		CCD_Interface_Open();
	}

	/**
	 * Routine to close the interface selected with <a href="#CCDInitialise">CCDInitialise</a> 
	 * or CCD_Interface_Set_Device
	 * (a libccd routine, not implemented in CCDLibrary at the moment) and opened with 
	 * <a href="#CCDInterfaceOpen">CCDInterfaceOpen</a>.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the device could
	 * 	not be closed.
	 * @see #CCDInitialise
	 * @see #CCDInterfaceOpen
	 * @see #CCD_Interface_Close
	 */
	public void CCDInterfaceClose() throws CCDLibraryNativeException
	{
		CCD_Interface_Close();
	}

	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Interface_Get_Error_Number
	 */
	public int CCDInterfaceGetErrorNumber()
	{
		return CCD_Interface_Get_Error_Number();
	}

	/**
	 * Routine to get an interface device number from a string representation of the device. Used for
	 * getting a device number from a string in a properties file.
	 * @param s A string representing a device number, one of:
	 * 	<ul>
	 * 	<li>CCD_INTERFACE_DEVICE_NONE,
	 * 	<li>CCD_INTERFACE_DEVICE_TEXT,
	 * 	<li>CCD_INTERFACE_DEVICE_PCI.
	 * 	</ul>.
	 * @return An interface device number, one of:
	 * 	<ul>
	 * 	<li><a href="#CCD_INTERFACE_DEVICE_NONE">CCD_INTERFACE_DEVICE_NONE</a>,
	 * 	<li><a href="#CCD_INTERFACE_DEVICE_TEXT">CCD_INTERFACE_DEVICE_TEXT</a>,
	 * 	<li><a href="#CCD_INTERFACE_DEVICE_PCI">CCD_INTERFACE_DEVICE_PCI</a>.
	 * 	</ul>. 
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDInterfaceDeviceFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_INTERFACE_DEVICE_NONE"))
			return CCD_INTERFACE_DEVICE_NONE;
		if(s.equals("CCD_INTERFACE_DEVICE_TEXT"))
			return CCD_INTERFACE_DEVICE_TEXT;
		if(s.equals("CCD_INTERFACE_DEVICE_PCI"))
			return CCD_INTERFACE_DEVICE_PCI;
		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDInterfaceDeviceFromString",s);
	}

// ccd_pci.h
	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_PCI_Get_Error_Number
	 */
	public int CCDPCIGetErrorNumber()
	{
		return CCD_PCI_Get_Error_Number();
	}

// ccd_setup.h
	/**
	 * This routine sets up the SDSU CCD Controller. This routine does the following:
	 * <ul>
	 * <li>Resets setup completion flags.</li>
	 * <li>Resets the SDSU CCD Controller.</li>
	 * <li>Does a hardware test on the data links to each board in the controller.</li>
	 * <li>Loads a PCI interface board DSP program from ROM/file.</li>
	 * <li>Loads a timing board DSP program from ROM/application/file.</li>
	 * <li>Loads a utility board DSP program from ROM/application/file.</li>
	 * <li>Switches the boards analogue power on.</li>
	 * <li>Sets the arrays target temperature.</li>
	 * <li>Setup the array's gain and readout speed.</li>
	 * <li>Setup the readout clocks to idle(or not!).</li>
	 * </ul>
	 * Array dimension information also needs to be setup before the controller can take exposures.
	 * This routine can be aborted with CCDSetupAbort.
	 * @param pci_load_type Where to load the PCI DSP program code from. Acceptable values are
	 * 	<a href="#CCD_SETUP_LOAD_ROM">CCD_SETUP_LOAD_ROM</a>,
	 * 	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> and
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a>.
	 * @param pci_filename If pci_load_type is
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a> this specifies which file to load from.
	 * @param timing_load_type Where to load the Timing application DSP code from. Acceptable values are
	 * 	<a href="#CCD_SETUP_LOAD_ROM">CCD_SETUP_LOAD_ROM</a>,
	 * 	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> and
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a>.
	 * @param timing_application_number If timing_load_type is
	 *	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> this specifies which 
	 * 	application to load.
	 * @param timing_filename If timing_load_type is
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a> this specifies which file to load from.
	 * @param utility_load_type Where to load the Utility application DSP code from. Acceptable values are
	 * 	<a href="#CCD_SETUP_LOAD_ROM">CCD_SETUP_LOAD_ROM</a>,
	 * 	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> and
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a>.
	 * @param utility_application_number If utility_load_type is
	 *	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> this specifies which 
	 * 	application to load.
	 * @param utility_filename If utility_load_type is
	 *	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a> this specifies which file to load from.
	 * @param target_temperature Specifies the target temperature the CCD is meant to run at. 
	 * @param gain Specifies the gain to use for the CCD video processors. Acceptable values are:
	 * 	<a href="#CCD_DSP_GAIN_ONE">CCD_DSP_GAIN_ONE</a>, 
	 * 	<a href="#CCD_DSP_GAIN_TWO">CCD_DSP_GAIN_TWO</a>,
	 * 	<a href="#CCD_DSP_GAIN_FOUR">CCD_DSP_GAIN_FOUR</a> and 
	 * 	<a href="#CCD_DSP_GAIN_NINE">CCD_DSP_GAIN_NINE</a>.
	 * @param gain_speed Set to true for fast integrator speed, false for slow integrator speed.
	 * @param idle If true puts CCD clocks in readout sequence, but not transferring any data, whenever a
	 * 	command is not executing.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the setup failed.
	 * @see #CCDSetupAbort
	 * @see #CCD_Setup_Startup
	 */
	public void CCDSetupStartup(int pci_load_type, String pci_filename,
		int timing_load_type,int timing_application_number,String timing_filename,
		int utility_load_type,int utility_application_number,String utility_filename,
		double target_temperature,int gain,boolean gain_speed,boolean idle) throws CCDLibraryNativeException
	{
		CCD_Setup_Startup(pci_load_type,pci_filename,
				timing_load_type,timing_application_number,timing_filename,
				utility_load_type,utility_application_number,utility_filename,
				target_temperature,gain,gain_speed,idle);
	}

	/**
	 * Routine to shut down the SDSU CCD Controller board. This consists of:
	 * <ul>
	 * <li>Reseting the setup completion flags.
	 * <li>Performing a power off command to switch off analogue voltages.
	 * </ul>
	 * It then just remains to close the connection to the astro device driver.
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
	 * @param amplifier The amplifier to use when reading out CCD data. One of:
	 * 	<a href="#CCD_DSP_AMPLIFIER_LEFT">CCD_DSP_AMPLIFIER_LEFT</a>,
	 * 	<a href="#CCD_DSP_AMPLIFIER_RIGHT">CCD_DSP_AMPLIFIER_RIGHT</a> or
	 * 	<a href="#CCD_DSP_AMPLIFIER_BOTH">CCD_DSP_AMPLIFIER_BOTH</a>.
	 * @param deinterlaceSetting The algorithm to use for deinterlacing the resulting data. The data needs to be
	 * 	deinterlaced if the CCD is read out from multiple readouts.One of:
	 * 	<a href="#CCD_DSP_DEINTERLACE_SINGLE">CCD_DSP_DEINTERLACE_SINGLE</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_FLIP">CCD_DSP_DEINTERLACE_FLIP</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_SPLIT_PARALLEL">CCD_DSP_DEINTERLACE_SPLIT_PARALLEL</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_SPLIT_SERIAL">CCD_DSP_DEINTERLACE_SPLIT_SERIAL</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_SPLIT_QUAD">CCD_DSP_DEINTERLACE_SPLIT_QUAD</a>.
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
		int amplifier,int deinterlaceSetting,
		int windowFlags,CCDLibrarySetupWindow windowList[]) throws CCDLibraryNativeException
	{
		CCD_Setup_Dimensions(ncols,nrows,nsbin,npbin,amplifier,deinterlaceSetting,windowFlags,windowList);
	}

	/**
	 * Method to perform a hardware data link test, to ensure we can communicate with each board in the
	 * controller.
	 * This routine can be aborted with CCDSetupAbort.
	 * @param testCount The number of times to test each board.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if the test failed.
	 * @see #CCDSetupAbort
	 * @see #CCD_Setup_Hardware_Test
	 */
	public void CCDSetupHardwareTest(int testCount) throws CCDLibraryNativeException
	{
		CCD_Setup_Hardware_Test(testCount);
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
	 * Returns which amplifier has been setup. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns the amplifier, one of:
	 * 	CCD_DSP_AMPLIFIER_LEFT,CCD_DSP_AMPLIFIER_RIGHT,CCD_DSP_AMPLIFIER_BOTH.
	 * @see #CCD_DSP_AMPLIFIER_LEFT
	 * @see #CCD_DSP_AMPLIFIER_RIGHT
	 * @see #CCD_DSP_AMPLIFIER_BOTH
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_Amplifier
	 */
	public int CCDSetupGetAmplifier()
	{
		return CCD_Setup_Get_Amplifier();
	}

	/**
	 * Returns which de-interlace type has been setup. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns the deinterlace type, one of:
	 * 	<a href="#CCD_DSP_DEINTERLACE_SINGLE">CCD_DSP_DEINTERLACE_SINGLE</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_FLIP">CCD_DSP_DEINTERLACE_FLIP</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_SPLIT_PARALLEL">CCD_DSP_DEINTERLACE_SPLIT_PARALLEL</a>,
	 * 	<a href="#CCD_DSP_DEINTERLACE_SPLIT_SERIAL">CCD_DSP_DEINTERLACE_SPLIT_SERIAL</a> and 
	 *	<a href="#CCD_DSP_DEINTERLACE_SPLIT_QUAD">CCD_DSP_DEINTERLACE_SPLIT_QUAD</a>.
	 * @see #CCDSetupDimensions
	 * @see #CCD_Setup_Get_DeInterlace_Type
	 */
	public int CCDSetupGetDeInterlaceType()
	{
		return CCD_Setup_Get_DeInterlace_Type();
	}

	/**
	 * Returns which gain has been setup. This value
	 * is got from the stored setup data, rather than querying the camera directly.
	 * @return Returns the gain, one of:
	 * 	<a href="#CCD_DSP_GAIN_ONE">CCD_DSP_GAIN_ONE</a>, 
	 * 	<a href="#CCD_DSP_GAIN_TWO">CCD_DSP_GAIN_TWO</a>,
	 * 	<a href="#CCD_DSP_GAIN_FOUR">CCD_DSP_GAIN_FOUR</a> and 
	 * 	<a href="#CCD_DSP_GAIN_NINE">CCD_DSP_GAIN_NINE</a>.
	 * @see #CCDSetupStartup
	 * @see #CCD_Setup_Get_Gain
	 */
	public int CCDSetupGetGain()
	{
		return CCD_Setup_Get_Gain();
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
	 * Routine to get the current High Voltage (+36v) Analogue to Digital count, a measure of 
	 * the actual voltage being supplied to the SDSU board.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Setup_Get_High_Voltage_Analogue_ADU
	 */
	public int CCDSetupGetHighVoltageAnalogueADU() throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_High_Voltage_Analogue_ADU();
	}

	/**
	 * Routine to get the current Low Voltage (+15v) Analogue to Digital count, a measure of 
	 * the actual voltage being supplied to the SDSU board.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Setup_Get_Low_Voltage_Analogue_ADU
	 */
	public int CCDSetupGetLowVoltageAnalogueADU() throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_Low_Voltage_Analogue_ADU();
	}

	/**
	 * Routine to get the current Negative Low Voltage (-15v) Analogue to Digital count, a measure of 
	 * the actual voltage being supplied to the SDSU board.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU
	 */
	public int CCDSetupGetMinusLowVoltageAnalogueADU() throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_Minus_Low_Voltage_Analogue_ADU();
	}

	/**
	 * Routine to get the current ADU count from the vacuum pressure gauge on the dewar.
	 * @return The ADU count from the dewar vacuum pressure gauge.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Setup_Get_Vacuum_Gauge_ADU
	 */
	public int CCDSetupGetVacuumGaugeADU() throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_Vacuum_Gauge_ADU();
	}

	/**
	 * Routine to get the current pressure in the dewar in mbar, using the vacuum pressure gauge.
	 * @return The dewar pressure, in mbar.
	 * @exception CCDLibraryNativeException This method throws a CCDLibraryNativeException if it failed.
	 * @see #CCD_Setup_Get_Vacuum_Gauge_ADU
	 */
	public double CCDSetupGetVacuumGaugeMBar() throws CCDLibraryNativeException
	{
		return CCD_Setup_Get_Vacuum_Gauge_MBar();
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

	/**
	 * Routine to parse a setup load type string and return a setup load type to pass into
	 * <a href="#CCDSetupStartup">CCDSetupStartup</a>. 
	 * @param s The string to parse.
	 * @return The load type, either <a href="#CCD_SETUP_LOAD_ROM">CCD_SETUP_LOAD_ROM</a>, 
	 * 	<a href="#CCD_SETUP_LOAD_APPLICATION">CCD_SETUP_LOAD_APPLICATION</a> or
	 * 	<a href="#CCD_SETUP_LOAD_FILENAME">CCD_SETUP_LOAD_FILENAME</a>.
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDSetupLoadTypeFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_SETUP_LOAD_ROM"))
			return CCD_SETUP_LOAD_ROM;
		if(s.equals("CCD_SETUP_LOAD_APPLICATION"))
			return CCD_SETUP_LOAD_APPLICATION;
		if(s.equals("CCD_SETUP_LOAD_FILENAME"))
			return CCD_SETUP_LOAD_FILENAME;
		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDSetupLoadTypeFromString",s);
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

// ccd_text.h
	/**
	 * Returns the current error number from this module of the library. A zero means there is no error.
	 * @return Returns an error number.
	 * @see #CCD_Text_Get_Error_Number
	 */
	public int CCDTextGetErrorNumber()
	{
		return CCD_Text_Get_Error_Number();
	}

	/**
	 * Routine thats set the amount of information displayed when the text interface device
	 * is enabled.
	 * @param level An integer describing how much information is displayed. Can be one of:
	 * <a href="#CCD_TEXT_PRINT_LEVEL_COMMANDS">CCD_TEXT_PRINT_LEVEL_COMMANDS</a>,
	 * <a href="#CCD_TEXT_PRINT_LEVEL_REPLIES">CCD_TEXT_PRINT_LEVEL_REPLIES</a>,
	 * <a href="#CCD_TEXT_PRINT_LEVEL_VALUES">CCD_TEXT_PRINT_LEVEL_VALUES</a> and
	 * <a href="#CCD_TEXT_PRINT_LEVEL_ALL">CCD_TEXT_PRINT_LEVEL_ALL</a>.
	 * @see #CCD_Text_Set_Print_Level
	 */
	public void CCDTextSetPrintLevel(int level)
	{
		CCD_Text_Set_Print_Level(level);
	}

	/**
	 * Routine to parse a string version of a text print level and to return
	 * the numeric value of that level, suitable for passing into 
	 * <a href="#CCDTextSetPrintLevel">CCDTextSetPrintLevel</a>.
	 * @param s The string to parse.
	 * @return The printlevel number, one of:
	 * 	<ul>
	 * 	<a href="#CCD_TEXT_PRINT_LEVEL_COMMANDS">CCD_TEXT_PRINT_LEVEL_COMMANDS</a>,
	 * 	<a href="#CCD_TEXT_PRINT_LEVEL_REPLIES">CCD_TEXT_PRINT_LEVEL_REPLIES</a>,
	 * 	<a href="#CCD_TEXT_PRINT_LEVEL_VALUES">CCD_TEXT_PRINT_LEVEL_VALUES</a> and
	 * 	<a href="#CCD_TEXT_PRINT_LEVEL_ALL">CCD_TEXT_PRINT_LEVEL_ALL</a>.
	 * 	</ul>.
	 * @exception CCDLibraryFormatException If the string was not an accepted value an exception is thrown.
	 */
	public int CCDTextPrintLevelFromString(String s) throws CCDLibraryFormatException
	{
		if(s.equals("CCD_TEXT_PRINT_LEVEL_COMMANDS"))
			return CCD_TEXT_PRINT_LEVEL_COMMANDS;
		if(s.equals("CCD_TEXT_PRINT_LEVEL_REPLIES"))
			return CCD_TEXT_PRINT_LEVEL_REPLIES;
		if(s.equals("CCD_TEXT_PRINT_LEVEL_VALUES"))
			return CCD_TEXT_PRINT_LEVEL_VALUES;
		if(s.equals("CCD_TEXT_PRINT_LEVEL_ALL"))
			return CCD_TEXT_PRINT_LEVEL_ALL;

		throw new CCDLibraryFormatException(this.getClass().getName(),"CCDTextPrintLevelFromString",s);
	}
}
 
//
// $Log: not supported by cvs2svn $
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
