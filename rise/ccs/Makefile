# Makefile
# $Header: /space/home/eng/cjm/cvs/rise/ccs/Makefile,v 1.1 2003-11-14 15:31:31 cjm Exp $ 

include ../Makefile.common

MAKE = make
DIRS = java test latex npt mat

top:
	@for i in $(DIRS); \
	do \
		(echo making in $$i...; cd $$i; $(MAKE) ); \
	done;

checkin:
	-@for i in $(DIRS); \
	do \
		(echo checkin in $$i...; cd $$i; $(MAKE) checkin; $(CI) $(CI_OPTIONS) Makefile); \
	done;

checkout:
	@for i in $(DIRS); \
	do \
		(echo checkout in $$i...; cd $$i; $(CO) $(CO_OPTIONS) Makefile; $(MAKE) checkout); \
	done;

depend:
	echo ccs package has no depend at the moment.

clean:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo clean in $$i...; cd $$i; $(MAKE) clean); \
	done;

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo tidy in $$i...; cd $$i; $(MAKE) tidy); \
	done;

backup: checkin
	@for i in $(DIRS); \
	do \
		(echo backup in $$i...; cd $$i; $(MAKE) backup); \
	done;
	tar cvf $(BACKUP_DIR)/ccs.tar .
	compress $(BACKUP_DIR)/ccs.tar

# $Log: not supported by cvs2svn $
