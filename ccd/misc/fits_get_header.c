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
/* fits_get_header.c -*- mode: Fundamental;-*-
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_get_header.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_get_header &lt;FITS filename&gt;
 * </pre>
 * fits_get_header reads the FITS file headers and displays them.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include "fitsio.h"

/* hash definitions */
/**
 * The END keyword in the FITS file is the last header to print out.
 * We should stop when we reach this header otherwise CFITSIO complains about 
 * going over the ends of the headers.
 */
#define END_CARD_STRING 	"END"

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_get_header.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

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
	char card_string[81];
	int retval=0,status=0,done=0,i;

/* check arguments */
	if(argc != 2)
	{
		fprintf(stderr,"fits_get_header <FITS filename>.\n");
		return 1;
	}
/* open file */
	retval = fits_open_file(&fp,argv[1],READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
/* update key based on type */
	i = 1;
	done = 0;
	while((status == 0)&&(done == 0))
	{
		retval = fits_read_record(fp,i,card_string,&status);
		if(retval == 0)
		{
			fprintf(stdout,"%s\n",card_string);
		/* if we reach the END keyword, terminate the loop. */
			if(strncmp(card_string,END_CARD_STRING,strlen(END_CARD_STRING))==0)
				done = 1;
		}
		i++;
	}
/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 5;
	}
	return 0;
}
/*
** $Log: not supported by cvs2svn $
** Revision 0.4  2006/05/16 18:22:45  cjm
** gnuify: Added GNU General Public License.
**
** Revision 0.3  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 0.2  2001/02/01 12:07:13  cjm
** Added END_CARD_STRING to detect end of headers.
**
*/
