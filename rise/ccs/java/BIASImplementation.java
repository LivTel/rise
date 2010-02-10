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
// BIASImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/BIASImplementation.java,v 1.2 2010-02-10 11:03:07 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.BIAS_DONE;

/**
 * This class provides the implementation for the BIAS command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class BIASImplementation extends CALIBRATEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: BIASImplementation.java,v 1.2 2010-02-10 11:03:07 cjm Exp $");

	/**
	 * Constructor.
	 */
	public BIASImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.BIAS&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.BIAS";
	}

	/**
	 * This method gets the BIAS command's acknowledge time. The BIAS command has no exposure time, 
	 * so this returns the server connection threads default acknowledge time if available.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the BIAS command. It generates some FITS headers from the CCD setup and
	 * the ISS and saves this to disc. It performs a bias exposure and saves the data from this to disc.
	 * It sends the generated FITS data to the Real Time Data Pipeline to get some data from it.
	 * The resultant data or the relevant error code is put into the an object of class BIAS_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#checkNonWindowedSetup
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see FITSImplementation#unLockFile
	 * @see ngat.ccd.CCDLibrary#CCDExposureBias
	 * @see CALIBRATEImplementation#reduceCalibrate
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		BIAS_DONE biasDone = new BIAS_DONE(command.getId());
		String filename = null;

		if(testAbort(command,biasDone) == true)
			return biasDone;
		if(checkNonWindowedSetup(biasDone) == false)
			return biasDone;
	// fits headers
		clearFitsHeaders();
		if(setFitsHeaders(command,biasDone,FitsHeaderDefaults.OBSTYPE_VALUE_BIAS,0) == false)
			return biasDone;
		if(getFitsHeadersFromISS(command,biasDone) == false)
			return biasDone;
		if(testAbort(command,biasDone) == true)
			return biasDone;
	// get a filename to store frame in
		ccsFilename.nextMultRunNumber();
		try
		{
			ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_BIAS);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":"+e.toString());
			biasDone.setFilename(filename);
			biasDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+701);
			biasDone.setErrorString(e.toString());
			biasDone.setSuccessful(false);
			return biasDone;
		}
		ccsFilename.nextRunNumber();
		filename = ccsFilename.getFilename();
		if(saveFitsHeaders(command,biasDone,filename) == false)
		{
			unLockFile(command,biasDone,filename);
			return biasDone;
		}
	// do exposure
		try
		{
			libccd.CCDExposureBias(filename);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+":processCommand:"+
				command+":"+e.toString());
			biasDone.setFilename(filename);
			biasDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+700);
			biasDone.setErrorString(e.toString());
			biasDone.setSuccessful(false);
			unLockFile(command,biasDone,filename);
			return biasDone;
		}
		// remove lock files created in saveFitsHeaders
		if(unLockFile(command,biasDone,filename) == false)
			return biasDone;
	// Test abort status.
		if(testAbort(command,biasDone) == true)
			return biasDone;
	// Call pipeline to reduce data.
		if(reduceCalibrate(command,biasDone,filename) == false)
			return biasDone;
	// set the done object to indicate success.
		biasDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		biasDone.setErrorString("");
		biasDone.setSuccessful(true);
	// return done object.
		return biasDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.11  2006/05/16 14:25:38  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.10  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.9  2003/03/26 15:40:18  cjm
// Added windowing check.
//
// Revision 0.8  2001/07/03 16:36:48  cjm
// Added Ccs base error code to error numbers.
// Changed CcsFilename references to ngat.fits.FitsFilename.
//
// Revision 0.7  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.6  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
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
