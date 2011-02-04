package org.hedstrom.maven.plugin.diff;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * Goal which compares to files
 * 
 * @goal diff
 * 
 */
public class DiffMojo extends AbstractMojo {
	/**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;
	
	/**
	 * URIs to original files
	 * 
	 * @parameter
	 * @required
	 */
	private URI[] originalFiles;
	
	/**
	 * URIs to revised files.
	 * 
	 * @parameter
	 * @required
	 */
	private URI[] revisedFiles;
	
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
	

	private Log log;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		log = getLog();
		try {
			if(originalFiles.length != revisedFiles.length) {
				log.error(String.format("Original and revised files must match [originals: %s] [revised: %s]", originalFiles.length, revisedFiles.length));
			} else {
				boolean diffFound = false;
				for(int i = 0; i < originalFiles.length; ++i) {
					List<String> original = toLines(originalFiles[i]);
					List<String> revised = toLines(revisedFiles[i]);

					Patch patch = DiffUtils.diff(original, revised);
					
					if(!patch.getDeltas().isEmpty()) {
						for(Delta delta : patch.getDeltas()) {
							log.warn(String.format("diff: \n\t[original] -> %s\n\t[revised] -> %s", delta.getOriginal().toString(), delta.getRevised().toString()));
						}
						diffFound = true;
					} else {
						log.info(String.format("The resources: [%s] and [%s] are identical", originalFiles[i].toString(), revisedFiles[i].toString()));
					}
				}
				if(diffFound) {
					if(abortBuildOnDiff) {
						throw new MojoFailureException("diffs found! See above");
					}
				}
			}
		} catch (Exception e) {
			if(e instanceof MojoFailureException) {
				throw (MojoFailureException) e;
			}
			log.error("ERRORS comparing files.", e);
		}
	}
	
	private List<String> toLines(URI uri) throws Exception {
		List<String> lines = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = createReader(uri);
			String line = null;
			while ((line = in.readLine()) != null) {
				if(line.trim().length() == 0 && removeEmptyLines) {
					continue;
				}
				lines.add(line);
			}
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException ignore) {}
			}
		}
		
		return lines;
	}
	
	private BufferedReader createReader(URI uri) throws IOException {
		log.debug(uri.toString());
		
		BufferedReader in = null;
		if (!uri.isAbsolute() || "file".equals(uri.getScheme())) {
			File f = null;
			if(!uri.isAbsolute()) {
				f = new File(project.getBasedir() , uri.toString());
			} else {
				f = new File(String.format("%s%s", uri.getAuthority(),
						uri.getPath()));
			}
			log.debug("***** file -> " + f.getAbsolutePath());
			if(f.exists()) {
				in = new BufferedReader(new FileReader(f));
			} else {
				throw new IOException(String.format("%s doesn't exist", f.getAbsolutePath()));
			}
		} else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
			URLConnection conn = uri.toURL().openConnection();
			conn.setConnectTimeout(connectTimeout);
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} else {
			throw new IllegalArgumentException(String.format("unsupported uri: %s", uri.toString()));
		}
		return in;
	}
}
