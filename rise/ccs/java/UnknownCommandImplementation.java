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
// UnknownCommandImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/UnknownCommandImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.message.base.*;

/**
 * This class provides the implementation of a command sent to a server using the
 * Java Message System. The command sent is unknown to the server in this case.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class UnknownCommandImplementation extends CommandImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: UnknownCommandImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * This method gets the unknown command's acknowledge time. This returns the server connection threads 
	 * min acknowledge time.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getMinAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(serverConnectionThread.getMinAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This routine performs the command implementation of a command that is unknown to the server.
	 * This just returns a COMMAND_DONE instance, with successful set to false and an error code.
	 * @param command The command to be implemented.
	 * @return The results of the implementation of this command.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		COMMAND_DONE done = null;

		done = new COMMAND_DONE(command.getId());
		ccs.error("Unknown Commmand:"+command.getClass().getName());
		done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+400);
		done.setErrorString("Unknown Commmand:"+command.getClass().getName());
		done.setSuccessful(false);
		return done;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.6  2006/05/16 14:26:11  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.5  2001/07/03 15:13:09  cjm
// Changed error code to include Ccs sub-system offset.
//
// Revision 0.4  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.3  1999/11/01 15:53:41  cjm
// Changed calculateAcknowledgeTime to return ACK rather than an int.
// This is to keep up to date with the changes to ngat.net.TCPServerConnectionThread class.
//
// Revision 0.2  1999/11/01 10:45:51  cjm
// Got rid of init methods that just called super-class's method.
// Added constructor to setup implement string correctly.
//
// Revision 0.1  1999/10/28 10:44:44  cjm
// initial revision.
//
//
