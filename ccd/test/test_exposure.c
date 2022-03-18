/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Rise.

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
/* test_exposure.c
 * $Header: /space/home/eng/cjm/cvs/rise/ccd/test/test_exposure.c,v 1.2 2022-03-15 16:12:44 cjm Exp $
 */
#include <stdio.h>
#include <string.h>
#include <time.h>
#include "ccd_global.h"
#include "ccd_temperature.h"
#include "ccd_setup.h"
#include "ccd_exposure.h"
#include "fitsio.h"

/**
 * This program initialises the SDSU controller using CCD_Setup_Startup.
 * It configures the CCD using CCD_Setup_Dimensions.
 * It then calls CCD_Exposure_Expose or CCD_Exposure_Bias to do a bias,dark or exposure.
 * <pre>
 * test_exposure [-temperature &lt;temperature&gt;]
 * 	[-xs[ize] &lt;no. of pixels&gt;][-ys[ize] &lt;no. of pixels&gt;]
 * 	[-xb[in] &lt;binning factor&gt;][-yb[in] &lt;binning factor&gt;]
 * 	[-w[indow] &lt;no&gt; &lt;xstart&gt; &lt;ystart&gt; &lt;xend&gt; &lt;yend&gt;]
 * 	[-b[ias]][-d[ark] &lt;exposure length&gt;][-e[xpose] &lt;exposure length&gt;]\n");
 * 	[-f[ilename] &lt;filename&gt;][-h[elp]]
 * </pre>
 * @author $Author: cjm $
 * @version $Revision: 1.2 $
 */
/* hash definitions */
/**
 * Maximum length of some of the strings in this program.
 */
#define MAX_STRING_LENGTH	(256)
/**
 * Default temperature to set the CCD to.
 */
#define DEFAULT_TEMPERATURE	(-40.0)
/**
 * Default number of columns in the CCD.
 */
#define DEFAULT_SIZE_X		(1024)
/**
 * Default number of rows in the CCD.
 */
#define DEFAULT_SIZE_Y		(1024)

/* enums */
/**
 * Enumeration determining which command this program executes. One of:
 * <ul>
 * <li>COMMAND_ID_NONE
 * <li>COMMAND_ID_BIAS
 * <li>COMMAND_ID_DARK
 * <li>COMMAND_ID_EXPOSURE
 * </ul>
 */
enum COMMAND_ID
{
	COMMAND_ID_NONE=0,COMMAND_ID_BIAS,COMMAND_ID_DARK,COMMAND_ID_EXPOSURE
};

/* internal variables */
/**
 * Revision control system identifier.
 */
static char rcsid[] = "$Id: test_exposure.c,v 1.2 2022-03-15 16:12:44 cjm Exp $";
/**
 * Temperature to set the CCD to.
 * @see #DEFAULT_TEMPERATURE
 */
static double Temperature = DEFAULT_TEMPERATURE;
/**
 * The number of columns in the CCD.
 * @see #DEFAULT_SIZE_X
 */
static int Size_X = DEFAULT_SIZE_X;
/**
 * The number of rows in the CCD.
 * @see #DEFAULT_SIZE_Y
 */
static int Size_Y = DEFAULT_SIZE_Y;
/**
 * The number binning factor in columns.
 */
static int Bin_X = 1;
/**
 * The number binning factor in rows.
 */
static int Bin_Y = 1;
/**
 * Window flags specifying which window to use.
 */
static int Window_Flags = 0;
/**
 * Window data.
 */
static struct CCD_Setup_Window_Struct Window_List[CCD_SETUP_WINDOW_COUNT];
/**
 * Which type ef exposure command to call.
 * @see #COMMAND_ID
 */
static enum COMMAND_ID Command = COMMAND_ID_NONE;
/**
 * If doing a dark or exposure, the exposure length.
 */
static int Exposure_Length = 0;
/**
 * Filename to store resultant fits image in. Unwindowed only - windowed get more complex.
 */
static char *Filename = NULL;

/* internal routines */
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);
static int Test_Save_Fits_Headers(int exposure_time,int ncols,int nrows,char *filename);
static void Test_Fits_Header_Error(int status);

