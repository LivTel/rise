// ReadOutThread.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccd/java/ReadOutThread.java,v 1.3 1999-09-08 10:52:40 cjm Exp $
import java.lang.*;
import java.io.*;

/**
 * This class extends thread to support the readout of a CCD camera using the SDSU CCD Controller/libccd/CCDLibrary
 * in a separate thread, so that it may be aborted by the main program whilst it is underway.
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
class ReadOutThread extends Thread
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: ReadOutThread.java,v 1.3 1999-09-08 10:52:40 cjm Exp $");
	/**
	 * CCDLibrary object, the library object used to interface with the SDSU CCD Controller
	 * @see CCDLibrary
	 */
	private CCDLibrary libccd = null;
	/**
	 * Private copy of variable to be passed into 
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a>.
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 */
	private String filename = null;
	/**
	 * Private copy of the Return value of 
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a>.
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 */
	private boolean returnValue = false;

	/**
	 * Constructor of the thread. Copys all the parameters, ready to pass them into
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a> when the thread is run.
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 */
	public ReadOutThread(CCDLibrary libccd,String filename)
	{
		this.libccd = libccd;
		if(filename != null)
			this.filename = new String(filename);
		else
			this.filename = null;
	}

	/**
	 * Run method of the thread. Calls
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a> with the parameters passed into 
	 * the constructor. This causes the CCD to expose. The success or failure of the
	 * operation is stored in <a href="#returnValue">returnValue</a>, which can be reteived using the 
	 * <a href="#getReturnValue">getReturnValue</a> method. Exposure can be aborted using the
	 * <a href="#abort">abort</a> method.
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 * @see #getReturnValue
	 * @see #abort
	 */
	public void run()
	{
		returnValue = libccd.CCDExposureReadOutCCD(filename);
	}

	/**
	 * This method will terminate a partly completed Read Out. 
	 * <a href="CCDLibrary.html#CCDExposureAbortReadout">CCDExposureAbortReadout</a> is called which stops the CCD.
	 * reading out. 
	 * <a href="CCDLibrary.html#CCDDSPGetExposureStatus">CCDDSPGetExposureStatus</a> is used to determine
	 * the current state of the exposure.
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a> to stop what it is doing. 
	 * This causes
	 * the <a href="#run">run</a> method to finish executing, and the <a href="#returnValue">returnValue</a>
	 * will be false
	 * @see CCDLibrary#CCDDSPGetExposureStatus
	 * @see CCDLibrary#CCDExposureAbortReadout
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 * @see #getReturnValue
	 * @see #run
	 */
	public void abort()
	{
		int exposureStatus = 0;

		returnValue = false;
		exposureStatus = libccd.CCDDSPGetExposureStatus();
		if(exposureStatus == libccd.CCD_DSP_EXPOSURE_STATUS_READOUT)
			libccd.CCDExposureAbortReadout();
		else
			stop();
	}

	/**
	 * This returns the return value generated by 
	 * <a href="CCDLibrary.html#CCDExposureReadOutCCD">CCDExposureReadOutCCD</a>
	 * in the <a href="#run">run</a> method. If the thread hasn't been run yet it returns false. If the exposure 
	 * was successfully completed it returns true, otherwise it returns false.
	 * @see CCDLibrary#CCDExposureReadOutCCD
	 * @see #run
	 */
	public boolean getReturnValue()
	{
		return returnValue;
	}
}
 
//
// $Log: not supported by cvs2svn $
// Revision 1.2  1999/05/28 09:54:18  dev
// "Name
//
// Revision 1.1  1999/02/23 11:08:00  dev
// Initial revision
//
//
