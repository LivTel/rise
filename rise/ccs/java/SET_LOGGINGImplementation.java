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
// SET_LOGGINGImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/SET_LOGGINGImplementation.java,v 1.2 2010-03-26 14:38:29 cjm Exp $

import java.lang.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.SET_LOGGING;
import ngat.message.ISS_INST.SET_LOGGING_DONE;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the SET_LOGGING command sent to a server using the
 * Java Message System. It extends SETUPImplementation.
 * @see SETUPImplementation
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class SET_LOGGINGImplementation extends SETUPImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: SET_LOGGINGImplementation.java,v 1.2 2010-03-26 14:38:29 cjm Exp $");

	/**
	 * Constructor.
	 */
	public SET_LOGGINGImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.SET_LOGGING&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.SET_LOGGING";
	}

	/**
	 * This method gets the SET_LOGGING command's acknowledge time.
	 * This involves changing one number, so the default acknowledge time is returned.
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
	 * This method implements the SET_LOGGING command. This just involves changing the log level held
	 * in the CcsStatus class.
	 * An object of class SET_LOGGING_DONE is returned.
	 * @see CcsStatus#setLogLevel
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		SET_LOGGING setLoggingCommand = (SET_LOGGING)command;
		SET_LOGGING_DONE setLoggingDone = new SET_LOGGING_DONE(command.getId());

	// log whats level we are setting
		ccs.log(Logging.VERBOSITY_INTERMEDIATE,"Command:"+setLoggingCommand.getClass().getName()+
			":Level:"+setLoggingCommand.getLevel()+".");
	// set log level
		ccs.getStatus().setLogLevel(setLoggingCommand.getLevel());
		ccs.setLogLevelFilter(setLoggingCommand.getLevel());
	// setup done object.
		setLoggingDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		setLoggingDone.setErrorString("");
		setLoggingDone.setSuccessful(true);
	// return done object.
		return setLoggingDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.8  2006/05/16 14:26:04  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.7  2002/09/12 17:14:10  cjm
// Added logging to SET_LOGGING.
//
// Revision 0.6  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
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
