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
// TWILIGHT_CALIBRATEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/TWILIGHT_CALIBRATEImplementation.java,v 1.4 2017-07-29 15:31:47 cjm Exp $
import java.io.*;
import java.lang.*;
import java.util.*;

import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.OFFSET_FOCUS;
import ngat.message.ISS_INST.OFFSET_FOCUS_DONE;
import ngat.message.ISS_INST.OFFSET_RA_DEC;
import ngat.message.ISS_INST.OFFSET_RA_DEC_DONE;
import ngat.message.ISS_INST.INST_TO_ISS_DONE;
import ngat.message.ISS_INST.TWILIGHT_CALIBRATE;
import ngat.message.ISS_INST.TWILIGHT_CALIBRATE_ACK;
import ngat.message.ISS_INST.TWILIGHT_CALIBRATE_DP_ACK;
import ngat.message.ISS_INST.TWILIGHT_CALIBRATE_DONE;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation of a TWILIGHT_CALIBRATE command sent to a server using the
 * Java Message System. It performs a series of SKYFLAT frames from a configurable list,
 * taking into account frames done in previous invocations of this command (it saves it's state).
 * The exposure length is dynamically adjusted as the sky gets darker or brighter. TWILIGHT_CALIBRATE commands
 * should be sent to the Ccs just after sunset and just before sunrise.
 * @author Chris Mottram
 * @version $Revision: 1.4 $
 */
public class TWILIGHT_CALIBRATEImplementation extends CALIBRATEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: TWILIGHT_CALIBRATEImplementation.java,v 1.4 2017-07-29 15:31:47 cjm Exp $");
	/**
	 * Initial part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_STRING = "ccs.twilight_calibrate.";
	/**
	 * Middle part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_CALIBRATION_STRING = "calibration.";
	/**
	 * Middle part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_SUNSET_STRING = "sunset.";
	/**
	 * Middle part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_SUNRISE_STRING = "sunrise.";
	/**
	 * Final part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_BIN_STRING = ".bin";
	/**
	 * Final part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_AMPLIFIER_STRING = ".window_amplifier";
	/**
	 * Final part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_FILTER_LOWER_STRING = ".filter.lower";
	/**
	 * Final part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_FILTER_UPPER_STRING = ".filter.upper";
	/**
	 * Final part of a key string, used to create a list of potential twilight calibrations to
	 * perform from a Java property file.
	 */
	protected final static String LIST_KEY_FREQUENCY_STRING = ".frequency";
	/**
	 * Middle part of a key string, used for saving and restoring the stored calibration state.
	 */
	protected final static String LIST_KEY_LAST_TIME_STRING = "last_time.";
	/**
	 * Middle part of a key string, used to load a list of telescope RA/DEC offsets from a Java property file.
	 */
	protected final static String LIST_KEY_OFFSET_STRING = "offset.";
	/**
	 * Last part of a key string, used to load a list of telescope RA/DEC offsets from a Java property file.
	 */
	protected final static String LIST_KEY_RA_STRING = ".ra";
	/**
	 * Last part of a key string, used to load a list of telescope RA/DEC offsets from a Java property file.
	 */
	protected final static String LIST_KEY_DEC_STRING = ".dec";
	/**
	 * Middle part of a key string, used to get comparative filter sensitivity.
	 */
	protected final static String LIST_KEY_FILTER_SENSITIVITY_STRING = "filter_sensitivity.";
	/**
	 * Constant used for time of night determination.
	 * @see #timeOfNight
	 */
	protected final static int TIME_OF_NIGHT_UNKNOWN = 0;
	/**
	 * Constant used for time of night determination.
	 * @see #timeOfNight
	 */
	protected final static int TIME_OF_NIGHT_SUNSET	= 1;
	/**
	 * Constant used for time of night determination.
	 * @see #timeOfNight
	 */
	protected final static int TIME_OF_NIGHT_SUNRISE = 2;
	/**
	 * A possible state of a frame taken by this command. 
	 * The frame did not have enough counts to be useful, i.e. the mean counts were less than minMeanCounts.
	 * @see #minMeanCounts
	 */
	protected final static int FRAME_STATE_UNDEREXPOSED 	= 0;
	/**
	 * A possible state of a frame taken by this command. 
	 * The mean counts for the frame were sensible, i.e. the mean counts were more than minMeanCounts and less
	 * than maxMeanCounts.
	 * @see #minMeanCounts
	 * @see #maxMeanCounts
	 */
	protected final static int FRAME_STATE_OK 		= 1;
	/**
	 * A possible state of a frame taken by this command. 
	 * The frame had too many counts to be useful, i.e. the mean counts were higher than maxMeanCounts.
	 * @see #maxMeanCounts
	 */
	protected final static int FRAME_STATE_OVEREXPOSED 	= 2;
	/**
	 * The number of possible frame states.
	 * @see #FRAME_STATE_UNDEREXPOSED
	 * @see #FRAME_STATE_OK
	 * @see #FRAME_STATE_OVEREXPOSED
	 */
	protected final static int FRAME_STATE_COUNT 	= 3;
	/**
	 * Description strings for the frame states, indexed by the frame state enumeration numbers.
	 * @see #FRAME_STATE_UNDEREXPOSED
	 * @see #FRAME_STATE_OK
	 * @see #FRAME_STATE_OVEREXPOSED
	 * @see #FRAME_STATE_COUNT
	 */
	protected final static String FRAME_STATE_NAME_LIST[] = {"underexposed","ok","overexposed"};
	/**
	 * The time, in milliseconds since the epoch, that the implementation of this command was started.
	 */
	private long implementationStartTime = 0L;
	/**
	 * The saved state of calibrations done over time by invocations of this command.
	 * @see TWILIGHT_CALIBRATESavedState
	 */
	private TWILIGHT_CALIBRATESavedState twilightCalibrateState = null;
	/**
	 * The filename holding the saved state data.
	 */
	private String stateFilename = null;
	/**
	 * The list of calibrations to select from.
	 * Each item in the list is an instance of TWILIGHT_CALIBRATECalibration.
	 * @see TWILIGHT_CALIBRATECalibration
	 */
	protected List calibrationList = null;
	/**
	 * The list of telescope offsets to do each calibration for.
	 * Each item in the list is an instance of TWILIGHT_CALIBRATEOffset.
	 * @see TWILIGHT_CALIBRATEOffset
	 */
	protected List offsetList = null;
	/**
	 * The frame overhead for a full frame, in milliseconds. This takes into account readout time,
	 * real time data reduction, communication overheads and the like.
	 */
	private int frameOverhead = 0;
	/**
	 * The minimum allowable exposure time for a frame, in milliseconds.
	 */
	private int minExposureLength = 0;
	/**
	 * The maximum allowable exposure time for a frame, in milliseconds.
	 */
	private int maxExposureLength = 0;
	/**
	 * The exposure time for the current frame, in milliseconds.
	 */
	private int exposureLength = 0;
	/**
	 * The exposure time for the last frame exposed, in milliseconds.
	 */
	private int lastExposureLength = 0;
	/**
	 * What time of night we are doing the calibration, is it sunset or sunrise.
	 * @see #TIME_OF_NIGHT_UNKNOWN
	 * @see #TIME_OF_NIGHT_SUNSET
	 * @see #TIME_OF_NIGHT_SUNRISE
	 */
	private int timeOfNight = TIME_OF_NIGHT_UNKNOWN;
	/**
	 * Filename used to save FITS frames to, until they are determined to contain valid data
	 * (the counts in them are within limits).
	 */
	private String temporaryFITSFilename = null;
	/**
	 * The minimum mean counts. A &quot;good&quot; frame will have a mean counts greater than this number.
	 */
	private int minMeanCounts = 0;
	/**
	 * The best mean counts. The &quot;ideal&quot; frame will have a mean counts of this number.
	 */
	private int bestMeanCounts = 0;
	/**
	 * The maximum mean counts. A &quot;good&quot; frame will have a mean counts less than this number.
	 */
	private int maxMeanCounts = 0;
	/**
	 * The amount to multiply an exposure length by based on the state of the last frame.
	 * The index's into the list are based on the last frame state.
	 * @see #FRAME_STATE_COUNT
	 * @see #FRAME_STATE_UNDEREXPOSED
	 * @see #FRAME_STATE_OK
	 * @see #FRAME_STATE_OVEREXPOSED
	 */
	private double multiplicationFactorList[] = new double[FRAME_STATE_COUNT];
	/**
	 * The last relative filter sensitivity used for calculating exposure lengths.
	 */
	private double lastFilterSensitivity = 1.0;
	/**
	 * The last bin factor used for calculating exposure lengths.
	 */
	private int lastBin = 1;
	/**
	 * Loop terminator for the calibration list loop.
	 */
	private boolean doneCalibration = false;
	/**
	 * Loop terminator for the offset list loop.
	 */
	private boolean doneOffset = false;
	/**
	 * The number of calibrations completed that have a frame state of good,
	 * for the currently executing calibration.
	 * A calibration is successfully completed if the calibrationFrameCount is
	 * equal to the offsetList size at the end of the offset list loop.
	 */
	private int calibrationFrameCount = 0;

	/**
	 * Constructor.
	 */
	public TWILIGHT_CALIBRATEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.TWILIGHT_CALIBRATE&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.TWILIGHT_CALIBRATE";
	}

	/**
	 * This method is the first to be called in this class. 
	 * <ul>
	 * <li>It calls the superclass's init method.
	 * </ul>
	 * @param command The command to be implemented.
	 */
	public void init(COMMAND command)
	{
		super.init(command);
	}

	/**
	 * This method gets the TWILIGHT_CALIBRATE command's acknowledge time. 
	 * This returns the server connection threads default acknowledge time plus the config property:
	 * ccs.server_connection.twilight_calibrate_acknowledge_time.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see #status
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;
		int time;

		acknowledge = new ACK(command.getId());
		time = status.getPropertyInteger("ccs.server_connection.twilight_calibrate_acknowledge_time");
		acknowledge.setTimeToComplete(time+serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the TWILIGHT_CALIBRATE command.
	 * <ul>
	 * <li>The implementation start time is saved.
	 * <li><b>setTimeOfNight</b> is called to set the time of night flag.
	 * <li><b>loadProperties</b> is called to get configuration data from the properties.
	 * <li><b>addSavedStateToCalibration</b> is called, which finds the correct last time for each
	 * 	calibration in the list and sets the relevant field.
	 * <li>The FITS headers are cleared, and a the MULTRUN number is incremented.
	 * <li>The fold mirror is moved to the correct location using <b>moveFold</b>.
	 * <li>For each calibration, we do the following:
	 *      <ul>
	 *      <li><b>doCalibration</b> is called.
	 *      </ul>
	 * <li>sendBasicAck is called, to stop the client timing out whilst creating the master flat.
	 * <li>The makeMasterFlat method is called, to create master flat fields from the data just taken.
	 * </ul>
	 * Note this method assumes the loading and initialisation before the main loop takes less than the
	 * default acknowledge time, as no ACK's are sent to the client until we are ready to do the first
	 * sequence of calibration frames.
	 * @param command The command to be implemented.
	 * @return An instance of TWILIGHT_CALIBRATE_DONE is returned, with it's fields indicating
	 * 	the result of the command implementation.
	 * @see #implementationStartTime
	 * @see #exposureLength
	 * @see #lastFilterSensitivity
	 * @see #setTimeOfNight
	 * @see #addSavedStateToCalibration
	 * @see FITSImplementation#moveFold
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#ccsFilename
	 * @see #doCalibration
	 * @see #frameOverhead
	 * @see CALIBRATEImplementation#makeMasterFlat
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		TWILIGHT_CALIBRATE twilightCalibrateCommand = (TWILIGHT_CALIBRATE)command;
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone = new TWILIGHT_CALIBRATE_DONE(command.getId());
		TWILIGHT_CALIBRATECalibration calibration = null;
		int calibrationListIndex = 0;
		String directoryString = null;
		int makeFlatAckTime;
	        Vector selectedHeaders = new Vector(); // Allocate an array for the headers IT

		twilightCalibrateDone.setMeanCounts(0.0f);
		twilightCalibrateDone.setPeakCounts(0.0f);
	// initialise
		implementationStartTime = System.currentTimeMillis();
		setTimeOfNight();
		if(loadProperties(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return twilightCalibrateDone;
	// initialise status/fits header info, in case any frames are produced.
		status.setExposureCount(-1);
		status.setExposureNumber(0);
	// increment multrun number if filename generation object
		ccsFilename.nextMultRunNumber();
		try
		{
			ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_SKY_FLAT);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":"+e.toString());
			twilightCalibrateDone.setFilename(null);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2320);
			twilightCalibrateDone.setErrorString(e.toString());
			twilightCalibrateDone.setSuccessful(false);
			return twilightCalibrateDone;
		}
	// match saved state to calibration list (put last time into calibration list)
	//	if(addSavedStateToCalibration(twilightCalibrateCommand,twilightCalibrateDone) == false)
	//		return twilightCalibrateDone;
	// move the fold mirror to the correct location
 		if(moveFold(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return twilightCalibrateDone;
	//	if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
	//		return twilightCalibrateDone;
	// initialise exposureLength
		if(timeOfNight == TIME_OF_NIGHT_SUNRISE)
			exposureLength = maxExposureLength/2;
		else if(timeOfNight == TIME_OF_NIGHT_SUNSET)
			exposureLength = minExposureLength;
		else // this should never happen
			exposureLength = minExposureLength;
		lastFilterSensitivity = 1.0;
		lastBin = 1;
		calibrationListIndex = 0;
		doneCalibration = false;
	/* This bit isn't needed for RISE  IT

	// main loop, do calibrations until we run out of time.
		while((doneCalibration == false) && (calibrationListIndex < calibrationList.size()))
		{
		// get calibration
			calibration = (TWILIGHT_CALIBRATECalibration)(calibrationList.get(calibrationListIndex));
		// do calibration
			if(doCalibration(twilightCalibrateCommand,twilightCalibrateDone,calibration) == false)
				return twilightCalibrateDone;
			calibrationListIndex++;
		}// end for on calibration list
	// send an ack before make master processing, so the client doesn't time out.
		makeFlatAckTime = status.getPropertyInteger("ccs.twilight_calibrate.acknowledge_time.make_flat");
		if(sendBasicAck(twilightCalibrateCommand,twilightCalibrateDone,makeFlatAckTime) == false)
			return twilightCalibrateDone;
	// Call pipeline to create master flat.
		directoryString = status.getProperty("ccs.file.fits.path");
		if(directoryString.endsWith(System.getProperty("file.separator")) == false)
			directoryString = directoryString.concat(System.getProperty("file.separator"));
		if(makeMasterFlat(twilightCalibrateCommand,twilightCalibrateDone,directoryString) == false)
			return twilightCalibrateDone; */
	// setup fits headers
			ccs.error(this.getClass().getName()+": Getting Fits Header info...");

			clearFitsHeaders();
			if(setFitsHeaders(twilightCalibrateCommand,twilightCalibrateDone,
				FitsHeaderDefaults.OBSTYPE_VALUE_SKY_FLAT,exposureLength) == false){
			 	ccs.error(this.getClass().getName()+": setFitsHeaders failed.");
				return twilightCalibrateDone;
			}
			if(getFitsHeadersFromISS(twilightCalibrateCommand,twilightCalibrateDone) == false){
			 	ccs.error(this.getClass().getName()+": getFitsHeadersFromISS failed.");
				return twilightCalibrateDone;
			}
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true){
			 	ccs.error(this.getClass().getName()+": testAbort failed.");
				return twilightCalibrateDone;
			}
			/* if(saveFitsHeaders(twilightCalibrateCommand,twilightCalibrateDone,
				temporaryFITSFilename) == false)
				return twilightCalibrateDone;
			} */
	
	// Don't forget the toString() or the JVM will crash!!
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("RA"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("DEC"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LATITUDE").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LONGITUD").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBSTYPE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("AIRMASS").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELFOCUS").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ORIGIN"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("INSTATUS"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CONFIGID").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELESCOP"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELMODE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LST"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CAT-RA"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CAT-DEC"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELSTAT"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("AUTOGUID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTMODE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTSKYPA").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WINDSPEE").toString());
			//selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("REFTEMP").toString());
			//selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("REFHUMID").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WMSTEMP").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WMSHUMID").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBJECT"));
			
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("INSTRUME"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CONFNAME"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("DETECTOR"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GAIN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("READNOIS").toString());
		
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TAGID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("USERID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PROGID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PROPID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GROUPID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBSID"));

			
			// New headers added at request of RJS (2008-11) - Test implementation in flats 2009-06-10
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("EXPTOTAL").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PRESCAN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POSTSCAN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTCENTX").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTCENTY").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POICENTX").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POICENTY").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("FILTERI1"));

			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CCDSCALE").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("RADECSYS"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("EQUINOX").toString());

			// Additional headers. Nulls send during daytime obs, so check for null!
			//if(ccsFitsHeader.getKeywordValue("GRPNUMOB") != null)
			//    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNUMOB").toString());

			if(ccsFitsHeader.getKeywordValue("GRPTIMNG") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPTIMNG"));
			}

			if(ccsFitsHeader.getKeywordValue("GRPNUMOB") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNUMOB").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPUID") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPUID").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPNOMEX") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNOMEX").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPMONP") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPMONP").toString());
			}

			if(ccsFitsHeader.getKeywordValue("FILTER1") == null){
				selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("FILTER1"));
			}

			if(ccsFitsHeader.getKeywordValue("ROTANGLE") == null){
				selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTANGLE").toString());
			}




	// Start the exposure
	// twilightCalibrateCommand.getTimeToComplete() is used as a 'Number of frames' for now!
			try {
			 ccs.error(this.getClass().getName()+" Running CCDMultflatExpose for " + 
				twilightCalibrateCommand.getTimeToComplete() + " ms");
			 libccd.CCDMultflatExpose(true,-1,exposureLength,twilightCalibrateCommand.getTimeToComplete(),selectedHeaders);
			}
 			catch (Exception e) {
			 String errorString = new String(twilightCalibrateCommand.getId()+
				":CCDMultflatExpose failed.");
			 ccs.error(this.getClass().getName()+":"+errorString,e);
			}
			

	// return done
		twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		twilightCalibrateDone.setErrorString("");
		twilightCalibrateDone.setSuccessful(true);
		return twilightCalibrateDone;
	}

	/**
	 * Method to set time of night flag.
	 * @see #TIME_OF_NIGHT_UNKNOWN
	 * @see #TIME_OF_NIGHT_SUNRISE
	 * @see #TIME_OF_NIGHT_SUNSET
	 * @see #timeOfNight
	 */
	protected void setTimeOfNight()
	{
		Calendar calendar = null;
		int hour;

		timeOfNight = TIME_OF_NIGHT_UNKNOWN;
	// get Instance initialises the calendar to the current time.
		calendar = Calendar.getInstance();
	// the hour returned using HOUR of DAY is between 0 and 23
		hour = calendar.get(Calendar.HOUR_OF_DAY);
		if(hour < 12)
			timeOfNight = TIME_OF_NIGHT_SUNRISE;
		else
			timeOfNight = TIME_OF_NIGHT_SUNSET;
	}

	/**
	 * Method to load twilight calibration configuration data from the Ccs Properties file.
	 * The following configuration properties are retrieved:
	 * <ul>
	 * <li>frame overhead
	 * <li>minimum exposure length
	 * <li>maximum exposure length
	 * <li>temporary FITS filename
	 * <li>saved state filename
	 * <li>minimum mean counts
	 * <li>maximum mean counts
	 * <li>frame state multiplication factors (dependant on the time of night).
	 * </ul>
	 * The following methods are then called to load more calibration data:
	 * <ul>
	 * <li><b>loadCalibrationList</b>
	 * <li><b>loadOffsetList</b>
	 * <li><b>loadState</b>
	 * </ul>
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if the method succeeds, false if an error occurs.
	 * 	If false is returned the error data in twilightCalibrateDone is filled in.
	 * @see #loadCalibrationList
	 * @see #loadState
	 * @see #loadOffsetList
	 * @see #frameOverhead
	 * @see #minExposureLength
	 * @see #maxExposureLength
	 * @see #temporaryFITSFilename
	 * @see #stateFilename
	 * @see #minMeanCounts
	 * @see #bestMeanCounts
	 * @see #maxMeanCounts
	 * @see #timeOfNight
	 * @see #LIST_KEY_SUNSET_STRING
	 * @see #LIST_KEY_SUNRISE_STRING
	 * @see #LIST_KEY_STRING
	 */
	protected boolean loadProperties(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		String timeOfNightString = null;
		String propertyName = null;

		if(timeOfNight == TIME_OF_NIGHT_SUNSET)
			timeOfNightString = LIST_KEY_SUNSET_STRING;
		else
			timeOfNightString = LIST_KEY_SUNRISE_STRING;
		try
		{
		// frame overhead
			propertyName = LIST_KEY_STRING+"frame_overhead";
			frameOverhead = status.getPropertyInteger(propertyName);
		// minimum exposure length
			propertyName = LIST_KEY_STRING+"min_exposure_time";
			minExposureLength = status.getPropertyInteger(propertyName);
		// maximum exposure length
			propertyName = LIST_KEY_STRING+"max_exposure_time";
			maxExposureLength = status.getPropertyInteger(propertyName);
		// temporary FITS filename
			propertyName = LIST_KEY_STRING+"file.tmp";
			temporaryFITSFilename = status.getProperty(propertyName);
		// saved state filename
			propertyName = LIST_KEY_STRING+"state_filename";
			stateFilename = status.getProperty(propertyName);
		// minimum mean counts
			propertyName = LIST_KEY_STRING+"mean_counts.min";
			minMeanCounts = status.getPropertyInteger(propertyName);
		// best mean counts
			propertyName = LIST_KEY_STRING+"mean_counts.best";
			bestMeanCounts = status.getPropertyInteger(propertyName);
		// maximum mean counts
			propertyName = LIST_KEY_STRING+"mean_counts.max";
			maxMeanCounts = status.getPropertyInteger(propertyName);
		// underexposed multiplication factor
		// diddly This factor is not used anymore?
			propertyName = new String(LIST_KEY_STRING+"exposure_time.multiplication_factor."+
					timeOfNightString+"underexposed");
			multiplicationFactorList[FRAME_STATE_UNDEREXPOSED] = status.getPropertyDouble(propertyName);
		// nominal multiplication factor
		// diddly This factor is not used anymore?
			propertyName = new String(LIST_KEY_STRING+"exposure_time.multiplication_factor."+
				timeOfNightString+"nominal");
			multiplicationFactorList[FRAME_STATE_OK] = status.getPropertyDouble(propertyName);
		// saturated multiplication factor
		// diddly This factor is not used anymore?
			propertyName = new String(LIST_KEY_STRING+"exposure_time.multiplication_factor."+
				timeOfNightString+"saturated");
			multiplicationFactorList[FRAME_STATE_OVEREXPOSED] = status.getPropertyDouble(propertyName);
		}
		catch (Exception e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":loadProperties:Failed to get property:"+propertyName);
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2300);
			twilightCalibrateDone.setErrorString(errorString);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
		if(loadCalibrationList(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return false;
		if(loadOffsetList(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return false;
		if(loadState(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return false;
		return true;
	}

	/**
	 * Method to load a list of calibrations to do. The list used depends on whether timeOfNight is set to
	 * sunrise or sunset.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if it succeeds, false if it fails. If false is returned the error
	 * 	data in twilightCalibrateDone is filled in.
	 * @see #calibrationList
	 * @see #getFilterSensitivity
	 * @see #LIST_KEY_STRING
	 * @see #LIST_KEY_CALIBRATION_STRING
	 * @see #LIST_KEY_FILTER_LOWER_STRING
	 * @see #LIST_KEY_FILTER_UPPER_STRING
	 * @see #LIST_KEY_BIN_STRING
	 * @see #LIST_KEY_AMPLIFIER_STRING
	 * @see #LIST_KEY_FREQUENCY_STRING
	 * @see #LIST_KEY_SUNSET_STRING
	 * @see #LIST_KEY_SUNRISE_STRING
	 * @see #timeOfNight
	 */
	protected boolean loadCalibrationList(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		TWILIGHT_CALIBRATECalibration calibration = null;
		String timeOfNightString = null;
		String lowerFilter = null;
		String upperFilter = null;
		int index,bin;
		long frequency;
		double lowerFilterSensitivity,upperFilterSensitivity;
		boolean done,useWindowAmplifier;

		index = 0;
		done = false;
		calibrationList = new Vector();
		if(timeOfNight == TIME_OF_NIGHT_SUNSET)
			timeOfNightString = LIST_KEY_SUNSET_STRING;
		else
			timeOfNightString = LIST_KEY_SUNRISE_STRING;
		while(done == false)
		{
			lowerFilter = status.getProperty(LIST_KEY_STRING+LIST_KEY_CALIBRATION_STRING+
							timeOfNightString+index+LIST_KEY_FILTER_LOWER_STRING);
			upperFilter = status.getProperty(LIST_KEY_STRING+LIST_KEY_CALIBRATION_STRING+
							timeOfNightString+index+LIST_KEY_FILTER_UPPER_STRING);
			if((lowerFilter != null)||(upperFilter != null))
			{
			// create calibration instance
				calibration = new TWILIGHT_CALIBRATECalibration();
			// get parameters from properties
				try
				{
					frequency = status.getPropertyLong(LIST_KEY_STRING+LIST_KEY_CALIBRATION_STRING+
							timeOfNightString+index+LIST_KEY_FREQUENCY_STRING);
					bin = status.getPropertyInteger(LIST_KEY_STRING+LIST_KEY_CALIBRATION_STRING+
							timeOfNightString+index+LIST_KEY_BIN_STRING);
					useWindowAmplifier  = status.getPropertyBoolean(LIST_KEY_STRING+
											LIST_KEY_CALIBRATION_STRING+
											timeOfNightString+index+
											LIST_KEY_AMPLIFIER_STRING);
				}
				catch(Exception e)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":loadCalibrationList:Failed at index "+index+".");
					ccs.error(this.getClass().getName()+":"+errorString,e);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2301);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// set calibration data
				try
				{
					calibration.setBin(bin);
					calibration.setUseWindowAmplifier(useWindowAmplifier);
					calibration.setLowerFilter(lowerFilter);
					calibration.setUpperFilter(upperFilter);
					calibration.setFrequency(frequency);
				}
				catch(Exception e)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":loadCalibrationList:Failed to set calibration data at index "+index+
						":bin:"+bin+":use window amplifier:"+useWindowAmplifier+
						":lower filter:"+lowerFilter+":upper filter:"+upperFilter+
						":frequency:"+frequency+".");
					ccs.error(this.getClass().getName()+":"+errorString,e);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2302);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// get filter sensitivities, and set calibration sensitivities
				try
				{
					lowerFilterSensitivity = getFilterSensitivity(lowerFilter);
					upperFilterSensitivity = getFilterSensitivity(upperFilter);
					calibration.setFilterSensitivity(lowerFilterSensitivity*
									upperFilterSensitivity);
				}
				catch(Exception e)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":loadCalibrationList:Failed to set filter sensitivities at index "+
						index+":bin:"+bin+
						":lower filter:"+lowerFilter+":upper filter:"+upperFilter+
						":frequency:"+frequency+".");
					ccs.error(this.getClass().getName()+":"+errorString,e);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2303);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// add calibration instance to list
				calibrationList.add(calibration);
			// log
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getClass().getName()+
					":Loaded calibration "+index+
					"\n\tbin:"+calibration.getBin()+
					":use window amplifier:"+calibration.useWindowAmplifier()+
					":lower filter:"+calibration.getLowerFilter()+
					":upper filter:"+calibration.getUpperFilter()+
					":frequency:"+calibration.getFrequency()+".");
			}
			else
				done = true;
			index++;
		}
		return true;
	}

	/**
	 * Method to get the relative filter sensitivity of a filter.
	 * @param filterType The type name of the filter to find the sensitivity for.
	 * @return A double is returned, which is the filter sensitivity relative to no filter,
	 * 	and should be in the range 0 to 1.
	 * @exception IllegalArgumentException Thrown if the filter sensitivity returned from the
	 * 	property is out of the range 0..1.
	 * @exception NumberFormatException  Thrown if the filter sensitivity property is not a valid double.
	 */
	protected double getFilterSensitivity(String filterType) throws NumberFormatException, IllegalArgumentException
	{
		double filterSensitivity;

		filterSensitivity = status.getPropertyDouble(LIST_KEY_STRING+LIST_KEY_FILTER_SENSITIVITY_STRING+
								filterType);
		if((filterSensitivity < 0.0)||(filterSensitivity > 1.0))
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterSensitivity failed:filter type "+filterType+
				" has filter sensitivity "+filterSensitivity+", which is out of range.");
		}
		return filterSensitivity;
	}

	/**
	 * Method to initialse twilightCalibrateState.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if it succeeds, false if it fails. If false is returned the error
	 * 	data in twilightCalibrateDone is filled in.
	 * @see #twilightCalibrateState
	 * @see #stateFilename
	 */
	protected boolean loadState(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
	// initialise and load twilightCalibrateState instance
		twilightCalibrateState = new TWILIGHT_CALIBRATESavedState();
		try
		{
			twilightCalibrateState.load(stateFilename);
		}
		catch (Exception e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":loadState:Failed to load state filename:"+stateFilename);
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2304);
			twilightCalibrateDone.setErrorString(errorString);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * Method to load a list of telescope RA/DEC offsets. These are used to offset the telescope
	 * between frames of the same calibration.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if it succeeds, false if it fails. If false is returned the error
	 * 	data in twilightCalibrateDone is filled in.
	 * @see #offsetList
	 * @see #LIST_KEY_OFFSET_STRING
	 * @see #LIST_KEY_RA_STRING
	 * @see #LIST_KEY_DEC_STRING
	 */
	protected boolean loadOffsetList(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{

		TWILIGHT_CALIBRATEOffset offset = null;
		String testString = null;
		int index;
		double raOffset,decOffset;
		boolean done;

		index = 0;
		done = false;
		offsetList = new Vector();
		while(done == false)
		{
			testString = status.getProperty(LIST_KEY_STRING+LIST_KEY_OFFSET_STRING+
							index+LIST_KEY_RA_STRING);
			if((testString != null))
			{
			// create offset
				offset = new TWILIGHT_CALIBRATEOffset();
			// get parameters from properties
				try
				{
					raOffset = status.getPropertyDouble(LIST_KEY_STRING+LIST_KEY_OFFSET_STRING+
							index+LIST_KEY_RA_STRING);
					decOffset = status.getPropertyDouble(LIST_KEY_STRING+LIST_KEY_OFFSET_STRING+
							index+LIST_KEY_DEC_STRING);
				}
				catch(Exception e)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":loadOffsetList:Failed at index "+index+".");
					ccs.error(this.getClass().getName()+":"+errorString,e);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2305);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// set offset data
				try
				{
					offset.setRAOffset((float)raOffset);
					offset.setDECOffset((float)decOffset);
				}
				catch(Exception e)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":loadOffsetList:Failed to set data at index "+index+
						":RA offset:"+raOffset+":DEC offset:"+decOffset+".");
					ccs.error(this.getClass().getName()+":"+errorString,e);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2306);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// add offset instance to list
				offsetList.add(offset);
			// log
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getClass().getName()+
					":Loaded offset "+index+
					"\n\tRA Offset:"+offset.getRAOffset()+
					":DEC Offset:"+offset.getDECOffset()+".");
			}
			else
				done = true;
			index++;
		}
		return true;
	}

	/**
	 * This method matches the saved state to the calibration list to set the last time
	 * each calibration was completed.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if it succeeds, false if it fails. Currently always returns true.
	 * @see #calibrationList
	 * @see #twilightCalibrateState
	 */
	protected boolean addSavedStateToCalibration(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		TWILIGHT_CALIBRATECalibration calibration = null;
		String lowerFilter = null;
		String upperFilter = null;
		int bin;
		long lastTime;
		boolean useWindowAmplifier;

		for(int i = 0; i< calibrationList.size(); i++)
		{
			calibration = (TWILIGHT_CALIBRATECalibration)(calibrationList.get(i));
			bin = calibration.getBin();
			useWindowAmplifier = calibration.useWindowAmplifier();
			lowerFilter = calibration.getLowerFilter();
			upperFilter = calibration.getUpperFilter();
			lastTime = twilightCalibrateState.getLastTime(bin,useWindowAmplifier,lowerFilter,upperFilter);
			calibration.setLastTime(lastTime);
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+":Calibration:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+
				":frequency:"+calibration.getFrequency()+
				"\n\t\tnow has last time set to:"+lastTime+".");
		}
		return true;
	}

	/**
	 * This method does the specified calibration.
	 * <ul>
	 * <li>The relevant data is retrieved from the calibration parameter.
	 * <li>If we did this calibration more recently than frequency, log and return.
	 * <li>The start exposure length is recalculated, by dividing by the last relative sensitivity used
	 * 	(to get the exposure length as if though a clear filter), and then dividing by the 
	 * 	new relative filter sensitivity (to increase the exposure length). 
	 * <li>If the new exposure length is too short, it is reset to the minimum exposure length.
	 * <li>If the new exposure length is too long, the fact is logged and the method returns.
	 * <li><b>sendBasicAck</b> is called to stop the client timing out before the config is completed.
	 * <li><b>doConfig</b> is called for the relevant binning factor/filter set to be setup.
	 * <li><b>sendBasicAck</b> is called to stop the client timing out before the first frame is completed.
	 * <li><b>doOffsetList</b> is called to go through the telescope RA/DEC offsets and take frames at
	 * 	each offset.
	 * <li>If the calibration suceeded, the saved state's last time is updated to now, and the state saved.
	 * </ul>
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @param calibration The calibration to do.
	 * @return The method returns true if the calibration was done successfully, false if an error occured.
	 * @see #doConfig
	 * @see #doOffsetList
	 * @see #sendBasicAck
	 * @see #stateFilename
	 * @see #lastFilterSensitivity
	 * @see #lastBin
	 * @see #calibrationFrameCount
	 */
	protected boolean doCalibration(TWILIGHT_CALIBRATE twilightCalibrateCommand,
			TWILIGHT_CALIBRATE_DONE twilightCalibrateDone,TWILIGHT_CALIBRATECalibration calibration)
	{
		String lowerFilter = null;
		String upperFilter = null;
		int bin;
		long lastTime,frequency;
		long now;
		double filterSensitivity;
		boolean useWindowAmplifier;

		ccs.log(Logging.VERBOSITY_INTERMEDIATE,
			"Command:"+twilightCalibrateCommand.getClass().getName()+
			":doCalibrate:"+
			"\n\tbin:"+calibration.getBin()+
			":use window amplifier:"+calibration.useWindowAmplifier()+
			":lower filter:"+calibration.getLowerFilter()+
			":upper filter:"+calibration.getUpperFilter()+
			":frequency:"+calibration.getFrequency()+
			":filter sensitivity:"+calibration.getFilterSensitivity()+
			":last time:"+calibration.getLastTime()+".");
	// get copy of calibration data
		bin = calibration.getBin();
		useWindowAmplifier = calibration.useWindowAmplifier();
		lowerFilter = calibration.getLowerFilter();
		upperFilter = calibration.getUpperFilter();
		frequency = calibration.getFrequency();
		lastTime = calibration.getLastTime();
		filterSensitivity = calibration.getFilterSensitivity();
	// get current time
		now = System.currentTimeMillis();
	// if we did the calibration more recently than frequency, log and return
		if(now-lastTime < frequency)
		{
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+
				":frequency:"+calibration.getFrequency()+
				":last time:"+lastTime+
				"\n\tNOT DONE: too soon since last completed:"+
				"\n\t"+now+" - "+lastTime+" < "+frequency+".");
			return true;
		}
	// recalculate the exposure length
		exposureLength = (int)((((double)exposureLength)*lastFilterSensitivity)/filterSensitivity);
		exposureLength = (exposureLength*(lastBin*lastBin))/(bin*bin);
	// if we are going to do this calibration, reset the last filter sensitivity for next time
	// We need to think about when to do this when the new exposure length means we DON'T do the calibration
		lastFilterSensitivity = filterSensitivity;
		lastBin = bin;
	// check exposure time
		if(exposureLength < minExposureLength)
		{
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+
				":frequency:"+calibration.getFrequency()+
				":last time:"+lastTime+
				"\n\tcalculated exposure length:"+exposureLength+
				" too short, using minimum:"+minExposureLength+".");
			exposureLength = minExposureLength;
		}
		if(exposureLength > maxExposureLength)
		{
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+
				":frequency:"+calibration.getFrequency()+
				":last time:"+lastTime+
				"\n\tcalculated exposure length:"+exposureLength+
				" too long, using maximum:"+maxExposureLength+".");
			exposureLength = maxExposureLength;
		}
		if((now+exposureLength+frameOverhead) > 
			(implementationStartTime+twilightCalibrateCommand.getTimeToComplete()))
		{
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:Ran out of time to complete:"+
				"\n\t((now:"+now+
				")+(exposureLength:"+exposureLength+
				")+(frameOverhead:"+frameOverhead+")) > "+
				"\n\t((implementationStartTime:"+implementationStartTime+
				")+(timeToComplete:"+twilightCalibrateCommand.getTimeToComplete()+")).");
			return true;
		}
	// send an ack before the frame, so the client doesn't time out during configuration
		if(sendBasicAck(twilightCalibrateCommand,twilightCalibrateDone,frameOverhead) == false)
			return false;
	// configure CCD camera
		if(doConfig(twilightCalibrateCommand,twilightCalibrateDone,bin,useWindowAmplifier,
			    lowerFilter,upperFilter) == false)
			return false;
	// send an ack before the frame, so the client doesn't time out during the first exposure
		if(sendBasicAck(twilightCalibrateCommand,twilightCalibrateDone,exposureLength+frameOverhead) == false)
			return false;
	// do the frames with this configuration
		calibrationFrameCount = 0;
		if(doOffsetList(twilightCalibrateCommand,twilightCalibrateDone) == false)
			return false;
	// update state, if we completed the whole calibration.
		if(calibrationFrameCount == offsetList.size())
		{
			twilightCalibrateState.setLastTime(bin,useWindowAmplifier,lowerFilter,upperFilter);
			try
			{
				twilightCalibrateState.save(stateFilename);
			}
			catch(IOException e)
			{
				String errorString = new String(twilightCalibrateCommand.getId()+
					":doCalibration:Failed to save state filename:"+stateFilename);
				ccs.error(this.getClass().getName()+":"+errorString,e);
				twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2307);
				twilightCalibrateDone.setErrorString(errorString);
				twilightCalibrateDone.setSuccessful(false);
				return false;
			}
			lastTime = twilightCalibrateState.getLastTime(bin,useWindowAmplifier,lowerFilter,upperFilter);
			calibration.setLastTime(lastTime);
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:Calibration successfully completed:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+".");
		}// end if done calibration
		else
		{
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":doCalibrate:Calibration NOT completed:"+
				"\n\tbin:"+calibration.getBin()+
				":use window amplifier:"+calibration.useWindowAmplifier()+
				":lower filter:"+calibration.getLowerFilter()+
				":upper filter:"+calibration.getUpperFilter()+".");
		}
		return true;
	}

	/**
	 * Method to setup the CCD configuration with the specified binning factor.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @param bin The binning factor to use.
	 * @param useWindowAmplifier Boolean specifying whether to use the default amplifier (false), or the
	 *                           amplifier used for windowing readouts (true).
	 * @param lowerFilter The type of filter to use in the lower wheel.
	 * @param upperFilter The type of filter to use in the upper wheel.
	 * @return The method returns true if the calibration was done successfully, false if an error occured.
	 * @see #setFocusOffset
	 * @see CcsStatus#getNumberColumns
	 * @see CcsStatus#getNumberRows
	 */
	protected boolean doConfig(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone,int bin,boolean useWindowAmplifier,
				   String lowerFilter,String upperFilter)
	{
		CCDLibrarySetupWindow windowList[] = new CCDLibrarySetupWindow[CCDLibrary.CCD_SETUP_WINDOW_COUNT];
		int numberColumns,numberRows,amplifier,deInterlaceSetting;

	// load other required config for dimension configuration from CCS properties file.
		try
		{
			numberColumns = status.getNumberColumns(bin);
			numberRows = status.getNumberRows(bin);
		}
	// CCDLibraryFormatException,IllegalArgumentException,NumberFormatException.
		catch(Exception e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":doConfig:Failed to get config properties:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2308);
			twilightCalibrateDone.setErrorString(errorString);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
	// test abort
		if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
			return false;
	// set up blank windows
		for(int i = 0; i < CCDLibrary.CCD_SETUP_WINDOW_COUNT; i++)
		{
			windowList[i] = new CCDLibrarySetupWindow(-1,-1,-1,-1);
		}
	// test abort
		if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
			return false;
	// send configuration to the SDSU controller
		try
		{
			libccd.CCDSetupDimensions(numberColumns,numberRows,bin,bin,0,windowList);
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
		}
		catch(Exception e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":doConfig:Failed to configure CCD/filter wheel:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2309);
			twilightCalibrateDone.setErrorString(errorString);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
	// test abort
		if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
			return false;
	// Issue ISS OFFSET_FOCUS commmand based on the optical thickness of the filter(s)
		if(setFocusOffset(twilightCalibrateCommand,twilightCalibrateDone) == false)
		{
			return false;
		}
	// Increment unique config ID.
	// This is queried when saving FITS headers to get the CONFIGID value.
		try
		{
			status.incConfigId();
		}
		catch(Exception e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":doConfig:Incrementing configuration ID failed:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2310);
			twilightCalibrateDone.setErrorString(errorString+e);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
	// Store name of configuration used in status object.
	// This is queried when saving FITS headers to get the CONFNAME value.
		status.setConfigName("TWILIGHT_CALIBRATION:"+twilightCalibrateCommand.getId()+
					":"+bin+":"+useWindowAmplifier);
		return true;
	}

	/**
	 * Routine to set the telescope focus offset, due to the filters selected. Sends a OFFSET_FOCUS command to
	 * the ISS. The OFFSET_FOCUS sent is the total of the selected filter's optical thickness.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if the telescope attained the focus offset, otherwise false is
	 * 	returned an telFocusDone is filled in with an error message.
	 */
	protected boolean setFocusOffset(TWILIGHT_CALIBRATE twilightCalibrateCommand,
					 TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		OFFSET_FOCUS focusOffsetCommand = null;
		INST_TO_ISS_DONE instToISSDone = null;
		String filterIdName = null;
		String filterTypeString = null;
		float focusOffset = 0.0f;

		focusOffsetCommand = new OFFSET_FOCUS(twilightCalibrateCommand.getId());
		focusOffset = 0.0f;
	// get default focus offset
		focusOffset += status.getPropertyDouble("ccs.focus.offset");
		ccs.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			":setFocusOffset:Master offset is "+focusOffset+".");
	// log focus offset
		ccs.log(Logging.VERBOSITY_INTERMEDIATE,
			"Command:"+twilightCalibrateCommand.getClass().getName()+
			":Attempting focus offset "+focusOffset+".");
	// set the commands focus offset
		focusOffsetCommand.setFocusOffset(focusOffset);
		//instToISSDone = ccs.sendISSCommand(focusOffsetCommand,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false )
		{
			String errorString = null;

			errorString = new String("setFocusOffset failed:"+focusOffset+":"+
				instToISSDone.getErrorString());
			ccs.error(errorString);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2311);
			twilightCalibrateDone.setErrorString(this.getClass().getName()+":"+errorString);
			//twilightCalibrateDone.setSuccessful(false);   IT
			twilightCalibrateDone.setSuccessful(true);
			//return false;
			return true;    // IT
		}
		return true;
	}

	/**
	 * This method goes through the offset list for the configured calibration. It trys to
	 * get a frame for each offset.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true when the offset list is terminated, false if an error occured.
	 * @see #offsetList
	 * @see #doFrame
	 * @see Ccs#sendISSCommand
	 * @see ngat.message.ISS_INST.OFFSET_RA_DEC
	 */
	protected boolean doOffsetList(TWILIGHT_CALIBRATE twilightCalibrateCommand,
					TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		TWILIGHT_CALIBRATEOffset offset = null;
		OFFSET_RA_DEC offsetRaDecCommand = null;
		INST_TO_ISS_DONE instToISSDone = null;
		int offsetListIndex;

		doneOffset = false;
		offsetListIndex = 0;
		while((doneOffset == false) && (offsetListIndex < offsetList.size()))
		{
		// get offset
			offset = (TWILIGHT_CALIBRATEOffset)(offsetList.get(offsetListIndex));
		// log telescope offset
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getClass().getName()+
				":Attempting telescope position offset index "+offsetListIndex+
				"\n\tRA:"+offset.getRAOffset()+
				":DEC:"+offset.getDECOffset()+".");
		// tell telescope of offset RA and DEC
			offsetRaDecCommand = new OFFSET_RA_DEC(twilightCalibrateCommand.getId());
			offsetRaDecCommand.setRaOffset(offset.getRAOffset());
			offsetRaDecCommand.setDecOffset(offset.getDECOffset());
			// instToISSDone = ccs.sendISSCommand(offsetRaDecCommand,serverConnectionThread);  IT
			if(instToISSDone.getSuccessful() == false)
			{
				String errorString = null;

				errorString = new String("Offset Ra Dec failed:ra = "+offset.getRAOffset()+
					", dec = "+offset.getDECOffset()+":"+instToISSDone.getErrorString());
				ccs.error(errorString);
				twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2312);
				twilightCalibrateDone.setErrorString(this.getClass().getName()+
									":doOffsetList:"+errorString);
				twilightCalibrateDone.setSuccessful(false);
				return false;
			}
		// do exposure at this offset
			if(doFrame(twilightCalibrateCommand,twilightCalibrateDone) == false)
				return false;
			offsetListIndex++;
		}// end for on offset list
		return true;
	}

	/**
	 * The method that does a calibration frame with the current configuration. The following is performed 
	 * in a while loop, that is terminated when a good frame has been taken.
	 * <ul>
	 * <li>The pause and resume times are cleared, and the FITS headers setup from the current configuration.
	 * <li>Some FITS headers are got from the ISS.
	 * <li><b>testAbort</b> is called to see if this command implementation has been aborted.
	 * <li>The FITS headers are saved using <b>saveFitsHeaders</b>, to the temporary FITS filename.
	 * <li>The frame is taken, using libccd's <b>CCDExposureExpose</b> method.
	 * <li>The last exposure length variable is updated.
	 * <li>An instance of TWILIGHT_CALIBRATE_ACK is sent back to the client using <b>sendTwilightCalibrateAck</b>.
	 * <li><b>testAbort</b> is called to see if this command implementation has been aborted.
	 * <li><b>reduceCalibrate</b> is called to pass the frame to the Real Time Data Pipeline for processing.
	 * <li>The frame state is derived from the returned mean counts.
	 * <li>If the frame state was good, the raw frame and DpRt reduced (if different) are renamed into
	 * 	the standard FITS filename using ccsFilename, by incrementing the run number.
	 * <li><b>testAbort</b> is called to see if this command implementation has been aborted.
	 * <li>The exposure Length is modified by multiplying by the ratio of best mean counts over mean counts.
	 * <li>If the calculated exposure length is out of the acceptable range,
	 *     but the last attempt was not at the range limit or the last attempt was at the limit but acceptable, 
	 *     reset the exposure length to the limit.
	 * <li>An instance of TWILIGHT_CALIBRATE_DP_ACK is sent back to the client using
	 * 	<b>sendTwilightCalibrateDpAck</b>.
	 * <li>We check to see if the loop should be terminated:
	 * 	<ul>
	 * 	<li>If the frame state is OK, the loop is exited and the method stopped.
	 * 	<li>If the next exposure will take longer than the time remaining, we stop the frame loop,
	 * 		offset loop and calibration loop (i.e. the TWILIGHT_CALIBRATE command is terminated).
	 * 	<li>If the next exposure will take longer than the maximum exposure length, we stop the frame loop and
	 * 		offset loop. (i.e. we try the next calibration).
	 * 	<li>If the next exposure will be shorter than the minimum exposure length, we stop the frame loop and
	 * 		offset loop. (i.e. we try the next calibration).
	 * 	</ul>
	 * </ul>
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @return The method returns true if no errors occured, false if an error occured.
	 * @see FITSImplementation#testAbort
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see FITSImplementation#ccsFilename
	 * @see FITSImplementation#libccd
	 * @see ngat.rise.ccd.CCDLibrary#CCDExposureExpose
	 * @see #sendTwilightCalibrateAck
	 * @see #sendTwilightCalibrateDpAck
	 * @see CALIBRATEImplementation#reduceCalibrate
	 * @see #exposureLength
	 * @see #lastExposureLength
	 * @see #minExposureLength
	 * @see #maxExposureLength
	 * @see #frameOverhead
	 * @see #temporaryFITSFilename
	 * @see #implementationStartTime
	 * @see #FRAME_STATE_OVEREXPOSED
	 * @see #FRAME_STATE_UNDEREXPOSED
	 * @see #FRAME_STATE_OK
	 * @see #FRAME_STATE_NAME_LIST
	 */
	protected boolean doFrame(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone)
	{
		File temporaryFile = null;
		File newFile = null;
		String filename = null;
		String reducedFilename = null;
		long now;
		int frameState;
		float meanCounts,countsDifference;
		boolean doneFrame;

		doneFrame = false;
		while(doneFrame == false)
		{
		// Clear the pause and resume times.
			status.clearPauseResumeTimes();
		// delete old temporary file.
			temporaryFile = new File(temporaryFITSFilename);
			if(temporaryFile.exists())
				temporaryFile.delete();
		// setup fits headers
			clearFitsHeaders();
			if(setFitsHeaders(twilightCalibrateCommand,twilightCalibrateDone,
				FitsHeaderDefaults.OBSTYPE_VALUE_SKY_FLAT,exposureLength) == false)
				return false;
			if(getFitsHeadersFromISS(twilightCalibrateCommand,twilightCalibrateDone) == false)
				return false;
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
			if(saveFitsHeaders(twilightCalibrateCommand,twilightCalibrateDone,
				temporaryFITSFilename) == false)
				return false;
			status.setExposureFilename(temporaryFITSFilename);
		// log exposure attempt
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getId()+
				":doFrame:Attempting exposure:"+
				"\n\tlength "+exposureLength+".");
		// do exposure
			try
			{
				libccd.CCDExposureExpose(true,-1,exposureLength,temporaryFITSFilename);
			}
			catch(CCDLibraryNativeException e)
			{
				String errorString = new String(twilightCalibrateCommand.getId()+
					":doFrame:Doing frame of length "+exposureLength+" failed:");
				ccs.error(this.getClass().getName()+":"+errorString,e);
				twilightCalibrateDone.setFilename(temporaryFITSFilename);
				twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2313);
				twilightCalibrateDone.setErrorString(errorString+e);
				twilightCalibrateDone.setSuccessful(false);
				return false;
			}
		// set last exposure length
			lastExposureLength = exposureLength;
		// send with filename back to client
		// time to complete is reduction time, we will send another ACK after reduceCalibrate
			if(sendTwilightCalibrateAck(twilightCalibrateCommand,twilightCalibrateDone,frameOverhead,
				temporaryFITSFilename) == false)
				return false; 
		// Test abort status.
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
		// Call pipeline to reduce data.
			if(reduceCalibrate(twilightCalibrateCommand,twilightCalibrateDone,
				temporaryFITSFilename) == false)
				return false;
		// Test abort status.
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
		// log reduction
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getId()+
				":doFrame:Exposure reduction:"+
				"\n\tlength "+exposureLength+
				":filename:"+twilightCalibrateDone.getFilename()+
				":mean counts:"+twilightCalibrateDone.getMeanCounts()+
				":peak counts:"+twilightCalibrateDone.getPeakCounts()+".");
		// get reduced filename from done
			reducedFilename = twilightCalibrateDone.getFilename();
		// get mean counts and set frame state.
			meanCounts = twilightCalibrateDone.getMeanCounts();
			if(meanCounts > maxMeanCounts)
				frameState = FRAME_STATE_OVEREXPOSED;
			else if(meanCounts < minMeanCounts)
				frameState = FRAME_STATE_UNDEREXPOSED;
			else
				frameState = FRAME_STATE_OK;
		// log frame state
			ccs.log(Logging.VERBOSITY_INTERMEDIATE,
				"Command:"+twilightCalibrateCommand.getId()+
				":doFrame:Exposure frame state:"+
				"\n\tlength "+exposureLength+
				":mean counts:"+twilightCalibrateDone.getMeanCounts()+
				":peak counts:"+twilightCalibrateDone.getPeakCounts()+
				":frame state:"+FRAME_STATE_NAME_LIST[frameState]+".");
		// if the frame was good, rename it
			if(frameState == FRAME_STATE_OK)
			{
			// raw frame
				temporaryFile = new File(temporaryFITSFilename);
			// does the temprary file exist?
				if(temporaryFile.exists() == false)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
								":File does not exist:"+temporaryFITSFilename);

					ccs.error(this.getClass().getName()+
						":doFrame:"+errorString);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2314);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// get a filename to store frame in
				ccsFilename.nextRunNumber();
				filename = ccsFilename.getFilename();
				newFile = new File(filename);
			// rename temporary filename to filename
				if(temporaryFile.renameTo(newFile) == false)
				{
					String errorString = new String(twilightCalibrateCommand.getId()+
						":Failed to rename '"+temporaryFile.toString()+"' to '"+
						newFile.toString()+"'.");

					ccs.error(this.getClass().getName()+":doFrame:"+errorString);
					twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2315);
					twilightCalibrateDone.setErrorString(errorString);
					twilightCalibrateDone.setSuccessful(false);
					return false;
				}
			// log rename
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getId()+
					":doFrame:Exposure raw frame rename:"+
					"\n\trenamed "+temporaryFile+" to "+newFile+".");
			// reset twilight calibrate done's filename to renamed file
			// in case pipelined reduced filename does not exist/cannot be renamed
				twilightCalibrateDone.setFilename(filename);
			// real time pipelined processed file
				temporaryFile = new File(reducedFilename);
			// does the temprary file exist? If it doesn't this is not an error,
			// if the DpRt returned the same file it was passed in it will have already been renamed
				if(temporaryFile.exists())
				{
				// get a filename to store pipelined processed frame in
					try
					{
						ccsFilename.setPipelineProcessing(FitsFilename.
									PIPELINE_PROCESSING_FLAG_REAL_TIME);
						filename = ccsFilename.getFilename();
						ccsFilename.setPipelineProcessing(FitsFilename.
									PIPELINE_PROCESSING_FLAG_NONE);
					}
					catch(Exception e)
					{
						String errorString = new String(twilightCalibrateCommand.getId()+
									    ":doFrame:setPipelineProcessing failed:");
						ccs.error(this.getClass().getName()+":"+errorString,e);
						twilightCalibrateDone.setFilename(reducedFilename);
						twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2321);
						twilightCalibrateDone.setErrorString(errorString+e);
						twilightCalibrateDone.setSuccessful(false);
						return false;
					}
					newFile = new File(filename);
				// rename temporary filename to filename
					if(temporaryFile.renameTo(newFile) == false)
					{
						String errorString = new String(twilightCalibrateCommand.getId()+
							":Failed to rename '"+temporaryFile.toString()+"' to '"+
							newFile.toString()+"'.");

						ccs.error(this.getClass().getName()+":doFrame:"+errorString);
						twilightCalibrateDone.
							setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2316);
						twilightCalibrateDone.setErrorString(errorString);
						twilightCalibrateDone.setSuccessful(false);
						return false;
					}// end if renameTo failed
				// reset twilight calibrate done's pipelined processed filename
					twilightCalibrateDone.setFilename(filename);
				// log rename
					ccs.log(Logging.VERBOSITY_INTERMEDIATE,
						"Command:"+twilightCalibrateCommand.getId()+
						":doFrame:Exposure DpRt frame rename:"+
						"\n\trenamed "+temporaryFile+" to "+newFile+".");
				}// end if temporary file exists
			}// end if frameState was OK
		// Test abort status.
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
		// multiply exposure Length by scale factor 
		// to scale current mean counts to best mean counts
			exposureLength = (int)(((float) exposureLength) * (((float)bestMeanCounts)/meanCounts));
		// If the calculated exposure length is out of the acceptable range,
	        // but the last attempt was not at the range limit or 
                // the last attempt was at the limit but acceptable, 
		// reset the exposure length to the limit.
			if((exposureLength > maxExposureLength)&&
			   ((lastExposureLength != maxExposureLength)||(frameState == FRAME_STATE_OK)))
			{
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getId()+
					":doFrame:Calculated exposure length:"+exposureLength+
					"\n\tout of range, but going to try "+maxExposureLength+
					"\n\tas last exposure length was "+lastExposureLength+
					" with frame state "+FRAME_STATE_NAME_LIST[frameState]+".");
				exposureLength = maxExposureLength;
			}
			if((exposureLength < minExposureLength)&&
			   ((lastExposureLength != minExposureLength)||(frameState == FRAME_STATE_OK)))
			{
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getId()+
					":doFrame:Calculated exposure length:"+exposureLength+
					"\n\tout of range, but going to try "+minExposureLength+
					"\n\tas last exposure length was "+lastExposureLength+
					" with frame state "+FRAME_STATE_NAME_LIST[frameState]+".");
				exposureLength = minExposureLength;
			}
		// send dp_ack, filename/mean counts/peak counts are all retrieved from twilightCalibrateDone,
		// which had these parameters filled in by reduceCalibrate
		// time to complete is readout overhead + exposure Time for next frame
			if(sendTwilightCalibrateDpAck(twilightCalibrateCommand,twilightCalibrateDone,
				exposureLength+frameOverhead) == false)
				return false;
		// Test abort status.
			if(testAbort(twilightCalibrateCommand,twilightCalibrateDone) == true)
				return false;
		// check loop termination
			now = System.currentTimeMillis();
			if(frameState == FRAME_STATE_OK)
			{
				doneFrame = true;
				calibrationFrameCount++;
			// log
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getId()+
					":doFrame:Frame completed.");
			}
			if((now+exposureLength+frameOverhead) > 
				(implementationStartTime+twilightCalibrateCommand.getTimeToComplete()))
			{
			// try next calibration
				doneFrame = true;
				doneOffset = true;
			// log
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,
					"Command:"+twilightCalibrateCommand.getId()+
					":doFrame:Ran out of time to complete:"+
					"\n\t((now:"+now+
					")+(exposureLength:"+exposureLength+
					")+(frameOverhead:"+frameOverhead+")) > "+
					"\n\t((implementationStartTime:"+implementationStartTime+
					")+(timeToComplete:"+twilightCalibrateCommand.getTimeToComplete()+")).");
			}
			if(exposureLength > maxExposureLength)
			{
				if(timeOfNight == TIME_OF_NIGHT_SUNSET)
				{
					// try next calibration
					doneFrame = true;
					doneOffset = true;
					// log
					ccs.log(Logging.VERBOSITY_INTERMEDIATE,
						"Command:"+twilightCalibrateCommand.getId()+
						":doFrame:Exposure length too long:"+
						"\n\t(exposureLength:"+exposureLength+") > "+
						"(maxExposureLength:"+maxExposureLength+").");
				}
				else // retry this calibration - it has got lighter
				{
					// log
					ccs.log(Logging.VERBOSITY_INTERMEDIATE,
						"Command:"+twilightCalibrateCommand.getId()+
						":doFrame:Calulated Exposure length too long:"+
						"\n\t(exposureLength:"+exposureLength+") > "+
						"(maxExposureLength:"+maxExposureLength+"): retrying as it is dawn.");
					exposureLength = maxExposureLength;
				}
			}
			if(exposureLength < minExposureLength)
			{
				if(timeOfNight == TIME_OF_NIGHT_SUNRISE)
				{
					// try next calibration
					doneFrame = true;
					doneOffset = true;
					// log
					ccs.log(Logging.VERBOSITY_INTERMEDIATE,
						"Command:"+twilightCalibrateCommand.getId()+
						":doFrame:Exposure length too short:"+
						"\n\t(exposureLength:"+exposureLength+") < "+
						"(minExposureLength:"+minExposureLength+").");
				}
				else // retry this calibration - it has got darker
				{
					// log
					ccs.log(Logging.VERBOSITY_INTERMEDIATE,
						"Command:"+twilightCalibrateCommand.getId()+
						":doFrame:Calulated Exposure length too short:"+
						"\n\t(exposureLength:"+exposureLength+") < "+
						"(maxExposureLength:"+maxExposureLength+"): retrying as it is dusk.");
					exposureLength = minExposureLength;
				}
			}
		}// end while !doneFrame
		return true;
	}

	/**
	 * Method to send an instance of ACK back to the client. This stops the client timing out, whilst we
	 * work out what calibration to attempt next.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @param timeToComplete The time it will take to complete the next set of operations
	 *	before the next ACK or DONE is sent to the client. The time is in milliseconds. 
	 * 	The server connection thread's default acknowledge time is added to the value before it
	 * 	is sent to the client, to allow for network delay etc.
	 * @return The method returns true if the ACK was sent successfully, false if an error occured.
	 */
	protected boolean sendBasicAck(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone,
		int timeToComplete)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(twilightCalibrateCommand.getId());
		acknowledge.setTimeToComplete(timeToComplete+serverConnectionThread.getDefaultAcknowledgeTime());
		try
		{
			serverConnectionThread.sendAcknowledge(acknowledge,true);
		}
		catch(IOException e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":sendBasicAck:Sending ACK failed:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2317);
			twilightCalibrateDone.setErrorString(errorString+e);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * Method to send an instance of TWILIGHT_CALIBRATE_ACK back to the client. This tells the client about
	 * a FITS frame that has been produced, and also stops the client timing out.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * @param timeToComplete The time it will take to complete the next set of operations
	 *	before the next ACK or DONE is sent to the client. The time is in milliseconds. 
	 * 	The server connection thread's default acknowledge time is added to the value before it
	 * 	is sent to the client, to allow for network delay etc.
	 * @param filename The FITS filename to be sent back to the client, that has just completed
	 * 	processing.
	 * @return The method returns true if the ACK was sent successfully, false if an error occured.
	 */
	protected boolean sendTwilightCalibrateAck(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone,int timeToComplete,String filename)
	{
		TWILIGHT_CALIBRATE_ACK twilightCalibrateAck = null;

	// send acknowledge to say frame is completed.
		twilightCalibrateAck = new TWILIGHT_CALIBRATE_ACK(twilightCalibrateCommand.getId());
		twilightCalibrateAck.setTimeToComplete(timeToComplete+
			serverConnectionThread.getDefaultAcknowledgeTime());
		twilightCalibrateAck.setFilename(filename);
		try
		{
			serverConnectionThread.sendAcknowledge(twilightCalibrateAck,true);
		}
		catch(IOException e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":sendTwilightCalibrateAck:Sending TWILIGHT_CALIBRATE_ACK failed:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2318);
			twilightCalibrateDone.setErrorString(errorString+e);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * Method to send an instance of TWILIGHT_CALIBRATE_DP_ACK back to the client. This tells the client about
	 * a FITS frame that has been produced, and the mean and peak counts in the frame.
	 * The time to complete parameter stops the client timing out.
	 * @param twilightCalibrateCommand The instance of TWILIGHT_CALIBRATE we are currently running.
	 * @param twilightCalibrateDone The instance of TWILIGHT_CALIBRATE_DONE to fill in with errors we receive.
	 * 	It also contains the filename and mean and peak counts returned from the last reduction calibration.
	 * @param timeToComplete The time it will take to complete the next set of operations
	 *	before the next ACK or DONE is sent to the client. The time is in milliseconds. 
	 * 	The server connection thread's default acknowledge time is added to the value before it
	 * 	is sent to the client, to allow for network delay etc.
	 * @return The method returns true if the ACK was sent successfully, false if an error occured.
	 */
	protected boolean sendTwilightCalibrateDpAck(TWILIGHT_CALIBRATE twilightCalibrateCommand,
		TWILIGHT_CALIBRATE_DONE twilightCalibrateDone,int timeToComplete)
	{
		TWILIGHT_CALIBRATE_DP_ACK twilightCalibrateDpAck = null;

	// send acknowledge to say frame is completed.
		twilightCalibrateDpAck = new TWILIGHT_CALIBRATE_DP_ACK(twilightCalibrateCommand.getId());
		twilightCalibrateDpAck.setTimeToComplete(timeToComplete+
			serverConnectionThread.getDefaultAcknowledgeTime());
		twilightCalibrateDpAck.setFilename(twilightCalibrateDone.getFilename());
		twilightCalibrateDpAck.setMeanCounts(twilightCalibrateDone.getMeanCounts());
		twilightCalibrateDpAck.setPeakCounts(twilightCalibrateDone.getPeakCounts());
		try
		{
			serverConnectionThread.sendAcknowledge(twilightCalibrateDpAck,true);
		}
		catch(IOException e)
		{
			String errorString = new String(twilightCalibrateCommand.getId()+
				":sendTwilightCalibrateDpAck:Sending TWILIGHT_CALIBRATE_DP_ACK failed:");
			ccs.error(this.getClass().getName()+":"+errorString,e);
			twilightCalibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2319);
			twilightCalibrateDone.setErrorString(errorString+e);
			twilightCalibrateDone.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * Private inner class that deals with loading and interpreting the saved state of calibrations
	 * (the TWILIGHT_CALIBRATE calibration database).
	 */
	private class TWILIGHT_CALIBRATESavedState
	{
		private NGATProperties properties = null;

		/**
		 * Constructor.
		 */
		public TWILIGHT_CALIBRATESavedState()
		{
			super();
			properties = new NGATProperties();
		}

		/**
	 	 * Load method, that retrieves the saved state from file.
		 * Calls the <b>properties</b> load method.
		 * @param filename The filename to load the saved state from.
		 * @exception FileNotFoundException Thrown if the file described by filename does not exist.
		 * @exception IOException Thrown if an IO error occurs whilst reading the file.
		 * @see #properties
	 	 */
		public void load(String filename) throws FileNotFoundException, IOException
		{
			properties.load(filename);
		}

		/**
	 	 * Save method, that stores the saved state into a file.
		 * Calls the <b>properties</b> load method.
		 * @param filename The filename to save the saved state to.
		 * @exception FileNotFoundException Thrown if the file described by filename does not exist.
		 * @exception IOException Thrown if an IO error occurs whilst writing the file.
		 * @see #properties
	 	 */
		public void save(String filename) throws IOException
		{
			Date now = null;

			now = new Date();
			properties.save(filename,"TWILIGHT_CALIBRATE saved state saved on:"+now);
		}

		/**
		 * Method to get the last time a calibration with these attributes was done.
		 * @param bin The binning factor used for this calibration.
		 * @param useWindowAmplifier Whether we are using the default amplifier (false) or the amplifier
		 *        used for windowing (true).
		 * @param lowerFilter The lower filter type string used for this calibration.
		 * @param upperFilter The lower filter type string used for this calibration.
		 * @return The number of milliseconds since the EPOCH, the last time a calibration with these
		 * 	parameters was completed. If this calibraion has not been performed before, zero
		 * 	is returned.
		 * @see #LIST_KEY_STRING
		 * @see #LIST_KEY_LAST_TIME_STRING
		 */
		public long getLastTime(int bin,boolean useWindowAmplifier,String lowerFilter,String upperFilter)
		{
			long time;

			try
			{
				time = properties.getLong(LIST_KEY_STRING+LIST_KEY_LAST_TIME_STRING+bin+"."+
							  useWindowAmplifier+"."+lowerFilter+"."+upperFilter);
			}
			catch(NGATPropertyException e)/* assume failure due to key not existing */
			{
				time = 0;
			}
			return time;
		}

		/**
		 * Method to set the last time a calibration with these attributes was done.
		 * The time is set to now. The property file should be saved after a call to this method is made.
		 * @param bin The binning factor used for this calibration.
		 * @param useWindowAmplifier Whether we are using the default amplifier (false) or the amplifier
		 *        used for windowing (true).
		 * @param lowerFilter The lower filter type string used for this calibration.
		 * @param upperFilter The lower filter type string used for this calibration.
		 * @see #LIST_KEY_STRING
		 * @see #LIST_KEY_LAST_TIME_STRING
		 */
		public void setLastTime(int bin,boolean useWindowAmplifier,String lowerFilter,String upperFilter)
		{
			long now;

			now = System.currentTimeMillis();
			properties.setProperty(LIST_KEY_STRING+LIST_KEY_LAST_TIME_STRING+bin+"."+
					       useWindowAmplifier+"."+lowerFilter+"."+upperFilter,new String(""+now));
		}
	}// end class TWILIGHT_CALIBRATESavedState

	/**
	 * Private inner class that stores data pertaining to one possible calibration run that can take place during
	 * a TWILIGHT_CALIBRATE command invocation.
	 */
	private class TWILIGHT_CALIBRATECalibration
	{
		/**
		 * What binning to configure the ccd to for this calibration.
		 */
		protected int bin;
		/**
		 * Which amplifier should we use to readout.
		 * If false, we use the default amplifier.
		 * If true, we use the window amplifier.
		 */
		protected boolean useWindowAmplifier;
		/**
		 * The filter to use in the lower (zero) wheel.
		 */
		protected String lowerFilter = null;
		/**
		 * The filter to use in the upper (one) wheel.
		 */
		protected String upperFilter = null;
		/**
		 * How often we should perform the calibration in milliseconds.
		 */
		protected long frequency;
		/**
		 * How sensitive is the filter (combination) to twilight daylight,
		 * as compared to no filters (1.0). A double between zero and one.
		 */
		protected double filterSensitivity = 0.0;
		/**
		 * The last time this calibration was performed. This is retrieved from the saved state,
		 * not from the calibration list.
		 */
		protected long lastTime;
		
		/**
		 * Constructor.
		 */
		public TWILIGHT_CALIBRATECalibration()
		{
			super();
		}

		/**
		 * Method to set the binning configuration for this calibration.
		 * @param b The binning to use. This should be greater than 0 and less than 5.
		 * @exception IllegalArgumentException Thrown if parameter b is out of range.
		 * @see #bin
		 */
		public void setBin(int b) throws IllegalArgumentException
		{
			if((b < 1)||(b > 4))
			{
				throw new IllegalArgumentException(this.getClass().getName()+":setBin failed:"+
					b+" not a legal binning value.");
			}
			bin = b;
		}

		/**
		 * Method to get the binning configuration for this calibration.
		 * @return The binning.
		 * @see #bin
		 */
		public int getBin()
		{
			return bin;
		}

		/**
		 * Method to set whether we are using the default amplifier or the one used for windowing.
		 * @param b Set to true to use the windowing amplifier, false to use the default amplifier.
		 * @see #useWindowAmplifier
		 */
		public void setUseWindowAmplifier(boolean b)
		{
			useWindowAmplifier = b;
		}

		/**
		 * Method to get whether we are using the default amplifier or the one used for windowing.
		 * @return Returns true if we are using the windowing amplifier, 
		 *         false if we are using the default amplifier.
		 * @see #useWindowAmplifier
		 */
		public boolean useWindowAmplifier()
		{
			return useWindowAmplifier;
		}

		/**
		 * Method to set the lower filter type name.
	 	 * @param s The name to use.
		 * @exception NullPointerException Thrown if the filter string was null.
		 */
		public void setLowerFilter(String s) throws NullPointerException
		{
			if(s == null)
			{
				throw new NullPointerException(this.getClass().getName()+
						":setLowerFilter:Lower filter was null.");
			}
			lowerFilter = s;
		}

		/**
		 * Method to return the lower filter type for this calibration.
		 * @return A string containing the lower filter string.
		 */
		public String getLowerFilter()
		{
			return lowerFilter;
		}

		/**
		 * Method to set the upper filter type name.
	 	 * @param s The name to use.
		 * @exception NullPointerException Thrown if the filter string was null.
		 */
		public void setUpperFilter(String s) throws NullPointerException
		{
			if(s == null)
			{
				throw new NullPointerException(this.getClass().getName()+
						":setUpperFilter:Upper filter was null.");
			}
			upperFilter = s;
		}

		/**
		 * Method to return the upper filter type for this calibration.
		 * @return A string containing the upper filter string.
		 */
		public String getUpperFilter()
		{
			return upperFilter;
		}

		/**
		 * Method to set the frequency this calibration should be performed.
		 * @param f The frequency in milliseconds. This should be greater than zero.
		 * @exception IllegalArgumentException Thrown if parameter f is out of range.
		 * @see #frequency
		 */
		public void setFrequency(long f) throws IllegalArgumentException
		{
			if(f <= 0)
			{
				throw new IllegalArgumentException(this.getClass().getName()+":setFrequency failed:"+
					f+" not a legal frequency.");
			}
			frequency = f;
		}

		/**
		 * Method to get the frequency configuration for this calibration.
		 * @return The frequency this calibration should be performed, in milliseconds.
		 * @see #frequency
		 */
		public long getFrequency()
		{
			return frequency;
		}

		/**
		 * Method to set the relative filter sensitivity of the filters at twilight in this calibration.
		 * @param d The relative filter sensitivity, compared to no filters. 
		 * 	This should be greater than zero and less than 1.0 (inclusive).
		 * @exception IllegalArgumentException Thrown if parameter d is out of range.
		 * @see #filterSensitivity
		 */
		public void setFilterSensitivity(double d) throws IllegalArgumentException
		{
			if((d < 0.0)||(d > 1.0))
			{
				throw new IllegalArgumentException(this.getClass().getName()+
					":setFilterSensitivity failed:"+d+" not a legal relative sensitivity.");
			}
			filterSensitivity = d;
		}

		/**
		 * Method to get the relative filter sensitivity of the filters at twilight for this calibration.
		 * @return The relative filter sensitivity of the filters, between 0.0 and 1.0, where 1.0 is
		 * 	the sensitivity no filters have.
		 * @see #filterSensitivity
		 */
		public double getFilterSensitivity()
		{
			return filterSensitivity;
		}

		/**
		 * Method to set the last time this calibration was performed.
		 * @param t A long representing the last time the calibration was done, as a 
		 * 	number of milliseconds since the EPOCH.
		 * @exception IllegalArgumentException Thrown if parameter f is out of range.
		 * @see #frequency
		 */
		public void setLastTime(long t) throws IllegalArgumentException
		{
			if(t < 0)
			{
				throw new IllegalArgumentException(this.getClass().getName()+":setLastTime failed:"+
					t+" not a legal last time.");
			}
			lastTime = t;
		}

		/**
		 * Method to get the last time this calibration was performed.
		 * @return The number of milliseconds since the epoch that this calibration was last performed.
		 * @see #frequency
		 */
		public long getLastTime()
		{
			return lastTime;
		}
	}// end class TWILIGHT_CALIBRATECalibration

	/**
	 * Private inner class that stores data pertaining to one telescope offset.
	 */
	private class TWILIGHT_CALIBRATEOffset
	{
		/**
		 * The offset in RA, in arcseconds.
		 */
		protected float raOffset;
		/**
		 * The offset in DEC, in arcseconds.
		 */
		protected float decOffset;
		
		/**
		 * Constructor.
		 */
		public TWILIGHT_CALIBRATEOffset()
		{
			super();
		}

		/**
		 * Method to set the offset in RA.
		 * @param o The offset in RA, in arcseconds, to use. This parameter must be in the range
		 * 	[-3600..3600] arcseconds.
		 * @exception IllegalArgumentException Thrown if parameter o is out of range.
		 * @see #raOffset
		 */
		public void setRAOffset(float o) throws IllegalArgumentException
		{
			if((o < -3600)||(o > 3600))
			{
				throw new IllegalArgumentException(this.getClass().getName()+":setRAOffset failed:"+
					o+" out of range.");
			}
			raOffset = o;
		}

		/**
		 * Method to get the offset in RA.
		 * @return The offset, in arcseconds.
		 * @see #raOffset
		 */
		public float getRAOffset()
		{
			return raOffset;
		}

		/**
		 * Method to set the offset in DEC.
		 * @param o The offset in DEC, in arcseconds, to use. This parameter must be in the range
		 * 	[-3600..3600] arcseconds.
		 * @exception IllegalArgumentException Thrown if parameter o is out of range.
		 * @see #decOffset
		 */
		public void setDECOffset(float o) throws IllegalArgumentException
		{
			if((o < -3600)||(o > 3600))
			{
				throw new IllegalArgumentException(this.getClass().getName()+":setDECOffset failed:"+
					o+" out of range.");
			}
			decOffset = o;
		}

		/**
		 * Method to get the offset in DEC.
		 * @return The offset, in arcseconds.
		 * @see #decOffset
		 */
		public float getDECOffset()
		{
			return decOffset;
		}
	}// end TWILIGHT_CALIBRATEOffset

}

//
// $Log: not supported by cvs2svn $
// Revision 1.3  2010/03/26 14:38:29  cjm
// Changed from bitwise to absolute logging levels.
//
// Revision 1.2  2010/01/14 16:12:49  cjm
// Added PROGID FITS header setting.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 1.2  2008/11/28 10:27:29  wasp
// Update preformed months ago, yet seems to be working ok, thus committing to CVS.
//
// Revision 1.1.1.1  2008/03/11 13:36:56  wasp
// Start
//
// Revision 1.14  2007/06/19 16:38:54  cjm
// Added default focus offset code.
//
// Revision 1.13  2006/05/16 14:26:10  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.12  2005/06/30 15:39:29  cjm
// Changed algorithm. When calculated exposure length exceed max but it is dawn, retry at maximum
// until the sky is bright enough.
// When calculated exposure length is shorter than minimum but it is dusk, retry at minimum
// until the sky is dark enough.
//
// Revision 1.11  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 1.10  2005/03/03 18:03:25  cjm
// Updated log messages.
// Updated comments.
//
// Revision 1.9  2005/03/03 15:42:27  cjm
// Exposure length tweaks in doFrame.
// Always re-calculate exposure length to ideal.
// If the recalculated exposure length is out of range, but the last
// exposure length was not at a range limit of it was at the range limit but the frame was OK,
// retry with the exposure length at the frame limit.
//
// Revision 1.8  2005/01/13 16:14:08  cjm
// Changed calculation of new exposure time by
// scaling current mean counts to best mean counts.
//
// Revision 1.7  2003/06/06 12:47:15  cjm
// Changes relating to using the windowing amplifier during day calibration.
//
// Revision 1.6  2002/12/16 17:00:27  cjm
// Check status property to disable filter wheel calls.
//
// Revision 1.5  2002/11/26 18:56:05  cjm
// Now calls makeMasterFlat at end of calibration.
//
// Revision 1.4  2001/09/24 19:33:21  cjm
// Fixed documentation errors.
// Added better frame state logging.
// Multiplication factors now got by time of night.
//
// Revision 1.3  2001/09/13 11:52:34  cjm
// Added code to delete old temporary file to stop saveFitsHeaders failing.
//
// Revision 1.2  2001/09/13 10:37:25  cjm
// Initial complete implementation.
//
// Revision 1.1  2001/09/03 10:01:16  cjm
// Initial revision
//
// Revision 1.1  2001/08/24 16:45:15  cjm
// Initial revision
//
//
