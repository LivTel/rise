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
/* fits_create_blank.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_create_blank.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * Create a blank FITS image of the specified dimension, all pixels containing the same data value.
 * A TFLOAT FITS image is created.
 * <pre>
 * fits_create_blank -c[olumns] <n> -r[ows] <n> -o[utput] <fits filename> [-v[alue] <n>]
 * </pre>
 */
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <float.h>
#include "fitsio.h"

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_create_blank.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
static char *Output_Filename = NULL;
static int NCols = -1;
static int NRows = -1;
static float Value = 0.0;
static fitsfile *Output_Fits_Fp = NULL;

static int Parse_Arguments(int argc, char *argv[]);
static int Open_Output(void);
static int Close_Output(void);
static void Help(void);

/*===========================================================================*/
/*	External routines.                                                   */
/*===========================================================================*/
/**
 * Main Program.
 * @param argc The number of arguments. This should be 2.
 * @param argv The arguments.
 */
int main( int argc, char *argv[] )
{
	int retval,cfitsio_status,i;
	float *data_list = NULL;

/* check arguments */
	if(!Parse_Arguments(argc,argv))
		return 1;
	if(Output_Filename == NULL)
	{
		fprintf(stderr,"fits_create_blank:output filename was NULL.\n");
		Help();
		return 2;
	}
	if(NCols < 1)
	{
		fprintf(stderr,"fits_create_blank:Number of columns less than 1.\n");
		Help();
		return 3;
	}
	if(NRows < 1)
	{
		fprintf(stderr,"fits_create_blank:Number of rows less than 1.\n");
		Help();
		return 4;
	}
	/* open */
	if(!Open_Output())
	{
		Close_Output();
		return 5;
	}
	/* allocate memory for one row */
	data_list = (float *)malloc(NCols*sizeof(float));
	if(data_list == NULL)
	{
		fprintf(stderr,"fits_create_blank:malloc(%d) failed.\n",NCols);
		Close_Output();
		return 6;
	}
	/* write value to array */
	for(i=0;i<NCols;i++)
	{
		data_list[i] = Value;
	}
	/* write data_list for each row to fits image */
	cfitsio_status=0;
	for(i=0;i<NRows;i++)
	{
		retval = fits_write_img(Output_Fits_Fp,TFLOAT,(i*NCols)+1,NCols,data_list,&cfitsio_status);
		if(retval != 0)
		{
			fits_report_error(stderr,cfitsio_status);
			fprintf(stderr,"fits_create_blank:fits_write_img failed.\n");
			if(data_list != NULL)
				free(data_list);
			Close_Output();
			return 7;
		}	
	}
	if(data_list != NULL)
		free(data_list);
	/* close */
	Close_Output();
	return 0;
}

/**
 * Opens the output FITS files, using the filename specified. Also setups BITPIX, NAXIS, NAXIS1 and
 * NAXIS2 keywords.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Output_Fits_Fp
 * @see #Output_Filename
 * @see #NCols
 * @see #NRows
 */
static int Open_Output(void)
{
	int retval=0,cfitsio_status=0,ivalue;
	long axes_list[2];
	double dvalue;

	cfitsio_status=0;
/* open output files */
	retval = fits_create_file(&Output_Fits_Fp,Output_Filename,&cfitsio_status);
	if(retval)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"fits_create_blank: Create %s for output failed.\n",Output_Filename);
		return FALSE;
	}
	/* create image */
	axes_list[0] = NCols;
	axes_list[1] = NRows;
	retval = fits_create_img(Output_Fits_Fp,FLOAT_IMG,2,axes_list,&cfitsio_status);
	if(retval != 0)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"fits_create_blank:fits_create_img failed.\n");
		return FALSE;
	}
	return TRUE;
}

/**
 * Closes the FITS file, using Output_Fits_Fp.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 */
static int Close_Output(void)
{
	int retval=0,cfitsio_status=0,i;

	if(Output_Fits_Fp != NULL)
	{
		retval = fits_close_file(Output_Fits_Fp,&cfitsio_status);
		Output_Fits_Fp = NULL;
		if(retval)
		{
			fits_report_error(stderr,cfitsio_status);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Output_Filename
 * @see #NCols
 * @see #NRows
 * @see #Value
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i,retval;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-c")==0)||(strcmp(argv[i],"-columns")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%ld",&NCols);
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Illegal Number of columns(%s).\n",argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Number of columns missing.\n");
				return FALSE;
			}
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
				Output_Filename = strdup(argv[i+1]);
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Output filename missing.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-r")==0)||(strcmp(argv[i],"-rows")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%ld",&NRows);
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Illegal Number of rows(%s).\n",argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Number of rows missing.\n");
				return FALSE;
			}
		}
		else if((strcmp(argv[i],"-v")==0)||(strcmp(argv[i],"-value")==0))
		{
			if((i+1)<argc)
			{
				retval = sscanf(argv[i+1],"%f",&Value);
				if(retval != 1)
				{
					fprintf(stderr,"Parse_Arguments:Illegal value(%s).\n",argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:value missing.\n");
				return FALSE;
			}
		}
		else
		{
			fprintf(stderr,"Parse_Arguments:Illegal Argument %s",argv[i]);
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
	fprintf(stdout,"FITS Create Blank:Help.\n");
	fprintf(stdout,"FITS Create Blank creates a blank FITS image of the specified dimensions with the specified value (0 default).\n");
	fprintf(stdout,"The resultant FITS file is of FLOAT type.\n");
	fprintf(stdout,"fits_create_blank -c[olumns] <n> -r[ows] <n> -o[utput] <fits filename> [-v[alue] <n>][-h[elp]]\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:22:40  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2006/05/05 09:42:42  cjm
** Fixed bugs.
**
** Revision 1.1  2006/05/05 09:32:13  cjm
** Initial revision
**
*/
