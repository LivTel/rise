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
// GET_STATUSImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/GET_STATUSImplementation.java,v 1.3 2017-07-29 15:33:11 cjm Exp $

import java.lang.*;
import java.util.Hashtable;

import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.ISS_TO_INST;
import ngat.message.ISS_INST.GET_STATUS;
import ngat.message.ISS_INST.GET_STATUS_DONE;
import ngat.util.ExecuteCommand;  
import ngat.util.logging.*;

/**
 * This class provides the implementation for the GET_STATUS command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class GET_STATUSImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: GET_STATUSImplementation.java,v 1.3 2017-07-29 15:33:11 cjm Exp $");
	/**
	 * Internal constant used when converting temperatures in centigrade (from the CCD controller) to Kelvin 
	 * returned in GET_STATUS.
	 * @see #getIntermediateStatus
	 */
	private final static double CENTIGRADE_TO_KELVIN = 273.15;
	/**
	 * Local copy of the Ccs status object.
	 * @see Ccs#getStatus
	 * @see CcsStatus
	 */
	private CcsStatus status = null;
	/**
	 * This hashtable is created in processCommand, and filled with status data,
	 * and is returned in the GET_STATUS_DONE object.
	 */
	private Hashtable hashTable = null;

	/**
	 * Constructor. 
	 */
	public GET_STATUSImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.GET_STATUS&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.GET_STATUS";
	}

	/**
	 * This method gets the GET_STATUS command's acknowledge time. 
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
	 * This method implements the GET_STATUS command. 
	 * The local hashTable is setup (returned in the done object) and a local copy of status setup.
	 * The current mode of the camera is returned by calling getCurrentMode.
	 * The following data is put into the hashTable:
	 * <ul>
	 * <li><b>currentCommand</b> The current command from the status object, or blank if no current command.
	 * <li><b>Instrument</b> The name of this instrument, retrieved from the property 
	 * 	<i>ccs.get_status.instrument_name</i>.
	 * <li><b>NRows, NCols</b> Number of rows and columns setup on the CCD (from libccd, not the camera hardware).
	 * <li><b>NSBin, NPBin</b> Binning factor for rows and columns setup on the CCD 
	 * 	(from libccd, not the camera hardware).
	 * <li><b>DeInterlace Type</b> The de-interlace type, which tells us how many readouts we are using 
	 * 	(from libccd, not the camera hardware).
	 * <li><b>Window Flags</b> The window flags, which tell us which windows are in effect
	 * 	(from libccd, not the camera hardware).
	 * <li><b>Setup Status</b> Whether the camera has been setup sufficiently for exposures to be taken
	 * 	(from libccd, not the camera hardware).
	 * <li><b>Exposure Start Time, Exposure Length</b> The exposure start time, and the length of the current
	 * 	(or last) exposure.
	 * <li><b>Exposure Count, Exposure Number</b> How many exposures the current command has taken and how many
	 * 	it will do in total.
	 * </ul>
	 * If the command requests a <b>INTERMEDIATE</b> level status, getIntermediateStatus is called.
	 * If the command requests a <b>FULL</b> level status, getFullStatus is called.
	 * An object of class GET_STATUS_DONE is returned, with the information retrieved.
	 * @see #status
	 * @see #hashTable
	 * @see #getCurrentMode
	 * @see #getIntermediateStatus
	 * @see #getFullStatus
	 * @see CcsStatus#getCurrentCommand
	 * @see CCDLibrary#CCDExposureGetExposureStatus
	 * @see CCDLibrary#CCDMultrunGetExposureLength
	 * @see CCDLibrary#CCDMultrunGetExposureStartTime
	 * @see CCDLibrary#CCDMultrunGetExposureNumber
	 * @see CCDLibrary#CCDSetupGetNCols
	 * @see CCDLibrary#CCDSetupGetNRows
	 * @see CCDLibrary#CCDSetupGetNSBin
	 * @see CCDLibrary#CCDSetupGetNPBin
	 * @see CCDLibrary#CCDSetupGetWindowFlags
	 * @see CCDLibrary#CCDSetupGetSetupComplete
	 * @see CcsStatus#getExposureCount
	 * @see CcsStatus#getProperty
	 * @see CcsStatus#getPropertyInteger
	 * @see CcsStatus#getPropertyBoolean
	 * @see GET_STATUS#getLevel
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		GET_STATUS getStatusCommand = (GET_STATUS)command;
		GET_STATUS_DONE getStatusDone = new GET_STATUS_DONE(command.getId());
		ISS_TO_INST currentCommand = null;
		int currentMode;

	 // Create new hashtable to be returned
		hashTable = new Hashtable();
	// get local reference to CcsStatus object.
		status = ccs.getStatus();
	// current mode
		currentMode = getCurrentMode();
		getStatusDone.setCurrentMode(currentMode);
	// What instrument is this?
		hashTable.put("Instrument",status.getProperty("ccs.get_status.instrument_name"));
	// current command
		currentCommand = status.getCurrentCommand();
		if(currentCommand == null)
			hashTable.put("currentCommand","");
		else
			hashTable.put("currentCommand",currentCommand.getClass().getName());
	// Currently, we query libccd setup stored settings, not hardware.
		hashTable.put("NCols",new Integer(libccd.CCDSetupGetNCols()));
		hashTable.put("NRows",new Integer(libccd.CCDSetupGetNRows()));
		hashTable.put("NSBin",new Integer(libccd.CCDSetupGetNSBin()));
		hashTable.put("NPBin",new Integer(libccd.CCDSetupGetNPBin()));
		hashTable.put("Window Flags",new Integer(libccd.CCDSetupGetWindowFlags()));
		hashTable.put("Setup Status",new Boolean(libccd.CCDSetupGetSetupComplete()));
		hashTable.put("Exposure Length",new Integer(libccd.CCDMultrunGetExposureLength()));
		hashTable.put("Exposure Start Time",new Long(libccd.CCDMultrunGetExposureStartTime()));
		hashTable.put("Exposure Count",new Integer(status.getExposureCount()));
		hashTable.put("Exposure Number",new Integer(libccd.CCDMultrunGetExposureNumber()));
	// intermediate level information - basic plus controller calls.
		if(getStatusCommand.getLevel() >= GET_STATUS.LEVEL_INTERMEDIATE)
		{
			getIntermediateStatus();
		}// end if intermediate level status
	// Get full status information.
		if(getStatusCommand.getLevel() >= GET_STATUS.LEVEL_FULL)
		{
			getFullStatus();
		}
	// set hashtable and return values.
		getStatusDone.setDisplayInfo(hashTable);
		getStatusDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		getStatusDone.setErrorString("");
		getStatusDone.setSuccessful(true);
	// return done object.
		return getStatusDone;
	}

	/**
	 * Internal method to get the current mode, the GET_STATUS command will return.
	 * @see #libccd
	 */
	private int getCurrentMode()
	{
		int currentMode;

		currentMode = GET_STATUS_DONE.MODE_IDLE;
		switch(libccd.CCDMultrunGetExposureStatus())
		{
			case CCDLibrary.CCD_EXPOSURE_STATUS_NONE:
				if(libccd.CCDSetupGetSetupInProgress())
					currentMode = GET_STATUS_DONE.MODE_CONFIGURING;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_CLEAR:
				currentMode =  GET_STATUS_DONE.MODE_CLEARING;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_WAIT_START:
				currentMode =  GET_STATUS_DONE.MODE_WAITING_TO_START;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_EXPOSE:
				currentMode = GET_STATUS_DONE.MODE_EXPOSING;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_PRE_READOUT:
				currentMode = GET_STATUS_DONE.MODE_PRE_READOUT;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_READOUT:
				currentMode = GET_STATUS_DONE.MODE_READING_OUT;
				break;
			case CCDLibrary.CCD_EXPOSURE_STATUS_POST_READOUT:
				currentMode = GET_STATUS_DONE.MODE_POST_READOUT;
				break;
			default:
				currentMode = GET_STATUS_DONE.MODE_ERROR;
				break;
		}
		return currentMode;
	}

	/**
	 * Routine to get status, when level INTERMEDIATE has been selected.
	 * Intermediate level status is usually useful data which can only be retrieved by querying the
	 * SDSU controller directly. 
	 * The following data is put into the hashTable:
	 * <ul>
	 * <li><b>Elapsed Exposure Time</b> The Elapsed Exposure Time, this is read from the controller.
	 * </ul>
	 * If the <i>ccs.get_status.temperature</i> boolean property is TRUE, 
	 * the following data is put into the hashTable:
	 * <ul>
	 * <li><b>Temperature</b> The current CCD (dewar) temperature, this is read from the controller.
	 * <li><b>Heater ADU</b> The current Heater ADU count, this is read from the controller.
	 * <li><b>Utility Board Temperature ADU</b> The Utility Board ADU count, 
	 * 	this is read from the utility board temperature sensor.
	 * </ul>
	 * If the <i>ccs.get_status.supply_voltages</i> boolean property is TRUE, 
	 * the following data is put into the hashTable:
	 * <ul>
	 * <li><b>High Voltage Supply ADU</b> The SDSU High Voltage supply ADU count, 
	 * 	this is read from the utility board.
	 * <li><b>Low Voltage Supply ADU</b> The SDSU Low Voltage supply ADU count, 
	 * 	this is read from the utility board.
	 * <li><b>Minus Low Voltage Supply ADU</b> The SDSU Negative Voltage supply ADU count, 
	 * 	this is read from the utility board.
	 * </ul>
	 * If the <i>ccs.get_status.pressure</i> boolean property is TRUE, 
	 * the following data is put into the hashTable:
	 * <ul>
	 * <li><b>Dewar Vacuum Gauge ADU</b> The ADU counts returned by the vacuum gauge attached to the dewar.
	 * <li><b>Dewar Vacuum Gauge</b> The pressure in the dewar, in mbar, calculated from the vacuum gauge.
	 * </ul>
	 * @see #libccd
	 * @see #status
	 * @see #hashTable
	 * @see #CENTIGRADE_TO_KELVIN
	 * @see CCDLibrary#CCDMultrunGetElapsedExposureTime
	 * @see CCDLibrary#CCDTemperatureGet
	 * @see CcsStatus#getPropertyBoolean
	 */
	private void getIntermediateStatus()
	{
		CCDLibraryDouble ccdTemperature = null;
		int elapsedExposureTime,adu;
		double dvalue;

		elapsedExposureTime = libccd.CCDMultrunGetElapsedExposureTime();
		// Always add the exposure time, if we are reading out it has been set to 0
		hashTable.put("Elapsed Exposure Time",new Integer(elapsedExposureTime));
		if(status.getPropertyBoolean("ccs.get_status.temperature"))
		{
			// CCD temperature
			// This involves a read of the utility board, which will fail when exposing...
			// Therefore only put the temperature in the hashtable on success.
			// Return temperature in degrees kelvin.
			ccdTemperature = new CCDLibraryDouble();
			try
			{
				libccd.CCDTemperatureGet(ccdTemperature);
				hashTable.put("Temperature",new Double(ccdTemperature.getValue()+
								       CENTIGRADE_TO_KELVIN));
				ccs.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
					":getIntermediateStatus:Temperature returned was:"+ccdTemperature.getValue()+
					" C.");
			}
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+
					  ":processCommand:Get Temperature failed.",e);
			}// catch
			// Dewar heater ADU counts - how much we are heating the dewar to control the temperature.
		        // Utility Board ADU counts - how hot the temperature sensor is on the utility board.
			hashTable.put("Heater ADU",new Integer(0));
		}// end if get temperature status
	}

	/**
	 * Method to get misc status, when level FULL has been selected.
	 * The following data is put into the hashTable:
	 * <ul>
	 * <li><b>Log Level</b> The current log level used by the Ccs.
	 * <li><b>Disk Usage</b> The results of running a &quot;df -k&quot;, to get the disk usage.
	 * <li><b>Process List</b> The results of running a &quot;ps -e -o pid,pcpu,vsz,ruser,stime,time,args&quot;, 
	 * 	to get the processes running on this machine.
	 * <li><b>Uptime</b> The results of running a &quot;uptime&quot;, 
	 * 	to get system load and time since last reboot.
	 * <li><b>Total Memory, Free Memory</b> The total and free memory in the Java virtual machine.
	 * <li><b>java.version, java.vendor, java.home, java.vm.version, java.vm.vendor, java.class.path</b> 
	 * 	Java virtual machine version, classpath and type.
	 * <li><b>os.name, os.arch, os.version</b> The operating system type/version.
	 * <li><b>user.name, user.home, user.dir</b> Data about the user the process is running as.
	 * <li><b>thread.list</b> A list of threads the Ccs process is running.
	 * </ul>
	 * @see #serverConnectionThread
	 * @see #hashTable
	 * @see ExecuteCommand#run
	 * @see CcsStatus#getLogLevel
	 */
	private void getFullStatus()
	{
		ExecuteCommand executeCommand = null;
		Runtime runtime = null;
		StringBuffer sb = null;
		Thread threadList[] = null;
		int threadCount;

		// log level
		hashTable.put("Log Level",new Integer(status.getLogLevel()));
		// execute 'df -k' on instrument computer
		executeCommand = new ExecuteCommand("df -k");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Disk Usage",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Disk Usage",new String(executeCommand.getException().toString()));
		// execute "ps -e -o pid,pcpu,vsz,ruser,stime,time,args" on instrument computer
		executeCommand = new ExecuteCommand("ps -e -o pid,pcpu,vsz,ruser,stime,time,args");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Process List",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Process List",new String(executeCommand.getException().toString()));
		// execute "uptime" on instrument computer
		executeCommand = new ExecuteCommand("uptime");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Uptime",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Uptime",new String(executeCommand.getException().toString()));
		// get vm memory situation
		runtime = Runtime.getRuntime();
		hashTable.put("Free Memory",new Long(runtime.freeMemory()));
		hashTable.put("Total Memory",new Long(runtime.totalMemory()));
		// get some java vm information
		hashTable.put("java.version",new String(System.getProperty("java.version")));
		hashTable.put("java.vendor",new String(System.getProperty("java.vendor")));
		hashTable.put("java.home",new String(System.getProperty("java.home")));
		hashTable.put("java.vm.version",new String(System.getProperty("java.vm.version")));
		hashTable.put("java.vm.vendor",new String(System.getProperty("java.vm.vendor")));
		hashTable.put("java.class.path",new String(System.getProperty("java.class.path")));
		hashTable.put("os.name",new String(System.getProperty("os.name")));
		hashTable.put("os.arch",new String(System.getProperty("os.arch")));
		hashTable.put("os.version",new String(System.getProperty("os.version")));
		hashTable.put("user.name",new String(System.getProperty("user.name")));
		hashTable.put("user.home",new String(System.getProperty("user.home")));
		hashTable.put("user.dir",new String(System.getProperty("user.dir")));
		// get a list of threads running in the vm
		threadCount = serverConnectionThread.activeCount();
		threadList = new Thread[threadCount];
		serverConnectionThread.enumerate(threadList);
		sb = new StringBuffer();
		for(int i = 0;i< threadCount;i++)
		{
			if(threadList[i] != null)
			{
				sb.append(threadList[i].getName());
				sb.append("\n");
			}
		}
		hashTable.put("thread.list",sb.toString());
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.2  2013/06/18 14:12:08  cjm
// Removed heater ADUs and SDSU supply voltages.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 0.30  2006/05/16 14:25:53  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.29  2004/03/03 15:57:38  cjm
// Temperature now returned in degrees Kelvin.
//
// Revision 0.28  2004/02/24 17:25:33  cjm
// Added Log Level status in full level GET_STATUS.
//
// Revision 0.27  2003/12/08 15:15:27  cjm
// Added CCD_EXPOSURE_STATUS_WAIT_START test in getCurrentMode.
//
// Revision 0.26  2003/06/09 11:31:42  cjm
// Added exposure status check error codes for VON/VOF to stop
// VON/VOF failures during readout being logged.
//
// Revision 0.25  2002/12/16 17:00:27  cjm
// Rewritten
//
// Revision 0.24  2002/09/19 14:09:15  cjm
// Added Utility Board temperature ADUs,
// SDSU supply voltage ADUs to intermediate level GET_STATUS.
//
// Revision 0.23  2001/07/13 10:18:27  cjm
// Added call to CCDTemperatureGetHeaterADU.
//
// Revision 0.22  2001/07/03 16:23:09  cjm
// Added instrument keyword.
//
// Revision 0.21  2001/02/09 18:43:41  cjm
// Changed to CCDFilterWheelGetStatus.
//
// Revision 0.20  2000/12/21 15:16:45  cjm
// Added filter wheel status calls.
//
// Revision 0.19  2000/12/20 11:20:47  cjm
// Fixed filter wheel position when -1 is returned.
//
// Revision 0.18  2000/12/19 18:25:03  cjm
// More ngat.ccd.CCDLibrary filter wheel code changes.
//
// Revision 0.17  2000/11/20 15:26:06  cjm
// Changed PS command so it is the same on Linux machines.
//
// Revision 0.16  2000/08/09 16:38:53  cjm
// Changed filter wheel code.
//
// Revision 0.15  2000/07/06 18:27:42  cjm
// Added user status.
//
// Revision 0.14  2000/07/03 10:31:23  cjm
// Updated comment.
//
// Revision 0.13  2000/07/03 09:53:09  cjm
// Level test fix.
//
// Revision 0.12  2000/07/03 09:35:36  cjm
// INTERMEDIATE level added, BASIC level information that queried SDSU electronics
// directly now stored at this level. e.g. Elapsed exposure time and current temperature.
//
// Revision 0.11  2000/06/19 08:49:45  cjm
// Backup.
//
// Revision 0.10  2000/06/13 18:00:09  cjm
// Fixed filterwheel.count property fetch.
//
// Revision 0.9  2000/03/20 15:56:27  cjm
// GET_STATUS now calls libccd methods that send command to the controller.
// We can do this, because libccd was built with mutex locking round
// SDSU controller commands.
//
// Revision 0.8  2000/03/02 10:48:45  cjm
// Added comment.
//
// Revision 0.7  2000/03/01 14:52:50  cjm
// Added thread.list status.
//
// Revision 0.6  2000/02/28 19:14:00  cjm
// Backup.
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
