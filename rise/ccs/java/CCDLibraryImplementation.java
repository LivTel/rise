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
// CCDLibraryImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CCDLibraryImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.message.base.*;
import ngat.rise.ccd.*;

/**
 * This class provides the generic implementation of commands that use libccd to send commands to the
 * SDSU CCD Controller. 
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CCDLibraryImplementation extends CommandImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CCDLibraryImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * A reference to the CCDLibrary class instance used to communicate with the SDSU CCD Controller.
	 */
	protected CCDLibrary libccd = null;

	/**
	 * This method calls the super-classes method. It then tries to fill in the reference to the libccd
	 * object.
	 * @param command The command to be implemented.
	 * @see #libccd
	 * @see Ccs#getLibccd
	 */
	public void init(COMMAND command)
	{
		super.init(command);
		if(ccs != null)
			libccd = ccs.getLibccd();
	}

	/**
	 * This method is used to calculate how long an implementation of a command is going to take, so that the
	 * client has an idea of how long to wait before it can assume the server has died.
	 * @param command The command to be implemented.
	 * @return The time taken to implement this command, or the time taken before the next acknowledgement
	 * is to be sent.
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		return super.calculateAcknowledgeTime(command);
	}

	/**
	 * This routine performs the generic command implementation.
	 * @param command The command to be implemented.
	 * @return The results of the implementation of this command.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		return super.processCommand(command);
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.4  2006/05/16 14:25:40  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.3  1999/11/01 15:53:41  cjm
// Changed calculateAcknowledgeTime to return ACK rather than an int.
// This is to keep up to date with the changes to ngat.net.TCPServerConnectionThread class.
//
// Revision 0.2  1999/10/27 16:47:25  cjm
// Changed definition of RCSID so that file Ids are picked up properly.
//
// Revision 0.1  1999/10/27 16:25:54  cjm
// initial revision.
//
//
