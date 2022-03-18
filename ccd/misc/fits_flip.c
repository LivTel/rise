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
/* fits_flip.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_flip.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_flip -i &lt;Input FITS filename&gt; -o &lt;Output FITS filename&gt; [-x] [-y]
 * </pre>
 * fits_flip flips the image data around one or more axes.
 * The program only supports SIMPLE FITS files NAXIS = 2. It checks NAXIS1 and NAXIS2
 * to get image dimensions from both input files and gives an error if they are different sizes.
 * It scales the input data values using BZERO and BSCALE (automatically via CFITSIO).
 * @see #main
 */
#include <stdio.h>
#include <string.h>
#include "fitsio.h"

/* hash definitions */
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_GET_DATA_NAXIS		(2)
/**
 * Number of input FITS files.
 */
#define INPUT_FILE_COUNT		(1)
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
static char rcsid[] = "$Id: fits_flip.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Input FITS file pointer.
 */
static fitsfile *Input_Fits_Fp;
/**
 * Output FITS file pointer.
 */
static fitsfile *Output_Fits_Fp;
/**
 * FITS image dimensions for input file.
 */
static int Fits_Axis_List[2];
/**
 * Input filename.
 */
static char *Input_Filename = NULL;
/**
 * Output filename.
 */
static char *Output_Filename = NULL;
/**
 * Boolean flag, TRUE if flip around X axes.
 */
static int Flip_X = FALSE;
/**
 * Boolean flag, TRUE if flip around Y axes.
 */
static int Flip_Y = FALSE;
/**
 * FITS data.
 */
static unsigned short *Fits_Data = NULL;

/* internal functions */
static int Open_Input(void);
static int Open_Output(void);
static int Close(void);
static int Check_BitPix(void);
static int Get_Axes(void);
static int Allocate_Data(void);
static int Free_Data(void);
static int Read_Data(void);
static int Invert_Data(void);
static int Write_Data(void);
static int Parse_Arguments(int argc, char *argv[]);
static void Help(void);

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
/* check arguments */
	if(!Parse_Arguments(argc,argv))
		return 1;
/* check filenames */
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
/* open input file */
	if(!Open_Input())
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
	if(!Open_Output())
	{
		Free_Data();
		Close();
		return 6;
	}
	/* read data */
	if(!Read_Data())
	{
		Free_Data();
		Close();
		return 7;
	}
	/* calculate output values */
        if(!Invert_Data())
	{
		Free_Data();
		Close();
		return 8;
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
 * Opens the input FITS files, using Input_Filename.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Input_Filename
 * @see #Input_Fits_Fp
 */
static int Open_Input(void)
{
	int retval=0,status=0;

/* open input files */
       	retval = fits_open_file(&Input_Fits_Fp,Input_Filename,READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_flip: Open %s failed.\n",Input_Filename);
		Close();
		return FALSE;
	}
	return TRUE;
}

/**
 * Opens the output FITS files, using the filename specified. Also setups BITPIX, NAXIS, NAXIS1 and
 * NAXIS2 keywords using Fits_Axis_List.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Output_Filename
 * @see #Output_Fits_Fp
 * @see #Fits_Axis_List
 */
static int Open_Output(void)
{
	int retval=0,status=0,ivalue;
	long axes_list[2];
	double dvalue;

/* open output files */
	retval = fits_create_file(&Output_Fits_Fp,Output_Filename,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_flip: Create %s for output failed.\n",Output_Filename);
		return FALSE;
	}
/* copy the input files (0) header to the output */
	retval = fits_copy_header(Input_Fits_Fp,Output_Fits_Fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"fits_flip: Copy header to %s failed.\n",Output_Filename);
		return FALSE;
	}
/* ensure the simple keywords are correct */
/* SIMPLE */
	ivalue = TRUE;
	retval = fits_update_key(Output_Fits_Fp,TLOGICAL,(char*)"SIMPLE",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"SIMPLE keyword.\n");
		return FALSE;
	}
/* BITPIX */
	ivalue = 16;
	retval = fits_update_key(Output_Fits_Fp,TINT,(char*)"BITPIX",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"BITPIX keyword.\n");
		return FALSE;
	}
/* NAXIS */
	ivalue = 2;
	retval = fits_update_key(Output_Fits_Fp,TINT,(char*)"NAXIS",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS keyword.\n");
		return FALSE;
	}
/* NAXIS1 */
	ivalue = Fits_Axis_List[0];
	retval = fits_update_key(Output_Fits_Fp,TINT,(char*)"NAXIS1",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS1 keyword = %d.\n",ivalue);
		return FALSE;
	}
/* NAXIS2 */
	ivalue = Fits_Axis_List[1];
	retval = fits_update_key(Output_Fits_Fp,TINT,(char*)"NAXIS2",&ivalue,NULL,&status);
	if(retval != 0)
	{
		fits_report_error(stderr, status);
		fprintf(stderr,"NAXIS2 keyword = %d.\n",ivalue);
		return FALSE;
	}
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
	return TRUE;
}

