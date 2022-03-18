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
// CcsStatus.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsStatus.java,v 1.1 2009-10-15 10:21:18 cjm Exp $
import java.lang.*;
import java.io.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.util.PersistentUniqueInteger;
import ngat.util.FileUtilitiesNativeException;
import ngat.util.logging.FileLogHandler;

/**
 * This class holds status information for the Ccs program.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CcsStatus
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsStatus.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * Default filename containing network properties for ccs.
	 */
	private final static String DEFAULT_NET_PROPERTY_FILE_NAME = "./ccs.net.properties";
	/**
	 * Default filename containing properties for ccs.
	 */
	private final static String DEFAULT_PROPERTY_FILE_NAME = "./ccs.properties";
	/**
	 * Default filename containing filter wheel properties for ccs.
	 */
	private final static String DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME = "../../config/current.filter.properties";
	/**
	 * Default filename containing filter wheel properties for ccs.
	 */
	private final static String DEFAULT_FILTER_PROPERTY_FILE_NAME = "../../config/filter.properties";

	/**
	 * The logging level.
	 */
	private int logLevel = CcsConstants.CCS_LOG_LEVEL_NONE;
	/**
	 * The current thread that the CCD Control System is using to process the
	 * <a href="#currentCommand">currentCommand</a>. This does not get set for
	 * commands that can be sent while others are in operation, such as Abort and get status comamnds.
	 * This can be null when no command is currently being processed.
	 */
	private Thread currentThread = null;
	/**
	 * The current command that the CCD Control System is working on. This does not get set for
	 * commands that can be sent while others are in operation, such as Abort and get status comamnds.
	 * This can be null when no command is currently being processed.
	 */
	private ISS_TO_INST currentCommand = null;
	/**
	 * A list of properties held in the properties file. This contains configuration information in ccs
	 * that needs to be changed irregularily.
	 */
	private Properties properties = null;
	/**
	 * The count of the number of exposures needed for the current command to be implemented.
	 */
	private int exposureCount = 0;
	/**
	 * The number of the current exposure being taken.
	 */
	private int exposureNumber = 0;
	/**
	 * The filename of the current exposure being taken (if any).
	 */
	private String exposureFilename = null;
	/**
	 * The current unique config ID, held on disc over reboots.
	 * Incremented each time a new configuration is attained,
	 * and stored in the FITS header.
	 */
	private PersistentUniqueInteger configId = null;
	/**
	 * The name of the ngat.phase2.CCDConfig object instance that was last used	
	 * to configure the CCD Camera (via an ngat.message.ISS_INST.CONFIG message).
	 * Used for the CONFNAME FITS keyword value.
	 * Initialised to 'UNKNOWN', so that if we try to take a frame before configuring the CCD
	 * we get an error about setup not being complete, rather than an error about NULL FITS values.
	 */
	private String configName = "UNKNOWN";
	/**
	 * A list of Java longs holding the current time in milliseconds each time a 
	 * pause exposure command was initiated during an exposure.
	 */
	private Vector pauseTimeList = null;
	/**
	 * A list of Java longs holding the current time in milliseconds each time a 
	 * resume exposure command was initiated during an exposure.
	 */
	private Vector resumeTimeList = null;
	/**
	 * Cached value of calibrateBefore boolean specified by last actioned CONFIG command.
	 */
	private boolean cachedCalibrateBefore = false;
	/**
	 * Cached value of calibrateAfter boolean specified by last actioned CONFIG command.
	 */
	private boolean cachedCalibrateAfter = false;

	/**
	 * Default constructor. Initialises the pause and resume time lists, and the properties.
	 * @see #pauseTimeList
	 * @see #resumeTimeList
	 * @see #properties
	 */
	public CcsStatus()
	{
		pauseTimeList = new Vector();
		resumeTimeList = new Vector();
		properties = new Properties();
	}

	/**
	 * The load method for the class. This loads the property file from disc, using the specified
	 * filename. Any old properties are first cleared.
	 * The configId unique persistent integer is then initialised, using a filename stored in the properties.
	 * @param netFilename The filename of a Java property file containing network configuration for the Ccs.
	 * 	If netFilename is null, DEFAULT_NET_PROPERTY_FILE_NAME is used.
	 * @param filename The filename of a Java property file containing general configuration for the Ccs.
	 * 	If filename is null, DEFAULT_PROPERTY_FILE_NAME is used.
	 * @param currentFilterFilename The filename of a Java property file containing filter 
	 * 	configuration for the Ccs. This is the filters currently resident in the filter wheel.
	 * 	If currentFilterFilename is null, DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME is used.
	 * @param filterFilename The filename of a Java property file containing filter configuration for the Ccs. 
	 * 	This is the filter database of filters the LT has.
	 * 	If filterFilename is null, DEFAULT_FILTER_PROPERTY_FILE_NAME is used. 
	 * @see #properties
	 * @see #initialiseConfigId
	 * @see #DEFAULT_NET_PROPERTY_FILE_NAME
	 * @see #DEFAULT_PROPERTY_FILE_NAME
	 * @see #DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME
	 * @see #DEFAULT_FILTER_PROPERTY_FILE_NAME
	 * @exception FileNotFoundException Thrown if a configuration file is not found.
	 * @exception IOException Thrown if an IO error occurs whilst loading a configuration file.
	 */
	public void load(String netFilename,String filename,
		String currentFilterFilename, String filterFilename) throws FileNotFoundException, IOException
	{
		FileInputStream fileInputStream = null;

	// clear old properties
		properties.clear();
	// network properties load
		if(netFilename == null)
			netFilename = DEFAULT_NET_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(netFilename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// normal properties load
		if(filename == null)
			filename = DEFAULT_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(filename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// current filter properties load
		if(currentFilterFilename == null)
			currentFilterFilename = DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(currentFilterFilename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// filter properties load
		if(filterFilename == null)
			filterFilename = DEFAULT_FILTER_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(filterFilename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// initialise configId
		initialiseConfigId();
	}

	/**
	 * The reload method for the class. This reloads the specified property files from disc.
	 * The current properties are not cleared, as network properties are not re-loaded, as this would
	 * involve resetting up the server connection thread which may be in use. If properties have been
	 * deleted from the loaded files, reload does not clear these properties. Any new properties or
	 * ones where the values have changed will change.
	 * The configId unique persistent integer is then initialised, using a filename stored in the properties.
	 * @param filename The filename of a Java property file containing general configuration for the Ccs.
	 * 	If filename is null, DEFAULT_PROPERTY_FILE_NAME is used.
	 * @param currentFilterFilename The filename of a Java property file containing filter 
	 * 	configuration for the Ccs. This is the filters currently resident in the filter wheel.
	 * 	If currentFilterFilename is null, DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME is used.
	 * @param filterFilename The filename of a Java property file containing filter configuration for the Ccs.
	 * 	This is the filter database of filters the LT has.
	 * 	If filterFilename is null, DEFAULT_FILTER_PROPERTY_FILE_NAME is used.
	 * @see #properties
	 * @see #initialiseConfigId
	 * @see #DEFAULT_NET_PROPERTY_FILE_NAME
	 * @see #DEFAULT_PROPERTY_FILE_NAME
	 * @see #DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME
	 * @see #DEFAULT_FILTER_PROPERTY_FILE_NAME
	 * @exception FileNotFoundException Thrown if a configuration file is not found.
	 * @exception IOException Thrown if an IO error occurs whilst loading a configuration file.
	 */
	public void reload(String filename,
		String currentFilterFilename,String filterFilename) throws FileNotFoundException,IOException
	{
		FileInputStream fileInputStream = null;

	// don't clear old properties, the network properties are not re-loaded
	// normal properties load
		if(filename == null)
			filename = DEFAULT_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(filename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// current filter properties load
		if(currentFilterFilename == null)
			currentFilterFilename = DEFAULT_CURRENT_FILTER_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(currentFilterFilename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// filter properties load
		if(filterFilename == null)
			filterFilename = DEFAULT_FILTER_PROPERTY_FILE_NAME;
		fileInputStream = new FileInputStream(filterFilename);
		properties.load(fileInputStream);
		fileInputStream.close();
	// initialise configId
		initialiseConfigId();
	}

	/**
	 * Set the logging level for Ccs.
	 * @param level The level of logging.
	 */
	public synchronized void setLogLevel(int level)
	{
		logLevel = level;
	}

	/**
	 * Get the logging level for Ccs.
	 * @return The current log level.
	 */	
	public synchronized int getLogLevel()
	{
		return logLevel;
	}

	/**
	 * Set the command that is currently executing.
	 * @param command The command that is currently executing.
	 */
	public synchronized void setCurrentCommand(ISS_TO_INST command)
	{
		currentCommand = command;
	}

	/**
	 * Get the the command the Ccs is currently processing.
	 * @return The command currently being processed.
	 */
	public synchronized ISS_TO_INST getCurrentCommand()
	{
		return currentCommand;
	}

	/**
	 * Set the thread that is currently executing the <a href="#currentCommand">currentCommand</a>.
	 * @param thread The thread that is currently executing.
	 */
	public synchronized void setCurrentThread(Thread thread)
	{
		currentThread = thread;
	}

	/**
	 * Get the the thread the Ccs is currently executing to process the 
	 * <a href="#currentCommand">currentCommand</a>.
	 * @return The thread currently being executed.
	 */
	public synchronized Thread getCurrentThread()
	{
		return currentThread;
	}

	/**
	 * This routine determines whether a command can be run given the current status of the Ccs.
	 * If the Ccs is currently not running a command the command can be run.
	 * If the command is getting status we can generally run this whilst other things are going on.
	 * If the command is a reboot or abort command that tells the Ccs to stop what it is going this is
	 * generally allowed to be run, otherwise we couldn't stop execution of exposures mid-exposure.
	 * @param command The command we want to run.
	 * @return Whether the command can be run given the current status of the system.
	 */
	public synchronized boolean commandCanBeRun(ISS_TO_INST command)
	{
		if(currentCommand == null)
			return true;
		if(command instanceof INTERRUPT)
			return true;
		return false;
	}

	/**
	 * Set the number of exposures needed to complete the current command implementation.
	 * @param c The total number of exposures needed.
	 * @see #exposureCount
	 */
	public synchronized void setExposureCount(int c)
	{
		exposureCount = c;
	}

	/**
	 * Get the number of exposures needed to complete the current command implementation.
	 * @return Returns the number of exposures needed.
	 * @see #exposureCount
	 */
	public synchronized int getExposureCount()
	{
		return exposureCount;
	}

	/**
	 * Set the current exposure number the current command implementation is on.
	 * @param n The current exposure number.
	 * @see #exposureNumber
	 */
	public synchronized void setExposureNumber(int n)
	{
		exposureNumber = n;
	}

	/**
	 * Get the current exposure number the current command implementation is on.
	 * @return Returns the current exposure number.
	 * @see #exposureNumber
	 */
	public synchronized int getExposureNumber()
	{
		return exposureNumber;
	}

	/**
	 * Set the current exposure filename being taken.
	 * @param f The current filename.
	 * @see #exposureFilename
	 */
	public synchronized void setExposureFilename(String f)
	{
		exposureFilename = f;
	}

	/**
	 * Get the current exposure filename.
	 * @return Returns the current exposure filename.
	 * @see #exposureFilename
	 */
	public synchronized String getExposureFilename()
	{
		return exposureFilename;
	}

	/**
	 * Method to change (increment) the unique ID number of the last
	 * ngat.phase2.CCDConfig instance to successfully configure the CCD camera.
	 * This is done by calling <i>configId.increment()</i>.
	 * @see #configId
	 * @see ngat.util.PersistentUniqueInteger#increment
	 * @exception FileUtilitiesNativeException Thrown if <i>PersistentUniqueInteger.increment()</i> fails.
	 * @exception NumberFormatException Thrown if <i>PersistentUniqueInteger.increment()</i> fails.
	 * @exception Exception Thrown if <i>PersistentUniqueInteger.increment()</i> fails.
	 */
	public synchronized void incConfigId() throws FileUtilitiesNativeException,
		NumberFormatException, Exception
	{
		configId.increment();
	}

	/**
	 * Method to get the unique config ID number of the last
	 * ngat.phase2.CCDConfig instance to successfully configure the CCD camera.
	 * @return The unique config ID number.
	 * This is done by calling <i>configId.get()</i>.
	 * @see #configId
	 * @see ngat.util.PersistentUniqueInteger#get
	 * @exception FileUtilitiesNativeException Thrown if <i>PersistentUniqueInteger.get()</i> fails.
	 * @exception NumberFormatException Thrown if <i>PersistentUniqueInteger.get()</i> fails.
	 * @exception Exception Thrown if <i>PersistentUniqueInteger.get()</i> fails.
	 */
	public synchronized int getConfigId() throws FileUtilitiesNativeException,
		NumberFormatException, Exception
	{
		return configId.get();
	}

	/**
	 * Method to set our reference to the string identifier of the last
	 * ngat.phase2.CCDConfig instance to successfully configure the CCD camera.
	 * @param s The string from the configuration object instance.
	 * @see #configName
	 */
	public synchronized void setConfigName(String s)
	{
		configName = s;
	}

	/**
	 * Mehtod to get the string identifier of the last
	 * ngat.phase2.CCDConfig instance to successfully configure the CCD camera.
	 * @return The string identifier, or null if the CCD camera has not been configured
	 * 	since the Ccs started.
	 * @see #configName
	 */
	public synchronized String getConfigName()
	{
		return configName;
	}

	/**
	 * Method to add a time a pause was initiated to the list. The time is put into a Long object.
	 * @param time A time, in milliseconds since the 1st January 1970, that a pause was initiated.
	 * @see #pauseTimeList
	 */
	public void addPauseTime(long time)
	{
		pauseTimeList.add(new Long(time));
	}

	/**
	 * Method to add a time a resume was initiated to the list. The time is put into a Long object.
	 * @param time A time, in milliseconds since the 1st January 1970, that a resume was initiated.
	 * @see #resumeTimeList
	 */
	public void addResumeTime(long time)
	{
		resumeTimeList.add(new Long(time));
	}

	/**
	 * Method to clear the pause and resume times held in the status. The <b>clear</b> methods of the
	 * two lists are called.
	 * @see #pauseTimeList
	 * @see #resumeTimeList
	 */
	public void clearPauseResumeTimes()
	{
		pauseTimeList.clear();
		resumeTimeList.clear();
	}

	/**
	 * Method to get the number of times in the Pause time list.
	 * @return The number of times in the Pause time list.
	 * @see #pauseTimeList
	 */
	public int getPauseTimeListSize()
	{
		return pauseTimeList.size();
	}

	/**
	 * Method to get one of the pause times held in the pause time list. The time is returned 
	 * as a Long object, the value of which is the number of milliseconds after 1st January 1970 the
	 * pause was initiated.
	 * @param index The index in the pause time list to get the time for.
	 * @return A Long object, the contents of which are the milliseconds since 1st January 1970 the
	 * 	(index +1)th pause occured during the last exposure.
	 * @exception ArrayIndexOutOfBoundsException Thrown if the index is not in the list.
	 */
	public Long getPauseTime(int index) throws ArrayIndexOutOfBoundsException
	{
		return (Long)(pauseTimeList.get(index));
	}

	/**
	 * Method to get the number of times in the resume time list.
	 * @return The number of times in the resume time list.
	 * @see #resumeTimeList
	 */
	public int getResumeTimeListSize()
	{
		return resumeTimeList.size();
	}

	/**
	 * Method to get one of the resume times held in the resume time list. The time is returned 
	 * as a Long object, the value of which is the number of milliseconds after 1st January 1970 the
	 * resume was initiated.
	 * @param index The index in the resume time list to get the time for.
	 * @return A Long object, the contents of which are the milliseconds since 1st January 1970 the
	 * 	(index +1)th resume occured during the last exposure.
	 * @exception ArrayIndexOutOfBoundsException Thrown if the index is not in the list.
	 */
	public Long getResumeTime(int index) throws ArrayIndexOutOfBoundsException
	{
		return (Long)(resumeTimeList.get(index));
	}

	/**
	 * Set the value of the calibrate before boolean specified in the last actioned CONFIG command.
	 * @param b The boolean's value.
	 * @see #cachedCalibrateBefore
	 */
	public void setCachedCalibrateBefore(boolean b)
	{
		cachedCalibrateBefore = b;
	}

	/**
	 * Get the value of the calibrate before boolean specified in the last actioned CONFIG command.
	 * @return The boolean's value.
	 * @see #cachedCalibrateBefore
	 */
	public boolean getCachedCalibrateBefore()
	{
		return cachedCalibrateBefore;
	}

	/**
	 * Set the value of the calibrate after boolean specified in the last actioned CONFIG command.
	 * @param b The boolean's value.
	 * @see #cachedCalibrateAfter
	 */
	public void setCachedCalibrateAfter(boolean b)
	{
		cachedCalibrateAfter = b;
	}

	/**
	 * Get the value of the calibrate after boolean specified in the last actioned CONFIG command.
	 * @return The boolean's value.
	 * @see #cachedCalibrateAfter
	 */
	public boolean getCachedCalibrateAfter()
	{
		return cachedCalibrateAfter;
	}

	/**
	 * Method to return whether the loaded properties contain the specified keyword.
	 * Calls the proprties object containsKey method. Note assumes the properties object has been initialised.
	 * @param p The property key we wish to test exists.
	 * @return The method returnd true if the specified key is a key in out list of properties,
	 *         otherwise it returns false.
	 * @see #properties
	 */
	public boolean propertyContainsKey(String p)
	{
		return properties.containsKey(p);
	}

	/**
	 * Routine to get a properties value, given a key. Just calls the properties object getProperty routine.
	 * @param p The property key we want the value for.
	 * @return The properties value, as a string object.
	 * @see #properties
	 */
	public String getProperty(String p)
	{
		return properties.getProperty(p);
	}

	/**
	 * Routine to get a properties value, given a key. The value must be a valid integer, else a 
	 * NumberFormatException is thrown.
	 * @param p The property key we want the value for.
	 * @return The properties value, as an integer.
	 * @exception NumberFormatException If the properties value string is not a valid integer, this
	 * 	exception will be thrown when the Integer.parseInt routine is called.
	 * @see #properties
	 */
	public int getPropertyInteger(String p) throws NumberFormatException
	{
		String valueString = null;
		int returnValue = 0;

		valueString = properties.getProperty(p);
		try
		{
			returnValue = Integer.parseInt(valueString);
		}
		catch(NumberFormatException e)
		{
			// re-throw exception with more information e.g. keyword
			throw new NumberFormatException(this.getClass().getName()+":getPropertyInteger:keyword:"+
				p+":valueString:"+valueString);
		}
		return returnValue;
	}

	/**
	 * Routine to get a properties value, given a key. The value must be a valid long, else a 
	 * NumberFormatException is thrown.
	 * @param p The property key we want the value for.
	 * @return The properties value, as a long.
	 * @exception NumberFormatException If the properties value string is not a valid long, this
	 * 	exception will be thrown when the Long.parseLong routine is called.
	 * @see #properties
	 */
	public long getPropertyLong(String p) throws NumberFormatException
	{
		String valueString = null;
		long returnValue = 0;

		valueString = properties.getProperty(p);
		try
		{
			returnValue = Long.parseLong(valueString);
		}
		catch(NumberFormatException e)
		{
			// re-throw exception with more information e.g. keyword
			throw new NumberFormatException(this.getClass().getName()+":getPropertyLong:keyword:"+
				p+":valueString:"+valueString);
		}
		return returnValue;
	}

	/**
	 * Routine to get a properties value, given a key. The value must be a valid double, else a 
	 * NumberFormatException is thrown.
	 * @param p The property key we want the value for.
	 * @return The properties value, as an double.
	 * @exception NumberFormatException If the properties value string is not a valid double, this
	 * 	exception will be thrown when the Double.valueOf routine is called.
	 * @see #properties
	 */
	public double getPropertyDouble(String p) throws NumberFormatException
	{
		String valueString = null;
		Double returnValue = null;

		valueString = properties.getProperty(p);
		try
		{
			returnValue = Double.valueOf(valueString);
		}
		catch(NumberFormatException e)
		{
			// re-throw exception with more information e.g. keyword
			throw new NumberFormatException(this.getClass().getName()+":getPropertyDouble:keyword:"+
				p+":valueString:"+valueString);
		}
		return returnValue.doubleValue();
	}

	/**
	 * Routine to get a properties boolean value, given a key. The properties value should be either 
	 * "true" or "false".
	 * Boolean.valueOf is used to convert the string to a boolean value.
	 * @param p The property key we want the boolean value for.
	 * @return The properties value, as an boolean.
	 * @exception NullPointerException If the properties value string is null, this
	 * 	exception will be thrown.
	 * @see #properties
	 */
	public boolean getPropertyBoolean(String p) throws NullPointerException
	{
		String valueString = null;
		Boolean b = null;

		valueString = properties.getProperty(p);
		if(valueString == null)
		{
			throw new NullPointerException(this.getClass().getName()+":getPropertyBoolean:keyword:"+
				p+":Value was null.");
		}
		b = Boolean.valueOf(valueString);
		return b.booleanValue();
	}

	/**
	 * Routine to get an integer representing a ngat.util.logging.FileLogHandler time period.
	 * The value of the specified property should contain either:'HOURLY_ROTATION', 'DAILY_ROTATION' or
	 * 'WEEKLY_ROTATION'.
	 * @param p The property key we want the time period value for.
	 * @return The properties value, as an FileLogHandler time period (actually an integer).
	 * @exception NullPointerException If the properties value string is null an exception is thrown.
	 * @exception IllegalArgumentException If the properties value string is not a valid time period,
	 *            an exception is thrown.
	 * @see #properties
	 */
	public int getPropertyLogHandlerTimePeriod(String p) throws NullPointerException, IllegalArgumentException
	{
		String valueString = null;
		int timePeriod = 0;
 
		valueString = properties.getProperty(p);
		if(valueString == null)
		{
			throw new NullPointerException(this.getClass().getName()+
						       ":getPropertyLogHandlerTimePeriod:keyword:"+
						       p+":Value was null.");
		}
		if(valueString.equals("HOURLY_ROTATION"))
			timePeriod = FileLogHandler.HOURLY_ROTATION;
		else if(valueString.equals("DAILY_ROTATION"))
			timePeriod = FileLogHandler.DAILY_ROTATION;
		else if(valueString.equals("WEEKLY_ROTATION"))
			timePeriod = FileLogHandler.WEEKLY_ROTATION;
		else
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":getPropertyLogHandlerTimePeriod:keyword:"+
							   p+":Illegal value:"+valueString+".");
		}
		return timePeriod;
	}

	/**
	 * Method to get the position of a filter in a filter wheel. The filter wheel number (equivalent to
	 * upper/lower) and the type name of the filter are passed in. The number of the filter in this filter
	 * wheel is returned, or an IllegalArgumentException is thrown if the filter wheel does not exist
	 * or the filter type name does not exist.
	 * @param filterWheelNumber The filter wheel number, starting at 0.
	 * @param filterTypeName The type name of the filter.
	 * @return Returns the number of the filter in the specified wheel.
	 * @exception IllegalArgumentException Thrown if a parameter is an illegal value, or the 
	 * 	property file is not setup correctly.
	 * @exception NumberFormatException Thrown if some of the properties are not a valid integer when
	 * 	they should be.
	 * @see #getPropertyInteger
	 */
	public int getFilterWheelPosition(int filterWheelNumber,String filterTypeName) throws IllegalArgumentException,
		NumberFormatException
	{
		String s = null;
		int filterWheelCount;
		int filterWheelFilterCount;
		int filterWheelFilterIndex;
		boolean found;

	// check filterWheelNumber is legal.
		filterWheelCount = getPropertyInteger("filterwheel.count");
		if(filterWheelNumber >= filterWheelCount)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterWheelPosition:Illegal filterWheelNumber:"+filterWheelNumber+
				" of "+filterWheelCount);
		}
	// get number of filters in this wheel.
		filterWheelFilterCount = getPropertyInteger("filterwheel."+filterWheelNumber+".count");
	// compare type name against filters in wheel. Stop when we find the first match.
		filterWheelFilterIndex = 0;
		found = false;
		while((!found)&&(filterWheelFilterIndex<filterWheelFilterCount))
		{
		// get the filter type name at index filterWheelFilterIndex into s
			s = getProperty("filterwheel."+filterWheelNumber+"."+filterWheelFilterIndex+".type");
		// is it the right filter?
			if(s != null)
				found = s.equals(filterTypeName);
		// if it isn't try the next filter.
			if(found == false)
				filterWheelFilterIndex++;
		}// end while
		if(found)
		{
			return filterWheelFilterIndex;
		}
		else
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterWheelPosition:Illegal filter:"+filterWheelNumber+":"+filterTypeName);
		}
	}

	/**
	 * Method to get the type name of a filter in a filter wheel. The filter wheel number (equivalent to
	 * upper/lower) and the position of the wheel are passed in. The type name of the filter in that position 
	 * in the filter wheel is returned, or an IllegalArgumentException is thrown if the filter wheel does not exist
	 * or the position does not exist.
	 * @param filterWheelNumber The filter wheel number, starting at 0.
	 * @param filterWheelPosition The position of that filter wheel.
	 * @return Returns the type name of the filter in the specified wheel and position.
	 * @exception IllegalArgumentException Thrown if a parameter is an illegal value, or the 
	 * 	property file is not setup correctly.
	 * @exception NumberFormatException Thrown if some of the properties are not a valid integer when
	 * 	they should be.
	 * @see #getPropertyInteger
	 */
	public String getFilterTypeName(int filterWheelNumber,int filterWheelPosition) 
		throws IllegalArgumentException,NumberFormatException
	{
		String s = null;
		int filterWheelCount;
		int filterWheelFilterCount;

	// check filterWheelNumber is legal.
		filterWheelCount = getPropertyInteger("filterwheel.count");
		if((filterWheelNumber < 0)||(filterWheelNumber >= filterWheelCount))
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterWheelName:Illegal filterWheelNumber:"+filterWheelNumber+
				" of "+filterWheelCount);
		}
	// check position is legal
		filterWheelFilterCount = getPropertyInteger("filterwheel."+filterWheelNumber+".count");
		if((filterWheelPosition < 0)||(filterWheelPosition >= filterWheelFilterCount))
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterWheelName:Illegal filterWheelPosition:"+filterWheelPosition+
				" of "+filterWheelFilterCount);
		}
	// get the filter type name  into s
		s = getProperty("filterwheel."+filterWheelNumber+"."+filterWheelPosition+".type");
		if(s == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterWheelName:Filter name not found in property:filterwheel."+
				filterWheelNumber+"."+filterWheelPosition);
		}
		return s;
	}

	/**
	 * Method to get a filter's Id name from it's type name. This information is stored in the
	 * per-semester filter property file, under the 'filterwheel.<filterTypeName>.id' property.
	 * @param filterTypeName The filter type name to get the actual filter id from.
	 * @return A string, which is the unique filter id for this type of filter.
	 * @exception IllegalArgumentException Thrown if the specified property cannot be found.
	 */
	public String getFilterIdName(String filterTypeName)
	{
		String s = null;

	// get the filter id name into s
		s = getProperty("filterwheel."+filterTypeName+".id");
		if(s == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterIdName:Filter id name not found in property:filterwheel."+
				filterTypeName+".id");
		}
		return s;
	}

	/**
	 * Method to get a filter's Id optical thickness from it's name . This information is stored in the
	 * filter database property file, under the 'filter.<filterIdName>.optical_thickness' property.
	 * The filter's Id string can be retrieved from a filter type string using getFilterIdName.
	 * @param filterIdName The filter id name to get the optical thickness for.
	 * @return A double, which is the optical thickness of the given filter.
	 * @exception IllegalArgumentException Thrown if the specified property/filter id cannot be found.
	 * @exception NumberFormatException Thrown if the property cannot be parsed.
	 * @see #getFilterIdName
	 */
	public double getFilterIdOpticalThickness(String filterIdName) throws NumberFormatException
	{
		String s = null;

	// get the filter id name into s
		s = getProperty("filter."+filterIdName+".optical_thickness");
		if(s == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterIdOpticalThickness:Property not found/is null:filter."+
				filterIdName+".optical_thickness");
		}
		return Double.parseDouble(s);
	}

	/**
	 * Method to get a filter's Id central wave-length from it's name . This information is stored in the
	 * filter database property file, under the 'filter.<filterIdName>.center' property.
	 * The filter's Id string can be retrieved from a filter type string using getFilterIdName.
	 * @param filterIdName The filter id name to get the central wave-length for.
	 * @return A double, which is the central wave-length of the given filter.
	 * @exception IllegalArgumentException Thrown if the specified property/filter id cannot be found.
	 * @exception NumberFormatException Thrown if the property cannot be parsed into a double.
	 * @see #getFilterIdName
	 */
	public double getFilterIdWaveLength(String filterIdName) throws NumberFormatException
	{
		String s = null;

	// get the filter id name into s
		s = getProperty("filter."+filterIdName+".center");
		if(s == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
				":getFilterIdWaveLength:Property not found/is null:filter."+
				filterIdName+".center");
		}
		return Double.parseDouble(s);
	}

	/**
	 * Method to get the number of columns to tell the SDSU controller to read out, for a specified
	 * binning factor. The binning factor is needed for DUAL readout amplifiers, which need different
	 * dimensions to be sent to the controller depending on the binning setting, to stop bias strips appearing
	 * in the centre of the image, or missing central columns.
	 * This information is stored in the Ccs property file, under the 'ccs.config.ncols.<binning factor>' property.
	 * @param xbin The X binning factor to get the number of columns for.
	 * @return An integer, the number of columns to configure the controller with.
	 * @exception NumberFormatException Thrown if the property cannot be found, or parsed into a valid int.
	 * @see #getPropertyInteger
	 */
	public int getNumberColumns(int xbin) throws NumberFormatException
	{
		return getPropertyInteger("ccs.config.ncols."+xbin);
	}

	/**
	 * Method to get the number of rows to tell the SDSU controller to read out, for a specified
	 * binning factor. The binning factor is not really needed when computing nrows, but is retained for 
	 * completness against ncols configuration.
	 * This information is stored in the Ccs property file, under the 'ccs.config.nrows.<binning factor>' property.
	 * @param ybin The Y binning factor to get the number of rows for.
	 * @return An integer, the number of rows to configure the controller with.
	 * @exception NumberFormatException Thrown if the property cannot be found, or parsed into a valid int.
	 * @see #getPropertyInteger
	 */
	public int getNumberRows(int ybin) throws NumberFormatException
	{
		return getPropertyInteger("ccs.config.nrows."+ybin);
	}

	/**
	 * Method to get the thread priority to run the server thread at.
	 * The value is retrieved from the <b>ccs.thread.priority.server</b> property.
	 * If this fails the default CCS_DEFAULT_THREAD_PRIORITY_SERVER is returned.
	 * @return A valid thread priority between threads MIN_PRIORITY and MAX_PRIORITY.
	 * @see CcsConstants#CCS_DEFAULT_THREAD_PRIORITY_SERVER
	 */
	public int getThreadPriorityServer()
	{
		int retval;

		try
		{
			retval = getPropertyInteger("ccs.thread.priority.server");
			if(retval < Thread.MIN_PRIORITY)
				retval = Thread.MIN_PRIORITY;
			if(retval > Thread.MAX_PRIORITY)
				retval = Thread.MAX_PRIORITY;
		}
		catch(NumberFormatException e)
		{
			retval = CcsConstants.CCS_DEFAULT_THREAD_PRIORITY_SERVER;
		}
		return retval;
	}

	/**
	 * Method to get the thread priority to run interrupt threads at.
	 * The value is retrieved from the <b>ccs.thread.priority.interrupt</b> property.
	 * If this fails the default CCS_DEFAULT_THREAD_PRIORITY_INTERRUPT is returned.
	 * @return A valid thread priority between threads MIN_PRIORITY and MAX_PRIORITY.
	 * @see CcsConstants#CCS_DEFAULT_THREAD_PRIORITY_INTERRUPT
	 */
	public int getThreadPriorityInterrupt()
	{
		int retval;

		try
		{
			retval = getPropertyInteger("ccs.thread.priority.interrupt");
			if(retval < Thread.MIN_PRIORITY)
				retval = Thread.MIN_PRIORITY;
			if(retval > Thread.MAX_PRIORITY)
				retval = Thread.MAX_PRIORITY;
		}
		catch(NumberFormatException e)
		{
			retval = CcsConstants.CCS_DEFAULT_THREAD_PRIORITY_INTERRUPT;
		}
		return retval;
	}

	/**
	 * Method to get the thread priority to run normal threads at.
	 * The value is retrieved from the <b>ccs.thread.priority.normal</b> property.
	 * If this fails the default CCS_DEFAULT_THREAD_PRIORITY_NORMAL is returned.
	 * @return A valid thread priority between threads MIN_PRIORITY and MAX_PRIORITY.
	 * @see CcsConstants#CCS_DEFAULT_THREAD_PRIORITY_NORMAL
	 */
	public int getThreadPriorityNormal()
	{
		int retval;

		try
		{
			retval = getPropertyInteger("ccs.thread.priority.normal");
			if(retval < Thread.MIN_PRIORITY)
				retval = Thread.MIN_PRIORITY;
			if(retval > Thread.MAX_PRIORITY)
				retval = Thread.MAX_PRIORITY;
		}
		catch(NumberFormatException e)
		{
			retval = CcsConstants.CCS_DEFAULT_THREAD_PRIORITY_NORMAL;
		}
		return retval;
	}

	/**
	 * Method to get the thread priority to run the Telescope Image Transfer server and client 
	 * connection threads at.
	 * The value is retrieved from the <b>ccs.thread.priority.tit</b> property.
	 * If this fails the default CCS_DEFAULT_THREAD_PRIORITY_TIT is returned.
	 * @return A valid thread priority between threads MIN_PRIORITY and MAX_PRIORITY.
	 * @see CcsConstants#CCS_DEFAULT_THREAD_PRIORITY_TIT
	 */
	public int getThreadPriorityTIT()
	{
		int retval;

		try
		{
			retval = getPropertyInteger("ccs.thread.priority.tit");
			if(retval < Thread.MIN_PRIORITY)
				retval = Thread.MIN_PRIORITY;
			if(retval > Thread.MAX_PRIORITY)
				retval = Thread.MAX_PRIORITY;
		}
		catch(NumberFormatException e)
		{
			retval = CcsConstants.CCS_DEFAULT_THREAD_PRIORITY_TIT;
		}
		return retval;
	}

	/**
	 * Internal method to initialise the configId field. This is not done during construction
	 * as the property files need to be loaded to determine the filename to use.
	 * This is got from the <i>ccs.config.unique_id_filename</i> property.
	 * The configId field is then constructed.
	 * @see #configId
	 */
	private void initialiseConfigId()
	{
		String fileName = null;

		fileName = getProperty("ccs.config.unique_id_filename");
		configId = new PersistentUniqueInteger(fileName);
	}

}

