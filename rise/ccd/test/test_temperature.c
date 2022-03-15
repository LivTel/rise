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
/* test_temperature.c
 * $Header: /space/home/eng/cjm/cvs/rise/ccd/test/test_temperature.c,v 1.2 2022-03-15 16:12:44 cjm Exp $
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "ccd_global.h"
#include "ccd_temperature.h"
#include "ccd_setup.h"

/**
 * This program allows the user to:
 * <ul>
 * <li>Get the current CCD temperature.
 * <li>Set the current CCD temperature.
 * </ul>
 * <pre>
 * test_temperature -g[et] -s[et] &lt;temperature (degrees C)&gt; -h[elp]
 * </pre>
 * @author $Author: cjm $
 * @version $Revision: 1.2 $
 */
/* hash definitions */
/**
 * Maximum length of some of the strings in this program.
 */
#define MAX_STRING_LENGTH	(256)

/* enums */
/**
 * Enumeration determining which command this program executes. One of:
 * <ul>
 * <li>COMMAND_ID_NONE
 * <li>COMMAND_ID_GET
 * <li>COMMAND_ID_SET
 * </ul>
 */
enum COMMAND_ID
{
	COMMAND_ID_NONE=0,COMMAND_ID_GET,COMMAND_ID_SET
};

/* internal variables */
/**
 * Revision control system identifier.
 */
static char rcsid[] = "$Id: test_temperature.c,v 1.2 2022-03-15 16:12:44 cjm Exp $";
/**
 * Which SDSU command to call.
 * @see #COMMAND_ID
 */
static enum COMMAND_ID Command = COMMAND_ID_NONE;
/**
 * The target temperature to use when setting the temperature.
 */
static double Target_Temperature = 0.0;

/* internal routines */
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);

/**
 * Main program.
 * @param argc The number of arguments to the program.
 * @param argv An array of argument strings.
 * @return This function returns 0 if the program succeeds, and a positive integer if it fails.
 * @see #Text_Print_Level
 * @see #Interface_Device
 * @see #Command
 */
int main(int argc, char *argv[])
{
	int retval;
	int adu;
	double temperature;

	fprintf(stdout,"Parsing Arguments.\n");
	if(!Parse_Arguments(argc,argv))
		return 1;
	CCD_Global_Initialise();
	CCD_Global_Set_Log_Handler_Function(CCD_Global_Log_Handler_Stdout);
	CCD_Setup_Initialise();
	fprintf(stdout,"Initialise Camera.\n");
	retval = CCD_Setup_Startup(-40.0);
	if(retval == FALSE)
	{
		CCD_Global_Error();
		return 1;
	}
	switch(Command)
	{
		case COMMAND_ID_GET:
			fprintf(stdout,"Calling CCD_Temperature_Get.\n");
			retval = CCD_Temperature_Get(&temperature);
			if(retval == FALSE)
			{
				CCD_Global_Error();
				return 2;
			}
			fprintf(stdout,"The current temperature is %.2ff degrees centigrade.\n",temperature);
			break;
		case COMMAND_ID_SET:
			fprintf(stdout,"Calling CCD_Temperature_Set.\n");
			retval = CCD_Temperature_Set(Target_Temperature);
			if(retval == FALSE)
			{
				CCD_Global_Error();
				return 3;
			}
			fprintf(stdout,"The temperature has been set to %.2f.\n",Target_Temperature);
			break;
		case COMMAND_ID_NONE:
			fprintf(stdout,"Please select a command to execute:"
				"(-g[et] | -set ).\n");
			Help();
			exit(5);
	}
	fprintf(stdout,"Command Completed.\n");
	return retval;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Text_Print_Level
 * @see #Interface_Device
 * @see #Command
 * @see #COMMAND_ID
 * @see #Target_Temperature
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i,retval;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-get")==0)||(strcmp(argv[i],"-g")==0))
		{
			Command = COMMAND_ID_GET;
		}
		else if((strcmp(argv[i],"-help")==0)||(strcmp(argv[i],"-h")==0))
		{
			Help();
			exit(0);
		}
		else if((strcmp(argv[i],"-set")==0)||(strcmp(argv[i],"-s")==0))
		{
			if((i+1)<argc)
			{
				Command = COMMAND_ID_SET;
				retval = sscanf(argv[i+1],"%lf",&Target_Temperature);
				if(retval != 1)
				{
					fprintf(stderr,
						"Parse_Arguments:Set temperature:Parsing temperature %s failed.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Set requires a temperature.\n");
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
	fprintf(stdout,"Test Temperature:Help.\n");
	fprintf(stdout,"This program allows the user to set/get the current CCD temperature.\n");
	fprintf(stdout,"test_temperature [-g[et]] [-s[et] <temperature>] [-h[elp]]\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t-get Gets the current CCD temperature, in degrees centigrade.\n");
	fprintf(stdout,"\t-set Sets the current CCD temperature. The parameter is in degrees centigrade.\n");
	fprintf(stdout,"\t-help prints out this message and stops the program.\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2009/10/15 10:15:01  cjm
** Initial revision
**
** Revision 1.3  2006/11/06 16:52:49  eng
** Added includes to fix implicit function declarations.
**
** Revision 1.2  2006/05/16 18:18:32  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2002/11/07 19:18:22  cjm
** Initial revision
**
*/
