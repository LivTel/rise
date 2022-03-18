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
// CommandImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CommandImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.message.base.*;
import ngat.message.ISS_INST.INTERRUPT;

/**
 * This class provides the generic implementation of a command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CommandImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CommandImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * The Ccs object.
	 */
	protected Ccs ccs = null;
	/**
	 * Reference to the Ccs thread running the implementation of this command.
	 */
	protected CcsTCPServerConnectionThread serverConnectionThread = null;

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;null&quot;, as this class implements no message class.
	 * @return A string, the classname of the class of command this class implements.
	 */
	public static String getImplementString()
	{
		return null;
	}

	/**
	 * This method is called from the TCPServerConnection's init method, after the command to be 
	 * implemented has been 
	 * received. This enables us to do any setup required for the implementation before implementation 
	 * actually starts.
	 * @param command The command to be implemented.
	 */
	public void init(COMMAND command)
	{
		if(command == null)
			return;
	}

	/**
	 * This method is used to calculate how long an implementation of a command is going to take, so that the
	 * client has an idea of how long to wait before it can assume the server has died.
	 * @param command The command to be implemented.
	 * @return An instance of class ngat.message.base.ACK, with the time taken to implement 
	 * 	this command, or the time taken before the next acknowledgement is to be sent.
	 * @see ngat.message.base.ACK
	 * @see ngat.message.base.ACK#setTimeToComplete
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(1000);
		return acknowledge;
	}

	/**
	 * This routine performs the generic command implementation.
	 * @param command The command to be implemented.
	 * @return The results of the implementation of this command.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		COMMAND_DONE done = null;

		done = new COMMAND_DONE(command.getId());
		done.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		done.setErrorString("Generic Command Implementation.");
		done.setSuccessful(true);
		return done;
	}

	/**
	 * Routine to set this objects pointer to the ccs object.
	 * @param c The ccs object.
	 */
	public void setCcs(Ccs c)
	{
		this.ccs = c;
	}

	/**
	 * Routine to set this objects pointer to the Ccs server connection thread running this commands
	 * implementation.
	 * @param s The server connection thread.
	 */
	public void setServerConnectionThread(CcsTCPServerConnectionThread s)
	{
		this.serverConnectionThread = s;
	}

	/**
	 * This routine tests the current status of the threads abortProcessCommand
	 * to see whether the operation is to be terminated. 
	 * @param command The command being implemented. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation has been aborted or not.
	 * @see CcsTCPServerConnectionThread#getAbortProcessCommand
	 * @see #serverConnectionThread
	 */
	public boolean testAbort(COMMAND command,COMMAND_DONE done)
	{
		boolean abortProcessCommand = false;

		if(serverConnectionThread != null)
		{
			abortProcessCommand = serverConnectionThread.getAbortProcessCommand();
			if(abortProcessCommand)
			{
				String s = new String("Command "+command.getClass().getName()+
						" Operation Aborted.");
				ccs.error(s);
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+200);
				done.setErrorString(s);
				done.setSuccessful(false);
			}
		}
		return abortProcessCommand;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.9  2006/05/16 14:25:48  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.8  2001/07/03 16:29:30  cjm
// Added Ccs base error number to error numbers.
//
// Revision 0.7  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.6  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.5  1999/11/02 11:53:17  cjm
// Deleted serverConnectionThread.setPriority from init method.
// This is currently done in the CcsTCPServerConnectionThread.
//
// Revision 0.4  1999/11/01 14:48:08  cjm
// Changed calculateAcknowledgeTime methos in response to changes in ngat.net.TCPServerConnectionThread.
// It now returns an instance of ACK rather than an int.
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
