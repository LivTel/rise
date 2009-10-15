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
/* targa.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/targa.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * Some routines that operate on the graphic file format called Targa. They operate on
 * type 2 Targa's i.e. 24 bit RGB TrueColour uncompressed Targas.
 */
#include <errno.h>
#include <stdio.h>
#include "targa.h"

/* hash definitions */
/**
 * The length of the error string.
 */
#define TARGA_ERROR_STRING_LENGTH 256

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: targa.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";
/**
 * Error string
 * @see #TARGA_ERROR_STRING_LENGTH
 */
static char Targa_Error_String[TARGA_ERROR_STRING_LENGTH] = "";
/**
 * Error Number.
 */
static int Targa_Error_Number = 0;

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Read a Targa (.tga) file from disc. Note this is a very simple implementation at the moment.
 * It only copes with 24bit uncompressed TrueColour (RGB) data (Targa type 2).
 * @param filename A non-NULL filename.
 * @param width A non-NULL pointer to an integer, to store the width of the image.
 * @param height A non-NULL pointer to an integer, to store the height of the image.
 * @param red_bits The address of a pointer to a unsigned char data element array. The 
 * 	pointer is set to an array of data allocated within this routine, and should be freed by free.
 * 	The array of data contains the red component of the pixel in the image. If the address was passed
 * 	in NULL, the array is not allocated.
 * @param green_bits The address of a pointer to a unsigned char data element array. The 
 * 	pointer is set to an array of data allocated within this routine, and should be freed by free.
 * 	The array of data contains the green component of the pixel in the image. If the address was passed
 * 	in NULL, the array is not allocated.
 * @param blue_bits The address of a pointer to a unsigned char data element array. The 
 * 	pointer is set to an array of data allocated within this routine, and should be freed by free.
 * 	The array of data contains the blue component of the pixel in the image. If the address was passed
 * 	in NULL, the array is not allocated.
 * @return The routine returns TRUE if it suceeded, FALSE if it fails.
 */
int Targa_Read(char *filename,int *width,int *height,
		unsigned char **red_bits,unsigned char **green_bits,unsigned char **blue_bits)
{
	FILE *fp = NULL;
	int i,retval,local_errno,pixel_count;
	int id_length, colourmap_type, image_type, x_origin, y_origin, pixel_depth, image_descriptor;

/* check parameters */
	if(filename == NULL)
	{
		Targa_Error_Number = 1;
		sprintf(Targa_Error_String,"Targa_Read:Filename was NULL.");
		return FALSE;
	}
	if((width == NULL)||(height == NULL))
	{
		Targa_Error_Number = 2;
		sprintf(Targa_Error_String,"Targa_Read:Width/Height was NULL(%p,%p).",width,height);
		return FALSE;
	}
/* open filename for binary read */
	fp = fopen(filename, "rb");
	if(fp == NULL)
	{
		Targa_Error_Number = 3;
		sprintf(Targa_Error_String,"Targa_Read:Failed to open %s.",filename);
		return FALSE;
	}
/* Read TGA image header */
	id_length = fgetc(fp);
	colourmap_type = fgetc(fp);
	if(colourmap_type != 0)
	{
		fclose(fp);
		Targa_Error_Number = 4;
		sprintf(Targa_Error_String,"Targa_Read:Colourmap type %d not supported for %s.",
			colourmap_type,filename);
		return FALSE;
	}
	image_type = fgetc(fp);
	if(image_type != 2)
	{
		fclose(fp);
		Targa_Error_Number = 5;
		sprintf(Targa_Error_String,"Targa_Read:Image type %d not supported for %s.",image_type,filename);
		return FALSE;
	}
/* Colour map specification - 5 bytes */
	for(i=0; i < 5; i++)
		fgetc(fp);
	x_origin = fgetc(fp);
	x_origin += fgetc(fp)*256;

	y_origin = fgetc(fp);
	y_origin += fgetc(fp)*256;
	(*width) = fgetc(fp);
	(*width) += fgetc(fp)*256;
	(*height) = fgetc(fp);
	(*height) += fgetc(fp)*256;
	pixel_depth = fgetc(fp);
	if(pixel_depth != 24)
	{
		fclose(fp);
		Targa_Error_Number = 6;
		sprintf(Targa_Error_String,"Targa_Read:Pixel Depth %d not supported for %s.",pixel_depth,filename);
		return FALSE;
	}
	image_descriptor = fgetc(fp);
	if(image_descriptor != 32) /* Bitmask, pertinent bit: top-down raster */
	{
		fclose(fp);
		Targa_Error_Number = 7;
		sprintf(Targa_Error_String,"Targa_Read:Image Descriptor %d not supported for %s.",image_descriptor,
			filename);
		return FALSE;
	}
/* read image id */
	for(i=0; i< id_length; i++)
		fgetc(fp);
/* allocate data memory */
	pixel_count = ((*width)*(*height));
	if(red_bits != NULL)
	{
		(*red_bits) = (unsigned char *)malloc(pixel_count*sizeof(unsigned char));
		if((*red_bits) == NULL)
		{
			fclose(fp);
			Targa_Error_Number = 8;
			sprintf(Targa_Error_String,"Targa_Read:Allocating Red Image data failed(%d,%d,%d,%s).",
				(*width),(*height),pixel_count,filename);
			return FALSE;
		}
	}
	if(green_bits != NULL)
	{
		(*green_bits) = (unsigned char *)malloc(pixel_count*sizeof(unsigned char));
		if((*green_bits) == NULL)
		{
			fclose(fp);
			Targa_Error_Number = 9;
			sprintf(Targa_Error_String,"Targa_Read:Allocating Green Image data failed(%d,%d,%d,%s).",
				(*width),(*height),pixel_count,filename);
			return FALSE;
		}
	}
	if(blue_bits != NULL)
	{
		(*blue_bits) = (unsigned char *)malloc(pixel_count*sizeof(unsigned char));
		if((*blue_bits) == NULL)
		{
			fclose(fp);
			Targa_Error_Number = 10;
			sprintf(Targa_Error_String,"Targa_Read:Allocating Blue Image data failed(%d,%d,%d,%s).",
				(*width),(*height),pixel_count,filename);
			return FALSE;
		}
	}
/* read data */
	for(i = 0; i < pixel_count; i++)
	{
		retval = fgetc(fp);
		if(blue_bits != NULL)
			(*blue_bits)[i] = (unsigned char)retval;
		retval = fgetc(fp);
		if(green_bits != NULL)
			(*green_bits)[i] = (unsigned char)retval;
		retval = fgetc(fp);
		if(red_bits != NULL)
			(*red_bits)[i] = (unsigned char)retval;
	}
/* close file */
	retval = fclose(fp);
	if(retval != 0)
	{
		local_errno = errno;
		Targa_Error_Number = 11;
		sprintf(Targa_Error_String,"Targa_Read:Failed to close %s(%d).",filename,local_errno);
		return FALSE;
	}
	return TRUE;
}

