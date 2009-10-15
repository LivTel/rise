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
// CCDLibraryNativeException.java -*- mode: Fundamental;-*-
// $Header: /space/home/eng/cjm/cvs/rise/ngat/rise/ccd/CCDLibraryNativeException.java,v 1.1 2009-10-15 10:23:09 cjm Exp $
package ngat.rise.ccd;

/**
 * This class extends Exception. Objects of this class are thrown when the underlying C code in CCDLibrary produces an
 * error. The individual parts of the error generated are stored in the exception as well as the complete message.
 * The JNI interface itself can also generate these exceptions.
 * @author Chris Mottram
 * @version $Revision: 1.1 $
 */
public class CCDLibraryNativeException extends Exception
{
	/**
	 * Revision Control System id string, showing the version of the Class
	 */
	public final static String RCSID = new String("$Id: CCDLibraryNativeException.java,v 1.1 2009-10-15 10:23:09 cjm Exp $");
	/**
	 * A type of error that can cause this exception to be created. This type is when the error type
	 * is unknown.
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_NATIVE
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_JNI
	 */
	public final static int CCD_NATIVE_EXCEPTION_TYPE_NONE = 0;
	/**
	 * A type of error that can cause this exception to be created. This type is for errors created
	 * in the native C library code.
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_JNI
	 */
	public final static int CCD_NATIVE_EXCEPTION_TYPE_NATIVE = 1;
	/**
	 * A type of error that can cause this exception to be created. This type is for errors created
	 * by the JNI Java Native Interface functions.
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_NATIVE
	 */
	public final static int CCD_NATIVE_EXCEPTION_TYPE_JNI = 2;
	/**
	 * String representation of the possible error types.
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_NONE
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_NATIVE
	 * @see #CCD_NATIVE_EXCEPTION_TYPE_JNI
	 */
	private String errorTypeString[] = {"None","Native","JNI"};
	/**
	 * The error string supplied to the exception.
	 */
	private String errorString = null;
	/**
	 * The type of error that caused this exception to be created. This is one of
	 * CCD_NATIVE_EXCEPTION_TYPE_NATIVE or CCD_NATIVE_EXCEPTION_TYPE_JNI, depending whether the error
	 * occured in the native code or the JNI interface code.
	 */
	private int errorType = CCD_NATIVE_EXCEPTION_TYPE_NONE;
	/**
	 * The current value of the error number in the DSP module.
	 */
	protected int DSPErrorNumber = 0;
	/**
	 * The current value of the error number in the Exposure module.
	 */
	protected int exposureErrorNumber = 0;
	/**
	 * The current value of the error number in the filter wheel module.
	 */
	protected int filterWheelErrorNumber = 0;
	/**
	 * The current value of the error number in the Interface module.
	 */
	protected int interfaceErrorNumber = 0;
	/**
	 * The current value of the error number in the PCI module.
	 */
	protected int PCIErrorNumber = 0;
	/**
	 * The current value of the error number in the Setup module.
	 */
	protected int setupErrorNumber = 0;
	/**
	 * The current value of the error number in the Temperature module.
	 */
	protected int temperatureErrorNumber = 0;
	/**
	 * The current value of the error number in the Text module.
	 */
	protected int textErrorNumber = 0;

	/**
	 * Constructor for the exception.
	 * @param errorString The error string.
	 */
	public CCDLibraryNativeException(String errorString)
	{
		super(errorString);
		this.errorString = new String(errorString);
		this.errorType = CCD_NATIVE_EXCEPTION_TYPE_NONE;
	}

	/**
	 * Constructor for the exception. The exception is assumed to be of type CCD_NATIVE_EXCEPTION_TYPE_NATIVE.
	 * The error number fields of the created exception are filled with error numbers retrieved using
	 * JNI calls to the libccd instance passed in.
	 * @param errorString The error string.
	 * @param libccd The instance of CCDLibrary that threw this exception.
	 * @see #errorString
	 * @see #errorType
	 * @see #DSPErrorNumber
	 * @see #exposureErrorNumber
	 * @see #filterWheelErrorNumber
	 * @see #interfaceErrorNumber
	 * @see #PCIErrorNumber
	 * @see #setupErrorNumber
	 * @see #temperatureErrorNumber
	 * @see #textErrorNumber
	 * @see CCDLibrary#CCDDSPGetErrorNumber
	 * @see CCDLibrary#CCDExposureGetErrorNumber
	 * @see CCDLibrary#CCDFilterWheelGetErrorNumber
	 * @see CCDLibrary#CCDInterfaceGetErrorNumber
	 * @see CCDLibrary#CCDPCIGetErrorNumber
	 * @see CCDLibrary#CCDSetupGetErrorNumber
	 * @see CCDLibrary#CCDTemperatureGetErrorNumber
	 * @see CCDLibrary#CCDTextGetErrorNumber
	 */
	public CCDLibraryNativeException(String errorString,CCDLibrary libccd)
	{
		super(errorString);
		this.errorString = new String(errorString);
		this.errorType = CCD_NATIVE_EXCEPTION_TYPE_NATIVE;
		this.DSPErrorNumber = libccd.CCDDSPGetErrorNumber();
		this.exposureErrorNumber = libccd.CCDExposureGetErrorNumber();
		this.filterWheelErrorNumber = libccd.CCDFilterWheelGetErrorNumber();
		this.interfaceErrorNumber = libccd.CCDInterfaceGetErrorNumber();
		this.PCIErrorNumber = libccd.CCDPCIGetErrorNumber();
		this.setupErrorNumber = libccd.CCDSetupGetErrorNumber();
		this.temperatureErrorNumber = libccd.CCDTemperatureGetErrorNumber();
		this.textErrorNumber = libccd.CCDTextGetErrorNumber();
	}

