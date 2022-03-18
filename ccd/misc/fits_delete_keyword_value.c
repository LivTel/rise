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
/* fits_delete_keyword_value.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_delete_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_delete_keyword_value &lt;FITS filename&gt; &lt;keyword&gt;
 * </pre>
 * fits_delete_keyword_value reads the FITS file headers and deletes and cards with the specified keyword.
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
static char rcsid[] = "$Id: fits_delete_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

/* ---------------------------------------------------------------------------------
**	External routines.
** --------------------------------------------------------------------------------- */
/**
 * Main Program.
 * @param argc The number of arguments. This should be 2.
 * @param argv The arguments.
 * @see #BLANK_KEYWORD_STRING
 * @see #END_CARD_STRING
 */
int main(int argc,char *argv[])
{
	fitsfile *fp = NULL;
	char *filename = NULL;
	char *keyword = NULL;
	char card_string[81];
	int retval=0,status=0,done=0,i,count;

/* check arguments */
	if(argc != 3)
	{
		fprintf(stderr,"fits_delete_keyword_value <FITS filename> <keyword>.\n");
		return 1;
	}
	filename = argv[1];
	keyword = argv[2];
/* open file */
	retval = fits_open_file(&fp,filename,READONLY,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_keyword_value:failed to open filename %s.\n",filename);
		fits_report_error(stderr,status);
		return 2;
	}
	retval = fits_delete_key(fp,keyword,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_keyword_value:failed to delete keyword %s.\n",keyword);
		fits_report_error(stderr,status);
		return 3;
	}
	/* add end to end of headers, as CFITSIO seems to lose it and fail to close the file */
	/* diddly
	retval = fits_update_key_null(fp,"END",NULL,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_keyword_value:failed to add END.\n");
		fits_report_error(stderr,status);
		return 5;
	}
	*/
/* close file */
	status = 0;
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_keyword_value:failed to close file.\n",i);
		fits_report_error(stderr,status);
		return 6;
	}
	return 0;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.2  2006/05/16 18:22:41  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.1  2003/01/28 16:31:57  cjm
** Initial revision
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2002/11/07 19:48:48  cjm
** Initial revision
**
*/
