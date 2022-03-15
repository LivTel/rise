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
/* test_setup_dimensions.c
 * $Header: /space/home/eng/cjm/cvs/rise/ccd/test/test_setup_dimensions.c,v 1.2 2022-03-15 16:12:44 cjm Exp $
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "ccd_global.h"
#include "ccd_temperature.h"
#include "ccd_setup.h"

/**
 * This program tests CCD_Setup_Dimensions, which does dimension configuration of the SDSU controller.
 * <pre>
 * test_setup_dimensions [-xs[ize] &lt;no. of pixels&gt;][-ys[ize] &lt;no. of pixels&gt;]
 * 	[-xb[in] &lt;binning factor&gt;][-yb[in] &lt;binning factor&gt;]
 * 	[-w[indow] &lt;no&gt; &lt;xstart&gt; &lt;ystart&gt; &lt;xend&gt; &lt;yend&gt;]
 * 	[-h[elp]]
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
 * Default number of columns in the CCD.
 */
#define DEFAULT_SIZE_X		(1024)
/**
 * Default number of rows in the CCD.
 */
#define DEFAULT_SIZE_Y		(1024)

/* internal variables */
/**
 * Revision control system identifier.
 */
static char rcsid[] = "$Id: test_setup_dimensions.c,v 1.2 2022-03-15 16:12:44 cjm Exp $";
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

/* internal routines */
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);

/**
 * Main program.
 * @param argc The number of arguments to the program.
 * @param argv An array of argument strings.
 * @return This function returns 0 if the program succeeds, and a positive integer if it fails.
 * @see #Size_X
 * @see #Size_Y
 * @see #Bin_X
 * @see #Bin_Y
 * @see #Window_Flags
 * @see #Window_List
 */
int main(int argc, char *argv[])
{
	int retval;
	int value,bit_value;

/* parse arguments */
	fprintf(stdout,"Parsing Arguments.\n");
	if(!Parse_Arguments(argc,argv))
		return 1;
/* set logging */
	CCD_Global_Set_Log_Handler_Function(CCD_Global_Log_Handler_Stdout);
	/* open connection to camera and do initial initialisation */
	if(!CCD_Setup_Startup(-40.0))
	{
		CCD_Global_Error();
		return 2;
	}
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
	return 0;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Size_X
 * @see #Size_Y
 * @see #Bin_X
 * @see #Bin_Y
 * @see #Window_Flags
 * @see #Window_List
 */
static int Parse_Arguments(int argc, char *argv[])
{
	struct CCD_Setup_Window_Struct window;
	int i,retval,window_number;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-help")==0)||(strcmp(argv[i],"-h")==0))
		{
			Help();
			exit(0);
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
	fprintf(stdout,"Test Setup Dimensions:Help.\n");
	fprintf(stdout,"This program tests the CCD_Setup_Dimensions routine, which sets up the camera dimensions.\n");
	fprintf(stdout,"test_setup_dimensions [-xs[ize] <no. of pixels>][-ys[ize] <no. of pixels>]\n");
	fprintf(stdout,"\t[-xb[in] <binning factor>][-yb[in] <binning factor>]\n");
	fprintf(stdout,"\t[-w[indow] <no> <xstart> <ystart> <xend> <yend>][-h[elp]]\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t-help prints out this message and stops the program.\n");
	fprintf(stdout,"\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:15:01  cjm
** Initial revision
**
** Revision 1.4  2006/11/06 16:52:49  eng
** Added includes to fix implicit function declarations.
**
** Revision 1.3  2006/05/16 18:18:29  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2004/11/04 16:03:58  cjm
** Added Deinterlace-type = FLIP for right amplifier.
**
** Revision 1.1  2002/11/07 19:18:22  cjm
** Initial revision
**
*/
