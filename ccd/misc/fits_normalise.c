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
/* fits_normalise.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_normalise.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * fits_normalise scales the values in the associated fits image so the mean of the image is 1.0, and
 * writes an output float FITS image
 * <pre>
 * fits_normalise [-help] -i[nput] <FITS filename> -o[utput] <FITS filename>
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
/* internal variables */
/* ------------------------------------------------------- */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_normalise.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Input Filename of file to be processed.
 */
static char Input_Filename[256] = "";
/**
 * Output Filename of file to be processed.
 */
static char Output_Filename[256] = "";
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
 * The mean pixel value of the input image.
 */
static float Mean;

/* ------------------------------------------------------- */
/* internal functions declarations */
/* ------------------------------------------------------- */
static int Parse_Arguments(int argc,char *argv[]);
static void Help(void);
static int Load(void);
/*static int Get_Min_Max(void);*/
static int Get_Mean(void);
static int Normalise_Image_Data(void);
static int Save(void);

/* ------------------------------------------------------- */
/* external functions */
/* ------------------------------------------------------- */
/**
 * The main program.
 * @see #Parse_Arguments
 * @see #Help
 * @see #Load
 * @see #Get_Mean
 */
int main(int argc, char *argv[])
{
	int retval;

	if(argc == 0)
	{
		Help();
		return 0;
	}
	if(!Parse_Arguments(argc,argv))
		return 1;
	if(strcmp(Input_Filename,"")==0)
	{
		fprintf(stderr,"fits_normalise: No input filename specified.\n");
		return 2;
	}
	/* load array */
	if(!Load())
		return 3;
	if(!Get_Mean())
	{
		/* free image */
		if(Image_Data != NULL)
			free(Image_Data);
		return 4;
	}
	fprintf(stdout,"Input mean %.2f\n",Mean);
	/* normalise */
	if(!Normalise_Image_Data())
	{
		/* free image */
		if(Image_Data != NULL)
			free(Image_Data);
		return 4;
	}
	/* output normalised array */
	if(strcmp(Output_Filename,"")==0)
	{
		fprintf(stderr,"fits_normalise: No outputfilename specified.\n");
		/* free image */
		if(Image_Data != NULL)
			free(Image_Data);
		return 5;
	}
	if(!Save())
	{
		/* free image */
		if(Image_Data != NULL)
			free(Image_Data);
		return 6;
	}
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
static int Parse_Arguments(int argc,char *argv[])
{
	int i,retval;

	strcpy(Input_Filename,"");
	for(i=1;i<argc;i++)
	{
		if(strcmp(argv[i],"-help")==0)
		{
			Help();
			exit(0);
		}
		else if((strcmp(argv[i],"-input")==0)||(strcmp(argv[i],"-i")==0))
		{
			if((i+1) >= argc)
			{
				fprintf(stderr,"fits_normalise:-input requires an argument.\n");
				return FALSE;
			}
			strcpy(Input_Filename,argv[i+1]);
			i++;
		}
		else if((strcmp(argv[i],"-output")==0)||(strcmp(argv[i],"-o")==0))
		{
			if((i+1) >= argc)
			{
				fprintf(stderr,"fits_normalise:-output requires an argument.\n");
				return FALSE;
			}
			strcpy(Output_Filename,argv[i+1]);
			i++;
		}
		else
		{
			fprintf(stderr,"fits_normalise:Unknown argument %s.\n",argv[i]);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Routine to produce some help.
 */
static void Help(void)
{
	fprintf(stdout,"fits_normalise scales the values in the associated fits image so the mean pixel value is 1,\n"
		"and writes an output float FITS image.\n");
	fprintf(stdout,"fits_normalise [-help] -i[nput] <FITS filename> -o[utput] <FITS filename>\n");
	fprintf(stdout,"-help prints this help message and exits.\n");
	fprintf(stdout,"You must always specify a filename to process.\n");
}

/**
 * Load the FITS image into a float array.
 * @return TRUE on success, FALSE on failure.
 * @see #Input_Filename
 * @see #Naxis1
 * @see #Naxis2
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
		fprintf(stderr,"fits_normalise: Wrong NAXIS value(%d).\n",integer_value);
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
	/*
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
	*/
	/* allocate image */
	Image_Data = (float *)malloc(Naxis1*Naxis2*sizeof(float));
	if(Image_Data == NULL)
	{
		fprintf(stderr,"fits_normalise: failed to allocate memory (%d,%d).\n",
			Naxis1,Naxis2);
		fits_close_file(fits_fp,&status);
		return FALSE;
	}
	/* read data */
	retval = fits_read_img(fits_fp,TFLOAT,1,Naxis1*Naxis2,NULL,Image_Data,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(fits_fp,&status);
		fprintf(stderr,"fits_normalise:fits_read_img:Failed to read FITS (%d,%d).\n",Naxis1,Naxis2);
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

/**
 * Read the Image_Data and set the Minimum_Value and Maximum_Value as appropriate.
 * @return TRUE on success, FALSE on failure.
 * @see #Image_Data
 * @see #Naxis1
 * @see #Naxis2
 * @see #Minimum_Value
 * @see #Maximum_Value
 */
/*
static int Get_Min_Max(void)
{
	int i;

	Maximum_Value = -1;
	Minimum_Value = 99999;
	for(i=0;i<(Naxis1*Naxis2);i++)
	{
		if(Image_Data[i] < Minimum_Value)
			Minimum_Value = Image_Data[i];
		if(Image_Data[i] > Maximum_Value)
			Maximum_Value = Image_Data[i];
	}
	return TRUE;
}
*/

/**
 * Read the Image_Data and set the Mean as appropriate.
 * @return TRUE on success, FALSE on failure.
 * @see #Image_Data
 * @see #Naxis1
 * @see #Naxis2
 * @see #Mean
 */
static int Get_Mean(void)
{
	double total_value;
	int i;

	total_value = 0.0;
	for(i=0;i<(Naxis1*Naxis2);i++)
	{
		total_value += (double)(Image_Data[i]);
	}
	Mean = (float)(total_value/((double)(Naxis1*Naxis2)));
	return TRUE;
}

/**
 * Normalise the Image_Data by dividing through by the Mean, so the mean value of the image is 1.
 * @return TRUE on success, FALSE on failure.
 * @see #Image_Data
 * @see #Naxis1
 * @see #Naxis2
 * @see #Mean
 */
static int Normalise_Image_Data(void)
{
	int i;


	for(i=0;i<(Naxis1*Naxis2);i++)
	{
		Image_Data[i] = (Image_Data[i]/Mean);
	}
	return TRUE;
}

/**
 * Save the modified Image_Data, as a FLOAT type FITS file.
 * @see #Output_Filename
 * @see #Image_Data
 * @see #Naxis1
 * @see #Naxis2
 */
static int Save(void)
{
	fitsfile *fits_fp = NULL;
	int retval=0,cfitsio_status=0,ivalue;
	long axes_list[2];
	cfitsio_status=0;

/* open output files */
	retval = fits_create_file(&fits_fp,Output_Filename,&cfitsio_status);
	if(retval)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"Save: Create %s for output failed.\n",Output_Filename);
		return FALSE;
	}
	/* create image */
	axes_list[0] = Naxis1;
	axes_list[1] = Naxis2;
	retval = fits_create_img(fits_fp,FLOAT_IMG,2,axes_list,&cfitsio_status);
	if(retval != 0)
	{
		fits_report_error(stderr,cfitsio_status);
		fits_close_file(fits_fp,&cfitsio_status);
		fprintf(stderr,"Save:fits_create_img failed.\n");
		return FALSE;
	}
	/* write image */
	retval = fits_write_img(fits_fp,TFLOAT,1,Naxis1*Naxis2,Image_Data,&cfitsio_status);
	if(retval != 0)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"Save:fits_write_img failed.\n");
		fits_close_file(fits_fp,&cfitsio_status);
		return FALSE;
	}	

	/* close file */
	retval = fits_close_file(fits_fp,&cfitsio_status);
	if(retval)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"Save:fits_close_file failed.\n");
		return FALSE;
	}
	return TRUE;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/06/21 13:05:29  cjm
** Rewritten to normalise so mean of image is 1.0.
**
** Revision 1.2  2006/05/16 18:24:54  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2006/05/10 14:52:05  cjm
** Initial revision
**
*/
