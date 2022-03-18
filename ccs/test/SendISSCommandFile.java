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
// SendISSCommandFile.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/SendISSCommandFile.java,v 1.1 2009-10-15 10:19:32 cjm Exp $

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.util.*;

/**
 * This class reads in a series of ngat.message.ISS_INST messages and will send them to a specified ip address and
 * a specified port. It is used as a test harness for testing instrument control processes (specifically the CCS).
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class SendISSCommandFile
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
	 * The filename to get commands from.
	 */
	private String filename = null;
	/**
	 * The stream to input objects from.
	 */
	private ObjectInputStream inputStream = null;
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
	 * Do we want to start a thread monitor?
	 */
	private boolean threadMonitor = false;
	/**
	 * The thread monitor window.
	 */
	private ThreadMonitorFrame threadMonitorFrame = null;
	/**
	 * The value to return from this program.
	 */
	private boolean returnValue = false;

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
	 * This routine opens the file and sets up objects streams.
	 * @see #inputStream
	 */
	private void open()
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(filename);
		}
		catch(IOException e)
		{
			System.err.println("Opening file '"+filename+"' failed:"+e);
			System.exit(1);
		}
		try
		{
			inputStream = new ObjectInputStream(fis);
		}
		catch(IOException e)
		{
			System.err.println("Opening ObjectInputStream '"+filename+"' failed:"+e);
			System.exit(1);
		}
	}

	/**
	 * This is the run routine. It reads in objects from the specified file and puts them in a list.
	 * It then goes through this list, sending each message in turn to the instrument control process
	 * using a SicfTCPClientConnectionThread, and awaiting the thread termination to signify message
	 * completion. Some commands are sent in parallel to simulate ABORT/GET_STATUS and similar requests
	 * which would occur when other messages are being dealt with.
	 * @see SicfTCPClientConnectionThread
	 */
	private void run()
	{
		String string = null;
		ISS_TO_INST issCommand = null;
		SicfTCPClientConnectionThread thread = null;
		boolean done = false;
		Vector commandList = null;
		Vector threadList = null;
		int index = 0,threadListIndex = 0;

		if(inputStream ==null)
			return;
		// are we supposed to be starting  a thread monitor.
		if(threadMonitor)
		{
			threadMonitorFrame = new ThreadMonitorFrame(this.getClass().getName());
			threadMonitorFrame.pack();
			threadMonitorFrame.setVisible(true);
			threadMonitorFrame.getThreadMonitor().setUpdateTime(1000);
		}
		// while we have not reached the end of the file
		commandList = new Vector();
		while(!done)
		{
			// try to get a command object from the file.
			try
			{
				issCommand = (ISS_TO_INST)(inputStream.readObject());
			}
			catch(IOException e)
			{
				if(e instanceof java.io.EOFException)
					System.err.println("run:Loaded "+commandList.size()+" objects.");
				else
					System.err.println("run:Reading object failed:"+e);
				done = true;
			}
			catch(ClassNotFoundException e)
			{
				System.err.println("run:Reading object failed:"+e);
				done = true;
			}
			if(!done)
			{
				commandList.addElement(issCommand);
			}
		}
		// send the commands to the server
		threadList = new Vector();
		index = 0;
		while((index < commandList.size()))
		{
			issCommand = (ISS_TO_INST)commandList.elementAt(index);
			// should we wait for the last commands to finish before starting this one?
			if(!(issCommand instanceof INTERRUPT))
			{
				threadListIndex = 0;
				while((threadListIndex < threadList.size()))
				{
					thread = (SicfTCPClientConnectionThread)threadList.elementAt(threadListIndex);
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
					threadListIndex++;
				}// end while threads in list
			}// end if this command is not an interrupt command
			// start a client thread to send this command to the server.
			thread = new SicfTCPClientConnectionThread(address,ccsPortNumber,issCommand);
			thread.start();
			threadList.addElement(thread);
			System.err.println("run:thread started for command:"+issCommand.getClass().getName());
			if(issCommand instanceof RUNAT)
				System.err.println("run:RUNAT due to start at "+
					((RUNAT)issCommand).getStartTime().toString()+" for "+
					((RUNAT)issCommand).getExposureTime()+" milliseconds.");
			index++;
		}
		// Initialise return value to positive.
		returnValue = true;
		// ensure all threads are dead before stopping program, and print out some results.
		index = 0;
		while((index < threadList.size()))
		{
			thread = (SicfTCPClientConnectionThread)threadList.elementAt(index);
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
			}
			printThreadResult(thread);
			index++;
		}
	}

	/**
	 * Print out the final status of some variables in thread. If something failed,
	 * returnValue is set to false.
	 * @param thread The Thread to print some information for.
	 * @see #returnValue
	 */
	private void printThreadResult(SicfTCPClientConnectionThread thread)
	{
		if(thread.getAcknowledge() == null)
		{
			System.err.println("Acknowledge was null");
			returnValue = false;
		}
		else
			System.err.println("Acknowledge with timeToComplete:"+
				thread.getAcknowledge().getTimeToComplete());
		if(thread.getDone() == null)
		{
			System.err.println("Done was null");
			returnValue = false;
		}
		else
		{
			if(thread.getDone().getSuccessful())
				System.err.println("Done was successful");
			else
			{
				System.err.println("Done returned error("+thread.getDone().getErrorNum()+
					"): "+thread.getDone().getErrorString());
				returnValue = false;
			}
			if(thread.getDone() instanceof GET_STATUS_DONE)
				printCcsStatus((GET_STATUS_DONE)thread.getDone());
		}
	}

	/**
	 * Print out the return values for a get status command.
	 * @param d The GET_STATUS_DONE object with the results in.
	 */
	private void printCcsStatus(GET_STATUS_DONE d)
	{
		Hashtable hashtable = null;
		String keyString = null;
		Object value = null;
		String valueString = null;
		Integer valueInteger = null;
		Long valueLong = null;
		Boolean valueBoolean = null;
		Double valueDouble = null;

		System.err.println("Ccs Status:"+d.getCurrentMode());
		hashtable = d.getDisplayInfo();
		hashtable.elements();
		for(Enumeration e = hashtable.keys(); e.hasMoreElements();)
		{
			keyString = (String)e.nextElement();
			value = (Object)hashtable.get(keyString);
			if(value instanceof String)
			{
				valueString = (String)value;
				System.out.println(keyString+" = "+valueString);
			}
			else if(value instanceof Integer)
			{
				valueInteger = (Integer)value;
				System.out.println(keyString+" = "+valueInteger.toString());
			}
			else if(value instanceof Long)
			{
				valueLong = (Long)value;
				System.out.println(keyString+" = "+valueLong.toString());
			}
			else if(value instanceof Double)
			{
				valueDouble = (Double)value;
				System.out.println(keyString+" = "+valueDouble.toString());
			}
			else if(value instanceof Boolean)
			{
				valueBoolean = (Boolean)value;
				System.out.println(keyString+" = "+valueBoolean.toString());
			}
			else
			{
				System.out.println(keyString+" has value of unknown class:"+
					value.getClass().getName());
			}
		}
	}

	/**
	 * Routine to be called at the end of execution of SendISSCommandFile to close the file.
	 */
	private void close()
	{
		if(inputStream == null)
			return;
		try
		{
			inputStream.close();
		}
		catch(IOException e)
		{
			System.err.println("close:close:"+e);
		}
	}

	/**
	 * This routine parses arguments passed into SendISSCommandFile.
	 * @see #filename
	 * @see #ccsPortNumber
	 * @see #address
	 * @see #threadMonitor
	 * @see #help
	 */
	private void parseArgs(String[] args)
	{
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-f")||args[i].equals("-file"))
			{
				if((i+1)< args.length)
				{
					filename = new String(args[i+1]);
					i++;
				}
				else
					errorStream.println("-filename requires a filename");
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
			else if(args[i].equals("-t")||args[i].equals("-threadmonitor"))
			{
				threadMonitor = true;
			}
			else if(args[i].equals("-h")||args[i].equals("-help"))
			{
				help();
				System.exit(0);
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
		System.out.println("\t-[ip]|[address] <address> - Address to send commands to.");
		System.out.println("\t-c[csport] <port number> - Port to send commands to.");
		System.out.println("\t-s[erverport] <port number> - Port for the CCS to send commands back.");
		System.out.println("\t-f[ile] <filename> - filename to load commands from.");
		System.out.println("\t-t[hreadmonitor] - show thread monitor.");
		System.out.println("The default server port is "+DEFAULT_SERVER_PORT_NUMBER+".");
		System.out.println("The default CCS port is "+DEFAULT_CCS_PORT_NUMBER+".");
	}

	/**
	 * The main routine, called when SendISSCommandFile is executed. This initialises the object, parses
	 * it's arguments, opens the filename, runs the run routine, and then closes the file.
	 * @see #parseArgs
	 * @see #open
	 * @see #run
	 * @see #close
	 */
	public static void main(String[] args)
	{
		SendISSCommandFile sicf = new SendISSCommandFile();
		sicf.parseArgs(args);
		sicf.init();
		if(sicf.filename == null)
		{
			System.err.println("No Filename Specified.");
			sicf.help();
			System.exit(1);
		}
		if(sicf.address == null)
		{
			System.err.println("No Address Specified.");
			sicf.help();
			System.exit(1);
		}
		sicf.open();
		sicf.run();
		sicf.close();
		if(sicf.returnValue)
			System.exit(0);
		else
			System.exit(1);
	}
}

// $Log: not supported by cvs2svn $
// Revision 1.13  2006/05/16 16:54:44  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.12  2001/06/26 13:43:13  cjm
// Added extra ACK and DONE prints for filenames.
//
// Revision 1.11  2001/03/08 14:41:05  cjm
// Return value reflects whether commands were done successfully.
//
// Revision 1.10  2001/03/05 19:18:54  cjm
// Changed setController.
//
// Revision 1.9  1999/11/05 13:57:55  cjm
// Change related to new ACK subclasses.
//
// Revision 1.8  1999/07/09 10:50:18  dev
// Fixed error is sending commands. If a non-interruptable command was
// follwed by an interuptable followed by a non-interuptable the last
// command would fail if the first was still running as the loop did not
// wait for the first to stop. It does now.
//
// Revision 1.7  1999/06/24 12:40:21  dev
// "Backup"
//
// Revision 1.6  1999/06/07 16:55:37  dev
// CCDConfig object improvements/fake ISS server
//
// Revision 1.5  1999/06/02 15:19:13  dev
// "Backup"
//
// Revision 1.4  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.3  1999/04/27 11:26:51  dev
// Backup
//
// Revision 1.2  1999/03/25 14:02:57  dev
// Backup
//
// Revision 1.1  1999/03/19 11:51:09  dev
// Backup
//
