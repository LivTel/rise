# $Header: /space/home/eng/cjm/cvs/rise/ccd/c/Makefile,v 1.3 2022-03-14 16:08:46 cjm Exp $

include			../../../Makefile.common
include			../../Makefile.common
include			../Makefile.common

INCDIR = $(CCD_SRC_HOME)/include
DOCSDIR = $(CCD_DOC_HOME)/cdocs

#Do we want to byte swap the ccd data read from the camera.
# Previous to v1.3 - does NOT byte swap in the device driver - we do it in software, this flag is set
# v1.4 byte swapped in the device driver - this flag was unset
# v1.7 does NOT byte swap in the device driver - we do it in software, this flag is set
BYTESWAPCFLAGS = -DCCD_EXPOSURE_BYTE_SWAP=1
#BYTESWAPCFLAGS = 

# Whether to fake filter wheel movements or actually do them
# Usually, we should actually move the filter wheel, but if we have a system with a camera but no filter wheels,
# we can choose to fake the filter wheel.
# Fake moving the filter wheel.
#FILTERWHEELFAKECFLAGS = -DCCD_FILTER_WHEEL_FAKE
# Actually move the filter wheel
FILTERWHEELFAKECFLAGS = 

# The home input of the filter wheel mechanism can be either a proximity sensor
# or a magnetic one. The inputs received by the SDSU electronics are reversed depending
# on which input is used. We select which to compile for here.
# Use a proximity sensor for filter wheel input.
#INPUTHOMETYPE = PROXIMITY
# Use a magnetic sensor for filter wheel input.
INPUTHOMETYPE = MAGNETIC
# define PROXIMITY/MAGNETIC and CCD_FILTER_WHEEL_INPUT_HOME
FILTERWHEELINPUTHOMECFLAGS = -DPROXIMITY=1 -DMAGNETIC=2 -DCCD_FILTER_WHEEL_INPUT_HOME=$(INPUTHOMETYPE)

# Do we wish to link with CFITSIO routines?
FITSCFLAGS = -DCFITSIO=1 -I$(CFITSIOINCDIR) 
CFITSIOLIB = -lcfitsio

# FITSCFLAGS =
# CFITSIOLIB =

# ANDOR STUFF linking
# e.g. Frame transfer mode on (1) or off (0)
FTMODE = -DFTMODE=1
# Andor libraries 
# installed in /usr/local  on rise
#ANDORCFLAGS = -I/usr/local/include $(FTMODE)
#ANDORLIB = -L/usr/local/lib -landor -lm
#installed in $(RISE_SRC_HOME)/andor/include and $(RISE_SRC_HOME)/andor/lib
#ANDORCFLAGS = -I$(RISE_SRC_HOME)/andor/include $(FTMODE)
#ANDORLIB = -L$(RISE_SRC_HOME)/andor/lib -landor -lm
# Now use common ltdevsrv version
ANDORCFLAGS = -I$(LT_SRC_HOME)/andor/andor-2.85.30000/include $(FTMODE)
ANDORLIB = -L$(LT_SRC_HOME)/andor/andor-2.85.30000/lib -landor -lm

# estar_config (used for property loading)
ESTARCONFIGLIBHOME = $(eSTAR_LIB_HOME)
ESTARCONFIGCFLAGS = -I$(eSTAR_SRC_HOME)/config/include/

# Do we want to enable logging?
# no logging
#LOGGINGCFLAGS = -DLOGGING=0
# some logging
#LOGGINGCFLAGS = -DLOGGING=1
# some logging
#LOGGINGCFLAGS = -DLOGGING=4
#lots of logging. This can cause time critical operations like BIAS frames to fail.
#LOGGINGCFLAGS = -DLOGGING=5
LOGGINGCFLAGS = -DLOGGING=10

# SLALIB link information - used only for calculating MJD at the moment
#MJDCFLAGS 	= -DSLALIB=1 -I${LT_SRC_HOME}/astrometry/include
#MJDLIB		= -lsla
# NGAT Astro MJD link informatiom
MJDCFLAGS 	= -DNGATASTRO=1 -I${LT_SRC_HOME}/ngatastro/include
MJDLIB		= -lngatastro

