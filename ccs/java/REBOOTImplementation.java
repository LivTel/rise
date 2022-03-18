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
// REBOOTImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/REBOOTImplementation.java,v 1.2 2010-03-26 14:38:29 cjm Exp $

import java.lang.*;
import java.io.IOException;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.REBOOT;
import ngat.util.ICSDRebootCommand;
import ngat.util.ICSDShutdownCommand;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the REBOOT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class REBOOTImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: REBOOTImplementation.java,v 1.2 2010-03-26 14:38:29 cjm Exp $");
	/**
	 * Class constant used in calculating acknowledge times, when the acknowledge time connot be found in the
	 * configuration file.
	 */
	public final static int DEFAULT_ACKNOWLEDGE_TIME = 		300000;
	/**
	 * String representing the root part of the property key used to get the acknowledge time for 
	 * a certain level of reboot.
	 */
	public final static String ACK_TIME_PROPERTY_KEY_ROOT =	    "ccs.reboot.acknowledge_time.";
	/**
	 * String representing the root part of the property key used to decide whether a certain level of reboot
	 * is enabled.
	 */
	public final static String ENABLE_PROPERTY_KEY_ROOT =       "ccs.reboot.enable.";
	/**
	 * Set of constant strings representing levels of reboot. The levels currently start at 1, so index
	 * 0 is currently "NONE". These strings need to be kept in line with level constants defined in
	 * ngat.message.ISS_INST.REBOOT.
	 */
	public final static String REBOOT_LEVEL_LIST[] =  {"NONE","REDATUM","SOFTWARE","HARDWARE","POWER_OFF"};

	/**
	 * Constructor.
	 */
	public REBOOTImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.REBOOT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.REBOOT";
	}

	/**
	 * This method gets the REBOOT command's acknowledge time. This time is dependant on the level.
	 * This is calculated as follows:
	 * <ul>
	 * <li>If the level is LEVEL_REDATUM, the number stored in &quot; 
	 * ccs.reboot.acknowledge_time.REDATUM &quot; in the Ccs properties file is the timeToComplete.
	 * <li>If the level is LEVEL_SOFTWARE, the number stored in &quot; 
	 * ccs.reboot.acknowledge_time.SOFTWARE &quot; in the Ccs properties file is the timeToComplete.
	 * <li>If the level is LEVEL_HARDWARE, the number stored in &quot; 
	 * ccs.reboot.acknowledge_time.HARDWARE &quot; in the Ccs properties file is the timeToComplete.
	 * <li>If the level is LEVEL_POWER_OFF, the number stored in &quot; 
	 * ccs.reboot.acknowledge_time.POWER_OFF &quot; in the Ccs properties file is the timeToComplete.
	 * </ul>
	 * If these numbers cannot be found, the default number DEFAULT_ACKNOWLEDGE_TIME is used instead.
	 * <br>Note, this return value is irrelevant in the SOFTWARE,HARDWARE and POWER_OFF cases, 
	 * the client does not expect a DONE message back from
	 * the process as the CCS should restart in the implementation of this command.
	 * However, the value returned here will be how long the client waits before trying to restart communications
	 * with the CCS server, so a reasonable value here may be useful.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set to a time (in milliseconds).
	 * @see #DEFAULT_ACKNOWLEDGE_TIME
	 * @see #ACK_TIME_PROPERTY_KEY_ROOT
	 * @see #REBOOT_LEVEL_LIST
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsStatus#getPropertyInteger
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ngat.message.ISS_INST.REBOOT rebootCommand = (ngat.message.ISS_INST.REBOOT)command;
		ACK acknowledge = null;
		int timeToComplete = 0;

		acknowledge = new ACK(command.getId()); 
		try
		{
			timeToComplete = ccs.getStatus().getPropertyInteger(ACK_TIME_PROPERTY_KEY_ROOT+
								   REBOOT_LEVEL_LIST[rebootCommand.getLevel()]);
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+":calculateAcknowledgeTime:"+
					rebootCommand.getLevel(),e);
			timeToComplete = DEFAULT_ACKNOWLEDGE_TIME;
		}
	//set time and return
		acknowledge.setTimeToComplete(timeToComplete);
		return acknowledge;
	}

	/**
	 * This method implements the REBOOT command. 
	 * An object of class REBOOT_DONE is returned.
	 * The <i>ccs.reboot.enable.&lt;level&gt;</i> property is checked to see to whether to really
	 * do the specified level of reboot. Thsi enables us to say, disbale to POWER_OFF reboot, if the
	 * instrument control computer is not connected to an addressable power supply.
	 * The following four levels of reboot are recognised:
	 * <ul>
	 * <li>REDATUM. This shuts down the connection to the controller, and then
	 * 	restarts it.
	 * <li>SOFTWARE. This shuts down the connection to the SDSU CCD Controller and closes the
	 * 	server socket using the Ccs close method. It then exits the Ccs.
	 * <li>HARDWARE. This shuts down the connection to the SDSU CCD Controller and closes the
	 * 	server socket using the Ccs close method. It then issues a reboot
	 * 	command to the underlying operating system, to restart the instrument computer.
	 * <li>POWER_OFF. This shuts down the connection to the SDSU CCD Controller and closes the
	 * 	server socket using the Ccs close method. It then issues a shutdown
	 * 	command to the underlying operating system, to put the instrument computer into a state
	 * 	where power can be switched off.
	 * </ul>
	 * Note: You need to perform at least a SOFTWARE level reboot to re-read the Ccs configuration file,
	 * as it contains information such as server ports.
	 * @param command The command instance we are implementing.
	 * @return An instance of REBOOT_DONE. Note this is only returned on a REDATUM level reboot,
	 * all other levels cause the Ccs to terminate (either directly or indirectly) and a DONE
	 * message cannot be returned.
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_REDATUM
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_SOFTWARE
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_HARDWARE
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_POWER_OFF
	 * @see #ENABLE_PROPERTY_KEY_ROOT
	 * @see #REBOOT_LEVEL_LIST
	 * @see Ccs#close
	 * @see Ccs#shutdownController
	 * @see Ccs#startupController
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		ngat.message.ISS_INST.REBOOT rebootCommand = (ngat.message.ISS_INST.REBOOT)command;
		ngat.message.ISS_INST.REBOOT_DONE rebootDone = new ngat.message.ISS_INST.REBOOT_DONE(command.getId());
		ngat.message.INST_DP.REBOOT dprtReboot = new ngat.message.INST_DP.REBOOT(command.getId());
		ICSDRebootCommand icsdRebootCommand = null;
		ICSDShutdownCommand icsdShutdownCommand = null;
		CcsREBOOTQuitThread quitThread = null;
		boolean enable;

		try
		{
			ccs.error(this.getClass().getName()+
				":processCommand:"+command+":Trying reboot level: "+rebootCommand.getLevel());
			// is reboot enabled at this level
			enable = ccs.getStatus().getPropertyBoolean(ENABLE_PROPERTY_KEY_ROOT+
							   REBOOT_LEVEL_LIST[rebootCommand.getLevel()]);
			// if not enabled return OK
			if(enable == false)
			{
				ccs.log(Logging.VERBOSITY_TERSE,"Command:"+
					   rebootCommand.getClass().getName()+":Level:"+rebootCommand.getLevel()+
					   " is not enabled.");
				rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
				rebootDone.setErrorString("");
				rebootDone.setSuccessful(true);
				return rebootDone;
			}
			// do relevent reboot based on level
			switch(rebootCommand.getLevel())
			{
				case REBOOT.LEVEL_REDATUM:
					ccs.shutdownController();
					ccs.reInit();
					ccs.startupController();
					break;
				case REBOOT.LEVEL_SOFTWARE:
				// send REBOOT to the data pipeline
					dprtReboot.setLevel(rebootCommand.getLevel());
					ccs.sendDpRtCommand(dprtReboot,serverConnectionThread);
				// Don't check DpRt done, chances are the DpRt quit before sending the done message.
					ccs.close();
					quitThread = new CcsREBOOTQuitThread("quit:"+rebootCommand.getId());
					quitThread.setCcs(ccs);
					quitThread.setWaitThread(serverConnectionThread);
					quitThread.start();
					break;
				case REBOOT.LEVEL_HARDWARE:
				// send REBOOT to the data pipeline
					dprtReboot.setLevel(rebootCommand.getLevel());
					ccs.sendDpRtCommand(dprtReboot,serverConnectionThread);
				// Don't check DpRt done, chances are the DpRt quit before sending the done message.
				// We don't call close to minimise computer lock-ups.
				// send reboot to the icsd_inet
					icsdRebootCommand = new ICSDRebootCommand();
					icsdRebootCommand.send();
					break;
				case REBOOT.LEVEL_POWER_OFF:
				// send REBOOT to the data pipeline
					dprtReboot.setLevel(rebootCommand.getLevel());
					ccs.sendDpRtCommand(dprtReboot,serverConnectionThread);
				// Don't check done, chances are the DpRt quit before sending the done message.
				// We don't call close to minimise computer lock-ups.
				// send shutdown to the icsd_inet
					icsdShutdownCommand = new ICSDShutdownCommand();
					icsdShutdownCommand.send();
					break;
				default:
					ccs.error(this.getClass().getName()+
						":processCommand:"+command+":Illegal level:"+rebootCommand.getLevel());
					rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1400);
					rebootDone.setErrorString("Illegal level:"+rebootCommand.getLevel());
					rebootDone.setSuccessful(false);
					return rebootDone;
			};// end switch
		}
		catch(CCDLibraryNativeException e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":",e);
			rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1401);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
		catch(IOException e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":",e);
			rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1402);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
		catch(InterruptedException e)
		{
			ccs.error(this.getClass().getName()+
				":processCommand:"+command+":",e);
			rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1403);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+
					":processCommand:"+command+":",e);
			rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1404);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
	// return done object.
		rebootDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		rebootDone.setErrorString("");
		rebootDone.setSuccessful(true);
		return rebootDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.23  2006/05/16 14:26:00  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.22  2004/03/26 15:06:30  cjm
// Added reboot enabling configuration.
//
// Revision 0.21  2003/07/14 12:31:46  cjm
// Changed to reflect level constants now in ngat.message.ISS_INST.REBOOT.
//
// Revision 0.20  2001/07/03 16:02:26  cjm
// Added Ccs error code base to error numbers.
//
// Revision 0.19  2001/05/15 10:50:20  cjm
// Now using ICSDRebootCommand and ICSDShutdownCommand to shutdown and reboot,
// using the new icsd_inet daemon.
//
// Revision 0.18  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 0.17  2001/03/09 17:44:17  cjm
// Fixed return values for level 2 reboot.
//
// Revision 0.16  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.15  2000/11/13 17:26:35  cjm
// Stopped error strings in exceptions being printed twice: printStackTrace prints error message.
//
// Revision 0.14  2000/11/09 14:37:55  cjm
// Changed now that reInit returns an exception when the
// directory cannot be read.
//
// Revision 0.13  2000/08/01 15:52:15  cjm
// Changed when we send the DpRt reboot command.
//
// Revision 0.12  2000/08/01 14:28:08  cjm
// Added DpRt Reboot call.
//
// Revision 0.11  2000/07/07 16:43:02  cjm
// Test version with close commented out for HARDWARE and POWER OFF levels.
//
// Revision 0.10  2000/07/06 19:04:01  cjm
// send_ccsd now uses user.dir to get absolute path name.
//
// Revision 0.9  2000/06/19 08:49:45  cjm
// Backup.
//
// Revision 0.8  2000/02/16 16:39:59  cjm
// Now using send_ccsd for HARDWARE and POWER_OFF level reboot/shutdown.
//
// Revision 0.7  2000/02/11 14:05:52  cjm
// Changed implementation, now that level is no longer a bit-field.
//
// Revision 0.6  2000/02/08 16:15:05  cjm
// Initial implementation.
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
