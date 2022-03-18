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
/* fits_to_targa.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_to_targa.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_to_targa &lt;-i FITS filename&gt; &lt;-o TARGA filename&gt;  [-pc &lt;low high&gt;]
 *              [-v[alue_scaling] &lt;min value&lt; &lt;max value&lt;]
 * </pre>
 * fits_to_targa reads the FITS file. It extracts the data segment from a simple file, and writes a 
 * greyscale TARGA file as output. The -pc option supports percentile scaling.
 * The program only supports SIMPLE FITS files with NAXIS = 2. It reads NAXIS1 and NAXIS2
 * to get image dimensions, and scales the output data values using BITPIX, BZERO and BSCALE 
 * (automatically via CFITSIO). The processing is done in doubles.
 * @see #main
 */
#include <stdio.h>
#include "fitsio.h"
#include "targa.h"

/* hash definitions */
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_NAXIS		(2)
/**
 * Maximum data value in a FITS file +1?
 */
#define FITS_MAX_VALUE		(65536)

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_to_targa.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Input filename. 
 */
static char *Input_Filename = NULL;
/**
 * Output filename. 
 */
static char *Output_Filename = NULL;
/**
 * Whether to do percentile scaling.
 */
static int Percentile_Scaling = FALSE;
/**
 * Min Percentile scaling. 
 */
static double Min_Percentile = 0.0;
/**
 * Max Percentile scaling. 
 */
static double Max_Percentile = 0.0;
/**
 * Whether to do value scaling.
 */
static int Value_Scaling = FALSE;
/**
 * Min Value scaling. 
 */
static int Min_Value = 0;
/**
 * Max Value scaling. 
 */
static int Max_Value = 0;
/**
 * FITS data.
 */
static double *Fits_Data = NULL;
/**
 * Targa Data.
 */
static unsigned char *Targa_Data = NULL;
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
	fitsfile *fp = NULL;
	int retval=0,status=0,integer_value,naxis_one,naxis_two,i,j,pixel_count;
	unsigned short pixel_value_frequency[FITS_MAX_VALUE];
	double min_value,max_value,double_value;

/* check arguments */
	if(!Parse_Arguments(argc,argv))
		return 1;
/* check filenames, percentiles */
	if(Input_Filename == NULL)
	{
		fprintf(stderr,"Input filename was NULL.\n");
		Help();
		return 1;
	}
	if(Output_Filename == NULL)
	{
		fprintf(stderr,"Output filename was NULL.\n");
		Help();
		return 1;
	}
	if(Percentile_Scaling)
	{
		if((Min_Percentile < 0.0)||(Min_Percentile > 100.0)||
			(Max_Percentile < 0.0)||(Max_Percentile > 100.0)||
			(Min_Percentile > Max_Percentile))
		{
			fprintf(stderr,"Percentile Scaling error (%.2f,%.2f).\n",Min_Percentile,Max_Percentile);
			Help();
			return 1;
		}
	}
	if(Value_Scaling)
	{
		if((Min_Value < 0)||(Min_Value > 65535)||
			(Max_Value < 0)||(Max_Value > 65535)||
			(Min_Value >= Max_Value))
		{
			fprintf(stderr,"Value Scaling error (%.2f,%.2f).\n",Min_Value,Max_Value);
			Help();
			return 1;
		}
	}
/* open file */
	retval = fits_open_file(&fp,Input_Filename,READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
/* check bitpix */
	retval = fits_read_key(fp,TINT,"BITPIX",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 3;
	}
/* check naxis */
	retval = fits_read_key(fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 5;
	}
	if(integer_value != FITS_NAXIS)
	{
		fprintf(stderr,"fits_to_targa: %s has wrong NAXIS value(%d).\n",argv[1],integer_value);
		return 6;
	}
/* get naxis1,naxis2 */
	retval = fits_read_key(fp,TINT,"NAXIS1",&naxis_one,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 7;
	}
	retval = fits_read_key(fp,TINT,"NAXIS2",&naxis_two,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 8;
	}
/* allocate input data */
	pixel_count = naxis_one*naxis_two;
	Fits_Data = (double *)malloc(pixel_count*sizeof(double));
	if(Fits_Data == NULL)
	{
		fprintf(stderr,"fits_to_targa: failed to allocate input memory (%d,%d,%d).\n",
			naxis_one,naxis_two,pixel_count*sizeof(double));
		return 9;
	}
/* read the data */
	retval = fits_read_img(fp,TDOUBLE,1,pixel_count,NULL,Fits_Data,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 10;
	}
/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 11;
	}
/* rescale double input to 16 bit unsigned  - get minmax values of input array*/
	min_value = 99999.0;
	max_value = -99999.0;
	for(j=0;j<naxis_two;j++)
	{
		for(i=0;i<naxis_one;i++)
		{
			double_value = Fits_Data[(naxis_one*j)+i];
			if(double_value < min_value)
				min_value = double_value;
			if(double_value > max_value)
				max_value = double_value;
		}
	}
	/* rescale input array to 0..FITS_MAX_VALUE */
	for(j=0;j<naxis_two;j++)
	{
		for(i=0;i<naxis_one;i++)
		{
			double_value = Fits_Data[(naxis_one*j)+i];
			double_value =  (((double_value-min_value)/(max_value-min_value))*((double)FITS_MAX_VALUE));
			Fits_Data[(naxis_one*j)+i] = double_value;
		}
	}