# when reading out it could help if we lock down the memory we are reading out into to prevent page swaps
# 1. No memory locking
MLOCKCFLAGS	= 
# 2. Memory locking
#MLOCKCFLAGS	= -DCCD_GLOBAL_READOUT_MLOCK

#Do we want MUTEX protected locking around controller command communication?
MUTEXCFLAGS = -DCCD_DSP_MUTEXED=1
#MUTEXCFLAGS = 

# when reading out it helps if we are running at a better priority
# This can be done by using priority change code and running as root.
# - No priority changes in library, code will run as user.
PRIORITYCFLAGS		= -DCCD_GLOBAL_READOUT_PRIORITY=0
# - Priority changes in library, using POSIX.4 scheduler, code must run as root.
#PRIORITYCFLAGS		= -DCCD_GLOBAL_READOUT_PRIORITY=1
# - Priority changes in library, using BSD/SVr4 process management, code must run as root.
#PRIORITYCFLAGS		= -DCCD_GLOBAL_READOUT_PRIORITY=2

# Do we want to stop the timing board Idling mode whilst downloading a DSP program to it?
# It is not clear whether we should do this.
# Don't stop timing board Idling whilst downloading DSP code. (voodoo v1.4 did this)
#TIMINGDOWNLOADIDLECFLAGS =
# Stop timing board Idling whilst downloading DSP code. (ccdtool did this/voodoo v1.7 does this)
TIMINGDOWNLOADIDLECFLAGS = -DCCD_SETUP_TIMING_DOWNLOAD_IDLE=1

# In some versions of the DSP code the utility board used to lock the controller up,
# if we sent an RDM/WRM/Read temperature during an exposure.
# In v1.7, the controller locks up if a RDM is sent during a readout.
# This flag enables us to conditionally compile some code to test for this:
# Return an error if RDM/WRM/temperature get called when we are exposing.
#UTILEXPOSURECHECKFLAGS = -DCCD_DSP_UTIL_EXPOSURE_CHECK=1
# Return an error if RDM/WRM/temperature get called when we are reading out.
UTILEXPOSURECHECKFLAGS = -DCCD_DSP_UTIL_EXPOSURE_CHECK=2
# Allow RDM/WRM/temperature get calls when we are exposing.
#UTILEXPOSURECHECKFLAGS = 

CFLAGS = -g $(CCHECKFLAG) -I$(INCDIR) -I$(JNIINCDIR) -I$(JNIMDINCDIR) $(ESTARCONFIGCFLAGS) \
	$(FITSCFLAGS) $(MJDCFLAGS) $(MUTEXCFLAGS) $(TIMINGDOWNLOADIDLECFLAGS) \
	$(UTILEXPOSURECHECKFLAGS) $(BYTESWAPCFLAGS) \
	$(FILTERWHEELINPUTHOMECFLAGS) $(FILTERWHEELFAKECFLAGS) $(PRIORITYCFLAGS) $(MLOCKCFLAGS) $(LOGGINGCFLAGS) \
	$(LOG_UDP_CFLAGS) $(ANDORCFLAGS)

LINTFLAGS = -I$(INCDIR) -I$(JNIINCDIR) -I$(JNIMDINCDIR)
DOCFLAGS = -static
SRCS 		= 	ccd_global.c ccd_temperature.c ccd_setup.c ccd_exposure.c ccd_multrun.c 
# ccd_filter_wheel.c ccd_interface.c ccd_pci.c ccd_text.c ccd_dsp.c ccd_dsp_download.c 
HEADERS		=	$(SRCS:%.c=%.h)
OBJS		=	$(SRCS:%.c=%.o)
DOCS 		= 	$(SRCS:%.c=$(DOCSDIR)/%.html)
JAVASRCS 	= 	$(SRCS) ngat_rise_ccd_CCDLibrary.c
JAVAOBJS	=	$(JAVASRCS:%.c=%.o)
JAVADOCS 	= 	$(JAVASRCS:%.c=$(DOCSDIR)/%.html)

top: $(INCDIR)/ngat_rise_ccd_CCDLibrary.h shared docs

