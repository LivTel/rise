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
// Ccs.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/Ccs.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.ISS_INST.*;
import ngat.message.INST_DP.*;

/**
 * This class is the start point for the CCD Control System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class Ccs
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: Ccs.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * Logger channel id.
	 */
	public final static String LOGGER_CHANNEL_ID = new String("RISE");
	/**
	 * The minimum port number to listen for connections on.
	 */
	static final int MINIMUM_PORT_NUMBER = 1025;
	/**
	 * The maximum port number to send ISS commands on.
	 */
	static final int MAXIMUM_PORT_NUMBER = 65535;
	/**
	 * The server class that listens for connections.
	 */
	private CcsTCPServer server = null;
	/**
	 * The server class that listens for Telescope Image Transfer request connections.
	 */
	private TitServer titServer = null;
	/**
	 * The CCDLibrary class - used to interface with the SDSU CCD Controller.
	 */
	private CCDLibrary libccd = null;
	/**
	 * Ccs status object.
	 */
	private CcsStatus status = null;
	/**
	 * Ccs FITS Filename object to generate unique fits filenames according to ISS rules.
	 */
	private FitsFilename fitsFilename = null;
	/**
	 * The only instance of the ngat.fits.FitsHeader class - used to interface with some Java JNI routines 
	 * to write FITS headers.
	 */
	private FitsHeader libngatfits = null;
	/**
	 * The only instance of the ngat.fits.FitsHeaderDefaults class - used to specify some default
	 * values/comments/units/fits keyword ordering when writing FITS files.
	 */
	private FitsHeaderDefaults fitsHeaderDefaults = null;
	/**
	 * This hashtable holds the map between COMMAND sub-class names and their implementations, which
	 * are stored as the Hashtable data values as class objects of sub-classes of CommandImplementation.
	 * When the CCS gets a COMMAND from a client it can query this Hashtable to find it's implementation class.
	 */
	private Hashtable implementationList = null;
	/**
	 * The instance of the argument parser class, which sort out command line arguments
	 * passed to the Ccs.
	 * @see CcsArgumentParser
	 */
	CcsArgumentParser arguments = null;
	/**
	 * The port number to listen for connections from clients on.
	 */
	private int ccsPortNumber = 0;
	/**
	 * The ip address of the machine the ISS is running on, to send ISS commands to.
	 */
	private InetAddress issAddress = null;
	/**
	 * The port number to send iss commands to.
	 */
	private int issPortNumber = 0;
	/**
	 * The ip address of the machine the DP(RT) is running on, to send Data Pipeline (Real Time) commands to.
	 */
	private InetAddress dprtAddress = null;
	/**
	 * The port number to send DP(RT) commands to.
	 */
	private int dprtPortNumber = 0;
	/**
	 * The port number to listen for Telescope Image Transfer requests.
	 */
	private int titPortNumber = 0;
	/**
	 * The logging logger.
	 */
	protected Logger logLogger = null;
	/**
	 * The error logger.
	 */
	protected Logger errorLogger = null;
	/**
	 * The filter used to filter messages sent to the logging logger.
	 * @see #logLogger
	 */
	protected BitFieldLogFilter logFilter = null;
	/**
	 * The thread monitor window.
	 */
	private ThreadMonitorFrame threadMonitorFrame = null;

	/**
	 * This is the initialisation routine. This creates the status,
	 * fitsFilename, libccd, libngatfits and fitsHeaderDefaults objects. 
	 * The properties for the application are loaded into the status object.
	 * The error and log files are opened.<br>
	 * It calls the <a href="#initImplementationList">initImplementationList</a> method, which creates the
	 * <a href="#implementationList">implementationList</a>. This stores the mapping between messages sent
	 * to the server and classes to perform the tasks to complete the messages.
	 * See the reInit method, which is called when a REDATUM level reboot is performed,
	 * and re-initialises some of the above from new values in the configuration files.
	 * The reInit method must be kept up to date with respect to this method.
	 * This method assumes the CcsArgumentParser arguments instance has been created, and uses parameters
	 * from this in preference to parameters from the property files for network addresses/port numbers.
	 * @exception FileNotFoundException Thrown if the property file cannot be found.
	 * @exception IOException Thrown if the property file cannot be accessed and the properties cannot
	 * 	be loaded for some reason.
	 * @exception NumberFormatException Thrown if getting a port number from the configuration file that
	 * 	is not a valid integer.
	 * @exception Exception Thrown from FitsFilename.initialise, if the directory listing failed.
	 * @see #arguments
	 * @see #status
	 * @see #fitsFilename
	 * @see #libccd
	 * @see #libngatfits
	 * @see #fitsHeaderDefaults
	 * @see #initLoggers
	 * @see #implementationList
	 * @see #initImplementationList
	 * @see #reInit
	 * @see #titPortNumber
	 * @see #ccsPortNumber
	 * @see #issPortNumber
	 * @see #dprtPortNumber
	 * @see #issAddress
	 * @see #dprtAddress
	 */
	private void init() throws FileNotFoundException,IOException,
		CCDLibraryFormatException,NumberFormatException,CCDLibraryNativeException,Exception
	{
		String filename = null;
		int time;

	// create status object and load ccs properties into it
		status = new CcsStatus();
		status.setLogLevel(arguments.getLogLevel());
		try
		{
		// note these filenames can be null if the command line argument was not given,
		// but status.load uses the default in this case.
			status.load(arguments.getNetConfigurationFilename(),arguments.getConfigurationFilename(),
			arguments.getCurrentFilterConfigurationFilename(),arguments.getFilterConfigurationFilename());
		}
		catch(FileNotFoundException e)
		{
			error(this.getClass().getName()+":init:loading properties:",e);
			throw e;
		}
		catch(IOException e)
		{
			error(this.getClass().getName()+":init:loading properties:",e);
			throw e;
		}
	// Logging
		initLoggers();
	// create the fits filename object
		fitsFilename = new FitsFilename();
		fitsFilename.setInstrumentCode(status.getProperty("ccs.file.fits.instrument_code"));
		fitsFilename.setDirectory(status.getProperty("ccs.file.fits.path"));
		fitsFilename.initialise();
	// create CCDLibrary control object
		libccd = new CCDLibrary();
	// Create instance of the FITS header JNI library.
		libngatfits = new FitsHeader();
	// Create instance of FITS header defaults
		fitsHeaderDefaults = new FitsHeaderDefaults();
	// Load defaults from properties file.
		try
		{
		// note the filename can be null if the command line argument was not given,
		// but status.load uses the default in this case.
			if(arguments.getFitsHeaderDefaultsFilename() == null)
				fitsHeaderDefaults.load();
			else
				fitsHeaderDefaults.load(arguments.getFitsHeaderDefaultsFilename());
		}
		catch(FileNotFoundException e)
		{
			error(this.getClass().getName()+":init:loading default FITS header properties:",e);
			throw e;
		}
		catch(IOException e)
		{
			error(this.getClass().getName()+":init:loading default FITS header properties:",e);
			throw e;
		}
	// Create and initialise the implementationList
		initImplementationList();
	// initialise port numbers from properties file/ command line arguments
		try
		{
			ccsPortNumber = arguments.getCcsPortNumber();
			if(ccsPortNumber == 0)
				ccsPortNumber = status.getPropertyInteger("ccs.net.default_CCS_port_number");
			issPortNumber = arguments.getIssPortNumber();
			if(issPortNumber == 0)
				issPortNumber = status.getPropertyInteger("ccs.net.default_ISS_port_number");
			dprtPortNumber = arguments.getDpRtPortNumber();
			if(dprtPortNumber == 0)
				dprtPortNumber = status.getPropertyInteger("ccs.net.default_DP_port_number");
			titPortNumber = arguments.getTitPortNumber();
			if(titPortNumber == 0)
				titPortNumber = status.getPropertyInteger("ccs.net.default_TIT_port_number");
		}
		catch(NumberFormatException e)
		{
			error(this.getClass().getName()+":init:initialsing port number:",e);
			throw e;
		}
	// initialise address's from properties file
		try
		{
			issAddress = arguments.getIssAddress();
			if(issAddress == null)
				issAddress = InetAddress.getByName(status.getProperty("ccs.net.default_ISS_address"));
			dprtAddress = arguments.getDpRtAddress();
			if(dprtAddress == null)
				dprtAddress = InetAddress.getByName(status.getProperty("ccs.net.default_DP_address"));
		}
		catch(UnknownHostException e)
		{
			error(this.getClass().getName()+":illegal internet address:",e);
			throw e;
		}
	// initialise default connection response times from properties file
		try
		{
			time = status.getPropertyInteger("ccs.server_connection.default_acknowledge_time");
			CcsTCPServerConnectionThread.setDefaultAcknowledgeTime(time);
			time = status.getPropertyInteger("ccs.server_connection.min_acknowledge_time");
			CcsTCPServerConnectionThread.setMinAcknowledgeTime(time);
		}
		catch(NumberFormatException e)
		{
			error(this.getClass().getName()+":init:initialsing server connection thread times:",e);
			// don't throw the error - failing to get this property is not 'vital' to ccs.
		}		
	}

	/**
	 * Initialise log handlers. Called from init only, not re-configured on a REDATUM level reboot.
	 * @see #LOGGER_CHANNEL_ID
	 * @see #init
	 * @see #initLogHandlers
	 * @see #copyLogHandlers
	 * @see #errorLogger
	 * @see #logLogger
	 * @see #logFilter
	 */
	protected void initLoggers()
	{
	// errorLogger setup
		errorLogger = LogManager.getLogger("error");
		errorLogger.setChannelID(LOGGER_CHANNEL_ID+"-ERROR");
		initLogHandlers(errorLogger);
		errorLogger.setLogLevel(Logging.ALL);
	// ngat.net error loggers
		copyLogHandlers(errorLogger,LogManager.getLogger("ngat.net.TCPServer"),null);
		copyLogHandlers(errorLogger,LogManager.getLogger("ngat.net.TCPServerConnectionThread"),null);
		copyLogHandlers(errorLogger,LogManager.getLogger("ngat.net.TCPClientConnectionThreadMA"),null);
	// logLogger setup
		logLogger = LogManager.getLogger("log");
		logLogger.setChannelID(LOGGER_CHANNEL_ID);
		initLogHandlers(logLogger);
		logLogger.setLogLevel(Logging.ALL);
		logFilter = new BitFieldLogFilter(status.getLogLevel());
		logLogger.setFilter(logFilter);
	// CCDLibrary logging logger
		copyLogHandlers(logLogger,LogManager.getLogger("ngat.rise.ccd.CCDLibrary"),logFilter);
		copyLogHandlers(logLogger,LogManager.getLogger("ngat.net.TitServer"),logFilter);
	}

	/**
	 * Method to create and add all the handlers for the specified logger.
	 * These handlers are in the status properties:
	 * "ccs.log."+l.getName()+".handler."+index+".name" retrieves the relevant class name
	 * for each handler.
	 * @param l The logger.
	 * @see #initFileLogHandler
	 * @see #initConsoleLogHandler
	 * @see #initDatagramLogHandler
	 */
	protected void initLogHandlers(Logger l)
	{
		LogHandler handler = null;
		String handlerName = null;
		int index = 0;

		do
		{
			handlerName = status.getProperty("ccs.log."+l.getName()+".handler."+index+".name");
			if(handlerName != null)
			{
				try
				{
					handler = null;
					if(handlerName.equals("ngat.util.logging.FileLogHandler"))
					{
						handler = initFileLogHandler(l,index);
					}
					else if(handlerName.equals("ngat.util.logging.ConsoleLogHandler"))
					{
						handler = initConsoleLogHandler(l,index);
					}
					else if(handlerName.equals("ngat.util.logging.MulticastLogHandler"))
					{
						handler = initMulticastLogHandler(l,index);
					}
					else if(handlerName.equals("ngat.util.logging.MulticastLogRelay"))
					{
						handler = initMulticastLogRelay(l,index);
					}
					else if(handlerName.equals("ngat.util.logging.DatagramLogHandler"))
					{
						handler = initDatagramLogHandler(l,index);
					}
					else
					{
						error("initLogHandlers:Unknown handler:"+handlerName);
					}
					if(handler != null)
					{
						handler.setLogLevel(Logging.ALL);
						l.addHandler(handler);
					}
				}
				catch(Exception e)
				{
					error("initLogHandlers:Adding Handler failed:",e);
				}
				index++;
			}
		}
		while(handlerName != null);
	}

	/**
	 * Routine to add a FileLogHandler to the specified logger.
	 * This method expects either 3 or 6 constructor parameters to be in the status properties.
	 * If there are 6 parameters, we create a record limited file log handler with parameters:
	 * <ul>
	 * <li><b>param.0</b> is the filename.
	 * <li><b>param.1</b> is the formatter class name.
	 * <li><b>param.2</b> is the record limit in each file.
	 * <li><b>param.3</b> is the start index for file suffixes.
	 * <li><b>param.4</b> is the end index for file suffixes.
	 * <li><b>param.5</b> is a boolean saying whether to append to files.
	 * </ul>
	 * If there are 3 parameters, we create a time period file log handler with parameters:
	 * <ul>
	 * <li><b>param.0</b> is the filename.
	 * <li><b>param.1</b> is the formatter class name.
	 * <li><b>param.2</b> is the time period, either 'HOURLY_ROTATION','DAILY_ROTATION' or 'WEEKLY_ROTATION'.
	 * </ul>
	 * @param l The logger to add the handler to.
	 * @param index The index in the property file of the handler we are adding.
	 * @return A LogHandler of the relevant class is returned, if no exception occurs.
	 * @exception NumberFormatException Thrown if the numeric parameters in the properties
	 * 	file are not valid numbers.
	 * @exception FileNotFoundException Thrown if the specified filename is not valid in some way.
	 * @exception NullPointerException Thrown if a property value is null.
	 * @exception IllegalArgumentException Thrown if a property value is not legal.
	 * @see #status
	 * @see #initLogFormatter
	 * @see CcsStatus#getProperty
	 * @see CcsStatus#getPropertyInteger
	 * @see CcsStatus#getPropertyBoolean
	 * @see CcsStatus#propertyContainsKey
	 * @see CcsStatus#getPropertyLogHandlerTimePeriod
	 */
	protected LogHandler initFileLogHandler(Logger l,int index) throws NumberFormatException,
	          FileNotFoundException, NullPointerException, IllegalArgumentException
	{
		LogFormatter formatter = null;
		LogHandler handler = null;
		String fileName;
		int recordLimit,fileStart,fileLimit,timePeriod;
		boolean append;

		fileName = status.getProperty("ccs.log."+l.getName()+".handler."+index+".param.0");
		formatter = initLogFormatter("ccs.log."+l.getName()+".handler."+index+".param.1");
		// if we have more then 3 parameters, we are using a recordLimit FileLogHandler
		// rather than a time period log handler.
		if(status.propertyContainsKey("ccs.log."+l.getName()+".handler."+index+".param.3"))
		{
			recordLimit = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.2");
			fileStart = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.3");
			fileLimit = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.4");
			append = status.getPropertyBoolean("ccs.log."+l.getName()+".handler."+index+".param.5");
			handler = new FileLogHandler(fileName,formatter,recordLimit,fileStart,fileLimit,append);
		}
		else
		{
			// This is a time period log handler.
			timePeriod = status.getPropertyLogHandlerTimePeriod("ccs.log."+l.getName()+".handler."+
									    index+".param.2");
			handler = new FileLogHandler(fileName,formatter,timePeriod);
		}
		return handler;
	}

	/**
	 * Routine to add a MulticastLogHandler to the specified logger.
	 * The parameters to the constructor are stored in the status properties:
	 * <ul>
	 * <li>param.0 is the multicast group name i.e. "228.0.0.1".
	 * <li>param.1 is the port number i.e. 5000.
	 * <li>param.2 is the formatter class name.
	 * </ul>
	 * @param l The logger to add the handler to.
	 * @param index The index in the property file of the handler we are adding.
	 * @return A LogHandler of the relevant class is returned, if no exception occurs.
	 * @exception IOException Thrown if the multicast socket cannot be created for some reason.
	 */
	protected LogHandler initMulticastLogHandler(Logger l,int index) throws IOException
	{
		LogFormatter formatter = null;
		LogHandler handler = null;
		String groupName = null;
		int portNumber;

		groupName = status.getProperty("ccs.log."+l.getName()+".handler."+index+".param.0");
		portNumber = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.1");
		formatter = initLogFormatter("ccs.log."+l.getName()+".handler."+index+".param.2");
		handler = new MulticastLogHandler(groupName,portNumber,formatter);
		return handler;
	}

	/**
	 * Routine to add a MulticastLogRelay to the specified logger.
	 * The parameters to the constructor are stored in the status properties:
	 * <ul>
	 * <li>param.0 is the multicast group name i.e. "228.0.0.1".
	 * <li>param.1 is the port number i.e. 5000.
	 * </ul>
	 * @param l The logger to add the handler to.
	 * @param index The index in the property file of the handler we are adding.
	 * @return A LogHandler of the relevant class is returned, if no exception occurs.
	 * @exception IOException Thrown if the multicast socket cannot be created for some reason.
	 */
	protected LogHandler initMulticastLogRelay(Logger l,int index) throws IOException
	{
		LogHandler handler = null;
		String groupName = null;
		int portNumber;

		groupName = status.getProperty("ccs.log."+l.getName()+".handler."+index+".param.0");
		portNumber = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.1");
		handler = new MulticastLogRelay(groupName,portNumber);
		return handler;
	}

	/**
	 * Routine to add a DatagramLogHandler to the specified logger.
	 * The parameters to the constructor are stored in the status properties:
	 * <ul>
	 * <li>param.0 is the hostname i.e. "ltproxy".
	 * <li>param.1 is the port number i.e. 2371.
	 * </ul>
	 * @param l The logger to add the handler to.
	 * @param index The index in the property file of the handler we are adding.
	 * @return A LogHandler of the relevant class is returned, if no exception occurs.
	 * @exception IOException Thrown if the multicast socket cannot be created for some reason.
	 */
	protected LogHandler initDatagramLogHandler(Logger l,int index) throws IOException
	{
		LogHandler handler = null;
		String hostname = null;
		int portNumber;

		hostname = status.getProperty("ccs.log."+l.getName()+".handler."+index+".param.0");
		portNumber = status.getPropertyInteger("ccs.log."+l.getName()+".handler."+index+".param.1");
		handler = new DatagramLogHandler(hostname,portNumber);
		return handler;
	}

	/**
	 * Routine to add a ConsoleLogHandler to the specified logger.
	 * The parameters to the constructor are stored in the status properties:
	 * <ul>
	 * <li>param.0 is the formatter class name.
	 * </ul>
	 * @param l The logger to add the handler to.
	 * @param index The index in the property file of the handler we are adding.
	 * @return A LogHandler of class FileLogHandler is returned, if no exception occurs.
	 */
	protected LogHandler initConsoleLogHandler(Logger l,int index)
	{
		LogFormatter formatter = null;
		LogHandler handler = null;

		formatter = initLogFormatter("ccs.log."+l.getName()+".handler."+index+".param.0");
		handler = new ConsoleLogHandler(formatter);
		return handler;
	}

	/**
	 * Method to create an instance of a LogFormatter, given a property name
	 * to retrieve it's details from. If the property does not exist, or the class does not exist
	 * or an instance cannot be instansiated we try to return a ngat.util.logging.BogstanLogFormatter.
	 * @param propertyName A property name, present in the status's properties, 
	 * 	which has a value of a valid LogFormatter sub-class name. i.e.
	 * 	<pre>ccs.log.log.handler.0.param.1 =ngat.util.logging.BogstanLogFormatter</pre>
	 * @return An instance of LogFormatter is returned.
	 */
	protected LogFormatter initLogFormatter(String propertyName)
	{
		LogFormatter formatter = null;
		String formatterName = null;
		Class formatterClass = null;

		formatterName = status.getProperty(propertyName);
		if(formatterName == null)
		{
			error("initLogFormatter:NULL formatter for:"+propertyName);
			formatterName = "ngat.util.logging.BogstanLogFormatter";
		}
		try
		{
			formatterClass = Class.forName(formatterName);
		}
		catch(ClassNotFoundException e)
		{
			error("initLogFormatter:Unknown class formatter:"+formatterName+
				" from property "+propertyName);
			formatterClass = BogstanLogFormatter.class;
		}
		try
		{
			formatter = (LogFormatter)formatterClass.newInstance();
		}
		catch(Exception e)
		{
			error("initLogFormatter:Cannot create instance of formatter:"+formatterName+
				" from property "+propertyName);
			formatter = (LogFormatter)new BogstanLogFormatter();
		}
	// set better date format if formatter allows this.
	// Note we really need LogFormatter to generically allow us to do this
		if(formatter instanceof BogstanLogFormatter)
		{
			BogstanLogFormatter blf = (BogstanLogFormatter)formatter;

			blf.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z"));
		}
		if(formatter instanceof SimpleLogFormatter)
		{
			SimpleLogFormatter slf = (SimpleLogFormatter)formatter;

			slf.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z"));
		}
		return formatter;
	}

	/**
	 * Method to copy handlers from one logger to another. The outputLogger's channel ID is also
	 * copied from the input logger.
	 * @param inputLogger The logger to copy handlers from.
	 * @param outputLogger The logger to copy handlers to.
	 * @param lf The log filter to apply to the output logger. If this is null, the filter is not set.
	 */
	protected void copyLogHandlers(Logger inputLogger,Logger outputLogger,LogFilter lf)
	{
		LogHandler handlerList[] = null;
		LogHandler handler = null;

		handlerList = inputLogger.getHandlers();
		for(int i = 0; i < handlerList.length; i++)
		{
			handler = handlerList[i];
			outputLogger.addHandler(handler);
		}
		outputLogger.setLogLevel(inputLogger.getLogLevel());
		if(lf != null)
			outputLogger.setFilter(lf);
		// set all loggers to have the same channel ID
		outputLogger.setChannelID(inputLogger.getChannelID());
	}

	/**
	 * This is the re-initialisation routine. This is called on a REDATUM level reboot, and
	 * does some of the operations in the init routine. It re-loads the Ccs,filter and FITS configuration
	 * files, but NOT the network one. It resets the FitsFilename directory and instrument code. 
	 * It re-loads FITS header defaults.
	 * It re-initialises default connection response times from properties file.
	 * The init method must be kept up to date with respect to this method.
	 * @see #status
	 * @see #fitsFilename
	 * @see #fitsHeaderDefaults
	 * @see #init
	 * @exception FileNotFoundException Thrown if the property file cannot be found.
	 * @exception IOException Thrown if the property file cannot be accessed and the properties cannot
	 * 	be loaded for some reason.
	 * @exception Exception Thrown from FitsFilename.initialise, if the directory listing failed.
	 */
	public void reInit() throws FileNotFoundException,IOException,
		CCDLibraryFormatException,NumberFormatException,CCDLibraryNativeException,Exception
	{
		String filename = null;
		int time;

	// reload properties into the status object
		try
		{
			status.reload(arguments.getConfigurationFilename(),
			arguments.getCurrentFilterConfigurationFilename(),arguments.getFilterConfigurationFilename());
		}
		catch(FileNotFoundException e)
		{
			error(this.getClass().getName()+":reinit:loading properties:",e);
			throw e;
		}
		catch(IOException e)
		{
			error(this.getClass().getName()+":reinit:loading properties:",e);
			throw e;
		}
	// don't change errorLogger to files defined in loaded properties
	// don't change logLogger to files defined in loaded properties
	// set the fits filename instrument code/directory, and re-initialise runnum etc.
		fitsFilename.setInstrumentCode(status.getProperty("ccs.file.fits.instrument_code"));
		fitsFilename.setDirectory(status.getProperty("ccs.file.fits.path"));
		fitsFilename.initialise();
	// don't create CCDLibrary control object
	// don't create instance of FITS header defaults
	// reload  FITS header defaults from properties file.
		try
		{
			if(arguments.getFitsHeaderDefaultsFilename() == null)
				fitsHeaderDefaults.load();
			else
				fitsHeaderDefaults.load(arguments.getFitsHeaderDefaultsFilename());
		}
		catch(FileNotFoundException e)
		{
			error(this.getClass().getName()+":reinit:loading default FITS header properties:",e);
			throw e;
		}
		catch(IOException e)
		{
			error(this.getClass().getName()+":reinit:loading default FITS header properties:",e);
			throw e;
		}
	// don't create and initialise the implementationList
	// don't initialise port numbers from properties file
	// don't initialise address's from properties file
	// initialise default connection response times from properties file
		try
		{
			time = status.getPropertyInteger("ccs.server_connection.default_acknowledge_time");
			CcsTCPServerConnectionThread.setDefaultAcknowledgeTime(time);
			time = status.getPropertyInteger("ccs.server_connection.min_acknowledge_time");
			CcsTCPServerConnectionThread.setMinAcknowledgeTime(time);
		}
		catch(NumberFormatException e)
		{
			error(this.getClass().getName()+":reinit:initialsing server connection thread times:",e);
			// don't throw the error - failing to get this property is not 'vital' to ccs.
		}
	}

	/**
	 * This method creates the implementationList, and fills it with Class objects of sub-classes
	 * of CommandImplementation. The command implementation namess are retrieved from the Ccs property files,
	 * using keys of the form <b>ccs.command.implmentation.&lt;<i>N</i>&gt;</b>, where <i>N</i> is
	 * an integer is incremented. It puts the class object reference in the Hashtable with the 
	 * results of it's getImplementString static method
	 * as the key. If an implementation object class fails to be put in the hashtable for some reason
	 * it ignores it and continues for the next object in the list.
	 * @see #implementationList
	 * @see CommandImplementation#getImplementString
	 */
	private void initImplementationList()
	{
		Class cl = null;
		Class oldClass = null;
		Method method = null;
		Class methodClassParameterList[] = {};
		Object methodParameterList[] = {};
		String implementString = null;
		String className = null;
		int index;
		boolean done;

		implementationList = new Hashtable();
		index = 0;
		done = false;
		while(done == false)
		{
			className = status.getProperty("ccs.command.implmentation."+index);
			if(className != null)
			{
				try
				{
				// get Class object associated with class name
					cl = Class.forName(className);
				// get method object associated with getImplementString method of cl.
					method = cl.getDeclaredMethod("getImplementString",methodClassParameterList);
				// invoke getImplementString class method to get ngat.message class name it implements
					implementString = (String)method.invoke(null,methodParameterList);
				// put key and class into implementationList
					oldClass = (Class)implementationList.put(implementString,cl);
					if(oldClass != null)// the put returned another class with the same key.
					{
						error(this.getClass().getName()+":initImplementationList:Classes "+
							oldClass.getName()+" and "+cl.getName()+
							" both implement command:"+implementString);
					}
				}
				catch(ClassNotFoundException e)//Class.forName exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						className+":ClassNotFoundException:",e);
					// keep trying for next implementation in the list
				}
				catch(NoSuchMethodException e)//Class.getDeclaredMethod exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						className+":NoSuchMethodException:",e);
					// keep trying for next implementation in the list
				}
				catch(SecurityException e)//Class.getDeclaredMethod exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						className+":SecurityException:",e);
					// keep trying for next implementation in the list
				}
				catch(NullPointerException e)// Hashtable.put exception - null key.
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						cl.getName()+" implement string is null?:",e);
					// keep trying for next implementation in the list
				}
				catch(IllegalAccessException e)// Method.invoke exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						cl.getName()+":IllegalAccessException:",e);
					// keep trying for next implementation in the list
				}
				catch(IllegalArgumentException e)// Method.invoke exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						cl.getName()+":IllegalArgumentException:",e);
					// keep trying for next implementation in the list
				}
				catch(InvocationTargetException e)// Method.invoke exception
				{
					error(this.getClass().getName()+":initImplementationList:Class "+
						cl.getName()+":InvocationTargetException:",e);
					// keep trying for next implementation in the list
				}
			// try next class name in list
				index++;
			}
			else
				done = true;
		}// end while not done
	}

	/**
	 * Method to open a connection to the SDSU CCD Controller and send initialisation control sequences
	 * to it. This method assumes the <a href="#init">init</a> method has already been run to
	 * construct the <a href="#libccd">libccd</a> and <a href="#status">status</a> objects.
	 * It gets it's configuration from the CCS config file. This includes:
	 * <ul>
	 * <li>Device type number.
	 * <li>Print level.
	 * <li>Timing and utility board load information (from ROM/filename)
	 * <li>Target CCD temperature.
	 * <li>Gain and gain_speed.
	 * <li>Whether to idle clock the chip between exposures.
	 * </ul>
	 * The relevant CCDLibrary methods called to open the selected device, and initally configure it.
	 * If the filter wheels are enabled, the filter wheel is driven into a known position (0,0).</b>
	 * @exception CCDLibraryFormatException Thrown if the configuration properties cannot be determined.
	 * @exception CCDLibraryNativeException Thrown if the call to open or setup the device fails.
	 * @see #libccd
	 * @see #status
	 * @see CcsStatus#getProperty
	 * @see CcsStatus#getPropertyInteger
	 * @see CcsStatus#getPropertyBoolean
	 * @see CcsStatus#getPropertyDouble
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupLoadTypeFromString
	 * @see ngat.rise.ccd.CCDLibrary#CCDDSPGainFromString
	 * @see ngat.rise.ccd.CCDLibrary#CCDInitialise
	 * @see ngat.rise.ccd.CCDLibrary#CCDTextSetPrintLevel
	 * @see ngat.rise.ccd.CCDLibrary#CCDInterfaceOpen
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupStartup
	 * @see ngat.rise.ccd.CCDLibrary#CCDFilterWheelSetPositionCount
	 * @see ngat.rise.ccd.CCDLibrary#CCDFilterWheelSetDeBounceMs
	 */
	public void startupController() throws CCDLibraryFormatException, CCDLibraryNativeException
	{
		int deviceNumber,textPrintLevel;
		int pciLoadType,timingLoadType,timingApplicationNumber,utilityLoadType,utilityApplicationNumber,gain;
		int startExposureClearTime,startExposureOffsetTime,readoutRemainingTime;
		int filterWheelFilterCount,filterWheelDeBounceMs;
		boolean gainSpeed,idle,filterWheelEnable;
		double targetTemperature;
		String pciFilename,timingFilename,utilityFilename;

	// get the relevant configuration information from the CCS configuration file.
	// CCDLibraryFormatException is caught and re-thrown by this method.
	// Other exceptions (NumberFormatException) are not caught here, but by the calling method catch(Exception e)
		try
		{
			deviceNumber = libccd.CCDInterfaceDeviceFromString(status.getProperty("ccs.libccd.device"));
			textPrintLevel = libccd.CCDTextPrintLevelFromString(
				status.getProperty("ccs.libccd.device.text.print_level"));
			pciLoadType = libccd.CCDSetupLoadTypeFromString(status.
				getProperty("ccs.config.pci_load_type"));
			pciFilename = status.getProperty("ccs.config.pci_filename");
			timingLoadType = libccd.CCDSetupLoadTypeFromString(status.
				getProperty("ccs.config.timing_load_type"));
			timingApplicationNumber = status.getPropertyInteger("ccs.config.timing_application_number");
			timingFilename = status.getProperty("ccs.config.timing_filename");
			utilityLoadType = libccd.CCDSetupLoadTypeFromString(status.
				getProperty("ccs.config.utility_load_type"));
			utilityApplicationNumber = status.getPropertyInteger("ccs.config.utility_application_number");
			utilityFilename = status.getProperty("ccs.config.utility_filename");
			targetTemperature = status.getPropertyDouble("ccs.config.target_temperature");
			gain = libccd.CCDDSPGainFromString(status.getProperty("ccs.config.gain"));
			gainSpeed = status.getPropertyBoolean("ccs.config.gain_speed");
			idle = status.getPropertyBoolean("ccs.config.idle");
			// note we assume the two filter wheels have the same number of positions here
			filterWheelEnable = status.getPropertyBoolean("ccs.config.filter_wheel.enable");
			filterWheelFilterCount = status.getPropertyInteger("filterwheel.0.count");
			filterWheelDeBounceMs = status.getPropertyInteger("ccs.config.filter_wheel.de_bounce_ms");
			startExposureClearTime = status.getPropertyInteger("ccs.config.start_exposure_clear_time");
			startExposureOffsetTime = status.getPropertyInteger("ccs.config.start_exposure_offset_time");
			readoutRemainingTime = status.getPropertyInteger("ccs.config.readout_remaining_time");
		}
		catch(CCDLibraryFormatException e)
		{
			error(this.getClass().getName()+":startupController:",e);
			throw e;
		}
		libccd.CCDInitialise(deviceNumber);
		libccd.CCDTextSetPrintLevel(textPrintLevel);
		try
		{
			libccd.CCDInterfaceOpen();
			libccd.CCDSetupStartup(pciLoadType,pciFilename,
				timingLoadType,timingApplicationNumber,timingFilename,
				utilityLoadType,utilityApplicationNumber,utilityFilename,
				targetTemperature,gain,gainSpeed,idle);
			libccd.CCDFilterWheelSetPositionCount(filterWheelFilterCount);
			libccd.CCDFilterWheelSetDeBounceMs(filterWheelDeBounceMs);
			if(filterWheelEnable)
			{
				libccd.CCDFilterWheelReset(0);
				libccd.CCDFilterWheelReset(1);
			}
			else
			{
				log(CcsConstants.CCS_LOG_LEVEL_ALL,this.getClass().getName()+
					":startupController:Filter wheels not enabled:Filter wheels NOT reset.");
			}
		}
		catch (CCDLibraryNativeException e)
		{
			error(this.getClass().getName()+":startupController:",e);
			throw e;
		}
		libccd.CCDExposureSetStartExposureClearTime(startExposureClearTime);
		libccd.CCDExposureSetStartExposureOffsetTime(startExposureOffsetTime);
		libccd.CCDExposureSetReadoutRemainingTime(readoutRemainingTime);
	}

	/**
	 * Method to shut down the connection to the SDSU CCD Controller.
	 * This calls the CCDLibrary CCDSetupShutdown method followed by the CCDInterfaceClose method.
	 * @exception CCDLibraryNativeException Thrown if the device failed to shut down.
	 * @see #libccd
	 * @see ngat.rise.ccd.CCDLibrary#CCDSetupShutdown
	 * @see ngat.rise.ccd.CCDLibrary#CCDInterfaceClose
	 */
	public void shutdownController() throws CCDLibraryNativeException
	{
		libccd.CCDSetupShutdown();
		libccd.CCDInterfaceClose();
	}

	/**
	 * This is the run routine. It starts a new server to handle incoming requests, and waits for the
	 * server to terminate. A thread monitor is also started if it was requested from the command line.
	 * @see #server
	 * @see #ccsPortNumber
	 * @see #titServer
	 * @see #titPortNumber
	 * @see #arguments
	 */
	private void run()
	{
		int threadMonitorUpdateTime;
		Date nowDate = null;

		server = new CcsTCPServer("CCS",ccsPortNumber);
		server.setCcs(this);
		server.setPriority(status.getThreadPriorityServer());
		titServer = new TitServer("TitServer on port "+titPortNumber,titPortNumber);
		titServer.setPriority(status.getThreadPriorityTIT());
		nowDate = new Date();
		log(CcsConstants.CCS_LOG_LEVEL_ALL,
			this.getClass().getName()+":run:server started at:"+nowDate.toString());
		log(CcsConstants.CCS_LOG_LEVEL_ALL,
			this.getClass().getName()+":run:server started on port:"+ccsPortNumber);
		error(this.getClass().getName()+":run:server started at:"+nowDate.toString());
		error(this.getClass().getName()+":run:server started on port:"+ccsPortNumber);
		if(arguments.getThreadMonitor())
		{
			threadMonitorFrame = new ThreadMonitorFrame(this.getClass().getName());
			threadMonitorFrame.pack();
			threadMonitorFrame.setVisible(true);
			try
			{
				threadMonitorUpdateTime = status.getPropertyInteger("ccs.thread_monitor.update_time");
			}
			catch(NumberFormatException e)
			{
				error(this.getClass().getName()+":run:getting thread monitor update time:",e);
				threadMonitorUpdateTime  = 1000;
			}
			threadMonitorFrame.getThreadMonitor().setUpdateTime(threadMonitorUpdateTime);
		}
		server.start();
		titServer.start();
		try
		{
			server.join();
		}
		catch(InterruptedException e)
		{
			error(this.getClass().getName()+":run:",e);
		}
	}

	/**
	 * Routine to be called at the end of execution of Ccs to close down communications.
	 * Currently closes CCDLibrary, CcsTCPServer and TitServer.
	 * @see ngat.rise.ccd.CCDLibrary#CCDInterfaceClose
	 * @see CcsTCPServer#close
	 * @see #server
	 * @see TitServer#close
	 * @see #titServer
	 * @see #shutdownController
	 */
	public void close()
	{
		try
		{
			libccd.CCDInterfaceClose();
		}
		catch(CCDLibraryNativeException e)
		{
			error(this.getClass().getName()+":close:",e);
		}
		server.close();
		titServer.close();
	}

	/**
	 * Get Socket Server instance.
	 * @return The server instance.
	 */
	public CcsTCPServer getServer()
	{
		return server;
	}

	/**
	 * Get Fits filename generation object instance.
	 * @return The Ccs FitsFilename fitsFilename instance.
	 */
	public FitsFilename getFitsFilename()
	{
		return fitsFilename;
	}

	/**
	 * Get libccd instance.
	 * @return The libccd instance.
	 */
	public CCDLibrary getLibccd()
	{
		return libccd;
	}

	/**
	 * Get libngatfits instance. This is the only instance of the ngat.fits.FitsHeader class in this application.
	 * It is used to write FITS header cards to disk, ready to append the relevant data to it.
	 * @return The libngatfits instance.
	 * @see #libngatfits
	 */
	public FitsHeader getFitsHeader()
	{
		return libngatfits;
	}

	/**
	 * Get FitsHeaderDefaults instance. This is the only instance of the ngat.fits.FitsHeaderDefaults 
	 * class in this application.
	 * It is used to get defaults values for field in the FITS headers the application writes to disk.
	 * @return The fitsHeaderDefaults instance.
	 * @see #fitsHeaderDefaults
	 */
	public FitsHeaderDefaults getFitsHeaderDefaults()
	{
		return fitsHeaderDefaults;
	}

	/**
	 * Get status instance.
	 * @return The status instance.
	 */
	public CcsStatus getStatus()
	{
		return status;
	}

	/**
	 * This routine returns an instance of the sub-class of CommandImplementation that
	 * implements the command with class name commandClassName. If an implementation is
	 * not found or an instance cannot be created, an instance of UnknownCommandImplementation is returned instead.
	 * The instance is constructed 
	 * using a null argument constructor, from the Class object stored in the implementationList.
	 * @param commandClassName The class-name of a COMMAND sub-class.
	 * @return A new instance of a sub-class of CommandImplementation that implements the 
	 * 	command, or an instance of UnknownCommandImplementation.
	 */
	public JMSCommandImplementation getImplementation(String commandClassName)
	{
		JMSCommandImplementation unknownCommandImplementation = new UnknownCommandImplementation();
		JMSCommandImplementation object = null;
		Class cl = null;

		cl = (Class)implementationList.get(commandClassName);
		if(cl != null)
		{
			try
			{
				object = (JMSCommandImplementation)cl.newInstance();
			}
			catch(InstantiationException e)//Class.newInstance exception
			{
				error(this.getClass().getName()+":getImplementation:Class "+
					cl.getName()+":InstantiationException:",e);
				object = null;
			}
			catch(IllegalAccessException e)//Class.newInstance exception
			{
				error(this.getClass().getName()+":getImplementation:Class "+
					cl.getName()+":IllegalAccessException:",e);
				object = null;
			}
		}// end if found class
		if(object != null)
			return object;
		else
			return unknownCommandImplementation;
	}

	/**
	 * Method to set the level of logging filtered by the log level filter.
	 * @param level An integer, used as a bit-field. Each bit set will allow
	 * any messages with that level bit set to be logged. e.g. 0 logs no messages,
	 * 127 logs any messages with one of the first 8 bits set.
	 */
	public void setLogLevelFilter(int level)
	{
		logFilter.setLevel(level);
	}

	/**
	 * Routine to send a command from the instrument (this application/CCS) to the ISS. The routine
	 * waits until the command's done message has been returned from the ISS and returns this.
	 * If the commandThread is aborted this also stops waiting for the done message to be returned.
	 * @param command The command to send to the ISS.
	 * @param commandThread The thread the passed in command (and this method) is running on.
	 * @return The done message returned from te ISS, or an error message created by this routine
	 * 	if the done was null.
	 * @see #issAddress
	 * @see #issPortNumber
	 * @see #sendISSCommand(INST_TO_ISS,CcsTCPServerConnectionThread,boolean)
	 * @see CcsTCPClientConnectionThread
	 * @see CcsTCPServerConnectionThread#getAbortProcessCommand
	 */
	public INST_TO_ISS_DONE sendISSCommand(INST_TO_ISS command,CcsTCPServerConnectionThread commandThread)
	{
		return sendISSCommand(command,commandThread,true);
	}

	/**
	 * Routine to send a command from the instrument (this application/CCS) to the ISS. The routine
	 * waits until the command's done message has been returned from the ISS and returns this.
	 * If checkAbort is set and the commandThread is aborted this also stops waiting for the 
	 * done message to be returned.
	 * @param command The command to send to the ISS.
	 * @param commandThread The thread the passed in command (and this method) is running on.
	 * @param checkAbort A boolean, set to true if we want to check for commandThread aborting.
	 * 	This should be set to false when the command is being sent to the ISS in response
	 * 	to an abort occuring.
	 * @return The done message returned from te ISS, or an error message created by this routine
	 * 	if the done was null.
	 * @see #issAddress
	 * @see #issPortNumber
	 * @see CcsTCPClientConnectionThread
	 * @see CcsTCPServerConnectionThread#getAbortProcessCommand
	 */
	public INST_TO_ISS_DONE sendISSCommand(INST_TO_ISS command,CcsTCPServerConnectionThread commandThread,
		boolean checkAbort)
	{
		CcsTCPClientConnectionThread thread = null;
		INST_TO_ISS_DONE done = null;
		boolean finished = false;

		log(CcsConstants.CCS_LOG_LEVEL_COMMANDS,
			this.getClass().getName()+":sendISSCommand:"+command.getClass().getName());
		thread = new CcsTCPClientConnectionThread(issAddress,issPortNumber,command,commandThread);
		thread.setCcs(this);
		thread.start();
		finished = false;
		while(finished == false)
		{
			try
			{
				thread.join(100);// wait 100 millis for the thread to finish
			}
			catch(InterruptedException e)
			{
				error("run:join interrupted:",e);
			}
		// If the thread has finished so has this loop
			finished = (thread.isAlive() == false);
		// check if the thread has been aborted, if checkAbort has been set.
			if(checkAbort)
			{
			// If the commandThread has been aborted, stop processing this thread
				if(commandThread.getAbortProcessCommand())
					finished = true;
			}
		}
		done = (INST_TO_ISS_DONE)thread.getDone();
		if(done == null)
		{
			// one reason the done is null is if we escaped from the loop
			// because the Ccs server thread was aborted.
			if(commandThread.getAbortProcessCommand())
			{
				done = new INST_TO_ISS_DONE(command.getId());
				error(this.getClass().getName()+":sendISSCommand:"+
					command.getClass().getName()+":Server thread Aborted");
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1);
				done.setErrorString("sendISSCommand:Server thread Aborted:"+
					command.getClass().getName());
				done.setSuccessful(false);		
			}
			else // a communication failure occured
			{
				done = new INST_TO_ISS_DONE(command.getId());
				error(this.getClass().getName()+":sendISSCommand:"+
					command.getClass().getName()+":Getting Done failed");
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2);
				done.setErrorString("sendISSCommand:Getting Done failed:"+
					command.getClass().getName());
				done.setSuccessful(false);
			}
		}
		log(CcsConstants.CCS_LOG_LEVEL_REPLIES,
			"Done:"+done.getClass().getName()+":successful:"+done.getSuccessful()+
			":error number:"+done.getErrorNum()+":error string:"+done.getErrorString());
		return done;
	}

	/**
	 * Routine to send a command from the instrument (this application/CCS) to the DP(RT). The routine
	 * waits until the command's done message has been returned from the DP(RT) and returns this.
	 * If the commandThread is aborted this also stops waiting for the done message to be returned.
	 * @param command The command to send to the DP(RT).
	 * @param commandThread The thread the passed in command (and this method) is running on.
	 * @return The done message returned from te DP(RT), or an error message created by this routine
	 * 	if the done was null.
	 * @see #dprtAddress
	 * @see #dprtPortNumber
	 * @see CcsTCPClientConnectionThread
	 * @see CcsTCPServerConnectionThread#getAbortProcessCommand
	 */
	public INST_TO_DP_DONE sendDpRtCommand(INST_TO_DP command,CcsTCPServerConnectionThread commandThread)
	{
		CcsTCPClientConnectionThread thread = null;
		INST_TO_DP_DONE done = null;
		boolean finished = false;

		log(CcsConstants.CCS_LOG_LEVEL_COMMANDS,
			this.getClass().getName()+":sendDpRtCommand:"+command.getClass().getName());
		thread = new CcsTCPClientConnectionThread(dprtAddress,dprtPortNumber,command,commandThread);
		thread.setCcs(this);
		thread.start();
		finished = false;
		while(finished == false)
		{
			try
			{
				thread.join(100);// wait 100 millis for the thread to finish
			}
			catch(InterruptedException e)
			{
				error("run:join interrupted:",e);
			}
		// If the thread has finished so has this loop
			finished = (thread.isAlive() == false);
		// If the commandThread has been aborted, stop processing this thread
			if(commandThread.getAbortProcessCommand())
				finished = true;
		}
		done = (INST_TO_DP_DONE)thread.getDone();
		if(done == null)
		{
			// one reason the done is null is if we escaped from the loop
			// because the Ccs server thread was aborted.
			if(commandThread.getAbortProcessCommand())
			{
				done = new INST_TO_DP_DONE(command.getId());
				error(this.getClass().getName()+":sendDpRtCommand:"+
					command.getClass().getName()+":Server thread Aborted");
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+3);
				done.setErrorString("sendDpRtCommand:Server thread Aborted:"+
					command.getClass().getName());
				done.setSuccessful(false);
			}
			else // a communication failure occured
			{
				done = new INST_TO_DP_DONE(command.getId());
				error(this.getClass().getName()+":sendDpRtCommand:"+
					command.getClass().getName()+":Getting Done failed");
				done.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+4);
				done.setErrorString("sendDpRtCommand:Getting Done failed:"+
					command.getClass().getName());
				done.setSuccessful(false);
			}
		}
		log(CcsConstants.CCS_LOG_LEVEL_REPLIES,
			"Done:"+done.getClass().getName()+":successful:"+done.getSuccessful()+
			":error number:"+done.getErrorNum()+":error string:"+done.getErrorString());
		return done;
	}

	/**
	 * Routine to write the string to the relevant logger. If the relevant logger has not been
	 * created yet the error gets written to System.out.
	 * @param level The level of logging this message belongs to.
	 * @param s The string to write.
	 * @see #logLogger
	 */
	public void log(int level,String s)
	{
		if(logLogger != null)
			logLogger.log(level,s);
		else
		{
			if((status.getLogLevel()&level) > 0)
				System.out.println(s);
		}
	}

	/**
	 * Routine to write the string to the relevant logger. If the relevant logger has not been
	 * created yet the error gets written to System.err.
	 * @param s The string to write.
	 * @see #errorLogger
	 */
	public void error(String s)
	{
		if(errorLogger != null)
			errorLogger.log(CcsConstants.CCS_LOG_LEVEL_ERROR,s);
		else
			System.err.println(s);
	}

	/**
	 * Routine to write the string to the relevant logger. If the relevant logger has not been
	 * created yet the error gets written to System.err.
	 * @param s The string to write.
	 * @param e An exception that caused the error to occur.
	 * @see #errorLogger
	 */
	public void error(String s,Exception e)
	{
		if(errorLogger != null)
		{
			errorLogger.log(CcsConstants.CCS_LOG_LEVEL_ERROR,s,e);
			errorLogger.dumpStack(CcsConstants.CCS_LOG_LEVEL_ERROR,e);
		}
		else
			System.err.println(s+e);
	}

	/**
	 * Routine that checks whether the arguments loaded from the property files and set using the arguments
	 * are sensible.
	 * @see #ccsPortNumber
	 * @see #issAddress
	 * @see #issPortNumber
	 * @see #dprtAddress
	 * @see #dprtPortNumber
	 * @see #titPortNumber
	 * @exception Exception Thrown when an argument is not acceptable.
	 */
	private void checkArgs() throws Exception
	{
		if(issAddress == null)
		{
			arguments.help();
			throw new Exception("No ISS Address Specified.");
		}
		if(dprtAddress == null)
		{
			arguments.help();
			throw new Exception("No DP(RT) Address Specified.");
		}
		if((ccsPortNumber < MINIMUM_PORT_NUMBER)||(ccsPortNumber > MAXIMUM_PORT_NUMBER))
		{
			throw new Exception("Server Port Number '"+ccsPortNumber+"' out of range.");
		}
		if((issPortNumber < MINIMUM_PORT_NUMBER)||(issPortNumber > MAXIMUM_PORT_NUMBER))
		{
			throw new Exception("ISS Port Number '"+issPortNumber+"' out of range.");
		}
		if((dprtPortNumber < MINIMUM_PORT_NUMBER)||(dprtPortNumber > MAXIMUM_PORT_NUMBER))
		{
			throw new Exception("DP(RT) Port Number '"+dprtPortNumber+"' out of range.");
		}
		if((titPortNumber < MINIMUM_PORT_NUMBER)||(titPortNumber > MAXIMUM_PORT_NUMBER))
		{
			throw new Exception("TIT Server Port Number '"+titPortNumber+"' out of range.");
		}
	}

	/**
	 * The main routine, called when Ccs is executed. This createsa new instance of the Ccs class.
	 * It calls the following methods:
	 * <ul>
	 * <li>Creates an argument instance using CcsArgumentParser.parse.
	 * <li>init.
	 * <li>checkArgs.
	 * <li>startupController.
	 * <li>run.
	 * </ul>
	 * @see #init
	 * @see CcsArgumentParser#parse
	 * @see #checkArgs
	 * @see #startupController
	 * @see #run
	 */
	public static void main(String[] args)
	{
		Ccs ccs = new Ccs();
		ccs.arguments = CcsArgumentParser.parse(args);
		try
		{
			ccs.init();
		}
		catch(Exception e)
		{
 			ccs.error("main:init failed:",e);
			System.exit(1);
		}
		try
		{
			ccs.checkArgs();
		}
		catch(Exception e)
		{
 			ccs.error("main:checkArgs failed:",e);
			System.exit(1);
		}
		try
		{
			ccs.startupController();
		}
		catch(Exception e)
		{
 			ccs.error("main:startupController failed:",e);
			System.exit(1);
		}
		ccs.run();
	// We get here if the server thread has terminated. If it has been quit
	// this is a successfull termination, otherwise an error has occured.
	// Note the program can also be terminated from within a REBOOT call.
		if(ccs.server.getQuit() == false)
			System.exit(1);
	}
}

