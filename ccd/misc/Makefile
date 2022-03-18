# $Header: /space/home/eng/cjm/cvs/rise/ccd/misc/Makefile,v 1.3 2022-03-18 11:13:14 cjm Exp $

include ../../../Makefile.common
include ../../Makefile.common
include ../Makefile.common

MISCNAME	= misc
BINDIR		= $(CCD_BIN_HOME)/$(MISCNAME)/$(HOSTTYPE)
INCDIR 		= $(CCD_SRC_HOME)/include
DOCSDIR 	= $(CCD_DOC_HOME)/$(MISCNAME)

#DEBUG_CFLAGS	= 
DEBUG_CFLAGS	= -DDEBUG 

CFLAGS 		= -g -I$(INCDIR) -I$(CFITSIOINCDIR) $(DEBUG_CFLAGS)
DOCFLAGS 	= -static

EXE_SRCS 	= fits_get_data.c fits_get_header.c fits_get_mean.c \
		fits_add_keyword_value.c fits_get_keyword_value.c fits_get_keyword_comment.c \
		fits_delete_keyword_value.c fits_copy_keyword_card.c fits_create_blank.c \
		fits_sub.c fits_sub_value.c fits_flip.c fits_flip_double.c \
		fits_delete_blank_header.c fits_median.c fits_modify_comment.c fits16_signed_to_unsigned.c \
		fits_to_targa.c targa_combine.c fits_sub_image.c fits_normalise.c

LIB_SRCS	= targa.c
SRCS		= $(EXE_SRCS) $(LIB_SRCS)
OBJS		=	$(SRCS:%.c=%.o)
LIB_OBJS	=	$(LIB_SRCS:%.c=%.o)
EXES		=	$(EXE_SRCS:%.c=$(BINDIR)/%)
STATIC_EXES	=	$(EXE_SRCS:%.c=$(BINDIR)/%_static) 
DOCS 		= 	$(SRCS:%.c=$(DOCSDIR)/%.html)

SCRIPT_SRCS	= fits_txt_get_min_max_value.awk fits_get_object fits_get_keyword_value_all
SCRIPT_EXES	= $(SCRIPT_SRCS:%.awk=$(CCD_BIN_HOME)/$(MISCNAME)/%.awk)

top: $(LT_LIB_HOME)/libtarga.so $(EXES) docs $(SCRIPT_EXES)

static: $(LT_LIB_HOME)/libtarga.a $(STATIC_EXES) docs

$(CCD_BIN_HOME)/$(MISCNAME)/%.awk: %.awk
	$(CP) $< $@
	$(CHMOD) +x $@

$(LT_LIB_HOME)/libtarga.so : $(LIB_OBJS)
	$(CC) $(CCSHAREDFLAG) $(CFLAGS) $(LIB_OBJS) -o $@

$(LT_LIB_HOME)/libtarga.a : $(LIB_OBJS)
	ar rcv $@ $?

$(BINDIR)/fits_to_targa: fits_to_targa.o $(LT_LIB_HOME)/libtarga.so 
	$(CC) fits_to_targa.o -o $@ -L$(LT_LIB_HOME) -ltarga -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/fits_to_targa_static: fits_to_targa.o $(LT_LIB_HOME)/libtarga.so 
	$(CC) fits_to_targa.o -o $@ $(CCSTATICFLAG) -L$(LT_LIB_HOME) -ltarga -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/targa_combine: targa_combine.o $(LT_LIB_HOME)/libtarga.so 
	$(CC) targa_combine.o -o $@  -L$(LT_LIB_HOME) -ltarga -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/targa_combine_static: targa_combine.o $(LT_LIB_HOME)/libtarga.so 
	$(CC) targa_combine.o -o $@ $(CCSTATICFLAG) -L$(LT_LIB_HOME) -ltarga -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/%_static: %.o
	$(CC) $< -o $@ $(CCSTATICFLAG) -L$(LT_LIB_HOME) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

$(BINDIR)/%: %.o
	$(CC) $< -o $@  -L$(LT_LIB_HOME) -lcfitsio $(TIMELIB) $(SOCKETLIB) -lm -lc

docs: $(DOCS)

