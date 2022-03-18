/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of CCD-Misc.

    CCD-Misc is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    CCD-Misc is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CCD-Misc; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
/* targa_combine.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/targa_combine.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * targa_combine &lt;-r red_targa_filename&gt; &lt;-g green_targa_filename&gt; &lt;-b blue_targa_filename&gt; 
 * &lt;-o TARGA filename&gt;
 * </pre>
 * targa_combine combines up to three targa files (specifying the resultant RGB components) into 1 colour Targa file.
 * @see #main
 */
#include <stdio.h>
#include "targa.h"

/* hash definitions */

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: targa_combine.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Input filename for red component. 
 */
static char *Red_Filename = NULL;
/**
 * Input filename for green component. 
 */
static char *Green_Filename = NULL;
/**
 * Input filename for blue component. 
 */
static char *Blue_Filename = NULL;
/**
 * Output filename. 
 */
static char *Output_Filename = NULL;
/**
 * Red Data.
 */
static unsigned char *Red_Data = NULL;
/**
 * Green Data.
 */
static unsigned char *Green_Data = NULL;
/**
 * Blue Data.
 */
static unsigned char *Blue_Data = NULL;

/* internal routines */
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments.
 * @param argv The arguments.
 */
int main(int argc,char *argv[])
{
	int width,height,check_width,check_height;

/* check arguments */
	if(!Parse_Arguments(argc,argv))
		return 1;
/* check filenames, percentiles */
	if((Red_Filename == NULL)&&(Green_Filename == NULL)&&(Blue_Filename == NULL))
	{
		fprintf(stderr,"No input filenames specified.\n");
		Help();
		return 1;
	}
	if(Output_Filename == NULL)
	{
		fprintf(stderr,"Output filename was NULL.\n");
		Help();
		return 2;
	}
	check_width = -1;
	check_height = -1;
/* open red file */
	if(Red_Filename != NULL)
	{
		if(!Targa_Read(Red_Filename,&width,&height,&Red_Data,NULL,NULL))
		{
			Targa_Error(stderr);
			return 3;
		}
		check_width = width;
		check_height = height;
	}
/* open green file */
	if(Green_Filename != NULL)
	{
		if(!Targa_Read(Green_Filename,&width,&height,&Green_Data,NULL,NULL))
		{
			Targa_Error(stderr);
			return 4;
		}
		if((check_width < 0)&&(check_height < 0))
		{
			check_width = width;
			check_height = height;
		}
		if((check_width != width)||(check_height != height))
		{
			fprintf(stderr,"Filenames have different dimensions (%d,%d) vs. (%d,%d).\n",
				check_width,check_height,width,height);
			return 5;
		}
	}
/* open blue file */
	if(Blue_Filename != NULL)
	{
		if(!Targa_Read(Blue_Filename,&width,&height,&Blue_Data,NULL,NULL))
		{
			Targa_Error(stderr);
			return 6;
		}
		if((check_width < 0)&&(check_height < 0))
		{
			check_width = width;
			check_height = height;
		}
		if((check_width != width)||(check_height != height))
		{
			fprintf(stderr,"Filenames have different dimensions (%d,%d) vs. (%d,%d).\n",
				check_width,check_height,width,height);
			return 7;
		}
	}
/* write targa file */
	if(!Targa_Write(Output_Filename,width,height,Red_Data,Green_Data,Blue_Data))
	{
		Targa_Error(stderr);
		return 12;
	}
	return 0;
}
/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Red_Filename
 * @see #Green_Filename
 * @see #Blue_Filename
 * @see #Output_Filename
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-b")==0)||(strcmp(argv[i],"-blue")==0))
		{
			if((i+1)<argc)
			{
				Blue_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"Targa Combine:Parse_Arguments:"
					"Blue Input filename missing.\n");
		}
		else if((strcmp(argv[i],"-g")==0)||(strcmp(argv[i],"-green")==0))
		{
			if((i+1)<argc)
			{
				Green_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"Targa Combine:Parse_Arguments:"
					"Green Input filename missing.\n");
		}
		else if((strcmp(argv[i],"-h")==0)||(strcmp(argv[i],"-help")==0))
		{
			Help();
			exit(0);
		}
		else if((strcmp(argv[i],"-o")==0)||(strcmp(argv[i],"-output")==0))
		{
			if((i+1)<argc)
			{
				Output_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"Targa Combine:Parse_Arguments:"
					"Output filename missing.\n");
		}
		else if((strcmp(argv[i],"-r")==0)||(strcmp(argv[i],"-red")==0))
		{
			if((i+1)<argc)
			{
				Red_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"Targa Combine:Parse_Arguments:"
					"Input filename missing.\n");
		}
		else
			fprintf(stderr,"Targa Combine:Parse_Arguments:Illegal Argument %s",argv[i]);
	}
	return TRUE;
}

/**
 * Help routine.
 */
static void Help(void)
{
	fprintf(stdout,"Targa Combine:Help.\n");
	fprintf(stdout,"Targa Combine converts three red/green/blue Targa files to one 24 bit colour Targa.\n");
	fprintf(stdout,"targa_combine -r[ed] <Targa filename> -g[reen] <Targa filename> -b[lue] <Targa filename> "
			"-o[utput] <Targa filename>\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:24:52  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2001/07/10 19:02:34  cjm
** Initial revision
**
*/

