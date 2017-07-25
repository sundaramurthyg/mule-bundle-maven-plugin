package software.hoegg.mule;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class PrepareBundleMojoTest {

	private static String basedir;

	@Rule
	public MojoRule rule = new MojoRule();

	@BeforeClass
	public static void getSystemProperties() {
		basedir = System.getProperty( "basedir" );
	}

	@Before
	public void prepareApplicationZips() throws IOException {
		for (File appDir : bundledAppsDir().listFiles()) {
			pluginTestDir().mkdirs();
			final File applicationZip = new File(pluginTestDir(), appDir.getName() + ".zip");
			ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(applicationZip));
			try {
				compressSubDirectories(appDir, appDir, zipFile);
			} finally {
				IOUtils.closeQuietly(zipFile);
			}
		}
	}

	private void compressSubDirectories(File root, File subdirectory, ZipOutputStream zip) throws IOException {
		for (File f : subdirectory.listFiles()) {
			if (f.isDirectory()) {
				compressSubDirectories(root, f, zip);
			} else {
				zip.putNextEntry(
					new ZipEntry(f.getAbsolutePath().replace(root.getAbsolutePath() + "/", "")));
				FileInputStream in = new FileInputStream(f);
				IOUtils.copy(in, zip);
				IOUtils.closeQuietly(in);
			}
		}
	}

	@Test
	public void shouldGenerateDeploymentDescriptor() throws Exception {
		execute();

		Properties deployProps = getBundleMuleDeployProperties();
		assertTrue("Missing expected property config.resources",
			deployProps.containsKey("config.resources"));
		List<String> configResources = Arrays.asList(deployProps.getProperty("config.resources").split(","));
		assertEquals("config files", 5, configResources.size());
	}

	@Test
	public void shouldPrefixConfigurationFilenamesWithArtifactId() throws Exception {
		execute();

		Properties deployProps = getBundleMuleDeployProperties();
		assertTrue("Missing expected property config.resources",
			deployProps.containsKey("config.resources"));
		List<String> configResources = Arrays.asList(deployProps.getProperty("config.resources").split(","));
		assertThat(configResources, containsInAnyOrder(
			"test-app1.config1.xml",
			"test-app1.config2.xml",
			"test-app1.globals.xml",
			"test-app2.config-a.xml",
			"test-app2.globals.xml"));
	}

	@Test
	public void shouldCopyConfigurationFiles() throws Exception {
		execute();

		assertBundledFileContents("test-app1.config1.xml", "<config1/>");
		assertBundledFileContents("test-app1.config2.xml", "<config2/>");
		assertBundledFileContents("test-app1.globals.xml", "<globals/>");
		assertBundledFileContents("test-app2.config-a.xml", "<config-a/>");
		assertBundledFileContents("test-app2.globals.xml", "<globals/>");
	}

	private void assertBundledFileContents(String filename, String contents) throws IOException {
		File f1 = new File(bundleTargetDir(), filename);
		assertTrue(f1.exists());
		assertEquals(contents, FileUtils.readFileToString(f1, Charset.defaultCharset()));
	}

	private void execute() throws Exception {
		PrepareBundleMojo mojo = (PrepareBundleMojo) rule.lookupMojo("bundle", new File(bundleProjectDir(), "pom.xml"));
		mojo.execute();
	}

	private Properties getBundleMuleDeployProperties() throws IOException {
		File descriptor = new File(bundleTargetDir(), "mule-deploy.properties");
		assertTrue(descriptor.exists());

		FileInputStream in = FileUtils.openInputStream(descriptor);
		Properties deployProps = new Properties();
		deployProps.load(in);
		IOUtils.closeQuietly(in);
		return deployProps;
	}

	private File bundleProjectDir() {
		return new File( basedir,
			"target/test-classes/simple-bundle-project" );
	}

	private File bundledAppsDir() {
		return new File( basedir,
			"target/test-classes/bundled-apps");
	}

	private File pluginTestDir() {
		return new File( basedir,
			"target/test-bundle-resources");
	}

	private File bundleTargetDir() {
		return new File( basedir,
			"target/bundle-target/mule-bundle" );
	}
}