shared: $(LT_LIB_HOME)/librise_ccd.so

$(LT_LIB_HOME)/librise_ccd.so: $(JAVAOBJS)
	cc $(CCSHAREDFLAG) $(CFLAGS) $(JAVAOBJS) -o $@ -L$(LT_LIB_HOME) -L$(ESTARCONFIGLIBHOME) $(TIMELIB) $(CFITSIOLIB) $(MJDLIB) $(ANDORLIB) -lestar_config

static: $(LT_LIB_HOME)/librise_ccd.a

$(LT_LIB_HOME)/librise_ccd.a: $(OBJS)
	ar rcv $@ $?

# special case for ngat_rise_ccd_CCDLibrary - depending on whether ngat.rise.ccd.CCDLibrary has changed
# recreate the machine generated JNI header
# touch it, javah does not update the timestamp correctly
$(INCDIR)/ngat_rise_ccd_CCDLibrary.h: $(LT_JAVALIB_HOME)/ngat_rise_ccd.jar
	$(JAVAH) -jni -d $(INCDIR) ngat.rise.ccd.CCDLibrary
	$(TOUCH) $@

docs: $(JAVADOCS)

$(JAVADOCS): $(JAVASRCS)
	-$(CDOC) -d $(DOCSDIR) -h $(INCDIR) $(DOCFLAGS) $(JAVASRCS)

$(JAVADOCS) : $(JAVASRCS)

checkout:
	-$(CO) $(CO_OPTIONS) $(JAVASRCS)
	-(cd $(INCDIR); $(CO) $(CO_OPTIONS) $(HEADERS);)

checkin:
	-$(CI) $(CI_OPTIONS) $(JAVASRCS)
	-(cd $(INCDIR); $(CI) $(CI_OPTIONS) $(HEADERS);)

staticdepend:
	makedepend $(MAKEDEPENDFLAGS) -- $(CFLAGS) -- $(SRCS)

depend:
	makedepend $(MAKEDEPENDFLAGS) -- $(CFLAGS) -- $(JAVASRCS)

lint:
	$(LINT)	$(LINTFLAGS) $(JAVASRCS)

clean:
	-$(RM) $(RM_OPTIONS) $(JAVAOBJS) $(INCDIR)/ngat_rise_ccd_CCDLibrary.h $(LT_LIB_HOME)/librise_ccd.so $(LT_LIB_HOME)/librise_ccd_g.so $(LT_LIB_HOME)/librise_ccd.a $(TIDY_OPTIONS)

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	-(cd $(INCDIR); $(RM) $(RM_OPTIONS) $(TIDY_OPTIONS);)

backup: tidy
	-$(RM) $(RM_OPTIONS) $(JAVAOBJS) $(INCDIR)/ngat_rise_ccd_CCDLibrary.h