$(DOCS): $(SRCS)
	-$(CDOC) -d $(DOCSDIR) -h $(INCDIR) $(DOCFLAGS) $(SRCS)

$(DOCS) : $(SRCS)

depend:
	makedepend $(MAKEDEPENDFLAGS) -- $(CFLAGS) -- $(SRCS)

clean:
	$(RM) $(RM_OPTIONS) $(EXES) $(OBJS) $(TIDY_OPTIONS)

tidy:
	$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)

backup: tidy
	$(RM) $(RM_OPTIONS) $(OBJS)

checkin:
	-$(CI) $(CI_OPTIONS) $(SRCS)

checkout:
	-$(CO) $(CO_OPTIONS) $(SRCS)

# DO NOT DELETE

fits_get_data.o: /usr/include/stdio.h /usr/include/features.h
fits_get_data.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_get_data.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_get_data.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_get_data.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_get_data.o: /usr/include/libio.h /usr/include/_G_config.h
fits_get_data.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_get_data.o: /usr/include/gconv.h
fits_get_data.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_get_data.o: /usr/include/bits/stdio_lim.h
fits_get_data.o: /usr/include/bits/sys_errlist.h
fits_get_data.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_get_data.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_get_data.o: /usr/include/time.h /usr/include/endian.h
fits_get_data.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_get_data.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_get_data.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_get_data.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_get_data.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_get_data.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_get_data.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_get_data.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_get_data.o: /usr/include/bits/posix2_lim.h
fits_get_data.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_get_header.o: /usr/include/stdio.h /usr/include/features.h
fits_get_header.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_get_header.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_get_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_get_header.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_get_header.o: /usr/include/libio.h /usr/include/_G_config.h
fits_get_header.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_get_header.o: /usr/include/gconv.h
fits_get_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_get_header.o: /usr/include/bits/stdio_lim.h
fits_get_header.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
fits_get_header.o: /usr/include/sys/types.h /usr/include/time.h
fits_get_header.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_get_header.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_get_header.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_get_header.o: /usr/include/sys/sysmacros.h
fits_get_header.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_get_header.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_get_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_get_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_get_header.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_get_header.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_get_header.o: /usr/include/bits/posix2_lim.h
fits_get_header.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_get_mean.o: /usr/include/stdio.h /usr/include/features.h
fits_get_mean.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_get_mean.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_get_mean.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_get_mean.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_get_mean.o: /usr/include/libio.h /usr/include/_G_config.h
fits_get_mean.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_get_mean.o: /usr/include/gconv.h
fits_get_mean.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_get_mean.o: /usr/include/bits/stdio_lim.h
fits_get_mean.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
fits_get_mean.o: /usr/include/sys/types.h /usr/include/time.h
fits_get_mean.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_get_mean.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_get_mean.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_get_mean.o: /usr/include/sys/sysmacros.h
fits_get_mean.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_get_mean.o: /usr/include/strings.h
fits_get_mean.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_get_mean.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_get_mean.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_get_mean.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_get_mean.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_get_mean.o: /usr/include/bits/posix2_lim.h
fits_get_mean.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_add_keyword_value.o: /usr/include/stdio.h /usr/include/features.h
fits_add_keyword_value.o: /usr/include/sys/cdefs.h
fits_add_keyword_value.o: /usr/include/bits/wordsize.h
fits_add_keyword_value.o: /usr/include/gnu/stubs.h
fits_add_keyword_value.o: /usr/include/gnu/stubs-32.h
fits_add_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_add_keyword_value.o: /usr/include/bits/types.h
fits_add_keyword_value.o: /usr/include/bits/typesizes.h /usr/include/libio.h
fits_add_keyword_value.o: /usr/include/_G_config.h /usr/include/wchar.h
fits_add_keyword_value.o: /usr/include/bits/wchar.h /usr/include/gconv.h
fits_add_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_add_keyword_value.o: /usr/include/bits/stdio_lim.h
fits_add_keyword_value.o: /usr/include/bits/sys_errlist.h
fits_add_keyword_value.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_add_keyword_value.o: /usr/include/time.h /usr/include/endian.h
fits_add_keyword_value.o: /usr/include/bits/endian.h
fits_add_keyword_value.o: /usr/include/sys/select.h
fits_add_keyword_value.o: /usr/include/bits/select.h
fits_add_keyword_value.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_add_keyword_value.o: /usr/include/sys/sysmacros.h
fits_add_keyword_value.o: /usr/include/bits/pthreadtypes.h
fits_add_keyword_value.o: /usr/include/alloca.h
fits_add_keyword_value.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_add_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_add_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_add_keyword_value.o: /usr/include/limits.h
fits_add_keyword_value.o: /usr/include/bits/posix1_lim.h
fits_add_keyword_value.o: /usr/include/bits/local_lim.h
fits_add_keyword_value.o: /usr/include/linux/limits.h
fits_add_keyword_value.o: /usr/include/bits/posix2_lim.h
fits_add_keyword_value.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_get_keyword_value.o: /usr/include/stdio.h /usr/include/features.h
fits_get_keyword_value.o: /usr/include/sys/cdefs.h
fits_get_keyword_value.o: /usr/include/bits/wordsize.h
fits_get_keyword_value.o: /usr/include/gnu/stubs.h
fits_get_keyword_value.o: /usr/include/gnu/stubs-32.h
fits_get_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_get_keyword_value.o: /usr/include/bits/types.h
fits_get_keyword_value.o: /usr/include/bits/typesizes.h /usr/include/libio.h
fits_get_keyword_value.o: /usr/include/_G_config.h /usr/include/wchar.h
fits_get_keyword_value.o: /usr/include/bits/wchar.h /usr/include/gconv.h
fits_get_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_get_keyword_value.o: /usr/include/bits/stdio_lim.h
fits_get_keyword_value.o: /usr/include/bits/sys_errlist.h
fits_get_keyword_value.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_get_keyword_value.o: /usr/include/time.h /usr/include/endian.h
fits_get_keyword_value.o: /usr/include/bits/endian.h
fits_get_keyword_value.o: /usr/include/sys/select.h
fits_get_keyword_value.o: /usr/include/bits/select.h
fits_get_keyword_value.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_get_keyword_value.o: /usr/include/sys/sysmacros.h
fits_get_keyword_value.o: /usr/include/bits/pthreadtypes.h
fits_get_keyword_value.o: /usr/include/alloca.h
fits_get_keyword_value.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_get_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_get_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_get_keyword_value.o: /usr/include/limits.h
fits_get_keyword_value.o: /usr/include/bits/posix1_lim.h
fits_get_keyword_value.o: /usr/include/bits/local_lim.h
fits_get_keyword_value.o: /usr/include/linux/limits.h
fits_get_keyword_value.o: /usr/include/bits/posix2_lim.h
fits_get_keyword_value.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_get_keyword_comment.o: /usr/include/stdio.h /usr/include/features.h
fits_get_keyword_comment.o: /usr/include/sys/cdefs.h
fits_get_keyword_comment.o: /usr/include/bits/wordsize.h
fits_get_keyword_comment.o: /usr/include/gnu/stubs.h
fits_get_keyword_comment.o: /usr/include/gnu/stubs-32.h
fits_get_keyword_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_get_keyword_comment.o: /usr/include/bits/types.h
fits_get_keyword_comment.o: /usr/include/bits/typesizes.h
fits_get_keyword_comment.o: /usr/include/libio.h /usr/include/_G_config.h
fits_get_keyword_comment.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_get_keyword_comment.o: /usr/include/gconv.h
fits_get_keyword_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_get_keyword_comment.o: /usr/include/bits/stdio_lim.h
fits_get_keyword_comment.o: /usr/include/bits/sys_errlist.h
fits_get_keyword_comment.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_get_keyword_comment.o: /usr/include/time.h /usr/include/endian.h
fits_get_keyword_comment.o: /usr/include/bits/endian.h
fits_get_keyword_comment.o: /usr/include/sys/select.h
fits_get_keyword_comment.o: /usr/include/bits/select.h
fits_get_keyword_comment.o: /usr/include/bits/sigset.h
fits_get_keyword_comment.o: /usr/include/bits/time.h
fits_get_keyword_comment.o: /usr/include/sys/sysmacros.h
fits_get_keyword_comment.o: /usr/include/bits/pthreadtypes.h
fits_get_keyword_comment.o: /usr/include/alloca.h
fits_get_keyword_comment.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_get_keyword_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_get_keyword_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_get_keyword_comment.o: /usr/include/limits.h
fits_get_keyword_comment.o: /usr/include/bits/posix1_lim.h
fits_get_keyword_comment.o: /usr/include/bits/local_lim.h
fits_get_keyword_comment.o: /usr/include/linux/limits.h
fits_get_keyword_comment.o: /usr/include/bits/posix2_lim.h
fits_get_keyword_comment.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_delete_keyword_value.o: /usr/include/stdio.h /usr/include/features.h
fits_delete_keyword_value.o: /usr/include/sys/cdefs.h
fits_delete_keyword_value.o: /usr/include/bits/wordsize.h
fits_delete_keyword_value.o: /usr/include/gnu/stubs.h
fits_delete_keyword_value.o: /usr/include/gnu/stubs-32.h
fits_delete_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_delete_keyword_value.o: /usr/include/bits/types.h
fits_delete_keyword_value.o: /usr/include/bits/typesizes.h
fits_delete_keyword_value.o: /usr/include/libio.h /usr/include/_G_config.h
fits_delete_keyword_value.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_delete_keyword_value.o: /usr/include/gconv.h
fits_delete_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_delete_keyword_value.o: /usr/include/bits/stdio_lim.h
fits_delete_keyword_value.o: /usr/include/bits/sys_errlist.h
fits_delete_keyword_value.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_delete_keyword_value.o: /usr/include/time.h /usr/include/endian.h
fits_delete_keyword_value.o: /usr/include/bits/endian.h
fits_delete_keyword_value.o: /usr/include/sys/select.h
fits_delete_keyword_value.o: /usr/include/bits/select.h
fits_delete_keyword_value.o: /usr/include/bits/sigset.h
fits_delete_keyword_value.o: /usr/include/bits/time.h
fits_delete_keyword_value.o: /usr/include/sys/sysmacros.h
fits_delete_keyword_value.o: /usr/include/bits/pthreadtypes.h
fits_delete_keyword_value.o: /usr/include/alloca.h
fits_delete_keyword_value.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_delete_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_delete_keyword_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_delete_keyword_value.o: /usr/include/limits.h
fits_delete_keyword_value.o: /usr/include/bits/posix1_lim.h
fits_delete_keyword_value.o: /usr/include/bits/local_lim.h
fits_delete_keyword_value.o: /usr/include/linux/limits.h
fits_delete_keyword_value.o: /usr/include/bits/posix2_lim.h
fits_delete_keyword_value.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_copy_keyword_card.o: /usr/include/stdio.h /usr/include/features.h
fits_copy_keyword_card.o: /usr/include/sys/cdefs.h
fits_copy_keyword_card.o: /usr/include/bits/wordsize.h
fits_copy_keyword_card.o: /usr/include/gnu/stubs.h
fits_copy_keyword_card.o: /usr/include/gnu/stubs-32.h
fits_copy_keyword_card.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_copy_keyword_card.o: /usr/include/bits/types.h
fits_copy_keyword_card.o: /usr/include/bits/typesizes.h /usr/include/libio.h
fits_copy_keyword_card.o: /usr/include/_G_config.h /usr/include/wchar.h
fits_copy_keyword_card.o: /usr/include/bits/wchar.h /usr/include/gconv.h
fits_copy_keyword_card.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_copy_keyword_card.o: /usr/include/bits/stdio_lim.h
fits_copy_keyword_card.o: /usr/include/bits/sys_errlist.h
fits_copy_keyword_card.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_copy_keyword_card.o: /usr/include/time.h /usr/include/endian.h
fits_copy_keyword_card.o: /usr/include/bits/endian.h
fits_copy_keyword_card.o: /usr/include/sys/select.h
fits_copy_keyword_card.o: /usr/include/bits/select.h
fits_copy_keyword_card.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_copy_keyword_card.o: /usr/include/sys/sysmacros.h
fits_copy_keyword_card.o: /usr/include/bits/pthreadtypes.h
fits_copy_keyword_card.o: /usr/include/alloca.h
fits_copy_keyword_card.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_copy_keyword_card.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_copy_keyword_card.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_copy_keyword_card.o: /usr/include/limits.h
fits_copy_keyword_card.o: /usr/include/bits/posix1_lim.h
fits_copy_keyword_card.o: /usr/include/bits/local_lim.h
fits_copy_keyword_card.o: /usr/include/linux/limits.h
fits_copy_keyword_card.o: /usr/include/bits/posix2_lim.h
fits_copy_keyword_card.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_create_blank.o: /usr/include/stdio.h /usr/include/features.h
fits_create_blank.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_create_blank.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_create_blank.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_create_blank.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_create_blank.o: /usr/include/libio.h /usr/include/_G_config.h
fits_create_blank.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_create_blank.o: /usr/include/gconv.h
fits_create_blank.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_create_blank.o: /usr/include/bits/stdio_lim.h
fits_create_blank.o: /usr/include/bits/sys_errlist.h /usr/include/string.h
fits_create_blank.o: /usr/include/malloc.h
fits_create_blank.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/float.h
fits_create_blank.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_create_blank.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_create_blank.o: /usr/include/time.h /usr/include/endian.h
fits_create_blank.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_create_blank.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_create_blank.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_create_blank.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_create_blank.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_create_blank.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_create_blank.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_create_blank.o: /usr/include/bits/local_lim.h
fits_create_blank.o: /usr/include/linux/limits.h
fits_create_blank.o: /usr/include/bits/posix2_lim.h
fits_create_blank.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_sub.o: /usr/include/stdio.h /usr/include/features.h
fits_sub.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_sub.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_sub.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_sub.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_sub.o: /usr/include/libio.h /usr/include/_G_config.h
fits_sub.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_sub.o: /usr/include/gconv.h
fits_sub.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_sub.o: /usr/include/bits/stdio_lim.h /usr/include/bits/sys_errlist.h
fits_sub.o: /home/dev/src/cfitsio3.006/include/fitsio.h /usr/include/stdlib.h
fits_sub.o: /usr/include/sys/types.h /usr/include/time.h
fits_sub.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_sub.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_sub.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_sub.o: /usr/include/sys/sysmacros.h /usr/include/bits/pthreadtypes.h
fits_sub.o: /usr/include/alloca.h
fits_sub.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_sub.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_sub.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_sub.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_sub.o: /usr/include/bits/posix2_lim.h
fits_sub.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_sub_value.o: /usr/include/stdio.h /usr/include/features.h
fits_sub_value.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_sub_value.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_sub_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_sub_value.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_sub_value.o: /usr/include/libio.h /usr/include/_G_config.h
fits_sub_value.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_sub_value.o: /usr/include/gconv.h
fits_sub_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_sub_value.o: /usr/include/bits/stdio_lim.h
fits_sub_value.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
fits_sub_value.o: /usr/include/sys/types.h /usr/include/time.h
fits_sub_value.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_sub_value.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_sub_value.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_sub_value.o: /usr/include/sys/sysmacros.h
fits_sub_value.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_sub_value.o: /usr/include/errno.h /usr/include/bits/errno.h
fits_sub_value.o: /usr/include/linux/errno.h /usr/include/asm/errno.h
fits_sub_value.o: /usr/include/asm-generic/errno.h
fits_sub_value.o: /usr/include/asm-generic/errno-base.h
fits_sub_value.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_sub_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_sub_value.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_sub_value.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_sub_value.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_sub_value.o: /usr/include/bits/posix2_lim.h
fits_sub_value.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_flip.o: /usr/include/stdio.h /usr/include/features.h
fits_flip.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_flip.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_flip.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_flip.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_flip.o: /usr/include/libio.h /usr/include/_G_config.h
fits_flip.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_flip.o: /usr/include/gconv.h
fits_flip.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_flip.o: /usr/include/bits/stdio_lim.h /usr/include/bits/sys_errlist.h
fits_flip.o: /usr/include/string.h
fits_flip.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_flip.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_flip.o: /usr/include/time.h /usr/include/endian.h
fits_flip.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_flip.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_flip.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_flip.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_flip.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_flip.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_flip.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_flip.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_flip.o: /usr/include/bits/posix2_lim.h
fits_flip.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_flip_double.o: /usr/include/stdio.h /usr/include/features.h
fits_flip_double.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_flip_double.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_flip_double.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_flip_double.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_flip_double.o: /usr/include/libio.h /usr/include/_G_config.h
fits_flip_double.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_flip_double.o: /usr/include/gconv.h
fits_flip_double.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_flip_double.o: /usr/include/bits/stdio_lim.h
fits_flip_double.o: /usr/include/bits/sys_errlist.h /usr/include/string.h
fits_flip_double.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_flip_double.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_flip_double.o: /usr/include/time.h /usr/include/endian.h
fits_flip_double.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_flip_double.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_flip_double.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_flip_double.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_flip_double.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_flip_double.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_flip_double.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_flip_double.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_flip_double.o: /usr/include/bits/posix2_lim.h
fits_flip_double.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_delete_blank_header.o: /usr/include/stdio.h /usr/include/features.h
fits_delete_blank_header.o: /usr/include/sys/cdefs.h
fits_delete_blank_header.o: /usr/include/bits/wordsize.h
fits_delete_blank_header.o: /usr/include/gnu/stubs.h
fits_delete_blank_header.o: /usr/include/gnu/stubs-32.h
fits_delete_blank_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_delete_blank_header.o: /usr/include/bits/types.h
fits_delete_blank_header.o: /usr/include/bits/typesizes.h
fits_delete_blank_header.o: /usr/include/libio.h /usr/include/_G_config.h
fits_delete_blank_header.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_delete_blank_header.o: /usr/include/gconv.h
fits_delete_blank_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_delete_blank_header.o: /usr/include/bits/stdio_lim.h
fits_delete_blank_header.o: /usr/include/bits/sys_errlist.h
fits_delete_blank_header.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_delete_blank_header.o: /usr/include/time.h /usr/include/endian.h
fits_delete_blank_header.o: /usr/include/bits/endian.h
fits_delete_blank_header.o: /usr/include/sys/select.h
fits_delete_blank_header.o: /usr/include/bits/select.h
fits_delete_blank_header.o: /usr/include/bits/sigset.h
fits_delete_blank_header.o: /usr/include/bits/time.h
fits_delete_blank_header.o: /usr/include/sys/sysmacros.h
fits_delete_blank_header.o: /usr/include/bits/pthreadtypes.h
fits_delete_blank_header.o: /usr/include/alloca.h
fits_delete_blank_header.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_delete_blank_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_delete_blank_header.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_delete_blank_header.o: /usr/include/limits.h
fits_delete_blank_header.o: /usr/include/bits/posix1_lim.h
fits_delete_blank_header.o: /usr/include/bits/local_lim.h
fits_delete_blank_header.o: /usr/include/linux/limits.h
fits_delete_blank_header.o: /usr/include/bits/posix2_lim.h
fits_delete_blank_header.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_median.o: /usr/include/stdio.h /usr/include/features.h
fits_median.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_median.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_median.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_median.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_median.o: /usr/include/libio.h /usr/include/_G_config.h
fits_median.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_median.o: /usr/include/gconv.h
fits_median.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_median.o: /usr/include/bits/stdio_lim.h /usr/include/bits/sys_errlist.h
fits_median.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_median.o: /usr/include/time.h /usr/include/endian.h
fits_median.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_median.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_median.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_median.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_median.o: /usr/include/string.h
fits_median.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_median.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_median.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_median.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_median.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_median.o: /usr/include/bits/posix2_lim.h
fits_median.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_modify_comment.o: /usr/include/stdio.h /usr/include/features.h
fits_modify_comment.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_modify_comment.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_modify_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_modify_comment.o: /usr/include/bits/types.h
fits_modify_comment.o: /usr/include/bits/typesizes.h /usr/include/libio.h
fits_modify_comment.o: /usr/include/_G_config.h /usr/include/wchar.h
fits_modify_comment.o: /usr/include/bits/wchar.h /usr/include/gconv.h
fits_modify_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_modify_comment.o: /usr/include/bits/stdio_lim.h
fits_modify_comment.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
fits_modify_comment.o: /usr/include/sys/types.h /usr/include/time.h
fits_modify_comment.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_modify_comment.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_modify_comment.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_modify_comment.o: /usr/include/sys/sysmacros.h
fits_modify_comment.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_modify_comment.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_modify_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_modify_comment.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_modify_comment.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_modify_comment.o: /usr/include/bits/local_lim.h
fits_modify_comment.o: /usr/include/linux/limits.h
fits_modify_comment.o: /usr/include/bits/posix2_lim.h
fits_modify_comment.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits16_signed_to_unsigned.o: /usr/include/stdio.h /usr/include/features.h
fits16_signed_to_unsigned.o: /usr/include/sys/cdefs.h
fits16_signed_to_unsigned.o: /usr/include/bits/wordsize.h
fits16_signed_to_unsigned.o: /usr/include/gnu/stubs.h
fits16_signed_to_unsigned.o: /usr/include/gnu/stubs-32.h
fits16_signed_to_unsigned.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits16_signed_to_unsigned.o: /usr/include/bits/types.h
fits16_signed_to_unsigned.o: /usr/include/bits/typesizes.h
fits16_signed_to_unsigned.o: /usr/include/libio.h /usr/include/_G_config.h
fits16_signed_to_unsigned.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits16_signed_to_unsigned.o: /usr/include/gconv.h
fits16_signed_to_unsigned.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits16_signed_to_unsigned.o: /usr/include/bits/stdio_lim.h
fits16_signed_to_unsigned.o: /usr/include/bits/sys_errlist.h
fits16_signed_to_unsigned.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits16_signed_to_unsigned.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits16_signed_to_unsigned.o: /usr/include/time.h /usr/include/endian.h
fits16_signed_to_unsigned.o: /usr/include/bits/endian.h
fits16_signed_to_unsigned.o: /usr/include/sys/select.h
fits16_signed_to_unsigned.o: /usr/include/bits/select.h
fits16_signed_to_unsigned.o: /usr/include/bits/sigset.h
fits16_signed_to_unsigned.o: /usr/include/bits/time.h
fits16_signed_to_unsigned.o: /usr/include/sys/sysmacros.h
fits16_signed_to_unsigned.o: /usr/include/bits/pthreadtypes.h
fits16_signed_to_unsigned.o: /usr/include/alloca.h
fits16_signed_to_unsigned.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits16_signed_to_unsigned.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits16_signed_to_unsigned.o: /usr/include/limits.h
fits16_signed_to_unsigned.o: /usr/include/bits/posix1_lim.h
fits16_signed_to_unsigned.o: /usr/include/bits/local_lim.h
fits16_signed_to_unsigned.o: /usr/include/linux/limits.h
fits16_signed_to_unsigned.o: /usr/include/bits/posix2_lim.h
fits16_signed_to_unsigned.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_to_targa.o: /usr/include/stdio.h /usr/include/features.h
fits_to_targa.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_to_targa.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_to_targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_to_targa.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_to_targa.o: /usr/include/libio.h /usr/include/_G_config.h
fits_to_targa.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_to_targa.o: /usr/include/gconv.h
fits_to_targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_to_targa.o: /usr/include/bits/stdio_lim.h
fits_to_targa.o: /usr/include/bits/sys_errlist.h
fits_to_targa.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_to_targa.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_to_targa.o: /usr/include/time.h /usr/include/endian.h
fits_to_targa.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_to_targa.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_to_targa.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_to_targa.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_to_targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_to_targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_to_targa.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_to_targa.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_to_targa.o: /usr/include/bits/posix2_lim.h
fits_to_targa.o: /home/dev/src/cfitsio3.006/include/longnam.h targa.h
targa_combine.o: /usr/include/stdio.h /usr/include/features.h
targa_combine.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
targa_combine.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
targa_combine.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
targa_combine.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
targa_combine.o: /usr/include/libio.h /usr/include/_G_config.h
targa_combine.o: /usr/include/wchar.h /usr/include/bits/wchar.h
targa_combine.o: /usr/include/gconv.h
targa_combine.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
targa_combine.o: /usr/include/bits/stdio_lim.h
targa_combine.o: /usr/include/bits/sys_errlist.h targa.h
fits_sub_image.o: /usr/include/stdio.h /usr/include/features.h
fits_sub_image.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_sub_image.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_sub_image.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_sub_image.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_sub_image.o: /usr/include/libio.h /usr/include/_G_config.h
fits_sub_image.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_sub_image.o: /usr/include/gconv.h
fits_sub_image.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_sub_image.o: /usr/include/bits/stdio_lim.h
fits_sub_image.o: /usr/include/bits/sys_errlist.h /usr/include/string.h
fits_sub_image.o: /usr/include/malloc.h
fits_sub_image.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/float.h
fits_sub_image.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_sub_image.o: /usr/include/stdlib.h /usr/include/sys/types.h
fits_sub_image.o: /usr/include/time.h /usr/include/endian.h
fits_sub_image.o: /usr/include/bits/endian.h /usr/include/sys/select.h
fits_sub_image.o: /usr/include/bits/select.h /usr/include/bits/sigset.h
fits_sub_image.o: /usr/include/bits/time.h /usr/include/sys/sysmacros.h
fits_sub_image.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_sub_image.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_sub_image.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_sub_image.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_sub_image.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_sub_image.o: /usr/include/bits/posix2_lim.h
fits_sub_image.o: /home/dev/src/cfitsio3.006/include/longnam.h
fits_normalise.o: /usr/include/stdio.h /usr/include/features.h
fits_normalise.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
fits_normalise.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
fits_normalise.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
fits_normalise.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
fits_normalise.o: /usr/include/libio.h /usr/include/_G_config.h
fits_normalise.o: /usr/include/wchar.h /usr/include/bits/wchar.h
fits_normalise.o: /usr/include/gconv.h
fits_normalise.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
fits_normalise.o: /usr/include/bits/stdio_lim.h
fits_normalise.o: /usr/include/bits/sys_errlist.h /usr/include/stdlib.h
fits_normalise.o: /usr/include/sys/types.h /usr/include/time.h
fits_normalise.o: /usr/include/endian.h /usr/include/bits/endian.h
fits_normalise.o: /usr/include/sys/select.h /usr/include/bits/select.h
fits_normalise.o: /usr/include/bits/sigset.h /usr/include/bits/time.h
fits_normalise.o: /usr/include/sys/sysmacros.h
fits_normalise.o: /usr/include/bits/pthreadtypes.h /usr/include/alloca.h
fits_normalise.o: /usr/include/strings.h
fits_normalise.o: /home/dev/src/cfitsio3.006/include/fitsio.h
fits_normalise.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/limits.h
fits_normalise.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/syslimits.h
fits_normalise.o: /usr/include/limits.h /usr/include/bits/posix1_lim.h
fits_normalise.o: /usr/include/bits/local_lim.h /usr/include/linux/limits.h
fits_normalise.o: /usr/include/bits/posix2_lim.h
fits_normalise.o: /home/dev/src/cfitsio3.006/include/longnam.h
targa.o: /usr/include/errno.h /usr/include/features.h
targa.o: /usr/include/sys/cdefs.h /usr/include/bits/wordsize.h
targa.o: /usr/include/gnu/stubs.h /usr/include/gnu/stubs-32.h
targa.o: /usr/include/bits/errno.h /usr/include/linux/errno.h
targa.o: /usr/include/asm/errno.h /usr/include/asm-generic/errno.h
targa.o: /usr/include/asm-generic/errno-base.h /usr/include/stdio.h
targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stddef.h
targa.o: /usr/include/bits/types.h /usr/include/bits/typesizes.h
targa.o: /usr/include/libio.h /usr/include/_G_config.h /usr/include/wchar.h
targa.o: /usr/include/bits/wchar.h /usr/include/gconv.h
targa.o: /usr/lib/gcc/i386-redhat-linux/4.1.1/include/stdarg.h
targa.o: /usr/include/bits/stdio_lim.h /usr/include/bits/sys_errlist.h
targa.o: targa.h
