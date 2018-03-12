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


