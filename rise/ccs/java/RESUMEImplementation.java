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
// RESUMEImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/RESUMEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.io.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.RESUME;
import ngat.message.ISS_INST.RESUME_DONE;

/**
 * This class provides the implementation for the RESUME command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class RESUMEImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: RESUMEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public RESUMEImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.RESUME&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.RESUME";
	}

	/**
	 * This method gets the RESUME command's acknowledge time.
	 * This takes the default acknowledge time to implement.
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
	 * This method implements the RESUME command. 
	 * An object of class RESUME_DONE is returned.
	 * The following actions are performed:
	 * <ul>
	 * <li>No check is made as to whether we are paused, as we do not hold this state information.
	 * 	<b>This needs to be added.</b> 
	 * <li>A resume exposure command is sent to the controller.
	 * <li>The current system time is added to a list of pause times.
	 * <li>An acknowledge message is returned to the current command threads client 
	 * 	(assumed to be the thread performing the exposure) with timeToComplete the original
	 * 	acknowledge time, so that the client will time out if the server stops for some reason.
	 * <li>A successful done is setup.
	 * </ul>
	 * If an error occurs during these operations, the done message is setup accordingly and the method returns.
	 * @param command The command to be performed (an instance of RESUME).
	 * @return An instance of RESUME_DONE is returned, with it's field set dependant on
	 * 	whether the resume was successful.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		RESUME_DONE resumeDone = new RESUME_DONE(command.getId());
		ACK acknowledge = null;
		CcsStatus status = null;
		CcsTCPServerConnectionThread commandThread = null;

		status = ccs.getStatus();
	// Send resume exposure command to controller to open shutter.
		try
		{
			libccd.CCDDSPCommandREX();
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+"Resume command failed:"+e);
			resumeDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1500);
			resumeDone.setErrorString("Resume command failed:"+e);
			resumeDone.setSuccessful(false);
			return resumeDone;
		}
	// get UTC time of resume
		status.addResumeTime(System.currentTimeMillis());
	// issue an ACK to the client on the current thread with timeToComplete back to what it was before
	// the pause occured. This is stored in the CcsTCPServerConnectionThread.
		commandThread = (CcsTCPServerConnectionThread)(status.getCurrentThread());
		if(commandThread != null)
		{
			acknowledge = new ACK(command.getId());
			acknowledge.setTimeToComplete(commandThread.getAcknowledgeTime());
			try
			{
				commandThread.sendAcknowledge(acknowledge);
			}
			catch(IOException e)
			{
				ccs.error(this.getClass().getName()+"Resume:Sending acknowledge to client failed:"+e);
				resumeDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1501);
				resumeDone.setErrorString("Resume:Sending acknowledge to client failed:"+e);
				resumeDone.setSuccessful(false);
				return resumeDone;
			}
		}
	// Setup successful DONE message.
		resumeDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		resumeDone.setErrorString("");
		resumeDone.setSuccessful(true);
	// return done object.
		return resumeDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.9  2006/05/16 14:26:01  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.8  2001/07/03 16:01:24  cjm
// Added Ccs error code base to error numbers.
//
// Revision 0.7  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.6  2000/03/08 15:11:36  cjm
// Initial implementation.
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
