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
// STARFOCUSImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/STARFOCUSImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.STARFOCUS;
import ngat.message.ISS_INST.STARFOCUS_DONE;

/**
 * This class provides the implementation for the STARFOCUS command sent to a server using the
 * Java Message System. It extends SETUPImplementation. This command requires the instrument to make any 
 * adjustments to its internal focusing mechanisms to focus the image of a star. 
 * The SDSU CCD Controller has no such internal focusing mechanisms,
 * so the implementation just consists of returning success.
 * @see SETUPImplementation
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class STARFOCUSImplementation extends SETUPImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: STARFOCUSImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public STARFOCUSImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.STARFOCUS&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.STARFOCUS";
	}

	/**
	 * This method gets the STARFOCUS command's acknowledge time.
	 * This command is not implemented on the CCS, so it returns the min acknowledge time.
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
	 * This method implements the STARFOCUS command. The SDSU CCD Controller has no internal focusing mechanisms,
	 * so this involves doing nothing for the CCS, and returning success.
	 * An object of class STARFOCUS_DONE is returned.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		STARFOCUS_DONE starFocusDone = new STARFOCUS_DONE(command.getId());

		starFocusDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		starFocusDone.setErrorString("");
		starFocusDone.setSuccessful(true);
	// return done object.
		return starFocusDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.6  2006/05/16 14:26:06  cjm
// gnuify: Added GNU General Public License.
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
