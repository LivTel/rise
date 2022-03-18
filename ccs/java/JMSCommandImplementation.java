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
// JMSCommandImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/JMSCommandImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.String;
import ngat.message.base.*;

/**
 * This interface provides the generic implementation interface of a command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public interface JMSCommandImplementation
{
	/**
	 * This routine is called after the server has recieved a COMMAND sub-class that is the class of command
	 * this implementation class implements. This enables any initial startup to be performed before the 
	 * command is implemented.
	 * @param command The command passed to the server that is to be implemented.
	 */
	void init(COMMAND command);
	/**
	 * This routine is called by the server when it requires an acknowledge time to send back to the client.
	 * The implementation of this command should take less than the returned value, or else the processCommand
	 * method must call the server threads sendAcknowledge method to keep the server-client link alive.
	 * @param command The command to be implemented.
	 * @return An instance of a (sub)class of ngat.message.base.ACK, with the time, in milliseconds, 
	 * to complete the implementation of the command.
	 * @see ngat.message.base.ACK
	 */
	ACK calculateAcknowledgeTime(COMMAND command);
	/**
	 * This method is called to actually perform the implementation of the passed in command. It
	 * generates a done message that will be sent back to the client describing any errors that
	 * occured.
	 * @param command The command passed to the server for implementation.
	 * @return An object of (sub)class COMMAND_DONE is returned, with it's relevant fields filled in.
	 */
	COMMAND_DONE processCommand(COMMAND command);
}

//
// $Log: not supported by cvs2svn $
// Revision 0.5  2006/05/16 14:25:55  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.4  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.3  1999/11/01 14:48:10  cjm
// Changed calculateAcknowledgeTime methos in response to changes in ngat.net.TCPServerConnectionThread.
// It now returns an instance of ACK rather than an int.
//
// Revision 0.2  1999/10/28 10:07:46  cjm
// Added getImplementString.
//
// Revision 0.1  1999/10/27 16:25:54  cjm
// initial revision.
//
//
