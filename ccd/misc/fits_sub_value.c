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
/* fits_sub_value.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_sub_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_sub_value &lt;Input FITS filename&gt; &lt;number to subtract&gt;
 * </pre>
 * fits_sub subtracts a constant value from the data in the input filename.
 * It scales the input data values using BZERO and BSCALE (automatically via CFITSIO).
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include "fitsio.h"

#ifndef    errno
/**
 * Dodgy errno declaration. Probably not needed.
 */
extern int errno;
#endif

/* hash definitions */
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_GET_DATA_NAXIS		(2)

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_sub_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * FITS file pointer.
 */
static fitsfile *Fits_Fp;
/**
 * List of FITS image dimensions for input file.
 */
static int Fits_Axis_List[2];
/**
 * List of FITS image data. The data is for one row only.
 */
static int *Fits_Data;
/**
 * The constant value to subtract from each data value.
 */
static int Subtract_Value = 0;

/* internal functions */
static int Open_Input(char *filename);
static int Close(void);
static int Get_Axes(void);
static int Allocate_Data(void);
static int Free_Data(void);
static int Read_Data(int row_number);
static int Data_Sub(int y);
static int Write_Data(int row_number);

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments. This should be 3.
 * @param argv The arguments.
 */
int main(int argc,char *argv[])
{
	int i,j;

/* check arguments */
	if(argc != 3)
	{
		fprintf(stderr,"fits_sub <Input FITS filename> <number to subtract>.\n");
		return 1;
	}
/* initialise all files to NULL */
	Fits_Fp = NULL;
/* parse subtract value */
	Subtract_Value =  strtol(argv[2],NULL,10);
	if(errno != 0)
	{
		perror("Parsing subtraction value failed.");
		return 3;
	}
/* open input file */
	if(!Open_Input(argv[1]))
	{
		Close();
		return 2;
	}
/* get axes */
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
/* for each row in the data */
	for(j=0;j<Fits_Axis_List[1];j++)
	{
	/* read data from input file */
		if(!Read_Data(j))
		{
			Free_Data();
			Close();
			return 7;
		}
	/* calculate output values */
		if(!Data_Sub(j))
		{
			Free_Data();
			Close();
			return 8;
		}
/* write output data */
		if(!Write_Data(j))
		{
			Free_Data();
			Close();
			return 9;
		}
	} /* end for on rows */
/* free allocated memory *.
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
 * Opens the input FITS file, using the filename specified.
 * @param filename The FITS filename to open.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 */
static int Open_Input(char *filename)
{
	int retval=0,status=0,i,j;

/* open input files */
	retval = fits_open_file(&(Fits_Fp),filename,READWRITE,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_sub: Open %s failed.\n",filename);
		Close();
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine stores the dimension of the 2 axes in Fits_Axis_List.
 * @return The routine returns TRUE if it was successfull, FALSE if a failure occurs, all input files
 * 	did not have two axes or the dimensions of each input file were not the same. An error
 * 	is printed if FALSE is returned.
 * @see #FITS_GET_DATA_NAXIS
 * @see #Fits_Axis_List
 */
static int Get_Axes(void)
{
	int retval=0,status=0,i,integer_value;

/* check naxis */
	retval = fits_read_key(Fits_Fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"fits_sub: Wrong NAXIS value(%d).\n",integer_value);
		return FALSE;
	}
/* get naxis1,naxis2 */
	retval = fits_read_key(Fits_Fp,TINT,"NAXIS1",&(Fits_Axis_List[0]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	retval = fits_read_key(Fits_Fp,TINT,"NAXIS2",&(Fits_Axis_List[1]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to allocate memory, for all the input files and output file. The files have one 
 * row allocated only.
 * Fits_Data is allocated, using the dimension Fits_Axis_List[0].
 * The allocated memeory should be freed using Free_Data.
 * @return The routine returns TRUE if the allocations are completed successfully.
 * 	The routine returns FALSE if an allocation fails.
 * @see #Fits_Data
 * @see #Fits_Axis_List
 * @see #Free_Data
 */
static int Allocate_Data(void)
{
	int i;

/* initialse pointers to NULL */
	Fits_Data = NULL;
/* allocate */
	Fits_Data = (int *)malloc(Fits_Axis_List[0]*sizeof(int));
	if(Fits_Data == NULL)
	{
		fprintf(stderr,"fits_sub: failed to allocate memory (%d).\n",
			Fits_Axis_List[0]);
		Free_Data();
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine reads in one line of data from the input file. The data is put into Fits_Data.
 * @param row_number The number of the row to retrieve, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data
 */
static int Read_Data(int row_number)
{
	int retval=0,status=0,j;
	long start_pixel;

	start_pixel = (Fits_Axis_List[0]*row_number)+1;/* CFITSIO image offsets start from 1 */
	retval = fits_read_img(Fits_Fp,TINT,start_pixel,Fits_Axis_List[0],NULL,Fits_Data,
		NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Read_Data:fits_read_img:from %d to %d.\n",start_pixel,Fits_Axis_List[0]);
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to subtract the Data Subtract_Value away from Fits_Data[i] and to put the 
 * results in Fits_Data[i]. This is done for one row of data (i = 0..Fits_Axis_List[0]).
 * @param y The row we are currently processing, used for messages only.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 */
static int Data_Sub(int y)
{
	int i;

	for(i=0;i<Fits_Axis_List[0];i++)
	{
		Fits_Data[i] = Fits_Data[i] - Subtract_Value;
	/* check for over/underflow and replace with minimum/maximum value */
		if(Fits_Data[i] < 0)
		{
			Fits_Data[i] = 0;
			fprintf(stderr,"Underflow at (%d,%d) from %d.\n",i,y,Fits_Data[i]);
		}
		else if(Fits_Data[i] > ((1<<16)-1))
		{
			Fits_Data[i] = ((1<<16)-1);
			fprintf(stderr,"Overflow at (%d,%d) from %d.\n",i,y,Fits_Data[i]);
		}
	}
	return TRUE;
}

/**
 * This routine writes one line of data from Fits_Data[i] to the output file.
 * @param row_number The number of the row to write, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data
 */
static int Write_Data(int row_number)
{
	int retval=0,status=0,i;
	long start_pixel;

	start_pixel = (Fits_Axis_List[0]*row_number)+1; /* CFITSIO image offsets start from 1 */
	retval = fits_write_img(Fits_Fp,TINT,start_pixel,Fits_Axis_List[0],
		Fits_Data,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Write_Data:fits_write_img:%d from %d to %d.\n",row_number,start_pixel,
			Fits_Axis_List[0]);
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to free data allocated by Allocate_Data. The routine only frees memory allocated
 * (the pointer is not NULL).
 * @return This routine always returns TRUE: it cannot fail.
 * @see #Allocate_Data
 * @see #Fits_Data_List
 */
static int Free_Data(void)
{
	if(Fits_Data != NULL)
		free(Fits_Data);
	Fits_Data = NULL;
	return TRUE;
}

/**
 * Closes the FITS files, using Fits_Fp.
 * It only closes FITS files in Fits_Fp that are non-null, i.e. have been successfully opened.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 */
static int Close(void)
{
	int retval=0,status=0;

	if(Fits_Fp != NULL)
	{
		retval = fits_close_file(Fits_Fp,&status);
		Fits_Fp = NULL;
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
	}
	return TRUE;
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:27:39  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2002/11/07 19:48:48  cjm
** Initial revision
**
*/
