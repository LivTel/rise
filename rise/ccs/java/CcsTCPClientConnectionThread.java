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
// CcsTCPClientConnectionThread.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsTCPClientConnectionThread.java,v 1.2 2010-03-26 14:38:29 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Date;

import ngat.net.*;
import ngat.message.base.*;
import ngat.util.logging.*;

/**
 * The CcsTCPClientConnectionThread extends TCPClientConnectionThread. 
 * It implements the generic ISS/DP(RT) instrument command protocol with multiple acknowledgements. 
 * The CCS starts one of these threads each time
 * it wishes to send a message to the ISS/DP(RT).
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class CcsTCPClientConnectionThread extends TCPClientConnectionThreadMA
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsTCPClientConnectionThread.java,v 1.2 2010-03-26 14:38:29 cjm Exp $");
	/**
	 * The commandThread was spawned by the Ccs to deal with a Ccs command request. As part of the running of
	 * the commandThread, this client connection thread was created. We need to know the server thread so
	 * that we can pass back any acknowledge times from the ISS/DpRt back to the Ccs client (ISS/CcsGUI etc).
	 */
	private CcsTCPServerConnectionThread commandThread = null;
	/**
	 * The Ccs object.
	 */
	private Ccs ccs = null;

	/**
	 * A constructor for this class. Currently just calls the parent class's constructor.
	 * @param address The internet address to send this command to.
	 * @param portNumber The port number to send this command to.
	 * @param c The command to send to the specified address.
	 * @param ct The Ccs command thread, the implementation of which spawned this command.
	 */
	public CcsTCPClientConnectionThread(InetAddress address,int portNumber,COMMAND c,
		CcsTCPServerConnectionThread ct)
	{
		super(address,portNumber,c);
		commandThread = ct;
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
	 * This routine processes the acknowledge object returned by the server. It
	 * prints out a message, giving the time to completion if the acknowledge was not null.
	 * It sends the acknowledgement to the Ccs client for this sub-command of the command,
	 * so that the Ccs's client does not time out if,say, a zero is returned.
	 * @see CcsTCPServerConnectionThread#sendAcknowledge
	 * @see #commandThread
	 */
	protected void processAcknowledge()
	{
		if(acknowledge == null)
		{
			ccs.error(this.getClass().getName()+":processAcknowledge:"+
				command.getClass().getName()+":acknowledge was null.");
			return;
		}
		ccs.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+":processAcknowledge:Command:"+
			command.getClass().getName()+" sent ACK with time to complete "+
			acknowledge.getTimeToComplete()+".");
	// send acknowledge to Ccs client.
		try
		{
			commandThread.sendAcknowledge(acknowledge);
		}
		catch(IOException e)
		{
			ccs.error(this.getClass().getName()+":processAcknowledge:"+
				command.getClass().getName()+":sending acknowledge to client failed:",e);
		}
	}

	/**
	 * This routine processes the done object returned by the server. 
	 * It prints out the basic return values in done.
	 */
	protected void processDone()
	{
		ACK acknowledge = null;

		if(done == null)
		{
			ccs.error(this.getClass().getName()+":processDone:"+
				command.getClass().getName()+":done was null.");
			return;
		}
	// construct an acknowledgement to sent to the Ccs client to tell it how long to keep waiting
	// it currently returns the time the Ccs origianally asked for to complete this command
	// This is because the Ccs assumed zero time for all sub-commands.
		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(commandThread.getAcknowledgeTime());
		try
		{
			commandThread.sendAcknowledge(acknowledge);
		}
		catch(IOException e)
		{
			ccs.error(this.getClass().getName()+":processDone:"+
				command.getClass().getName()+":sending acknowledge to client failed:",e);
		}
	}
}
//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.10  2006/05/16 14:25:45  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.9  2004/11/23 15:23:41  cjm
// Added ACK logging.
//
// Revision 0.8  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 0.7  2001/02/16 11:11:08  cjm
// Removed spurious prints to error log.
//
// Revision 0.6  1999/12/07 12:18:25  cjm
// Added extra functionality so that ISS and DpRt acknowledgements are sent back to the Ccs client.
// This stops the Ccs's client timing out when the ISS or DpRt takes longer than expected.
//
// Revision 0.5  1999/09/01 10:32:51  cjm
// This class now inherits from TCPClientConnectionThreadMA rather than TCPClientConnectionThread to support multiple acknowledgement.
//
// Revision 0.4  1999/07/01 13:57:57  dev
// RCSID added and Log comments
//
//
