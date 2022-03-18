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
/* test_setup_startup.c
 * $Header: /space/home/eng/cjm/cvs/rise/ccd/test/test_setup_startup.c,v 1.2 2022-03-15 16:12:44 cjm Exp $
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "ccd_global.h"
#include "ccd_temperature.h"
#include "ccd_setup.h"

/**
 * This program tests CCD_Setup_Startup, which does initial configuration of the SDSU controller.
 * <pre>
 * test_setup_startup -temperature &lt;temperature&gt; -h[elp]
 * </pre>
 * @author $Author: cjm $
 * @version $Revision: 1.2 $
 */
/* hash definitions */
/**
 * Maximum length of some of the strings in this program.
 */
#define MAX_STRING_LENGTH	(256)

/* internal variables */
/**
 * Revision control system identifier.
 */
static char rcsid[] = "$Id: test_setup_startup.c,v 1.2 2022-03-15 16:12:44 cjm Exp $";
/**
 * Temperature to set the CCD to. Defaults to -100.0 degrees C.
 */
static double Temperature = -110.0;

/* internal routines */
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);

/**
 * Main program.
 * @param argc The number of arguments to the program.
 * @param argv An array of argument strings.
 * @return This function returns 0 if the program succeeds, and a positive integer if it fails.
 * @see #Temperature
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
/* call CCD_Setup_Startup */
	fprintf(stdout,"Calling CCD_Setup_Startup:\n");
	fprintf(stdout,"Temperature:%.2f\n",Temperature);
	if(!CCD_Setup_Startup(Temperature))
	{
		CCD_Global_Error();
		return 3;
	}
	fprintf(stdout,"CCD_Setup_Startup completed\n");
	return 0;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Temperature
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i,retval;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-help")==0)||(strcmp(argv[i],"-h")==0))
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
	fprintf(stdout,"Test Setup Startup:Help.\n");
	fprintf(stdout,"This program tests the CCD_Setup_Startup routine, which initialises the Andor camera.\n");
	fprintf(stdout,"test_setup_startup [-temperature <temperature>] [-h[elp]]\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t-help prints out this message and stops the program.\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t<temperature> should be a valid double, a temperature in degrees Celcius.\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:15:01  cjm
** Initial revision
**
** Revision 1.3  2006/11/06 16:52:49  eng
** Added includes to fix implicit function declarations.
**
** Revision 1.2  2006/05/16 18:18:30  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2002/11/07 19:18:22  cjm
** Initial revision
**
*/
