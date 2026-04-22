package com.opencellsoft.maven.plugin.validator;

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public class VersionBuilderMojoTest extends TestCase {

	VersionBuilderMojo mojo = new VersionBuilderMojo();

	public void testExecute() throws MojoExecutionException, MojoFailureException {
		mojo.versionOverlay = "14.2.X";
		mojo.commit = "TEST--14.2.X";
		mojo.coreVersionPath = getClass().getResource("/version.json").getPath();
		mojo.execute();
	}
}