#
# $Log: not supported by cvs2svn $
# Revision 1.2  2010/03/26 14:40:05  cjm
# Changed from bitwise to absolute logging levels.
# Added log_udp CFLAGS.
#
# Revision 1.1  2009/10/15 10:24:02  cjm
# Initial revision
#
# Revision 1.19  2003/03/04 17:10:27  cjm
# Added alternate MJDCFLAGS and MJDLIB flags for NGATASTRO MJD library.
#
# Revision 1.18  2002/11/08 12:14:52  cjm
# *** empty log message ***
#
# Revision 1.17  2002/11/07 19:17:55  cjm
# Changes to make library work with SDSU version 1.7 DSP code.
#
# Revision 1.16  2002/05/27 13:20:03  cjm
# Added FILTERWHEELFAKECFLAGS, which determines whether to fake filter wheel movements
# or actually try to do them.
#
# Revision 1.15  2001/07/11 10:11:21  cjm
# Added SLALIB compilation flags.
#
# Revision 1.14  2001/06/04 14:55:56  cjm
# More flags:logging, memory locking, process priority...
#
# Revision 1.13  2001/04/05 09:07:26  cjm
# Added LOGGINGCFLAGS.
#
# Revision 1.12  2001/03/19 18:38:42  cjm
# Added FILTERWHEELINPUTHOMECFLAGS.
#
# Revision 1.11  2001/03/12 15:14:06  cjm
# Added UTILEXPOSURECHECKFLAGS.
#
# Revision 1.10  2001/01/30 10:06:49  cjm
# Fixed ngat_ccd_CCDLibrary.h dependancy so the header always gets built using
# javah when the ngat_ccd.jar changes or the header does not exist (has been cleaned).
#
# Revision 1.9  2000/12/19 12:03:58  cjm
# Added ccd_filter_wheel source.
#
# Revision 1.8  2000/12/18 11:59:42  cjm
# Fixed JNI include dependancy on ngat.jar - now on ngat_ccd.jar.
#
# Revision 1.7  2000/09/25 09:52:15  cjm
# y
# Changes to use with v1.4 SDSU DSP code.
#
# Revision 1.6  2000/07/06 14:34:21  cjm
# Added touch to javah command.
#
# Revision 1.5  2000/06/26 17:01:44  cjm
# Changed default CFLAGS with respect to debugging (DEBUGCFLAGS).
#
# Revision 1.4  2000/06/19 08:49:00  cjm
# Backup.
#
# Revision 1.3  2000/06/13 17:15:18  cjm
# Changes to make Ccs agree with voodoo.
#
# Revision 1.2  2000/03/08 18:27:35  cjm
# Added initial MUTEXCFLAGS options.
#
# Revision 1.1  2000/02/08 18:12:33  cjm
# Initial revision
#
#

# DO NOT DELETE

