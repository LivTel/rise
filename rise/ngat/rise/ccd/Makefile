# ngat.rise.ccd Makefile
include ../../../../Makefile.common
include ../../../Makefile.common

PACKAGENAME    = ngat.rise.ccd
PACKAGEDIR     = ngat/rise/ccd
JAR_FILE       = ngat_rise_ccd.jar

SRCS           = CCDLibraryDouble.java CCDLibraryFormatException.java CCDLibraryNativeException.java \
                 CCDLibrary.java CCDLibrarySetupWindow.java

PROJECT_SRC_DIR = $(LT_SRC_HOME)/rise
DOCSDIR 	= $(LT_DOC_HOME)/ngat/javadocs/$(PACKAGEDIR)
LIBDIR		= $(LT_HOME)/classes
JARLIBDIR 	= $(LT_BIN_HOME)/javalib
JAVACFLAGS 	= -g -d $(LIBDIR) -sourcepath $(PROJECT_SRC_DIR) -classpath $(LIBDIR):$(CLASSPATH)
DOCFLAGS 	= -version -author -private

OBJS 		= $(SRCS:%.java=$(LIBDIR)/$(PACKAGEDIR)/%.class)
DOCS 		= $(SRCS:%.java=$(DOCSDIR)/$(PACKAGEDIR)/%.html)

top: classes jar docs 

classes: $(OBJS)

$(LIBDIR)/$(PACKAGEDIR)/%.class: %.java
	$(JAVAC) $(JAVAC_OPTIONS) $(JAVACFLAGS) $<

jar: $(JARLIBDIR)/$(JAR_FILE)

$(JARLIBDIR)/$(JAR_FILE): $(OBJS)
	( cd $(LIBDIR); $(JAR) $(JAR_OPTIONS) $(JARLIBDIR)/$(JAR_FILE) $(PACKAGEDIR)/*.class )

docs: $(DOCS)

$(DOCS): $(SRCS)
	( mkdir -p $(DOCSDIR)/$(PACKAGEDIR); $(JAVADOC) -sourcepath $(PROJECT_SRC_DIR) -classpath $(LIBDIR):$(CLASSPATH) -d $(DOCSDIR) $(DOCFLAGS) $(PACKAGENAME) )

checkin:
	-$(CI) $(CI_OPTIONS) $(SRCS)
	-@for i in $(DIRS); \
	do \
		(echo checkin in $$i...; cd $$i ; $(MAKE) checkin; $(CI) $(CI_OPTIONS) Makefile ); \
	done;

checkout:
	$(CO) $(CO_OPTIONS) $(SRCS)
	@for i in $(DIRS); \
	do \
		(echo checkout in $$i...; cd $$i; $(CO) $(CO_OPTIONS) Makefile; $(MAKE) checkout); \
	done;

clean:
	-$(RM) $(RM_OPTIONS) $(OBJS) $(JARLIBDIR)/$(JAR_FILE) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo cleaning in $$i...; cd $$i ; $(MAKE) clean); \
	done;

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo cleaning in $$i...; cd $$i ; $(MAKE) tidy); \
	done;

classesonly: $(OBJS)
	@for i in $(DIRS); \
	do \
		( echo make in $$i...; mkdir -p $(LT_HOME)/classes/$(PACKAGEDIR); cd $$i ; $(MAKE) classesonly ); \
	done;

