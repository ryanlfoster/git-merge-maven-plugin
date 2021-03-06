package cool.arch.maven.plugin.gitmerge.mojo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Objects.requireNonNull;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import cool.arch.maven.plugin.gitmerge.model.Branch;
import cool.arch.maven.plugin.gitmerge.model.MergeStrategy;
import cool.arch.maven.plugin.gitmerge.model.Release;
import cool.arch.maven.plugin.gitmerge.model.Releases;

/**
 *
 */
@Mojo(name="add", requiresOnline=true, requiresProject=true)
public class AddMojo extends AbstractGitMergeMojo {

	/**
	 * Filename of the releases tracking file.
	 */
	private static final String RELEASES_JSON = "releases.json";

	@Parameter(defaultValue="${targetBranch}")
	private String targetBranch;

	/**
	 * File where the releases.json is located.
	 */
	private	File releasesJson = null;

	/**
	 * Executes the Maven Mojo.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		discoverRepoDirectory();
		validateRepoExists();

		if (isChildProject()) {
			return;
		}

		releasesJson = new File(getBaseDirectory(), RELEASES_JSON);

		if (targetBranch == null || !targetBranch.startsWith("release/")) {
      throw new MojoFailureException("Invalid target branch name.  Branch names must start with release/");
		}

		if (!releasesJsonExists()) {
			throw new MojoFailureException("releases.json does not exist.  Please run git-merge:create to create the releases.json first");
		}

		final Releases releases = readReleasesJson();

		if (releases.getReleases() == null) {
		  releases.setReleases(new ArrayList<Release>());
		}

		if (!branchExists(targetBranch)) {
		  throw new MojoFailureException(String.format("Branch %s does not exists", targetBranch));
		}

		if (containsBranch(releases, targetBranch)) {
      throw new MojoFailureException(String.format("Branch %s already is configured in releases.json", targetBranch));
		}

    final Release release = new Release();
    release.setTargetBranch(targetBranch);
    release.setStrategy(MergeStrategy.OCTOPUS);
    release.setBranches(new ArrayList<Branch>());

    releases.getReleases().add(release);

		writeReleasesJson(releases);
	}

	private boolean releasesJsonExists() {
		return (releasesJson != null && releasesJson.isFile());
	}

  private Releases readReleasesJson() throws MojoFailureException {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Releases releases = null;

    try {
      releases = mapper.readValue(releasesJson, Releases.class);

    } catch (final IOException e) {
      throw new MojoFailureException("Error writing releases.json", e);
    }

    return releases;
  }

	private void writeReleasesJson(final Releases releases) throws MojoFailureException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		try {
			mapper.writeValue(releasesJson, releases);
		} catch (final IOException e) {
			throw new MojoFailureException("Error writing releases.json", e);
		}
	}

	private boolean containsBranch(final Releases releases, final String targetBranch) {
	  requireNonNull(targetBranch, "targetBranch shall not be null");

	  boolean branchFound = false;

	  for (final Release release : releases.getReleases()) {
	    if (targetBranch.equals(release.getTargetBranch())) {
	      branchFound = true;
	      break;
	    }
	  }

	  return branchFound;
	}

	public File getReleasesJson() {
		return releasesJson;
	}

	public void setReleasesJson(final File releasesJson) {
		this.releasesJson = releasesJson;
	}
}
