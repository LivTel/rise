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
// SendRISEConfigCommand.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/SendRISEConfigCommand.java,v 1.1 2022-03-14 15:21:10 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.phase2.*;
import ngat.util.*;

/**
 * This class send a RISEConfig configuration to the RISE Control System.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SendRISEConfigCommand
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
	 * binning of configuration. Defaults to 1.
	 */
	private int bin = 1;

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
	 * This routine creates a CONFIG command. This object
	 * has a RISEConfig phase2 object with it, this is created and it's fields initialised.
	 * @return An instance of CONFIG.
	 * @see #bin
	 */
	private CONFIG createConfig()
	{
		String string = null;
		CONFIG configCommand = null;
		RISEConfig riseConfig = null;
		RISEDetector detector = null;

		configCommand = new CONFIG("Object Id");
		riseConfig = new RISEConfig("Object Id");
	// detector for config
		detector = new RISEDetector();
	// windows
		detector.clearAllWindows();
		detector.setWindowFlags(0);
		riseConfig.setDetector(0,detector);
		detector.setXBin(bin);
		detector.setYBin(bin);
	// InstrumentConfig fields.
		configCommand.setConfig(riseConfig);
		return configCommand;
	}

	/**
	 * This is the run routine. It creates a CONFIG object and sends it to the using a 
	 * SicfTCPClientConnectionThread, and awaiting the thread termination to signify message
	 * completion. 
	 * @return The routine returns true if the command succeeded, false if it failed.
	 * @exception Exception Thrown if an exception occurs.
	 * @see #createConfig
	 * @see SicfTCPClientConnectionThread
	 * @see #getThreadResult
	 */
	private boolean run() throws Exception
	{
		ISS_TO_INST issCommand = null;
		SicfTCPClientConnectionThread thread = null;
		boolean retval;

		issCommand = (ISS_TO_INST)(createConfig());
		if(issCommand instanceof CONFIG)
		{
			CONFIG configCommand = (CONFIG)issCommand;
			RISEConfig riseConfig = (RISEConfig)(configCommand.getConfig());
			System.err.println("CONFIG:"+
				riseConfig.getDetector(0).getXBin()+":"+
				riseConfig.getDetector(0).getYBin()+".");
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
	 * This routine parses arguments passed into SendRISEConfigCommand.
	 * @see #ccsPortNumber
	 * @see #address
	 * @see #bin
	 * @see #help
	 */
	private void parseArgs(String[] args)
	{
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-b")||args[i].equals("-binning"))
			{
				if((i+1)< args.length)
				{
					bin = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-binning requires a valid number.");
			}
			else if(args[i].equals("-c")||args[i].equals("-ccsport"))
			{
				if((i+1)< args.length)
				{
					ccsPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-ccsport requires a port number");
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
		System.out.println("\t-b[inning] <binning factor> - detector binning factor for the CCD.");
		System.out.println("\t-c[csport] <port number> - Port to send commands to.");
		System.out.println("\t-f[ile] <filename> - filter wheel filename.");
		System.out.println("\t-[ip]|[address] <address> - Address to send commands to.");
		System.out.println("\t-s[erverport] <port number> - Port for the CCS to send commands back.");
		System.out.println("The default server port is "+DEFAULT_SERVER_PORT_NUMBER+".");
		System.out.println("The default CCS port is "+DEFAULT_CCS_PORT_NUMBER+".");
	}

	/**
	 * The main routine, called when SendRISEConfigCommand is executed. This initialises the object, parses
	 * it's arguments, opens the filename, runs the run routine, and then closes the file.
	 * @see #parseArgs
	 * @see #init
	 * @see #run
	 */
	public static void main(String[] args)
	{
		boolean retval;
		SendRISEConfigCommand sicf = new SendRISEConfigCommand();

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
//
