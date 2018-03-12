# Git file case fixer

Java command line application to list and fix file case mismatches between git and the file system.

Usage:

    java -jar ./gitFixCase.jar option

Options:
* -a list all files in git and their names in file system
* -l list files with file case mismatch between git and the file system
* -f fix file case in git to match the file system's case for the files
* -g fix file case in the file system to match the git's case for the file

Walks up the directory tree to find the `.git/` directory of the repository then applies the
requested options to files in the current directory.

For comfort add an alias to launch the jar file:

alias gitfixcase='java -jar /dirPathToJar/gitFixCase.jar'

### Building with IntelliJ IDEA

The project files are included in this repo. The jar file will contain all the dependencies but
need to have their individual signature files removed. There is a post processing step
`copy.jar` in the `artifacts.xml` ant build script to accomplish this.

After building the project run the `artifacts.xml` ant build script's `copy.jar` target to copy
the jar to project root with signatures removed.

If you have a better way to create a single application jar with dependency signed jar files
please let me know so I can fix it in this project. Ditto for building such a jar file with
Maven.
