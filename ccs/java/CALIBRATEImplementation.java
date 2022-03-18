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
// CALIBRATEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CALIBRATEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.CALIBRATE_DONE;
import ngat.message.INST_DP.*;

/**
 * This class provides the generic implementation for CALIBRATE commands sent to a server using the
 * Java Message System. It extends FITSImplementation, as CALIBRATE commands needs access to
 * resources to make FITS files.
 * @see FITSImplementation
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CALIBRATEImplementation extends FITSImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CALIBRATEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * This method gets the CALIBRATE command's acknowledge time. It returns the server connection 
	 * threads min acknowledge time. This method should be over-written in sub-classes.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getMinAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(serverConnectionThread.getMinAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method is a generic implementation for the CALIBRATE command, that does nothing.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
	       	// do nothing 
		CALIBRATE_DONE calibrateDone = new CALIBRATE_DONE(command.getId());

		calibrateDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		calibrateDone.setErrorString("");
		calibrateDone.setSuccessful(true);
		return calibrateDone;
	}

	/**
	 * This routine calls the Real Time Data Pipeline to process the calibration FITS image we have just captured.
	 * It sends the filename to the Data Pipeline and waits for a result. If an error occurs the done
	 * object is filled in and the method returns. If it succeeds and the done object is of class CALIBRATE_DONE,
	 * the data returned from the Data Pipeline is copied into the done object.
	 * @param command The command being implemented that made this call to the DP(RT). This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param filename The filename of the FITS image to be processed by the Data Pipeline(Real Time).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendDpRtCommand
	 */
	public boolean reduceCalibrate(COMMAND command,COMMAND_DONE done,String filename)
	{
		CALIBRATE_REDUCE reduce = new CALIBRATE_REDUCE(command.getId());
		INST_TO_DP_DONE instToDPDone = null;
		CALIBRATE_REDUCE_DONE reduceDone = null;
		CALIBRATE_DONE calibrateDone = null;

		reduce.setFilename(filename);
		instToDPDone = ccs.sendDpRtCommand(reduce,serverConnectionThread);
		if(instToDPDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":reduce:"+
				command+":"+instToDPDone.getErrorNum()+":"+instToDPDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+500);
			done.setErrorString(instToDPDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
		// Copy the DP REDUCE DONE paramaters to the CCS CALIBRATE DONE paramaters
		if(instToDPDone instanceof CALIBRATE_REDUCE_DONE)
		{
			reduceDone = (CALIBRATE_REDUCE_DONE)instToDPDone;
			if(done instanceof CALIBRATE_DONE)
			{
				calibrateDone = (CALIBRATE_DONE)done;
				calibrateDone.setFilename(reduceDone.getFilename());
				calibrateDone.setMeanCounts(reduceDone.getMeanCounts());
				calibrateDone.setPeakCounts(reduceDone.getPeakCounts());
			}
		}
		return true;
	}

	/**
	 * This routine calls the Real Time Data Pipeline to create a master bias frame, from a series of 
	 * calibration FITS images in a directory.
	 * It sends the directory to the Data Pipeline and waits for a result. If an error occurs the done
	 * object is filled in and the method returns. If it succeeds and the done object is of class 
	 * MAKE_MASTER_BIAS_DONE, the data returned from the Data Pipeline is copied into the done object.
	 * @param command The command being implemented that made this call to the DP(RT). This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param dirname The directory of the FITS images to be processed by the Data Pipeline(Real Time).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendDpRtCommand
	 */
	public boolean makeMasterBias(COMMAND command,COMMAND_DONE done,String dirname)
	{
		MAKE_MASTER_BIAS makeMasterBiasCommand = null;
		INST_TO_DP_DONE instToDPDone = null;

		makeMasterBiasCommand = new MAKE_MASTER_BIAS(command.getId());
		makeMasterBiasCommand.setDirname(dirname);
		instToDPDone = ccs.sendDpRtCommand(makeMasterBiasCommand,serverConnectionThread);
		if(instToDPDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":makeMasterBias:"+
				command+":"+instToDPDone.getErrorNum()+":"+instToDPDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+501);
			done.setErrorString(instToDPDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * This routine calls the Real Time Data Pipeline to create a master flat frame, from a series of 
	 * calibration flat field FITS images in a directory.
	 * It sends the directory to the Data Pipeline and waits for a result. If an error occurs the done
	 * object is filled in and the method returns. If it succeeds and the done object is of class 
	 * MAKE_MASTER_FLAT_DONE, the data returned from the Data Pipeline is copied into the done object.
	 * @param command The command being implemented that made this call to the DP(RT). This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param dirname The directory of the FITS images to be processed by the Data Pipeline(Real Time).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendDpRtCommand
	 */
	public boolean makeMasterFlat(COMMAND command,COMMAND_DONE done,String dirname)
	{
		MAKE_MASTER_FLAT makeMasterFlatCommand = null;
		INST_TO_DP_DONE instToDPDone = null;

		makeMasterFlatCommand = new MAKE_MASTER_FLAT(command.getId());
		makeMasterFlatCommand.setDirname(dirname);
		instToDPDone = ccs.sendDpRtCommand(makeMasterFlatCommand,serverConnectionThread);
		if(instToDPDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":makeMasterFlat:"+
				command+":"+instToDPDone.getErrorNum()+":"+instToDPDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+502);
			done.setErrorString(instToDPDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
		return true;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.9  2006/05/16 14:25:39  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.8  2002/11/26 18:56:05  cjm
// Added makeMasterFlat and makeMasterBias commands.
//
// Revision 0.7  2001/07/03 16:36:12  cjm
// Added Ccs base error code to error numbers.
//
// Revision 0.6  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.5  1999/12/03 16:00:38  cjm
// sendDpRtCommand now uses the thread the command that needs reduction is running on.
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
