// AbortThread.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccd/java/AbortThread.java,v 0.2 1999-05-20 16:37:58 dev Exp $
import java.io.*;

/**
 * This class is a thread which when run, looks for a keypress on System.in, and then calls the parents
 * abort method.
 * @author Chris Mottram
 * @version $Revision: 0.2 $
 */
class AbortThread extends Thread
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: AbortThread.java,v 0.2 1999-05-20 16:37:58 dev Exp $");
	/**
	 * Parent class, call it's abort method if a keypress is detected.
	 */
	private Test parent = null;

	/**
	 * Constructor for the thread. 
	 * @param test The parent object whose abort method is called if a keypress is detected.
	 */
	public AbortThread(Test test)
	{
		this.parent = test;
	}

	/**
	 * Run method for this thread. The thread yields while there is no character in System.in's buffer.
	 * If a keypress is detected it is read, and then the parents abort method is called.
	 */
	public void run()
	{
		int retval;

		try
		{
			while(System.in.available() == 0)
				Thread.yield();
			retval = System.in.read();
		}
		catch (IOException e)
		{
			System.err.println(this.getClass().getName()+":run:available/read:"+e);
			retval = -1;
		}
		if((retval >= 0)&&(parent != null))
			parent.abort();
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.1  1999/01/27 10:54:30  dev
// initial revision
//
//
//
