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
// CCDLibraryDouble.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ngat/rise/ccd/CCDLibraryDouble.java,v 1.1 2009-10-15 10:23:09 cjm Exp $
package ngat.rise.ccd;

/**
 * This class is a simple double data type object wrapper, allowing us to return a double value from a 
 * method..
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CCDLibraryDouble
{
	/**
	 * The variable used to hold the double value.
	 */
	private double value;
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: CCDLibraryDouble.java,v 1.1 2009-10-15 10:23:09 cjm Exp $");

	/**
	 * This method sets the value of the double.
	 * @param value The value the double is set to.
	 */
	public void setValue(double value)
	{
		this.value = value;
	}

	/**
	 * This method gets the value of the double.
	 * @return The current value of the double.
	 */
	public double getValue()
	{
		return value;
	}
}
 
//
// $Log: not supported by cvs2svn $
// Revision 0.6  2006/05/16 17:41:32  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.5  1999/09/13 13:56:02  cjm
// Class is now public.
//
// Revision 0.4  1999/09/10 15:32:56  cjm
// Changed package to ngat.ccd.
//
// Revision 0.3  1999/09/08 10:52:40  cjm
// Trying to fix file permissions of these files.
//
// Revision 0.2  1999/02/23 11:08:00  dev
// backup/transfer to ltccd1.
//
// Revision 0.1  1999/01/21 15:45:18  dev
// initial revision
//
//
