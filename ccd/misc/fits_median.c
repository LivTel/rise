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
/* fits_median.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_median.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_median -i &lt;Input FITS filename&gt; [-i &lt;Input FITS filename&gt;...] -o &lt;Output FITS filename&gt; 
 * </pre>
 * fits_median creates a new FITS image with the median value from the input files 
 * (suggest you specify at least three).
 * The program only supports SIMPLE FITS files NAXIS = 2. It checks NAXIS1 and NAXIS2
 * to get image dimensions from all the input files and gives an error if they are different sizes.
 * It output s median FITS image with BIXPIX FLOAT.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "fitsio.h"

/* hash definitions */
/**
 * This program only accepts FITS files with this number of axes.
 */
#define FITS_GET_DATA_NAXIS		(2)

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_median.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

/**
 * List of Input FITS filenames
 * @see #Input_Fits_Count
 */
static char **Input_Fits_Filename_List = NULL;
/**
 * List of FITS file pointers.
 * @see #Input_Fits_Count
 */
static fitsfile **Input_Fits_Fp_List = NULL;
/**
 * Number of input FITS files 
 * @see #Input_Fits_Filename_List
 * @see #Input_Fits_Fp_List
 */
static int Input_Fits_Count = 0;
/**
 * Dimensions of the FITS images. They must all be the same!
 */
static int Fits_Axes_List[2];
/**
 * List of FITS image data for each input file. The data is for one row only.
 * @see #Input_Fits_Count
 */
static float **Input_Fits_Data_List = NULL;

/**
 * Output FITS filename.
 */
static char *Output_Fits_Filename = NULL;
/**
 * Ouput file pointers.
 */
static fitsfile *Output_Fits_Fp = NULL;
/**
 * FITS image data for the output file. The data is for one row only.
 */
static float *Output_Fits_Data_List = NULL;
/**
 * List of pixel values to median. List allocated to length Input_Fits_Count.
 * @see #Input_Fits_Count
 */
static float *Median_Data_List = NULL;

/* internal functions */
static int Open_Input(void);
static int Get_Axes(void);
static int Open_Output(void);
static int Allocate_Data(void);
static int Read_Data(int row_number);
static int Median_Data(void);
static int Sort_Float_List(const void *p1, const void *p2);
static int Write_Data(int row_number);
static int Close(void);
static int Free_Data(void);
static int Parse_Arguments(int argc, char *argv[]);
static int String_List_Add(char **string_list[],int *string_list_count,char *string);
static void Help(void);

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments. This should be 4.
 * @param argv The arguments.
 * @see #Parse_Arguments
 * @see #Open_Input
 * @see #Get_Axes
 * @see #Open_Output
 * @see #Allocate_Data
 * @see #Read_Data
 * @see #Median_Data
 * @see #Write_Data
 * @see #Close
 * @see #Free_Data
 * @see #Fits_Axes_List
 */
int main(int argc,char *argv[])
{
	int y;

/* check arguments */
	if(!Parse_Arguments(argc,argv))
		return 1;
	/* open input */
	if(!Open_Input())
		return 2;
	if(!Get_Axes())
	{
		Close();
		return 3;
	}
	/* open output file, using dimensions from Get_Axes */
	if(!Open_Output())
	{
		Close();
		return 4;
	}
	/* allocate one rows worth of memory for each input and the output file */
	if(!Allocate_Data())
	{
		Close();
		return 4;
	}
	/* for each row in the fist image:
	** - Read that row for all the input files into the data array
	** - median each pixel in the list of input images and write the median to the output data list
	** - write the output data list to the output FITS image 
	*/
	for(y=0;y<Fits_Axes_List[1];y++)
	{
		if(!Read_Data(y))
		{
			Close();
			return 5;
		}
		if(!Median_Data())
		{
			Close();
			return 6;
		}
		if(!Write_Data(y))
		{
			Close();
			return 7;
		}
	}
	/* close input and output FITS images */
	if(!Close())
	{
		return 8;
	}
	/* free allocated memory */
	if(!Free_Data())
	{
		return 9;
	}
	return 0;
}

/* ---------------------------------------------------------------------------------
**	Internal routines.
** --------------------------------------------------------------------------------- */
/**
 * Open all the input FITS files
 * @see #Input_Fits_Fp_List
 * @see #Input_Fits_Count
 * @see #Input_Fits_Filename_List
 */