/**
 * This routine checks that each FITS input file has the same number of axes (FITS_GET_DATA_NAXIS),
 * stores the dimension of the 2 axes in Fits_Axis_List.
 * @return The routine returns TRUE if it was successfull, FALSE if a failure occurs, the input file
 * 	did not have two axes. An error
 * 	is printed if FALSE is returned.
 * @see #FITS_GET_DATA_NAXIS
 * @see #Fits_Axis_List
 * @see #Input_Fits_Fp
 */
static int Get_Axes(void)
{
	int retval=0,status=0,integer_value;

	/* check naxis */
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"fits_flip: Wrong NAXIS value(%d).\n",integer_value);
		return FALSE;
	}
	/* get naxis1,naxis2 */
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS1",&(Fits_Axis_List[0]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	retval = fits_read_key(Input_Fits_Fp,TINT,"NAXIS2",&(Fits_Axis_List[1]),NULL,&status);
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
	Fits_Data = (unsigned short *)malloc(Fits_Axis_List[0]*Fits_Axis_List[1]*sizeof(unsigned short));
	if(Fits_Data == NULL)
	{
		fprintf(stderr,"fits_flip: failed to allocate memory (%d,%d).\n",
			Fits_Axis_List[0],Fits_Axis_List[1]);
		Free_Data();
		return FALSE;
	}
	return TRUE;
}

/**
 * This routine reads in the data array from the input file. The data is put into Fits_Data.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Input_Fits_Fp
 * @see #Fits_Axis_List
 * @see #Fits_Data
 */
static int Read_Data(void)
{
	int retval=0,status=0;

	retval = fits_read_img(Input_Fits_Fp,TUSHORT,1,Fits_Axis_List[0]*Fits_Axis_List[1],NULL,
			       Fits_Data,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Read_Data:fits_read_img:failed.\n");
		return FALSE;
	}
	return TRUE;
}

/**
 * Routine to invert data.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Flip_X
 * @see #Flip_Y
 */
static int Invert_Data(void)
{
	unsigned short value;
	int x,y,otherx,othery;

	if(Flip_Y)
	{
		for(y=0;y<(Fits_Axis_List[1]/2);y++)
		{
			othery = (Fits_Axis_List[1]-y)-1;
			for(x=0;x<Fits_Axis_List[0];x++)
			{
				value = Fits_Data[(y*Fits_Axis_List[0])+x];
				Fits_Data[(y*Fits_Axis_List[0])+x] = Fits_Data[(othery*Fits_Axis_List[0])+x];
				Fits_Data[(othery*Fits_Axis_List[0])+x] = value;
			}
		}
	}
	if(Flip_X)
	{
		for(y=0;y<Fits_Axis_List[1];y++)
		{
			for(x=0;x<(Fits_Axis_List[0]/2);x++)
			{
				otherx =  (Fits_Axis_List[0]-x)-1;
				value = Fits_Data[(y*Fits_Axis_List[0])+x];
				Fits_Data[(y*Fits_Axis_List[0])+x] = Fits_Data[(y*Fits_Axis_List[0])+otherx];
				Fits_Data[(y*Fits_Axis_List[0])+otherx] = value;
			}
		}
	}
	return TRUE;
}

/**
 * This routine writes the data from Fits_Data to the output file.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Output_Fits_Fp
 * @see #Fits_Data
 */
static int Write_Data(void)
{
	int retval=0,status=0;

	retval = fits_write_img(Output_Fits_Fp,TUSHORT,1,Fits_Axis_List[0]*Fits_Axis_List[1],
		Fits_Data,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fprintf(stderr,"Write_Data:fits_write_img.\n");
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
 * Closes the FITS files, using Input_Fits_Fp,Output_Fits_Fp.
 * It only closes FITS files that are non-null, i.e. have been successfully opened.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 */
static int Close(void)
{
	int retval=0,status=0,i;

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
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Input_Filename
 * @see #Output_Filename
 * @see #Flip_X
 * @see #Flip_Y
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i;

	Flip_X = FALSE;
	Flip_Y = FALSE;
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
				Input_Filename = strdup(argv[i+1]);
				i++;
			}
			else
				fprintf(stderr,"Parse_Arguments:"
					"Input filename missing.\n");
		}
		else if((strcmp(argv[i],"-o")==0)||(strcmp(argv[i],"-output")==0))
		{
			if((i+1)<argc)
			{
				Output_Filename = strdup(argv[i+1]);
				i++;
			}
			else
				fprintf(stderr,"Parse_Arguments:"
					"Output filename missing.\n");
		}
		else if(strcmp(argv[i],"-x")==0)
			Flip_X = TRUE;
		else if(strcmp(argv[i],"-y")==0)
			Flip_Y = TRUE;
		else
			fprintf(stderr,"Parse_Arguments:Illegal Argument %s",argv[i]);
	}
	return TRUE;
}

/**
 * Help routine.
 */
static void Help(void)
{
	fprintf(stdout,"FITS Flip:Help.\n");
	fprintf(stdout,"FITS Flip flips the image data in a fits file around one or more axes.\n");
	fprintf(stdout,"fits_flip -i[nput] <FITS filename> -o[utput] <FITS filename> "
			"[-x][-y][-help]\n");
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:22:42  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2003/02/19 12:42:02  cjm
** Fixed Flip X routine, it was flipping the top (y) half of the image twice!
**
** Revision 1.1  2002/12/09 14:38:19  cjm
** Initial revision
**
*/
