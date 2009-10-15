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
/* fits_add_keyword_value.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_add_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_add_keyword_value &lt;FITS filename&gt; &lt;keyword&gt; &lt;type&gt; &lt;value&gt; 
 * </pre>
 * fits_add_keyword_value reads the FITS file. It updates the specified keyword with the value supplied (or
 * adds it if the keyword does not exist). The type paramater determines how the value is treated.
 * @see #main
 */
#include <stdio.h>
#include <stdlib.h>
#include "fitsio.h"

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_add_keyword_value.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

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
	fitsfile *fp = NULL;
	int retval=0,status=0,integer_value;
	double double_value;

/* check arguments */
	if(argc != 5)
	{
		fprintf(stderr,"fits_add_keyword_value <FITS filename> <keyword> <type> <value>.\n");
		fprintf(stderr,"Keyword is a valid FITS keyword, to update/add.\n");
		fprintf(stderr,"Value is the new value for the keyword.\n");
		fprintf(stderr,"Type determines how the value is treated. It can take one of the following forms:");
		fprintf(stderr,"\tSTRING, BOOLEAN, INT, DOUBLE, FIXDOUBLE (don't use exponent).\n");
		return 1;
	}
/* open file */
	retval = fits_open_file(&fp,argv[1],READWRITE,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
/* update key based on type */
	if(strcmp(argv[3],"STRING")==0)
	{
		retval = fits_update_key(fp,TSTRING,argv[2],argv[4],NULL,&status);
	}
	else if(strcmp(argv[3],"INT")==0)
	{
		integer_value = atoi(argv[4]);
		retval = fits_update_key(fp,TINT,argv[2],&integer_value,NULL,&status);
	}
	else if(strcmp(argv[3],"BOOLEAN")==0)
	{
		if((strcmp(argv[4],"TRUE")==0)||(strcmp(argv[4],"True")==0)||
			(strcmp(argv[4],"true")==0)||(strcmp(argv[4],"T")==0))
			integer_value = TRUE;
		else if((strcmp(argv[4],"FALSE")==0)||(strcmp(argv[4],"False")==0)||
			(strcmp(argv[4],"false")==0)||(strcmp(argv[4],"F")==0))
			integer_value = FALSE;
		else
		{
			fprintf(stderr,"Illegal boolean value `%s'. Boolean's can have one of the following values:",
				argv[4]);
			fprintf(stderr,"\tTRUE, True, true, T, FALSE, False, false, F.\n");
			return 3;
		}
		retval = fits_update_key(fp,TLOGICAL,argv[2],&integer_value,NULL,&status);
	}
	else if(strcmp(argv[3],"DOUBLE")==0)
	{
		double_value = atof(argv[4]);
		retval = fits_update_key(fp,TDOUBLE,argv[2],&double_value,NULL,&status);
	}
	else if(strcmp(argv[3],"FIXDOUBLE")==0)
	{
		double_value = atof(argv[4]);
		retval = fits_update_key_fixdbl(fp,argv[2],double_value,6,NULL,&status);
	}
	else
	{
		fprintf(stderr,"Illegal type `%s'. It can take one of the following forms:",argv[3]);
		fprintf(stderr,"\tSTRING, BOOLEAN, INT, DOUBLE, FIXDOUBLE (don't use exponent).\n");
		return 4;
	}
	if(retval)
	{
		fits_report_error(stderr,status);
		return 5;
	}
/* close file */
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 6;
	}
	return 0;
}
