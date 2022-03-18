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
// SETUPImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/SETUPImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.SETUP_DONE;

/**
 * This class provides the generic implementation for SETUP commands sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SETUPImplementation extends FITSImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: SETUPImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * This method gets the SETUP command's acknowledge time. It returns the server connection 
	 * threads min acknowledge time. This method should be over-written in sub-classes.
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
	 * This method is a generic implementation for the SETUP command, that does nothing.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
	       	// do nothing 
		SETUP_DONE setupDone = new SETUP_DONE(command.getId());

		setupDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		setupDone.setErrorString("");
		setupDone.setSuccessful(true);
		return setupDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.5  2006/05/16 14:26:04  cjm
// gnuify: Added GNU General Public License.
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
