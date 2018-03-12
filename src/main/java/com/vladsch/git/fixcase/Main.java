package com.vladsch.git.fixcase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    static void printUsageInfo() {
        System.out.println("Usage:");
        System.out.println("    java -jar ./gitFixCase.jar option");
        System.out.println("");
        System.out.println("    -a list all files in git and their names in file system");
        System.out.println("    -l list files with file case mismatch between git and the file system");
        System.out.println("    -f fix file case in git to match the file system's case for the files");
        System.out.println("    -g fix file case in the file system to match the git's case for the file");
    }

    static String getFileSystemPath(Repository repository, String path) {
        String[] parts = path.split("/");
        File parentDir = repository.getDirectory().getParentFile();
        StringBuilder sb = new StringBuilder();
        String sep = "";

        for (String part : parts) {
            File[] listFiles = parentDir.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (file.getName().equalsIgnoreCase(part)) {
                        sb.append(sep).append(file.getName());
                        sep = "/";
                        parentDir = file;
                        break;
                    }
                }
            }
        }

        return sb.toString();
    }

    static HashMap<String, String> getMismatchedFiles() {
        return getMismatchedFiles(false);
    }

    static Repository getRepository() {
        Repository repository = null;
        File dir = new File(".").getAbsoluteFile();
        while (dir != null && dir.exists()) {
            File gitDir = new File(dir, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                try {
                    repository = new FileRepository(gitDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            dir = dir.getParentFile();
        }
        return repository;
    }

    static HashMap<String, String> getMismatchedFiles(boolean listFiles) {
        Repository repository = null;
        HashMap<String, String> mismatchedFiles = new HashMap<>();

        try {
            repository = getRepository();
            if (repository != null) {
                DirCache dirCache = repository.readDirCache();
                String absolutePath = repository.getDirectory().getParentFile().getAbsolutePath();
                if (absolutePath.endsWith("/.")) {
                    absolutePath = absolutePath.substring(0, absolutePath.length() - "/.".length());
                }
                String prefix = new File(".").getAbsoluteFile().getParentFile().getAbsolutePath();
                if (!prefix.equals(absolutePath)) {
                    prefix = prefix.substring(absolutePath.length() + 1);
                } else {
                    prefix = "";
                }

                int iMax = dirCache.getEntryCount();

                for (int i = 0; i < iMax; i++) {
                    DirCacheEntry entry = dirCache.getEntry(i);
                    if (entry.getPathString().startsWith(prefix)) {
                        String fileSystemPath = getFileSystemPath(repository, entry.getPathString());
                        if (listFiles) System.out.format("entry[%d]: %s -> %s\n", i, entry.getPathString(), fileSystemPath);
                        if (!entry.getPathString().equals(fileSystemPath)) {
                            mismatchedFiles.put(entry.getPathString(), fileSystemPath);
                        }
                    }
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
            boolean listAllFiles = false;
            boolean listMismatches = false;
            boolean matchToFileSystem = false;
            boolean matchToGit = false;

            for (int i = 0; i < iMax; i++) {
                String arg = args[i];

                if (arg.startsWith("-")) {
                    // option
                    switch (arg) {
                        case "-a":
                            // list mismatches
                            listAllFiles = true;
                            break;

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
                if (listAllFiles) {
                    HashMap<String, String> mismatchedFiles = getMismatchedFiles(listAllFiles);
                    if (listMismatches) {
                        for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                            System.out.format("git file %s -> %s\n", entry.getKey(), entry.getValue());
                        }
                    }
                } else if (listMismatches) {
                    HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                    for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                        System.out.format("git file %s -> %s\n", entry.getKey(), entry.getValue());
                    }
                }

                if (matchToFileSystem) {
                    Repository repository = null;

                    try {
                        repository = getRepository();
                        if (repository != null) {
                            Git git = new Git(repository);

                            HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                            for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                                System.out.format("renaming git file %s -> %s\n", entry.getKey(), entry.getValue());
                                git.rm().addFilepattern(entry.getKey()).setCached(true).call();
                                git.add().addFilepattern(entry.getValue()).setUpdate(false).call();
                            }
                        }
                    } catch (NoFilepatternException e) {
                        e.printStackTrace();
                    } catch (GitAPIException e) {
                        e.printStackTrace();
                    }
                } else if (matchToGit) {
                    Repository repository = null;

                    repository = getRepository();
                    if (repository != null) {
                        Git git = new Git(repository);

                        HashMap<String, String> mismatchedFiles = getMismatchedFiles();
                        for (Map.Entry<String, String> entry : mismatchedFiles.entrySet()) {
                            System.out.format("renaming file %s -> %s\n", entry.getValue(), entry.getKey());
                            File file = new File(entry.getValue());
                            File toFile = new File(entry.getKey());
                            file.renameTo(toFile);
                        }
                    }
                }
            } else {
                printUsageInfo();
            }

            System.exit(haveErrors);
        }
    }
}
