# Makefile
# $Header: /home/dev/src/ccs/RCS/Makefile,v 1.2 2007/01/09 15:03:04 cjm Exp $ 

include ../../Makefile.common
include ../Makefile.common

MAKE = make
DIRS = java test 
#latex npt mat

top:
	@for i in $(DIRS); \
	do \
		(echo making in $$i...; $(MAKE) -C $$i ); \
	done;

checkin:
	-@for i in $(DIRS); \
	do \
		(echo checkin in $$i...; $(MAKE) -C $$i checkin; cd $$i; $(CI) $(CI_OPTIONS) Makefile); \
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
		(echo clean in $$i...; $(MAKE) -C $$i clean); \
	done;

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo tidy in $$i...; $(MAKE) -C $$i tidy); \
	done;

backup: checkin
	@for i in $(DIRS); \
	do \
		(echo backup in $$i...; $(MAKE) -C $$i backup); \
	done;
	tar cvf $(BACKUP_DIR)/ccs.tar .
	compress $(BACKUP_DIR)/ccs.tar

# $Log: Makefile,v $
# Revision 1.2  2007/01/09 15:03:04  cjm
# Changed some 'cd's to $(MAKE) -C.
#
# Revision 1.1  2003/11/14 15:31:31  cjm
# Initial revision
#