	/**
	 * Constructor for the exception.
	 * @param errorString The error string.
	 * @param errorType The type of error that caused the exception to be raised. One of
	 * CCD_NATIVE_EXCEPTION_TYPE_NATIVE or CCD_NATIVE_EXCEPTION_TYPE_JNI.
	 */
	public CCDLibraryNativeException(String errorString,int errorType)
	{
		super(errorString);
		this.errorString = new String(errorString);
		if(isErrorType(errorType))
			this.errorType = errorType;
		else
			this.errorType = CCD_NATIVE_EXCEPTION_TYPE_NONE;
	}

	/**
	 * Class routine to create the exception from the actual values in the CCDLibrary. Is uses the 
	 * CCDLibrary routine CCDErrorString to create the error string.
	 * @param libccd The instance of the CCDLibrary class we are getting the error string from.
	 * @return Returns the created exception.
	 * @see CCDLibrary#CCDErrorString
	 */
	public static CCDLibraryNativeException createNativeException(CCDLibrary libccd)
	{
		return new CCDLibraryNativeException(libccd.CCDErrorString(),
			CCDLibraryNativeException.CCD_NATIVE_EXCEPTION_TYPE_NATIVE);
	}

	/**
	 * Class routine to create a JNI related exception.
	 * @param s The error string.
	 * @return Returns the created exception.
	 */
	public static CCDLibraryNativeException createJNIException(String s)
	{
		return new CCDLibraryNativeException(s,CCDLibraryNativeException.CCD_NATIVE_EXCEPTION_TYPE_JNI);
	}

	/**
	 * Retrieve routine for the error string supplied to the exception.
	 * @return Returns a copy of the errorString for this exception.
	 * @see #errorString
	 */
	public String getErrorString()
	{
		return new String(errorString);
	}

	/**
	 * Retrieve routine for the error type supplied to the exception.
	 * @return Returns the error type number supplied for this exception.
	 * @see #errorType
	 */
	public int getErrorType()
	{
		return errorType;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #DSPErrorNumber
	 */
	public int getDSPErrorNumber()
	{
		return DSPErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #exposureErrorNumber
	 */
	public int getExposureErrorNumber()
	{
		return exposureErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #filterWheelErrorNumber
	 */
	public int getFilterWheelErrorNumber()
	{
		return filterWheelErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #interfaceErrorNumber
	 */
	public int getInterfaceErrorNumber()
	{
		return interfaceErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #PCIErrorNumber
	 */
	public int getPCIErrorNumber()
	{
		return PCIErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #setupErrorNumber
	 */
	public int getSetupErrorNumber()
	{
		return setupErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #temperatureErrorNumber
	 */
	public int getTemperatureErrorNumber()
	{
		return temperatureErrorNumber;
	}

	/**
	 * Retrieve routine for the error number for the relevant C module.
	 * @return Returns the error number supplied for this exception, 
	 * 	if the number was supplied in a constructor.
	 * @see #textErrorNumber
	 */
	public int getTextErrorNumber()
	{
		return textErrorNumber;
	}

	/**
	 * Routine to get a string representation of the error type supplied to the exception.
	 * @return Returns a string, describing the error type of this exception.
	 * @see #errorType
	 * @see #errorTypeString
	 */
	public String getErrorTypeString()
	{
		return new String(errorTypeString[errorType]);
	}

	/**
	 * Private check routine for the error type supplied to the exception.
	 * @param errorType An integer to be checked to see it if is a legal error type.
	 * @return Returns a boolean, true if the supplied number is a legal error type and false otherwise.
	 * @see #errorType
	 */
	private boolean isErrorType(int errorType)
	{
		return ((errorType == CCD_NATIVE_EXCEPTION_TYPE_NONE)||
			(errorType == CCD_NATIVE_EXCEPTION_TYPE_NATIVE)||
			(errorType == CCD_NATIVE_EXCEPTION_TYPE_JNI));
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.4  2006/05/16 17:41:34  cjm
// gnuify: Added GNU General Public License.
//
// Revision 1.3  2001/04/05 16:50:46  cjm
// Added new constructor and local copies of libccd error codes,
// so that exceptions can contain gettable error codes as integers.
//
// Revision 1.2  1999/09/23 10:45:49  cjm
// Changed message going into super constructor.
//
// Revision 1.1  1999/09/20 14:40:08  cjm
// Initial revision
//
//
