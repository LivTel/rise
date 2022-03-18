# Makefile
# $Header: /space/home/eng/cjm/cvs/rise/ccs/test/Makefile,v 1.3 2022-03-14 15:22:12 cjm Exp $ 

include ../../../Makefile.common
include ../../Makefile.common
include ../Makefile.common

BINDIR 		= $(RISE_CCS_BIN_HOME)/test
DOCSDIR 	= $(RISE_CCS_DOC_HOME)/test_javadocs
DOCFLAGS 	= -version -author -private
SRCS = MakeISSCommandFile.java SendISSCommandFile.java SicfTCPClientConnectionThread.java \
	SicfTCPServer.java SicfTCPServerConnectionThread.java MakeCCDConfigFile.java \
	SendConfigCommand.java SendRISEConfigCommand.java SendMultrunCommand.java MakeRISEConfigFile.java
OBJS 		= $(SRCS:%.java=$(BINDIR)/%.class) 
DOCS 		= $(SRCS:%.java=$(DOCSDIR)/%.html)
SCRIPTS		= sendisscommands.csh calibrate_ccd_dark.csh calibrate_ccd_light.csh test_config.csh \
		test_ccd_stop.csh
BIN_SCRIPTS	= $(SCRIPTS:%=$(BINDIR)/%)

top: $(OBJS) docs scripts

$(BINDIR)/%.class:%.java
	$(JAVAC) $(JAVAC_OPTIONS) -d $(BINDIR) $<

docs: $(DOCS)

$(DOCS): $(SRCS)
	$(JAVADOC) -d $(DOCSDIR) $(DOCFLAGS) $(SRCS)

scripts: $(BIN_SCRIPTS)

$(BINDIR)/%.csh:%.csh
	$(CP) $< $@

$(BINDIR)/%:%
	$(CP) $< $@

checkout:
	$(CO) $(CO_OPTIONS) $(SRCS)

checkin:
	-$(CI) $(CI_OPTIONS) $(SRCS)

clean: tidy
	-$(RM) $(RM_OPTIONS) $(OBJS) $(BINDIR)/*.obj

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)

backup: tidy

# $Log: not supported by cvs2svn $
# Revision 1.2  2009/10/21 08:58:41  cjm
# Changed to RISE_CCS_BIN_HOME/ RISE_CCS_DOC_HOME.
#
# Revision 1.1  2009/10/15 10:21:57  cjm
# Initial revision
#
# Revision 1.15  2004/06/09 15:35:58  cjm
# Allows scripts with no .csh end.
#
# Revision 1.14  2002/12/16 17:25:40  cjm
# *** empty log message ***
#
# Revision 1.13  2001/07/03 18:26:09  cjm
# Added SendMultrunCommand.java.
#
# Revision 1.12  2001/03/05 19:19:30  cjm
# Added SendConfigCommand.java.
#
# Revision 1.11  1999/12/10 11:22:21  cjm
# Added backup target.
#
# Revision 1.10  1999/09/08 11:19:43  cjm
# Changed checkin target to use CI_OPTIONS.
#
# Revision 1.9  1999/09/08 09:20:51  cjm
# Changed checkin target so that it accepts failure
# (from files not checked out as locked).
#
# Revision 1.8  1999/08/24 09:50:50  cjm
# Changed to reflect new directory structure on ltccd1.
# Java class files now built with generic rather than specific rule.
#
# Revision 1.7  1999/07/09 14:08:21  dev
# Get rids of objs on Clean target
#
# Revision 1.6  1999/07/09 14:04:01  dev
# Clean/Tidy option improvement for backup.
#
# Revision 1.5  1999/06/24 12:40:21  dev
# "Backup"
#
# Revision 1.4  1999/06/07 16:56:23  dev
# "Backup"
#
# Revision 1.3  1999/05/10 15:58:25  dev
# "Backup"
#
# Revision 1.2  1999/04/27 11:26:52  dev
# Backup
#
# Revision 1.1  1999/03/19 11:52:53  dev
# Backup
#
#
