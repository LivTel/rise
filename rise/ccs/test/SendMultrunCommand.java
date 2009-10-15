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
// SendMultrunCommand.java 
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/SendMultrunCommand.java,v 1.1 2009-10-15 10:19:32 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.phase2.*;
import ngat.util.*;

/**
 * This class send a MULTRUN to the CCD Control System. 
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SendMultrunCommand
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
	 * Exposure length. Defaults to zero, which should cause MULTRUN to return an error.
	 */
	private int exposureLength = 0;
	/**
	 * Number of exposures for the MULTRUN to take. 
	 * Defaults to zero, which should cause MULTRUN to return an error.
	 */
	private int exposureCount = 0;
	/**
	 * Whether this MULTRUN has standard flags set (is of a standard source). Defaults to false.
	 */
	private boolean standard = false;
	/**
	 * Whether to send the generated filenames to the DpRt. Defaults to false.
	 */
	private boolean pipelineProcess = false;

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
	 * This routine creates a MULTRUN command. 
	 * @return An instance of MULTRUN.
	 * @see #exposureLength
	 * @see #exposureCount
	 * @see #standard
	 * @see #pipelineProcess
	 */
	private MULTRUN createMultrun()
	{
		String string = null;
		MULTRUN multrunCommand = null;

		multrunCommand = new MULTRUN("SendMultrunCommand");
		multrunCommand.setExposureTime(exposureLength);
		multrunCommand.setNumberExposures(exposureCount);
		multrunCommand.setStandard(standard);
		multrunCommand.setPipelineProcess(pipelineProcess);
		return multrunCommand;
	}

	/**
	 * This is the run routine. It creates a MULTRUN object and sends it to the using a 
	 * SicfTCPClientConnectionThread, and awaiting the thread termination to signify message
	 * completion. 
	 * @return The routine returns true if the command succeeded, false if it failed.
	 * @exception Exception Thrown if an exception occurs.
	 * @see #createMultrun
	 * @see SicfTCPClientConnectionThread
	 * @see #getThreadResult
	 */
	private boolean run() throws Exception
	{
		ISS_TO_INST issCommand = null;
		SicfTCPClientConnectionThread thread = null;
		boolean retval;

		issCommand = (ISS_TO_INST)(createMultrun());
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
			System.out.println("Done was null");
			retval = false;
		}
		else
		{
			if(thread.getDone().getSuccessful())
			{
				System.out.println("Done was successful");
				if(thread.getDone() instanceof EXPOSE_DONE)
				{
					System.out.println("\tFilename:"+
						((EXPOSE_DONE)(thread.getDone())).getFilename());
				}
				retval = true;
			}
			else
			{
				System.out.println("Done returned error("+thread.getDone().getErrorNum()+
					"): "+thread.getDone().getErrorString());
				retval = false;
			}
		}
		return retval;
	}

	/**
	 * This routine parses arguments passed into SendMultrunCommand.
	 * @see #exposureLength
	 * @see #exposureCount
	 * @see #standard
	 * @see #pipelineProcess
	 * @see #ccsPortNumber
	 * @see #address
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
			else if(args[i].equals("-l")||args[i].equals("-exposureLength"))
			{
				if((i+1)< args.length)
				{
					exposureLength = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-exposureLength requires an argument.");
			}
			else if(args[i].equals("-n")||args[i].equals("-exposureCount"))
			{
				if((i+1)< args.length)
				{
					exposureCount = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-exposureCount requires an argument.");
			}
			else if(args[i].equals("-p")||args[i].equals("-pipelineProcess"))
			{
					pipelineProcess = true;
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
			else if(args[i].equals("-t")||args[i].equals("-standard"))
			{
				standard = true;
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
		System.out.println("\t-[ip]|[address] <address> - Address to send commands to.");
		System.out.println("\t-[l]|[exposureLength] <time in millis> - Specify exposure length.");
		System.out.println("\t-[n]|[exposureCount] <number> - Specify number of exposures.");
		System.out.println("\t-s[erverport] <port number> - Port for the CCS to send commands back.");
		System.out.println("\t-p[ipelineProcess] - Send frames to pipeline process.");
		System.out.println("\t-[t]|[standard] - Set standard parameters.");
		System.out.println("The default server port is "+DEFAULT_SERVER_PORT_NUMBER+".");
		System.out.println("The default CCS port is "+DEFAULT_CCS_PORT_NUMBER+".");
	}

	/**
	 * The main routine, called when SendMultrunCommand is executed. This initialises the object, parses
	 * it's arguments, opens the filename, runs the run routine, and then closes the file.
	 * @see #parseArgs
	 * @see #init
	 * @see #run
	 */
	public static void main(String[] args)
	{
		boolean retval;
		SendMultrunCommand smc = new SendMultrunCommand();

		smc.parseArgs(args);
		smc.init();
		if(smc.address == null)
		{
			System.err.println("No Ccs Address Specified.");
			smc.help();
			System.exit(1);
		}
		try
		{
			retval = smc.run();
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
// Revision 1.3  2006/05/16 16:54:45  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.2  2003/03/26 15:54:11  cjm
// Fixed phase2 change.
//
// Revision 1.1  2001/07/03 18:34:23  cjm
// Initial revision
//
// Revision 1.1  2001/03/09 17:45:29  cjm
// Initial revision
//
//
