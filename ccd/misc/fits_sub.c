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
/* fits_sub.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_sub.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_sub &lt;Input FITS filename&gt; &lt;Subtraction  FITS filename&gt; &lt;Output FITS filename&gt; 
 * </pre>
 * fits_sub subtracts the data values in one filename away from another.
 * The program only supports SIMPLE FITS files NAXIS = 2. It checks NAXIS1 and NAXIS2
 * to get image dimensions from both input files and gives an error if they are different sizes.
 * It scales the input data values using BZERO and BSCALE (automatically via CFITSIO).
 * @see #main
 */
#include <stdio.h>
#include "fitsio.h"

/* hash definitions */
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_GET_DATA_NAXIS		(2)
/**
 * Number of input FITS files.
 */
#define INPUT_FILE_COUNT		(2)
/**
 * Number of output FITS files.
 */
#define OUTPUT_FILE_COUNT		(1)
/**
 * Total Number of FITS files. The result of adding INPUT_FILE_COUNT and OUTPUT_FILE_COUNT.
 */
#define	FILE_COUNT			(INPUT_FILE_COUNT+OUTPUT_FILE_COUNT)

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_sub.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * List of FITS file pointers.
 * @see #FILE_COUNT
 */
static fitsfile *Fits_Fp_List[FILE_COUNT];
/**
 * List of FITS image dimensions for input files.
 * @see #INPUT_FILE_COUNT
 */
static int Fits_Axis_List[INPUT_FILE_COUNT][2];
/**
 * List of FITS image data each file. The data is for one row only.
 * @see #FILE_COUNT
 */
static int *Fits_Data_List[FILE_COUNT];

/* internal functions */
static int Open_Input(char *argv[]);
static int Open_Output(char *filename);
static int Close(void);
static int Check_BitPix(void);
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
 * @param argc The number of arguments. This should be 4.
 * @param argv The arguments.
 */
