// Test.java
// $Header: /space/home/dev/src/ccd/java/RCS/Test.java,v 0.12 2003/03/26 15:52:25 cjm Exp $
import java.lang.*;
import java.io.*;

import ngat.rise.ccd.*;

/**
 * This is the main test program.
 * @author Chris Mottram
 * @version $Revision: 0.12 $
 */
class Test
{
	/**
	 * A CCD X size to pass into the library.
	 */
	private final static int CCD_X_SIZE 	= 1024;
	/**
	 * A CCD Y size to pass into the library.
	 */
	private final static int CCD_Y_SIZE 	= 1024;
	/**
	 * A CCD X bin size to pass into the library.
	 */
	private final static int CCD_XBIN_SIZE 	= 1;
	/**
	 * A CCD Y bin size to pass into the library.
	 */
	private final static int CCD_YBIN_SIZE 	= 1;
	/**
	 * The CCDLibrary object that handles communication with the low level librise_ccd library.
	 */
	private CCDLibrary libccd = null;
	/**
	 * A Thread to manage the setup of the CCD.
	 */
	private SetupThread setupThread = null;
	/**
	 * A Thread to manage the exposure of the CCD.
	 */
	private ExposureThread exposureThread = null;
	/**
	 * A boolean determining whether to do an exposure or not.
	 */
	private boolean doExpose = false;
	/**
	 * A boolean determining whether to get the ccd's current temperature.
	 */
	private boolean doTemperature = false;
	/**
	 * A boolean determining whether the exposure thread was aborted.
	 */
	private boolean exposureAborted = false;

	/**
	 * Initialisation routine. Sets up the libccd library interface. 
	 * @exception CCDLibraryNativeException Thrown if CCDInterfaceOpen fails.
	 * @see #libccd
	 * @see #interfaceDevice
	 */
	public void init() throws CCDLibraryNativeException
	{
		libccd = new CCDLibrary();
	}

