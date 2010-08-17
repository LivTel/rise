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
// MULTRUNImplementation.java
// $Header: /space/home/eng/cjm/cvs/rise/ccs/java/MULTRUNImplementation.java,v 1.4 2010-08-17 17:21:24 cjm Exp $

import java.lang.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import ngat.rise.ccd.*;
import ngat.fits.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.MULTRUN;
import ngat.message.ISS_INST.MULTRUN_ACK;
import ngat.message.ISS_INST.MULTRUN_DP_ACK;
import ngat.message.ISS_INST.MULTRUN_DONE;

/**
 * This class provides the implementation for the MULTRUN command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 1.4 $
 */
public class MULTRUNImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id: MULTRUNImplementation.java,v 1.4 2010-08-17 17:21:24 cjm Exp $");

	/**
	 * Constructor.
	 */
	public MULTRUNImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.MULTRUN&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.MULTRUN";
	}

	/**
	 * This method returns the MULTRUN command's acknowledge time. Each frame in the MULTRUN takes 
	 * the exposure time plus the default acknowledge time to complete. The default acknowledge time
	 * allows time to setup the camera, get information about the telescope and save the frame to disk.
	 * This method returns the time for the first frame in the MULTRUN only, as a MULTRUN_ACK message
	 * is returned to the client for each frame taken.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see CcsTCPServerConnectionThread#getDefaultAcknowledgeTime
	 * @see MULTRUN#getExposureTime
	 * @see MULTRUN#getNumberExposures
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		MULTRUN multRunCommand = (MULTRUN)command;
		ACK acknowledge = null;
		int time;

		acknowledge = new ACK(command.getId());
		time = status.getPropertyInteger("ccs.server_connection.multrun_acknowledge_time");
		acknowledge.setTimeToComplete(multRunCommand.getExposureTime()+
			time);
		return acknowledge;
	}

	/**
	 * This method implements the MULTRUN command. 
	 * <ul>
	 * <li>It moves the fold mirror to the correct location.
	 * <li>It starts the autoguider.
	 * <li>For each exposure it performs the following:
	 *	<ul>
	 * 	<li>It generates some FITS headers from the CCD setup and the ISS. 
	 * 	<li>Sets the time of exposure and saves the Fits headers.
	 * 	<li>It performs an exposure and saves the data from this to disc.
	 * 	<li>Keeps track of the generated filenames in the list.
	 * 	</ul>
	 * <li>It stops the autoguider.
	 * <li>It calls the Real Time Data Pipeline to reduce the data for each exposure taken.
	 * <li>It sets up the return values to return to the client.
	 * </ul>
	 * The resultant filename or the relevant error code is put into the an object of class MULTRUN_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see CommandImplementation#testAbort
	 * @see FITSImplementation#clearFitsHeaders
	 * @see FITSImplementation#setFitsHeaders
	 * @see FITSImplementation#getFitsHeadersFromISS
	 * @see FITSImplementation#saveFitsHeaders
	 * @see ngat.rise.ccd.CCDLibrary#CCDExposureExpose
	 * @see EXPOSEImplementation#reduceExpose
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		MULTRUN multRunCommand = (MULTRUN)command;
		MULTRUN_ACK multRunAck = null;
		MULTRUN_DP_ACK multRunDpAck = null;
		MULTRUN_DONE multRunDone = new MULTRUN_DONE(command.getId());
		CcsStatus status = null;
		String obsType = null;
		String filename = null;
		Vector filenameList = null;
		Vector reduceFilenameList = null;
	        Vector selectedHeaders = new Vector(); // Allocate an array for the headers IT
		int index;
		boolean retval = false;

		if(testAbort(multRunCommand,multRunDone) == true)
			return multRunDone;
	// get local status reference, setup exposure status.
		status = ccs.getStatus();
		status.setExposureCount(multRunCommand.getNumberExposures());
		status.setExposureNumber(0);
	// move the fold mirror to the correct location
		if(moveFold(multRunCommand,multRunDone) == false)
			return multRunDone;
	//	if(testAbort(multRunCommand,multRunDone) == true)
	//		return multRunDone;
	// setup filename object
		ccsFilename.nextMultRunNumber();
		ccs.error(this.getClass().getName()+": Begin Multrun with " + 
			multRunCommand.getNumberExposures()+ " exposures of " + 
			multRunCommand.getExposureTime() + " ms");
		try
		{
			if(multRunCommand.getStandard())
			{
				//ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_STANDARD);
				//obsType = FitsHeaderDefaults.OBSTYPE_VALUE_STANDARD;
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_SKY_FLAT);
				obsType = FitsHeaderDefaults.OBSTYPE_VALUE_SKY_FLAT;
			}
			else
			{
				ccsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_EXPOSURE);
				obsType = FitsHeaderDefaults.OBSTYPE_VALUE_EXPOSURE;
			}
		 }
		catch(Exception e)
		{
			ccs.error(this.getClass().getName()+
				  ":processCommand:"+command+":"+e.toString());
			multRunDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1204);
			multRunDone.setErrorString(e.toString());
			multRunDone.setSuccessful(false);
			return multRunDone;
		} /*
	// autoguider on
		if(autoguiderStart(multRunCommand,multRunDone) == false)
			return multRunDone;
		if(testAbort(multRunCommand,multRunDone) == true)
		{
			autoguiderStop(multRunCommand,multRunDone,false);
			return multRunDone;
		}
	// do exposures */
	// get fits headers
			clearFitsHeaders();
			if(setFitsHeaders(multRunCommand,multRunDone,obsType,
				multRunCommand.getExposureTime(),multRunCommand.getNumberExposures()) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
			if(getFitsHeadersFromISS(multRunCommand,multRunDone) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			} /*
		// save FITS headers 
			if(saveFitsHeaders(multRunCommand,multRunDone,filenameList) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}  */
			// Don't forget the toString() or the JVM will crash!!
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("RA"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("DEC"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LATITUDE").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LONGITUD").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBSTYPE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("AIRMASS").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELFOCUS").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ORIGIN"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("INSTATUS"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CONFIGID").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELESCOP"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELMODE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("LST"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CAT-RA"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CAT-DEC"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TELSTAT"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("AUTOGUID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTMODE"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTSKYPA").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WINDSPEE").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WMSTEMP").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("WMSHUMID").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBJECT"));
			
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("INSTRUME"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CONFNAME"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("DETECTOR"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GAIN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("READNOIS").toString());
		
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("TAGID"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("USERID"));
                        if(ccsFitsHeader.getKeywordValue("PROGID") == null)
			    selectedHeaders.addElement("UNKNOWN");
                        else
                            selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PROGID"));
                        if(ccsFitsHeader.getKeywordValue("PROPID") == null)
                            selectedHeaders.addElement("UNKNOWN");
                        else
                            selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PROPID"));
                        if(ccsFitsHeader.getKeywordValue("GROUPID") == null)
                            selectedHeaders.addElement("UNKNOWN");
                        else
                            selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GROUPID"));
                        if(ccsFitsHeader.getKeywordValue("OBSID") == null)
                            selectedHeaders.addElement("UNKNOWN");
                        else
                            selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("OBSID"));
			
			// New headers added at request of RJS (2008-11)
                        if(ccsFitsHeader.getKeywordValue("EXPTOTAL") == null)
                            selectedHeaders.addElement("UNKNOWN");
                        else
                            selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("EXPTOTAL").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("PRESCAN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POSTSCAN").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTCENTX").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTCENTY").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POICENTX").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("POICENTY").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("FILTERI1"));

			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("CCDSCALE").toString());
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("RADECSYS"));
			selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("EQUINOX").toString());

			// Additional headers. Nulls send during daytime obs, so check for null!
			//if(ccsFitsHeader.getKeywordValue("GRPNUMOB") != null)
			//    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNUMOB").toString());

			if(ccsFitsHeader.getKeywordValue("GRPTIMNG") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPTIMNG"));
			}

			if(ccsFitsHeader.getKeywordValue("GRPNUMOB") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNUMOB").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPUID") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPUID").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPNOMEX") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPNOMEX").toString());
			}

			if(ccsFitsHeader.getKeywordValue("GRPMONP") == null){
				 selectedHeaders.addElement("UNKNOWN");
			} else {
			    selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("GRPMONP").toString());
			}

			if(ccsFitsHeader.getKeywordValue("FILTER1") == null){
				selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("FILTER1"));
			}

			if(ccsFitsHeader.getKeywordValue("ROTANGLE") == null){
				selectedHeaders.addElement("UNKNOWN");
			} else {
				selectedHeaders.addElement(ccsFitsHeader.getKeywordValue("ROTANGLE").toString());
			}


			//for(int i=0; i<10; i++) { System.out.println(selectedHeaders.elementAt(i));}

			try {
			  if(multRunCommand.getStandard()) {	
				//Here, standard means "take a flat" - for now! 
				libccd.CCDMultflatExpose(true,-1,multRunCommand.getExposureTime(),multRunCommand.getNumberExposures(),selectedHeaders);
			  }
			else {
				libccd.CCDMultrunExpose(true,-1,multRunCommand.getExposureTime(),multRunCommand.getNumberExposures(),selectedHeaders);
			}
		
			} 
	/*
		index = 0;
		retval = true;
		reduceFilenameList = new Vector();
		while(retval&&(index < multRunCommand.getNumberExposures()))
		{
		// initialise list of FITS filenames for this frame
			filenameList = new Vector();
		// clear pause and resume times.
			status.clearPauseResumeTimes();
		// get a new filename.
			ccsFilename.nextRunNumber();
			filename = ccsFilename.getFilename();
// diddly window 1 only
		// get fits headers
			clearFitsHeaders();
			if(setFitsHeaders(multRunCommand,multRunDone,obsType,
				multRunCommand.getExposureTime(),multRunCommand.getNumberExposures()) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
			if(getFitsHeadersFromISS(multRunCommand,multRunDone) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
		// save FITS headers
			if(saveFitsHeaders(multRunCommand,multRunDone,filenameList) == false)
			{
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			}
		// do exposure.
// diddly window 1 only
			status.setExposureFilename(filename);
			try
			{
// diddly window 1 filename only
				libccd.CCDExposureExpose(true,-1,multRunCommand.getExposureTime(),filenameList);
			} */
			catch(CCDLibraryNativeException e)
			{
				ccs.error(this.getClass().getName()+
					":processCommand:"+command+":"+e.toString());
				multRunDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1201);
				multRunDone.setErrorString(e.toString());
				multRunDone.setSuccessful(false);
				autoguiderStop(multRunCommand,multRunDone,false);
				return multRunDone;
			} 
		// send acknowledge to say frame is completed.
			multRunAck = new MULTRUN_ACK(command.getId());
			multRunAck.setTimeToComplete(multRunCommand.getExposureTime()+
				serverConnectionThread.getDefaultAcknowledgeTime()); /*
// diddly window 1 filename only
			multRunAck.setFilename(filename);
			try
			{
				serverConnectionThread.sendAcknowledge(multRunAck);
			}
			catch(IOException e)
			{
				retval = false;
				ccs.error(this.getClass().getName()+
					":processCommand:sendAcknowledge:"+command+":"+e.toString());
				multRunDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1202);
				multRunDone.setErrorString(e.toString());
				multRunDone.setSuccessful(false);
				return multRunDone;
			}
			status.setExposureNumber(index+1);
		// add filename to list for data pipeline processing.
// diddly window 1 filename only
			reduceFilenameList.addAll(filenameList);
		// test whether an abort has occured.
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				retval = false;
			}
			index++;
		}
	// autoguider off
		if(autoguiderStop(multRunCommand,multRunDone,true) == false)
			return multRunDone;
	// if a failure occurs, return now
		if(!retval)
			return multRunDone;
		index = 0;
		retval = true;
	// call pipeline to process data and get results
		if(multRunCommand.getPipelineProcess())
		{
			while(retval&&(index < multRunCommand.getNumberExposures()))
			{
				filename = (String)reduceFilenameList.get(index);
			// do reduction.
				retval = reduceExpose(multRunCommand,multRunDone,filename);
			// send acknowledge to say frame has been reduced.
				multRunDpAck = new MULTRUN_DP_ACK(command.getId());
				multRunDpAck.setTimeToComplete(serverConnectionThread.getDefaultAcknowledgeTime());
			// copy Data Pipeline results from DONE to ACK
				multRunDpAck.setFilename(multRunDone.getFilename());
				multRunDpAck.setCounts(multRunDone.getCounts());
				multRunDpAck.setSeeing(multRunDone.getSeeing());
				multRunDpAck.setXpix(multRunDone.getXpix());
				multRunDpAck.setYpix(multRunDone.getYpix());
				multRunDpAck.setPhotometricity(multRunDone.getPhotometricity());
				multRunDpAck.setSkyBrightness(multRunDone.getSkyBrightness());
				multRunDpAck.setSaturation(multRunDone.getSaturation());
				try
				{
					serverConnectionThread.sendAcknowledge(multRunDpAck);
				}
				catch(IOException e)
				{
					retval = false;
					ccs.error(this.getClass().getName()+
						":processCommand:sendAcknowledge(DP):"+command+":"+e.toString());
					multRunDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_BASE+1203);
					multRunDone.setErrorString(e.toString());
					multRunDone.setSuccessful(false);
					return multRunDone;
				}
				if(testAbort(multRunCommand,multRunDone) == true)
				{
					retval = false;
				}
				index++;
			}// end while on MULTRUN exposures
		}// end if Data Pipeline is to be called
		else
		{
		// no pipeline processing occured, set return value to something bland.
		// set filename to last filename exposed.
			multRunDone.setFilename(filename);
			multRunDone.setCounts(0.0f);
			multRunDone.setSeeing(0.0f);
			multRunDone.setXpix(0.0f);
			multRunDone.setYpix(0.0f);
			multRunDone.setPhotometricity(0.0f);
			multRunDone.setSkyBrightness(0.0f);
			multRunDone.setSaturation(false);
		}   
	// if a failure occurs, return now
		if(!retval)
			return multRunDone;
	// setup return values.
	// setCounts,setFilename,setSeeing,setXpix,setYpix 
	// setPhotometricity, setSkyBrightness, setSaturation set by reduceExpose for last image reduced.
	*/
		multRunDone.setErrorNum(CcsConstants.CCS_ERROR_CODE_NO_ERROR);
		multRunDone.setErrorString("");
		multRunDone.setSuccessful(true);
		ccs.error(this.getClass().getName()+": Finished Multrun.");
	// return done object.
		return multRunDone;
	}
}

