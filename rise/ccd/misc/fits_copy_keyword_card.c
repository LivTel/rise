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
/* fits_copy_keyword_card.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_copy_keyword_card.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_copy_keyword_card &lt;Input FITS filename&gt; &lt;keyword&gt; &lt;Output FITS filename&gt; 
 * </pre>
 * fits_copy_keyword_card reads the FITS file. It copies the specified keyword from the input FITS file to the output
 * FITS file.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include "fitsio.h"

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_copy_keyword_card.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments. This should be 5.
 * @param argv The arguments.
 */
int main(int argc,char *argv[])
{
	fitsfile *input_fp = NULL;
	fitsfile *output_fp = NULL;
	char *input_filename = NULL;
	char *output_filename = NULL;
	char *keyword = NULL;
	char card[81];
	int retval=0,status=0;

/* check arguments */
	if(argc != 4)
	{
		fprintf(stderr,"fits_copy_keyword_cardy <Input FITS filename> <keyword> <Output FITS filename>\n");
		fprintf(stderr,"Copy the specified keyword from the input FITS file to the output FITS file.\n");
		fprintf(stderr,"Keyword is a valid FITS keyword, to update/add.\n");
		return 1;
	}
	input_filename = argv[1];
	keyword = argv[2];
	output_filename = argv[3];
/* open files */
	retval = fits_open_file(&input_fp,input_filename,READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
	retval = fits_open_file(&output_fp,output_filename,READWRITE,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(input_fp,&status);
		return 2;
	}
/* get card */
	retval = fits_read_card(input_fp,keyword,card,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(input_fp,&status);
		fits_close_file(output_fp,&status);
		return 5;
	}
/* write card. If the keyword already exists, it's value is updated. */
	retval = fits_update_card(output_fp,keyword,card,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(input_fp,&status);
		fits_close_file(output_fp,&status);
		return 5;
	}
/* close files */
	retval = fits_close_file(output_fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		fits_close_file(input_fp,&status);
		return 6;
	}
	retval = fits_close_file(input_fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 6;
	}
	return 0;
}
