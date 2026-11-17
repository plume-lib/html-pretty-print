This directory holds the data for the end-to-end tests of the html-pretty-print program.  The tests
are data, not code, so that they can be run against any implementation of the program, in any
language.  For this project's Java implementation, they are run by
src/test/java/org/plumelib/htmlprettyprint/HtmlPrettyPrintEndToEndTest.java.


The program
===========

The program takes the names of zero or more HTML files as command-line arguments.  It has no
command-line options.  For each file, in order, it converts the HTML to XML (XHTML) and writes the
pretty-printed XML, in UTF-8 and preceded by an XML declaration, to standard output.  For a file
that cannot be read or cannot be converted, it writes an error message to standard error and
continues with the next file.  Its exit status is 1 if any file could not be processed, and 0
otherwise.  It does not read standard input and does not write any file.


Test cases
==========

Each subdirectory of this directory is one test case; the name of the subdirectory is the name of
the test case.  To add a test case, add a subdirectory; no harness code needs to change.

A test case directory contains the following files, all of them optional.  A test harness should
reject a test case that contains any other file, because such a file is probably a misspelled
goal file.

  description.txt      What the test case checks.  The test harness ignores this file.  A
                       description that starts with "BUG:" describes a way in which the program
                       behaves differently than it should; the goal files record what the Java
                       implementation currently does.

  input/               Files for the program to read.  The test harness copies this directory, with
                       its subdirectories, to "input/" in the working directory.

  args.txt             The program's command-line arguments, one per line, in UTF-8; empty lines
                       are ignored.  Use this file when the arguments are not simply the files of
                       "input/": for example, to name a file that does not exist, to name a file
                       twice, or to run the program with no arguments at all (an empty args.txt).
                       If this file is absent, the arguments are the names of every file (not
                       directory) under "input/", relative to the working directory and using "/"
                       as the separator (such as "input/page.html"), sorted in lexicographic order
                       of their UTF-16 code units (for these ASCII names, the same as byte order).

  stdin.txt            The program's standard input.  If this file is absent, standard input is
                       empty.

  goal-out.txt         What the program should write to standard output.  If this file is absent,
                       the program should write nothing to standard output.

  goal-err.txt         What the program should write to standard error.  If neither this file nor
                       goal-err-prefix.txt is present, the program should write nothing to standard
                       error.

  goal-err-prefix.txt  What the program's standard error should start with.  Trailing whitespace,
                       including the final line separator, is removed from this file's contents
                       before the comparison.  It is used instead of goal-err.txt when the rest of
                       the output (here, the text of a Java exception, such as "(No such file or
                       directory)") differs among platforms.

  goal-status.txt      The program's exit status, as a decimal integer.  If this file is absent,
                       the exit status should be 0.

  goal/                The files that the working directory should contain, other than those under
                       "input/", after the program runs.  If this directory is absent, the program
                       should create no files.  (The program does not write files, so no test case
                       has this directory.)


How to run a test case
======================

 1. Create a new, empty working directory.
 2. Copy the test case's "input/" directory, if any, to "input/" in the working directory.
 3. Run the program with the working directory as its current directory, with the arguments
    described above, with standard input from stdin.txt or empty, and with the environment
    variable LC_ALL set to "C.UTF-8" (so that non-ASCII file names are encoded in UTF-8).
 4. Compare the exit status to goal-status.txt.
 5. Normalize standard output and standard error as described below, then compare them to the
    goal files.  Both the program's output and the goal files are UTF-8.
 6. Compare the files in the working directory, other than those under "input/", to "goal/".

Normalization:

 * Line separators:  every CRLF (carriage return, line feed) is replaced by LF, in both the
   program's output and the goal files.  The goal files use LF.  The Java implementation ends each
   line of its standard output with CRLF (the line separator of the XML serializer it uses) and
   each line of its standard error with the platform's line separator.
 * Working directory:  in standard output and standard error, each occurrence of the working
   directory's absolute path is replaced by "{WORKDIR}".  The path is replaced both as the program
   sees it (with symbolic links resolved) and as the harness created it, and both with the
   platform's file separator and with "/".  For example, if the working directory is
   /tmp/junit123, then the error message "Could not read or write file:/tmp/junit123/missing.html:
   ..." is compared as "Could not read or write file:{WORKDIR}/missing.html: ...".


Running the tests against another implementation
================================================

The Java harness runs this project's HtmlPrettyPrint class by default.  To run the same test cases
against another implementation, give the command that runs it as the Gradle property e2e.command:

  ./gradlew test --tests '*EndToEndTest' -Pe2e.command="python3 /path/to/htmlpp.py"

The value is split at whitespace (there is no quoting), and each test case's arguments are appended
to it.  Use absolute paths in it, because the command runs in the test case's working directory.


Byte-exact test data
====================

Because these files are test data, their exact bytes matter: some of them intentionally have
trailing whitespace, lack a final line separator, use CR or CRLF line separators, contain a byte
order mark or control characters, or are not valid UTF-8.  prek.toml excludes this directory from
the hooks that would "fix" such things.
