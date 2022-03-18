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
/**
 * Simple file to subset a fits data array and write the result as a new fits
 * file.
 * The input arguments are:
 * <ul>
 * <li>filename of original FITS file</li>
 * <li>sub-array beginning X coordinate</li>
 * <li>sub-array beginning Y coordinate</li>
 * <li>sub-array end X coordinate</li>
 * <li>sub-array end Y coordinate</li>
 * <li>filename of new FITS file</li>
 * </ul>
 */
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <float.h>
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
static char rcsid[] = "$Id: fits_sub_image.c,v 1.1 2009-10-15 10:15:44 cjm Exp $";

/*===========================================================================*/
/*	External routines.                                                   */
/*===========================================================================*/
/**
 * Main Program.
 * @param argc The number of arguments. This should be 2.
 * @param argv The arguments.
 */
int main( int argc, char *argv[] )
{
  fitsfile *in_fp = NULL, *out_fp = NULL;
  int retval = 0, status = 0, n_bins = 256, *hist, sigma_N = 0;
  int naxis, naxis_one, naxis_two, i, j, nan = 0;
  char infile[ 256 ], outfile[ 256 ], *ptr;
  int x0, x1, y0, y1, new_x, new_y;
  long axes[ 2 ];
  double *indata, *outdata;

  /* check arguments */
  if( argc != 7 )
  {
    fprintf( stderr, "%s fits-in x-start y-start x-finish y-finish fits-out\n",
	     argv[ 0 ] );
    return 1;
  }

  /* get input filename */
  strncpy( infile, argv[ 1 ], 255 );
  infile[ 255 ] = '\0';

  /* parse coords */
  x0 = strtol( argv[ 2 ], &ptr, 10 );
  if( ptr == argv[ 2 ] )
  {
    fprintf( stderr, "failed parsing x0 [%s]\n", *ptr );
    return( 2 );
  }

  y0 = strtol( argv[ 3 ], &ptr, 10 );
  if( ptr == argv[ 3 ] )
  {
    fprintf( stderr, "failed parsing y0 [%s]\n", *ptr );
    return( 3 );
  }

  x1 = strtol( argv[ 4 ], &ptr, 10 );
  if( ptr == argv[ 4 ] )
  {
    fprintf( stderr, "failed parsing x1 [%s]\n", *ptr );
    return( 4 );
  }

  y1 = strtol( argv[ 5 ], &ptr, 10 );
  if( ptr == argv[ 5 ] )
  {
    fprintf( stderr, "failed parsing y1 [%s]\n", *ptr );
    return( 5 );
  }

  new_x = ( x1 - x0 );
  new_y = ( y1 - y0 );

  fprintf
    ( stderr, "x0 = %d\nx1 = %d\ny0 = %d\ny1 = %d\nnew_x = %d\nnew_y = %d\n",
      x0, x1, y0, y1, new_x, new_y );

  /* get input infile */
  strncpy( outfile, argv[ 6 ], 255 );
  infile[ 255 ] = '\0';

  /* open file */
  retval = fits_open_file( &in_fp, infile, READONLY, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 4;
  }

  fprintf( stdout, "file %s open\n", infile );

  /* check naxis */
  retval = fits_read_key
    ( in_fp, TINT, "NAXIS", &naxis, NULL, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 7;
  }
  if( naxis != FITS_GET_DATA_NAXIS )
  {
    fprintf( stderr, "%s : %s has wrong NAXIS value(%d).\n",
	     argv[ 0 ], infile, naxis );
    return 8;
  }

  fprintf( stdout, "NAXIS = %d\n", naxis );

  /* get naxis1,naxis2 */
  retval = fits_read_key( in_fp, TINT, "NAXIS1", &naxis_one, NULL, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 9;
  }

  fprintf( stdout, "NAXIS1 = %d\n", naxis_one );

  retval = fits_read_key( in_fp, TINT, "NAXIS2", &naxis_two, NULL, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 10;
  }

  fprintf( stdout, "NAXIS2 = %d\n", naxis_two );

  /* allocate data */
  indata = (double *)malloc( naxis_one * naxis_two * sizeof( double ) );
  if( indata == NULL )
  {
    fprintf( stderr, "%s : failed to allocate memory (%d,%d,%d).\n",
	     argv[ 0 ],
	     naxis_one, naxis_two, naxis_one*naxis_two*sizeof( double ) );
    return 11;
  }

  outdata = (double *)malloc( new_x * new_y * sizeof( double ) );

  if( outdata == NULL )
  {
    fprintf( stderr,"%s : failed to allocate memory (%d,%d,%d).\n",
	     argv[ 0 ],  new_x, new_y, new_x*new_y*sizeof( double ) );
    return 111;
  }

  fprintf( stdout, "reading indata ... " );

  /* read the indata */
  retval = fits_read_img( in_fp, TDOUBLE, 1, naxis_one * naxis_two, NULL,
			  indata, NULL, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 12;
  }

  fprintf( stdout, "done\n" );

  /* close file */
  retval = fits_close_file( in_fp, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 13;
  }

  fprintf( stdout, "file %s closed\n", infile );

  /* create sub-array */
  for( j = 0; j < new_y; j++ )
  {
    for( i = 0; i < new_x; i++ )
    {
      *( outdata + ( j * new_x ) + i ) =
	*( indata + ( ( y0 + j ) * naxis_one ) + x0 + i );
    }
  }

  fprintf( stderr, "creating sub-array... " );

  /* write new file */
  retval = fits_create_file( &out_fp, outfile, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 14;
  }

  fprintf( stderr, "done\ncreating new FITS image... " );

  axes[ 0 ] = (long)new_x;
  axes[ 1 ] = (long)new_y;
  retval = fits_create_img( out_fp, DOUBLE_IMG, naxis, axes, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 155;
  }

  retval = fits_write_img
    ( out_fp, TDOUBLE, 1, ( new_x * new_y ), outdata, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 156;
  }

  retval = fits_close_file( out_fp, &status );
  if( retval )
  {
    fits_report_error( stderr, status );
    return 16;
  }

  fprintf( stderr, "done\nfile %s closed\n", outfile );

  free( indata );
  free( outdata );
  return 0;
}
