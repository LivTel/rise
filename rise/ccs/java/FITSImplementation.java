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
// FITSImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/FITSImplementation.java,v 1.3 2010-03-26 14:38:29 cjm Exp $

import java.lang.*;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class provides the generic implementation of commands that write FITS files. It extends those that
 * use the SDSU CCD Library as this is needed to generate FITS files.
 * @see CCDLibraryImplementation
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class FITSImplementation extends CCDLibraryImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: FITSImplementation.java,v 1.3 2010-03-26 14:38:29 cjm Exp $");
	/**
	 * A reference to the CcsStatus class instance that holds status information for the Ccs.
	 */
	protected CcsStatus status = null;
	/**
	 * A reference to the FitsFilename class instance used to generate unique FITS filenames.
	 */
	protected FitsFilename ccsFilename = null;
	/**
	 * A local reference to the FitsHeader object held in Ccs. This is used for writing FITS headers to disk
	 * and setting the values of card images within the headers.
	 */
	/*protected FitsHeader ccsFitsHeader = null; */
	public FitsHeader ccsFitsHeader = null; 
	/**
	 * A local reference to the FitsHeaderDefaults object held in the Ccs. This is used to supply default values, 
	 * units and comments for FITS header card images.
	 */
	protected FitsHeaderDefaults ccsFitsHeaderDefaults = null;
	/**
	 * Internal constant used when converting temperatures in centigrade (from the CCD controller/CCS
	 * configuration file) to Kelvin (used in FITS file). Used in setFitsHeaders.
	 * @see #setFitsHeaders
	 */
	private final static double CENTIGRADE_TO_KELVIN = 273.15;
	/**
	 * Internal constant used when the order number offset defined in the property
	 * 'ccs.get_fits.order_number_offset' is not found or is not a valid number.
	 * @see #getFitsHeadersFromISS
	 */
	private final static int DEFAULT_ORDER_NUMBER_OFFSET = 255;

	/**
	 * This method calls the super-classes method, and tries to fill in the reference to the
	 * FITS filename object, the FITS header object and the FITS default value object.
	 * @param command The command to be implemented.
	 * @see #status
	 * @see Ccs#getStatus
	 * @see #ccsFilename
	 * @see Ccs#getFitsFilename
	 * @see #ccsFitsHeader
	 * @see Ccs#getFitsHeader
	 * @see #ccsFitsHeaderDefaults
	 * @see Ccs#getFitsHeaderDefaults
	 */
	public void init(COMMAND command)
	{
		super.init(command);
		if(ccs != null)
		{
			status = ccs.getStatus();
			ccsFilename = ccs.getFitsFilename();
			ccsFitsHeader = ccs.getFitsHeader();
			ccsFitsHeaderDefaults = ccs.getFitsHeaderDefaults();
		}
	}

	/**
	 * This method is used to calculate how long an implementation of a command is going to take, so that the
	 * client has an idea of how long to wait before it can assume the server has died.
	 * @param command The command to be implemented.
	 * @return The time taken to implement this command, or the time taken before the next acknowledgement
	 * is to be sent.
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		return super.calculateAcknowledgeTime(command);
	}

	/**
	 * This routine performs the generic command implementation.
	 * @param command The command to be implemented.
	 * @return The results of the implementation of this command.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		return super.processCommand(command);
	}

	/**
	 * This routine tries to move the mirror fold to a certain location, by issuing a MOVE_FOLD command
	 * to the ISS. The position to move the fold to is specified by the ccs property file.
	 * If an error occurs the done objects field's are set accordingly.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see CcsStatus#getPropertyInteger
	 * @see Ccs#sendISSCommand
	 */
	public boolean moveFold(COMMAND command,COMMAND_DONE done)
	{
		INST_TO_ISS_DONE instToISSDone = null;
		MOVE_FOLD moveFold = null;
		int mirrorFoldPosition = 0;

		moveFold = new MOVE_FOLD(command.getId());
		try
		{
			mirrorFoldPosition = status.getPropertyInteger("ccs.mirror_fold_position");
		}
		catch(NumberFormatException e)
		{
			mirrorFoldPosition = 0;
			ccs.error(this.getClass().getName()+":moveFold:"+
				command.getClass().getName(),e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+300);
			done.setErrorString("moveFold:"+e);
			done.setSuccessful(false);
			return false;
		}
		moveFold.setMirror_position(mirrorFoldPosition);
		instToISSDone = ccs.sendISSCommand(moveFold,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":moveFold:"+
				command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+301);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);		
			return false;
		}
		return true;
	}

	/**
	 * This routine tries to start the autoguider, by issuing a AG_START command
	 * to the ISS.
	 * If an error occurs the done objects field's can be set accordingly.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendISSCommand
	 */
	public boolean autoguiderStart(COMMAND command,COMMAND_DONE done)
	{
		INST_TO_ISS_DONE instToISSDone = null;

		instToISSDone = ccs.sendISSCommand(new AG_START(command.getId()),serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":autoguiderStart:"+
				command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+302);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);		
			return false;
		}
		return true;
	}

	/**
	 * This routine tries to stop the autoguider, by issuing a AG_STOP command
	 * to the ISS.
	 * If an error occurs the done objects field's is set accordingly.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param checkAbort Whether sendISSCommand should check the thread's abort flag. This should be
	 * 	true for normal operation, and false when autoguiderStop is being used in response
	 * 	to a previously trapped abort (i.e. an exposure has been aborted).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendISSCommand
	 */
	public boolean autoguiderStop(COMMAND command,COMMAND_DONE done,boolean checkAbort)
	{
		INST_TO_ISS_DONE instToISSDone = null;

		instToISSDone = ccs.sendISSCommand(new AG_STOP(command.getId()),serverConnectionThread,checkAbort);
		if(instToISSDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":autoguiderStop:"+
				command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+303);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);		
			return false;
		}
		return true;
	}

	/**
	 * This routine clears the current set of FITS headers. The FITS headers are held in the main Ccs
	 * object. This is retrieved and the relevant method called.
	 * @see #ccsFitsHeader
	 * @see ngat.fits.FitsHeader#clearKeywordValueList
	 */
	public void clearFitsHeaders()
	{
		ccsFitsHeader.clearKeywordValueList();
	}

	/**
	 * This routine sets up the Fits Header objects with some keyword value pairs.
	 * This routine calls the more complicated one below, with exposureCount set to 1.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param obsTypeString The type of image taken by the camera. This string should be
	 * 	one of the OBSTYPE_VALUE_* defaults in ngat.fits.FitsHeaderDefaults.
	 * @param exposureTime The exposure time,in milliseconds, to put in the EXPTIME keyword. It
	 * 	is converted into decimal seconds (a double).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #setFitsHeaders(COMMAND,COMMAND_DONE,String,int,int)
	 */
	public boolean setFitsHeaders(COMMAND command,COMMAND_DONE done,String obsTypeString,int exposureTime)
	{
		return setFitsHeaders(command,done,obsTypeString,exposureTime,1);
	}

	/**
	 * This routine sets up the Fits Header objects with some keyword value pairs.
	 * <p>The following mandatory keywords are filled in: SIMPLE,BITPIX,NAXIS,NAXIS1,NAXIS2. Note NAXIS1 and
	 * NAXIS2 are retrieved from libccd, assuming the library has previously been setup with a 
	 * configuration.</p>
	 * <p> A complete list of keywords is constructed from the Ccs FITS defaults file. Some of the values of
	 * these keywords are overwritten by real data obtained from the camera controller, or internal Ccs status.
	 * These are:
	 * OBSTYPE, RUNNUM, EXPNUM, EXPTOTAL, DATE, DATE-OBS, UTSTART, MJD, EXPTIME, 
	 * FILTER1, FILTERI1, FILTER2, FILTERI2, CONFIGID, CONFNAME, 
	 * PRESCAN, POSTSCAN, CCDXIMSI, CCDYIMSI, CCDSCALE, CCDRDOUT,
	 * CCDXBIN, CCDYBIN, CCDSTEMP, CCDATEMP, CCDWMODE,
	 * CCDWXOFF, CCDWYOFF, CCDWXSIZ, CCDWYSIZ, CALBEFOR, CALAFTER, ROTCENTX, ROTCENTY.
	 * Note the DATE, DATE-OBS, UTSTART and MJD keywords are given the value of the current
	 * system time, this value is updated to the exposure start time when the image has been exposed. </p>
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param obsTypeString The type of image taken by the camera. This string should be
	 * 	one of the OBSTYPE_VALUE_* defaults in ngat.fits.FitsHeaderDefaults.
	 * @param exposureTime The exposure time,in milliseconds, to put in the EXPTIME keyword. It
	 * 	is converted into decimal seconds (a double).
	 * @param exposureCount The number of exposures in the current MULTRUN, to put into the EXPTOTAL keyword.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #getCCDRDOUTValue
	 * @see #ccsFitsHeader
	 * @see #ccsFitsHeaderDefaults
	 * @see #CENTIGRADE_TO_KELVIN
	 * @see CCDLibraryImplementation#libccd
	 * @see CcsStatus#getNumberColumns
	 * @see CcsStatus#getNumberRows
	 */
	public boolean setFitsHeaders(COMMAND command,COMMAND_DONE done,String obsTypeString,
				      int exposureTime,int exposureCount)
	{
		CCDLibraryDouble actualTemperature = null;
		CCDLibrarySetupWindow window = null;
		FitsHeaderCardImage cardImage = null;
		Date date = null;
		String filterWheel1String = null;
		String filterWheel2String = null;
		String filterWheel1IdString = null;
		String filterWheel2IdString = null;
		Vector defaultFitsHeaderList = null;
		int filterWheelPosition;
		double doubleValue = 0.0;
		int windowFlags;
		int windowNumber,xbin,ybin;
		boolean filterWheelEnable;
		int preScan,postScan;

		ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+":setFitsHeaders:Started.");
		try
		{
			filterWheelEnable = status.getPropertyBoolean("ccs.config.filter_wheel.enable");
			if(filterWheelEnable)
			{
			// lower filter wheel, type name
				filterWheelPosition = libccd.CCDFilterWheelGetPosition(0);
				filterWheel1String = status.getFilterTypeName(0,filterWheelPosition);
			// lower filter wheel, filter id
				filterWheel1IdString = status.getFilterIdName(filterWheel1String);
			// upper filter wheel, type name
				filterWheelPosition = libccd.CCDFilterWheelGetPosition(1);
				filterWheel2String = status.getFilterTypeName(1,filterWheelPosition);
			// upper filter wheel, filter id
				filterWheel2IdString = status.getFilterIdName(filterWheel2String);
			}
			else
			{
				filterWheel1String = new String("UNKNOWN");
				filterWheel1IdString = new String("UNKNOWN");
				filterWheel2String = new String("UNKNOWN");
				filterWheel2IdString = new String("UNKNOWN");
			}
		}
		// ngat.rise.ccd.CCDNativeException thrown by libccd.CCDFilterWheelGetPosition
		// IllegalArgumentException thrown by CcsStatus.getFilterWheelName
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Headers failed:");
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+309);
			done.setErrorString(s+e);
			done.setSuccessful(false);
			return false;
		}
		try
		{
		// load all the FITS header defaults and put them into the ccsFitsHeader object
			defaultFitsHeaderList = ccsFitsHeaderDefaults.getCardImageList();
			ccsFitsHeader.addKeywordValueList(defaultFitsHeaderList,0);

		// get current binning for later
			xbin = libccd.CCDSetupGetNSBin();
			ybin = libccd.CCDSetupGetNPBin();
		// if the binning values are < 1, or are not the same in both axes a problem
		// has occured
		// note xbin can be zero if CCD setup not performed.
		// this will eventually cause this command to fail, but we don't want it to fail
		// with a 'division by zero' error here.
			if((xbin < 1)||(ybin < 1)||(xbin != ybin))
			{
				String s = new String("Command "+command.getClass().getName()+
					":Setting Fits Headers failed:Illegal binning values:X Bin:"+xbin+
					":Y Bin:"+ybin);
				ccs.error(s);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+308);
				done.setErrorString(s);
				done.setSuccessful(false);
				return false;
			}
		// NAXIS1
			cardImage = ccsFitsHeader.get("NAXIS1");
			cardImage.setValue(new Integer(libccd.CCDSetupGetNCols()));
		// NAXIS2
			cardImage = ccsFitsHeader.get("NAXIS2");
			cardImage.setValue(new Integer(libccd.CCDSetupGetNRows()));
		// OBSTYPE
			cardImage = ccsFitsHeader.get("OBSTYPE");
			cardImage.setValue(obsTypeString);
		// The current MULTRUN number and runNumber are used for these keywords at the moment.
		// They are updated in saveFitsHeaders, when the retrieved values are more likely 
		// to be correct.
		// RUNNUM
			cardImage = ccsFitsHeader.get("RUNNUM");
			cardImage.setValue(new Integer(ccsFilename.getMultRunNumber()));
		// EXPNUM
			cardImage = ccsFitsHeader.get("EXPNUM");
			cardImage.setValue(new Integer(ccsFilename.getRunNumber()));
		// EXPTOTAL
			cardImage = ccsFitsHeader.get("EXPTOTAL");
			cardImage.setValue(new Integer(exposureCount));
		// The DATE,DATE-OBS and UTSTART keywords are saved using the current date/time.
		// This is updated when the data is saved if CFITSIO is used.
			date = new Date();
		// DATE
			cardImage = ccsFitsHeader.get("DATE");
			cardImage.setValue(date);
		// DATE-OBS
			cardImage = ccsFitsHeader.get("DATE-OBS");
			cardImage.setValue(date);
		// UTSTART
			cardImage = ccsFitsHeader.get("UTSTART");
			cardImage.setValue(date);
		// MJD
			cardImage = ccsFitsHeader.get("MJD");
			cardImage.setValue(date);
		// EXPTIME
			cardImage = ccsFitsHeader.get("EXPTIME");
			cardImage.setValue(new Double(((double)exposureTime)/1000.0));
		// FILTER1
			cardImage = ccsFitsHeader.get("FILTER1");
			cardImage.setValue(filterWheel1String);
		// FILTERI1
			cardImage = ccsFitsHeader.get("FILTERI1");
			cardImage.setValue(filterWheel1IdString);
		// FILTER2
			cardImage = ccsFitsHeader.get("FILTER2");
			cardImage.setValue(filterWheel2String);
		// FILTERI2
			cardImage = ccsFitsHeader.get("FILTERI2");
			cardImage.setValue(filterWheel2IdString);
		// CONFIGID
			cardImage = ccsFitsHeader.get("CONFIGID");
			cardImage.setValue(new Integer(status.getConfigId()));
		// CONFNAME
			cardImage = ccsFitsHeader.get("CONFNAME");
			cardImage.setValue(status.getConfigName());
		// note xbin can be zero if CCD setup not performed.
		// this will eventually cause this command to fail, but we don't want it to fail
		// with a 'division by zero' error here.
		// PRESCAN
			cardImage = ccsFitsHeader.get("PRESCAN");
			preScan = ccsFitsHeaderDefaults.getValueInteger("PRESCAN."+status.getNumberColumns(xbin)+"."+
									getCCDRDOUTValue()+"."+xbin);
			cardImage.setValue(new Integer(preScan));
		// POSTSCAN
			cardImage = ccsFitsHeader.get("POSTSCAN");
			postScan = ccsFitsHeaderDefaults.getValueInteger("POSTSCAN."+status.getNumberColumns(xbin)+"."+
									 getCCDRDOUTValue()+"."+xbin);
			cardImage.setValue(new Integer(postScan));
		// CCDXIMSI
			cardImage = ccsFitsHeader.get("CCDXIMSI");
			cardImage.setValue(new Integer(ccsFitsHeaderDefaults.getValueInteger("CCDXIMSI")/xbin));
		// CCDYIMSI
			cardImage = ccsFitsHeader.get("CCDYIMSI");
			cardImage.setValue(new Integer(ccsFitsHeaderDefaults.getValueInteger("CCDYIMSI")/ybin));
		// CCDSCALE
			cardImage = ccsFitsHeader.get("CCDSCALE");
			// note this next line assumes xbin == ybin e.g. CCDSCALE is constant in both axes
			cardImage.setValue(new Double(ccsFitsHeaderDefaults.getValueDouble("CCDSCALE")*xbin));
		// CCDRDOUT
			cardImage = ccsFitsHeader.get("CCDRDOUT");
			cardImage.setValue(getCCDRDOUTValue());
		// CCDXBIN
			cardImage = ccsFitsHeader.get("CCDXBIN");
			cardImage.setValue(new Integer(xbin));
		// CCDYBIN
			cardImage = ccsFitsHeader.get("CCDYBIN");
			cardImage.setValue(new Integer(ybin));
		// CCDSTEMP
			doubleValue = status.getPropertyDouble("ccs.config.target_temperature")+
				CENTIGRADE_TO_KELVIN;
			cardImage = ccsFitsHeader.get("CCDSTEMP");
			cardImage.setValue(new Integer((int)doubleValue));
		// CCDATEMP
			actualTemperature = new CCDLibraryDouble();
			libccd.CCDTemperatureGet(actualTemperature);
			cardImage = ccsFitsHeader.get("CCDATEMP");
			cardImage.setValue(new Integer((int)(actualTemperature.getValue()+CENTIGRADE_TO_KELVIN)));
		// windowing keywords
		// CCDWMODE
			windowFlags = libccd.CCDSetupGetWindowFlags();
			cardImage = ccsFitsHeader.get("CCDWMODE");
			cardImage.setValue(new Boolean((boolean)(windowFlags>0)));
		// create a window based on window Flags
		// They are updated in saveFitsHeaders, when the retrieved values are for the correct window.
		// diddly always chooses window number 0 at the moment
			if(windowFlags > 0)
			{
				windowNumber=0;
				window = libccd.CCDSetupGetWindow(windowNumber);
				ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					":setFitsHeaders:Using window "+windowNumber+" : "+window+".");
			}
			else
			{
				ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					":setFitsHeaders:Default window X size = "+
					ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")+" / "+xbin+" = "+
					(ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")/xbin)+".");
				ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					":setFitsHeaders:Default window Y size = "+
					ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")+" / "+ybin+" = "+
					(ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")/ybin)+".");
				window = new CCDLibrarySetupWindow(0,0,
				       ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")/xbin,
				       ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")/ybin);
				ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					":setFitsHeaders:Using default window : "+window+".");
			}
		// CCDWXOFF
			cardImage = ccsFitsHeader.get("CCDWXOFF");
			cardImage.setValue(new Integer(window.getXStart()));
		// CCDWYOFF
			cardImage = ccsFitsHeader.get("CCDWYOFF");
			cardImage.setValue(new Integer(window.getYStart()));
		// CCDWXSIZ
			cardImage = ccsFitsHeader.get("CCDWXSIZ");
			ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:CCDWXSIZ = xend "+window.getXEnd()+" - xstart "+window.getXStart()+
				" = "+(window.getXEnd()-window.getXStart())+".");
			cardImage.setValue(new Integer(window.getXEnd()-window.getXStart()));
		// CCDWYSIZ
			cardImage = ccsFitsHeader.get("CCDWYSIZ");
			ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:CCDWYSIZ = yend "+window.getYEnd()+" - ystart "+window.getYStart()+
				" = "+(window.getYEnd()-window.getYStart())+".");
			cardImage.setValue(new Integer(window.getYEnd()-window.getYStart()));
		// CALBEFOR
			cardImage = ccsFitsHeader.get("CALBEFOR");
			cardImage.setValue(new Boolean(status.getCachedCalibrateBefore()));
		// CALAFTER
			cardImage = ccsFitsHeader.get("CALAFTER");
			cardImage.setValue(new Boolean(status.getCachedCalibrateAfter()));
		// ROTCENTX
		// Value specified in config file is unbinned without bias offsets added
			cardImage = ccsFitsHeader.get("ROTCENTX");
			cardImage.setValue(new Integer((ccsFitsHeaderDefaults.getValueInteger("ROTCENTX")/xbin)+
					   preScan));
		// ROTCENTY
		// Value specified in config file is unbinned 
			cardImage = ccsFitsHeader.get("ROTCENTY");
			cardImage.setValue(new Integer(ccsFitsHeaderDefaults.getValueInteger("ROTCENTY")/ybin));
		}// end try
		// ngat.fits.FitsHeaderException thrown by ccsFitsHeaderDefaults.getValue
		// ngat.util.FileUtilitiesNativeException thrown by CcsStatus.getConfigId
		// ngat.rise.ccd.CCDNativeException thrown by libccd.CCDFilterWheelGetPosition
		// IllegalArgumentException thrown by CcsStatus.getFilterWheelName/getCCDRDOUTValue
		// NumberFormatException thrown by CcsStatus.getFilterWheelName/CcsStatus.getConfigId
		// Exception thrown by CcsStatus.getConfigId
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Headers failed:");
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+304);
			done.setErrorString(s+e);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * This routine tries to get a set of FITS headers for an exposure, by issuing a GET_FITS command
	 * to the ISS. The results from this command are put into the Ccs's FITS header object.
	 * If an error occurs the done objects field's can be set to record the error.
	 * The order numbers returned from the ISS are incremented by the order number offset
	 * defined in the Ccs 'ccs.get_fits.order_number_offset' property.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendISSCommand
	 * @see Ccs#getStatus
	 * @see CcsStatus#getPropertyInteger
	 * @see #ccsFitsHeader
	 * @see #DEFAULT_ORDER_NUMBER_OFFSET
	 */
	public boolean getFitsHeadersFromISS(COMMAND command,COMMAND_DONE done)
	{
		INST_TO_ISS_DONE instToISSDone = null;
		GET_FITS_DONE getFitsDone = null;
		int orderNumberOffset;

		instToISSDone = ccs.sendISSCommand(new GET_FITS(command.getId()),serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":getFitsHeadersFromISS:"+
				command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+305);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
	// Get the returned FITS header information into the FitsHeader object.
		getFitsDone = (GET_FITS_DONE)instToISSDone;
	// get the order number offset
		try
		{
			orderNumberOffset = status.getPropertyInteger("ccs.get_fits.order_number_offset");
		}
		catch(NumberFormatException e)
		{
			orderNumberOffset = DEFAULT_ORDER_NUMBER_OFFSET;
			ccs.error(this.getClass().getName()+
				":getFitsHeadersFromISS:Getting order number offset failed.",e);
		}
		ccsFitsHeader.addKeywordValueList(getFitsDone.getFitsHeader(),orderNumberOffset);
		return true;
	}

	/**
	 * This routine uses the Fits Header object, stored in the ccs object, to save the headers to disc.
	 * A lock file is created before the FITS header is written, this allows synchronisation with the
	 * data transfer software. This will need deleting after the image data has been saved to the FITS header.
	 * This method also updates the RUNNUM and EXPNUM keywords with the current multRun and runNumber values
	 * in the ccsFilename object, as they must be correct when the file is saved.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param filename The filename to save the headers to.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #ccsFitsHeader
	 * @see #ccsFilename
	 * @see ngat.fits.FitsFilename#getMultRunNumber
	 * @see ngat.fits.FitsFilename#getRunNumber
	 * @see ngat.util.LockFile2
	 */
	public boolean saveFitsHeaders(COMMAND command,COMMAND_DONE done,String filename)
	{
		LockFile2 lockFile = null;

		try
		{
			ccsFitsHeader.add("RUNNUM",new Integer(ccsFilename.getMultRunNumber()),
				ccsFitsHeaderDefaults.getComment("RUNNUM"),
				ccsFitsHeaderDefaults.getUnits("RUNNUM"),
				ccsFitsHeaderDefaults.getOrderNumber("RUNNUM"));
			ccsFitsHeader.add("EXPNUM",new Integer(ccsFilename.getRunNumber()),
				ccsFitsHeaderDefaults.getComment("EXPNUM"),
				ccsFitsHeaderDefaults.getUnits("EXPNUM"),
				ccsFitsHeaderDefaults.getOrderNumber("EXPNUM"));
		}
		// FitsHeaderException thrown by ccsFitsHeaderDefaults.getValue
		// IllegalAccessException thrown by ccsFitsHeaderDefaults.getValue
		// InvocationTargetException thrown by ccsFitsHeaderDefaults.getValue
		// NoSuchMethodException thrown by ccsFitsHeaderDefaults.getValue
		// InstantiationException thrown by ccsFitsHeaderDefaults.getValue
		// ClassNotFoundException thrown by ccsFitsHeaderDefaults.getValue
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Headers in saveFitsHeaders failed:"+e);
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+306);
			done.setErrorString(s);
			done.setSuccessful(false);
			return false;
		}
		// create lock file
		try
		{
			lockFile = new LockFile2(filename);
			lockFile.lock();
		}
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
					":saveFitsHeaders:Creating lock file failed for file:"+filename+":"+e);
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+313);
			done.setErrorString(s);
			done.setSuccessful(false);
			return false;			
		}
		// write FITS header to FITS filename
		try
		{
			ccsFitsHeader.writeFitsHeader(filename);
		}
		catch(FitsHeaderException e)
		{
			String s = new String("Command "+command.getClass().getName()+
					":Saving Fits Headers failed for file:"+filename+":"+e);
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+307);
			done.setErrorString(s);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * This method tries to unlock the FITS filename.
	 * It first checks the lock file exists and only attempts to unlock locked files. This is because
	 * this method can be called after a partial failure, where the specified FITS file may or may not have been
	 * locked.
	 * @param command The command being implemented. This is used for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param filename The FITS filename which should have an associated lock file.
	 * @return true if the method succeeds, false if a failure occurs.
	 * @see ngat.util.LockFile2
	 */
	public boolean unLockFile(COMMAND command,COMMAND_DONE done,String filename)
	{
		LockFile2 lockFile = null;

		try
		{
			lockFile = new LockFile2(filename);
			if(lockFile.isLocked())
				lockFile.unLock();
		}
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
					      ":unLockFile:Unlocking lock file failed for file:"+filename+":"+e);
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+314);
			done.setErrorString(s);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * This routine uses the Fits Header object, stored in the ccs object, to save the headers to disc.
	 * A lock file is created before the FITS header is written, this allows synchronisation with the
	 * data transfer software.
	 * This method also updates the RUNNUM and EXPNUM keywords with the current multRun and runNumber values
	 * in the ccsFilename object, as they must be correct when the file is saved. 
	 * A list of windows are defined from the setup's window flags, and a set of headers
	 * are saved to a window specific filename for each window defined.
	 * It changess the CCDWXOFF, CCDWYOFF, CCDWXSIZ and CCDWYSIZ keywords for each window defined. 
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param filenameList An instance of a list. The filename's saved containing FITS headers are added
	 *        to the list.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #ccsFitsHeader
	 * @see #ccsFilename
	 * @see ngat.fits.FitsFilename#getMultRunNumber
	 * @see ngat.fits.FitsFilename#getRunNumber
	 */
	public boolean saveFitsHeaders(COMMAND command,COMMAND_DONE done,List filenameList)
	{
		FitsHeaderCardImage cardImage = null;
		CCDLibrarySetupWindow window = null;
		LockFile2 lockFile = null;
		List windowIndexList = null;
		String filename = null;
		int windowIndex,windowFlags,ncols,nrows,xbin,ybin;

		windowFlags = libccd.CCDSetupGetWindowFlags();
		windowIndexList = new Vector();
		if(windowFlags > 0)
		{
			// if the relevant bit is set, add an Integer with the appopriate
			// window index to the windowIndexList. The window index is one less than
			// the window number.
			if((windowFlags&CCDLibrary.CCD_SETUP_WINDOW_ONE) > 0)
			{
				windowIndexList.add(new Integer(0));
			}
			if((windowFlags&CCDLibrary.CCD_SETUP_WINDOW_TWO) > 0)
			{
				windowIndexList.add(new Integer(1));
			}
			if((windowFlags&CCDLibrary.CCD_SETUP_WINDOW_THREE) > 0)
			{
				windowIndexList.add(new Integer(2));
			}
			if((windowFlags&CCDLibrary.CCD_SETUP_WINDOW_FOUR) > 0)
			{
				windowIndexList.add(new Integer(3));
			}
		}// end if windowFlags > 0
		else
		{
			windowIndexList.add(new Integer(-1));
		}
		for(int i = 0; i < windowIndexList.size(); i++)
		{
			try
			{
				windowIndex = ((Integer)windowIndexList.get(i)).intValue();
				if(windowIndex > -1)
				{
					window = libccd.CCDSetupGetWindow(windowIndex);
					// window number is 1 more than index
					ccsFilename.setWindowNumber(windowIndex+1);
					ncols = libccd.CCDSetupGetWindowWidth(windowIndex);
					nrows = libccd.CCDSetupGetWindowHeight(windowIndex);
					// only change PRESCAN and POSTSCAN if windowed
				        // PRESCAN
					cardImage = ccsFitsHeader.get("PRESCAN");
					cardImage.setValue(new Integer(0));
				        // POSTSCAN
					cardImage = ccsFitsHeader.get("POSTSCAN");
				        //diddly see ccd_setup.c : SETUP_WINDOW_BIAS_WIDTH
					cardImage.setValue(new Integer(53));
				}
				else
				{
					// get current binning for later
					xbin = libccd.CCDSetupGetNSBin();
					ybin = libccd.CCDSetupGetNPBin();
					ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
						":saveFitsHeaders:Default window X size = "+
						ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")+" / "+xbin+" = "+
						(ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")/xbin)+".");
					ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
						":saveFitsHeaders:Default window Y size = "+
						ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")+" / "+ybin+" = "+
						(ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")/ybin)+".");
					window = new CCDLibrarySetupWindow(0,0,
							  ccsFitsHeaderDefaults.getValueInteger("CCDWXSIZ")/xbin,
							  ccsFitsHeaderDefaults.getValueInteger("CCDWYSIZ")/ybin);
					ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
						":saveFitsHeaders:Using default window : "+window+".");
					ccsFilename.setWindowNumber(1);
					ncols = libccd.CCDSetupGetNCols();
					nrows = libccd.CCDSetupGetNRows();
				}
				filename = ccsFilename.getFilename();
				// NAXIS1
				cardImage = ccsFitsHeader.get("NAXIS1");
				cardImage.setValue(new Integer(ncols));
				// NAXIS2
				cardImage = ccsFitsHeader.get("NAXIS2");
				cardImage.setValue(new Integer(nrows));
				// RUNNUM/EXPNUM
				ccsFitsHeader.add("RUNNUM",new Integer(ccsFilename.getMultRunNumber()),
						  ccsFitsHeaderDefaults.getComment("RUNNUM"),
						  ccsFitsHeaderDefaults.getUnits("RUNNUM"),
						  ccsFitsHeaderDefaults.getOrderNumber("RUNNUM"));
				ccsFitsHeader.add("EXPNUM",new Integer(ccsFilename.getRunNumber()),
						  ccsFitsHeaderDefaults.getComment("EXPNUM"),
						  ccsFitsHeaderDefaults.getUnits("EXPNUM"),
						  ccsFitsHeaderDefaults.getOrderNumber("EXPNUM"));
				// CCDWXOFF
				cardImage = ccsFitsHeader.get("CCDWXOFF");
				cardImage.setValue(new Integer(window.getXStart()));
				// CCDWYOFF
				cardImage = ccsFitsHeader.get("CCDWYOFF");
				cardImage.setValue(new Integer(window.getYStart()));
				// CCDWXSIZ
				cardImage = ccsFitsHeader.get("CCDWXSIZ");
				cardImage.setValue(new Integer(window.getXEnd()-window.getXStart()));
				// CCDWYSIZ
				cardImage = ccsFitsHeader.get("CCDWYSIZ");
				cardImage.setValue(new Integer(window.getYEnd()-window.getYStart()));
			}//end try
			// CCDLibraryNativeException thrown by CCDSetupGetWindow
			// FitsHeaderException thrown by ccsFitsHeaderDefaults.getValue
			// IllegalAccessException thrown by ccsFitsHeaderDefaults.getValue
			// InvocationTargetException thrown by ccsFitsHeaderDefaults.getValue
			// NoSuchMethodException thrown by ccsFitsHeaderDefaults.getValue
			// InstantiationException thrown by ccsFitsHeaderDefaults.getValue
			// ClassNotFoundException thrown by ccsFitsHeaderDefaults.getValue
			catch(Exception e)
			{
				String s = new String("Command "+command.getClass().getName()+
						      ":Setting Fits Headers in saveFitsHeaders failed:"+e);
				ccs.error(s,e);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+311);
				done.setErrorString(s);
				done.setSuccessful(false);
				return false;
			}
			// create lock file
			try
			{
				lockFile = new LockFile2(filename);
				lockFile.lock();
			}
			catch(Exception e)
			{
				String s = new String("Command "+command.getClass().getName()+
						":saveFitsHeaders:Create lock file failed for file:"+filename+":"+e);
				ccs.error(s,e);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+315);
				done.setErrorString(s);
				done.setSuccessful(false);
				return false;
			}
			// actually write FITS header
			try
			{
				ccsFitsHeader.writeFitsHeader(filename);
				filenameList.add(filename);
			}
			catch(FitsHeaderException e)
			{
				String s = new String("Command "+command.getClass().getName()+
						      ":Saving Fits Headers failed for file:"+filename+":"+e);
				ccs.error(s,e);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+312);
				done.setErrorString(s);
				done.setSuccessful(false);
				return false;
			}
		}// end for
		return true;
	}

	/**
	 * This method tries to unlock files associated with the FITS filename in filenameList.
	 * It first checks the lock file exists and only attempts to unlocked locked files. This is because
	 * this method can be called after a partial failure, where only some of the specified FITS files had been
	 * locked.
	 * @param command The command being implemented. This is used for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param filenameList A list containing FITS filenames which should have associated lock files.
	 * @return true if the method succeeds, false if a failure occurs
	 * @see ngat.util.LockFile2
	 */
	public boolean unLockFiles(COMMAND command,COMMAND_DONE done,List filenameList)
	{
		LockFile2 lockFile = null;
		String filename = null;

		for(int i = 0; i < filenameList.size(); i++)
		{
			try
			{
				filename = (String)(filenameList.get(i));
				lockFile = new LockFile2(filename);
				if(lockFile.isLocked())
					lockFile.unLock();
			}
			catch(Exception e)
			{
				String s = new String("Command "+command.getClass().getName()+
						":unLockFiles:Unlocking lock file failed for file:"+filename+":"+e);
				ccs.error(s,e);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+316);
				done.setErrorString(s);
				done.setSuccessful(false);
				return false;
			}
		}
		return true;
	}

	/**
	 * This method is called by command implementors that assume the CCD camera was configured to
	 * operate in non-windowed mode. The method checks libccd's setup window flags are zero.
	 * If they are non-zero, the done object is filled in with a suitable error message.
	 * @param done An instance of command done, the error string/number are set if the window flags are non-zero.
	 * @return The method returns true if the CCD is setup to be non-windowed, false if it is setup to be windowed.
	 * @see #libccd
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupGetWindowFlags
	 */
	public boolean checkNonWindowedSetup(COMMAND_DONE done)
	{
		if(libccd.CCDSetupGetWindowFlags() > 0)
		{
			String s = new String(":Configured for Windowed Readout:Expecting non-windowed.");
			ccs.error(s);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+310);
			done.setErrorString(s);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * Method to get an integer representing a SDSU output Amplifier,that can be passed into the CCDSetupDimensions
	 * method of ngat.rise.ccd.CCDLibrary. The amplifier to use depends on whether the exposure will be windowed
	 * or not, as windowed exposures  sometimes have to use a different amplifier (they can't use the DUAL readout
	 * amplifier setting). If windowed is true, the amplifier to use is got from the "ccs.config.window.amplifier"
	 * configuration property. If windowed is false, the amplifier to use is got from the "ccs.config.amplifier"
	 * configuration property.
	 * This implementation should agree with the eqivalent getDeInterlaceSetting method.
	 * @param windowed A boolean, should be true if we want to use the amplifier for windowing, false
	 *         if we want to use the default amplifier.
	 * @return An integer, representing a valid value to pass into CCDSetupDimensions to set the specified
	 *         amplifier.
	 * @exception NullPointerException Thrown if the property name, or it's value, are null.
	 * @exception CCDLibraryFormatException Thrown if the property's value, which is passed into
	 *            CCDDSPAmplifierFromString, does not contain a valid amplifier.
	 * @see #getAmplifier(java.lang.String)
	 */
	public int getAmplifier(boolean windowed) throws NullPointerException,CCDLibraryFormatException
	{
		int amplifier;

		if(windowed)
			amplifier = getAmplifier("ccs.config.window.amplifier");
		else
			amplifier = getAmplifier("ccs.config.amplifier");
		return amplifier;
	}

	/**
	 * Method to get an integer represeting a SDSU output Amplifier, that can be passed into the CCDSetupDimensions
	 * method of ngat.rise.ccd.CCDLibrary. The amplifier to use is retrieved from the specified property.
	 * @param propertyName A string, of the property keyword, the value of which is used to specify the
	 *        amplifier.
	 * @return An integer, representing a valid value to pass into CCDSetupDimensions to set the specified
	 *         amplifier.
	 * @exception NullPointerException Thrown if the property name, or it's value, are null.
	 * @exception CCDLibraryFormatException Thrown if the property's value, which is passed into
	 *            CCDDSPAmplifierFromString, does not contain a valid amplifier.
	 * @see #status
	 * @see #libccd
	 * @see CcsStatus#getProperty
	 * @see ngat.rise.ccd.CCDLibrary#CCDDSPAmplifierFromString
	 */
	public int getAmplifier(String propertyName) throws NullPointerException,CCDLibraryFormatException
	{
		String propertyValue = null;

		if(propertyName == null)
		{
			throw new NullPointerException(this.getClass().getName()+
						       ":getAmplifier:Property Name was null.");
		}
		propertyValue = status.getProperty(propertyName);
		if(propertyValue == null)
		{
			throw new NullPointerException(this.getClass().getName()+
						       ":getAmplifier:Property Value of keyword "+propertyName+
						       " was null.");
		}
		return libccd.CCDDSPAmplifierFromString(propertyValue);
	}

	/**
	 * Method to get an integer representing a SDSU output De-Interlace setting,
	 * that can be passed into the CCDSetupDimensions method of ngat.rise.ccd.CCDLibrary. 
	 * The setting to use depends on whether the exposure will be windowed
	 * or not, as windowed exposures  sometimes have to use a different amplifier (they can't use the DUAL readout
	 * amplifier setting). If windowed is true, the amplifier to use is got from the "ccs.config.window.amplifier"
	 * configuration property. If windowed is false, the amplifier to use is got from the "ccs.config.amplifier"
	 * configuration property. The chosen property name is passed to getDeInterlaceSetting to get the
	 * equivalent de-interlace setting.
	 * This implementation should agree with the eqivalent getAmplifier method.
	 * @param windowed A boolean, should be true if we want to use the de-interlace setting for windowing, false
	 *         if we want to use the default de-interlace setting.
	 * @return An integer, representing a valid value to pass into CCDSetupDimensions to set the specified
	 *         de-interlace setting.
	 * @exception NullPointerException Thrown if getDeInterlaceSetting fails.
	 * @exception CCDLibraryFormatException Thrown if getDeInterlaceSetting fails.
	 * @see #getDeInterlaceSetting(java.lang.String)
	 */
	public int getDeInterlaceSetting(boolean windowed) throws NullPointerException,CCDLibraryFormatException
	{
		int deInterlaceSetting;

		if(windowed)
			deInterlaceSetting = getDeInterlaceSetting("ccs.config.window.amplifier");
		else
			deInterlaceSetting = getDeInterlaceSetting("ccs.config.amplifier");
		return deInterlaceSetting;
	}

	/**
	 * Method to get an integer represeting a SDSU de-interlace setting,
	 * that can be passed into the CCDSetupDimensions method of ngat.rise.ccd.CCDLibrary. 
	 * The amplifier to use is retrieved from the specified property, and the de-interlace setting determined 
	 * from this.
	 * @param amplifierPropertyName A string, of the property keyword, the value of which is used to specify the
	 *        amplifier.
	 * @return An integer, representing a valid value to pass into CCDSetupDimensions to set the specified
	 *         de-interlace setting.
	 * @exception NullPointerException Thrown if the property name, or it's value, are null.
	 * @exception IllegalArgumentException Thrown if the amplifier was not recognised by this method.
	 * @exception CCDLibraryFormatException Thrown if the derived de-interlace string, which is passed into
	 *            CCDDSPDeinterlaceFromString, does not contain a valid de-interlace setting.
	 * @see #getAmplifier
	 * @see ngat.rise.ccd.CCDLibrary#CCDDSPDeinterlaceFromString
	 */
	public int getDeInterlaceSetting(String amplifierPropertyName) throws NullPointerException,
	                                 IllegalArgumentException,CCDLibraryFormatException
	{
		String deInterlaceString = null;
		int amplifier,deInterlaceSetting;

		amplifier = getAmplifier(amplifierPropertyName);
		// convert Amplifier to De-Interlace Setting string
		switch(amplifier)
		{
			case CCDLibrary.CCD_DSP_AMPLIFIER_LEFT:
				deInterlaceString = "CCD_DSP_DEINTERLACE_SINGLE";
				break;
			case CCDLibrary.CCD_DSP_AMPLIFIER_RIGHT:
				deInterlaceString = "CCD_DSP_DEINTERLACE_FLIP";
				break;
			case CCDLibrary.CCD_DSP_AMPLIFIER_BOTH:
				deInterlaceString = "CCD_DSP_DEINTERLACE_SPLIT_SERIAL";
				break;
			default:
				throw new IllegalArgumentException(this.getClass().getName()+
						       ":getDeInterlaceSetting:Amplifier String of keyword "+
						       amplifierPropertyName+" was illegal value "+amplifier+".");
		}
		// convert de-interlace string into value to pass to libccd.
		deInterlaceSetting = libccd.CCDDSPDeinterlaceFromString(deInterlaceString);
		return deInterlaceSetting;
	}

	/**
	 * This method retrieves the current Amplifier configuration used to configure the CCD controller.
	 * This determines which readout(s) the CCD uses. The numeric setting is then converted into a 
	 * valid string as specified by the LT FITS standard.
	 * @return A String is returned, either 'LEFT', 'RIGHT', or 'DUAL'. If the amplifier cannot be
	 * 	determined an exception is thrown.
	 * @exception IllegalArgumentException Thrown if the amplifier string cannot be determined.
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupGetAmplifier
	 * @see ngat.rise.ccd.CCDLibrary#CCD_DSP_AMPLIFIER_LEFT
	 * @see ngat.rise.ccd.CCDLibrary#CCD_DSP_AMPLIFIER_RIGHT
	 * @see ngat.rise.ccd.CCDLibrary#CCD_DSP_AMPLIFIER_BOTH
	 * @see #libccd
	 */
	private String getCCDRDOUTValue() throws IllegalArgumentException
	{
		String amplifierString = null;
		int amplifier;

		//get amplifier from libccd cached setting.
		amplifier = libccd.CCDSetupGetAmplifier();
		switch(amplifier)
		{
			case CCDLibrary.CCD_DSP_AMPLIFIER_LEFT:
				amplifierString = "LEFT";
				break;
			case CCDLibrary.CCD_DSP_AMPLIFIER_RIGHT:
				amplifierString = "RIGHT";
				break;
			case CCDLibrary.CCD_DSP_AMPLIFIER_BOTH:
				amplifierString = "DUAL";
				break;
			default:
				throw new IllegalArgumentException("getCCDRDOUTValue:amplifier:"+amplifier+
									" not known.");
		}
		return amplifierString;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.2  2010/02/10 11:03:07  cjm
// Added FITS lock file support to saveFitsHeaders, and unLockFile(s) methods.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.30  2006/05/16 14:25:52  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.29  2005/09/29 14:06:59  cjm
// Added CALBEFOR, CALAFTER, ROTCENTX, ROTCENTY value setting in setFitsHeaders
// Changed values of CCDWXSIZ, CCDWYSIZ in setFitsHeaders, saveFitsHeaders.
//
// Revision 0.28  2005/07/26 16:04:21  cjm
// Modified setFitsHeaders, added exposureCount parameter to support EXPTOTAL.
//
// Revision 0.27  2004/11/24 16:47:21  cjm
// RIGHT amplifier now uses FLIP deinterlace setting.
//
// Revision 0.26  2004/11/04 16:06:46  cjm
// Added commented out code for RIGHT amplifier equals FLIP.
//
// Revision 0.25  2003/06/06 12:47:15  cjm
// Windowing implementation and amplifier changes.
//
// Revision 0.24  2003/03/26 15:40:18  cjm
// First attempt at windowing implementation.
//
// Revision 0.23  2002/12/19 17:16:18  cjm
// Fixed setFitsHeaders for filterWheelEnable.
//
// Revision 0.22  2001/07/31 14:17:09  cjm
// Changed Utilites for Utilities.
//
// Revision 0.21  2001/07/12 17:50:53  cjm
// autoguiderStop changes.
//
// Revision 0.20  2001/07/12 10:23:45  cjm
// Updated ccs.error calls, where appropriate, to use Exception parameter method.
//
// Revision 0.19  2001/07/03 16:27:09  cjm
// Re-wrote setFitsHeaders method, to retrieve a default list of keyword/value card images
// from the loaded FitsDefaults properties. Simplyfied setting of values from Ccs status/libccd.
// Added CcsStatus local reference.
// Added Ccs error code base to error numbers.
// Changed ccsFilename type.
// Changed OBSTYPE definitions.
//
// Revision 0.18  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 0.17  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.16  2001/01/15 18:35:46  cjm
// Protected against xbin being zero, if the CCD Camera has not been configured yet,
// so the error returned is about Setup not being complete, not division by zero.
//
// Revision 0.15  2000/12/19 18:25:03  cjm
// More ngat.ccd.CCDLibrary filter wheel code changes.
//
// Revision 0.14  2000/12/19 17:52:10  cjm
// New ngat.ccd.CCDLibrary filter wheel method calls.
//
// Revision 0.13  2000/11/13 17:22:56  cjm
// Stopped error strings in exceptions being printed twice: printStackTrace prints error message.
//
// Revision 0.12  2000/08/09 16:38:40  cjm
// Changed filter wheel code.
//
// Revision 0.11  2000/07/13 09:13:00  cjm
// Changed PRESCAN, POSTSCAN, CCDXIMSI, CCDYIMSI values so
// that binning affects their values.
//
// Revision 0.10  2000/06/14 09:21:40  cjm
// Added extra debugging when general exceptions thrown:
// stack trace is printed to error file.
//
// Revision 0.9  2000/06/13 17:19:45  cjm
// Changes to properties file/ fits defaults from file.
//
// Revision 0.8  2000/05/26 13:59:56  cjm
// Updated setFitsHeaders and FITS header information.
//
// Revision 0.7  2000/05/17 09:43:59  cjm
// Replaced CcsFitsHeader with ngat.fits.FitsHeader.
//
// Revision 0.6  2000/02/07 11:33:28  cjm
// Fixed exeption handling error.
//
// Revision 0.5  1999/12/03 15:58:34  cjm
// sendISSCommand now takes thread it was called from as a parameter.
//
// Revision 0.4  1999/11/01 15:53:41  cjm
// Changed calculateAcknowledgeTime to return ACK rather than an int.
// This is to keep up to date with the changes to ngat.net.TCPServerConnectionThread class.
//
// Revision 0.3  1999/11/01 10:45:51  cjm
// Got rid of init methods that just called super-class's method.
// Added constructor to setup implement string correctly.
//
// Revision 0.2  1999/10/27 16:47:25  cjm
// Changed definition of RCSID so that file Ids are picked up properly.
//
// Revision 0.1  1999/10/27 16:25:54  cjm
// initial revision.
//
//
