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
// SicfTCPServer.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/SicfTCPServer.java,v 1.1 2009-10-15 10:19:32 cjm Exp $
import java.lang.*;
import java.io.*;
import java.net.*;

import ngat.net.*;

/**
 * This class extends the TCPServer class for the SendISSCommandFile application. The SendISSCommandFile sends
 * commands to the Ccs to test it's execution. Some Ccs commands involve sending commands back to the ISS, and
 * this class is designed to catch these requests and to spawn a SicfTCPServerConnectionThread to deal with them.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SicfTCPServer extends TCPServer
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: SicfTCPServer.java,v 1.1 2009-10-15 10:19:32 cjm Exp $");
	/**
	 * Field holding the instance of the controller object currently executing this server, 
	 * so we can pass this to spawned threads.
	 */
	private Object controller = null;

	/**
	 * The constructor.
	 */
	public SicfTCPServer(String name,int portNumber)
	{
		super(name,portNumber);
	}

	/**
	 * Routine to set this objects pointer to the controller object.
	 * @param o The controller object.
	 * @see #controller
	 */
	public void setController(Object o)
	{
		this.controller = o;
	}

	/**
	 * This routine spawns threads to handle connection to the server. This routine
	 * spawns <a href="SicfTCPServerConnectionThread.html">SicfTCPServerConnectionThread</a> threads.
	 */
	public void startConnectionThread(Socket connectionSocket)
	{
		SicfTCPServerConnectionThread thread = null;

		thread = new SicfTCPServerConnectionThread(connectionSocket);
		thread.setController(controller);
		thread.start();
	}

}

// $Log: not supported by cvs2svn $
// Revision 0.3  2006/05/16 16:54:48  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.2  2001/03/05 19:19:21  cjm
// Changed setController.
//
// Revision 0.1  1999/06/03 10:12:57  dev
// initial revision
//
