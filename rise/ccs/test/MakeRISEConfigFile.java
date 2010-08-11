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
// MakeRISEConfigFile.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/MakeRISEConfigFile.java,v 1.2 2010-08-11 15:10:07 cjm Exp $

import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.*;

import ngat.message.ISS_INST.*;
import ngat.phase2.*;
/**
 * This class is allows us to make a serialized object file, containing an ngat.message.ISS_INST.CONFIG
 * Message, which is used to send configurations between the ISS and the Instrument. This program was
 * written as a special case so that the ngat.phase2.CCDConfig object could be filled in to test the impact on
 * performance.
 * @author Chris Mottram
 * @version $Revision: 1.2 $
 */
public class MakeRISEConfigFile
{
	/**
	 * The filename to save commands to.
	 */
	private String filename = null;
	/**
	 * The CCD detector binning to use. Defaults to 1.
	 */
	private int bin = 1;
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
	 * This is the run routine. It creates a CONFIG command and writes it to the outputStream. This object
	 * has a RISEConfig phase2 object with it, this is created and it's fields initialised.
	 * @see #outputStream
	 * @see #bin
	 */
	private void run()
	{
		String string = null;
		CONFIG configCommand = null;
		RISEConfig riseConfig = null;
		RISEDetector detector = null;
		Window windowArray[];
		int xs,ys,width = 100,height = 100;

		if(outputStream ==null)
			return;
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
		try
		{
			outputStream.writeObject(configCommand);
		}
		catch(IOException e)
		{
			System.err.println("run:Writing object:"+configCommand+":failed.");
		}
	}

	/**
	 * Routine to be called at the end of execution of MakeRISEConfigFile to close the file.
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
	 * This routine parses arguments passed into MakeRISEConfigFile.
	 * @see #filename
	 * @see #bin
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
					errorStream.println("-filename requires a filename");
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
				System.out.println(this.getClass().getName()+" Help:");
				System.out.println("Options are:");
				System.out.println("\t-f[ile] <filename> - filename to save to.");
				System.out.println("\t-b[inning] <n> - CCD binning factor.");
				System.exit(0);
			}
		}
	}

	/**
	 * The main routine, called when MakeRISEConfigFile is executed.
	 * @see #parseArgs
	 * @see #open
	 * @see #run
	 * @see #close
	 */
	public static void main(String[] args)
	{
		MakeRISEConfigFile mrcf = new MakeRISEConfigFile();
		mrcf.init();
		mrcf.parseArgs(args);
		if(mrcf.filename == null)
		{
			System.err.println("No Filename Specified.");
			System.exit(1);
		}
		mrcf.open();
		mrcf.run();
		mrcf.close();
		System.exit(0);
	}
}
//
// $Log: not supported by cvs2svn $
// Revision 1.1  2009/10/15 10:19:32  cjm
// Initial revision
//
//
