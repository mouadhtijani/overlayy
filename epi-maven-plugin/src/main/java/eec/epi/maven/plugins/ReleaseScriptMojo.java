package eec.epi.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mojo(name = "release-script", threadSafe = true)
public class ReleaseScriptMojo extends AbstractMojo {

	/**
	 * directory to scan.
	 */
	@Parameter(property = "release-script.dir", defaultValue = "${project.basedir}/src/main/java")
	private String dir;

	/**
	 * regexp to match the script names.
	 */
	@Parameter(property = "release-script.inFormat", defaultValue = ".*\\.java")
	private String inFormat;

	/**
	 * name of the created collection file. Any {dirName} will be changed by the
	 * scanned directory name.
	 */
	@Parameter(property = "release-script.outFormat", defaultValue = "${project.basedir}/src/generated/resources/${project.artifactId}-scripts-${project.version}.postman_collection.json")
	private String outFile;

	/**
	 * name format of the collection inside the collection file. Any {dirName}
	 * will be changed by the directory name.
	 */
	@Parameter(property = "release-script.colFormat", defaultValue = "${project.artifactId}-scripts-${project.version}")
	private String colFormat;

	/**
	 * name format of the collection inside the collection file. Any {dirName}
	 * will be changed by the directory name.
	 */
	@Parameter(property = "release-script.method", defaultValue = "POST")
	private String method;

	/**
	 * name format of the collection inside the collection file. Any {dirName}
	 * will be changed by the directory name.
	 */
	@Parameter(property = "release-script.url")
	private String url;

	@Parameter(property = "release-script.basic.username", defaultValue = "{{server.username}}")
	private String username;

	@Parameter(property = "release-script.basic.password", defaultValue = "{{server.password}}")
	private String password;

	@Parameter(property = "release-script.meta.version")
	private String version;

	@Parameter(property = "release-script.meta.version")
	private String release;

	/**
	 * when set to true, do not add the test on the root collection to check if
	 * response is 2xx
	 */
	@Parameter(property = "release-script.skipResponseTest", defaultValue = "false")
	private boolean skipResponseTest;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File file = new File(dir);
		PostmanCollectionv2_1 coll = convertDir(file);
		if (coll != null) {
			coll.addMetaData(version, release);
			if (!skipResponseTest) {
				coll.addResponseTest();
			}
			String outFileName = outFile.replaceAll("\\{dirName\\}", file.getName());
			coll.write(new File(outFileName));
			getLog().info("exported " + dir + " into " + outFileName);
		}
	}

	protected PostmanCollectionv2_1 convertDir(File dir) {
		if (!dir.isDirectory()) {
			return null;
		}
		PostmanCollectionv2_1 ret = new PostmanCollectionv2_1();
		ret.info.name = colFormat.replaceAll("\\{dirName\\}", dir.getName());
		ret.auth = new PostmanCollectionv2_1.Auth();
		ret.auth.type = "basic";
		ret.auth.basic = new ArrayList<>();
		ret.auth.basic.add(PostmanCollectionv2_1.Item.Request.Header.keyVal("username", username));
		ret.auth.basic.add(PostmanCollectionv2_1.Item.Request.Header.keyVal("password", password));
		streamScripts(dir).sorted(Comparator.comparing(File::getName)).map(this::convertScriptFile).forEach(ret.item::add);
		return ret;
	}

	protected Stream<File> streamScripts(File rootDir) {
		if (!rootDir.isDirectory()) {
			return Stream.empty();
		}
		Stream<File> ret = Stream.of(rootDir.listFiles(f -> f.getName().matches(inFormat))).filter(File::isFile);
		for (File childDir : rootDir.listFiles(File::isDirectory)) {
			ret = Stream.concat(ret, streamScripts(childDir));
		}
		return ret;
	}

	protected PostmanCollectionv2_1.Item convertScriptFile(File scriptFile) {
		PostmanCollectionv2_1.Item ret = PostmanCollectionv2_1.Item.item(scriptFile.getName().replaceAll("\\..*", ""));
		ret.request = new PostmanCollectionv2_1.Item.Request();
		ret.request.method = method;
		ret.request.header = new ArrayList<>();
		PostmanCollectionv2_1.Item.Request.Header contenttype = new PostmanCollectionv2_1.Item.Request.Header();
		contenttype.key = "Content-Type";
		contenttype.value = "application/json";
		ret.request.header.add(contenttype);
		ret.request.url = url;
		ret.request.body = new PostmanCollectionv2_1.Item.Request.Body();
		ret.request.body.mode = "raw";
		try {
			ScriptInstance script = new ScriptInstance();
			script.script = Files.lines(scriptFile.toPath()).collect(Collectors.joining("\n"));
			ret.request.body.raw = script.toJsonLine();
		} catch (IOException e) {
			throw new UnsupportedOperationException("catch this", e);
		}
		return ret;
	}

	protected static class ScriptInstance {
		public String code;
		public Boolean disabled;
		public String script;
		public String type = "JAVA";

		public String toJsonLine() throws JsonProcessingException {
			ObjectMapper mapper = new ObjectMapper();
			// ignore the null fields globally
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			return mapper.writer().writeValueAsString(this);
		}

	}

}
