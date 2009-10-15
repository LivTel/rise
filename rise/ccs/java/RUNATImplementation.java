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
// RUNATImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/RUNATImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.util.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.RUNAT;
import ngat.message.ISS_INST.RUNAT_DONE;

/**
 * This class provides the implementation for the RUNAT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class RUNATImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: RUNATImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public RUNATImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.RUNAT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.RUNAT";
	}

	/**
	 * This method gets the RUNAT command's acknowledge time. The RUNAT command will complete in
	 * the exposure time plus the default acknowledge time after the start time of the exposure.
	 * The start time of the exposure minus the time now is how long we have to wait until the start
	 * of the exposure.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.ISS_INST.RUNAT#getStartTime
	 * @see ngat.message.ISS_INST.RUNAT#getExposureTime
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		RUNAT runatCommand = (RUNAT)command;
		ACK acknowledge = null;
		Date now = new Date();// assumes System clock accurate
		int startTime = 0;

		acknowledge = new ACK(command.getId());
		if(runatCommand.getStartTime().after(now))
			startTime = (int)(runatCommand.getStartTime().getTime() - now.getTime());
		else
			startTime = 0;
		acknowledge.setTimeToComplete(startTime+runatCommand.getExposureTime()+
			serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the RUNAT command. 
	 * <ul>
	 * <li>It generates some FITS headers from the CCD setup and
	 * the ISS and saves this to disc. 
	 * <li>It moves the fold mirror to the correct location.
	 * <li>It starts the autoguider.
	 * <li>It waits until the specified start time occurs.
	 * <li>It performs an exposure and saves the data from this to disc.
	 * <li>It stops the autoguider.
	 * <li>It calls the Real Time Data Pipeline to reduce the data, if applicable.
	 * </ul>
	 * The resultant filename or the relevant error code is put into the an object of class RUNAT_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see ngat.rise.ccd.CCDLibrary#CCDExposureExpose
	 * @see EXPOSEImplementation#reduceExpose
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		RUNAT runatCommand = (RUNAT)command;
		RUNAT_DONE runatDone = new RUNAT_DONE(command.getId());
		CcsStatus status = null;
		String filename = null;
		String obsType = null;
		List filenameList = null;

		if(testAbort(runatCommand,runatDone) == true)
			return runatDone;
	// get status reference. initialise exposure status.
		status = ccs.getStatus();
		status.setExposureCount(1);
		status.setExposureNumber(0);
		status.clearPauseResumeTimes();
	// get fits headers
		clearFitsHeaders();
		if(runatCommand.getStandard())
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_STANDARD;
		else
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_EXPOSURE;
		if(setFitsHeaders(runatCommand,runatDone,obsType,runatCommand.getExposureTime()) == false)
			return runatDone;
		if(getFitsHeadersFromISS(runatCommand,runatDone) == false)
			return runatDone;
		if(testAbort(runatCommand,runatDone) == true)
			return runatDone;
	// move the fold mirror to the correct location
		if(moveFold(runatCommand,runatDone) == false)
			return runatDone;
		if(testAbort(runatCommand,runatDone) == true)
			return runatDone;
	// set successful to indicate we are proceeding ok
		runatDone.setSuccessful(true);
		runatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		runatDone.setErrorString("");
		runatDone.setSuccessful(true);
	// get a filename to store frame in
		ccsFilename.nextMultRunNumber();
		try
		{
			if(runatCommand.getStandard())
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_STANDARD);
			else
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_EXPOSURE);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
			runatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1601);
			runatDone.setErrorString(e.toString());
			runatDone.setSuccessful(false);
			autoguiderStop(runatCommand,runatDone,false);
			return runatDone;
		}
		ccsFilename.nextRunNumber();
		filename = ccsFilename.getFilename();
// diddly 1st window filename only
	// save FITS headers.
		filenameList = new Vector();
		if(saveFitsHeaders(runatCommand,runatDone,filenameList) == false)
			return runatDone;
		runatDone.setFilename(filename);
// diddly 1st window filename only
		if(testAbort(runatCommand,runatDone) == true)
			return runatDone;
	// autoguider on
		if(autoguiderStart(runatCommand,runatDone) == false)
			return runatDone;
		if(testAbort(runatCommand,runatDone) == true)
		{
			autoguiderStop(runatCommand,runatDone,false);
			return runatDone;
		}
	// do exposure
// diddly 1st window filename only
		status.setExposureFilename(filename);
		try
		{
// diddly 1st window filename only
			libccd.CCDExposureExpose(true,runatCommand.getStartTime().getTime(),
				runatCommand.getExposureTime(),filenameList);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
			runatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1600);
			runatDone.setErrorString(e.toString());
			runatDone.setSuccessful(false);
			autoguiderStop(runatCommand,runatDone,false);
			return runatDone;
		}
		status.setExposureNumber(1);
	// autoguider off
		if(autoguiderStop(runatCommand,runatDone,true) == false)
			return runatDone;
	// test abort
		if(testAbort(runatCommand,runatDone) == true)
			return runatDone;
	// call pipeline to reduce data
	// done values should be set by this routine.
// diddly 1st window filename only
		runatDone.setFilename(filename);
		runatDone.setSeeing(0.0f);
		runatDone.setCounts(0.0f);
		runatDone.setXpix(0.0f);
		runatDone.setYpix(0.0f);
		runatDone.setPhotometricity(0.0f);
		runatDone.setSkyBrightness(0.0f);
		runatDone.setSaturation(false);
		if(runatCommand.getPipelineProcess())
		{
// diddly 1st window filename only
			for(int i = 0; i < filenameList.size();i++)
			{
				filename = (String)(filenameList.get(i));
				if(reduceExpose(runatCommand,runatDone,filename) == false)
					return runatDone;
			}
		}
	// return done object.
		return runatDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.20  2006/05/16 14:26:02  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.19  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.18  2003/03/26 15:40:18  cjm
// First attempt at windowing implementation.
// ACKS not sent correctly yet...
//
// Revision 0.17  2002/05/23 12:40:25  cjm
// Added defaults for extra fields in EXPOS_DONE.
//
// Revision 0.16  2001/07/12 17:50:07  cjm
// autoguiderStop changes.
//
// Revision 0.15  2001/07/03 16:00:27  cjm
// Added Ccs error code offset to error codes.
// Changed OBSTYPE codes.
//
// Revision 0.14  2001/04/26 17:08:11  cjm
// Added standard flag check, and sets exposure code to standard.
//
// Revision 0.13  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.12  2000/06/20 12:51:28  cjm
// CCDExposureExpose parameter change.
//
// Revision 0.11  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.10  2000/03/13 12:17:11  cjm
// Added clearing of pause and resume times.
//
// Revision 0.9  2000/03/01 10:13:28  cjm
// Removed wait loop for start of exposure - now done in C code for more accuracy.
//
// Revision 0.8  2000/02/28 19:14:00  cjm
// Backup.
//
// Revision 0.7  2000/02/17 17:56:34  cjm
// Setting ExposureCount/ExposureNumber status.
//
// Revision 0.6  1999/11/04 11:46:37  cjm
// Completely changed implementation of calculateAcknowledgeTime as the
// old CcsTCPServerConnectionThread version was not copied to this Implementation class
// correctly. It now takes account of the start time of the exposure!
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
