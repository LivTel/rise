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
// EXPOSEImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/EXPOSEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.EXPOSE_DONE;
import ngat.message.INST_DP.*;

/**
 * This class provides the generic implementation for EXPOSE commands sent to a server using the
 * Java Message System. It extends FITSImplementation, as EXPOSE commands needs access to
 * resources to make FITS files.
 * @see FITSImplementation
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class EXPOSEImplementation extends FITSImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: EXPOSEImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * This method gets the EXPOSE command's acknowledge time. It returns the server connection 
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
	 * This method is a generic implementation for the EXPOSE command, that does nothing.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
	       	// do nothing 
		EXPOSE_DONE exposeDone = new EXPOSE_DONE(command.getId());

		exposeDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		exposeDone.setErrorString("");
		exposeDone.setSuccessful(true);
		return exposeDone;
	}

	/**
	 * This routine calls the Real Time Data Pipeline to process the expose FITS image we have just captured.
	 * If an error occurs the done objects field's are set accordingly. If the operation succeeds, and the
	 * done object is of class EXPOSE_DONE, the done object is filled with data returned from the 
	 * reduction command.
	 * @param command The command being implemented that made this call to the DP(RT). This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see Ccs#sendDpRtCommand
	 */
	public boolean reduceExpose(COMMAND command,COMMAND_DONE done,String filename)
	{
		EXPOSE_REDUCE reduce = new EXPOSE_REDUCE(command.getId());
		INST_TO_DP_DONE instToDPDone = null;
		EXPOSE_REDUCE_DONE reduceDone = null;
		EXPOSE_DONE exposeDone = null;

		reduce.setFilename(filename);
		instToDPDone = ccs.sendDpRtCommand(reduce,serverConnectionThread);
		if(instToDPDone.getSuccessful() == false)
		{
			ccs.error(this.getClass().getName()+":reduce:"+
				command+":"+instToDPDone.getErrorNum()+":"+instToDPDone.getErrorString());
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+600);
			done.setErrorString(instToDPDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
		// Copy the DP REDUCE DONE parameters to the CCS EXPOSE DONE parameters
		if(instToDPDone instanceof EXPOSE_REDUCE_DONE)
		{
			reduceDone = (EXPOSE_REDUCE_DONE)instToDPDone;
			if(done instanceof EXPOSE_DONE)
			{
				exposeDone = (EXPOSE_DONE)done;
				exposeDone.setFilename(reduceDone.getFilename());
				exposeDone.setSeeing(reduceDone.getSeeing());
				exposeDone.setCounts(reduceDone.getCounts());
				exposeDone.setXpix(reduceDone.getXpix());
				exposeDone.setYpix(reduceDone.getYpix());
				exposeDone.setPhotometricity(reduceDone.getPhotometricity());
				exposeDone.setSkyBrightness(reduceDone.getSkyBrightness());
				exposeDone.setSaturation(reduceDone.getSaturation());
			}
		}
		return true;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.9  2006/05/16 14:25:51  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.8  2002/05/23 12:40:25  cjm
// Added extra fields that need copying from DpRt EXPOSE_DONE to CCS EXPOSE_DONE.
//
// Revision 0.7  2001/07/03 16:28:32  cjm
// Added Ccs base error number to error numbers.
//
// Revision 0.6  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.5  1999/12/03 16:01:38  cjm
// sendDpRtCommand now takes the thread the command is running on as a parameter.
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
