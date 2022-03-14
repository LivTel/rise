/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Ccs.

    Ccs is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Ccs is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Ccs; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
/* ccd_setup.h
** $Header: /space/home/eng/cjm/cvs/rise/ccd/include/ccd_setup.h,v 1.2 2022-03-14 15:58:51 cjm Exp $
*/
#ifndef CCD_SETUP_H
#define CCD_SETUP_H

/**
 * The number of windows the controller can put on the CCD.
 * @see #Setup_Struct
 */
#define CCD_SETUP_WINDOW_COUNT			(4)

/* These hash definitions should match with those in CCDLibrary.java */
/**
 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
 * first window position is to be used.
 * @see #CCD_Setup_Dimensions
 */
#define CCD_SETUP_WINDOW_ONE	(1<<0)
/**
 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
 * second window position is to be used.
 * @see #CCD_Setup_Dimensions
 */
#define CCD_SETUP_WINDOW_TWO	(1<<1)
/**
 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
 * third window position is to be used.
 * @see #CCD_Setup_Dimensions
 */
#define CCD_SETUP_WINDOW_THREE	(1<<2)
/**
 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify the
 * fourth window position is to be used.
 * @see #CCD_Setup_Dimensions
 */
#define CCD_SETUP_WINDOW_FOUR	(1<<3)
/**
 * Window flag used as part of the window_flags bit-field parameter of CCD_Setup_Dimensions to specify all the
 * window positions are to be used.
 * @see #CCD_Setup_Dimensions
 */
#define CCD_SETUP_WINDOW_ALL	(CCD_SETUP_WINDOW_ONE|CCD_SETUP_WINDOW_TWO| \
					CCD_SETUP_WINDOW_THREE|CCD_SETUP_WINDOW_FOUR)

/**
 * Structure holding position information for one window on the CCD. Fields are:
 * <dl>
 * <dt>X_Start</dt> <dd>The pixel number of the X start position of the window (upper left corner).</dd>
 * <dt>Y_Start</dt> <dd>The pixel number of the Y start position of the window (upper left corner).</dd>
 * <dt>X_End</dt> <dd>The pixel number of the X end position of the window (lower right corner).</dd>
 * <dt>Y_End</dt> <dd>The pixel number of the Y end position of the window (lower right corner).</dd>
 * </dl>
 * @see #CCD_Setup_Dimensions
 */
struct CCD_Setup_Window_Struct
{
	int X_Start;
	int Y_Start;
	int X_End;
	int Y_End;
};

extern void CCD_Setup_Initialise(void);
extern int CCD_Setup_Startup(double target_temperature);
extern int CCD_Setup_Shutdown(void);
extern int CCD_Setup_Dimensions(int ncols,int nrows,int nsbin,int npbin,
				int window_flags,struct CCD_Setup_Window_Struct window_list[]);
extern void CCD_Setup_Abort(void);
extern int CCD_Setup_Get_NCols(void);
extern int CCD_Setup_Get_NRows(void);
extern int CCD_Setup_Get_NSBin(void);
extern int CCD_Setup_Get_NPBin(void);
extern int CCD_Setup_Get_Readout_Pixel_Count(void);
extern int CCD_Setup_Get_Window_Pixel_Count(int window_index);
extern int CCD_Setup_Get_Window_Width(int window_index);
extern int CCD_Setup_Get_Window_Height(int window_index);
extern int CCD_Setup_Get_Idle(void);
extern int CCD_Setup_Get_Window_Flags(void);
extern int CCD_Setup_Get_Window(int window_index,struct CCD_Setup_Window_Struct *window);
extern int CCD_Setup_Get_Setup_Complete(void);
extern int CCD_Setup_Get_Setup_In_Progress(void);
extern int CCD_Setup_Get_Error_Number(void);
extern void CCD_Setup_Error(void);
extern void CCD_Setup_Error_String(char *error_string);
extern void CCD_Setup_Warning(void);
#endif
