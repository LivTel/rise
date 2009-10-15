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
// CcsConstants.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/CcsConstants.java,v 1.1 2009-10-15 10:21:18 cjm Exp $
import java.lang.*;
import java.io.*;

/**
 * This class holds some constant values for the Ccs program. 
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CcsConstants
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: CcsConstants.java,v 1.1 2009-10-15 10:21:18 cjm Exp $");

	/**
	 * Error code. No error.
	 */
	public final static int CCS_ERROR_CODE_NO_ERROR 		= 0;
	/**
	 * The base Error number, for all Ccs error codes. 
	 */
	public final static int CCS_ERROR_CODE_BASE 			= 100000;

	/**
	 * Logging level. Don't do any logging.
	 */
	public final static int CCS_LOG_LEVEL_NONE 			= 0;
	/**
	 * Logging level. Log Commands messages received/sent.
	 */
	public final static int CCS_LOG_LEVEL_COMMANDS 			= (1<<0);
	/**
	 * Logging level. Log Commands message replies received/sent.
	 */
	public final static int CCS_LOG_LEVEL_REPLIES 			= (1<<1);
	/**
	 * Logging level. Log acknowledgement messages received/sent.
	 */
	public final static int CCS_LOG_LEVEL_ACKS 			= (1<<2);
	/**
	 * Logging level. Extra TELFOCUS logging, on intermediate/accuracy (chi-squared) values
	 * for the quadratic fitting process.
	 */
	public final static int CCS_LOG_LEVEL_TELFOCUS 			= (1<<3);
	/**
	 * Logging level. Extra DAY_CALIBRATE logging.
	 */
	public final static int CCS_LOG_LEVEL_DAY_CALIBRATE 		= (1<<4);
	/**
	 * Logging level. Extra TWILIGHT_CALIBRATE logging.
	 */
	public final static int CCS_LOG_LEVEL_TWILIGHT_CALIBRATE 	= (1<<5);
	/**
	 * Logging level. Extra FITS header related logging.
	 */
	public final static int CCS_LOG_LEVEL_FITS 	                = (1<<6);
	/**
	 * Logging level. Log if any logging is turned on.
	 */
	public final static int CCS_LOG_LEVEL_ALL 			= (CCS_LOG_LEVEL_COMMANDS|
		CCS_LOG_LEVEL_REPLIES|CCS_LOG_LEVEL_ACKS|CCS_LOG_LEVEL_TELFOCUS|CCS_LOG_LEVEL_DAY_CALIBRATE|
		CCS_LOG_LEVEL_TWILIGHT_CALIBRATE|CCS_LOG_LEVEL_FITS);
	/**
	 * Logging level used by the error logger. We want to log all errors,
	 * hence this value should be used for all errors.
	 */
	public final static int CCS_LOG_LEVEL_ERROR			= 1;

	/**
	 * Default thread priority level. This is for the server thread. Currently this has the highest priority,
	 * so that new connections are always immediately accepted.
	 * This number is the default for the <b>ccs.thread.priority.server</b> property, if it does not exist.
	 */
	public final static int CCS_DEFAULT_THREAD_PRIORITY_SERVER		= Thread.NORM_PRIORITY+2;
	/**
	 * Default thread priority level. 
	 * This is for server connection threads dealing with sub-classes of the INTERRUPT
	 * class. Currently these have a higher priority than other server connection threads,
	 * so that INTERRUPT commands are always responded to even when another command is being dealt with.
	 * This number is the default for the <b>ccs.thread.priority.interrupt</b> property, if it does not exist.
	 */
	public final static int CCS_DEFAULT_THREAD_PRIORITY_INTERRUPT		= Thread.NORM_PRIORITY+1;
	/**
	 * Default thread priority level. This is for most server connection threads. 
	 * Currently this has a normal priority.
	 * This number is the default for the <b>ccs.thread.priority.normal</b> property, if it does not exist.
	 */
	public final static int CCS_DEFAULT_THREAD_PRIORITY_NORMAL		= Thread.NORM_PRIORITY;
	/**
	 * Default thread priority level. This is for the Telescope Image Transfer server/client threads. 
	 * Currently this has the lowest priority, so that the camera control is not interrupted by image
	 * transfer requests.
	 * This number is the default for the <b>ccs.thread.priority.tit</b> property, if it does not exist.
	 */
	public final static int CCS_DEFAULT_THREAD_PRIORITY_TIT			= Thread.MIN_PRIORITY;
}

// $Log: not supported by cvs2svn $
// Revision 0.25  2006/05/16 14:25:41  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.24  2005/09/29 14:04:55  cjm
// Added CCS_LOG_LEVEL_FITS.
//
// Revision 0.23  2004/11/23 15:24:08  cjm
// Added CCS_LOG_LEVEL_ACKS.
//
// Revision 0.22  2001/08/17 17:50:37  cjm
// Added DAY_CALIBRATE and TWILIGHT_CALIBRATE log levels.
//
// Revision 0.21  2001/07/03 16:32:31  cjm
// Added CCS_DEFAULT_THREAD_PRIORITY_TIT and CCS_ERROR_CODE_BASE.
//
// Revision 0.20  2001/04/05 16:57:45  cjm
// New logging using ngat.util.logging package.
//
// Revision 0.19  2001/03/01 15:15:49  cjm
// Removed error constants.
//
// Revision 0.18  2000/08/25 10:18:15  cjm
// Added CCS_LOG_LEVEL_TELFOCUS constant.
//
// Revision 0.17  2000/08/10 15:51:35  cjm
// Added  CCS_ERROR_CODE_TELFOCUS.
//
// Revision 0.16  2000/07/10 14:50:22  cjm
// Thread priority constant:NORM to NORMAL.
//
// Revision 0.15  2000/07/10 14:39:18  cjm
// Thread priority constants now defaults.
//
// Revision 0.14  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.13  2000/03/08 09:52:22  cjm
// Added CCS_ERROR_CODE_PAUSE/CCS_ERROR_CODE_RESUME error codes.
//
// Revision 0.12  2000/03/02 17:27:03  cjm
// Added CCS_ERROR_CODE_TEST.
//
// Revision 0.11  2000/02/07 11:23:53  cjm
// Added CCS_ERROR_CODE_REBOOT.
//
// Revision 0.10  1999/11/24 11:02:16  cjm
// Added CCS_ERROR_CODE_PROCESS_COMMAND error code.
//
// Revision 0.9  1999/10/28 10:35:34  cjm
// CCS_ERROR_CODE_UNKNOWN_COMMAND error code added.
//
// Revision 0.8  1999/09/09 16:05:11  cjm
// Added CCS_ERROR_CODE_SAVE error code.
//
// Revision 0.7  1999/07/05 11:10:50  dev
// New error code CCS_ERROR_CODE_FITS_HEADER_WRITE added.
//
// Revision 0.6  1999/07/01 13:39:58  dev
// Log level improved
//
// Revision 0.5  1999/06/24 12:40:20  dev
// "Backup"
//
// Revision 0.4  1999/06/09 16:52:36  dev
// thread abort procedure improvements and error/log file implementation
//
// Revision 0.3  1999/06/08 16:50:53  dev
// thread priority changes
//
// Revision 0.2  1999/06/07 16:54:59  dev
// properties file/more implementation
//
// Revision 0.1  1999/05/21 14:46:16  dev
// initial revision
//
