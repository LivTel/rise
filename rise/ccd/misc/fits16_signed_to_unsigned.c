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
/* fits16_signed_to_unsigned.c -*- mode: Fundamental;-*-
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits16_signed_to_unsigned.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits16_signed_to_unsigned &lt;Input FITS filename&gt; &lt;Output FITS filename&gt;
 * </pre>
 * fits16_signed_to_unsigned fixes the data values stored in a 16 bit image data array.
 * It does this by loading the data as signed, and saving as unsigned.
 * The program only supports SIMPLE FITS files with BITBIX = 16 and NAXIS = 2. 
 * On staru machines, compile as follows:
 * <pre>
 * cc -I/star/include/ -L/star/lib/ fits16_signed_to_unsigned.c -o fits16_signed_to_unsigned -lcfitsio -lm
 * </pre>
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
 * Input FITS file pointer.
 */
static fitsfile *Input_Fits_Fp = NULL;
/**
 * Input FITS file pointer.
 */
static fitsfile *Output_Fits_Fp = NULL;
/**
 * List of FITS image dimensions for input files.
 */
static int Fits_Axis[2];
/**
 * FITS image data.
 */
static signed short *Fits_Data = NULL;

/* internal functions */
static int Open_Input(char *filename);
static int Open_Output(char *filename);
static int Close(void);
static int Check_BitPix(void);
static int Get_Axes(void);
static int Allocate_Data(void);
static int Read_Data(void);
static int Write_Data(void);
static int Free_Data(void);
static void Get_Signed_Range(void);
static void Get_UnSigned_Range(void);

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
	int i,j;

/* check arguments */
	if(argc != 3)
	{
		fprintf(stderr,"fits16_signed_to_unsigned <Input FITS filename> <Output FITS filename>.\n");
		return 1;
	}
/* files to NULL */
	Input_Fits_Fp = NULL;
	Output_Fits_Fp = NULL;
/* open input file */
	if(!Open_Input(argv[1]))
	{
		Close();
		return 2;
	}
/* check bitpix */
	if(!Check_BitPix())
	{
		Close();
		return 3;
	}
	if(!Get_Axes())
	{
		Close();
		return 4;
	}
/* allocate data */
	if(!Allocate_Data())
	{
		Close();
		return 5;
	}
/* read data from each input file */
	if(!Read_Data())
	{
		Free_Data();
		Close();
		return 7;
	}
/* write some stats */
	Get_Signed_Range();
	Get_UnSigned_Range();
/* open output file */
	if(!Open_Output(argv[2]))
	{
		Close();
		return 2;
	}
/* write output data */
	if(!Write_Data())
	{
		Free_Data();
		Close();
		return 9;
	}
/* free allocated memory */
	if(!Free_Data())
	{
		Close();
		return 10;
	}
/* close file */
	if(!Close())
		return 11;
	return 0;
}

/**
 * Opens the input FITS file.
 * @param filename A valid FITS file to open.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 */
static int Open_Input(char *filename)
{
	int retval=0,status=0,i,j;

/* open input files */
	retval = fits_open_file(&Input_Fits_Fp,filename,READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits16_signed_to_unsigned: Open %s failed.\n",filename);
		return FALSE;
	}
	return TRUE;
}


/**
 * Opens the output FITS files, using the filename specified. 
 * Copy's the header from the input FITS file to the output file.
 * @param filename The name of the output file, passed into main.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Input_Fits_Fp
 * @see #Output_Fits_Fp
 * @see #Fits_Axis
 */
static int Open_Output(char *filename)
{
	int retval=0,status=0,ivalue;
	long axes_list[2];
	double dvalue;

/* open output files */
	retval = fits_create_file(&Output_Fits_Fp,filename,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits16_signed_to_unsigned: Create %s for output failed.\n",filename);
		return FALSE;
	}
/* copy input header */
	retval = fits_copy_header(Input_Fits_Fp,Output_Fits_Fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits16_signed_to_unsigned: Copy Header %s for output failed.\n",filename);
		return FALSE;
	}
	return TRUE;
}

/**
 * Checks BITPIX keyword is 16 bit. 
 * @return Returns TRUE if everything OK, FALSE otherwise.
 * @see #FITS_GET_DATA_BITPIX
 */
