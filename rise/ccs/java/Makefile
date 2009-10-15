# Makefile
# $Header: /space/home/eng/cjm/cvs/rise/ccs/java/Makefile,v 1.1 2009-10-15 10:21:54 cjm Exp $ 
include ../../../Makefile.common
include ../../Makefile.common
include	../Makefile.common

INCDIR 		= $(RISE_CCS_SRC_HOME)/include
BINDIR 		= $(RISE_CCS_BIN_HOME)/java
DOCSDIR 	= $(RISE_CCS_DOC_HOME)/javadocs
DOCFLAGS 	= -version -author -private
SRCS 		= $(MAIN_SRCS) $(IMPL_SRCS)

MAIN_SRCS = Ccs.java CcsConstants.java CcsArgumentParser.java CcsStatus.java \
CcsTCPClientConnectionThread.java CcsTCPServer.java CcsTCPServerConnectionThread.java CcsREBOOTQuitThread.java
IMPL_SRCS = $(BASE_IMPL_SRCS) $(CALIBRATE_IMPL_SRCS) $(EXPOSE_IMPL_SRCS) $(INTERRUPT_IMPL_SRCS) $(SETUP_IMPL_SRCS)
BASE_IMPL_SRCS = JMSCommandImplementation.java CommandImplementation.java UnknownCommandImplementation.java \
CCDLibraryImplementation.java FITSImplementation.java ACQUIREImplementation.java CALIBRATEImplementation.java \
EXPOSEImplementation.java INTERRUPTImplementation.java SETUPImplementation.java 
CALIBRATE_IMPL_SRCS = ARCImplementation.java BIASImplementation.java DARKImplementation.java \
DAY_CALIBRATEImplementation.java LAMPFLATImplementation.java SKYFLATImplementation.java \
TWILIGHT_CALIBRATEImplementation.java 
EXPOSE_IMPL_SRCS = GLANCEImplementation.java MOVIEImplementation.java MULTRUNImplementation.java \
RUNATImplementation.java SAVEImplementation.java
INTERRUPT_IMPL_SRCS = ABORTImplementation.java GET_STATUSImplementation.java PAUSEImplementation.java \
REBOOTImplementation.java RESUMEImplementation.java STOPImplementation.java
SETUP_IMPL_SRCS = CONFIGImplementation.java LAMPFOCUSImplementation.java SET_LOGGINGImplementation.java \
STARFOCUSImplementation.java TELFOCUSImplementation.java TESTImplementation.java

OBJS 		= $(SRCS:%.java=$(BINDIR)/%.class)
DOCS 		= $(SRCS:%.java=$(DOCSDIR)/%.html)

CONFIG_SRCS	= ltccd1.ccs.properties ltccd1.ccs.net.properties \
	ltccd1.fits.properties \
	ltccd2.ccs.properties ltccd2.ccs.net.properties ltccd2.fits.properties \
	ltccd3.ccs.properties ltccd3.ccs.net.properties ltccd3.fits.properties \
	ltccd4.ccs.properties ltccd4.ccs.net.properties ltccd4.fits.properties \
	rise.ccs.properties rise.ccs.net.properties rise.fits.properties \
	ftccd1.ccs.properties ftccd1.ccs.net.properties ftccd1.fits.properties \
	ftccd2.ccs.properties ftccd2.ccs.net.properties ftccd2.fits.properties 
CONFIG_OBJS	= $(CONFIG_SRCS:%.properties=$(BINDIR)/%.properties)

top: $(OBJS) config docs

$(BINDIR)/%.class:%.java
	$(JAVAC) $(JAVAC_OPTIONS) -d $(BINDIR) $<

config: $(CONFIG_OBJS)

$(BINDIR)/%.properties: %.properties
	$(CP) $< $@

docs: $(DOCS)

$(DOCS): $(SRCS)
	$(JAVADOC) -d $(DOCSDIR) $(DOCFLAGS) $(SRCS)

checkout:
	$(CO) $(CO_OPTIONS) $(SRCS)

checkin:
	-$(CI) $(CI_OPTIONS) $(SRCS)

clean: tidy
	-$(RM) $(RM_OPTIONS) $(OBJS)

tidy:
	-$(RM) $(RM_OPTIONS) ccs_error.txt ccs_log.txt $(TIDY_OPTIONS) *.fits

backup: tidy

# $Log: not supported by cvs2svn $
# Revision 1.30  2009/10/06 13:53:23  eng
# *** empty log message ***
#
# Revision 1.29  2007/06/19 16:39:21  cjm
# Added ltccd4 properties.
#
# Revision 1.28  2004/05/24 18:09:15  cjm
# Added ftccd2.
# Removed all livjm.ac.uk style property names.
#
# Revision 1.27  2004/01/15 15:19:19  cjm
# Added individual fits.properties for each installation.
#
# Revision 1.26  2003/01/28 16:28:42  cjm
# Removed fundamental mode.
#
# Revision 1.25  2002/12/16 17:08:20  cjm
# More config sources.
#
# Revision 1.24  2002/05/23 12:44:12  cjm
# New implementations.
#
# Revision 1.23  2001/03/09 16:22:36  cjm
# Added CcsREBOOTQuitThread.java source.
#
# Revision 1.22  2000/06/30 16:12:48  cjm
# Added CcsArgumentParser.java source.
#
# Revision 1.21  2000/06/13 17:24:15  cjm
# *** empty log message ***
#
# Revision 1.20  2000/06/01 14:01:59  cjm
# ccsFitsHeader replaced by ngat.fits.FitsHeader.
#
# Revision 1.19  2000/02/16 16:43:11  cjm
# Removed ccsreboot targets.
#
# Revision 1.18  2000/02/08 18:13:02  cjm
# backup.
#
# Revision 1.17  1999/12/10 11:20:32  cjm
# Added backup target.
#
# Revision 1.16  1999/11/01 10:50:32  cjm
# Added implementation classes.
#
# Revision 1.15  1999/10/28 11:33:28  cjm
# Added UnknownCommandImplementation.java.
#
# Revision 1.14  1999/10/27 16:23:24  cjm
# Implementation classes addded.
#
# Revision 1.13  1999/09/08 11:14:56  cjm
# Changed checkin target to use CI_OPTIONS.
#
# Revision 1.12  1999/09/08 09:19:08  cjm
# Changed checkin target to accept failure of the ci command
# (if the file is not checked out locked).
#
# Revision 1.11  1999/08/23 17:11:41  cjm
# Changed for ltccd1 directory setup.
#
# Revision 1.10  1999/07/09 14:03:27  dev
# Clean/Tidy option improvement for backup
#
# Revision 1.9  1999/07/02 14:34:11  dev
# Added CcsFitsHeader and CcsFitsHeaderException targets
#
# Revision 1.8  1999/06/30 16:31:17  dev
# changed tidy so that remove of latex directory works
#
# Revision 1.7  1999/06/24 12:40:21  dev
# "Backup"
#
# Revision 1.6  1999/06/07 16:56:23  dev
# "Backup"
#
# Revision 1.5  1999/05/28 09:54:34  dev
# "Name
#
# Revision 1.4  1999/04/27 11:26:51  dev
# Backup
#
# Revision 1.3  1999/03/25 14:02:57  dev
# Backup
#
# Revision 1.2  1999/03/19 11:50:29  dev
# Backup
#
# Revision 1.1  1999/03/16 17:05:21  dev
# Backup
#
