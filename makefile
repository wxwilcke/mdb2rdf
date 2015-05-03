# only consider these files
.SUFFIXES: .java .class

# JAVA Compiler
JC = javac

# Compiler Flags
JFLAGS = -g -Xlint

# Directories
LIBDIR 	= ./lib/
SRCDIR 	= ./src/
INDIR 	= ./input/
OUTDIR	= ./output/
BINDIR	= ./bin/

# Libraries
LIBS = \
	   commons-lang-2.6.jar \
	   commons-logging-1.2.jar \
	   jackcess-2.0.9.jar \
	   jena-core-2.13.0.jar \
	   slf4j-api-1.7.6.jar \
	   slf4j-log4j12-1.7.6.jar \
	   log4j-1.2.17.jar \
	   xercesImpl-2.11.0.jar \
	   xml-apis-1.4.01.jar \
	   json-simple-1.1.1.jar

# Classpath
CP := $(SRCDIR)

# define a space
space :=
space +=

# Build Target Executable
TARGETS = \
	 	  Convert.class \
		  RdbToRdf.class \
		  Config.class

# default target
default: mdb2rdf

# target build
mdb2rdf:
	$(foreach lib, $(LIBS), $(eval CP += $(LIBDIR)$(lib))) 
	$(eval CP := $(subst $(space),:,$(CP)))
	$(JC) -cp $(CP) -d $(BINDIR) $(JFLAGS) $(SRCDIR)*.java

# clean class files
clean:
	$(RM) $(BINDIR)*.class