// $Log: not supported by cvs2svn $
// Revision 1.49  2006/05/16 14:25:42  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.48  2005/07/26 15:54:08  cjm
// Documentation fix.
//
// Revision 1.47  2004/02/20 14:55:51  cjm
// Changed initFileLogHandler so that it can create time period based file log handlers.
//
// Revision 1.46  2003/01/28 16:26:08  cjm
// New filter wheel code.
//
// Revision 1.45  2002/12/16 17:00:27  cjm
// Changed libccd CCDDSP... calls to CCDExposure calls.
// Check status property to disablr filter wheel calls.
//
// Revision 1.44  2002/09/12 16:25:58  cjm
// copLogHandlers now copies log filters.
//
// Revision 1.43  2001/09/24 19:33:21  cjm
// Implementation class names now loaded from properties.
//
// Revision 1.42  2001/07/12 17:50:48  cjm
// autoguiderStop changes.
//
// Revision 1.41  2001/07/03 16:34:35  cjm
// Added TitServer instance code for Telescope Image Transfer server.
// Changed fitsFilename instance from CcsFilename to ngat.fits.FitsFilename.
//
// Revision 1.40  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 1.39  2001/03/09 16:23:18  cjm
// Changed quit code.
//
// Revision 1.38  2001/03/01 15:15:27  cjm
// Added to startupController:
// CCDFilterWheelSetPositionCount to setup positions per wheel.
// CCDFilterWheelSetMsPerStep and CCDFilterWheelSetDeBounceStepCount to configure filter wheel.
//
// Revision 1.37  2000/12/19 17:52:12  cjm
// New ngat.rise.ccd.CCDLibrary filter wheel method calls.
//
// Revision 1.36  2000/11/13 17:26:30  cjm
// Stopped error strings in exceptions being printed twice: printStackTrace prints error message.
//
// Revision 1.35  2000/11/08 16:59:01  cjm
// Added exception to init, reInit, for fitsFilename.initialise() Exception
// when the directory is failed to be read.
//
// Revision 1.34  2000/08/09 13:54:13  cjm
// CcsStatus load and reload changes, as there are now two filter wheel configuration filenames.
//
// Revision 1.33  2000/08/01 15:51:44  cjm
// Changed main quit, so it returns the correct exit code to the autobooter.
//
// Revision 1.32  2000/07/10 15:05:10  cjm
// Changed setPriority calls to use priorities from the configuration files (CcsStatus).
//
// Revision 1.31  2000/07/04 08:55:10  cjm
// Changed close implementation, so that it doesn't try to power off,
// but just closes the interface.
//
// Revision 1.30  2000/06/30 17:40:07  cjm
// Changed for separate argument parser.
// Argument parsing now done before init, so setting config filenames from command line works.
//
// Revision 1.29  2000/06/19 08:49:45  cjm
// Backup.
//
// Revision 1.28  2000/06/14 09:21:40  cjm
// Added extra debugging when general exceptions thrown:
// stack trace is printed to error file.
//
// Revision 1.27  2000/06/13 17:19:45  cjm
// Changes to property files..
//
// Revision 1.26  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 1.25  2000/05/17 09:08:10  cjm
// Swapped CcsFitsHeader for ngat.fits.FitsHeader.
//
// Revision 1.24  2000/02/17 16:48:43  cjm
// Added call to initialise the fitsFilename object with the last multRun performed this night.
//
// Revision 1.23  2000/02/07 18:18:15  cjm
// Sorted out how program terminates w.r.t. REBOOT commands.
//
// Revision 1.22  2000/02/07 11:07:00  cjm
// Reminder comments for filter wheel code in startup procedure.
//
// Revision 1.21  2000/02/04 16:28:39  cjm
// Changed startupController and shutdownController.
//
// Revision 1.20  1999/12/07 14:53:12  cjm
// Changed sendISSCommand and sendDpRtCommand so that they bail out if the
// Client decides to abort and they send ISS and DpRt acknowledge messages back to
// the client.
//
// Revision 1.19  1999/11/02 18:23:59  cjm
// Changed how Implementation instances are created.
// implementationList Hashtable how stores Class objects.
// getImplementation now creates new instances of these classes.
// This allows instances to store per-thread data without
// danger of corruption from two threads implementing the same command
// simultaneously.
//
// Revision 1.18  1999/11/02 11:15:42  cjm
// Fixed bugs in documentation...
//
// Revision 1.17  1999/11/02 11:09:56  cjm
// Added some more Javadoc comments for the init and initImplementation methods.
//
// Revision 1.16  1999/11/01 10:48:55  cjm
// Added implementation hashtable and methods to get an implmentation
// from a message class name.
//
// Revision 1.15  1999/09/20 14:37:56  cjm
// Changed due to libccd native routines throwing CCDLibraryNativeException when errors occur.
//
// Revision 1.14  1999/09/15 12:37:10  cjm
// Imported from ngat.ccd package now that CDLibrary is in this package.
//
// Revision 1.13  1999/07/05 09:00:30  dev
// Setup an instance of the CcsFitsHeader class at init, and supplied
// a
// retrieve method to get the instance.
//
// Revision 1.12  1999/07/01 13:39:58  dev
// Log level improved
//
// Revision 1.11  1999/07/01 13:15:10  dev
// error log now has server start message in it
//
// Revision 1.10  1999/06/24 12:40:20  dev
// "Backup"
//
// Revision 1.9  1999/06/10 10:23:54  dev
// Print Stream to PrintWriter error/log stream improvement
// CcsFilename directory improvement
//
// Revision 1.8  1999/06/09 16:52:36  dev
// thread abort procedure improvements and error/log file implementation
//
// Revision 1.7  1999/06/08 16:50:53  dev
// thread priority changes
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
// Revision 1.3  1999/03/25 14:02:16  dev
// Backup
//
// Revision 1.2  1999/03/19 11:50:05  dev
// Backup
//
// Revision 1.1  1999/03/16 17:03:32  dev
// Backup
//
