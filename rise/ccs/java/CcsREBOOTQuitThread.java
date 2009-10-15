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
// CcsREBOOTQuitThread.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsREBOOTQuitThread.java,v 1.1 2009-10-15 10:21:18 cjm Exp $
import java.lang.*;
import java.io.*;

/**
 * This class is a thread that is started when the Ccs is to terminate.
 * A thread is passed in, which must terminate before System.exit is called.
 * This is used in, for instance, the REBOOTImplementation, so that the 
 * REBOOT's DONE mesage is returned to the client before the Ccs is terminated.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CcsREBOOTQuitThread extends Thread
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsREBOOTQuitThread.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * The Thread, that has to terminatre before this thread calls System.exit
	 */
	private Thread waitThread = null;
	/**
	 * Field holding the instance of the ccs currently executing, used to access error handling routines etc.
	 */
	private Ccs ccs = null;

	/**
	 * The constructor.
	 * @param name The name of the thread.
	 */
	public CcsREBOOTQuitThread(String name)
	{
		super(name);
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
	 * Method to set a thread, such that this thread will not call System.exit until
	 * that thread has terminated.
	 * @param t The thread to wait for.
	 * @see #waitThread
	 */
	public void setWaitThread(Thread t)
	{
		waitThread = t;
	}

	/**
	 * Run method, called when the thread is started.
	 * If the waitThread is non-null, we try to wait until it has terminated.
	 * System.exit(0) is then called.
	 * @see #waitThread
	 */
	public void run()
	{
		if(waitThread != null)
		{
			try
			{
				waitThread.join();
			}
			catch (InterruptedException e)
			{
				ccs.error(this.getClass().getName()+":run:",e);
			}
		}
		System.exit(0);
	}
}
//
// $Log: not supported by cvs2svn $
// Revision 1.3  2006/05/16 14:25:43  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.2  2001/05/15 13:30:45  cjm
// Changed error call.
//
// Revision 1.1  2001/03/09 16:16:09  cjm
// Initial revision
//
//
