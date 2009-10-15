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
// SAVEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/SAVEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.io.File;
import ngat.rise.ccd.*;
import ngat.fits.FitsFilename;
import ngat.message.base.*;
import ngat.message.ISS_INST.SAVE;
import ngat.message.ISS_INST.SAVE_DONE;

/**
 * This class provides the implementation for the SAVE command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SAVEImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: SAVEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public SAVEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.SAVE&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.SAVE";
	}

	/**
	 * This method gets the SAVE command's acknowledge time. The SAVE command renames the
	 * temporary file, and then reduces the data.This takes the default acknowledge time to implement.
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
	 * This method implements the SAVE command. 
	 * <ul>
	 * <li>It checks the temporary file exists, and renames it to a &quot;real&quot; file. The temporary file
	 * is specified in the &quot;ccs.file.glance.tmp&quot; property held in the Ccs object.
	 * <li>It calls the Real Time Data Pipeline to reduce the data, if applicable.
	 * </ul>
	 * The resultant filename or the relevant error code is put into the an object of class SAVE_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see EXPOSEImplementation#reduceExpose
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		SAVE saveCommand = (SAVE)command;
		SAVE_DONE saveDone = new SAVE_DONE(command.getId());
		File temporaryFile = null;
		File newFile = null;
		String filename = null;

		if(testAbort(saveCommand,saveDone) == true)
			return saveDone;
	// get temporary filename
		filename = ccs.getStatus().getProperty("ccs.file.glance.tmp");
		temporaryFile = new File(filename);
	// does the temprary file exist?
		if(temporaryFile.exists() == false)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":file does not exist:"+filename);
			saveDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1700);
			saveDone.setErrorString("file does not exist:"+filename);
			saveDone.setSuccessful(false);
			return saveDone;
		}
	// get a filename to store frame in
		ccsFilename.nextMultRunNumber();
		try
		{
			if(saveCommand.getStandard())
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_STANDARD);
			else
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_EXPOSURE);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e);
			saveDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1702);
			saveDone.setErrorString(e.toString());
			saveDone.setSuccessful(false);
			return saveDone;
		}
		ccsFilename.nextRunNumber();
		filename = ccsFilename.getFilename();
		newFile = new File(filename);
	// test abort
		if(testAbort(saveCommand,saveDone) == true)
			return saveDone;
	// rename temporary filename to filename
		if(temporaryFile.renameTo(newFile) == false)
		{
			ccs.error(this.getClass().getName()+
				":processCommand:"+command+":failed to rename '"+
				temporaryFile.toString()+"' to '"+newFile.toString()+"'.");
			saveDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1701);
			saveDone.setErrorString("Failed to rename '"+
				temporaryFile.toString()+"' to '"+newFile.toString()+"'.");
			saveDone.setSuccessful(false);
			return saveDone;
		}
	// setup done object
		saveDone.setCounts(0.0f);
		saveDone.setFilename(filename);// this is the new filename
		saveDone.setSeeing(0.0f);
		saveDone.setXpix(0.0f);
		saveDone.setYpix(0.0f);
		saveDone.setPhotometricity(0.0f);
		saveDone.setSkyBrightness(0.0f);
		saveDone.setSaturation(false);
		saveDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		saveDone.setErrorString("");
		saveDone.setSuccessful(true);
	// test abort
		if(testAbort(saveCommand,saveDone) == true)
			return saveDone;
	// call pipeline to reduce data
	// done values should be set by this routine.
		if(saveCommand.getPipelineProcess())
		{
			if(reduceExpose(saveCommand,saveDone,filename) == false)
				return saveDone;
		}
	// return done object.
		return saveDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.11  2006/05/16 14:26:03  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.10  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.9  2002/05/23 12:40:25  cjm
// Added defaults for extra fields in EXPOS_DONE.
//
// Revision 0.8  2001/07/03 15:50:17  cjm
// Changed error code to include Ccs sub-system offset.
//
// Revision 0.7  2001/04/26 17:08:22  cjm
// Added standard flag check, and sets exposure code to standard.
//
// Revision 0.6  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
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
