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
// SendConfigCommand.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/SendConfigCommand.java,v 1.1 2009-10-15 10:19:32 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.phase2.*;
import ngat.util.*;

/**
 * This class send a CCD configuration to the CCD Control System. The configuration can be randonly generated 
 * (using a filter wheel database to get sensible names) or specified.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SendConfigCommand
{
	/**
	 * The default port number to send ISS commands to.
	 */
	static final int DEFAULT_CCS_PORT_NUMBER = 6783;
	/**
	 * The default port number for the server, to get commands from the CCS from.
	 */
	static final int DEFAULT_SERVER_PORT_NUMBER = 7383;
	/**
	 * The filename of a current filter wheel property file.
	 */
	private String filename = null;
	/**
	 * A property list of filter wheel properties.
	 */
	private NGATProperties filterWheelProperties = null;
	/**
	 * The ip address to send the messages read from file to, this should be the machine the CCS is on.
	 */
	private InetAddress address = null;
	/**
	 * The port number to send commands from the file to the CCS.
	 */
	private int ccsPortNumber = DEFAULT_CCS_PORT_NUMBER;
	/**
	 * The port number for the server, to recieve commands from the CCS.
	 */
	private int serverPortNumber = DEFAULT_SERVER_PORT_NUMBER;
	/**
	 * The server class that listens for connections from the CCS.
	 */
	private SicfTCPServer server = null;
	/**
	 * The stream to write error messages to - defaults to System.err.
	 */
	private PrintStream errorStream = System.err;
	/**
	 * Lower filter wheel string. Defaults to null, which will cause the Ccs
	 * to return an error.
	 */
	private String lowerFilterString = null;
	/**
	 * Upper filter wheel string. Defaults to null, which will cause the Ccs
	 * to return an error.
	 */
	private String upperFilterString = null;
	/**
	 * X Binning of configuration. Defaults to 1.
	 */
	private int xBin = 1;
	/**
	 * Y Binning of configuration. Defaults to 1.
	 */
	private int yBin = 1;

	/**
	 * This is the initialisation routine. This starts the server thread.
	 */
	private void init()
	{
		server = new SicfTCPServer(this.getClass().getName(),serverPortNumber);
		server.setController(this);
		server.start();
	}

	/**
	 * Routine to load the current filter wheel properties from filename
	 * into filterWheelProperties.
	 * @exception FileNotFoundException Thrown if the load failed.
	 * @exception IOException Thrown if the load failed.
	 * @see #filename
	 * @see #filterWheelProperties
	 */
	private void loadCurrentFilterProperties() throws FileNotFoundException, IOException
	{
		filterWheelProperties = new NGATProperties();
		filterWheelProperties.load(filename);
	}

	/**
	 * Routine to select and return a random filter from the specified wheel,
	 * using the loaded filter property database.
	 * @param wheel Which wheel, either zero or one.
	 * @return A string, a filter type, that is present in the specified wheel according to
	 * 	the loaded properties.
	 * @exception NGATPropertyException Thrown if a property retrieve fails.
	 * @see #filterWheelProperties
	 */
	private String selectRandomFilter(int wheel) throws NGATPropertyException
	{
		int positionCount,position;
		Random random = null;
		String filterType;

		positionCount = filterWheelProperties.getInt("filterwheel."+wheel+".count");
		random = new Random();
		position = random.nextInt(positionCount);
		filterType = (String)(filterWheelProperties.get("filterwheel."+wheel+"."+position+".type"));
		return filterType;
	}

	/**
	 * This routine creates a CONFIG command. This object
	 * has a CCDConfig phase2 object with it, this is created and it's fields initialised.
	 * @return An instance of CONFIG.
	 * @see #lowerFilterString
	 * @see #upperFilterString
	 * @see #xBin
	 * @see #yBin
	 */
	private CONFIG createConfig()
	{
		String string = null;
		CONFIG configCommand = null;
		CCDConfig ccdConfig = null;
		CCDDetector detector = null;
		Window windowArray[];
		int xs,ys,width = 100,height = 100;

		configCommand = new CONFIG("Object Id");
		ccdConfig = new CCDConfig("Object Id");
	// detector for config
		detector = new CCDDetector();
	// windows - 4 windows, each 100x100, 200x200 apart.
		windowArray = new Window[detector.getMaxWindowCount()];
		xs = 0;
		ys = 0;
		width = 100;
		height = 100;
		for(int i = 0; i < detector.getMaxWindowCount(); i++)
		{
			Window w = null;

			w = new Window();
			w.setXs(xs);
			w.setYs(ys);
			w.setXe(xs+width);
			w.setYe(ys+height);
			xs += width*2;
			ys += height*2;
			windowArray[i] = w;
		}
		detector.setWindows(windowArray);
		detector.setWindowFlags(0);
		ccdConfig.setDetector(0,detector);
		ccdConfig.setLowerFilterWheel(lowerFilterString);
		ccdConfig.setUpperFilterWheel(upperFilterString);
		detector.setXBin(xBin);
		detector.setYBin(yBin);
	// InstrumentConfig fields.
		configCommand.setConfig(ccdConfig);
		return configCommand;
	}

	/**
	 * This is the run routine. It creates a CONFIG object and sends it to the using a 
	 * SicfTCPClientConnectionThread, and awaiting the thread termination to signify message
	 * completion. 
	 * @return The routine returns true if the command succeeded, false if it failed.
	 * @exception Exception Thrown if an exception occurs.
	 * @see #loadCurrentFilterProperties
	 * @see #selectRandomFilter
	 * @see #createConfig
	 * @see SicfTCPClientConnectionThread
	 * @see #getThreadResult
	 */
	private boolean run() throws Exception
	{
		ISS_TO_INST issCommand = null;
		SicfTCPClientConnectionThread thread = null;
		boolean retval;

		if(filename != null)
		{
			loadCurrentFilterProperties();
			if(lowerFilterString == null)
				lowerFilterString = selectRandomFilter(0);
			if(upperFilterString == null)
				upperFilterString = selectRandomFilter(1);
		}
		else
		{
			if(lowerFilterString == null)
				System.err.println("Program should fail:No lower filter specified.");
			if(upperFilterString == null)
				System.err.println("Program should fail:No upper filter specified.");
		}
		issCommand = (ISS_TO_INST)(createConfig());
		if(issCommand instanceof CONFIG)
		{
			CONFIG configCommand = (CONFIG)issCommand;
			CCDConfig ccdConfig = (CCDConfig)(configCommand.getConfig());
			System.err.println("CONFIG:"+
				ccdConfig.getLowerFilterWheel()+":"+
				ccdConfig.getUpperFilterWheel()+":"+
				ccdConfig.getDetector(0).getXBin()+":"+
				ccdConfig.getDetector(0).getYBin()+".");
		}
		thread = new SicfTCPClientConnectionThread(address,ccsPortNumber,issCommand);
		thread.start();
		while(thread.isAlive())
		{
			try
			{
				thread.join();
			}
			catch(InterruptedException e)
			{
				System.err.println("run:join interrupted:"+e);
			}
		}// end while isAlive
		retval = getThreadResult(thread);
		return retval;
	}

	/**
	 * Find out the completion status of the thread and print out the final status of some variables.
	 * @param thread The Thread to print some information for.
	 * @return The routine returns true if the thread completed successfully,
	 * 	false if some error occured.
	 */
	private boolean getThreadResult(SicfTCPClientConnectionThread thread)
	{
		boolean retval;

		if(thread.getAcknowledge() == null)
			System.err.println("Acknowledge was null");
		else
			System.err.println("Acknowledge with timeToComplete:"+
				thread.getAcknowledge().getTimeToComplete());
		if(thread.getDone() == null)
		{
			System.err.println("Done was null");
			retval = false;
		}
		else
		{
			if(thread.getDone().getSuccessful())
			{
				System.err.println("Done was successful");
				retval = true;
			}
			else
			{
				System.err.println("Done returned error("+thread.getDone().getErrorNum()+
					"): "+thread.getDone().getErrorString());
				retval = false;
			}
		}
		return retval;
	}

	/**
	 * This routine parses arguments passed into SendConfigCommand.
	 * @see #filename
	 * @see #ccsPortNumber
	 * @see #address
	 * @see #lowerFilterString
	 * @see #upperFilterString
	 * @see #xBin
	 * @see #yBin
	 * @see #help
	 */
	private void parseArgs(String[] args)
	{
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-c")||args[i].equals("-ccsport"))
			{
				if((i+1)< args.length)
				{
					ccsPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-ccsport requires a port number");
			}
			else if(args[i].equals("-f")||args[i].equals("-file"))
			{
				if((i+1)< args.length)
				{
					filename = new String(args[i+1]);
					i++;
				}
				else
					errorStream.println("-filename requires a filename");
			}
			else if(args[i].equals("-h")||args[i].equals("-help"))
			{
				help();
				System.exit(0);
			}
			else if(args[i].equals("-ip")||args[i].equals("-address"))
			{
				if((i+1)< args.length)
				{
					try
					{
						address = InetAddress.getByName(args[i+1]);
					}
					catch(UnknownHostException e)
					{
						System.err.println(this.getClass().getName()+":illegal address:"+
							args[i+1]+":"+e);
					}
					i++;
				}
				else
					errorStream.println("-address requires an address");
			}
			else if(args[i].equals("-l")||args[i].equals("-lowerFilter"))
			{
				if((i+1)< args.length)
				{
					lowerFilterString = args[i+1];
					i++;
				}
				else
					errorStream.println("-lowerFilter requires a filter name");
			}
			else if(args[i].equals("-s")||args[i].equals("-serverport"))
			{
				if((i+1)< args.length)
				{
					serverPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-serverport requires a port number");
			}
			else if(args[i].equals("-u")||args[i].equals("-upperFilter"))
			{
				if((i+1)< args.length)
				{
					upperFilterString = args[i+1];
					i++;
				}
				else
					errorStream.println("-upperFilter requires a filter name");
			}
			else if(args[i].equals("-x")||args[i].equals("-xBin"))
			{
				if((i+1)< args.length)
				{
					xBin = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-xBin requires a valid number.");
			}
			else if(args[i].equals("-y")||args[i].equals("-yBin"))
			{
				if((i+1)< args.length)
				{
					yBin = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-yBin requires a valid number.");
			}
			else
				System.out.println(this.getClass().getName()+":Option not supported:"+args[i]);
		}
	}

	/**
	 * Help message routine.
	 */
	private void help()
	{
		System.out.println(this.getClass().getName()+" Help:");
		System.out.println("Options are:");
		System.out.println("\t-c[csport] <port number> - Port to send commands to.");
		System.out.println("\t-f[ile] <filename> - filter wheel filename.");
		System.out.println("\t-[ip]|[address] <address> - Address to send commands to.");
		System.out.println("\t-l[owerFilter] <filter type name> - Specify lower filter type.");
		System.out.println("\t-s[erverport] <port number> - Port for the CCS to send commands back.");
		System.out.println("\t-u[pperFilter] <filter type name> - Specify upper filter type.");
		System.out.println("\t-x[Bin] <binning factor> - X readout binning factor the CCD.");
		System.out.println("\t-y[Bin] <binning factor> - Y readout binning factor the CCD.");
		System.out.println("The default server port is "+DEFAULT_SERVER_PORT_NUMBER+".");
		System.out.println("The default CCS port is "+DEFAULT_CCS_PORT_NUMBER+".");
		System.out.println("The filters can be specified, otherwise if the filename is specified\n"+
			"the filters are selected randomly from that, otherwise 'null' is sent as a filter\n"+
			"and an error should occur.");
	}

	/**
	 * The main routine, called when SendConfigCommand is executed. This initialises the object, parses
	 * it's arguments, opens the filename, runs the run routine, and then closes the file.
	 * @see #parseArgs
	 * @see #init
	 * @see #run
	 */
	public static void main(String[] args)
	{
		boolean retval;
		SendConfigCommand sicf = new SendConfigCommand();

		sicf.parseArgs(args);
		sicf.init();
		if(sicf.address == null)
		{
			System.err.println("No Ccs Address Specified.");
			sicf.help();
			System.exit(1);
		}
		try
		{
			retval = sicf.run();
		}
		catch (Exception e)
		{
			retval = false;
			System.err.println("run failed:"+e);

		}
		if(retval)
			System.exit(0);
		else
			System.exit(2);
	}
}
//
// $Log: not supported by cvs2svn $
// Revision 1.3  2006/05/16 16:54:43  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.2  2002/12/16 17:08:45  cjm
// Changed NPCCDConfig to CCDConfig.
//
// Revision 1.1  2001/03/09 17:45:29  cjm
// Initial revision
//
//
