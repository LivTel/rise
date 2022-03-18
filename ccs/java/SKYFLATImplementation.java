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
// SKYFLATImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/SKYFLATImplementation.java,v 1.2 2010-02-10 11:03:07 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.SKYFLAT;
import ngat.message.ISS_INST.SKYFLAT_DONE;

/**
 * This class provides the implementation for the SKYFLAT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class SKYFLATImplementation extends CALIBRATEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: SKYFLATImplementation.java,v 1.2 2010-02-10 11:03:07 cjm Exp $");

	/**
	 * Constructor.
	 */
	public SKYFLATImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.SKYFLAT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.SKYFLAT";
	}

	/**
	 * This method gets the SKYFLAT command's acknowledge time. The SKYFLATs exposure time is based on the
	 * following:
	 * <ul>
	 * <li>the current local time.
	 * <li>the date
	 * <li>filter
	 * <li>the supplied current telescope position. 
	 * <li>etc.(!) 
	 * </ul> 
	 * An algorithm for this method needs to be devised.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		SKYFLAT skyFlatCommand = (SKYFLAT)command;
		ACK acknowledge = null;
		int exposureTime = 0;

		acknowledge = new ACK(command.getId());

		if(skyFlatCommand.getUseTime())
			exposureTime = skyFlatCommand.getExposureTime();
		else
		{
			// diddly - this needs to calculated from the variables above.
			// as done in processCommand
			exposureTime = 10*1000;
		}
		acknowledge.setTimeToComplete(serverConnectionThread.getDefaultAcknowledgeTime()+exposureTime);
		return acknowledge;
	}

	/**
	 * This method implements the SKYFLAT command. It generates some FITS headers from the CCD setup and
	 * the ISS and saves this to disc. It starts the autoguider,
	 * performs a Sky-Flat exposure and saves the data from this to disc, and then stops the autoguider.
	 * It sends the generated FITS data to the Real Time Data Pipeline to get some data from it.
	 * The resultant data or the relevant error code is put into the an object of class SKYFLAT_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
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
		SKYFLAT skyFlatCommand = (SKYFLAT)command;
		SKYFLAT_DONE skyFlatDone = new SKYFLAT_DONE(command.getId());
		String filename = null;
		int exposureTime = 0;
		CcsStatus status = null;

		status = ccs.getStatus();
		if(testAbort(skyFlatCommand,skyFlatDone) == true)
			return skyFlatDone;
	// get fits headers
		clearFitsHeaders();
		if(skyFlatCommand.getUseTime())
		{
			exposureTime = skyFlatCommand.getExposureTime();
		}
		else
		{
// diddly. Calculate exposure time (in milliseconds) from:
// 1. the current local time
// 2. the date
// 3. filter 
// 4. the supplied current telescope position. 
// 5. etc.(!) 
			exposureTime = 10*1000;
		}
		if(setFitsHeaders(skyFlatCommand,skyFlatDone,
			FitsHeaderDefaults.OBSTYPE_VALUE_SKY_FLAT,exposureTime) == false)
			return skyFlatDone;
		if(getFitsHeadersFromISS(skyFlatCommand,skyFlatDone) == false)
			return skyFlatDone;
		if(testAbort(skyFlatCommand,skyFlatDone) == true)
			return skyFlatDone;
	// move the fold mirror to the correct location
		if(moveFold(skyFlatCommand,skyFlatDone) == false)
			return skyFlatDone;
		if(testAbort(skyFlatCommand,skyFlatDone) == true)
			return skyFlatDone;
	// setup filename
		ccsFilename.nextMultRunNumber();
		try
		{
			ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_SKY_FLAT);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
			skyFlatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1801);
			skyFlatDone.setErrorString(e.toString());
			skyFlatDone.setSuccessful(false);
			return skyFlatDone;
		}
		ccsFilename.nextRunNumber();
		filename = ccsFilename.getFilename();
	// save FITS headers
		if(saveFitsHeaders(skyFlatCommand,skyFlatDone,filename) == false)
		{
			unLockFile(skyFlatCommand,skyFlatDone,filename);
			return skyFlatDone;
		}
	// start autoguider
		if(autoguiderStart(skyFlatCommand,skyFlatDone) == false)
		{
			unLockFile(skyFlatCommand,skyFlatDone,filename);
			return skyFlatDone;
		}
		if(testAbort(skyFlatCommand,skyFlatDone) == true)
		{
			autoguiderStop(skyFlatCommand,skyFlatDone,false);
			unLockFile(skyFlatCommand,skyFlatDone,filename);
			return skyFlatDone;
		}
	// do sky-flat command here
		status.setExposureFilename(filename);
		try
		{
			libccd.CCDExposureExpose(true,-1,exposureTime,filename);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
			skyFlatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1800);
			skyFlatDone.setErrorString(e.toString());
			skyFlatDone.setSuccessful(false);
			autoguiderStop(skyFlatCommand,skyFlatDone,false);
			unLockFile(skyFlatCommand,skyFlatDone,filename);
			return skyFlatDone;
		}
		// remove FITS lock file created in saveFitsHeaders
		if(unLockFile(skyFlatCommand,skyFlatDone,filename) == false)
			return skyFlatDone;
	// stop autoguider
		if(autoguiderStop(skyFlatCommand,skyFlatDone,true) == false)
			return skyFlatDone;
	// Test abort status.
		if(testAbort(skyFlatCommand,skyFlatDone) == true)
			return skyFlatDone;
	// Call pipeline to reduce data.
		skyFlatDone.setFilename(filename);
		if(reduceCalibrate(skyFlatCommand,skyFlatDone,filename) == false)
			return skyFlatDone;
	// set return values to indicate success.	
		skyFlatDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		skyFlatDone.setErrorString("");
		skyFlatDone.setSuccessful(true);
	// return done object.
		return skyFlatDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.14  2006/05/16 14:26:05  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.13  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.12  2001/09/24 19:33:21  cjm
// Now uses getUseTime correctly to calculate exposure length.
//
// Revision 0.11  2001/07/12 17:50:14  cjm
// autoguiderStop changes.
//
// Revision 0.10  2001/07/03 15:31:38  cjm
// Changed error code to include Ccs sub-system offset.
//
// Revision 0.9  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.8  2000/06/20 12:48:21  cjm
// CCDExposureExpose parameter change.
//
// Revision 0.7  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
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
