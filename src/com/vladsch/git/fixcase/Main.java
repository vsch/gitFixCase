package com.vladsch.git.fixcase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main {

    static void printUsageInfo() {
        System.out.println("Usage:");
        System.out.println("    java -jar ./gitFixCase.jar option");
        System.out.println("");
        System.out.println("    -f will fix file case in git to match the file system's case for the files");
        System.out.println("    -g will fix file case in the file system to match the git's case for the file");
        System.out.println("    -l list files with file case mismatch between git and the file system");
    }

    static String getFileSystemPath(String path) {
        String[] parts = path.split("/");
        File parentDir = new File(".");
        StringBuilder sb = new StringBuilder();
        String sep = "";

        for (String part : parts) {
            for (File file : Objects.requireNonNull(parentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    return pathname.getName().equalsIgnoreCase(part);
                }
            }))) {
                sb.append(sep).append(file.getName());
                sep = "/";
                parentDir = file;
                break;
            }
        }

        return sb.toString();
    }

    static HashMap<String, String> getMismatchedFiles() {
        Repository repository = null;
        HashMap<String, String> mismatchedFiles = new HashMap<>();

        try {
            repository = new FileRepository("./.git");
            DirCache dirCache = repository.readDirCache();
            int iMax = dirCache.getEntryCount();

            for (int i = 0; i < iMax; i++) {
                DirCacheEntry entry = dirCache.getEntry(i);
                String fileSystemPath = getFileSystemPath(entry.getPathString());
                //System.out.format("entry[%d]: %s -> %s\n", i, entry.getPathString(), fileSystemPath);
                if (!entry.getPathString().equals(fileSystemPath)) {
                    mismatchedFiles.put(entry.getPathString(), fileSystemPath);
                }
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
            boolean listMismatches = false;
            boolean matchToFileSystem = false;
            boolean matchToGit = false;

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
                    HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                    for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                        System.out.format("git file %s -> %s\n", entry.getKey(), entry.getValue());
                    }
                }

                if (matchToFileSystem) {
                    Repository repository = null;

                    try {
                        repository = new FileRepository("./.git");
                        Git git = new Git(repository);

                        HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                        for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                            System.out.format("renaming git file %s -> %s\n", entry.getKey(), entry.getValue());
                            git.rm().addFilepattern(entry.getKey()).setCached(true).call();
                            git.add().addFilepattern(entry.getValue()).setUpdate(false).call();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoFilepatternException e) {
                        e.printStackTrace();
                    } catch (GitAPIException e) {
                        e.printStackTrace();
                    }
                } else if (matchToGit) {
                    Repository repository = null;

                    try {
                        repository = new FileRepository("./.git");
                        Git git = new Git(repository);

                        HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                        for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                            System.out.format("renaming file %s -> %s\n", entry.getValue(), entry.getKey());
                            File file = new File(entry.getValue());
                            File toFile = new File(entry.getKey());
                            file.renameTo(toFile);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                printUsageInfo();
            }

            System.exit(haveErrors);
        }
    }
}
