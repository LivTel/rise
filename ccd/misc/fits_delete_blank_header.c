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
/* fits_delete_blank_header.c
** $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/fits_delete_blank_header.c,v 1.1 2009-10-15 10:15:44 cjm Exp $
*/
/**
 * <pre>
 * fits_delete_blank_header &lt;FITS filename&gt;
 * </pre>
 * fits_delete_blank_header reads the FITS file headers and deletes and blank keyword lines.
 * A blank keyword is either 8 spaces, or starts with a NULL (0) character.
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
/**
 * A blank keyword is one that contains only spaces. Hence this is 8 space
 * characters.
 */
#define BLANK_KEYWORD_STRING     "        "

/* internal variables */
/**
 * Revision Control System identifier.
 */
static char rcsid[] = "$Id: fits_delete_blank_header.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

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
	char card_string[81];
	int retval=0,status=0,done=0,i,count;

/* check arguments */
	if(argc != 2)
	{
		fprintf(stderr,"fits_delete_blank_header <FITS filename>.\n");
		return 1;
	}
/* open file */
	retval = fits_open_file(&fp,argv[1],READONLY,&status);
	if(retval)
	{
		fits_report_error(stderr,status);
		return 2;
	}
	/* how many card are there? */
	retval = fits_get_hdrspace(fp,&count,NULL,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_blank_header:failed to get number of keywords.\n",i);
		fits_report_error(stderr,status);
		status = 0;
		fits_close_file(fp,&status);
		return 3;
	}
/* read every card */
	done = 0;
	for(i=1;i<=count;i++)
	{
		retval = fits_read_record(fp,i,card_string,&status);
		if(retval == 0)
		{
			fprintf(stdout,"%3d. %8s.\n",i,card_string);
		/* check for blank (starting with NULL) keyword */
			if(strcmp(card_string,"")==0)
			{
				retval = fits_delete_record(fp,i,&status);
				/* if we suceeded, reset i (index into the list of cards)
				** as fits_delete_record moves the next card into the ith record. */
				if(retval == 0)
				{
					fprintf(stdout,"Deleted blank (NULL) card at index %d.\n",i);
					i--;
					count--;
				}
				else
				{
					fprintf(stderr,"fits_delete_blank_header:failed to delete blank (NULL) card %d.\n",i);
					fits_report_error(stderr,status);
					status = 0;
					fits_close_file(fp,&status);
					return 3;
				}
			}
		/* check for blank (spaces) keyword */
			if(strncmp(card_string,BLANK_KEYWORD_STRING,strlen(BLANK_KEYWORD_STRING))==0)
			{
				retval = fits_delete_record(fp,i,&status);
				/* if we suceeded, reset i (index into the list of cards)
				** as fits_delete_record moves the next card into the ith record. */
				if(retval == 0)
				{
					fprintf(stdout,"Deleted blank (space) card at index %d.\n",i);
					i--;
					count--;
				}
				else
				{
					fprintf(stderr,"fits_delete_blank_header:failed to delete blank (space) card %d.\n",i);
					fits_report_error(stderr,status);
					status = 0;
					fits_close_file(fp,&status);
					return 4;
				}
			}
		}
		else
		{
			fprintf(stderr,"fits_delete_blank_header:failed to read card %d.\n",i);
			fits_report_error(stderr,status);
		}
		i++;
	}
	/* add end to end of headers, as CFITSIO seems to lose it and fail to close the file */
	retval = fits_update_key_null(fp,"END",NULL,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_blank_header:failed to add END.\n");
		fits_report_error(stderr,status);
		return 5;
	}
/* close file */
	status = 0;
	retval = fits_close_file(fp,&status);
	if(retval)
	{
		fprintf(stderr,"fits_delete_blank_header:failed to close file.\n",i);
		fits_report_error(stderr,status);
		return 6;
	}
	return 0;
}
/*
** $Log: not supported by cvs2svn $
** Revision 1.3  2006/05/16 18:22:40  cjm
** gnuify: Added GNU General Public License.
**
** Revision 1.2  2002/11/28 17:55:22  cjm
** Added rcsid.
**
** Revision 1.1  2002/11/07 19:48:48  cjm
** Initial revision
**
*/
