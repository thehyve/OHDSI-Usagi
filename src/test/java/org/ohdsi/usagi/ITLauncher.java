package org.ohdsi.usagi;

import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/*
 * The intent of this integration test is to create a clean docker container with jre8,
 * copy the Usagi-test.jar into it, and run it. This verifies that the package created for distribution
 * will run with Java 8.
 * Usagi-test.jar will then execute one junit test that verifies that the application
 * properly starts and stops. This serves as a smoke test for the distribution: it has all the dependencies it needs,
 * and starts without problems.
 */
public class ITLauncher {
    public static final DockerImageName JRE8_IMAGE = DockerImageName.parse("eclipse-temurin:8-jre");

    private static final String usagiTestJarPath = Paths.get("target", "Usagi-test.jar").toAbsolutePath().toString();

    private static Logger LOGGER = LoggerFactory.getLogger(ITLauncher.class);
    @ClassRule
    public static GenericContainer<?> jre8Container = new GenericContainer<>(JRE8_IMAGE)
            .withCopyToContainer(MountableFile.forHostPath(usagiTestJarPath), "/tmp/Usagi-test.jar")
            .withCommand("/bin/sh", "-c", "tail -f /dev/null") // keeps the container running until it is explicitly stopped
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    @Test
    public void testNothing() throws IOException, InterruptedException {
        jre8Container.start();
        // install some extra dependencies for a java program that uses Swing for the GUI
        Container.ExecResult installCommand = jre8Container.execInContainer("/bin/sh", "-c",
                "apt update; apt install -y libxtst6 libxrender1 libxi6");
        assertEquals(0, installCommand.getExitCode());
        // run the test, check that it ran well
        Container.ExecResult javaRunCommand = jre8Container.execInContainer("/bin/sh", "-c", "java -jar /tmp/Usagi-test.jar");
        assertEquals(0, javaRunCommand.getExitCode());
        System.out.println(javaRunCommand.getStdout()); // so satisfying to see a test was OK :-)
        jre8Container.stop();
    }
}