	/**
	 * Routine to start a setup thread to setup the ccd camera.
	 * @see #setupThread
	 */
	public boolean setup()
	{
		CCDLibraryNativeException setupException = null;
		AbortThread abortThread = null;
		boolean retval = false;
		int errorNumber = 0;

		abortThread = new AbortThread(this);
		abortThread.setPriority(Thread.NORM_PRIORITY-1);
		setupThread = new SetupThread(libccd,-40.0,CCD_X_SIZE,CCD_Y_SIZE,CCD_XBIN_SIZE,CCD_YBIN_SIZE);
		setupThread.setPriority(Thread.NORM_PRIORITY-1);
		setupThread.start();
		abortThread.start();
		while(setupThread.isAlive())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				System.err.println(this.getClass().getName()+e);
			}
		}
		abortThread.quit();
		setupException = setupThread.getSetupException();
		if(setupException == null)
		{
			System.out.println("Test:Setup Completed.");
			retval = true;
		}
		else
		{
			System.err.println("Test:Setup returned:"+setupException);
			retval = false;
		}
		return retval;
	}

	/**
	 * Routine to get the temperature from the ccd.
	 */
	public void getTemperature()
	{
		CCDLibraryDouble temperature = null;

		temperature = new CCDLibraryDouble();
		try
		{
			libccd.CCDTemperatureGet(temperature);
			System.out.println("Test:CCDTemperatureGet:"+temperature.getValue());
		}
		catch(CCDLibraryNativeException e)
		{
			System.err.println(e.toString());
		}
	}

	/**
	 * Routine to start a thread to perform an exposure.
	 * @see #exposureThread
	 */
	public void expose()
	{
		AbortThread abortThread = null;
		Exception exposeException = null;
		int errorNumber = 0;

		abortThread = new AbortThread(this);
		abortThread.setPriority(Thread.NORM_PRIORITY-1);
		exposureThread = new ExposureThread(libccd,true,
			CCD_X_SIZE/CCD_XBIN_SIZE,CCD_Y_SIZE/CCD_YBIN_SIZE,
			10000,new String("test.fits"));
		exposureThread.setPriority(Thread.NORM_PRIORITY-1);
		exposureThread.start();
		abortThread.start();
		exposureAborted = false;
		while(exposureThread.isAlive())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				System.err.println(this.getClass().getName()+e);
			}
		}
		abortThread.quit();
		exposeException = exposureThread.getExposeException();
		exposureAborted = exposureThread.getAbortStatus();
		if(exposeException == null)
			System.out.println("Test:CCDExposureExpose Completed.");
		else
		{
			if(exposureAborted)
			{
				System.out.println("Test:CCDExposureExpose was Aborted");
				System.err.println("Test:CCDExposureExpose returned:"+exposeException);
			}
			else
				System.err.println("Test:CCDExposureExpose returned:"+exposeException);
		}
	}

	/**
	 * Routine called from the abort thread, to abort the operation currently taking place.
	 * @see SetupThread#abort
	 * @see ExposureThread#abort
	 * @see ReadOutThread#abort
	 */
	public void abort() throws CCDLibraryNativeException
	{
		if(setupThread != null)
		{
			if(setupThread.isAlive())
				setupThread.abort();
		}
		if(exposureThread != null)
		{
			if(exposureThread.isAlive())
				exposureThread.abort();
		}
	}
	
	/**
	 * Routine to close the libccd interface.
	 */
	public void close()
	{
	}

	/**
	 * Routine to parse arguments.
	 * @see #doTemperature
	 * @see #doExpose
	 */
	public void parseArgs(String []args)
	{
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-temperature")||args[i].equals("-t"))
			{
				doTemperature = true;
			}
			else if(args[i].equals("-expose")||args[i].equals("-e"))
			{
				doExpose = true;
			}
			else
			{
				System.out.println(this.getClass().getName()+" Help:");
				System.out.println(this.getClass().getName()+" test the CCD Controller library");
				System.out.println("jre -cp <pathname of classes> "+this.getClass().getName()+
					" [Options]");
				System.out.println("Options are:");
				System.out.println("\t-t[emperature] - Get current CCD temperature.");
				System.out.println("\t-e[xpose] - Do an exposure.");
				System.out.println("\t-h[elp] - display this help.");
				System.exit(0);
			}
		}
	}

	/**
	 * Main routine of program. Creates an instance of test,parses arguments, initialises it,
	 * calls the camera setup, and optionally does an exposure/takes the ccd temperature.
	 * @see #init
	 * @see #parseArgs
	 * @see #setup
	 * @see #getTemperature
	 * @see #expose
	 * @see #close
	 */
	public static void main(String[] args)
	{
		Test test = new Test();

		test.parseArgs(args);
		try
		{
			test.init();
		}
		catch(CCDLibraryNativeException e)
		{
			System.err.println("Test:init failed:"+e);
			System.exit(1);
		}
		if(!test.setup())
		{
			test.close();
			System.exit(1);
		}
		if(test.doTemperature)
			test.getTemperature();
		if(test.doExpose)
		{
			test.expose();
		}
		test.close();

		System.out.println("Finished Test ...");
		System.exit(0);
	}
}
//
// $Log: Test.java,v $
// Revision 0.12  2003/03/26 15:52:25  cjm
// Changed for windowing API change.
//
// Revision 0.11  2001/01/31 17:03:27  cjm
// Added interface selection capability/changed setup to load DSP code from .lod files.
//
// Revision 0.10  2000/02/03 16:59:21  cjm
// Changed for new ngat.ccd.CCLibrary interface to setup methods.
//
// Revision 0.9  1999/09/20 14:39:48  cjm
// Changed due to libccd native routines throwing CCDLibraryNativeException when errors occur.
//
// Revision 0.8  1999/09/10 15:55:30  cjm
// Changed due to CCDLibrary moving to ngat.ccd. package.
//
// Revision 0.7  1999/09/08 10:52:40  cjm
// Trying to fix file permissions of these files.
//
//
