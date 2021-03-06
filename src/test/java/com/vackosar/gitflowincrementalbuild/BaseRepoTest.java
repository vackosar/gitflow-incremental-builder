package com.vackosar.gitflowincrementalbuild;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import com.vackosar.gitflowincrementalbuild.control.Property;
import com.vackosar.gitflowincrementalbuild.mocks.LocalRepoMock;
import com.vackosar.gitflowincrementalbuild.mocks.MavenSessionMock;
import com.vackosar.gitflowincrementalbuild.mocks.server.TestServerType;

public abstract class BaseRepoTest {

    /** The project properties for the top-level project of {@link #getMavenSessionMock() MavenSessionMock} for config init. */
    protected final Properties projectProperties = new Properties();
    private final boolean useSymLinkedFolder;
    private final TestServerType remoteRepoServerType;

    protected LocalRepoMock localRepoMock;
    /** {@link LocalRepoMock#getRepoDir()} of {@link #localRepoMock}. */
    protected Path repoPath;

    @TempDir
    protected Path repoBaseDir;

    public BaseRepoTest() {
        this(false, null);
    }

    public BaseRepoTest(boolean useSymLinkedFolder, TestServerType remoteRepoServerType) {
        this.useSymLinkedFolder = useSymLinkedFolder;
        this.remoteRepoServerType = remoteRepoServerType;
    }

    @BeforeEach
    protected void before(TestInfo testInfo) throws Exception {
        init();

        // place repo in a sym-linked folder if requested by the concrete test class
        if (useSymLinkedFolder) {
            Path linkTarget = repoBaseDir.resolve("link-target");
            Path link = repoBaseDir.resolve("link");
            Files.createDirectory(linkTarget);

            // - creation of most links requires elevated rights on windows, including the ones
            //   created by Files.createSymbolicLink()
            // - creation of junctions should work without elevated rights but can only be created
            //   via mklink.exe /J ...
            if (SystemUtils.IS_OS_WINDOWS) {
                ProcessUtils.startAndWaitForProcess("mklink", "/J", link.toAbsolutePath().toString(), linkTarget.toAbsolutePath().toString());
            } else {
                Files.createSymbolicLink(link, linkTarget);
            }

            repoBaseDir = link;
        }
        localRepoMock = new LocalRepoMock(repoBaseDir, remoteRepoServerType);
        repoPath = localRepoMock.getRepoDir();
    }

    private void init() {
        projectProperties.setProperty(Property.uncommitted.prefixedName(), "false");
        projectProperties.setProperty(Property.untracked.prefixedName(), "false");
        projectProperties.setProperty(Property.referenceBranch.prefixedName(), "refs/heads/develop");
        projectProperties.setProperty(Property.compareToMergeBase.prefixedName(), "false");
    }

    @AfterEach
    protected void after() throws Exception {
        if (localRepoMock != null) {
            localRepoMock.close();
        }
    }

    protected MavenSession getMavenSessionMock() throws Exception {
        return MavenSessionMock.get(repoPath, projectProperties);
    }

    protected Path getRepoBaseDir() {
        return repoBaseDir;
    }
}
