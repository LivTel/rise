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
// CcsTCPServerConnectionThread.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsTCPServerConnectionThread.java,v 1.2 2010-03-26 14:38:29 cjm Exp $
import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.net.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.INST_DP.CALIBRATE_REDUCE;
import ngat.message.INST_DP.CALIBRATE_REDUCE_DONE;
import ngat.message.INST_DP.EXPOSE_REDUCE;
import ngat.message.INST_DP.EXPOSE_REDUCE_DONE;
import ngat.message.INST_DP.INST_TO_DP_DONE;
import ngat.phase2.*;
import ngat.util.logging.*;
/**
 * This class extends the TCPServerConnectionThread class for the Ccs application.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class CcsTCPServerConnectionThread extends TCPServerConnectionThread
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsTCPServerConnectionThread.java,v 1.2 2010-03-26 14:38:29 cjm Exp $");
	/**
	 * Default time taken to respond to a command. This is a class-wide field.
	 */
	private static int defaultAcknowledgeTime = 60*1000;
	/**
	 * Time taken to respond to a command not implemented on this instrument. This is a class-wide field.
	 */
	private static int minAcknowledgeTime = 30*1000;
	/**
	 * The Ccs object.
	 */
	private Ccs ccs = null;
	/**
	 * This reference stores the instance of the class that implements the command passed to this thread.
	 * This can be retrieved from the ccs main object, which has a Hashtable with mappings between
	 * ngat.message class names and implementations of these commands.
	 * @see JMSCommandImplementation
	 * @see Ccs#getImplementation
	 */
	JMSCommandImplementation commandImplementation = null;
	/**
	 * Variable used to track whether we should continue processing the command this process is meant to
	 * process. If the ISS has sent an ABORT message this variable is set to true using
	 * <a href="#setAbortProcessCommand">setAbortProcessCommand</a>, and then the 
	 * <a href="#processCommand">processCommand</a> routine should tidy up and return to stop this thread.
	 * @see #setAbortProcessCommand
	 * @see #getAbortProcessCommand
	 * @see #processCommand
	 */
	private boolean abortProcessCommand = false;
	/**
	 * Field holding the results of the JMSCommandImplementation.calculateAcknowledgeTime call in
	 * the calculateAcknowledgeTime method over-ridden from the default. We need this when
	 * calculating remaining acknowledge time after a sub-command call to the ISS/DpRt.
	 * @see JMSCommandImplementation#calculateAcknowledgeTime
	 * @see #commandImplementation
	 */
	private int acknowledgeTime = 0;

	/**
	 * Constructor of the thread. This just calls the superclass constructors.
	 * @param connectionSocket The socket the thread is to communicate with.
	 */
	public CcsTCPServerConnectionThread(Socket connectionSocket)
	{
		super(connectionSocket);
	}

	/**
	 * Class method to set the value of <a href="#defaultAcknowledgeTime">defaultAcknowledgeTime</a>. 
	 * @see #defaultAcknowledgeTime
	 */
	public static void setDefaultAcknowledgeTime(int m)
	{
		defaultAcknowledgeTime = m;
	}

	/**
	 * Class method to get the value set for the <a href="#defaultAcknowledgeTime">defaultAcknowledgeTime</a>. 
	 * @see #defaultAcknowledgeTime
	 */
	public static int getDefaultAcknowledgeTime()
	{
		return defaultAcknowledgeTime;
	}

	/**
	 * Class method to set the value of <a href="#minAcknowledgeTime">minAcknowledgeTime</a>. 
	 * @see #minAcknowledgeTime
	 */
	public static void setMinAcknowledgeTime(int m)
	{
		minAcknowledgeTime = m;
	}

	/**
	 * Class method to get the value set for the <a href="#minAcknowledgeTime">minAcknowledgeTime</a>. 
	 * @see #minAcknowledgeTime
	 */
	public static int getMinAcknowledgeTime()
	{
		return minAcknowledgeTime;
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
	 * Routine called by another thread to stop this
	 * thread implementing a command it has been sent. This variable should cause the processCommand
	 * method to return as soon as possible. The processCommand should still create a COMMAND_DONE
	 * object and fill it in with a suitable abort message. The processCommand should also undo any
	 * operation it has half completed - e.g. switch the autoguider off.
	 * The rest of this thread's run method should then execute
	 * to send the DONE message back to the client.
	 * @see #abortProcessCommand
	 */
	public synchronized void setAbortProcessCommand()
	{
		abortProcessCommand = true;
	}

	/**
	 * Method to return whether this thread has been requested to stop what it is processing.
	 * @see #abortProcessCommand
	 */
	public synchronized boolean getAbortProcessCommand()
	{
		return abortProcessCommand;
	}

	/**
	 * This method is called after the clients command is read over the socket. It allows us to
	 * initialise this threads response to a command. This method changes the threads priority now 
	 * that the command's class is known, if it is a sub-class of INTERRUPT the priority is higher.<br>
	 * It also finds the command implementation used to run this command, got from the mapping
	 * stored in the Ccs object. It sets up the implementation objects references to the CCS main
	 * object and this connection thread. It then runs the command implementation's init routine, to
	 * initialise the implementation.
	 * @see INTERRUPT
	 * @see Thread#setPriority
	 * @see Ccs#getImplementation
	 * @see CommandImplementation#setCcs
	 * @see CommandImplementation#setServerConnectionThread
	 * @see JMSCommandImplementation#init
	 * @see #commandImplementation
	 */
	protected void init()
	{
	// set the threads priority
		if(command instanceof INTERRUPT)
			this.setPriority(ccs.getStatus().getThreadPriorityInterrupt());
		else
			this.setPriority(ccs.getStatus().getThreadPriorityNormal());
	// get the implementation - this never returns null.
		commandImplementation = ccs.getImplementation(command.getClass().getName());
	// initialises the command implementations response
		((CommandImplementation)commandImplementation).setCcs(ccs);
		((CommandImplementation)commandImplementation).setServerConnectionThread(this);
		commandImplementation.init(command);
	}

	/**
	 * This method calculates the time it will take for the command to complete and is called
	 * from the classes inherited run method. It calls the command implementation to calculate this
	 * as this will calculate a suitable value. It stores the result in the acknowledgTime field
	 * for later reference before returning.
	 * @return An instance of a (sub)class of ngat.message.base.ACK is returned, with the timeToComplete
	 * 	field set to the time the command will take to process.
	 * @see ngat.message.base.ACK
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see #commandImplementation
	 * @see JMSCommandImplementation#calculateAcknowledgeTime
	 * @see #acknowledgeTime
	 */
	protected ACK calculateAcknowledgeTime()
	{
		ACK acknowledge = null;

		acknowledge =  commandImplementation.calculateAcknowledgeTime(command);
		if(acknowledge != null)
			acknowledgeTime = acknowledge.getTimeToComplete();
		else
			acknowledgeTime = 0;
		ccs.log(Logging.VERBOSITY_VERBOSE,"Command:"+command.getClass().getName()+
			" calculating ACK with time to complete "+acknowledgeTime+".");
		return acknowledge;
	}

	/**
	 * This method overrides the processCommand method in the ngat.net.TCPServerConnectionThread class.
	 * It is called from the inherited run method. It is responsible for performing the commands
	 * sent to it by the ISS. It should also construct the done object to describe the results of the command.<br>
	 * <ul>
	 * <li>This method checks whether the command in null and returns a generic done error message if this is the 
	 * case.
	 * <li>If suitable logging is enabled the command is logged.
	 * <li>The thread checks whether the command passed to it can be run, using the commandCanBeRun method.
	 * If it cannot a suitable done error message is returned.
	 * <li>If the command is not an interrupt command sub-class it sets the CCS's status to reflect
	 * the command/thread(this one) currently doing the processing.
	 * <li>This method delagates the command processing to the command implementation found for the command
	 * message class.
	 * <li>The CCS's status is again updated to reflect this command/thread has finished processing. (If it's
	 * not a sub-class of INTERRUPT again).
	 * <li>If suitable logging is enabled the command is logged as completed.
	 * </ul>
	 * @see CcsStatus#getLogLevel
	 * @see Ccs#log
	 * @see CcsStatus#commandCanBeRun
	 * @see CcsStatus#setCurrentCommand
	 * @see CcsStatus#setCurrentThread
	 * @see #commandImplementation
	 * @see JMSCommandImplementation#processCommand
	 */
	protected void processCommand()
	{
	// setup a generic done object until the command specific one is constructed.
		done = new COMMAND_DONE(command.getId());

		if(command == null)
		{
			ccs.error("processCommand:command was null.");
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+100);
			done.setErrorString("processCommand:command was null.");
			done.setSuccessful(false);
			return;
		}
		ccs.log(Logging.VERBOSITY_TERSE,"Command:"+command.getClass().getName()+" Started.");
		if(!ccs.getStatus().commandCanBeRun((ISS_TO_INST)command))
		{
			// ccs.getStatus().getCurrentCommand() may have been set to null between
			// the commandCanBeRun test above and the getCurrentCommand routine below.
			// We must allow for this when generating the error string.
			ISS_TO_INST currentCommand = ccs.getStatus().getCurrentCommand();
			String currentCommandString = null;

			if(currentCommand != null)
				currentCommandString = new String(currentCommand.getClass().getName());
			else
				currentCommandString = new String("Unknown Command");
			String s = new String("processCommand:command `"+command.getClass().getName()+
				"'could not be run:Command `"+
				currentCommandString+"' already running.");
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+101);
			done.setErrorString(s);
			done.setSuccessful(false);
			ccs.error(s);
			return;
		}
	// This test says interupt class commands should not become current command.
	// This class of commands probably want to see what the current command is anyway.
		if(!(command instanceof INTERRUPT))
		{
			ccs.getStatus().setCurrentCommand((ISS_TO_INST)command);
			ccs.getStatus().setCurrentThread((Thread)this);
		}
	// setup return object.
		try
		{
			done = commandImplementation.processCommand(command);
		}
	// We want to catch unthrown exceptions here - so that we can (almost) guarantee
	// the status's current command is reset to null.
		catch(Exception e)
		{
			String s = new String(this.getClass().getName()+":processCommand failed:");
			ccs.error(s,e);
			done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+102);
			done.setErrorString(s+e);
			done.setSuccessful(false);
		}
	// change CCS status once command has been done
		if(!(command instanceof INTERRUPT))
		{
			ccs.getStatus().setCurrentCommand(null);
			ccs.getStatus().setCurrentThread(null);
		}
	// log command/done
		ccs.log(Logging.VERBOSITY_TERSE,"Command:"+command.getClass().getName()+" Completed.");
		ccs.log(Logging.VERBOSITY_TERSE,"Done:"+done.getClass().getName()+
			":successful:"+done.getSuccessful()+
			":error number:"+done.getErrorNum()+":error string:"+done.getErrorString());
	}

	/**
	 * This routine sends an acknowledge back to the client.
	 * @param acknowledge The acknowledge object to send back to the client.
	 * @param setThreadAckTime If true, the local instance of <b>acknowledgeTime</b> is updated
	 * 	with the acknowledge object's time to complete.
	 * @exception NullPointerException If the acknowledge object is null this exception is thrown.
	 * @exception IOException If the acknowledge object fails to be sent an IOException results.
	 * @see #acknowledgeTime
	 * @see ngat.net.TCPServerConnectionThread#sendAcknowledge
	 */
	public void sendAcknowledge(ACK acknowledge,boolean setThreadAckTime) throws IOException
	{
		if(setThreadAckTime)
			acknowledgeTime = acknowledge.getTimeToComplete();
		ccs.log(Logging.VERBOSITY_VERBOSE,"Command:"+command.getClass().getName()+
			" sending ACK with time to complete "+acknowledge.getTimeToComplete()+".");
		super.sendAcknowledge(acknowledge);
	}

	/**
	 * Return the initial time the implementation thought it would take to complete this command.
	 * @return The acknowledge time, zero if the calculateAcknowledgTime routine has not been called yet,
	 *	or an acknowledge object was not returned.
	 * @see #acknowledgeTime
	 */
	public int getAcknowledgeTime()
	{
		return acknowledgeTime;
	}

}

// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 1.40  2006/05/16 14:25:46  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.39  2004/11/23 15:59:20  cjm
// Added ACK logging.
//
// Revision 1.38  2004/11/23 15:23:51  cjm
// Added ACK logging.
//
// Revision 1.37  2001/08/23 14:31:57  cjm
// Added sendAcknowledge override method, for implmentations that need to change the
// stored acknowledge time.
//
// Revision 1.36  2001/07/03 16:29:51  cjm
// Added Ccs base error number to error numbers.
//
// Revision 1.35  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 1.34  2001/03/01 15:15:49  cjm
// Changed from CcsCOnstants error numbers to hard-coded error numbers.
//
// Revision 1.33  2000/11/13 17:22:22  cjm
// Stopped error strings in exceptions being printed twice: printStackTrace prints error message.
//
// Revision 1.32  2000/08/08 17:04:24  cjm
// Synchronized access to abortProcessCommand.
//
// Revision 1.31  2000/07/10 15:04:14  cjm
// Changed setPriority calls to get priorities from configuration file.
//
// Revision 1.30  2000/06/14 09:21:40  cjm
// Added extra debugging when general exceptions thrown:
// stack trace is printed to error file.
//
// Revision 1.29  1999/12/07 12:29:03  cjm
// Added method for retrieving the acknowledge time calculated. This is needed for re-sending to clients
// after an ISS/DpRt sub-command that has sent it's own acknowledge times back to the Ccs's client.
//
// Revision 1.28  1999/11/24 11:00:34  cjm
// Caught any Exception sub-class from processCommand. This means is processCommand
// throws some runtime exception (such as NullPointerException) the Server Connection Thread
// still recover sufficiently to call CcsStatus.setCurrentCommand(null).
// This stops the Ccs from locking in the failed command.
//
// Revision 1.27  1999/11/02 12:05:02  cjm
// Changed calculateAcknowledgeTime to return an ACK object.
// This is because ngat.net.TCPServerConenctionThread has changed.
//
// Revision 1.26  1999/11/01 10:48:55  cjm
// Changed to aquire and use an implementation class.
//
// Revision 1.25  1999/10/27 16:24:00  cjm
// Last version before Implementation classes used for implementations.
// Added getDefaultAcknowledgeTime, getMinAcknowledgeTime.
//
// Revision 1.24  1999/10/25 11:02:06  cjm
// Move setting thread priority to new init routine.
//
// Revision 1.23  1999/09/20 14:37:56  cjm
// Changed due to libccd native routines throwing CCDLibraryNativeException when errors occur.
//
// Revision 1.22  1999/09/15 12:37:05  cjm
// Imported from ngat.ccd package now that CDLibrary is in this package.
//
// Revision 1.21  1999/09/10 14:54:51  cjm
// ARC command now returns successful without doing any processing - ARC is a spectrograph command.
// LAMPFLAT command now returns successful without doing any processing - no lamp.
// Initial implementation of SKYFLAT command - exposureTime needs to be calculated -
// not sure of libccd Expose command yet.
//
// Revision 1.20  1999/09/10 10:49:23  cjm
// Implemented GLANCE and SAVE commands.
// Tidied up other exposure commands so things like autoguider etc are switched off if the
// command has to bail out due to an error occuring. Also stopped commands trying to reduce data that had failed
// to be saved (RUNAT) and ensured done paramaters matched result of operation.
//
// Revision 1.19  1999/09/09 11:28:58  cjm
// BIAS implemetnation now uses libccd.CCDExposureBias.
//
// Revision 1.18  1999/09/07 14:19:36  cjm
// Fixed so that it compiles.
//
// Revision 1.17  1999/09/07 14:12:09  cjm
// Changed EXPOSE commands to take account of pipelineProcess flag.
// Changed REBOOT to be a subclass of INTERRUPT rather than a subclass of SETUP.
//
// Revision 1.16  1999/07/09 10:44:49  dev
// Fixed error in RUNAT so we can't sleep for negative time!
//
// Revision 1.15  1999/07/05 10:28:49  dev
// Made some changes to get the Fits File headers saved.
// Added  setFitsHeaders and saveFitsHeaders methods.
//
// Revision 1.14  1999/07/01 13:53:11  dev
// more log file improvements
//
// Revision 1.13  1999/07/01 13:39:58  dev
// Log level improved
//
// Revision 1.12  1999/06/30 16:29:43  dev
// changed calls to reduceCalibrate so they return if reduceCalibrate
// returns false!
//
// Revision 1.11  1999/06/24 12:40:20  dev
// "Backup"
//
// Revision 1.10  1999/06/09 16:52:36  dev
// thread abort procedure improvements and error/log file implementation
//
// Revision 1.9  1999/06/08 16:50:53  dev
// thread priority changes
//
// Revision 1.8  1999/06/07 16:54:59  dev
// properties file/more implementation
//
// Revision 1.7  1999/06/02 15:19:12  dev
// "Backup"
//
// Revision 1.6  1999/05/28 09:54:34  dev
// "Name
//
// Revision 1.5  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.4  1999/04/27 11:26:51  dev
// Backup
//
// Revision 1.3  1999/03/25 14:02:16  dev
// Backup
//
// Revision 1.2  1999/03/19 11:50:05  dev
// Backup
//
