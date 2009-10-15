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
// MakeISSCommandFile.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/MakeISSCommandFile.java,v 1.1 2009-10-15 10:19:32 cjm Exp $

import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.phase2.*;
/**
 * This class allows creation of a serialized object file, containing a series of ngat.message messages,
 * which are used to send messages between the ISS and CCS. The messages are saved to file to allow the 
 * SendISSCommandFile program to send them to a test server as a test harness.
 * @see SendISSCommandFile
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class MakeISSCommandFile
{
	/**
	 * The filename to save commands to.
	 */
	private String filename = null;
	/**
	 * The list of commands to save to file
	 */
	private Vector commandList = null;
	/**
	 * The stream to output objects to.
	 */
	private ObjectOutputStream outputStream = null;
	/**
	 * The stream to write error messages to - defaults to System.err.
	 */
	private PrintStream errorStream = System.err;

	/**
	 * This is the initialisation routine.
	 */
	private void init()
	{
		commandList = new Vector();
	}

	/**
	 * This routine opens the file and sets up objects streams.
	 * @see #outputStream
	 */
	private void open()
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(filename);
		}
		catch(IOException e)
		{
			System.err.println("Opening file '"+filename+"' failed:"+e);
			System.exit(1);
		}
		try
		{
			outputStream = new ObjectOutputStream(fos);
		}
		catch(IOException e)
		{
			System.err.println("Opening ObjectOutputStream '"+filename+"' failed:"+e);
			System.exit(1);
		}
	}

	/**
	 * This is the run routine. For each command string in the command list, it parses the string to create
	 * an ISS_TO_INST command object, and then writes it to the output stream.
	 * @see #outputStream
	 */
	private void run()
	{
		String string = null;
		ISS_TO_INST issCommand = null;

		if(outputStream ==null)
			return;
		for(int i = 0;i < commandList.size();i++)	
		{
			string = (String)(commandList.elementAt(i));
			issCommand = parse(string);
			if(issCommand != null)
			{
				try
				{
					outputStream.writeObject(issCommand);
				}
				catch(IOException e)
				{
					System.err.println("run:Writing object:"+issCommand+":failed.");
				}
			}
		}
	}

	/** 
	 * This routine parses the string and tries to create an ISS_TO_INST message object from it.
	 * @return The ISS_TO_INST command object.
	 */
	private ISS_TO_INST parse(String string)
	{
		ISS_TO_INST issCommand = null;
		Class constructorParamaterList[] = new Class[1];
		Object constructorArguments[] = new Object[1];
		Class cl = null;
		Constructor con = null;
		String className = null;
		String paramaterString = null;
		int sindex,eindex;

	// get constructor paramaters
		try
		{
			constructorParamaterList[0] = Class.forName("java.lang.String");
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("parse:String Class not found:"+e);
			return null;
		}
	// find className/paramaters in string
		sindex = string.indexOf('(');
		if(sindex < 0)
		{
			className = new String(string);
			paramaterString = null;
		}
		else
		{
			className = string.substring(0,sindex);
			eindex = string.indexOf(')');
			if(eindex < 0)
				paramaterString = string.substring(sindex+1,string.length());
			else
				paramaterString = string.substring(sindex+1,eindex);
		}
	// try to create class
		try
		{
			cl = Class.forName(className);
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("parse:Class '"+className+"' not found:"+e);
			return null;
		}
	// get a suitable constuctor
		try
		{	
			con = cl.getDeclaredConstructor(constructorParamaterList);
		}
		catch(NoSuchMethodException e)
		{
			System.err.println("parse:'"+className+"':"+e);
			return null;
		}
	// try to create instance of class
		constructorArguments[0] = new String(this.getClass().getName());
		try
		{
			issCommand = (ISS_TO_INST)con.newInstance(constructorArguments);
		}
		catch(IllegalAccessException e)
		{
			System.err.println("parse:'"+className+"':"+e);
			return null;
		}
		catch(IllegalArgumentException e)
		{
			System.err.println("parse:'"+className+"':"+e);
			return null;
		}
		catch(InstantiationException e)
		{
			System.err.println("parse:'"+className+"':"+e);
			return null;
		}
		catch(InvocationTargetException e)
		{
			System.err.println("parse:'"+className+"':"+e);
			return null;
		}
		System.out.println("parse:class:"+issCommand.getClass().getName());
		System.out.flush();
	// parse arguments of object
		parseArguments(issCommand,paramaterString);
	// return object
		return issCommand;
	}

	/** 
	 * This routine parses the argument string and tries to fill in paramater to an ISS_TO_INST from it.
	 * Currently assumes each paramater argument maps to a 'set'N method which takes one paramater
	 * which is a class that can be constructed with the parameter string passed in or it's an int.
	 * @param issCommand The command to set the arguments for.
	 * @param paramaterString The string to parse to get the paramaters.
	 */
	private void parseArguments(ISS_TO_INST issCommand,String paramaterString)
	{
		Method[] methodList;
		StringTokenizer st = null;
		String s =  null;
		int methodIndex = 0;

	// get method list
		methodList = issCommand.getClass().getMethods();
	// create string tokenizer for parameters
		st = new StringTokenizer(paramaterString,",");
	// go through class methods and string tokens matching
	// 'set' methods to paramaters
		methodIndex = 0;
		while(st.hasMoreTokens())
		{
			s = st.nextToken();
			while((methodIndex<methodList.length)&&
				(methodList[methodIndex].getName().indexOf("set")!=0))
			{
				methodIndex++;
			}
			if(methodIndex<methodList.length)
			{
				callMethod(issCommand,methodList[methodIndex],s);
			}// end if methodIndex still in list
			methodIndex++;
		}
	}

	/**
	 * Routine to call the specified method on the object, giving it the paramater in the string.
	 * @param object The object to call the method on.
	 * @param method The method to call for the object instance.
	 * @param string The string of the paramater to pass to the method.
	 */
	private void callMethod(Object object,Method method,String string)
	{
		Class methodParameterClassList[];
		Object argumentList[] = new Object[1];

		methodParameterClassList = method.getParameterTypes();
		if(methodParameterClassList.length == 1)
		{
			if(methodParameterClassList[0].getName().equals("boolean"))
			{
				try
				{
					argumentList[0] = new Boolean(string);
				}
				catch(NumberFormatException e)
				{
					System.err.println("call method:"+method.getName()+
						" failed parsing boolean ("+string+"), using false instead.");
					argumentList[0] = new Boolean(false);
				}
			}
			else if(methodParameterClassList[0].getName().equals("int"))
			{
				try
				{
					argumentList[0] = new Integer(string);
				}
				catch(NumberFormatException e)
				{
					System.err.println("call method:"+method.getName()+
						" failed parsing int ("+string+"), using 0 instead.");
					argumentList[0] = new Integer(0);
				}
			}
			else if(methodParameterClassList[0].getName().equals("long"))
			{
				try
				{
					argumentList[0] = new Long(string);
				}
				catch(NumberFormatException e)
				{
					System.err.println("call method:"+method.getName()+
						" failed parsing long ("+string+"), using 0 instead.");
					argumentList[0] = new Long(0);
				}
			}
			else if(methodParameterClassList[0].getName().equals("float"))
			{
				try
				{
					argumentList[0] = new Float(string);
				}
				catch(NumberFormatException e)
				{
					System.err.println("call method:"+method.getName()+
						" failed parsing float ("+string+"), using 0 instead.");
					argumentList[0] = new Float(0.0);
				}
			}
			else if(methodParameterClassList[0].getName().equals("java.lang.String"))
			{
				argumentList[0] = new String(string);
			}
			else if(methodParameterClassList[0].getName().equals("java.util.Date"))
			{
				Date date = new Date();

				if(string.equals("now"))
				{
					date = new Date();
				}
				else if(string.startsWith("+"))// increment by minutes
				{
					Calendar calendar = Calendar.getInstance();

					calendar.setTime(date);
					try
					{
						int increment  = Integer.parseInt(string.substring(1));
						calendar.add(Calendar.MINUTE,increment);
						date = calendar.getTime();
					}
					catch(NumberFormatException e)
					{
						System.err.println("callMethod:"+method.getName()+
							":Illegal Date increment argument:"+
							string+":using current date");
					}
				}
				else
				{
					try
					{
						date = DateFormat.getDateInstance().parse(string);
					}
					catch(ParseException e)
					{
						System.err.println("callMethod:"+method.getName()+
							":Illegal Date argument:"+string+
							":using current date");
						date = new Date();
					}
				}
				System.out.println("callMethod:"+method.getName()+":date is:"+date.toString());
				System.out.flush();
				argumentList[0] = date;
			}
			else if(methodParameterClassList[0].getName().
				equals("ngat.phase2.InstrumentConfig"))
			{
				CCDConfig ccdConfig = null;

				ccdConfig = createCCDConfig(string);
				argumentList[0] = ccdConfig;
			}
			else
			{
				System.err.println("callMethod:"+method.getName()+
					":method Parameter Class is not recognised:"+
					methodParameterClassList[0].getName());
				return;
			}
			System.out.println("call method:"+method.getName()+
					"("+string+") of type "+methodParameterClassList[0].getName());
			System.out.flush();
			try
			{
				method.invoke(object,argumentList);
			}
			catch(IllegalAccessException e)
			{
				System.err.println("callMethod:method invoke failed for:"+
					method.getName()+":for paramater string:"+string+":"+e);
			}
			catch(IllegalArgumentException e)
			{
				System.err.println("callMethod:method invoke failed for:"+
					method.getName()+":for paramater string:"+string+":"+e);
			}
			catch(InvocationTargetException e)
			{
				System.err.println("callMethod:method invoke failed for:"+
					method.getName()+":for paramater string:"+string+":"+e);
			}
		}// end if methodParameterClassList parameters have 1 parameter.
		else
		{
			System.err.println("callMethod:"+method.getName()+":methodParameterClassList has:"+
				methodParameterClassList.length+":parameters.");
		}
	}

	/**
	 * Create a CCDConfig object from the supplied string.
	 * @param string Config string.
	 * @return Return a valid CCDConfig.
	 */
	private CCDConfig createCCDConfig(String string)
	{
		CCDConfig ccdConfig = null;
		CCDDetector detector = null;
		Window windowArray[];
		String configName = null;
		String parameterListString = null;
		StringTokenizer st = null;
		String parameterString =  null;
		int sindex,eindex,num;

	// get configName and  parameterListString list of parameters
		sindex = string.indexOf('{');
		if(sindex < 0)
		{
			configName = new String(string);
			parameterListString = new String("");
		}
		else
		{
			configName = string.substring(0,sindex);
			eindex = string.indexOf('}');
			if(eindex < 0)
				parameterListString = string.substring(sindex+1,string.length());
			else
				parameterListString = string.substring(sindex+1,eindex);
		}
	// start setting up return object
		System.out.println("createCCDConfig:setId("+configName+")");
		System.out.flush();
	// setup CCDConfig
		ccdConfig = new CCDConfig(configName);
		ccdConfig.setId(configName);
	// detector for config
		detector = new CCDDetector();
	// no windows generated
		windowArray = new Window[detector.getMaxWindowCount()];
		for(int i = 0; i < detector.getMaxWindowCount(); i++)
			windowArray[i] = null;
		detector.setWindows(windowArray);
		detector.setWindowFlags(0);
		ccdConfig.setDetector(0,detector);
	// tokenize individual parameters from list
		st = new StringTokenizer(parameterListString,":");
	// setXbin
		if(st.hasMoreTokens())
		{
			try
			{
				parameterString = st.nextToken();
				num = Integer.parseInt(parameterString);
			}
			catch(NumberFormatException e)
			{
				System.err.println(this.getClass().getName()+
					":createCCDConfig:setXbin not a valid number:"+parameterString+":"+e);
				num = 1;
			}
		}
		else
			num = 1;
		System.out.println("createCCDConfig:setXbin("+num+")");
		System.out.flush();
		detector.setXBin(num);
	// setYbin
		if(st.hasMoreTokens())
		{
			try
			{
				parameterString = st.nextToken();
				num = Integer.parseInt(parameterString);
			}
			catch(NumberFormatException e)
			{
				System.err.println(this.getClass().getName()+
					":createCCDConfig:setYbin not a valid number:"+parameterString+":"+e);
				num = 1;
			}
		}
		else
			num = 1;
		System.out.println("createCCDConfig:setYbin("+num+")");
		System.out.flush();
		detector.setYBin(num);
	// setLowerFilterWheel
		if(st.hasMoreTokens())
			parameterString = st.nextToken();
		else
			parameterString = new String("clear");
		System.out.println("createCCDConfig:setLowerFilterWheel("+parameterString+")");
		System.out.flush();
		ccdConfig.setLowerFilterWheel(parameterString);
	// setUpperFilterWheel
		if(st.hasMoreTokens())
			parameterString = st.nextToken();
		else
			parameterString = new String("neutral");
		System.out.println("createCCDConfig:setUpperFilterWheel("+parameterString+")");
		System.out.flush();
		ccdConfig.setUpperFilterWheel(parameterString);
		return ccdConfig;
	}

	/**
	 * Routine to be called at the end of execution of MakeISSCommandFile to close the file.
	 */
	private void close()
	{
		if(outputStream == null)
			return;
		try
		{
			outputStream.flush();
		}
		catch(IOException e)
		{
			System.err.println("close:flush:"+e);
		}
		try
		{
			outputStream.close();
		}
		catch(IOException e)
		{
			System.err.println("close:close:"+e);
		}
	}


	/**
	 * This routine parses arguments passed into MakeISSCommandFile. Any paramaters not prefiexed
	 * with a switch get added to the commandList.
	 * @see #filename
	 * @see #commandList
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
			else if(args[i].equals("-h")||args[i].equals("-help"))
			{
				System.out.println(this.getClass().getName()+" Help:");
				System.out.println("java "+this.getClass().getName()+" [Options] <object list>");
				System.out.println("Options are:");
				System.out.println("\t-f[ile] <filename> - filename to save to.");
				System.exit(0);
			}
			else
				commandList.addElement(new String(args[i]));
		}
	}

	/**
	 * The main routine, called when MakeISSCommandFile is executed.
	 * @see #parseArgs
	 * @see #open
	 * @see #run
	 * @see #close
	 */
	public static void main(String[] args)
	{
		MakeISSCommandFile micf = new MakeISSCommandFile();
		micf.init();
		micf.parseArgs(args);
		if(micf.filename == null)
		{
			System.err.println("No Filename Specified.");
			System.exit(1);
		}
		if(micf.commandList.size() < 1)
		{
			System.err.println("No Commands Specified.");
			System.exit(1);
		}
		micf.open();
		micf.run();
		micf.close();
		System.exit(0);
	}
}

