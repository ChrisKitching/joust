Source-level compile-time optimisations for Java

A Part II project at the University of Cambridge.

Known to work with javac 1.8.0_20-ea. *probably* works with newer releases of 8.
Incompatible with javac 1.7.* because they weren't using enums in critical places where they
currently use enums.


If you just wish to run JOUST:

The joustBinaries.tar.gz archive available on the downloads page of this repository provides
everything you need. Stick that directory somewhere you know for it and add it to your javac
classpath. The ensuing javac command will be something like:

javac -cp .:joust/\* -processor joust.JOUST -AJOUSTStripAssertions -AJOUSTLogLevel=INFO ...

-cp                       adds all the jars in the joust directory to the classpath.

-procesor joust.JOUST     selects JOUST as an annotation processor to run

-AJOUSTStripAssertions    Has JOUST strip assertions

-AJOUSTLogLevel=INFO      Sets the logging level of JOST to "INFO". Valid values are "SEVERE", 
                          "WARNING", "INFO", "FINE", "FINER", "FINEST".

Other options supported by JOUST:
-AJOUSTMinCSEScore=X      Set the minimum expression complexity threshold for CSE to X.
-AJOUSTHelp               Print a usage message and exit.
-AJOUSTAnnotateLib         Activates library annotation mode. JOUST performs effect analysis on
                          the input and writes the results to the effect cache, but does not
                          perform any optimisation.
-AJOUSTPrintEffectCacheKeys Prints the current contents of the effect cache and exits.

The effect cache is stored at ~/.joust. If it becomes annoyingly big, delete it.

If for some reason you wish to compile Firefox for Android using JOUST, you'll want
joust.patch.gz: a gzipped patch for current mozilla-central that adds JOUST to their build
process. It takes a while.


If you wish to compile JOUST yourself:

Built with Maven 3.2.1. Ostensibly after cloning running `mvn verify' in the top-level
directory of the clones repository will compile the code and the unit/integration tests.
Derailment during integration tests may indicate an incompatability with the version of 
javac in use (particularly if it's an error from the Reflection API).

The mvn assembler plugin will create both a standalone and a dependent jar. To obtain a 
conveniently-distributable collection of the binary dependencies of JOUST, run `mvn clean
dependency:copy-dependencies'. This will cause the libraries on which JOUST depends to be
copied from the Maven repository to the ./target directory. These may be used in conjunction
with the produced JOUST-0.0.6-SNAPSHOT.jar to run JOUST.