package com.vladsch.git.fixcase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Main {

    static void printUsageInfo() {
        System.out.println("Usage:");
        System.out.println("---------------------");
        System.out.println("java -jar ./gitFixCase.jar -l");
        System.out.println("");
        System.out.println("list files with file case mismatch between git and the file system");
        System.out.println("");
        System.out.println("java -jar ./gitFixCase.jar -f");
        System.out.println("");
        System.out.println("will fix file case in git to match the file system's case for the files");
        System.out.println("");
        System.out.println("java -jar ./gitFixCase.jar -g");
        System.out.println("");
        System.out.println("will fix file case in the file system to match the git's case for the file");
    }

    static String getFileSystemPath(String path) {
        String[] parts = path.split("/");
        File parentDir = new File(".");
        StringBuilder sb = new StringBuilder();
        String sep = "";

        for (String part : parts) {
            File file = new File(parentDir, part);

            sb.append(sep).append(file.getName());
            sep = "/";
            parentDir = file;
        }

        return sb.toString();
    }

    static HashMap<String, String> getMismatchedFiles() {
        Repository repository = null;
        HashMap<String,String> mismatchedFiles = new HashMap<>();

        try {
            repository = new FileRepository("./.git");
            Git git = new Git(repository);

            DirCache dirCache = repository.readDirCache();
            int iMax = dirCache.getEntryCount();


            for (int i = 0; i < iMax; i++) {
                DirCacheEntry entry = dirCache.getEntry(i);
                System.out.format("entry[%d]: %s -> %s\n", i, entry.getPathString(), getFileSystemPath(entry.getPathString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mismatchedFiles;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageInfo();
        } else {
            int iMax = args.length;
            int fileIndex = 0;
            int haveErrors = 0;
            boolean listMismatches =false;
            boolean matchToFileSystem =false;
            boolean matchToGit =false;

outer:
            for (int i = 0; i < iMax; i++) {
                String arg = args[i];

                if (arg.startsWith("-")) {
                    // option
                    switch (arg) {
                        case "-l":
                            // list mismatches
                            listMismatches = true;
                            break;

                        case "-f":
                            // match git to file system
                            matchToFileSystem = true;
                            matchToGit = false;
                            break;

                        case "-g":
                            // match file system to git
                            matchToFileSystem = false;
                            matchToGit = true;
                            break;

                        default:
                            System.err.format("Unknown option %s", arg);
                            haveErrors++;
                            break;
                    }
                } else {
                    System.err.format("Unknown argument %s", arg);
                    haveErrors++;
                }
            }

            if (haveErrors == 0) {
                if (listMismatches) {
                    getMismatchedFiles();
                }

                if (matchToFileSystem) {

                } else if (matchToGit) {

                }
            } else {
                printUsageInfo();
            }

            System.exit(haveErrors);
        }
    }
}
