package org.ahedstrom.maven.plugin.diff;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal which compares to files
 *
 * @goal diff
 */
public class DiffMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * A list of <code>fileSet</code> rules to select files and directories.
     *
     * @parameter
     * @required
     */
    private List<FileSet> originalFiles;

    /**
     * URIs to revised files.
     *
     * @parameter
     * @required
     */
    private String revisedFilesDir;

    /**
     * Set to true if the build should be aborted on diff found.
     *
     * @parameter expression="${diff.abortBuildOnDiff}" default-value=false
     */
    private boolean abortBuildOnDiff;

    /**
     * Set to false if empty lines should be compared
     *
     * @parameter expression="${diff.removeEmptyLines}" default-value=true
     */
    private boolean removeEmptyLines;

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening
     * a communications link to the resource referenced by originalFile and/or
     * revisedFile if they are located at a remote location. If the timeout expires before
     * the connection can be established, a java.net.SocketTimeoutException is raised.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @parameter expression="${diff.connectTimeout}" default-value=5000
     */
    private int connectTimeout;

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. A non-zero
     * value specifies the timeout when reading from Input stream when a
     * connection is established to a resource. If the timeout expires before
     * there is data available for read, a java.net.SocketTimeoutException is
     * raised. A timeout of zero is interpreted as an infinite timeout.
     *
     * @parameter expression="${diff.readTimeout}" default-value=10000
     */
    private int readTimeout;

    /**
     * Set to true if the build should should skip the diff
     *
     * @parameter expression="${diff.skipDiff}" default-value=false
     */
    private boolean skipDiff;

    private Log log;

    public DiffMojo() {
    }

    public DiffMojo(List<FileSet> originalFiles, String revisedFilesDir) {
        this.originalFiles = originalFiles;
        this.revisedFilesDir = revisedFilesDir;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        try {
            for (FileSet fileSet : originalFiles) {
                if (!skipDiff) {
                    if (!(new File(fileSet.getDirectory()).isDirectory())) {
                        log.warn("Directory does not exist: " + fileSet.getDirectory());
                    }
                    final List<String> fileList = toFileList(fileSet);
                    boolean diffFound = false;
                    for (String fileName : fileList) {
                        File file = new File(fileSet.getDirectory() + FileUtils.FS + fileName);
                        List<String> original = toLines(file);
                        final File revisedFile = new File(revisedFilesDir + FileUtils.FS + fileName);
                        List<String> revised = toLines(revisedFile);

                        Patch<String> patch = DiffUtils.diff(original, revised);

                        if (!patch.getDeltas().isEmpty()) {
                            log.warn(String.format("The resources [%s] and [%s] are different:", file, revisedFile));
                            for (Delta<String> delta : patch.getDeltas()) {
                                log.warn(
                                        String.format(
                                                "\t[original] -> %s\n\t[revised]  -> %s", delta.getOriginal().toString(),
                                                delta.getRevised().toString()
                                        )
                                );
                            }
                            diffFound = true;
                        } else {
                            log.debug(String.format("The resources: [%s] and [%s] are identical", file, revisedFile));
                        }
                    }
                    if (diffFound) {
                        if (abortBuildOnDiff) {
                            throw new MojoFailureException("diffs found! See above");
                        }
                    }
                } else {
                    log.info(String.format("Skipping diff because skipDiff is set to %s", skipDiff));
                }
            }
        } catch (Exception e) {
            if (e instanceof MojoFailureException) {
                throw (MojoFailureException) e;
            }
            log.error("ERRORS comparing files.", e);
        }
    }

    private List<String> toLines(File file) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = null;
        try {
            in = createReader(file);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().length() == 0 && removeEmptyLines) {
                    continue;
                }
                lines.add(line);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }

        return lines;
    }

    private BufferedReader createReader(File file) throws IOException, MojoFailureException {
        log.debug(file.toString());

        if (file.exists()) {
            return new BufferedReader(new FileReader(file));
        } else {
            throw new MojoFailureException(String.format("%s doesn't exist", file.getAbsolutePath()));
        }
    }

    public List<String> toFileList(FileSet fileSet) throws IOException {
        log.debug("Original File Dir:" + fileSet.getDirectory());
        File directory = new File(fileSet.getDirectory());
        String includes = toString(fileSet.getIncludes());
        String excludes = toString(fileSet.getExcludes());
        return FileUtils.getFileNames(directory, includes, excludes, false);
    }

    private static String toString(List<String> strings) {
        if (strings.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(string);
        }
        return sb.toString();
    }
}
