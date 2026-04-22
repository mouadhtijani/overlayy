package eec.epi.maven.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * maven plugin to merge several postman collections together.
 * <p>
 * Default behavior is to search all the postman collections src/main/resources
 * and group them into resources.postman_collection.json, in the collection
 * "resources"
 * </p>
 *
 *
 */
@Mojo(name = "release-postman", threadSafe = true)
public class ReleasePostmanMojo extends AbstractMojo {

	/**
	 * list of directories to scan
	 */
	@Parameter(property = "release-postman.inputdirs")
	private String[] inputDirs;

	/**
	 * if {@link #inputDirs} is not set, default directory to scan.
	 */
	@Parameter(property = "release-postman.defaultInputDir", defaultValue = "${project.basedir}/src/main/resources")
	private String defaultInputDir;

	/**
	 * regexp to match the collections names.
	 */
	@Parameter(property = "release-postman.inFormat", defaultValue = ".*\\.postman_collection\\.json")
	private String inFormat;

	/**
	 * name of the aggregated file. Any {dirName} will be changed by the directory
	 * name.
	 */
	@Parameter(property = "release-postman.outFile", defaultValue = "${project.basedir}/src/generated/resources/{dirName}.postman_collection.json")
	private String outFile;

	/**
	 * name format of the collection inside the aggregated file. Any {dirName}
	 * will be changed by the directory name.
	 */
	@Parameter(property = "release-postman.colFormat", defaultValue = "{dirName}")
	private String colFormat;

	/**
	 * when set to true, do not add the test on the root collection to check if
	 * response is 2xx
	 */
	@Parameter(property = "release-postman.skipResponseTest", defaultValue = "false")
	private boolean skipResponseTest;

	/**
	 * when no collection is found, if this is set, an empty collection is create
	 */
	@Parameter(property = "release-postman.emptyCollectionName")
	private String emptyCollectionName;

	/**
	 * when set to true, for each folder to be scanned, only its children are
	 * scanned for collections
	 */
	@Parameter(property = "release-postman.skipRoot", defaultValue = "false")
	private boolean skipRoot;

	/**
	 * if a directory name does not match this pattern, then its collections are
	 * not added.
	 */
	@Parameter(property = "release-postman.dirMatches", defaultValue = ".*")
	private String dirMatches;

	@Parameter(property = "release-postman.meta.version")
	private String version;

	@Parameter(property = "release-postman.meta.version")
	private String release;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<File> dirList = streamInputDirs().collect(Collectors.toList());
		int done = 0;
		for (File dir : dirList) {
			PostmanCollectionv2_1 col = convertDir(dir);
			if (col != null) {
				col.addMetaData(version, release);
				if(!skipResponseTest) {
					col.addResponseTest();
				}
				String outFileName = outFile.replaceAll("\\{dirName\\}", dir.getName());
				col.write(new File(outFileName));
				done++;
				getLog().info("exported " + dir + " into " + outFileName);
			} else {
				getLog().info("no collection to merge from " + dir);
			}
		}
		if (done == 0 && emptyCollectionName != null) {
			getLog().info("creating empty collection into " + emptyCollectionName);
			PostmanCollectionv2_1 ret = new PostmanCollectionv2_1();
			ret.info.name = "empty";
			ret.addMetaData(version, release);
			ret.write(new File(emptyCollectionName));
		}
	}

	protected Stream<File> streamInputDirs() {
		String[] dirs = inputDirs == null ? new String[] { defaultInputDir } : inputDirs;
		return Stream.of(dirs).map(File::new);
	}

	protected PostmanCollectionv2_1 convertDir(File dir) {
		if (!dir.isDirectory()) {
			return null;
		}
		List<PostmanCollectionv2_1> childrenCollections = new ArrayList<>();
		File[] childrenFiles = dir.listFiles();
		Arrays.sort(childrenFiles, Comparator.comparing(File::getName));
		for (File child : childrenFiles) {
			PostmanCollectionv2_1 childCol = null;
			if (child.isDirectory()) {
				childCol = makeDirCollection(child);
			} else if (!skipRoot) {
				childCol = makeFileCollection(child);
			}
			if (childCol != null) {
				childrenCollections.add(childCol);
			}
		}
		if (childrenCollections.isEmpty()) {
			return null;
		}
		PostmanCollectionv2_1 ret = new PostmanCollectionv2_1();
		ret.info.name = colFormat.replaceAll("\\{dirName\\}", dir.getName());
		for (PostmanCollectionv2_1 merge : childrenCollections) {
			ret.item.add(merge.toItem());
		}
		return ret;
	}

	protected PostmanCollectionv2_1 makeDirCollection(File dir) {
		if (!dir.isDirectory() || !dir.getName().matches(dirMatches)) {
			return null;
		}
		List<PostmanCollectionv2_1> childrenCollections = new ArrayList<>();
		File[] childrenFiles = dir.listFiles();
		Arrays.sort(childrenFiles, Comparator.comparing(File::getName));
		for (File child : childrenFiles) {
			PostmanCollectionv2_1 childCol = null;
			if (child.isDirectory()) {
				childCol = makeDirCollection(child);
			} else {
				childCol = makeFileCollection(child);
			}
			if (childCol != null) {
				childrenCollections.add(childCol);
			}
		}
		if (childrenCollections.isEmpty()) {
			return null;
		}
		PostmanCollectionv2_1 ret = new PostmanCollectionv2_1();
		ret.info.name = dir.getName();
		for (PostmanCollectionv2_1 c : childrenCollections) {
			ret.item.add(c.toItem());
		}
		return ret;
	}

	protected PostmanCollectionv2_1 makeFileCollection(File file) {
		if (!file.isFile() || !file.getName().matches(inFormat)) {
			return null;
		}
		PostmanCollectionv2_1 ret = PostmanCollectionv2_1.loadCollectionFile(file);
		getLog().debug("added collection " + ret.info.name);
		return ret;
	}

}