static int Open_Input(void)
{
	int i;
	int retval=0,cfitsio_status=0;

	Input_Fits_Fp_List = (fitsfile **)malloc(Input_Fits_Count*sizeof(fitsfile *));
	if(Input_Fits_Fp_List == NULL)
	{
		fprintf(stderr,"Failed to allocate input fits fp list(%d).\n",Input_Fits_Count);
		return FALSE;
	}
	/* initialise all pointers to NULL, in case of failure */
	for(i=0;i<Input_Fits_Count;i++)
		Input_Fits_Fp_List[i] = NULL;
	/* open each input FITS image */
	for(i=0;i<Input_Fits_Count;i++)
	{
		retval = fits_open_file(&(Input_Fits_Fp_List[i]),Input_Fits_Filename_List[i],READONLY,&cfitsio_status);
		if(retval)
		{
			fits_report_error(stderr,cfitsio_status);
			fprintf(stderr,"Open_Input: Open %s failed.\n",Input_Fits_Filename_List[i]);
			return FALSE;
		}
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
 * @see #Fits_Axes_List
 * @see #Input_Fits_Fp_List
 */
static int Get_Axes(void)
{
	int retval=0,status=0,i,integer_value,naxis1,naxis2;

	/* first input file */
	/* check naxis */
	retval = fits_read_key(Input_Fits_Fp_List[0],TINT,"NAXIS",&integer_value,NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	if(integer_value != FITS_GET_DATA_NAXIS)
	{
		fprintf(stderr,"Get_Axes: Wrong NAXIS value(%d).\n",integer_value);
		return FALSE;
	}
	/* get naxis1,naxis2 */
	retval = fits_read_key(Input_Fits_Fp_List[0],TINT,"NAXIS1",&(Fits_Axes_List[0]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	retval = fits_read_key(Input_Fits_Fp_List[0],TINT,"NAXIS2",&(Fits_Axes_List[1]),NULL,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return FALSE;
	}
	for(i=1;i<Input_Fits_Count;i++)
	{
	/* check naxis */
		retval = fits_read_key(Input_Fits_Fp_List[i],TINT,"NAXIS",&integer_value,NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
		if(integer_value != FITS_GET_DATA_NAXIS)
		{
			fprintf(stderr,"Get_Axes: Wrong NAXIS value(%d).\n",integer_value);
			return FALSE;
		}
	/* get naxis1,naxis2 */
		retval = fits_read_key(Input_Fits_Fp_List[i],TINT,"NAXIS1",&(naxis1),NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
		retval = fits_read_key(Input_Fits_Fp_List[i],TINT,"NAXIS2",&(naxis2),NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return FALSE;
		}
		/* check axes match the first one */
		if((naxis1 != Fits_Axes_List[0])||(naxis2 != Fits_Axes_List[1]))
		{
			fprintf(stderr,"Get_Axes: Axes %d (%d,%d) do not match the first (%d,%d).\n",
				i,naxis1,naxis2,Fits_Axes_List[0],Fits_Axes_List[1]);
			return FALSE;
		}
	}/* end for */
	return TRUE;
}

/**
 * Opens the output FITS files, using the filename specified. Also setup output image dimensions.
 * Assumes Get_AXes has filled in Fits_Axes_List correctly.
 * @param filename The name of the output file, passed into main.
 * @return Returns TRUE if everything opened OK, FALSE otherwise.
 * @see #Fits_Axes_List
 * @see #Output_Fits_Filename
 * @see #Output_Fits_Fp
 */
static int Open_Output(void)
{
	int retval=0,cfitsio_status=0,ivalue;
	long axes_list[2];
	double dvalue;

	cfitsio_status=0;
/* open output files */
	retval = fits_create_file(&Output_Fits_Fp,Output_Fits_Filename,&cfitsio_status);
	if(retval)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"Open_Output: Create %s for output failed.\n",Output_Fits_Filename);
		return FALSE;
	}
	/* create image (FLOAT) */
	axes_list[0] = (long)(Fits_Axes_List[0]);
	axes_list[1] = (long)(Fits_Axes_List[1]);
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
 * Routine to allocate memory, for all the input files and output file. The files have one 
 * row allocated only.
 * The allocated memeory should be freed using Free_Data.
 * @return The routine returns TRUE if the allocations are completed successfully.
 * 	The routine returns FALSE if an allocation fails.
 * @see #Input_Fits_Count
 * @see #Input_Fits_Data_List
 * @see #Output_Fits_Data_List
 * @see #Fits_Axes_List
 * @see #Median_Data_List
 * @see #Free_Data
 */
static int Allocate_Data(void)
{
	int i;

	/* allocate input list pointers */
	Input_Fits_Data_List = (float **)malloc(Input_Fits_Count*sizeof(float *));
	if(Input_Fits_Data_List == NULL)
	{
		fprintf(stderr,"Allocate_Data:Allocating Input_Fits_Data_List failed (%d).\n",Input_Fits_Count);
		return FALSE;
	}
/* initialse pointers to NULL */
	for(i=0;i<Input_Fits_Count;i++)
		Input_Fits_Data_List[i] = NULL;
/* allocate input data */
	for(i=0;i<Input_Fits_Count;i++)
	{
		Input_Fits_Data_List[i] = (float *)malloc(Fits_Axes_List[0]*sizeof(float));
		if(Input_Fits_Data_List[i] == NULL)
		{
			fprintf(stderr,"Allocate_Data: failed to allocate memory (%d,%d).\n",
				i,Fits_Axes_List[0]);
			return FALSE;
		}
	} /* end for */
/* allocate output data */
	Output_Fits_Data_List = (float *)malloc(Fits_Axes_List[0]*sizeof(float));
	if(Output_Fits_Data_List == NULL)
	{
		fprintf(stderr,"Allocate_Data: failed to allocate output memory (%d).\n",
			Fits_Axes_List[0]);
		return FALSE;
	}
	/* median data list */
	Median_Data_List = (float *)malloc(Input_Fits_Count*sizeof(float));
	if(Median_Data_List == NULL)
	{
		fprintf(stderr,"Allocate_Data: failed to allocate median data list (%d).\n",
			Input_Fits_Count);
		return FALSE;

	}
	return TRUE;
}

/**
 * This routine reads in one line of data from the input files. The data is put into Input_Fits_Data_List.
 * @param row_number The number of the row to retrieve, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Input_Fits_Data_List
 * @see #Input_Fits_Fp_List
 * @see #Input_Fits_Count
 */
static int Read_Data(int row_number)
{
	int retval=0,status=0,i,j;
	long start_pixel;

	for(i=0;i<Input_Fits_Count;i++)
	{
		start_pixel = (Fits_Axes_List[0]*row_number)+1;/* CFITSIO image offsets start from 1 */
		retval = fits_read_img(Input_Fits_Fp_List[i],TFLOAT,start_pixel,Fits_Axes_List[0],NULL,
				       Input_Fits_Data_List[i],NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			fprintf(stderr,"Read_Data:fits_read_img:%d from %d to %d.\n",i,start_pixel,
				Fits_Axes_List[0]);
			return FALSE;
		}
	}
	return TRUE;
}

/**
 * Take the median value for each pixel, of the data in Input_Fits_Data_List, 
 * and put the median into Output_Fits_Data_List.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Input_Fits_Count
 * @see #Input_Fits_Data_List
 * @see #Output_Fits_Data_List
 * @see #Median_Data_List
 * @see #Sort_Float_List
 */
static int Median_Data(void)
{
	int i,x,median_index;

	median_index = Input_Fits_Count/2;
	for(x=0;x<Fits_Axes_List[0];x++)
	{
		for(i=0;i<Input_Fits_Count;i++)
		{
			Median_Data_List[i] = Input_Fits_Data_List[i][x];
		}
		qsort(Median_Data_List,Input_Fits_Count,sizeof(float),Sort_Float_List);
		Output_Fits_Data_List[x] = Median_Data_List[median_index];
	}
	return TRUE;
}

/**
 * Float list sort comparator, for use with qsort.
 */
static int Sort_Float_List(const void *p1, const void *p2)
{
	float f1,f2;

	f1 = (float)(*(float*)p1);
	f2 = (float)(*(float*)p2);
	if(f1 > f2)
		return -1;
	else if(f2 > f1)
		return 1;
	else
		return 0;
}

/**
 * This routine writes one line of data from Output_Fits_Data_List to the output file.
 * @param row_number The number of the row to write, from 0 to NAXIS2.
 * @return The routine returns TRUE if the operation suceeded, FALSE if it failed.
 * @see #Output_Fits_Data_List
 * @see #Output_Fits_Fp
 */
static int Write_Data(int row_number)
{
	int retval=0,cfitsio_status=0,i;
	long start_pixel;

	start_pixel = (Fits_Axes_List[0]*row_number)+1; /* CFITSIO image offsets start from 1 */
	retval = fits_write_img(Output_Fits_Fp,TFLOAT,start_pixel,Fits_Axes_List[0],Output_Fits_Data_List,
				&cfitsio_status);
	if(retval)
	{
		fits_report_error(stderr,cfitsio_status);
		fprintf(stderr,"Write_Data:fits_write_img:%d from %d to %d.\n",row_number,start_pixel,
			Fits_Axes_List[0]);
		return FALSE;
	}
	return TRUE;
}

/**
 * Closes the FITS files, using Input_Fits_Fp_List.
 * It only closes FITS files in Input_Fits_Fp_List that are non-null, i.e. have been successfully opened.
 * @return Returns TRUE if everything closed OK, FALSE otherwise.
 * @see #Input_Fits_Count
 * @see #Input_Fits_Fp_List
 * @see #Output_Fits_Fp
 */
static int Close(void)
{
	int retval=0,status=0,i;

	/* close input */
	for(i=0;i<Input_Fits_Count;i++)
	{
		if(Input_Fits_Fp_List[i] != NULL)
		{
			retval = fits_close_file(Input_Fits_Fp_List[i],&status);
			Input_Fits_Fp_List[i] = NULL;
			if(retval)
			{
				fits_report_error(stderr,status);
				return FALSE;
			}
		}
	}
	/* close output */
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
 * Routine to free data allocated by Allocate_Data. The routine only frees memory allocated
 * (the pointer is not NULL).
 * @return This routine always returns TRUE: it cannot fail.
 * @see #Allocate_Data
 * @see #Input_Fits_Data_List
 * @see #Input_Fits_Count
 * @see #Output_Fits_Data_List
 */
static int Free_Data(void)
{
	int i;

	/* Input_Fits_Data_List */
	for(i=0;i<Input_Fits_Count;i++)
	{
		if(Input_Fits_Data_List[i] != NULL)
			free(Input_Fits_Data_List[i]);
		Input_Fits_Data_List[i] = NULL;
	}
	if(Input_Fits_Data_List != NULL)
		free(Input_Fits_Data_List);
	Input_Fits_Data_List = NULL;
	/* Output_Fits_Data_List */
	if(Output_Fits_Data_List != NULL)
		free(Output_Fits_Data_List);
	Output_Fits_Data_List = NULL;
	/* Median_Data_List */
	if(Median_Data_List != NULL)
		free(Median_Data_List);
	Median_Data_List = NULL;
	/* currently we don't free allocated memory
	** Input_Fits_Filename_List (2d)
	** Input_Fits_Fp_List
	*/

	return TRUE;
}

/**
 * Routine to parse command line arguments.
 * @param argc The number of arguments sent to the program.
 * @param argv An array of argument strings.
 * @see #Help
 * @see #Output_Fits_Filename
 * @see #Input_Fits_Filename_List
 * @see #Input_Fits_Count
 * @see #String_List_Add
 */
static int Parse_Arguments(int argc, char *argv[])
{
	int i,retval;

	/* no arguments - need help */
	if(argc == 1)
	{
		Help();
		exit(0);
	}
	for(i=1;i<argc;i++)
	{
		if((strcmp(argv[i],"-i")==0)||(strcmp(argv[i],"-input")==0))
		{
			if((i+1)<argc)
			{
				if(!String_List_Add(&Input_Fits_Filename_List,&Input_Fits_Count,argv[i+1]))
				{
					fprintf(stderr,"Parse_Arguments:Failed to add Input filename %s to list.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Input filename missing.\n");
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
				Output_Fits_Filename = strdup(argv[i+1]);
				if(Output_Fits_Filename == NULL)
				{
					fprintf(stderr,"Parse_Arguments:Failed to copy Output filename %s.\n",
						argv[i+1]);
					return FALSE;
				}
				i++;
			}
			else
			{
				fprintf(stderr,"Parse_Arguments:Output filename missing.\n");
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
 * Add a string to a list of strings.
 * @param string_list The address of a reallocatable list of strings.
 * @param string_list_count The address of an integer holding the number of strings in the lsit.
 * @param string The string to add.
 * @return The routine returns TRUE on success and FALSE on failure.
 */
static int String_List_Add(char **string_list[],int *string_list_count,char *string)
{
	if(string_list == NULL)
	{
		fprintf(stderr,"String_List_Add:string_list was NULL.\n");
		return FALSE;
	}
	if(string_list_count == NULL)
	{
		fprintf(stderr,"String_List_Add:string_list_count was NULL.\n");
		return FALSE;
	}
	if((*string_list) == NULL)
		(*string_list) = (char **)malloc(sizeof(char *));
	else
		(*string_list) = (char **)realloc((*string_list),((*string_list_count)+1)*sizeof(char *));
	if((*string_list) == NULL)
	{
		fprintf(stderr,"String_List_Add:Failed to reallocate string_list(%d).\n",(*string_list_count));
		return FALSE;
	}
	(*string_list)[(*string_list_count)] = strdup(string);
	if((*string_list)[(*string_list_count)] == NULL)
	{
		fprintf(stderr,"String_List_Add:Failed to copy string_list[%d] (%s).\n",(*string_list_count),string);
		return FALSE;
	}
	(*string_list_count)++;
	return TRUE;
}

/**
 * Help routine.
 */
static void Help(void)
{
	fprintf(stdout,"FITS Median:Help.\n");
	fprintf(stdout,"fits_median creates a new FITS image with the median value from the input files "
		"(suggest you specify at least three).\n");
	fprintf(stdout,"The resultant FITS file is of FLOAT type.\n");
	fprintf(stdout,"fits_median -i <Input FITS filename> [-i <Input FITS filename>...] -o <Output FITS filename> [-h[elp]]\n");
}



/*
** $Log: not supported by cvs2svn $
** Revision 1.2  2006/05/16 18:24:34  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2006/05/16 18:23:05  cjm
** Initial revision
**
*/