/* get percentile numbers */
	if(Percentile_Scaling)
	{
	/* reset pixel_value_frequency */
		for(i=0; i < FITS_MAX_VALUE; i++)
			pixel_value_frequency[i] = 0;
	/* set pixel_value_frequency to the number of pixels where each value occurs */
		for(j=0;j<naxis_two;j++)
		{
			for(i=0;i<naxis_one;i++)
			{
				integer_value = (int)(Fits_Data[(naxis_one*j)+i]);
				pixel_value_frequency[integer_value]++;
			}
		}
	/* find min_value */
		double_value = 0; /* total no of pixels containg values less than i */
		i = 0;
		while((i < FITS_MAX_VALUE)&&((double_value*100.0/pixel_count) < Min_Percentile))
		{
			double_value += (double)(pixel_value_frequency[i]);
			i++;
		}
		min_value = (double)i;
	/* contunue to find max_value */
		while((i < FITS_MAX_VALUE)&&((double_value*100.0/pixel_count) < Max_Percentile))
		{
			double_value += pixel_value_frequency[i];
			i++;
		}
		max_value = (double)i;
	}
	else if(Value_Scaling)
	{
		min_value = (double)Min_Value;
		max_value = (double)Max_Value;
	}
	else
	{
		min_value = 0.0;
		max_value = 65535.0;
	}
/* allocate output data */
	Targa_Data = (unsigned char *)malloc(pixel_count*sizeof(unsigned char));
	if(Targa_Data == NULL)
	{
		fprintf(stderr,"fits_to_targa: failed to allocate output memory (%d,%d,%d).\n",
			naxis_one,naxis_two,pixel_count*sizeof(unsigned char));
		return 9;
	}
/* copy input array to out array,scaling */
	/*
	** See http://ltccd1.livjm.ac.uk/~dev/cfitsio/cfitsiohtml/node66.html for CFITSIO data ordering.
	** I've ignored this at the moment!.
	*/
	fprintf(stdout,"width:%d,height:%d,min_value:%.2f,max_value=%.2f\n",naxis_one,naxis_two,min_value,max_value);
	for(j=0;j<naxis_two;j++)
	{
		for(i=0;i<naxis_one;i++)
		{
			double_value = (double)(Fits_Data[(naxis_one*j)+i]);
			if(double_value < min_value)
				double_value = min_value;
			if(double_value > max_value)
				double_value = max_value;
			Targa_Data[(naxis_one*j)+i] = (unsigned char)(((double_value-min_value)/
									(max_value-min_value))*255.0);
		}
	}
/* write targa file */
	if(!Targa_Write(Output_Filename,naxis_one,naxis_two,Targa_Data,Targa_Data,Targa_Data))
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
 * @see #Input_Filename
 * @see #Output_Filename
 * @see #Percentile_Scaling
 * @see #Min_Percentile
 * @see #Max_Percentile
 * @see #Value_Scaling
 * @see #Min_Value
 * @see #Max_Value
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i;

	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-h")==0)||(strcmp(argv[i],"-help")==0))
		{
			Help();
			exit(0);
		}
		else if((strcmp(argv[i],"-i")==0)||(strcmp(argv[i],"-input")==0))
		{
			if((i+1)<argc)
			{
				Input_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"FITS to Targa:Parse_Arguments:"
					"Input filename missing.\n");
		}
		else if((strcmp(argv[i],"-o")==0)||(strcmp(argv[i],"-output")==0))
		{
			if((i+1)<argc)
			{
				Output_Filename = argv[i+1];
				i++;
			}
			else
				fprintf(stderr,"FITS to Targa:Parse_Arguments:"
					"Output filename missing.\n");
		}
		else if((strcmp(argv[i],"-p")==0)||(strcmp(argv[i],"-percentile_scaling")==0))
		{
			if((i+2)<argc)
			{
				Percentile_Scaling = TRUE;
				sscanf(argv[i+1],"%lf",&Min_Percentile);
				sscanf(argv[i+2],"%lf",&Max_Percentile);
				i+=2;
			}
			else
				fprintf(stderr,"FITS to Targa:Parse_Arguments:"
					"Percentile Scaling requires min and max percentages.\n");
		}
		else if((strcmp(argv[i],"-v")==0)||(strcmp(argv[i],"-value_scaling")==0))
		{
			if((i+2)<argc)
			{
				Value_Scaling = TRUE;
				sscanf(argv[i+1],"%d",&Min_Value);
				sscanf(argv[i+2],"%d",&Max_Value);
				i+=2;
			}
			else
				fprintf(stderr,"FITS to Targa:Parse_Arguments:"
					"Value Scaling requires min and max values.\n");
		}
		else
			fprintf(stderr,"FITS to Targa:Parse_Arguments:Illegal Argument %s",argv[i]);
	}
	return TRUE;
}

/**
 * Help routine.
 */
static void Help(void)
{
	fprintf(stdout,"FITS to Targa:Help.\n");
	fprintf(stdout,"FITS to Targa converts a 2D FITS file to a greyscale 8 bit Targa.\n");
	fprintf(stdout,"fits_to_targa -i[nput] <FITS filename> -o[utput] <Targa filename>\n"
			"\t[-p[ercentile_scaling] <min percentile> <max percentile>\n"
			"\t[-v[alue_scaling] <min value> <max value>\n");
	fprintf(stdout,"\n");
	fprintf(stdout,"\t<min percentile> is in the range [0..100].\n");
	fprintf(stdout,"\t<max percentile> is in the range [0..100].\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.6  2006/05/16 18:24:51  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.5  2004/03/20 14:34:48  cjm
** Supports all type of FITS, not just 16 bit unsigned.
**
** Revision 1.4  2003/03/11 10:41:21  cjm
** Comment change.
**
** Revision 1.3  2003/01/08 14:52:24  cjm
** Added value scaling.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2001/07/10 19:02:34  cjm
** Initial revision
**
*/

