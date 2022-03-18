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
/* fits_get_mean.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_get_mean.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * fits_get mean returns the mean value in the frame, excluding bias strips.
 * <pre>
 * fits_get_mean [-help] <filename> 
 * </pre>
 */
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <time.h>
#include "fitsio.h"

/* ------------------------------------------------------- */
/* internal hash definitions */
/* ------------------------------------------------------- */
/**
 * Number of axes in a valid FITS file.
 */
#define FITS_GET_DATA_NAXIS (2)

/* ------------------------------------------------------- */
/* internal functions declarations */
/* ------------------------------------------------------- */
static void Help(void);
static int Parse_Args(int argc,char *argv[]);
static int Load(void);
static int Get_Mean(float *mean);

/* ------------------------------------------------------- */
/* internal variables */
/* ------------------------------------------------------- */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_get_mean.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Filename of file to be processed.
 */
static char Input_Filename[256] = "";
/**
 * Data in image array.
 */
static float *Image_Data = NULL;
/**
 * Dimensions of data array.
 */
static int Naxis1;
/**
 * Dimensions of data array.
 */
static int Naxis2;
/**
 * Number of prescan bias pixels.
 */
static int Prescan;
/**
 * Number of postscan bias pixels.
 */
static int Postscan;
/**
 * Mean of background.
 */
static float Mean = 0.0;

/* ------------------------------------------------------- */
/* external functions */
/* ------------------------------------------------------- */
/**
 * The main program.
 */
int main(int argc, char *argv[])
{
	int retval;

	if(argc < 2)
	{
		Help();
		return 0;
	}
	if(!Parse_Args(argc,argv))
		return 1;
	if(strcmp(Input_Filename,"")==0)
	{
		fprintf(stderr,"fits_get_mean: No filename specified.\n");
		return 2;
	}
	/* load array */
	if(!Load())
		return 3;
	if(!Get_Mean(&Mean))
		return 4;
	fprintf(stdout,"%.2f\n",Mean);
	/* free image */
	if(Image_Data != NULL)
		free(Image_Data);
	return 0;
}

/* ------------------------------------------------------- */
/* internal functionss */
/* ------------------------------------------------------- */
/**
 * Routine to parse arguments.
 * @param argc The argument count.
 * @param argv The argument list.
 * @return Returns TRUE if the program can proceed, FALSE if it should stop (the user requested help).
 * @see #Input_Filename
 */
static int Parse_Args(int argc,char *argv[])
{
	int i,retval;
	int call_help = FALSE;

	strcpy(Input_Filename,"");
	for(i=1;i<argc;i++)
	{
		if(strcmp(argv[i],"-help")==0)
			call_help = TRUE;
		else
			strcpy(Input_Filename,argv[i]);
	}
	if(call_help)
	{
		Help();
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to produce some help.
 */
static void Help(void)
{
	fprintf(stdout,"fits_get_mean returns the mean pixel value in the data frame (excluding bias strips).\n");
	fprintf(stdout,"fits_get_mean [-help] <FITS filename>\n");
	fprintf(stdout,"-help prints this help message and exits.\n");
	fprintf(stdout,"You must always specify a filename to process.\n");
}

/**
 * Load the FITS image into a float array.
 * @return TRUE on success, FALSE on failure.
 * @see #Input_Filename
 * @see #Naxis1
 * @see #Naxis2
 * @see #Prescan
 * @see #Postscan
 * @see #Image_Data
 */
static int Load(void)
{
	fitsfile *fits_fp = NULL;
	int retval=0,status=0,integer_value;

	/* open file */
	retval = fits_open_file(&fits_fp,Input_Filename,READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	retval = fits_read_key(fits_fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"fits_get_mean: Wrong NAXIS value(%d).\n",integer_value);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	/* get naxis1,naxis2 */
	retval = fits_read_key(fits_fp,TINT,"NAXIS1",&Naxis1,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	retval = fits_read_key(fits_fp,TINT,"NAXIS2",&Naxis2,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	/* get prescan,postscan */
	retval = fits_read_key(fits_fp,TINT,"PRESCAN",&Prescan,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	retval = fits_read_key(fits_fp,TINT,"POSTSCAN",&Postscan,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	/* allocate image */
	Image_Data = (float *)malloc(Naxis1*Naxis2*sizeof(float));
	if(Image_Data == NULL)
	{
		fprintf(stderr,"fits_get_mean: failed to allocate memory (%d,%d).\n",
			Naxis1,Naxis2);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	/* read data */
	retval = fits_read_img(fits_fp,TFLOAT,1,Naxis1*Naxis2,NULL,Image_Data,
			       NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		fprintf(stderr,"fits_get_mean:fits_read_img:Failed to read FITS (%d,%d).\n",Naxis1,Naxis2);
		return FALSE;
	}
	/* close file */
	retval = fits_close_file(fits_fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	return TRUE;
}

static int Get_Mean(float *mean)
{
	int x,y,npix;
	double total;

	if(mean == NULL)
	{
		fprintf(stderr,"Get_Mean:mean was NULL.\n");
		return FALSE;
	}
	total = 0.0;
	for(y=0;y<Naxis2;y++)
	{
		for(x=Prescan;x<(Naxis1-Postscan);x++)
		{
			/*
			**if(y==0)
			**{
			**	fprintf(stdout,"Looking at pixel (%d,%d)=%.2f\n",x,y,
			**		(double)(Image_Data[(y*Naxis1)+x]));
			**}*/
			total += (double)(Image_Data[(y*Naxis1)+x]);
		}
		/*fprintf(stdout,"Line %d, total now %.2f\n",y,total);*/
	}
	npix = (Naxis1*Naxis2)-((Prescan+Postscan)*Naxis2);
	/*fprintf(stdout,"npix = %d = (%d*%d)-((%d+%d)*%d)\n",npix,Naxis1,Naxis2,Prescan,Postscan,Naxis2);*/
	(*mean) = (float)(total/((double)npix));
	return TRUE;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.2  2006/05/16 18:22:47  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2004/05/07 09:53:52  cjm
** Initial revision
**
*/
