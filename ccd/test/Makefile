# $Header: /space/home/eng/cjm/cvs/rise/ccd/test/Makefile,v 1.6 2022-03-18 11:44:39 cjm Exp $

include ../../../Makefile.common
include ../../Makefile.common
include ../Makefile.common

# estar_config (used for property loading)
ESTARCONFIGLIBHOME = $(eSTAR_LIB_HOME)
ESTARCONFIGCFLAGS = -I$(eSTAR_SRC_HOME)/config/include/
# Andor libraries 
# Now use common ltdevsrv version
ANDORCFLAGS = -I$(LT_SRC_HOME)/andor/andor-2.85.30000/include $(FTMODE)
ANDORLIB = -L$(LT_SRC_HOME)/andor/andor-2.85.30000/lib -landor -lm

INCDIR 		= $(CCD_SRC_HOME)/include
TESTDIR 	= test
BINDIR 		= $(CCD_BIN_HOME)/$(TESTDIR)/$(HOSTTYPE)
DOCSDIR 	= $(CCD_DOC_HOME)/$(TESTDIR)

CFLAGS 		= -g -I$(INCDIR) -I$(CFITSIOINCDIR) -I$(JNIINCDIR) -I$(JNIMDINCDIR) $(ESTARCONFIGCFLAGS) -c
DOCFLAGS 	= -static

SRCS 		= test.c time_millis.c test_temperature.c \
		  test_setup_startup.c test_setup_dimensions.c test_setup_shutdown.c test_exposure.c

OBJS 		= $(SRCS:%.c=$(BINDIR)/%.o)
PROGS 		= $(SRCS:%.c=$(BINDIR)/%)
DOCS 		= $(SRCS:%.c=$(DOCSDIR)/%.html)
SCRIPT_SRCS	= 
SCRIPT_BINS	= $(SCRIPT_SRCS:%=$(BINDIR)/%)

top: $(PROGS) scripts docs

$(BINDIR)/test: $(BINDIR)/test.o
	cc -o $@ $(BINDIR)/test.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/test_temperature: $(BINDIR)/test_temperature.o
	cc -o $@ $(BINDIR)/test_temperature.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/test_setup_startup: $(BINDIR)/test_setup_startup.o
	cc -o $@ $(BINDIR)/test_setup_startup.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/test_setup_dimensions: $(BINDIR)/test_setup_dimensions.o
	cc -o $@ $(BINDIR)/test_setup_dimensions.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/test_setup_shutdown: $(BINDIR)/test_setup_shutdown.o
	cc -o $@ $(BINDIR)/test_setup_shutdown.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/test_exposure: $(BINDIR)/test_exposure.o
	cc -o $@ $(BINDIR)/test_exposure.o -L$(LT_LIB_HOME) -lrise_ccd $(ANDORLIB) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/posix_time: $(BINDIR)/posix_time.o
	cc -o $@ $(BINDIR)/posix_time.o -L$(LT_LIB_HOME) $(TIMELIB) -lm -lc

$(BINDIR)/time_millis: $(BINDIR)/time_millis.o
	cc -o $@ $(BINDIR)/time_millis.o -L$(LT_LIB_HOME) $(TIMELIB) -lm -lc

scripts: $(SCRIPT_BINS)

$(BINDIR)/%.csh:%.csh
	$(CP) $< $@

$(BINDIR)/%:%
	$(CP) $< $@

docs: $(DOCS)

$(DOCS): $(SRCS)
	-$(CDOC) -d $(DOCSDIR) -h $(INCDIR) $(DOCFLAGS) $(SRCS)

depend:
	makedepend $(MAKEDEPENDFLAGS) -- $(CFLAGS) -- $(SRCS)

clean:
	$(RM) $(RM_OPTIONS) $(PROGS) $(OBJS) $(TIDY_OPTIONS)

tidy:
	$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)

backup: tidy
	$(RM) $(RM_OPTIONS) $(OBJS)

checkin:
	-$(CI) $(CI_OPTIONS) $(SRCS)