// $Log: not supported by cvs2svn $
// Revision 1.15  2006/05/16 16:54:42  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.14  2003/04/02 19:40:14  cjm
// Added long paramater parsing.
//
// Revision 1.13  2002/12/16 17:08:45  cjm
// *** empty log message ***
//
// Revision 1.12  2001/02/27 15:59:55  cjm
// Updated for new format of CONFIG message.
//
// Revision 1.11  1999/09/09 12:37:07  cjm
// Changed setting of CCDConfig, upper and lower filter wheels from char to String.
//
// Revision 1.10  1999/09/07 15:15:18  cjm
// Changed output to System.out, errors still reported on System.err.
//
// Revision 1.9  1999/09/07 14:51:02  cjm
// callMethod now supports set methods of type boolean. This is initially needed for the pipelineProcess flag in the EXPOSE class.
//
// Revision 1.8  1999/09/07 12:39:54  cjm
// setProposal method removed from ngat.phase2.CCDConfig.
//
// Revision 1.7  1999/07/16 15:18:46  cjm
// Caught NumberFormatException in callMethod for Integer and Float.
//
// Revision 1.6  1999/07/09 09:29:56  dev
// Added CONFIG parameter support for CCDConfig class.
// Enables specification of Binning and filter wheel.
//
// Revision 1.5  1999/06/07 16:55:37  dev
// CCDConfig object improvements
//
// Revision 1.4  1999/05/28 09:54:34  dev
// "Name
//
// Revision 1.3  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.2  1999/04/27 11:26:51  dev
// Backup
//
// Revision 1.1  1999/03/19 11:51:09  dev
// Backup
//
