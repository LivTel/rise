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
// GLANCEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/GLANCEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.io.File;
import ngat.rise.ccd.*;
import ngat.fits.FitsHeaderDefaults;
import ngat.message.base.*;
import ngat.message.ISS_INST.GLANCE;
import ngat.message.ISS_INST.GLANCE_DONE;

/**
 * This class provides the implementation for the GLANCE command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class GLANCEImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: GLANCEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor. 
	 */
	public GLANCEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.GLANCE&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.GLANCE";
	}

	/**
	 * This method gets the GLANCE command's acknowledge time. The GLANCE command takes the exposure time
	 * plus the default acknowledge time to implement.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		GLANCE glanceCommand = (GLANCE)command;
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(glanceCommand.getExposureTime()+
			serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the GLANCE command. 
	 * <ul>
	 * <li>It generates some FITS headers from the CCD setup and
	 * the ISS and saves this to disc. The filename is a temporary one got from the &quot ccs.file.glance.tmp &quot
	 * configuration property.
	 * <li>It moves the fold mirror to the correct location.
	 * <li>It starts the autoguider.
	 * <li>It performs an exposure and saves the data from this to disc.
	 * <li>It stops the autoguider.
	 * <li>Note it does <b>NOT</b> call the Real Time Data Pipeline to reduce the data.
	 * </ul>
	 * The resultant filename or the relevant error code is put into the an object of class GLANCE_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#checkNonWindowedSetup
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see ngat.rise.ccd.CCDLibrary#CCDExposureExpose
	 * @see EXPOSEImplementation#reduceExpose
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		GLANCE glanceCommand = (GLANCE)command;
		GLANCE_DONE glanceDone = new GLANCE_DONE(command.getId());
		CcsStatus status = null;
		File file = null;
		String filename = null;
		String obsType = null;

		if(testAbort(glanceCommand,glanceDone) == true)
			return glanceDone;
		if(checkNonWindowedSetup(glanceDone) == false)
			return glanceDone;
	// get local reference to status
		status = ccs.getStatus();
	// setup exposure status.
		status.setExposureCount(1);
		status.setExposureNumber(0);
		status.clearPauseResumeTimes();
	// get fits headers
		clearFitsHeaders();
		if(glanceCommand.getStandard())
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_STANDARD;
		else
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_EXPOSURE;
		if(setFitsHeaders(glanceCommand,glanceDone,obsType,glanceCommand.getExposureTime()) == false)
			return glanceDone;
		if(getFitsHeadersFromISS(glanceCommand,glanceDone) == false)
			return glanceDone;
		if(testAbort(glanceCommand,glanceDone) == true)
			return glanceDone;
	// move the fold mirror to the correct location
		if(moveFold(glanceCommand,glanceDone) == false)
			return glanceDone;
		if(testAbort(glanceCommand,glanceDone) == true)
			return glanceDone;
	// get filename
		filename = status.getProperty("ccs.file.glance.tmp");
	// delete old file if it exists
		file = new File(filename);
		if(file.exists())
			file.delete();
	// save FITS headers
		if(saveFitsHeaders(glanceCommand,glanceDone,filename) == false)
			return glanceDone;
	// autoguider on
		if(autoguiderStart(glanceCommand,glanceDone) == false)
			return glanceDone;
		if(testAbort(glanceCommand,glanceDone) == true)
		{
			autoguiderStop(glanceCommand,glanceDone,false);
			return glanceDone;
		}
	// do glance
		status.setExposureFilename(filename);
		try
		{
			ccs.error(this.getClass().getName()+
					"Glance for "+ glanceCommand.getExposureTime() + " ms" );
			libccd.CCDExposureExpose(true,-1,glanceCommand.getExposureTime(),filename);
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
			glanceDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1000);
			glanceDone.setErrorString(e.toString());
			glanceDone.setSuccessful(false);
			autoguiderStop(glanceCommand,glanceDone,false);
			return glanceDone;
		}
		status.setExposureNumber(1);
	// autoguider off
		if(autoguiderStop(glanceCommand,glanceDone,true) == false)
			return glanceDone;
	// don't call pipeline to reduce data
		glanceDone.setCounts(0.0f);
		glanceDone.setFilename(filename);
		glanceDone.setSeeing(0.0f);
		glanceDone.setXpix(0.0f);
		glanceDone.setYpix(0.0f);
		glanceDone.setPhotometricity(0.0f);
		glanceDone.setSkyBrightness(0.0f);
		glanceDone.setSaturation(false);
		glanceDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		glanceDone.setErrorString("");
		glanceDone.setSuccessful(true);
	// return done object.
		return glanceDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.16  2006/05/16 14:25:54  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.15  2003/03/26 15:40:18  cjm
// Added windowing check.
//
// Revision 0.14  2002/05/23 12:40:25  cjm
// Added defaults for extra fields in EXPOS_DONE.
//
// Revision 0.13  2001/07/12 17:49:53  cjm
// autoguiderStop changes.
//
// Revision 0.12  2001/07/03 16:22:41  cjm
// Added Ccs error code base to error numbers.
// Changed OBSTYPE declarations.
//
// Revision 0.11  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.10  2000/06/20 12:49:07  cjm
// CCDExposureExpose parameter change.
//
// Revision 0.9  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.8  2000/03/13 12:18:17  cjm
// Added clearing of pause and resume times.
//
// Revision 0.7  2000/02/28 19:14:00  cjm
// Backup.
//
// Revision 0.6  2000/02/17 17:51:43  cjm
// Setting ExposureCount/ExposureNumber status.
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
