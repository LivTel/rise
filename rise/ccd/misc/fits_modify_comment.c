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
/* fits_modify_comment.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_modify_comment.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_modify_comment &lt;FITS filename&gt; &lt;keyword&gt; &lt;comment&gt; 
 * </pre>
 * fits_modify_comment reads the FITS file and changes a comment associated with the specified keyword.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include "fitsio.h"

/* hash definitions */

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_modify_comment.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

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
	fitsfile *fp = NULL;
	char card_string[81];
	char *filename = NULL;
	char *keyword = NULL;
	char *comment = NULL;

	int retval=0,status=0,done=0,i,count;

/* check arguments */
	if(argc != 4)
	{
		fprintf(stderr,"fits_modify_comment <FITS filename> <keyword> <comment>.\n");
		return 1;
	}
	filename = argv[1];
	keyword = argv[2];
	comment = argv[3];
/* open file */
	retval = fits_open_file(&fp,filename,READWRITE,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
	/* how many card are there? */
	retval = fits_modify_comment(fp,keyword,comment,&status);
	if(retval)
	{
		fprintf(stderr,"fits_modify_comment failed.\n");
		fits_report_error(stderr,status);
		status = 0;
		fits_close_file(fp,&status);
		return 3;
	}
/* close file */
	status = 0;
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fprintf(stderr,"fits_modify_comment:failed to close file.\n",i);
		fits_report_error(stderr,status);
		return 6;
	}
	return 0;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:24:35  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2005/06/09 10:17:26  cjm
** Fixed comment.
**
** Revision 1.1  2005/06/09 10:14:56  cjm
** Initial revision
**
*/
