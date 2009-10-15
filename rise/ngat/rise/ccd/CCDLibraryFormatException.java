/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of NGAT.

    NGAT is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    NGAT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NGAT; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
// CCDLibraryFormatException.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ngat/rise/ccd/CCDLibraryFormatException.java,v 1.1 2009-10-15 10:23:09 cjm Exp $
package ngat.rise.ccd;

/**
 * This class extends java.lang.IllegalArgumentException. Objects of this class are thrown when an illegal
 * format argument is passed into various parse routines in CCDLibrary.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CCDLibraryFormatException extends IllegalArgumentException
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: CCDLibraryFormatException.java,v 1.1 2009-10-15 10:23:09 cjm Exp $");

	/**
	 * Constructor for the exception.
	 * @param fromClassName The name of the class the exception occured in.
	 * @param methodName The name of the method the exception occured in.
	 * @param illegalParameter The illegal string that could not be parsed by the method.
	 */
	public CCDLibraryFormatException(String fromClassName,String methodName,String illegalParameter)
	{
		super("CCDLibraryFormatException:"+fromClassName+":"+methodName+":Illegal Parameter:"+
			illegalParameter);
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 0.5  2006/05/16 17:41:33  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.4  1999/09/13 13:56:34  cjm
// Class is now public.
//
// Revision 0.3  1999/09/10 15:33:29  cjm
// Changed package to ngat.ccd.
//
// Revision 0.2  1999/06/07 16:56:41  dev
// String to Number parse routines
//
// Revision 0.1  1999/06/07 10:01:11  dev
// initial revision
//
//