int main(int argc,char *argv[])
{
	int i,j;

/* check arguments */
	if(argc != 4)
	{
		fprintf(stderr,"fits_sub <Input FITS filename> <Subtraction FITS filename> <Output FITS filename>.\n");
		return 1;
	}
/* initialise all files to NULL */
	for(i=0;i<FILE_COUNT;i++)
		Fits_Fp_List[i] = NULL;
/* open input file */
	if(!Open_Input(argv))
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
/* open output file */
	if(!Open_Output(argv[3]))
	{
		Free_Data();
		Close();
		return 6;
	}
/* for each row in the data */
	for(j=0;j<Fits_Axis_List[0][1];j++)
	{
	/* read data from each input file */
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
 * Opens the input FITS files, using the filename specified in argv.
 * It assumes argv[1] and argv[2] contain the two input filenames.
 * @param argv A list of filename, passed into main.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 */
static int Open_Input(char *argv[])
{
	int retval=0,status=0,i,j;

/* open input files */
	for(i=0;i<INPUT_FILE_COUNT;i++)
	{
		retval = fits_open_file(&(Fits_Fp_List[i]),argv[i+1],READONLY,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			fprintf(stderr,"fits_sub: Open %s failed.\n",argv[i+1]);
			Close();
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Opens the output FITS files, using the filename specified. Also setups BITPIX, NAXIS, NAXIS1 and
 * NAXIS2 keywords using Fits_Axis_List.
 * @param filename The name of the output file, passed into main.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Fits_Axis_List
 */
static int Open_Output(char *filename)
{
	int retval=0,status=0,ivalue;
	long axes_list[2];
	double dvalue;

/* open output files */
	retval = fits_create_file(&(Fits_Fp_List[INPUT_FILE_COUNT]),filename,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_sub: Create %s for output failed.\n",filename);
		return FALSE;
	}
/* copy the input files (0) header to the output */
	retval = fits_copy_header(Fits_Fp_List[0],Fits_Fp_List[INPUT_FILE_COUNT],&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_sub: Copy header to %s failed.\n",filename);
		return FALSE;
	}
/* ensure the simple keywords are correct */
/* SIMPLE */
	ivalue = TRUE;
	retval = fits_update_key(Fits_Fp_List[INPUT_FILE_COUNT],TLOGICAL,(char*)"SIMPLE",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"SIMPLE keyword.\n");
		return FALSE;
	}
/* BITPIX */
	ivalue = 16;
	retval = fits_update_key(Fits_Fp_List[INPUT_FILE_COUNT],TINT,(char*)"BITPIX",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BITPIX keyword.\n");
		return FALSE;
	}
/* NAXIS */
	ivalue = 2;
	retval = fits_update_key(Fits_Fp_List[INPUT_FILE_COUNT],TINT,(char*)"NAXIS",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS keyword.\n");
		return FALSE;
	}
/* NAXIS1 */
	ivalue = Fits_Axis_List[0][0];
	retval = fits_update_key(Fits_Fp_List[INPUT_FILE_COUNT],TINT,(char*)"NAXIS1",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS1 keyword = %d.\n",ivalue);
		return FALSE;
	}
/* NAXIS2 */
	ivalue = Fits_Axis_List[0][1];
	retval = fits_update_key(Fits_Fp_List[INPUT_FILE_COUNT],TINT,(char*)"NAXIS2",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS2 keyword = %d.\n",ivalue);
		return FALSE;
	}
/* BZERO */
	dvalue = 32768.0;
	retval = fits_update_key_fixdbl(Fits_Fp_List[INPUT_FILE_COUNT],"BZERO",dvalue,6,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BZERO keyword = %.2f.\n",dvalue);
		return FALSE;
	}
/* BSCALE */
	dvalue = 1.0;
	retval = fits_update_key_fixdbl(Fits_Fp_List[INPUT_FILE_COUNT],"BSCALE",dvalue,6,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BSCALE keyword = %.2f.\n",dvalue);
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine checks that each FITS input file has the same number of axes (FITS_GET_DATA_NAXIS),
 * stores the dimension of the 2 axes in Fits_Axis_List, and the dimensions are the
 * same for each input file.
 * @return The routine returns TRUE if it was successfull, FALSE if a failure occurs, all input files
 * 	did not have two axes or the dimensions of each input file were not the same. An error
 * 	is printed if FALSE is returned.
 * @see #FITS_GET_DATA_NAXIS
 * @see #Fits_Axis_List
 */
static int Get_Axes(void)
{
	int retval=0,status=0,i,integer_value;

	for(i=0;i<INPUT_FILE_COUNT;i++)
	{
	/* check naxis */
		retval = fits_read_key(Fits_Fp_List[i],TINT,"NAXIS",&integer_value,NULL,&status);
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
		retval = fits_read_key(Fits_Fp_List[i],TINT,"NAXIS1",&(Fits_Axis_List[i][0]),NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
		retval = fits_read_key(Fits_Fp_List[i],TINT,"NAXIS2",&(Fits_Axis_List[i][1]),NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
	}/* end for */
/* go though 1..INPUT_FILE_COUNT input files and compare dimensions to first */
	for(i=1;i<INPUT_FILE_COUNT;i++)
	{
		if((Fits_Axis_List[i][0] != Fits_Axis_List[0][0])||(Fits_Axis_List[i][1] != Fits_Axis_List[0][1]))
		{
			fprintf(stderr,"Get_Axes: Axes %d (%d,%d) do not match the first (%d,%d).\n",
				i,Fits_Axis_List[i][0],Fits_Axis_List[i][1],
				Fits_Axis_List[0][0],Fits_Axis_List[0][1]);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Routine to allocate memory, for all the input files and output file. The files have one 
 * row allocated only.
 * Fits_Data_List is allocated, using the dimension Fits_Axis_List[0][0].
 * The allocated memeory should be freed using Free_Data.
 * @return The routine returns TRUE if the allocations are completed successfully.
 * 	The routine returns FALSE if an allocation fails.
 * @see #Fits_Data_List
 * @see #Fits_Axis_List
 * @see #Free_Data
 */
static int Allocate_Data(void)
{
	int i;

/* initialse pointers to NULL */
	for(i=0;i<FILE_COUNT;i++)
		Fits_Data_List[i] = NULL;
/* allocate */
	for(i=0;i<FILE_COUNT;i++)
	{
		Fits_Data_List[i] = (int *)malloc(Fits_Axis_List[0][0]*sizeof(int));
		if(Fits_Data_List[i] == NULL)
		{
			fprintf(stderr,"fits_sub: failed to allocate memory (%d,%d).\n",
				i,Fits_Axis_List[0][0]);
			Free_Data();
			return FALSE;
		}
	} /* end for */
	return TRUE;
}

/**
 * This routine reads in one line of data from the two input files. The data is put into Fits_Data_List.
 * @param row_number The number of the row to retrieve, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data_List
 */
static int Read_Data(int row_number)
{
	int retval=0,status=0,i,j;
	long start_pixel;

	for(i=0;i<INPUT_FILE_COUNT;i++)
	{
		start_pixel = (Fits_Axis_List[i][0]*row_number)+1;/* CFITSIO image offsets start from 1 */
		retval = fits_read_img(Fits_Fp_List[i],TINT,start_pixel,Fits_Axis_List[i][0],NULL,Fits_Data_List[i],
			NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			fprintf(stderr,"Read_Data:fits_read_img:%d from %d to %d.\n",i,start_pixel,
				Fits_Axis_List[i][0]);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Routine to subtract the Data value in Fits_Data_List[1][i] away from Fits_Data_List[0][i] and to put the 
 * results in Fits_Data_List[2][i]. This is done for one row of data (i = 0..Fits_Axis_List[0][0]).
 * @param y The row we are currently processing, used for messages only.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 */
static int Data_Sub(int y)
{
	int i,value,value1,value2;

	for(i=0;i<Fits_Axis_List[0][0];i++)
	{
		value1 = Fits_Data_List[0][i];
		value2 = Fits_Data_List[1][i];
		value = value1-value2;
	/* check for over/underflow and replace with minimum/maximum value */
	/* output file is BITPIX 16, BSCALE 1 and BZERO 32768, i.e. 16 bit unsigned integers. */
		if(value < 0)
		{
			Fits_Data_List[2][i] = 0;
			fprintf(stderr,"Underflow at (%d,%d) from %d-%d.\n",i,y,value1,value2);
		}
		else if(value > ((1<<16)-1))
		{
			Fits_Data_List[2][i] = ((1<<16)-1);
			fprintf(stderr,"Overflow at (%d,%d) from %d-%d.\n",i,y,value1,value2);
		}
		else
			Fits_Data_List[2][i] = value;
	}
	return TRUE;
}

/**
 * This routine writes one line of data from Fits_Data_List[2][i] to the output file.
 * @param row_number The number of the row to write, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Fits_Data_List
 */
static int Write_Data(int row_number)
{
	int retval=0,status=0,i;
	long start_pixel;

	start_pixel = (Fits_Axis_List[0][0]*row_number)+1; /* CFITSIO image offsets start from 1 */
	retval = fits_write_img(Fits_Fp_List[INPUT_FILE_COUNT],TINT,start_pixel,Fits_Axis_List[0][0],
		Fits_Data_List[INPUT_FILE_COUNT],&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Write_Data:fits_write_img:%d from %d to %d.\n",row_number,start_pixel,
			Fits_Axis_List[0][0]);
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
	int i;

	for(i=0;i<FILE_COUNT;i++)
	{
		if(Fits_Data_List[i] != NULL)
			free(Fits_Data_List[i]);
		Fits_Data_List[i] = NULL;
	}
	return TRUE;
}

/**
 * Closes the FITS files, using Fits_Fp_List.
 * It only closes FITS files in Fits_Fp_List that are non-null, i.e. have been successfully opened.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 */
static int Close(void)
{
	int retval=0,status=0,i;

	for(i=0;i<FILE_COUNT;i++)
	{
		if(Fits_Fp_List[i] != NULL)
		{
			retval = fits_close_file(Fits_Fp_List[i],&status);
			Fits_Fp_List[i] = NULL;
			if(retval)
			{
				fits_report_error(stderr,status);
				return FALSE;
			}
		}
	}
	return TRUE;
}

/*
** $Log: not supported by cvs2svn $
** Revision 0.6  2006/05/16 18:27:38  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.5  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 0.4  2002/11/07 19:48:48  cjm
** Tried fixing numerous bugs. Un-tested checkin for ccd release.
**
** Revision 0.3  2001/07/10 19:17:48  cjm
** Fixed Linux compilation warnings.
**
** Revision 0.2  2001/03/02 18:15:18  cjm
** backup.
**
** Revision 0.1  2000/05/12 15:25:02  cjm
** initial revision.
**
*/
