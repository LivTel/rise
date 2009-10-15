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
/* fits_get_keyword_value.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_get_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_get_keyword_value &lt;FITS filename&gt; &lt;keyword&gt; &lt;type&gt; 
 * </pre>
 * fits_get_keyword_value reads the FITS file. It finds the specified keyword and prints out it's value.
 * The type parameter specifies the type of value expected.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include "fitsio.h"

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_get_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

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
	char buff[81];
	int retval=0,status=0,integer_value;
	double double_value;

/* check arguments */
	if(argc != 4)
	{
		fprintf(stderr,"fits_get_keyword_value <FITS filename> <keyword> <type>.\n");
		fprintf(stderr,"Keyword is a valid FITS keyword, to search for.\n");
		fprintf(stderr,"Type determines how the value is treated. It can take one of the following forms:");
		fprintf(stderr,"\tSTRING, INT, DOUBLE.\n");
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
	if(strcmp(argv[3],"STRING")==0)
	{
		retval = fits_read_key(fp,TSTRING,argv[2],buff,NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return 3;
		}
		fprintf(stdout,"%s\n",buff);
	}
	else if(strcmp(argv[3],"INT")==0)
	{
		retval = fits_read_key(fp,TINT,argv[2],&integer_value,NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return 4;
		}
		fprintf(stdout,"%d\n",integer_value);
	}
	else if(strcmp(argv[3],"DOUBLE")==0)
	{
		retval = fits_read_key(fp,TDOUBLE,argv[2],&double_value,NULL,&status);
		if(retval)
		{
			fits_report_error(stderr,status);
			return 5;
		}
		fprintf(stdout,"%.6f\n",double_value);
	}
	else
	{
		fprintf(stderr,"Illegal type `%s'. It can take one of the following forms:",argv[3]);
		fprintf(stderr,"\tSTRING, INT, DOUBLE.\n");
		return 6;
	}
/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 7;
	}
	return 0;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:22:46  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2001/07/10 19:02:26  cjm
** Initial revision
**
*/