/**
 * Write a targa (.tga) file to disc.
 * @param filename The filename.
 * @param width The image witdth, should be positive.
 * @param height The image height, should be positive.
 * @param red_bits The red component to save, should be of length (width*height). It can be NULL, in which case
 * 	zero is written for all red components.
 * @param green_bits The green component to save, should be of length (width*height). It can be NULL, in which case
 * 	zero is written for all green components.
 * @param blue_bits The blue component to save, should be of length (width*height). It can be NULL, in which case
 * 	zero is written for all blue components.
 * @return TRUE if the write suceeded, FALSE if it failed.
 */
int Targa_Write(char *filename,int width,int height,
		unsigned char *red_bits,unsigned char *green_bits,unsigned char *blue_bits)
{
	FILE *fp = NULL;
	int i,retval,local_errno,value;

/* check parameters */
	if(filename == NULL)
	{
		Targa_Error_Number = 12;
		sprintf(Targa_Error_String,"Targa_Write:Filename was NULL.");
		return FALSE;
	}
	if((red_bits == NULL)&& (green_bits == NULL)&&(blue_bits == NULL))
	{
		Targa_Error_Number = 13;
		sprintf(Targa_Error_String,"Targa_Write:Image data was NULL.");
		return FALSE;
	}
	if((width < 1)||(height < 1))
	{
		Targa_Error_Number = 14;
		sprintf(Targa_Error_String,"Targa_Write:Illegal image dimensions(%d,%d).",width,height);
		return FALSE;
	}
/* open filename for binary write */
	fp = fopen(filename, "wb");
	if(fp == NULL)
	{
		Targa_Error_Number = 15;
		sprintf(Targa_Error_String,"Targa_Write:Failed to open %s.",filename);
		return FALSE;
	}
/* Write TGA image header */
	for (i = 0; i < 10; i++)      /* 00, 00, 02, then 7 00's... */
	{
		if (i == 2)
			putc((short)i, fp);
		else
			putc(0, fp);
	}
	putc(0, fp); /* y origin set to "First_Line" */
	putc(0, fp);
	putc((short)(width % 256), fp);  /* write width and height */
	putc((short)(width / 256), fp);
	putc((short)(height % 256), fp);
	putc((short)(height / 256), fp);
	putc(24, fp);  /* 24 bits/pixel (16 million colors!) */
	putc(32, fp);  /* Bitmask, pertinent bit: top-down raster */

	for(i = 0; i < (width *height); i++)
	{
	/* blue */
		if(blue_bits != NULL)
			value = (int)(blue_bits[i]);
		else
			value = 0;
		putc(value,fp);
	/* green */
		if(green_bits != NULL)
			value = (int)(green_bits[i]);
		else
			value = 0;
		putc(value,fp); 
	/* red */
		if(red_bits != NULL)
			value = (int)(red_bits[i]);
		else
			value = 0;
		putc(value,fp); /* Red */
	}
	retval = fclose(fp);
	if(retval != 0)
	{
		local_errno = errno;
		Targa_Error_Number = 16;
		sprintf(Targa_Error_String,"Targa_Write:Failed to close %s.",filename);
		return FALSE;
	}
	return TRUE;
}

/**
 * Simple error reporting routine.
 * @param fp The file to print the error to.
 * @see #Targa_Error_Number
 * @see #Targa_Error_String
 */
void Targa_Error(FILE *fp)
{
	fprintf(fp,"Targa Error (%d): %s.\n",Targa_Error_Number,Targa_Error_String);
}

/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:24:54  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2001/07/10 19:02:34  cjm
** Initial revision
**
*/
