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
/* fits_get_data.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_get_data.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_get_data &lt;FITS filename&gt; 
 * </pre>
 * fits_get_data reads the FITS file. It extracts the data segment from a simple file and displays the data
 * values as numberic values, which can then be processed by diff,sed,awk etc.
 * The program only supports SIMPLE FITS files with BITBIX = 16 and NAXIS = 2. It reads NAXIS1 and NAXIS2
 * to get image dimensions, and scales the output data values using BZERO and BSCALE (automatically via CFITSIO).
 * @see #main
 */
#include <stdio.h>
#include "fitsio.h"

/* hash definitions */
/**
 * This program only accepts FITS files with the bits per pixel of this value.
 */
#define FITS_GET_DATA_BITPIX		(16)
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_GET_DATA_NAXIS		(2)

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_get_data.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments. This should be 2.
 * @param argv The arguments.
 */
int main(int argc,char *argv[])
{
	fitsfile *fp = NULL;
	int retval=0,status=0,integer_value,naxis_one,naxis_two,i,j;
	unsigned short *data = NULL;

/* check arguments */
	if(argc != 2)
	{
		fprintf(stderr,"fits_get_data <FITS filename>.\n");
		return 1;
	}
/* open file */
	retval = fits_open_file(&fp,argv[1],READONLY,&status);
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
	if(integer_value != FITS_GET_DATA_BITPIX)
	{
		fprintf(stderr,"fits_get_data: %s has wrong BITPIX value(%d).\n",argv[1],integer_value);
		return 4;
	}
/* check naxis */
	retval = fits_read_key(fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 5;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"fits_get_data: %s has wrong NAXIS value(%d).\n",argv[1],integer_value);
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
/* allocate data */
	data = (unsigned short *)malloc(naxis_one*naxis_two*sizeof(unsigned short));
	if(data == NULL)
	{
		fprintf(stderr,"fits_get_data: failed to allocate memory (%d,%d,%d).\n",
			naxis_one,naxis_two,naxis_one*naxis_two*sizeof(short));
		return 9;
	}
/* read the data */
	retval = fits_read_img(fp,TUSHORT,1,naxis_one*naxis_two,NULL,data,NULL,&status);
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
/* print out array */
	/*
	** See http://ltccd1.livjm.ac.uk/~dev/cfitsio/cfitsiohtml/node66.html for CFITSIO data ordering.
	** I've ignored this at the moment!.
	*/
	fprintf(stdout,"%d,%d\n",naxis_one,naxis_two);
	for(j=0;j<naxis_two;j++)
	{
		for(i=0;i<naxis_one;i++)
		{
			fprintf(stdout,"%hu,",data[(naxis_one*j)+i]);
		}
		fprintf(stdout,"\n");
	}
	return 0;
}