//
// $Log: not supported by cvs2svn $
// Revision 1.3  2010/03/26 14:38:29  cjm
// Changed from bitwise to absolute logging levels.
//
// Revision 1.2  2010/01/14 16:12:39  cjm
// Added PROGID FITS header setting.
//
// Revision 1.1  2009/10/15 10:21:18  cjm
// Initial revision
//
// Revision 1.3  2009/03/13 11:34:03  wasp
// Update to the logging and submitting current rise config file
//
// Revision 1.2  2008/11/28 10:27:29  wasp
// Update preformed months ago, yet seems to be working ok, thus committing to CVS.
//
// Revision 1.1.1.1  2008/03/11 13:36:56  wasp
// Start
//
// Revision 0.22  2006/05/16 14:25:59  cjm
// gnuify: Added GNU General Public License.
//
// Revision 0.21  2005/07/26 15:54:44  cjm
// Added EXPTOTAL keyword implementation to setFitsHeaders.
//
// Revision 0.20  2005/03/31 13:51:23  cjm
// Added try/catch around setExposureCode, following throw changes.
//
// Revision 0.19  2003/03/26 15:40:18  cjm
// First attempt at windowing implementation.
// ACKS not sent correctly yet...
//
// Revision 0.18  2002/05/23 12:40:25  cjm
// Added defaults for extra fields in EXPOS_DONE.
//
// Revision 0.17  2001/07/12 17:50:27  cjm
// autoguiderStop changes.
//
// Revision 0.16  2001/07/12 10:38:30  cjm
// Moved setFitsHeaders and getFitsHeadersFromISS to be per-frame,
// so contained data is more accurate.
//
// Revision 0.15  2001/07/05 10:56:57  cjm
// MULTRUN_DONE now returns last exposed filename if NOT pipeline processing.
//
// Revision 0.14  2001/07/03 16:21:42  cjm
// Added Ccs error code base to error numbers.
// Changed OBSTYPE declarations.
// Changed CcsFilename type.
//
// Revision 0.13  2001/04/26 17:08:01  cjm
// Added standard flag check, and sets exposure code to standard.
//
// Revision 0.12  2001/03/01 15:15:49  cjm
// Changed from CcsConstants error numbers to hard-coded error numbers.
//
// Revision 0.11  2000/06/20 12:50:31  cjm
// CCDExposureExpose parameter change.
//
// Revision 0.10  2000/06/01 14:01:18  cjm
// ccsFitsHeader replaced by ngat.fits.FitsHeader.
//
// Revision 0.9  2000/03/13 12:16:24  cjm
// Added clearing of pause and resume times.
//
// Revision 0.8  2000/02/28 19:14:00  cjm
// Backup.
//
// Revision 0.7  2000/02/21 11:28:25  cjm
// Added ACKnowledgements sent during exposures and data pipeline processing.
//
// Revision 0.6  1999/11/02 18:22:28  cjm
// Changed so that implementString was deleted.
// Now using over-ridden getImplementString to get a class implementation string.
//
// Revision 0.5  1999/11/01 17:56:15  cjm
// Minor Comment change.
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