ccd_global.o: /usr/include/stdio.h /usr/include/features.h
ccd_global.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_global.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_global.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_global.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_global.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_global.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_global.o: /usr/include/bits/sys_errlist.h /usr/include/getopt.h
ccd_global.o: /usr/include/errno.h /usr/include/bits/errno.h
ccd_global.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
ccd_global.o: /usr/include/asm-generic/errno.h
ccd_global.o: /usr/include/asm-generic/errno-base.h /usr/include/limits.h
ccd_global.o: /usr/include/bits/posix1_lim.h /usr/include/bits/local_lim.h
ccd_global.o: /usr/include/linux/limits.h /usr/include/bits/posix2_lim.h
ccd_global.o: /usr/include/bits/xopen_lim.h /usr/include/sys/types.h
ccd_global.o: /usr/include/time.h /usr/include/string.h /usr/include/unistd.h
ccd_global.o: /usr/include/bits/posix_opt.h /usr/include/bits/confname.h
ccd_global.o: /home/dev/src/log_udp/include/log_udp.h
ccd_global.o: /home/dev/src/rise/ccd/include/ccd_global.h
ccd_global.o: /home/dev/src/estar/config/include/estar_config.h
ccd_global.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
ccd_global.o: /home/dev/src/rise/ccd/include/ccd_multrun.h
ccd_global.o: /usr/include/dirent.h /usr/include/bits/dirent.h
ccd_global.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ccd_global.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_global.o: /home/dev/src/andor/andor-2.85.30000/include/atmcdLXd.h
ccd_temperature.o: /usr/include/stdio.h /usr/include/features.h
ccd_temperature.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_temperature.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_temperature.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_temperature.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_temperature.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_temperature.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_temperature.o: /usr/include/bits/sys_errlist.h /usr/include/getopt.h
ccd_temperature.o: /usr/include/string.h /usr/include/math.h
ccd_temperature.o: /usr/include/bits/huge_val.h /usr/include/bits/mathdef.h
ccd_temperature.o: /usr/include/bits/mathcalls.h /usr/include/time.h
ccd_temperature.o: /home/dev/src/log_udp/include/log_udp.h
ccd_temperature.o: /home/dev/src/rise/ccd/include/ccd_global.h
ccd_temperature.o: /home/dev/src/estar/config/include/estar_config.h
ccd_temperature.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ccd_temperature.o: /home/dev/src/andor/andor-2.85.30000/include/atmcdLXd.h
ccd_setup.o: /usr/include/stdio.h /usr/include/features.h
ccd_setup.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_setup.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_setup.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_setup.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_setup.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_setup.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_setup.o: /usr/include/bits/sys_errlist.h /usr/include/getopt.h
ccd_setup.o: /usr/include/stdlib.h /usr/include/bits/waitflags.h
ccd_setup.o: /usr/include/bits/waitstatus.h /usr/include/sys/types.h
ccd_setup.o: /usr/include/time.h /usr/include/string.h /usr/include/fcntl.h
ccd_setup.o: /usr/include/bits/fcntl.h /usr/include/sys/stat.h
ccd_setup.o: /usr/include/bits/stat.h /usr/include/signal.h
ccd_setup.o: /usr/include/bits/sigset.h /usr/include/bits/signum.h
ccd_setup.o: /usr/include/bits/siginfo.h /usr/include/bits/sigaction.h
ccd_setup.o: /usr/include/bits/sigstack.h /usr/include/sys/ucontext.h
ccd_setup.o: /usr/include/bits/sigcontext.h /usr/include/asm/sigcontext.h
ccd_setup.o: /usr/include/errno.h /usr/include/bits/errno.h
ccd_setup.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
ccd_setup.o: /usr/include/asm-generic/errno.h
ccd_setup.o: /usr/include/asm-generic/errno-base.h /usr/include/math.h
ccd_setup.o: /usr/include/bits/huge_val.h /usr/include/bits/mathdef.h
ccd_setup.o: /usr/include/bits/mathcalls.h /usr/include/unistd.h
ccd_setup.o: /usr/include/bits/posix_opt.h /usr/include/bits/confname.h
ccd_setup.o: /home/dev/src/log_udp/include/log_udp.h
ccd_setup.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
ccd_setup.o: /home/dev/src/rise/ccd/include/ccd_global.h
ccd_setup.o: /home/dev/src/estar/config/include/estar_config.h
ccd_setup.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ccd_setup.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_setup.o: /home/dev/src/andor/andor-2.85.30000/include/atmcdLXd.h
ccd_exposure.o: /usr/include/stdio.h /usr/include/features.h
ccd_exposure.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_exposure.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_exposure.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_exposure.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_exposure.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_exposure.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_exposure.o: /usr/include/bits/sys_errlist.h /usr/include/getopt.h
ccd_exposure.o: /usr/include/stdlib.h /usr/include/bits/waitflags.h
ccd_exposure.o: /usr/include/bits/waitstatus.h /usr/include/sys/types.h
ccd_exposure.o: /usr/include/time.h /usr/include/string.h
ccd_exposure.o: /usr/include/fcntl.h /usr/include/bits/fcntl.h
ccd_exposure.o: /usr/include/sys/stat.h /usr/include/bits/stat.h
ccd_exposure.o: /usr/include/signal.h /usr/include/bits/sigset.h
ccd_exposure.o: /usr/include/bits/signum.h /usr/include/bits/siginfo.h
ccd_exposure.o: /usr/include/bits/sigaction.h /usr/include/bits/sigstack.h
ccd_exposure.o: /usr/include/sys/ucontext.h /usr/include/bits/sigcontext.h
ccd_exposure.o: /usr/include/asm/sigcontext.h /usr/include/errno.h
ccd_exposure.o: /usr/include/bits/errno.h /usr/include/linux/errno.h
ccd_exposure.o: /usr/include/asm/errno.h /usr/include/asm-generic/errno.h
ccd_exposure.o: /usr/include/asm-generic/errno-base.h /usr/include/unistd.h
ccd_exposure.o: /usr/include/bits/posix_opt.h /usr/include/bits/confname.h
ccd_exposure.o: /home/dev/src/log_udp/include/log_udp.h
ccd_exposure.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
ccd_exposure.o: /home/dev/src/rise/ccd/include/ccd_global.h
ccd_exposure.o: /home/dev/src/estar/config/include/estar_config.h
ccd_exposure.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_exposure.o: /home/dev/src/cfitsio3310/include/fitsio.h
ccd_exposure.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
ccd_exposure.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
ccd_exposure.o: /usr/include/bits/posix2_lim.h /usr/include/bits/xopen_lim.h
ccd_exposure.o: /home/dev/src/cfitsio3310/include/longnam.h
ccd_exposure.o: /home/dev/src/ngatastro/include/ngat_astro.h
ccd_exposure.o: /home/dev/src/ngatastro/include/ngat_astro_mjd.h
ccd_exposure.o: /home/dev/src/andor/andor-2.85.30000/include/atmcdLXd.h
ccd_multrun.o: /usr/include/stdio.h /usr/include/features.h
ccd_multrun.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_multrun.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_multrun.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_multrun.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_multrun.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_multrun.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_multrun.o: /usr/include/bits/sys_errlist.h /usr/include/getopt.h
ccd_multrun.o: /usr/include/stdlib.h /usr/include/bits/waitflags.h
ccd_multrun.o: /usr/include/bits/waitstatus.h /usr/include/sys/types.h
ccd_multrun.o: /usr/include/time.h /usr/include/string.h /usr/include/fcntl.h
ccd_multrun.o: /usr/include/bits/fcntl.h /usr/include/sys/stat.h
ccd_multrun.o: /usr/include/bits/stat.h /usr/include/signal.h
ccd_multrun.o: /usr/include/bits/sigset.h /usr/include/bits/signum.h
ccd_multrun.o: /usr/include/bits/siginfo.h /usr/include/bits/sigaction.h
ccd_multrun.o: /usr/include/bits/sigstack.h /usr/include/sys/ucontext.h
ccd_multrun.o: /usr/include/bits/sigcontext.h /usr/include/asm/sigcontext.h
ccd_multrun.o: /usr/include/errno.h /usr/include/bits/errno.h
ccd_multrun.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
ccd_multrun.o: /usr/include/asm-generic/errno.h
ccd_multrun.o: /usr/include/asm-generic/errno-base.h /usr/include/unistd.h
ccd_multrun.o: /usr/include/bits/posix_opt.h /usr/include/bits/confname.h
ccd_multrun.o: /usr/include/math.h /usr/include/bits/huge_val.h
ccd_multrun.o: /usr/include/bits/mathdef.h /usr/include/bits/mathcalls.h
ccd_multrun.o: /home/dev/src/log_udp/include/log_udp.h
ccd_multrun.o: /home/dev/src/rise/ccd/include/ccd_global.h
ccd_multrun.o: /home/dev/src/estar/config/include/estar_config.h
ccd_multrun.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
ccd_multrun.o: /home/dev/src/rise/ccd/include/ccd_multrun.h
ccd_multrun.o: /usr/include/dirent.h /usr/include/bits/dirent.h
ccd_multrun.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_multrun.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ccd_multrun.o: /home/dev/src/cfitsio3310/include/fitsio.h
ccd_multrun.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
ccd_multrun.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
ccd_multrun.o: /usr/include/bits/posix2_lim.h /usr/include/bits/xopen_lim.h
ccd_multrun.o: /home/dev/src/cfitsio3310/include/longnam.h
ccd_multrun.o: /home/dev/src/ngatastro/include/ngat_astro.h
ccd_multrun.o: /home/dev/src/ngatastro/include/ngat_astro_mjd.h
ccd_multrun.o: /home/dev/src/andor/andor-2.85.30000/include/atmcdLXd.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/stdio.h /usr/include/features.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/sys/cdefs.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/wordsize.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/gnu/stubs.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/gnu/stubs-32.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/types.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/typesizes.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/libio.h /usr/include/_G_config.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/gconv.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/stdio_lim.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/sys_errlist.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/getopt.h /usr/include/stdlib.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/waitflags.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/bits/waitstatus.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/sys/types.h /usr/include/time.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/string.h
ngat_rise_ccd_CCDLibrary.o: /usr/java/jdk1.5.0_22/include/jni.h
ngat_rise_ccd_CCDLibrary.o: /usr/java/jdk1.5.0_22/include/linux/jni_md.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ccd_global.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/estar/config/include/estar_config.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ccd_multrun.h
ngat_rise_ccd_CCDLibrary.o: /usr/include/dirent.h /usr/include/bits/dirent.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ngat_rise_ccd_CCDLibrary.o: /home/dev/src/rise/ccd/include/ngat_rise_ccd_CCDLibrary.h
