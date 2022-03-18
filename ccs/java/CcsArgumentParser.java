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
// CcsArgumentParser.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsArgumentParser.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
/**
 * This class holds command line argument data for the Ccs program, and allows the Ccs
 * to parse the command line and retrieve arguments.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CcsArgumentParser
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsArgumentParser.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * The minimum port number to listen for connections on.
	 */
	static final int MINIMUM_PORT_NUMBER = 1025;
	/**
	 * The maximum port number to send ISS commands on.
	 */
	static final int MAXIMUM_PORT_NUMBER = 65535;
	/**
	 * Command line argument. The level of logging to perform in the Ccs.
	 */
	private int logLevel = 0;
	/**
	 * Command line argument. The port number to listen for connections from clients on.
	 */
	private int ccsPortNumber = 0;
	/**
	 * Command line argument. The port number to listen for Telescope Image Transfer connections from clients on.
	 */
	private int titPortNumber = 0;
	/**
	 * Command line argument. The IP address of the machine the ISS is running on, to send ISS commands to.
	 */
	private InetAddress issAddress = null;
	/**
	 * Command line argument. The port number to send iss commands to.
	 */
	private int issPortNumber = 0;
	/**
	 * Command line argument. The IP address of the machine the DP(RT) is running on, 
	 * to send Data Pipeline (Real Time) commands to.
	 */
	private InetAddress dprtAddress = null;
	/**
	 * Command line argument. The port number to send DP(RT) commands to.
	 */
	private int dprtPortNumber = 0;
	/**
	 * Command line argument. Do we want to start a thread monitor?
	 */
	private boolean threadMonitor = false;
	/**
	 * Command line argument. A non-default Ccs network configuration property filename.
	 * Used in init to tell status which file of properties to load.
	 * @see Ccs#init
	 * @see CcsStatus#load
	 */
	private String netConfigurationFilename = null;
	/**
	 * Command line argument. A non-default Ccs configuration property filename.
	 * Used in init to tell status which file of properties to load.
	 * @see Ccs#init
	 * @see CcsStatus#load
	 */
	private String configurationFilename = null;
	/**
	 * Command line argument. A non-default Ccs current filter wheel configuration property filename.
	 * This is the per-semester filter wheel configuration that specifies what type of filter
	 * is in each filter wheel location.
	 * Used in init to tell status which file of properties to load.
	 * @see Ccs#init
	 * @see CcsStatus#load
	 */
	private String currentFilterConfigurationFilename = null;
	/**
	 * Command line argument. A non-default Ccs filter wheel configuration property filename.
	 * This is the filter database of all filters the telescope has.
	 * Used in init to tell status which file of properties to load.
	 * @see Ccs#init
	 * @see CcsStatus#load
	 */
	private String filterConfigurationFilename = null;
	/**
	 * Command line argument. A non-default FITS header default configuration property filename.
	 * Used in init to tell fitsHeaderDefaults which file of properties to load.
	 * @see Ccs#init
	 * @see Ccs#fitsHeaderDefaults
	 */
	private String fitsHeaderDefaultsFilename = null;

	/**
	 * Internal Constructor to construct an instance of the argument parser.
	 * Use the parse method to externally construct an instance of this class.
	 */
	private CcsArgumentParser()
	{
		logLevel = 0;
		ccsPortNumber = 0;
		titPortNumber = 0;
		issAddress = null;
		issPortNumber = 0;
		dprtAddress = null;
		dprtPortNumber = 0;
		threadMonitor = false;
		netConfigurationFilename = null;
		configurationFilename = null;
		currentFilterConfigurationFilename = null;
		filterConfigurationFilename = null;
		fitsHeaderDefaultsFilename = null;
	}

	/**
	 * This routine parses arguments passed into Ccs.
	 * Note this method calls System.exit to stop the program if '-help' is selected.
	 * @param args A String list of arguments to parse.
	 * @see #logLevel
	 * @see #ccsPortNumber
	 * @see #titPortNumber
	 * @see #issPortNumber
	 * @see #issAddress
	 * @see #dprtPortNumber
	 * @see #dprtAddress
	 * @see #threadMonitor
	 * @see #configurationFilename
	 * @see #netConfigurationFilename
	 * @see #filterConfigurationFilename
	 * @see #currentFilterConfigurationFilename
	 * @see #fitsHeaderDefaultsFilename
	 * @see #help
	 */
	public static CcsArgumentParser parse(String[] args)
	{
		CcsArgumentParser arguments = null;

		arguments = new CcsArgumentParser();
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-l")||args[i].equals("-log"))
			{
				if((i+1)< args.length)
				{
					arguments.logLevel = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					System.err.println("-log requires a log level");
			}
			else if(args[i].equals("-c")||args[i].equals("-ccsport"))
			{
				if((i+1)< args.length)
				{
					arguments.ccsPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					System.err.println("-ccsport requires a port number");
			}
			else if(args[i].equals("-i")||args[i].equals("-issport"))
			{
				if((i+1)< args.length)
				{
					arguments.issPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					System.err.println("-issport requires a port number");
			}
			else if(args[i].equals("-issip")||args[i].equals("-issaddress"))
			{
				if((i+1)< args.length)
				{
					try
					{
						arguments.issAddress = InetAddress.getByName(args[i+1]);
					}
					catch(UnknownHostException e)
					{
						System.err.println("Ccs:argument parser:illegal ISS address:"+
							args[i+1]+":"+e);
					}
					i++;
				}
				else
					System.err.println("-issaddress requires a valid ip address");
			}
			else if(args[i].equals("-d")||args[i].equals("-dprtport"))
			{
				if((i+1)< args.length)
				{
					arguments.dprtPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					System.err.println("-dprtport requires a port number");
			}
			else if(args[i].equals("-dprtip")||args[i].equals("-dprtaddress"))
			{
				if((i+1)< args.length)
				{
					try
					{
						arguments.dprtAddress = InetAddress.getByName(args[i+1]);
					}
					catch(UnknownHostException e)
					{
						System.err.println("Ccs:argument parser::illegal DP(RT) address:"+
							args[i+1]+":"+e);
					}
					i++;
				}
				else
					System.err.println("-dprtaddress requires a valid ip address");
			}
			else if(args[i].equals("-nc")||args[i].equals("-netconfig"))
			{
				if((i+1)< args.length)
				{
					arguments.netConfigurationFilename = new String(args[i+1]);
					i++;
				}
				else
					System.err.println("-netconfig requires a filename");
			}
			else if(args[i].equals("-co")||args[i].equals("-config"))
			{
				if((i+1)< args.length)
				{
					arguments.configurationFilename = new String(args[i+1]);
					i++;
				}
				else
					System.err.println("-config requires a filename");
			}
			else if(args[i].equals("-fdc")||args[i].equals("-filterdatabaseconfig"))
			{
				if((i+1)< args.length)
				{
					arguments.filterConfigurationFilename = new String(args[i+1]);
					i++;
				}
				else
					System.err.println("-filterdatabaseconfig requires a filename");
			}
			else if(args[i].equals("-cfc")||args[i].equals("-currentfilterconfig"))
			{
				if((i+1)< args.length)
				{
					arguments.currentFilterConfigurationFilename = new String(args[i+1]);
					i++;
				}
				else
					System.err.println("-currentfilterconfig requires a filename");
			}
			else if(args[i].equals("-f")||args[i].equals("-fitsconfig"))
			{
				if((i+1)< args.length)
				{
					arguments.fitsHeaderDefaultsFilename = new String(args[i+1]);
					i++;
				}
				else
					System.err.println("-fitsconfig requires a filename");
			}
			else if(args[i].equals("-t")||args[i].equals("-titport"))
			{
				if((i+1)< args.length)
				{
					arguments.titPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					System.err.println("-titport requires a port number");
			}
			else if(args[i].equals("-threadmonitor"))
			{
				arguments.threadMonitor = true;
			}
			else if(args[i].equals("-h")||args[i].equals("-help"))
			{
				arguments.help();
				System.exit(0);
			}
			else
				System.err.println("Ccs '"+args[i]+"' not a recognised option");
		}// end for
		return arguments;
	}

	/**
	 * Help message routine. Prints all command line arguments.
	 */
	public void help()
	{
		System.out.println("Ccs Help:");
		System.out.println("Ccs is the 'CCD Control System', which controls a SDSU CCD Controller.");
		System.out.println("Arguments are:");
		System.out.println("\t-c[csport] <port number> - Port to wait for client connections on.");
		System.out.println("\t-t[itport] <port number> - Port to wait for Telescope Image Transfer "+
					"client connections on.");
		System.out.println("\t-i[ssport] <port number> - Port to send ISS commands to.");
		System.out.println("\t-[issip]|[issaddress] <address> - Address to send ISS commands to.");
		System.out.println("\t-d[prtport] <port number> - Port to send DP(RT) commands to.");
		System.out.println("\t-[dprtip]|[dprtaddress] <address> - Address to send DP(RT) commands to.");
		System.out.println("\t-[nc]|[netconfig] <filename> - Location of Ccs network "+
			"configuration properties file.");
		System.out.println("\t-[co]|[config] <filename> - Location of Ccs configuration properties file.");
		System.out.println("\t-[fdc]|[filterdatabaseconfig] <filename> - Location of Ccs filter "+
			"configuration database properties file.");
		System.out.println("\t-[cfc]|[currentfilterconfig] <filename> - Location of Ccs per-semester "+
			"current filter configuration properties file.");
		System.out.println("\t-f[itsconfig] <filename> - Location of FITS header defaults "+
			"configuration properties file.");
		System.out.println("\t-l[og] <log level> - log level.");
		System.out.println("\t-threadmonitor - show thread monitor.");
	}

	/**
	 * Retrieve method to get the log level.
	 * @return The log level set during argument parsing, or 0 if none set.
	 * @see #logLevel
	 */
	public int getLogLevel()
	{
		return logLevel;
	}

	/**
	 * Retrieve method to get the ccs port number.
	 * @return The port number set during argument parsing, or 0 if none set.
	 * @see #ccsPortNumber
	 */
	public int getCcsPortNumber()
	{
		return ccsPortNumber;
	}

	/**
	 * Retrieve method to get the tit port number.
	 * @return The port number set during argument parsing, or 0 if none set.
	 * @see #titPortNumber
	 */
	public int getTitPortNumber()
	{
		return titPortNumber;
	}

	/**
	 * Retrieve method to get the iss internet address.
	 * @return The internet address set during argument parsing, or null if none set.
	 * @see #issAddress
	 */
	public InetAddress getIssAddress()
	{
		return issAddress;
	}

	/**
	 * Retrieve method to get the iss port number.
	 * @return The port number set during argument parsing, or 0 if none set.
	 * @see #issPortNumber
	 */
	public int getIssPortNumber()
	{
		return issPortNumber;
	}

	/**
	 * Retrieve method to get the dprt internet address.
	 * @return The internet address set during argument parsing, or null if none set.
	 * @see #dprtAddress
	 */
	public InetAddress getDpRtAddress()
	{
		return dprtAddress;
	}

	/**
	 * Retrieve method to get the dprt port number.
	 * @return The port number set during argument parsing, or 0 if none set.
	 * @see #dprtPortNumber
	 */
	public int getDpRtPortNumber()
	{
		return dprtPortNumber;
	}

	/**
	 * Retrieve method to get whether to start the thread monitor.
	 * @return A boolean, true if thread monitoring was selected else false.
	 * @see #threadMonitor
	 */
	public boolean getThreadMonitor()
	{
		return threadMonitor;
	}

	/**
	 * Retrieve method to get the network configuration file filename.
	 * @return The filename set during argument parsing, or null if none set.
	 * @see #netConfigurationFilename
	 */
	public String getNetConfigurationFilename()
	{
		return netConfigurationFilename;
	}

	/**
	 * Retrieve method to get the general configuration file filename.
	 * @return The filename set during argument parsing, or null if none set.
	 * @see #configurationFilename
	 */
	public String getConfigurationFilename()
	{
		return configurationFilename;
	}

	/**
	 * Retrieve method to get the filter configuration database file filename.
	 * @return The filename set during argument parsing, or null if none set.
	 * @see #filterConfigurationFilename
	 */
	public String getFilterConfigurationFilename()
	{
		return filterConfigurationFilename;
	}

	/**
	 * Retrieve method to get the per-semester filter configuration file filename.
	 * @return The filename set during argument parsing, or null if none set.
	 * @see #currentFilterConfigurationFilename
	 */
	public String getCurrentFilterConfigurationFilename()
	{
		return currentFilterConfigurationFilename;
	}

	/**
	 * Retrieve method to get the FITS header defaults configuration file filename.
	 * @return The filename set during argument parsing, or null if none set.
	 * @see #fitsHeaderDefaultsFilename
	 */
	public String getFitsHeaderDefaultsFilename()
	{
		return fitsHeaderDefaultsFilename;
	}
}
//
// $Log: not supported by cvs2svn $
// Revision 0.5  2006/05/16 14:25:40  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.4  2001/07/03 16:33:10  cjm
// Added TIT support to command line arguments.
//
// Revision 0.3  2000/08/09 13:53:31  cjm
// Now two filter wheel configuration filenames.
//
// Revision 0.2  2000/06/30 17:40:20  cjm
// Fixed documentation bugs.
//
// Revision 0.1  2000/06/30 17:35:21  cjm
// initial revision.
//
//
