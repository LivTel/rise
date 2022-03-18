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
// PAUSEImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/PAUSEImplementation.java,v 1.2 2017-07-29 15:33:46 cjm Exp $

import java.lang.*;
import java.io.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.PAUSE;
import ngat.message.ISS_INST.PAUSE_DONE;

/**
 * This class provides the implementation for the PAUSE command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class PAUSEImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: PAUSEImplementation.java,v 1.2 2017-07-29 15:33:46 cjm Exp $");

	/**
	 * Constructor.
	 */
	public PAUSEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.PAUSE&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.PAUSE";
	}

	/**
	 * This method gets the PAUSE command's acknowledge time. This takes the default acknowledge time to implement.
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
	 * This method implements the PAUSE command. 
	 * An object of class PAUSE_DONE is returned.
	 * The following actions are performed:
	 * <ul>
	 * <li>If the libccd status is not exposing, an error is returned.
	 * <li>A pause exposure command is sent to the controller.
	 * <li>The current system time is added to a list of pause times.
	 * <li>An acknowledge message is returned to the current command threads client 
	 * 	(assumed to be the thread performing the exposure) with timeToComplete 0, meaning
	 * 	the client should wait infinitely long for the exposure to be completed.
	 * <li>A successful done is setup.
	 * </ul>
	 * If an error occurs during these operations, the done message is setup accordingly and the method returns.
	 * @param command The command to be performed (an instance of PAUSE).
	 * @return An instance of PAUSE_DONE is returned, with it's field set dependant on
	 * 	whether the pause was successful.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		ACK acknowledge = null;
		PAUSE_DONE pauseDone = new PAUSE_DONE(command.getId());
		CcsStatus status = null;
		CcsTCPServerConnectionThread commandThread = null;
		int exposureStatus =  CCDLibrary.CCD_EXPOSURE_STATUS_NONE;

		status = ccs.getStatus();
	// ensure we are exposing
		exposureStatus = libccd.CCDExposureGetExposureStatus();
	// Note we check for STATUS_EXPOSE only, even though we are still exposing in STATUS_PRE_READOUT.
	// This is because we are about to start reading out in PRE_READOUT, and trying to pause
	// after the readout has started will cause the controller to lock up.
		if(exposureStatus != CCDLibrary.CCD_EXPOSURE_STATUS_EXPOSE)
		{
			ccs.error(this.getClass().getName()+"Pause attempted whilst not in Exposure mode:"+
				exposureStatus);
			pauseDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1300);
			pauseDone.setErrorString("Pause attempted whilst not in Exposure mode:"+exposureStatus);
			pauseDone.setSuccessful(false);
			return pauseDone;
		}
	// Send pause exposure command to controller to close shutter.
	// get UTC time the pause was started
		status.addPauseTime(System.currentTimeMillis());
	// issue an ACK to the client on the current thread with timeToComplete 0 to 
	// force socket timout into an infinite wait mode.
		commandThread = (CcsTCPServerConnectionThread)(status.getCurrentThread());
		if(commandThread != null)
		{
			acknowledge = new ACK(command.getId());
			acknowledge.setTimeToComplete(0);
			try
			{
				commandThread.sendAcknowledge(acknowledge);
			}
			catch(IOException e)
			{
				ccs.error(this.getClass().getName()+"Pause:Sending acknowledge to client failed:"+e);
				pauseDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1302);
				pauseDone.setErrorString("Pause:Sending acknowledge to client failed:"+e);
				pauseDone.setSuccessful(false);
				return pauseDone;
			}
		}
	// Setup successful DONE message.
		pauseDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		pauseDone.setErrorString("");
		pauseDone.setSuccessful(true);
	// return done object.
		return pauseDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.11  2006/05/16 14:26:00  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.10  2002/12/16 17:00:27  cjm
// Changed to match exposure status changes.
//
// Revision 0.9  2001/07/03 16:03:55  cjm
// Added Ccs error code base to error numbers.
//
// Revision 0.8  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.7  2000/03/08 15:11:30  cjm
// Initial implementation.
//
// Revision 0.6  1999/11/04 15:46:40  cjm
// Added comment amount setting socket timeout.
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
