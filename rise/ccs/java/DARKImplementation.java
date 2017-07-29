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
// DARKImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/DARKImplementation.java,v 1.3 2017-07-29 15:28:40 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.DARK;
import ngat.message.ISS_INST.DARK_DONE;

/**
 * This class provides the implementation for the DARK command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class DARKImplementation extends CALIBRATEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: DARKImplementation.java,v 1.3 2017-07-29 15:28:40 cjm Exp $");

	/**
	 * Constructor.
	 */
	public DARKImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.DARK&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.DARK";
	}

	/**
	 * This method gets the DARK command's acknowledge time. This returns the server connection threads 
	 * default acknowledge time plus the dark exposure time.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;
		DARK darkCommand = (DARK)command;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(darkCommand.getExposureTime()+
			serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the DARK command. It generates some FITS headers from the CCD setup and
	 * the ISS and saves this to disc. It performs a dark exposure and saves the data from this to disc.
	 * The resultant data or the relevant error code is put into the an object of class DARK_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#checkNonWindowedSetup
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see FITSImplementation#unLockFile
	 * @see ngat.rise.ccd.CCDLibrary#CCDExposureExpose
	 * @see CALIBRATEImplementation#reduceCalibrate
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		DARK darkCommand = (DARK)command;
		DARK_DONE darkDone = new DARK_DONE(command.getId());
		CcsStatus status = null;
		String filename = null;

		status = ccs.getStatus();
		if(testAbort(command,darkDone) == true)
			return darkDone;
		if(checkNonWindowedSetup(darkDone) == false)
			return darkDone;
	// Clear the pause and resume times.
		status.clearPauseResumeTimes();
	// get fits headers
		clearFitsHeaders();
		if(setFitsHeaders(command,darkDone,FitsHeaderDefaults.OBSTYPE_VALUE_DARK,
			darkCommand.getExposureTime()) == false)
			return darkDone;
		if(getFitsHeadersFromISS(command,darkDone) == false)
			return darkDone;
		if(testAbort(command,darkDone) == true)
			return darkDone;
	// get a filename to store frame in
		ccsFilename.nextMultRunNumber();
		try
		{
			ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_DARK);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":"+e.toString());
			darkDone.setFilename(filename);
			darkDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+901);
			darkDone.setErrorString(e.toString());
			darkDone.setSuccessful(false);
			return darkDone;
		}
		ccsFilename.nextRunNumber();
		filename = ccsFilename.getFilename();
		if(saveFitsHeaders(command,darkDone,filename) == false)
		{
			unLockFile(command,darkDone,filename);
			return darkDone;
		}
	// do exposure
		status.setExposureFilename(filename);
		try
		{
			libccd.CCDExposureExpose(false,-1,darkCommand.getExposureTime(),filename);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":"+e.toString());
			darkDone.setFilename(filename);
			darkDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+900);
			darkDone.setErrorString(e.toString());
			darkDone.setSuccessful(false);
			unLockFile(command,darkDone,filename);
			return darkDone;
		}
		// remove lock files created in saveFitsHeaders
		if(unLockFile(command,darkDone,filename) == false)
			return darkDone;
	// Test abort status.
		if(testAbort(command,darkDone) == true)
			return darkDone;
	// Call pipeline to reduce data.
	//	if(reduceCalibrate(command,darkDone,filename) == false)
	//		return darkDone; 
	// set return values to indicate success.
		darkDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		darkDone.setErrorString("");
		darkDone.setSuccessful(true);
	// return done object.
		return darkDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.2  2010/02/10 11:03:07  cjm
// Added FITS lock file support.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.14  2006/05/16 14:25:49  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.13  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.12  2003/03/26 15:40:18  cjm
// Added windowing check.
//
// Revision 0.11  2001/07/03 16:29:17  cjm
// Added Ccs base error number to error numbers.
// Changed CcsFilename to FitsFilename.
//
// Revision 0.10  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.9  2000/06/20 12:47:36  cjm
// CCDExposureExpose parameter change.
//
// Revision 0.8  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.7  2000/03/13 12:12:41  cjm
// Added clearing of pause and resume times.
//
// Revision 0.6  2000/02/28 19:14:00  cjm
// Backup.
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
