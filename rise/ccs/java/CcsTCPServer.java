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
// CcsTCPServer.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsTCPServer.java,v 1.1 2009-10-15 10:21:18 cjm Exp $
import java.lang.*;
import java.io.*;
import java.net.*;

import ngat.net.*;

/**
 * This class extends the TCPServer class for the Ccs application.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CcsTCPServer extends TCPServer
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsTCPServer.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * Field holding the instance of the ccs currently executing, so we can pass this to spawned threads.
	 */
	private Ccs ccs = null;

	/**
	 * The constructor.
	 */
	public CcsTCPServer(String name,int portNumber)
	{
		super(name,portNumber);
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
	 * This routine spawns threads to handle connection to the server. This routine
	 * spawns <a href="CcsTCPServerConnectionThread.html">CcsTCPServerConnectionThread</a> threads.
	 * The routine also sets the new threads priority to higher than normal. This makes the thread
	 * reading it's command a priority so we can quickly determine whether the thread should
	 * continue to execute at a higher priority.
	 */
	public void startConnectionThread(Socket connectionSocket)
	{
		CcsTCPServerConnectionThread thread = null;

		thread = new CcsTCPServerConnectionThread(connectionSocket);
		thread.setCcs(ccs);
		thread.setPriority(ccs.getStatus().getThreadPriorityInterrupt());
		thread.start();
	}

}

// $Log: not supported by cvs2svn $
// Revision 1.9  2006/05/16 14:25:47  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.8  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 1.7  2000/07/10 15:05:02  cjm
// Changed setPriority calls to use priorities from the configuration files (CcsStatus).
//
// Revision 1.6  1999/06/09 16:52:36  dev
// thread abort procedure improvements and error/log file implementation
//
// Revision 1.5  1999/06/08 16:50:53  dev
// thread priority changes
//
// Revision 1.4  1999/05/28 09:54:34  dev
// "Name
//
// Revision 1.3  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.2  1999/03/19 11:50:05  dev
// Backup
//
// Revision 1.1  1999/03/16 17:03:32  dev
// Backup
//