static int Check_BitPix(void)
{
	int retval=0,status=0,integer_value;

	retval = fits_read_key(Input_Fits_Fp,TINT,"BITPIX",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_BITPIX)
	{
		fprintf(stderr,"fits16_signed_to_unsigned: Wrong BITPIX value(%d).\n",integer_value);
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine stores the dimension of the 2 axes in Fits_Axis.
 * @return The routine returns TRUE if it was successfull, FALSE if a failure occurs. An error
 * 	is printed if FALSE is returned.
 * @see #FITS_GET_DATA_NAXIS
 * @see #Fits_Axis
 */
static int Get_Axes(void)
{
	int retval=0,status=0,i,integer_value;


/* check naxis */
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"fits16_signed_to_unsigned: Wrong NAXIS value(%d).\n",integer_value);
		return FALSE;
	}
/* get naxis1,naxis2 */
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS1",&(Fits_Axis[0]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS2",&(Fits_Axis[1]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to allocate memory, for the input file.
 * Fits_Data is allocated, using the dimension Fits_Axis[0]*Fits_Axis[1].
 * The allocated memory should be freed using Free_Data.
 * @return The routine returns TRUE if the allocations are completed successfully.
 * 	The routine returns FALSE if an allocation fails.
 * @see #Fits_Data
 * @see #Fits_Axis
 * @see #Free_Data
 */
static int Allocate_Data(void)
{
/* initialse pointers to NULL */
	Fits_Data = NULL;
/* allocate */
	Fits_Data = (signed short *)malloc(Fits_Axis[0]*Fits_Axis[1]*sizeof(signed short));
	if(Fits_Data == NULL)
	{
		fprintf(stderr,"fits16_signed_to_unsigned: failed to allocate memory (%d).\n",
			Fits_Axis[0]*Fits_Axis[1]);
		Free_Data();
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine reads in the data from the input file as SIGNED short. The data is put into Fits_Data.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data
 */
static int Read_Data(void)
{
	int retval=0,status=0;

	retval = fits_read_img(Input_Fits_Fp,TSHORT,1,Fits_Axis[0]*Fits_Axis[1],NULL,(signed short*)Fits_Data,
		NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Read_Data:fits_read_img:1 to %d.\n",Fits_Axis[0]*Fits_Axis[1]);
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine writes data from Fits_Data as UNSIGNED short to the output file.
 * It first changes BZERO to 32768 so the data is saved without an overflow
 * (16 bit FITS files are always signed, BZERO is used to store unsigned).
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data
 * @see #Output_Fits_Fp
 */
static int Write_Data(void)
{
	double dvalue;
	int retval=0,status=0;

/* BZERO */
	dvalue = 32768.0;
	retval = fits_update_key_fixdbl(Output_Fits_Fp,"BZERO",dvalue,6,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BZERO keyword = %.2f.\n",dvalue);
		return FALSE;
	}
/* BSCALE */
	dvalue = 1.0;
	retval = fits_update_key_fixdbl(Output_Fits_Fp,"BSCALE",dvalue,6,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BSCALE keyword = %.2f.\n",dvalue);
		return FALSE;
	}
/* write */
	retval = fits_write_img(Output_Fits_Fp,TUSHORT,1,Fits_Axis[0]*Fits_Axis[1],(unsigned short*)Fits_Data,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Write_Data:fits_write_img:1 to %d.\n",Fits_Axis[0]*Fits_Axis[1]);
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to free data allocated by Allocate_Data. The routine only frees memory allocated
 * (the pointer is not NULL).
 * @return This routine always returns TRUE: it cannot fail.
 * @see #Allocate_Data
 * @see #Fits_Data
 */
static int Free_Data(void)
{
	if(Fits_Data != NULL)
		free(Fits_Data);
	Fits_Data = NULL;
	return TRUE;
}

/**
 * Closes the FITS files, using Input_Fits_Fp/Output_Fits_Fp.
 * It only closes FITS files in Input_Fits_Fp/Output_Fits_Fp that are non-null, i.e. have been successfully opened.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 * @see #Input_Fits_Fp
 * @see #Output_Fits_Fp
 */
static int Close(void)
{
	int retval=0,status=0;

	if(Input_Fits_Fp != NULL)
	{
		retval = fits_close_file(Input_Fits_Fp,&status);
		Input_Fits_Fp = NULL;
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
	}
	if(Output_Fits_Fp != NULL)
	{
		retval = fits_close_file(Output_Fits_Fp,&status);
		Output_Fits_Fp = NULL;
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Print out the range of data values as if Fits_Data really contained signed data.
 * @see #Fits_Data
 */
static void Get_Signed_Range(void)
{
	signed short int *data=NULL,min_value,max_value;
	int count,i;

	data = (signed short int *)Fits_Data;
	count = Fits_Axis[0]*Fits_Axis[1];
	min_value = 32767;
	max_value = -32767;
	for(i=0;i<count;i++)
	{
		if(data[i] < min_value)
			min_value = data[i];
		if(data[i] > max_value)
			max_value = data[i];
	}
	fprintf(stdout,"Data Range (Signed):%hd to %hd\n",min_value,max_value);
}

/**
 * Print out the range of data values as if Fits_Data really contained unsigned data.
 * @see #Fits_Data
 */
static void Get_UnSigned_Range(void)
{
	unsigned short int *data=NULL,min_value,max_value;
	int count,i;

	data = (unsigned short int *)Fits_Data;
	count = Fits_Axis[0]*Fits_Axis[1];
	min_value = 65535;
	max_value = 0;
	for(i=0;i<count;i++)
	{
		if(data[i] < min_value)
			min_value = data[i];
		if(data[i] > max_value)
			max_value = data[i];
	}
	fprintf(stdout,"Data Range (UnSigned):%hu to %hu\n",min_value,max_value);
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.1  2006/05/16 18:22:37  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.0  2001/04/26 15:14:42  cjm
** Initial revision
**
*/