// $Log: not supported by cvs2svn $
// Revision 1.29  2006/05/16 14:25:44  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.28  2005/09/29 14:04:55  cjm
// Added caching of config calibrateBefore and calibrateAfter.
//
// Revision 1.27  2005/07/26 15:54:16  cjm
// Documentation fix.
//
// Revision 1.26  2004/02/20 14:55:08  cjm
// Added propertyContainsKey and getPropertyLogHandlerTimePeriod.
//
// Revision 1.25  2003/06/06 12:47:15  cjm
// Windowing changes, added getNumberColumns and getNumberRows.
//
// Revision 1.24  2003/01/28 16:26:08  cjm
// Improved error reporting on getting data from config files.
//
// Revision 1.23  2001/12/14 14:27:49  cjm
// Changed default filter properties filenames.
//
// Revision 1.22  2001/08/16 16:28:33  cjm
// Added getPropertyLong.
//
// Revision 1.21  2001/07/31 14:17:16  cjm
// Changed Utilites for Utilities.
//
// Revision 1.20  2001/07/03 16:31:58  cjm
// Added PersistentUniqueInteger implementation for CONFIGID FITS keyword.
// Changed current setConfigId/getConfigId to setConfigName/getConfigName.
// Added getThreadPriorityTIT.
//
// Revision 1.19  2001/02/12 10:19:10  cjm
// Added exposureFilename.
//
// Revision 1.18  2000/08/09 17:36:55  cjm
// Changed filter.properties access. Uses new filter.properties format.
// Now two filter.properties file, a database of possible filters and
// a file of current filters in the wheel.
//
// Revision 1.17  2000/07/10 15:03:48  cjm
// Added getThreadPriority* methods.
//
// Revision 1.16  2000/06/19 08:49:45  cjm
// Backup.
//
// Revision 1.15  2000/06/13 17:19:45  cjm
// Changes to property files..
//
// Revision 1.14  2000/05/23 13:48:52  cjm
// configId field and set/get methods added. Used to set CONFIGID FITS header keyword value.
//
// Revision 1.13  2000/03/08 13:32:54  cjm
// List of pause and resume times added.
//
// Revision 1.12  2000/02/16 17:18:24  cjm
// Added exposureCount and exposureNumber to the status.
//
// Revision 1.11  2000/02/15 15:42:02  cjm
// Added getFilterWheelName method.
//
// Revision 1.10  2000/02/04 18:00:35  cjm
// Added getFilterWheelPosition method.
//
// Revision 1.9  1999/08/04 09:03:49  cjm
// Spelling changes.
//
// Revision 1.8  1999/07/01 13:39:58  dev
// Log level improved
//
// Revision 1.7  1999/06/09 16:52:36  dev
// thread abort procedure improvements and error/log file implementation
//
// Revision 1.6  1999/06/07 16:54:59  dev
// properties file/more implementation
//
// Revision 1.5  1999/05/28 09:54:34  dev
// "Name
//
// Revision 1.4  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.3  1999/04/27 11:26:51  dev
// Backup
//
// Revision 1.2  1999/03/25 14:02:16  dev
// Backup
//
// Revision 1.1  1999/03/19 11:50:05  dev
// Backup
//
