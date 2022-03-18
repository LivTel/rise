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
/* targa.h
** $Header: /home/dev/src/ccd/misc/RCS/targa.h,v 1.1 2006/05/16 18:25:11 cjm Exp $
*/

#ifndef TARGA_H
#define TARGA_H
#include <stdio.h>

/**
 * TRUE is the value usually returned from routines to indicate success.
 */
#ifndef TRUE
#define TRUE 1
#endif
/**
 * FALSE is the value usually returned from routines to indicate failure.
 */
#ifndef FALSE
#define FALSE 0
#endif

extern int Targa_Read(char *filename,int *width,int *height,
		unsigned char **red_bits,unsigned char **green_bits,unsigned char **blue_bits);
extern int Targa_Write(char *filename,int width,int height,
		unsigned char *red_bits,unsigned char *green_bits,unsigned char *blue_bits);
extern void Targa_Error(FILE *fp);
#endif
