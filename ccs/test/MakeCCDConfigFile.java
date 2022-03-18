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
// MakeCCDConfigFile.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/test/MakeCCDConfigFile.java,v 1.1 2009-10-15 10:19:32 cjm Exp $

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
 * @version $Revision: 1.1 $
 */
public class MakeCCDConfigFile
{
	/**
	 * The filename to save commands to.
	 */
	private String filename = null;
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
	 * has a CCDConfig phase2 object with it, this is created and it's fields initialised.
	 * @see #outputStream
	 */
	private void run()
	{
		String string = null;
		CONFIG configCommand = null;
		CCDConfig ccdConfig = null;
		CCDDetector detector = null;
		Window windowArray[];
		int xs,ys,width = 100,height = 100;

		if(outputStream ==null)
			return;
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
		detector.setWindowFlags(15);
		ccdConfig.setDetector(0,detector);
		ccdConfig.setLowerFilterWheel("SDSS-U");
		ccdConfig.setUpperFilterWheel("clear");
		detector.setXBin(1);
		detector.setYBin(1);
	// InstrumentConfig fields.
		configCommand.setConfig(ccdConfig);
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
	 * Routine to be called at the end of execution of MakeCCDConfigFile to close the file.
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
	 * This routine parses arguments passed into MakeCCDConfigFile.
	 * @see #filename
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
				System.out.println("Options are:");
				System.out.println("\t-f[ile] <filename> - filename to save to.");
				System.exit(0);
			}
		}
	}

	/**
	 * The main routine, called when MakeCCDConfigFile is executed.
	 * @see #parseArgs
	 * @see #open
	 * @see #run
	 * @see #close
	 */
	public static void main(String[] args)
	{
		MakeCCDConfigFile mccf = new MakeCCDConfigFile();
		mccf.init();
		mccf.parseArgs(args);
		if(mccf.filename == null)
		{
			System.err.println("No Filename Specified.");
			System.exit(1);
		}
		mccf.open();
		mccf.run();
		mccf.close();
		System.exit(0);
	}
}

// $Log: not supported by cvs2svn $
// Revision 1.7  2006/05/16 16:54:41  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.6  2002/12/16 17:08:45  cjm
// Changed NPCCDConfig to CCDConfig.
//
// Revision 1.5  2001/02/27 16:00:42  cjm
// New format of CCDConfig.
//
// Revision 1.4  1999/09/09 12:37:06  cjm
// Changed setting of CCDConfig, upper and lower filter wheels from char to String.
//
// Revision 1.3  1999/09/07 12:42:39  cjm
// setProposal method no longer exists in ngat.phase2.CCDConfig class.
//
// Revision 1.2  1999/05/20 16:38:13  dev
// "Backup"
//
// Revision 1.1  1999/05/10 15:57:41  dev
// "Backup"
//
