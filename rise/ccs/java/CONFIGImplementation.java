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
// CONFIGImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CONFIGImplementation.java,v 1.3 2017-07-29 15:35:32 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.CONFIG;
import ngat.message.ISS_INST.CONFIG_DONE;
import ngat.message.ISS_INST.OFFSET_FOCUS;
import ngat.message.ISS_INST.OFFSET_FOCUS_DONE;
import ngat.message.ISS_INST.INST_TO_ISS_DONE;
import ngat.phase2.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the CONFIG command sent to a server using the
 * Java Message System. It extends SETUPImplementation.
 * @see SETUPImplementation
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class CONFIGImplementation extends SETUPImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CONFIGImplementation.java,v 1.3 2017-07-29 15:35:32 cjm Exp $");

	/**
	 * Constructor. 
	 */
	public CONFIGImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.CONFIG&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.CONFIG";
	}

	/**
	 * This method gets the CONFIG command's acknowledge time.
	 * This can take a long time to move the filter wheels to the required position.
	 * This method returns an ACK with timeToComplete set to the &quot; ccs.config.acknowledge_time &quot;
	 * held in the Ccs configuration file. If this cannot be found/is not a valid number the default acknowledge
	 * time is used instead.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set to a time (in milliseconds).
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;
		int timeToComplete = 0;

		acknowledge = new ACK(command.getId());
		try
		{
			timeToComplete += ccs.getStatus().getPropertyInteger("ccs.config.acknowledge_time");
		}
		catch(NumberFormatException e)
		{
			ccs.error(this.getClass().getName()+":calculateAcknowledgeTime:"+e);
			timeToComplete += serverConnectionThread.getDefaultAcknowledgeTime();
		}
		acknowledge.setTimeToComplete(timeToComplete);
		return acknowledge;
	}

	/**
	 * This method implements the CONFIG command. 
	 * <ul>
	 * <li>It checks the message contains a suitable CCDConfig object to configure the controller.
	 * <li>It gets the number of rows and columns from the loaded CCS properties file.
	 * <li>It gets binning information from the CCDConfig object passed with the command.
	 * <li>It gets windowing information from the CCDConfig object passed with the command.
	 * <li>It gets filter wheel filter names from the CCDConfig object and converts them to positions
	 * 	using a configuration file.
	 * <li>It sends the information to the SDSU CCD Controller to configure it.
	 * <li>It issues an OFFSET_FOCUS commmand to the ISS based on the optical thickness of the filter(s).
	 * <li>It increments the unique configuration ID.
	 * </ul>
	 * An object of class CONFIG_DONE is returned. If an error occurs a suitable error message is returned.
	 * @see #setFocusOffset
	 * @see ngat.phase2.CCDConfig
	 * @see CcsStatus#getNumberColumns
	 * @see CcsStatus#getNumberRows
	 * @see CcsStatus#getPropertyInteger
	 * @see CcsStatus#incConfigId
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupDimensions
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		CONFIG configCommand = null;
		//CCDConfig ccdConfig = null;
		RISEConfig riseConfig = null; // Add the RISE configuration
		Detector detector = null;
		CONFIG_DONE configDone = null;
		CCDLibrarySetupWindow windowList[] = new CCDLibrarySetupWindow[CCDLibrary.CCD_SETUP_WINDOW_COUNT];
		CcsStatus status = null;
		int numberColumns,numberRows;

	// test contents of command.
		configCommand = (CONFIG)command;
		configDone = new CONFIG_DONE(command.getId());
		status = ccs.getStatus();
		if(testAbort(configCommand,configDone) == true)
			return configDone;
		if(configCommand.getConfig() == null)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+command+":Config was null.");
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+800);
			configDone.setErrorString(":Config was null.");
			configDone.setSuccessful(false);
			return configDone;
		}
		if((configCommand.getConfig() instanceof RISEConfig) == false)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":Config has wrong class:"+
				configCommand.getConfig().getClass().getName());
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+801);
			configDone.setErrorString(":Config has wrong class:"+
				configCommand.getConfig().getClass().getName());
			configDone.setSuccessful(false);
			return configDone;
		}
	// get ccdConfig from configCommand.
		riseConfig = (RISEConfig)configCommand.getConfig();
	// get local detector copy
		detector = riseConfig.getDetector(0);
	// set cached copy of calibrateBefore and calibrateAfter for FITS headers
		status.setCachedCalibrateBefore(riseConfig.getCalibrateBefore());
		status.setCachedCalibrateAfter(riseConfig.getCalibrateAfter());
	// load other required config for dimension configuration from CCS properties file.
		try
		{
			numberColumns = status.getNumberColumns(detector.getXBin());
			numberRows = status.getNumberRows(detector.getYBin());
		}
	// CCDLibraryFormatException is caught and re-thrown by this method.
	// Other exceptions (IllegalArgumentException,NumberFormatException) are not caught here, 
	// but by the calling method catch(Exception e)
		catch(CCDLibraryFormatException e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":",e);
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+802);
			configDone.setErrorString("processCommand:"+command+":"+e);
			configDone.setSuccessful(false);
			return configDone;
		}
	// test abort
		if(testAbort(configCommand,configDone) == true)
			return configDone;
	// check xbin and ybin: greater than zero (hardware restriction), xbin == ybin (pipeline restriction)
		if((detector.getXBin()<1)||(detector.getYBin()<1)||(detector.getXBin()!=detector.getYBin()))
		{
			String errorString = null;

			errorString = new String("Illegal xBin and yBin:xBin="+detector.getXBin()+",yBin="+
						detector.getYBin());
			ccs.error(this.getClass().getName()+":processCommand:"+command+":"+errorString);
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+807);
			configDone.setErrorString(errorString);
			configDone.setSuccessful(false);
			return configDone;
		}
	// We can either bin, or window, but not both at once. We only check one binning direction - see above
		if((detector.getWindowFlags() > 0) && (detector.getXBin() > 1))
		{
			String errorString = null;

			errorString = new String("Illegal binning and windowing:xBin="+detector.getXBin()+",window="+
						detector.getWindowFlags());
			ccs.error(this.getClass().getName()+":processCommand:"+command+":"+errorString);
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+808);
			configDone.setErrorString(errorString);
			configDone.setSuccessful(false);
			return configDone;
		}
	// setup window list from ccdConfig.
		for(int i = 0; i < detector.getMaxWindowCount(); i++)
		{
			Window w = null;

			if(detector.isActiveWindow(i))
			{
				w = detector.getWindow(i);
				if(w == null)
				{
					String errorString = new String("Window "+i+" is null.");

					ccs.error(this.getClass().getName()+":processCommand:"+
						command+":"+errorString);
					configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+803);
					configDone.setErrorString(errorString);
					configDone.setSuccessful(false);
					return configDone;
				}
				windowList[i] = new CCDLibrarySetupWindow(w.getXs(),w.getYs(),
					w.getXe(),w.getYe());
			}
			else
			{
				windowList[i] = new CCDLibrarySetupWindow(-1,-1,-1,-1);
			}
		}
		if(testAbort(configCommand,configDone) == true)
			return configDone;
	// send dimension/filter wheel configuration to the SDSU controller
		ccs.error(this.getClass().getName()+":Config: "+
				numberColumns+"x"+numberRows+" "+ detector.getXBin()+"x"+detector.getYBin());
		try
		{
			libccd.CCDSetupDimensions(numberColumns,numberRows,detector.getXBin(),detector.getYBin(),
						  detector.getWindowFlags(),windowList);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":",e);
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+804);
			configDone.setErrorString(":processCommand:"+command+":"+e);
			configDone.setSuccessful(false);
			return configDone;
		}
	// test abort
		if(testAbort(configCommand,configDone) == true)
			return configDone;
	// Issue ISS OFFSET_FOCUS commmand 
		if(setFocusOffset(configCommand.getId(),riseConfig,status,configDone) == false)
		    return configDone;
	// Increment unique config ID.
	// This is queried when saving FITS headers to get the CONFIGID value.
		try
		{
			status.incConfigId();
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":Incrementing configuration ID:"+e.toString());
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+806);
			configDone.setErrorString("Incrementing configuration ID:"+e.toString());
			configDone.setSuccessful(false);
			return configDone;
		}
	// Store name of configuration used in status object.
	// This is queried when saving FITS headers to get the CONFNAME value.
		status.setConfigName(riseConfig.getId());
	// setup return object.
		configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		configDone.setErrorString("");
		configDone.setSuccessful(true);
	// return done object.
		return configDone;
	}

	/**
	 * Routine to set the telescope focus offset. Sends a OFFSET_FOCUS command to
	 * the ISS. The OFFSET_FOCUS sent is raed from the config.
	 * @param id The Id is used as the OFFSET_FOCUS command's id.
	 * @param riseConfig The configuration to attain, including the type of filter we are using for each wheel.
	 * @param status A reference to the Ccs's status object, which contains the filter database.
	 * @param configDone The instance of CONFIG_DONE. This is filled in with an error message if the
	 * 	OFFSET_FOCUS fails.
	 * @return The method returns true if the telescope attained the focus offset, otherwise false is
	 * 	returned an telFocusDone is filled in with an error message.
	 */
	private boolean setFocusOffset(String id,RISEConfig riseConfig,CcsStatus status,CONFIG_DONE configDone)
	{
		OFFSET_FOCUS focusOffsetCommand = null;
		INST_TO_ISS_DONE instToISSDone = null;
		String filterIdName = null;
		String filterTypeString = null;
		float focusOffset = 0.0f;

		focusOffsetCommand = new OFFSET_FOCUS(id);
		focusOffset = 0.0f;
	// get default focus offset
		focusOffset += status.getPropertyDouble("ccs.focus.offset");
		ccs.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":setFocusOffset:Master offset is "+
			focusOffset+".");
	// set the commands focus offset
		focusOffsetCommand.setFocusOffset(focusOffset);
		ccs.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":setFocusOffset:Total offset is "+
			focusOffset+".");
		instToISSDone = ccs.sendISSCommand(focusOffsetCommand,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":focusOffset failed:"+focusOffset+":"+
				instToISSDone.getErrorString());
			configDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+805);
			configDone.setErrorString(instToISSDone.getErrorString());
			configDone.setSuccessful(false);
			return false;
		}
		return true;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.2  2010/03/26 14:38:29  cjm
