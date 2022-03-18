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
// TESTImplementation.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/TESTImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $

import java.lang.*;
import ngat.rise.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.TEST;
import ngat.message.ISS_INST.TEST_DONE;

/**
 * This class provides the implementation for the TEST command sent to a server using the
 * Java Message System. It extends SETUPImplementation. 
 * <br>This command causes the instrument to carry out self-test routines 
 * <br>Note, the implementation for this command has not yet been done.
 * @see SETUPImplementation
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class TESTImplementation extends SETUPImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: TESTImplementation.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");
	/**
	 * Bit-field constant for when the level parameter specifies a quick test.
	 */
	public final static int TEST_LEVEL_QUICK_TEST = (1<<0);
	/**
	 * Bit-field constant for when the level parameter specifies a software test.
	 */
	public final static int TEST_LEVEL_SOFTWARE = (1<<1);
	/**
	 * Bit-field constant for when the level parameter specifies a hardware test.
	 */
	public final static int TEST_LEVEL_HARDWARE = (1<<2);
	/**
	 * Number of tests to perform on each board.
	 */
	public final static int HARDWARE_TEST_COUNT = 1000;
	/**
	 * Number of boards to perform tests on.
	 */
	public final static int BOARD_COUNT = 3;
	/**
	 * Amount of time, in milliseconds, to perform one hardware link test.
	 */
	public final static long TIME_PER_TEST = 1;

	/**
	 * Constructor.
	 */
	public TESTImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.TEST&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.TEST";
	}

	/**
	 * This method gets the TEST command's acknowledge time.
	 * <br>This command causes the instrument to carry out self-test routines.
	 * The acknowledge time is the default acknowledge time, plaus a time depending on the 
	 * level of test to perform:
	 * <ul>
	 * <li>TEST_LEVEL_QUICK_TEST. This takes HARDWARE_TEST_COUNT*BOARD_COUNT*TIME_PER_TEST to perform.
	 * </ul> 
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see #TEST_LEVEL_QUICK_TEST
	 * @see #HARDWARE_TEST_COUNT
	 * @see #BOARD_COUNT
	 * @see #TIME_PER_TEST
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		TEST testCommand = (TEST)command;
		ACK acknowledge = null;
		int acknowledgeTime = 0;
		int level = 0;

		level = testCommand.getLevel();
		acknowledge = new ACK(command.getId());
		acknowledgeTime = serverConnectionThread.getDefaultAcknowledgeTime();
		if((level&TEST_LEVEL_QUICK_TEST) != 0)
			acknowledgeTime += HARDWARE_TEST_COUNT*BOARD_COUNT*TIME_PER_TEST;
		acknowledge.setTimeToComplete(acknowledgeTime);
		return acknowledge;
	}

	/**
	 * This method implements the TEST command. 
	 * The implementation depends on the level of test requested:
	 * <ul>
	 * <li>TEST_LEVEL_QUICK_TEST. A Setup Hardware Test is performed, which tests the data links to
	 * 	three boards on the controller (PCI/timing/utility) HARDWARE_TEST_COUNT times.
	 * </ul>
	 * Not all levels of TEST have been implemented.
	 * @param command The command, of class TEST, to perform.
	 * @return An object of class TEST_DONE is returned.
	 * @see #TEST_LEVEL_QUICK_TEST
	 * @see #HARDWARE_TEST_COUNT
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		TEST testCommand = (TEST)command;
		TEST_DONE testDone = new TEST_DONE(command.getId());
		int level = 0;

		level = testCommand.getLevel();
		if((level&TEST_LEVEL_QUICK_TEST) != 0)
		{
		// Test the data link.
			try
			{
				libccd.CCDSetupHardwareTest(HARDWARE_TEST_COUNT);
			}
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+":processCommand:"+
					command+":"+e);
				testDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+2100);
				testDone.setErrorString(e.toString());
				testDone.setSuccessful(false);
				return testDone;
			}
		}
		testDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		testDone.setErrorString("");
		testDone.setSuccessful(true);
	// return done object.
		return testDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.10  2006/05/16 14:26:09  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.9  2001/07/03 15:13:21  cjm
// Changed error code to include Ccs sub-system offset.
//
// Revision 0.8  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.7  2001/02/05 17:05:29  cjm
// Tidying.
//
// Revision 0.6  2000/03/02 17:51:58  cjm
// QUICK_TEST implementation using CCDSetupHardwareTest.
//
// Revision 0.5  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.4  1999/11/01 15:53:41  cjm
// Changed calculateAcknowledgeTime to return ACK rather than an int.
// This is to keep up to date with the changes to ngat.net.TCPServerConnectionThread class.
//
// Revision 0.3  1999/11/01 10:45:51  cjm
// Got rid of init methods that just called super-class's method.
// Added constructor to setup implement string correctly.
//
// Revision 0.2  1999/10/27 16:47:25  cjm
// Changed definition of RCSID so that file Ids are picked up properly.
//
// Revision 0.1  1999/10/27 16:25:54  cjm
// initial revision.
//
//
