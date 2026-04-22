package com.opencellsoft.maven.plugin.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Build a version of an overlay project.
 */
@Mojo(name = "version-builder", defaultPhase = LifecyclePhase.COMPILE)
public class VersionBuilderMojo extends AbstractMojo {

	/**
	 * a commit id.
	 */
	@Parameter(property = "commit")
	String commit;

	/**
	 * the overlay version number.
	 */
	@Parameter(property = "version-overlay")
	String versionOverlay;

	/**
	 * the core version path.
	 */
	@Parameter(property = "core-version-path")
	String coreVersionPath;

	public void execute() throws MojoExecutionException, MojoFailureException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Map<String, String>> versions = new HashMap<>();
		Map<String, String> coreMap = new HashMap<>();
		if (!isValid())
			return;
		File versionFile = new File(coreVersionPath);

		try {
			coreMap = mapper.readValue(versionFile, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			getLog().error("JsonProcessingException: " + e.getMessage(), e);
		} catch (IOException e) {
			getLog().error("IOException: " + e.getMessage(), e);
		}
		getLog().debug("Core Values: " + coreMap);
		Map<String, String> overlayMap = new HashMap<>();
		overlayMap.put("name", "Opencell Utilities Edition");
		overlayMap.put("commit", commit);
		overlayMap.put("version", versionOverlay);
		overlayMap.put("commitDate", String.valueOf(new Date().getTime()));

		getLog().debug("Overlay Values: " + overlayMap);

		versions.put("core", coreMap);
		versions.put("overlay", overlayMap);
		try {
			File resultFile = new File(versionFile.getParentFile(), versionFile.getName());
			mapper.writeValue(resultFile, versions);
			getLog().info("resultFile : " + resultFile.getAbsolutePath());
		} catch (IOException e) {
			getLog().error("WriteValues: " + e.getMessage(), e);
		}
	}

	private boolean isValid() {
		if (StringUtils.isBlank(coreVersionPath)) {
			getLog().error("core file version.json is missing");
			return false;
		}
		return true;
	}

	protected static Path find(String fileName, String searchDirectory) throws IOException {
		try (Stream<Path> files = Files.walk(Paths.get(searchDirectory))) {
			return files
					.filter(f -> f.getFileName().toString().equals(fileName))
					.findFirst().orElseThrow();
		}
	}

}