// Changed from bitwise to absolute logging levels.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.28  2007/06/19 16:39:46  cjm
// Added default focus offset from ccs.focus.offset property.
//
// Revision 0.27  2006/05/16 14:25:48  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.26  2005/09/29 14:06:59  cjm
// Added setting of cached calibrateBefore and calibrateAfter settings in Status.
//
// Revision 0.25  2003/06/06 12:47:15  cjm
// CHanges to config relating to windowing implementation.
//
// Revision 0.24  2003/03/26 15:40:18  cjm
// First attempt at windowing implementation.
//
// Revision 0.23  2002/12/16 17:00:27  cjm
// Filter wheel anable code checks added.
// NPCCDConfog replaced by CCDConfig.
//
// Revision 0.22  2001/08/20 15:45:52  cjm
// Fixed compilation error.
//
// Revision 0.21  2001/08/20 15:29:53  cjm
// Error message changes.
//
// Revision 0.20  2001/07/03 16:35:59  cjm
// Added Ccs base error code to error numbers.
// Added new binning checks.
// Added setConfigId/setConfigName calls .
//
// Revision 0.19  2001/04/25 19:14:08  cjm
// Removed subtraction of telfocus optical thickness from the FOCUS_OFFSET:
// the RCS is responsible for doing this.
//
// Revision 0.18  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.17  2001/01/24 18:01:55  cjm
// Added setFocusOffset method.
//
// Revision 0.16  2000/12/19 18:25:03  cjm
// More ngat.ccd.CCDLibrary filter wheel code changes.
//
// Revision 0.15  2000/12/19 17:52:14  cjm
// New ngat.ccd.CCDLibrary filter wheel method calls.
//
// Revision 0.14  2000/12/01 18:42:35  cjm
// backup.
//
// Revision 0.13  2000/06/13 17:19:45  cjm
// Changes to setup/output amplifier etc...
//
// Revision 0.12  2000/05/26 11:08:32  cjm
// CCDLibrary.CCD_SETUP_WINDOW_COUNT now used.
//
// Revision 0.11  2000/05/23 13:48:06  cjm
// CcsStatus.configId now updated with last successfull configuration Id string.
//
// Revision 0.10  2000/02/08 14:09:35  cjm
// Added coment regarding ISS OFFSET_FOCUS command.
//
// Revision 0.9  2000/02/07 18:17:10  cjm
// Changed to use CCDSetupDimensions, DSP program download and controller reset/test are
// now performed at startup.
// Initial windowing and filter wheel configuration added.
//
// Revision 0.8  2000/01/25 10:50:55  cjm
// Changed due to method name changes in ngat.ccd.CCDLibrary.
// This it self is due to CCDSetupSetupCCD using CCD_DSP_ constants for gain and de-interlacing,
// rather than CCD_SETUP_ copies.
//
// Revision 0.7  1999/12/15 15:12:39  cjm
// Changed calculateAcknowledgeTime to return more time for when we download
// DSP code from a file.
//
// Revision 0.6  1999/11/24 11:01:37  cjm
// Checked CONFIG.getConfig is not null, and setup an error message if it is.
//
// Revision 0.6  1999/11/24 10:21:10  cjm
// Added test for CONFIG.getConfig() returning null. Not sure whether this
// will always be necessary, but currently CcsGUI sends null configs.
//
// Revision 0.5  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
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