/**
 * Main program.
 * @param argc The number of arguments to the program.
 * @param argv An array of argument strings.
 * @return This function returns 0 if the program succeeds, and a positive integer if it fails.
 * @see #Temperature
 * @see #Size_X
 * @see #Size_Y
 * @see #Bin_X
 * @see #Bin_Y
 * @see #Window_Flags
 * @see #Window_List
 * @see #Command
 * @see #Exposure_Length
 * @see #Filename
 */
int main(int argc, char *argv[])
{
	struct timespec start_time;
	char buff[256];
	char command_buffer[256];
	char *filename_list[CCD_SETUP_WINDOW_COUNT];
	int filename_count,i;
	int retval;
	int value,bit_value;

/* parse arguments */
	fprintf(stdout,"Parsing Arguments.\n");
	if(!Parse_Arguments(argc,argv))
		return 1;
/* set text/interface options */
	CCD_Global_Set_Log_Handler_Function(CCD_Global_Log_Handler_Stdout);
/* call CCD_Setup_Startup */
	fprintf(stdout,"Calling CCD_Setup_Startup:\n");
	fprintf(stdout,"Temperature:%.2f\n",Temperature);
	if(!CCD_Setup_Startup(Temperature))
	{
		CCD_Global_Error();
		return 3;
	}
	fprintf(stdout,"CCD_Setup_Startup completed\n");
/* call CCD_Setup_Dimensions */
	fprintf(stdout,"Calling CCD_Setup_Dimensions:\n");
	fprintf(stdout,"Chip Size:(%d,%d)\n",Size_X,Size_Y);
	fprintf(stdout,"Binning:(%d,%d)\n",Bin_X,Bin_Y);
	fprintf(stdout,"Window Flags:%d\n",Window_Flags);
	if(!CCD_Setup_Dimensions(Size_X,Size_Y,Bin_X,Bin_Y,Window_Flags,Window_List))
	{
		CCD_Global_Error();
		return 3;
	}
	fprintf(stdout,"CCD_Setup_Dimensions completed\n");
/* save fits headers */
	if(Window_Flags > 0 )
	{
		filename_count = 0;
		if(strchr(Filename,'.') != NULL)
			Filename[strchr(Filename,'.')-Filename] = '\0';
		for(i=0;i<CCD_SETUP_WINDOW_COUNT;i++)
		{
			/* relies on values in CCD_SETUP_WINDOW_ONE... */
			if(Window_Flags & (1<<i))
			{
				sprintf(buff,"%sw%d.fits",Filename,i);
				filename_list[filename_count] = strdup(buff);
				/* binning? */
				if(!Test_Save_Fits_Headers(Exposure_Length,CCD_Setup_Get_Window_Width(i),
							   CCD_Setup_Get_Window_Height(i),
							   filename_list[filename_count]))
				{
					fprintf(stdout,"Saving FITS window headers (%s,%d,%d) failed.\n",
						filename_list[filename_count],CCD_Setup_Get_Window_Width(i),
						CCD_Setup_Get_Window_Height(i));
					return 4;
				}
				filename_count++;
			}/* end if window active */
		}/* end for */
	}
	else
	{
		filename_list[0] = Filename;
		filename_count = 1;
		if(!Test_Save_Fits_Headers(Exposure_Length,Size_X/Bin_X,Size_Y/Bin_Y,Filename))
		{
			fprintf(stdout,"Saving FITS headers failed.\n");
			return 4;
		}
	}
/* do command */
	start_time.tv_sec = 0;
	start_time.tv_nsec = 0;
	switch(Command)
	{
		case COMMAND_ID_BIAS:
			fprintf(stdout,"Calling CCD_Exposure_Bias.\n");
			retval = CCD_Exposure_Bias(Filename);
			break;
		case COMMAND_ID_DARK:
			fprintf(stdout,"Calling CCD_Exposure_Expose with open_shutter FALSE.\n");
			retval = CCD_Exposure_Expose(TRUE,FALSE,start_time,Exposure_Length,
						     filename_list,filename_count);
			break;
		case COMMAND_ID_EXPOSURE:
			fprintf(stdout,"Calling CCD_Exposure_Expose with open_shutter TRUE.\n");
			retval = CCD_Exposure_Expose(TRUE,TRUE,start_time,Exposure_Length,
						     filename_list,filename_count);
			break;
		case COMMAND_ID_NONE:
			fprintf(stdout,"Please select a command to execute (-bias | -dark | -expose).\n");
			Help();
			return 5;
	}
	if(retval == FALSE)
	{
		CCD_Global_Error();
		sprintf(command_buffer,"rm %s",Filename);
		system(command_buffer);
		return 6;
	}
	fprintf(stdout,"Command Completed.\n");
	return 0;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Temperature
 * @see #Size_X
 * @see #Size_Y
 * @see #Bin_X
 * @see #Bin_Y
 * @see #Window_Flags
 * @see #Window_List
 * @see #Command
 * @see #Exposure_Length
 * @see #Filename
 */
static int Parse_Arguments(int argc, char *argv[])
{
	struct CCD_Setup_Window_Struct window;
	int i,retval,window_number;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-bias")==0)||(strcmp(argv[i],"-b")==0))
		{
			Command = COMMAND_ID_BIAS;
		}
		else if((strcmp(argv[i],"-dark")==0)||(strcmp(argv[i],"-d")==0))
		{
			Command = COMMAND_ID_DARK;
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Exposure_Length);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing exposure length %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:dark requires exposure length.\n");
				return FALSE;
			}

		}
		else if((strcmp(argv[i],"-expose")==0)||(strcmp(argv[i],"-e")==0))
		{
			Command = COMMAND_ID_EXPOSURE;
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Exposure_Length);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing exposure length %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:exposure requires exposure length.\n");
				return FALSE;
			}

		}
		else if((strcmp(argv[i],"-filename")==0)||(strcmp(argv[i],"-f")==0))
		{
			if((i+1)<argc)
			{
				Filename = argv[i+1];
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:filename required.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-help")==0)||(strcmp(argv[i],"-h")==0))
		{
			Help();
			exit(0);
		}
		else if(strcmp(argv[i],"-temperature")==0)
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%lf",&Temperature);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing temperature %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:temperature required.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-window")==0)||(strcmp(argv[i],"-w")==0))
		{
			if((i+5)<argc)
			{
				/* window number */
				retval = sscanf(argv[i+1],"%d",&window_number);
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Parsing Window Number failed.\n",argv[i+1]);
					return FALSE;
				}
				switch(window_number)
				{
				case 1:
					Window_Flags |= CCD_SETUP_WINDOW_ONE;
					break;
				case 2:
					Window_Flags |= CCD_SETUP_WINDOW_TWO;
					break;
				case 3:
					Window_Flags |= CCD_SETUP_WINDOW_THREE;
					break;
				case 4:
					Window_Flags |= CCD_SETUP_WINDOW_FOUR;
					break;
				default:
					fprintf(stderr,"Parse_Arguments:Window Number %d out of range(1..4).\n",
						window_number);
					return FALSE;
				}
				/* x start */
				retval = sscanf(argv[i+2],"%d",&(window.X_Start));
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Parsing Window Start X (%s) failed.\n",
						argv[i+2]);
					return FALSE;
				}
				/* y start */
				retval = sscanf(argv[i+3],"%d",&(window.Y_Start));
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Parsing Window Start Y (%s) failed.\n",
						argv[i+3]);
					return FALSE;
				}
				/* x end */
				retval = sscanf(argv[i+4],"%d",&(window.X_End));
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Parsing Window End X (%s) failed.\n",
						argv[i+4]);
					return FALSE;
				}
				/* y end */
				retval = sscanf(argv[i+5],"%d",&(window.Y_End));
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Parsing Window End Y (%s) failed.\n",
						argv[i+5]);
					return FALSE;
				}
				/* put window into correct list index 1..4 -> 0..3 */
				Window_List[window_number-1] = window;
				i+= 5;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:-window requires 5 argument:%d supplied.\n",argc-i);
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-xsize")==0)||(strcmp(argv[i],"-xs")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Size_X);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing X Size %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:size required.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-ysize")==0)||(strcmp(argv[i],"-ys")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Size_Y);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing Y Size %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:size required.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-xbin")==0)||(strcmp(argv[i],"-xb")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Bin_X);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing X Bin %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:bin required.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-ybin")==0)||(strcmp(argv[i],"-yb")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%d",&Bin_Y);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Parsing Y Bin %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:bin required.\n");
				return FALSE;
			}
		}
		else
		{
			fprintf(stderr,"Parse_Arguments:argument '%s' not recognized.\n",argv[i]);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Help routine.
 */
static void Help(void)
{
	fprintf(stdout,"Test Exposure:Help.\n");
	fprintf(stdout,"This program calls CCD_Setup_Dimensions to set up the SDSU controller dimensions.\n");
	fprintf(stdout,"It then calls either CCD_Exposure_Bias or CCD_Exposure_Expose to perform an exposure.\n");
	fprintf(stdout,"test_exposure [-temperature <temperature>]\n");
	fprintf(stdout,"\t[-xs[ize] <no. of pixels>][-ys[ize] <no. of pixels>]\n");
	fprintf(stdout,"\t[-xb[in] <binning factor>][-yb[in] <binning factor>]\n");
	fprintf(stdout,"\t[-w[indow] <no> <xstart> <ystart> <xend> <yend>]\n");
	fprintf(stdout,"\t[-f[ilename] <filename>]\n");
	fprintf(stdout,"\t[-b[ias]][-d[ark] <exposure length>][-e[xpose] <exposure length>]\n");
	fprintf(stdout,"\t[-h[elp]]\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t-help prints out this message and stops the program.\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t<filename> is the FITS image filename the read out image is put into.\n");
	fprintf(stdout,"\t<temperature> should be a valid double, a temperature in degrees Celcius.\n");
	fprintf(stdout,"\t<exposure length> is a positive integer in milliseconds.\n");
	fprintf(stdout,"\t<no. of pixels> and <binning factor> is a positive integer.\n");
}

/**
 * Internal routine that saves some basic FITS headers to the relevant filename.
 * This is needed as CCD_Exposure_Expose/Bias routines need saved FITS headers to
 * not give an error.
 * @param exposure_time The amount of time, in milliseconds, of the exposure.
 * @param ncols The number of columns in the FITS file.
 * @param nrows The number of rows in the FITS file.
 * @param filename The filename to save the FITS headers in.
 */
static int Test_Save_Fits_Headers(int exposure_time,int ncols,int nrows,char *filename)
{
	static fitsfile *fits_fp = NULL;
	int status = 0,retval,ivalue;
	double dvalue;

/* open file */
	if(fits_create_file(&fits_fp,filename,&status))
	{
		Test_Fits_Header_Error(status);
		return FALSE;
	}
/* SIMPLE keyword */
	ivalue = TRUE;
	retval = fits_update_key(fits_fp,TLOGICAL,(char*)"SIMPLE",&ivalue,NULL,&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* BITPIX keyword */
	ivalue = 16;
	retval = fits_update_key(fits_fp,TINT,(char*)"BITPIX",&ivalue,NULL,&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* NAXIS keyword */
	ivalue = 2;
	retval = fits_update_key(fits_fp,TINT,(char*)"NAXIS",&ivalue,NULL,&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* NAXIS1 keyword */
	ivalue = ncols;
	retval = fits_update_key(fits_fp,TINT,(char*)"NAXIS1",&ivalue,NULL,&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* NAXIS2 keyword */
	ivalue = nrows;
	retval = fits_update_key(fits_fp,TINT,(char*)"NAXIS2",&ivalue,NULL,&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* BZERO keyword */
	dvalue = 32768.0;
	retval = fits_update_key_fixdbl(fits_fp,(char*)"BZERO",dvalue,6,
		(char*)"Number to offset data values by",&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* BSCALE keyword */
	dvalue = 1.0;
	retval = fits_update_key_fixdbl(fits_fp,(char*)"BSCALE",dvalue,6,
		(char*)"Number to multiply data values by",&status);
	if(retval != 0)
	{
		Test_Fits_Header_Error(status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
/* close file */
	if(fits_close_file(fits_fp,&status))
	{
		Test_Fits_Header_Error(status);
		return FALSE;
	}
	return TRUE;
}

/**
 * Internal routine to write the complete CFITSIO error stack to stderr.
 * @param status The status returned by CFITSIO.
 */
static void Test_Fits_Header_Error(int status)
{
	/* report the whole CFITSIO error message stack to stderr. */
	fits_report_error(stderr, status);
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:15:01  cjm
** Initial revision
**
** Revision 1.4  2006/05/16 18:18:25  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.3  2004/11/04 16:03:05  cjm
** Added DeInterlace_Type = CCD_DSP_DEINTERLACE_FLIP for right amplifier.
**
** Revision 1.2  2003/03/26 15:51:55  cjm
** Changed for windowing API change.
**
** Revision 1.1  2002/11/07 19:18:22  cjm
** Initial revision
**
*/
