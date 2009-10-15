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
// ABORTImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/ABORTImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.ABORT_DONE;

/**
 * This class provides the implementation for the ABORT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class ABORTImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: ABORTImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Constructor.
	 */
	public ABORTImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.ABORT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.ABORT";
	}

	/**
	 * This method gets the ABORT command's acknowledge time. This takes the default acknowledge time to implement.
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
	 * This method implements the ABORT command. 
	 * <ul>
	 * <li>It tells the currently executing thread to abort itself.
	 * <li>If the CCS is currently waiting for the SDSU CCD Controller to setup,expose or readout
	 * 	these are aborted with suitable libccd routines.
	 * <li>If the CCS is currently waiting for the SDSU CCD Controller to move/reset the filter wheel,
	 * 	this is aborted.
	 * </ul>
	 * An object of class ABORT_DONE is returned.
	 * @see CcsStatus#getCurrentThread
	 * @see CcsTCPServerConnectionThread#setAbortProcessCommand
	 * @see CCDLibrary#CCDSetupAbort
	 * @see CCDLibrary#CCDSetupGetSetupInProgress
	 * @see CCDLibrary#CCDExposureGetExposureStatus
	 * @see CCDLibrary#CCDExposureAbort
	 * @see CCDLibrary#CCDFilterWheelAbort
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		ngat.message.INST_DP.ABORT dprtAbort = new ngat.message.INST_DP.ABORT(command.getId());
		ABORT_DONE abortDone = new ABORT_DONE(command.getId());
	//	CcsTCPServerConnectionThread thread = null;
	//	CcsStatus status = null;
     	//	int readoutRemainingTime = 0;

	// tell the thread itself to abort at a suitable point
	//	status = ccs.getStatus();
	//	thread = (CcsTCPServerConnectionThread)status.getCurrentThread();
	//	if(thread != null)
	//		thread.setAbortProcessCommand();
	// if we are in the middle of a libccd exposure command, abort it
	// Or preferably abort in any case! I TODD
		//if(libccd.CCDExposureGetExposureStatus() != libccd.CCD_EXPOSURE_STATUS_NONE)
		if(true)
		{
			try
			{
				ccs.error(this.getClass().getName()+":Abort sent");
				libccd.CCDExposureAbort();
			}
			catch (CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+":Aborting exposure failed:"+e);
				abortDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2400);
				abortDone.setErrorString(e.toString());
				abortDone.setSuccessful(false);
				return abortDone;
			}
		}
	/*
	// If we are setting up the camera, abort the setup
		if(libccd.CCDSetupGetSetupInProgress())
		{
			libccd.CCDSetupAbort();
		}
	// if we are moving the filter wheel, stop it
		try
		{
		// note we don't check ccs.config.filter_wheel.enable here,
		// if this is false the status must be CCD_FILTER_WHEEL_STATUS_NONE.
			if(libccd.CCDFilterWheelGetStatus() != libccd.CCD_FILTER_WHEEL_STATUS_NONE)
				libccd.CCDFilterWheelAbort();
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+":Aborting filter wheel failed:"+e);
			abortDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2403);
			abortDone.setErrorString(e.toString());
			abortDone.setSuccessful(false);
			return abortDone;
		} */
	// abort the dprt
	//	ccs.sendDpRtCommand(dprtAbort,serverConnectionThread);
	// return done object.
		abortDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		abortDone.setErrorString("");
		abortDone.setSuccessful(true);
		return abortDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.12  2006/05/16 14:25:35  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.11  2004/05/16 14:17:32  cjm
// Re-written Abort implementation.
//
// Revision 0.10  2002/12/16 17:00:27  cjm
// Implementation changed.
//
// Revision 0.9  2001/02/09 18:43:39  cjm
// Changed to using CCDFilterWheelGetStatus.
//
// Revision 0.8  2000/12/22 11:48:29  cjm
// Added filter wheel abort
//
// Revision 0.7  2000/08/08 14:32:04  cjm
// Sends ABORT on to DpRt.
//
// Revision 0.6  2000/03/03 15:23:48  cjm
// Changed implementation to use CCDDSPAbort.
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
