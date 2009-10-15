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
// MOVIEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/MOVIEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import ngat.rise.ccd.*;
import ngat.fits.FitsHeaderDefaults;
import ngat.message.base.*;
import ngat.message.ISS_INST.MOVIE;
import ngat.message.ISS_INST.MOVIE_ACK;
import ngat.message.ISS_INST.MOVIE_DONE;

/**
 * This class provides the implementation for the MOVIE command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class MOVIEImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: MOVIEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public MOVIEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.MOVIE&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.MOVIE";
	}

	/**
	 * This method gets the MOVIE command's acknowledge time. The MOVIE command takes sequence of frames until
	 * told to stop when a STOP command is sent to the CCS. Therefore it's time to complete is potentially
	 * infinite.
	 * This method returns the exposure time
	 * plus the default acknowledge time as the time to complete a MOVIE command. Before this initial period has
	 * run out, the MOVIE command implementation should send another acknowledgement (using the JMS multiple
	 * acknowledge protocol), to ensure the client does not time out. The client can expect an acknowledement
	 * every frame, therefore.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see MOVIE#getExposureTime
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		MOVIE movieCommand = (MOVIE)command;
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(movieCommand.getExposureTime()+
			serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the MOVIE command. 
	 * <ul>
	 * <li>It moves the fold mirror to the correct location.
	 * <li>It starts the autoguider.
	 * <li>It goes into a loop, this loop is terminated when a STOP command is sent to the CCS.
	 *      <ul>
	 * 	<li>It generates some FITS headers from the CCD setup and the ISS.
	 * 	<li>Get a unique filename. Save some FITS headers.
	 * 	<li>Do the exposure and save the data to disk.
	 * 	<li>Send an acknowledgement back with the filename just completed.
	 * 	<li>If an ABORT/STOP message has been sent to the CCS, come out of the loop.
	 *      </ul>
	 * <li>It stops the autoguider.
	 * </ul>
	 * Note it does <b>NOT</b> call the Real Time Data Pipeline to reduce the data.<br>
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#checkNonWindowedSetup
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		MOVIE movieCommand = (MOVIE)command;
		MOVIE_ACK movieAck = null;
		MOVIE_DONE movieDone = new MOVIE_DONE(command.getId());
		CcsStatus status = null;
		File file = null;
		String directoryString = null;
		String filename = null;
		String obsType = null;
		int frameNumber = 0;
		boolean retval = false;

		if(testAbort(movieCommand,movieDone) == true)
			return movieDone;
		if(checkNonWindowedSetup(movieDone) == false)
			return movieDone;
	// get status reference, setup exposure status/directory
		status = ccs.getStatus();
		status.setExposureCount(-1);
		status.setExposureNumber(0);
		directoryString = status.getProperty("ccs.file.fits.path");
		if(directoryString.endsWith(System.getProperty("file.separator")) == false)
			directoryString = directoryString.concat(System.getProperty("file.separator"));
		if(movieCommand.getStandard())
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_STANDARD;
		else
			obsType = FitsHeaderDefaults.OBSTYPE_VALUE_EXPOSURE;
	// move the fold mirror to the correct location
		if(moveFold(movieCommand,movieDone) == false)
			return movieDone;
		if(testAbort(movieCommand,movieDone) == true)
			return movieDone;
	// autoguider on
		if(autoguiderStart(movieCommand,movieDone) == false)
			return movieDone;
		if(testAbort(movieCommand,movieDone) == true)
		{
			autoguiderStop(movieCommand,movieDone,false);
			return movieDone;
		}
	// do exposures until told to terminate
		frameNumber = 0;
		retval = true;
		while(retval)
		{
		// Clear pause and resume times.
			status.clearPauseResumeTimes();
		// get a new filename
			filename = new String(directoryString+"movie"+frameNumber+".fits");
		// delete an old version of this filename
			file = new File(filename);
			if(file.exists())
				file.delete();
		// get fits headers
			clearFitsHeaders();
			if(setFitsHeaders(movieCommand,movieDone,obsType,movieCommand.getExposureTime()) == false)
			{
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
			if(getFitsHeadersFromISS(movieCommand,movieDone) == false)
			{
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
			if(testAbort(movieCommand,movieDone) == true)
			{
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
		// save FITS headers
			if(saveFitsHeaders(movieCommand,movieDone,filename) == false)
			{
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
		// do an exposure
			status.setExposureFilename(filename);
			try
			{
				libccd.CCDExposureExpose(true,-1,movieCommand.getExposureTime(),filename);
			}
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+
					":processCommand:CCDExposureExpose:"+command+":"+e.toString());
				movieDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1101);
				movieDone.setErrorString(e.toString());
				movieDone.setSuccessful(false);
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
		// send acknowledge to say frame is completed.
			movieAck = new MOVIE_ACK(command.getId());
			movieAck.setTimeToComplete(movieCommand.getExposureTime()+
				serverConnectionThread.getDefaultAcknowledgeTime());
			movieAck.setFilename(filename);
			try
			{
				serverConnectionThread.sendAcknowledge(movieAck);
			}
			catch(IOException e)
			{
				ccs.error(this.getClass().getName()+
					":processCommand:sendAcknowledge:"+command+":"+e.toString());
				movieDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1102);
				movieDone.setErrorString(e.toString());
				movieDone.setSuccessful(false);
				autoguiderStop(movieCommand,movieDone,false);
				return movieDone;
			}
		// are we about to abort?
			if(testAbort(movieCommand,movieDone) == true)
			{
				retval = false;
			}
		// increment frame number.
			frameNumber++;
			status.setExposureNumber(frameNumber);
		}// end while
	// autoguider off
		if(autoguiderStop(movieCommand,movieDone,true) == false)
			return movieDone;
	// setup return - note, movie frames are not reduced.
		movieDone.setCounts(0.0f);// movie's are not reduced
		movieDone.setFilename("");// movie's are not reduced
		movieDone.setSeeing(0.0f);// movie's are not reduced
		movieDone.setXpix(0.0f);// movie's are not reduced
		movieDone.setYpix(0.0f);// movie's are not reduced
		movieDone.setPhotometricity(0.0f);// movie's are not reduced
		movieDone.setSkyBrightness(0.0f);// movie's are not reduced
		movieDone.setSaturation(false);// movie's are not reduced
		movieDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		movieDone.setErrorString("");
		movieDone.setSuccessful(true);
	// return done object.
		return movieDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.20  2006/05/16 14:25:58  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.19  2003/03/26 15:40:18  cjm
// Added windowing check.
//
// Revision 0.18  2002/05/23 12:40:25  cjm
// Added defaults for extra fields in EXPOS_DONE.
//
// Revision 0.17  2001/07/12 17:49:59  cjm
// autoguiderStop changes.
//
// Revision 0.16  2001/07/12 10:38:37  cjm
// Moved setFitsHeaders and getFitsHeadersFromISS to be per-frame,
// so contained data is more accurate.
//
// Revision 0.15  2001/07/03 16:22:04  cjm
// Added Ccs error code base to error numbers.
// Changed OBSTYPE declarations.
// Changed CcsFilename type.
//
// Revision 0.14  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.13  2000/06/20 12:49:47  cjm
// CCDExposureExpose paramater change.
//
// Revision 0.12  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.11  2000/03/13 12:15:10  cjm
// Added clearing of pause and resume times.
//
// Revision 0.10  2000/02/28 19:14:00  cjm
// Backup.
//
// Revision 0.9  2000/02/22 17:05:42  cjm
// Implementation now use movieN.fits temporary files.
//
// Revision 0.8  2000/02/18 14:51:34  cjm
// Decided on when to fill EXPOSE_DONE parameters.
//
// Revision 0.7  2000/02/17 17:56:38  cjm
// Setting ExposureCount/ExposureNumber status.
//
// Revision 0.6  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.5  1999/11/01 17:53:15  cjm
// First attempt at an implementation of MOVIE.
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
