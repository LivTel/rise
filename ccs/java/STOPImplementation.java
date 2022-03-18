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
// STOPImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/STOPImplementation.java,v 1.2 2017-07-29 15:32:21 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.STOP;
import ngat.message.ISS_INST.STOP_DONE;

/**
 * This class provides the implementation for the STOP command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class STOPImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: STOPImplementation.java,v 1.2 2017-07-29 15:32:21 cjm Exp $");

	/**
	 * Constructor.
	 */
	public STOPImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.STOP&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.STOP";
	}

	/**
	 * This method gets the STOP command's acknowledge time.
	 * This takes the default acknowledge time to implement.
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
	 * This method implements the STOP command. 
	 * If the CCD Library says we are currently exposing, the abort exposure routine is called,
	 * which sends a SDSU AEX DSP command. This closes the shutter and puts the CCD in idle mode.
	 * The controller is in a state to read-out the CCD, which the STOPed exposure/calibration
	 * command should do.
	 * An object of class STOP_DONE is returned.
	 * @param command The instance of the STOP class we are implementing.
	 * @return This implementation returns an instance of STOP_DONE which success set to true.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		STOP_DONE stopDone = new STOP_DONE(command.getId());
		CcsTCPServerConnectionThread thread = null;
		CcsStatus status = null;
		String filename = null;

		status = ccs.getStatus();
	// The STOP command only stops an ongoing exposure.
	// It allows the exposure command thread to readout to disc.
	// If we are in the middle of a libccd exposure command, abort it.
		if(libccd.CCDExposureGetExposureStatus() == libccd.CCD_EXPOSURE_STATUS_EXPOSE)
		{
		// tell controller to abort exposure
			try
			{
				libccd.CCDExposureAbort();
			}
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+
						":processCommand:"+command+":"+e.toString());
				stopDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1902);
				stopDone.setErrorString(e.toString());
				stopDone.setSuccessful(false);
				return stopDone;
			}
		// tell exposure thread to stop waiting for exposure
			thread = (CcsTCPServerConnectionThread)(status.getCurrentThread());
			while((thread != null)&&(thread.isAlive()))
			{
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException e)
				{
					ccs.error(this.getClass().getName()+
						":processCommand:"+command+":"+e.toString());
				}
			}
		// get filename
			filename = status.getExposureFilename();
		// read out to filename
			try
			{
				libccd.CCDExposureReadOutCCD(filename);
			}
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+
						":processCommand:"+command+":"+e.toString());
				stopDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1900);
				stopDone.setErrorString(e.toString());
				stopDone.setSuccessful(false);
				return stopDone;
			}
		// diddly update headers

			stopDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
			stopDone.setErrorString("");
			stopDone.setSuccessful(true);
		}// end if we are exposing
		else
		{
			stopDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1901);
			stopDone.setErrorString("STOP failed:Not in exposure mode.");
			stopDone.setSuccessful(true);
		}
	// return done object.
		return stopDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.11  2006/05/16 14:26:07  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.10  2002/12/16 17:00:27  cjm
// Changed to match exposure status changes.
// Abort can now throw an exception.
//
// Revision 0.9  2001/07/03 15:14:15  cjm
// Changed error code to include Ccs sub-system offset.
//
// Revision 0.8  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.7  1999/11/08 15:56:43  cjm
// Changed STOP implementation.
// If we are currently exposing this is aborted.
// The exposure/calibrate command that was running is then meant to read-out the CCD.
//
// Revision 0.6  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.5  1999/11/01 17:56:40  cjm
// First attempt at an implementation of STOP.
// Need to read out data after aborting exposure.
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