checkout:
	-$(CO) $(CO_OPTIONS) $(SRCS)

# DO NOT DELETE

test.o: /usr/include/stdio.h /usr/include/features.h /usr/include/sys/cdefs.h
test.o: /usr/include/bits/wordsize.h /usr/include/gnu/stubs.h
test.o: /usr/include/gnu/stubs-32.h /usr/include/bits/types.h
test.o: /usr/include/bits/typesizes.h /usr/include/libio.h
test.o: /usr/include/_G_config.h /usr/include/wchar.h
test.o: /usr/include/bits/wchar.h /usr/include/gconv.h
test.o: /usr/include/bits/stdio_lim.h /usr/include/bits/sys_errlist.h
test.o: /usr/include/string.h /usr/include/time.h /usr/include/bits/time.h
test.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
test.o: /home/dev/src/rise/ccd/include/ccd_global.h
test.o: /home/dev/src/estar/config/include/estar_config.h
test.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test.o: /home/dev/src/cfitsio3310/include/fitsio.h /usr/include/stdlib.h
test.o: /usr/include/sys/types.h /usr/include/endian.h
test.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test.o: /usr/include/sys/sysmacros.h /usr/include/bits/pthreadtypes.h
test.o: /usr/include/alloca.h /usr/include/limits.h
test.o: /usr/include/bits/posix1_lim.h /usr/include/bits/local_lim.h
test.o: /usr/include/linux/limits.h /usr/include/bits/posix2_lim.h
test.o: /home/dev/src/cfitsio3310/include/longnam.h
test_filter_wheel.o: /usr/include/stdio.h /usr/include/features.h
test_filter_wheel.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_filter_wheel.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_filter_wheel.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_filter_wheel.o: /usr/include/libio.h /usr/include/_G_config.h
test_filter_wheel.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_filter_wheel.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_filter_wheel.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_filter_wheel.o: /usr/include/sys/types.h /usr/include/time.h
test_filter_wheel.o: /usr/include/bits/time.h /usr/include/endian.h
test_filter_wheel.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_filter_wheel.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_filter_wheel.o: /usr/include/sys/sysmacros.h
test_filter_wheel.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_filter_wheel.o: /usr/include/string.h
test_filter_wheel.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_read_memory.o: /usr/include/stdio.h /usr/include/features.h
ccd_read_memory.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_read_memory.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_read_memory.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_read_memory.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_read_memory.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_read_memory.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_read_memory.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
ccd_read_memory.o: /usr/include/sys/types.h /usr/include/time.h
ccd_read_memory.o: /usr/include/bits/time.h /usr/include/endian.h
ccd_read_memory.o: /usr/include/bits/endian.h /usr/include/sys/select.h
ccd_read_memory.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
ccd_read_memory.o: /usr/include/sys/sysmacros.h
ccd_read_memory.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
ccd_read_memory.o: /usr/include/string.h
ccd_read_memory.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
ccd_read_memory.o: /home/dev/src/rise/ccd/include/ccd_setup.h
ccd_write_memory.o: /usr/include/stdio.h /usr/include/features.h
ccd_write_memory.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_write_memory.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_write_memory.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_write_memory.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_write_memory.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_write_memory.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_write_memory.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
ccd_write_memory.o: /usr/include/sys/types.h /usr/include/time.h
ccd_write_memory.o: /usr/include/bits/time.h /usr/include/endian.h
ccd_write_memory.o: /usr/include/bits/endian.h /usr/include/sys/select.h
ccd_write_memory.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
ccd_write_memory.o: /usr/include/sys/sysmacros.h
ccd_write_memory.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
ccd_write_memory.o: /usr/include/string.h
time_millis.o: /usr/include/stdio.h /usr/include/features.h
time_millis.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
time_millis.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
time_millis.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
time_millis.o: /usr/include/libio.h /usr/include/_G_config.h
time_millis.o: /usr/include/wchar.h /usr/include/bits/wchar.h
time_millis.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
time_millis.o: /usr/include/bits/sys_errlist.h /usr/include/limits.h
time_millis.o: /usr/include/bits/posix1_lim.h /usr/include/bits/local_lim.h
time_millis.o: /usr/include/linux/limits.h /usr/include/bits/posix2_lim.h
time_millis.o: /usr/include/errno.h /usr/include/bits/errno.h
time_millis.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
time_millis.o: /usr/include/asm-generic/errno.h
time_millis.o: /usr/include/asm-generic/errno-base.h /usr/include/time.h
time_millis.o: /usr/include/bits/time.h /usr/java/jdk1.5.0_22/include/jni.h
time_millis.o: /usr/java/jdk1.5.0_22/include/linux/jni_md.h
ccd_read_memory_17.o: /usr/include/stdio.h /usr/include/features.h
ccd_read_memory_17.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
ccd_read_memory_17.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_read_memory_17.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
ccd_read_memory_17.o: /usr/include/libio.h /usr/include/_G_config.h
ccd_read_memory_17.o: /usr/include/wchar.h /usr/include/bits/wchar.h
ccd_read_memory_17.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
ccd_read_memory_17.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
ccd_read_memory_17.o: /usr/include/sys/types.h /usr/include/time.h
ccd_read_memory_17.o: /usr/include/bits/time.h /usr/include/endian.h
ccd_read_memory_17.o: /usr/include/bits/endian.h /usr/include/sys/select.h
ccd_read_memory_17.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
ccd_read_memory_17.o: /usr/include/sys/sysmacros.h
ccd_read_memory_17.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
ccd_read_memory_17.o: /usr/include/string.h /usr/include/errno.h
ccd_read_memory_17.o: /usr/include/bits/errno.h /usr/include/linux/errno.h
ccd_read_memory_17.o: /usr/include/asm/errno.h
ccd_read_memory_17.o: /usr/include/asm-generic/errno.h
ccd_read_memory_17.o: /usr/include/asm-generic/errno-base.h
ccd_read_memory_17.o: /usr/include/fcntl.h /usr/include/bits/fcntl.h
ccd_read_memory_17.o: /usr/include/unistd.h /usr/include/bits/posix_opt.h
ccd_read_memory_17.o: /usr/include/bits/confname.h /usr/include/getopt.h
ccd_test_data_link_17.o: /usr/include/stdio.h /usr/include/features.h
ccd_test_data_link_17.o: /usr/include/sys/cdefs.h
ccd_test_data_link_17.o: /usr/include/bits/wordsize.h
ccd_test_data_link_17.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
ccd_test_data_link_17.o: /usr/include/bits/types.h
ccd_test_data_link_17.o: /usr/include/bits/typesizes.h /usr/include/libio.h
ccd_test_data_link_17.o: /usr/include/_G_config.h /usr/include/wchar.h
ccd_test_data_link_17.o: /usr/include/bits/wchar.h /usr/include/gconv.h
ccd_test_data_link_17.o: /usr/include/bits/stdio_lim.h
ccd_test_data_link_17.o: /usr/include/bits/sys_errlist.h
ccd_test_data_link_17.o: /usr/include/stdlib.h /usr/include/sys/types.h
ccd_test_data_link_17.o: /usr/include/time.h /usr/include/bits/time.h
ccd_test_data_link_17.o: /usr/include/endian.h /usr/include/bits/endian.h
ccd_test_data_link_17.o: /usr/include/sys/select.h /usr/include/bits/select.h
ccd_test_data_link_17.o: /usr/include/bits/sigset.h
ccd_test_data_link_17.o: /usr/include/sys/sysmacros.h
ccd_test_data_link_17.o: /usr/include/bits/pthreadtypes.h
ccd_test_data_link_17.o: /usr/include/alloca.h /usr/include/string.h
ccd_test_data_link_17.o: /usr/include/errno.h /usr/include/bits/errno.h
ccd_test_data_link_17.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
ccd_test_data_link_17.o: /usr/include/asm-generic/errno.h
ccd_test_data_link_17.o: /usr/include/asm-generic/errno-base.h
ccd_test_data_link_17.o: /usr/include/fcntl.h /usr/include/bits/fcntl.h
ccd_test_data_link_17.o: /usr/include/unistd.h /usr/include/bits/posix_opt.h
ccd_test_data_link_17.o: /usr/include/bits/confname.h /usr/include/getopt.h
test_dsp_download.o: /usr/include/stdio.h /usr/include/features.h
test_dsp_download.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_dsp_download.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_dsp_download.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_dsp_download.o: /usr/include/libio.h /usr/include/_G_config.h
test_dsp_download.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_dsp_download.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_dsp_download.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_dsp_download.o: /usr/include/sys/types.h /usr/include/time.h
test_dsp_download.o: /usr/include/bits/time.h /usr/include/endian.h
test_dsp_download.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_dsp_download.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_dsp_download.o: /usr/include/sys/sysmacros.h
test_dsp_download.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_dsp_download.o: /usr/include/string.h
test_dsp_download.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_dsp_download.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_reset_controller.o: /usr/include/stdio.h /usr/include/features.h
test_reset_controller.o: /usr/include/sys/cdefs.h
test_reset_controller.o: /usr/include/bits/wordsize.h
test_reset_controller.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_reset_controller.o: /usr/include/bits/types.h
test_reset_controller.o: /usr/include/bits/typesizes.h /usr/include/libio.h
test_reset_controller.o: /usr/include/_G_config.h /usr/include/wchar.h
test_reset_controller.o: /usr/include/bits/wchar.h /usr/include/gconv.h
test_reset_controller.o: /usr/include/bits/stdio_lim.h
test_reset_controller.o: /usr/include/bits/sys_errlist.h
test_reset_controller.o: /usr/include/stdlib.h /usr/include/sys/types.h
test_reset_controller.o: /usr/include/time.h /usr/include/bits/time.h
test_reset_controller.o: /usr/include/endian.h /usr/include/bits/endian.h
test_reset_controller.o: /usr/include/sys/select.h /usr/include/bits/select.h
test_reset_controller.o: /usr/include/bits/sigset.h
test_reset_controller.o: /usr/include/sys/sysmacros.h
test_reset_controller.o: /usr/include/bits/pthreadtypes.h
test_reset_controller.o: /usr/include/alloca.h /usr/include/string.h
test_reset_controller.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_reset_controller.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_data_link.o: /usr/include/stdio.h /usr/include/features.h
test_data_link.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_data_link.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_data_link.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_data_link.o: /usr/include/libio.h /usr/include/_G_config.h
test_data_link.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_data_link.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_data_link.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_data_link.o: /usr/include/sys/types.h /usr/include/time.h
test_data_link.o: /usr/include/bits/time.h /usr/include/endian.h
test_data_link.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_data_link.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_data_link.o: /usr/include/sys/sysmacros.h
test_data_link.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_data_link.o: /usr/include/string.h
test_idle_clocking.o: /usr/include/stdio.h /usr/include/features.h
test_idle_clocking.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_idle_clocking.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_idle_clocking.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_idle_clocking.o: /usr/include/libio.h /usr/include/_G_config.h
test_idle_clocking.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_idle_clocking.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_idle_clocking.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_idle_clocking.o: /usr/include/sys/types.h /usr/include/time.h
test_idle_clocking.o: /usr/include/bits/time.h /usr/include/endian.h
test_idle_clocking.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_idle_clocking.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_idle_clocking.o: /usr/include/sys/sysmacros.h
test_idle_clocking.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_idle_clocking.o: /usr/include/string.h
test_idle_clocking.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_idle_clocking.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_analogue_power.o: /usr/include/stdio.h /usr/include/features.h
test_analogue_power.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_analogue_power.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_analogue_power.o: /usr/include/bits/types.h
test_analogue_power.o: /usr/include/bits/typesizes.h /usr/include/libio.h
test_analogue_power.o: /usr/include/_G_config.h /usr/include/wchar.h
test_analogue_power.o: /usr/include/bits/wchar.h /usr/include/gconv.h
test_analogue_power.o: /usr/include/bits/stdio_lim.h
test_analogue_power.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_analogue_power.o: /usr/include/sys/types.h /usr/include/time.h
test_analogue_power.o: /usr/include/bits/time.h /usr/include/endian.h
test_analogue_power.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_analogue_power.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_analogue_power.o: /usr/include/sys/sysmacros.h
test_analogue_power.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_analogue_power.o: /usr/include/string.h
test_analogue_power.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_analogue_power.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_temperature.o: /usr/include/stdio.h /usr/include/features.h
test_temperature.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_temperature.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_temperature.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_temperature.o: /usr/include/libio.h /usr/include/_G_config.h
test_temperature.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_temperature.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_temperature.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_temperature.o: /usr/include/sys/types.h /usr/include/time.h
test_temperature.o: /usr/include/bits/time.h /usr/include/endian.h
test_temperature.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_temperature.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_temperature.o: /usr/include/sys/sysmacros.h
test_temperature.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_temperature.o: /usr/include/string.h
test_temperature.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_temperature.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_setup_startup.o: /usr/include/stdio.h /usr/include/features.h
test_setup_startup.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_setup_startup.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_setup_startup.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_setup_startup.o: /usr/include/libio.h /usr/include/_G_config.h
test_setup_startup.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_setup_startup.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_setup_startup.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_setup_startup.o: /usr/include/sys/types.h /usr/include/time.h
test_setup_startup.o: /usr/include/bits/time.h /usr/include/endian.h
test_setup_startup.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_setup_startup.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_setup_startup.o: /usr/include/sys/sysmacros.h
test_setup_startup.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_setup_startup.o: /usr/include/string.h
test_setup_startup.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_setup_startup.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_setup_dimensions.o: /usr/include/stdio.h /usr/include/features.h
test_setup_dimensions.o: /usr/include/sys/cdefs.h
test_setup_dimensions.o: /usr/include/bits/wordsize.h
test_setup_dimensions.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_setup_dimensions.o: /usr/include/bits/types.h
test_setup_dimensions.o: /usr/include/bits/typesizes.h /usr/include/libio.h
test_setup_dimensions.o: /usr/include/_G_config.h /usr/include/wchar.h
test_setup_dimensions.o: /usr/include/bits/wchar.h /usr/include/gconv.h
test_setup_dimensions.o: /usr/include/bits/stdio_lim.h
test_setup_dimensions.o: /usr/include/bits/sys_errlist.h
test_setup_dimensions.o: /usr/include/stdlib.h /usr/include/sys/types.h
test_setup_dimensions.o: /usr/include/time.h /usr/include/bits/time.h
test_setup_dimensions.o: /usr/include/endian.h /usr/include/bits/endian.h
test_setup_dimensions.o: /usr/include/sys/select.h /usr/include/bits/select.h
test_setup_dimensions.o: /usr/include/bits/sigset.h
test_setup_dimensions.o: /usr/include/sys/sysmacros.h
test_setup_dimensions.o: /usr/include/bits/pthreadtypes.h
test_setup_dimensions.o: /usr/include/alloca.h /usr/include/string.h
test_setup_dimensions.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_setup_dimensions.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_setup_shutdown.o: /usr/include/stdio.h /usr/include/features.h
test_setup_shutdown.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_setup_shutdown.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_setup_shutdown.o: /usr/include/bits/types.h
test_setup_shutdown.o: /usr/include/bits/typesizes.h /usr/include/libio.h
test_setup_shutdown.o: /usr/include/_G_config.h /usr/include/wchar.h
test_setup_shutdown.o: /usr/include/bits/wchar.h /usr/include/gconv.h
test_setup_shutdown.o: /usr/include/bits/stdio_lim.h
test_setup_shutdown.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_setup_shutdown.o: /usr/include/sys/types.h /usr/include/time.h
test_setup_shutdown.o: /usr/include/bits/time.h /usr/include/endian.h
test_setup_shutdown.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_setup_shutdown.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_setup_shutdown.o: /usr/include/sys/sysmacros.h
test_setup_shutdown.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_setup_shutdown.o: /usr/include/string.h
test_setup_shutdown.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_setup_shutdown.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_exposure.o: /usr/include/stdio.h /usr/include/features.h
test_exposure.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_exposure.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_exposure.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_exposure.o: /usr/include/libio.h /usr/include/_G_config.h
test_exposure.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_exposure.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_exposure.o: /usr/include/bits/sys_errlist.h /usr/include/string.h
test_exposure.o: /usr/include/time.h /usr/include/bits/time.h
test_exposure.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_exposure.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_exposure.o: /home/dev/src/rise/ccd/include/ccd_exposure.h
test_exposure.o: /home/dev/src/rise/ccd/include/ccd_global.h
test_exposure.o: /home/dev/src/estar/config/include/estar_config.h
test_exposure.o: /home/dev/src/cfitsio3310/include/fitsio.h
test_exposure.o: /usr/include/stdlib.h /usr/include/sys/types.h
test_exposure.o: /usr/include/endian.h /usr/include/bits/endian.h
test_exposure.o: /usr/include/sys/select.h /usr/include/bits/select.h
test_exposure.o: /usr/include/bits/sigset.h /usr/include/sys/sysmacros.h
test_exposure.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_exposure.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
test_exposure.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
test_exposure.o: /usr/include/bits/posix2_lim.h
test_exposure.o: /home/dev/src/cfitsio3310/include/longnam.h
test_shutter.o: /usr/include/stdio.h /usr/include/features.h
test_shutter.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_shutter.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_shutter.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_shutter.o: /usr/include/libio.h /usr/include/_G_config.h
test_shutter.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_shutter.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_shutter.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_shutter.o: /usr/include/sys/types.h /usr/include/time.h
test_shutter.o: /usr/include/bits/time.h /usr/include/endian.h
test_shutter.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_shutter.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_shutter.o: /usr/include/sys/sysmacros.h /usr/include/bits/pthreadtypes.h
test_shutter.o: /usr/include/alloca.h /usr/include/string.h
test_abort.o: /usr/include/stdio.h /usr/include/features.h
test_abort.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_abort.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_abort.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_abort.o: /usr/include/libio.h /usr/include/_G_config.h
test_abort.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_abort.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_abort.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_abort.o: /usr/include/sys/types.h /usr/include/time.h
test_abort.o: /usr/include/bits/time.h /usr/include/endian.h
test_abort.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_abort.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_abort.o: /usr/include/sys/sysmacros.h /usr/include/bits/pthreadtypes.h
test_abort.o: /usr/include/alloca.h /usr/include/string.h
test_abort.o: /home/dev/src/rise/ccd/include/ccd_temperature.h
test_abort.o: /home/dev/src/rise/ccd/include/ccd_setup.h
test_vacuum_gauge.o: /usr/include/stdio.h /usr/include/features.h
test_vacuum_gauge.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
test_vacuum_gauge.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
test_vacuum_gauge.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
test_vacuum_gauge.o: /usr/include/libio.h /usr/include/_G_config.h
test_vacuum_gauge.o: /usr/include/wchar.h /usr/include/bits/wchar.h
test_vacuum_gauge.o: /usr/include/gconv.h /usr/include/bits/stdio_lim.h
test_vacuum_gauge.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
test_vacuum_gauge.o: /usr/include/sys/types.h /usr/include/time.h
test_vacuum_gauge.o: /usr/include/bits/time.h /usr/include/endian.h
test_vacuum_gauge.o: /usr/include/bits/endian.h /usr/include/sys/select.h
test_vacuum_gauge.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
test_vacuum_gauge.o: /usr/include/sys/sysmacros.h
test_vacuum_gauge.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
test_vacuum_gauge.o: /usr/include/string.h
test_vacuum_gauge.o: /home/dev/src/rise/ccd/include/ccd_setup.h
