package de.kabambo.maven.optipng;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Goal which optimizes PNG images.
 *
 * @TODO recursion?
 *
 * @goal optimize
 * @phase process-sources
 */
public class OptimizePngMojo extends AbstractMojo {
    /**
     * File extension of PNG images.
     */
    private static final String PNG_SUFFIX = ".png";

    /**
     * Optipng executable.
     */
    private static final String OPTIPNG_EXE = "optipng";

    /**
     * Timeout in seconds for processes to terminate.
     */
    private static final int POOL_TIMEOUT = 10;

    /**
     * Lower bound for optimization level passed to optipng.
     */
    private static final int LEVEL_LOWER_BOUND = 0;

    /**
     * Upper bound for optimization level passed to optipng.
     */
    private static final int LEVEL_UPPER_BOUND = 7;

    /**
     * List of directories to consider.
     *
     * @parameter
     * @required
     */
    private List<String> pngDirectories;

    /**
     * Specifies the intensity of compression.
     *
     * @parameter default-value=2
     */
    private int level;

    /**
     * Thread pool containing processes which optimize a single image.
     */
    private ExecutorService pool;

    /**
     * Creates a new instance of this plugin.
     */
    public OptimizePngMojo() {
        pool = Executors.newCachedThreadPool();
    }

    /**
     * Executes the mojo.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (!verifyOptipngInstallation()) {
            throw new MojoExecutionException("Could not find optipng on "
                + "this system");
        }

        if (!verifyLevel()) {
            throw new MojoExecutionException("Invalid level. Must be >= "
                + LEVEL_LOWER_BOUND + " and <= " + LEVEL_UPPER_BOUND);
        }

        for (final String directory : pngDirectories) {
            File d = new File(directory);
            if (!d.exists()) {
                throw new MojoExecutionException("Directory " + directory
                    + " does not exist.");
            }

            if (!d.isDirectory()) {
                throw new MojoExecutionException("The path " + directory
                    + " is not a directory.");
            }

            File[] containedImages = d.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(PNG_SUFFIX);
                }
            });

            for (File image : containedImages) {
                pool.submit(new OptimizeTask(image, getLog()));
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(POOL_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Waiting for process termination "
                + "was interrupted.", e);
        }
    }

    private class OptimizeTask implements Runnable {
        private File image;
        private Log log;

        public OptimizeTask(final File image, final Log log) {
            this.image = image;
            this.log = log;
        }

        @Override
        public void run() {
            Process p = null;

            long sizeUnoptimized = image.length();
            try {
                p = startProcess(image);
            } catch (IOException e) {
                log.error("Failed to start a process.", e);
                return;
            }

            try {
                p.waitFor();
            } catch (InterruptedException e) {
                log.error("Failed to wait for the process to finish.", e);
                return;
            }

            float kbOptimized = (sizeUnoptimized - (long) image.length()) / 1024f;
            float percentageOptimized = kbOptimized / (sizeUnoptimized / 1024f) * 100;

            log.info(String.format("Optimized %s by %.2f kb (%.2f%%)",
                image.getPath(), kbOptimized, percentageOptimized));

        }
    }

    private Process startProcess(final File image) throws IOException {
        List<String> args = new LinkedList<String>();
        args.add(OPTIPNG_EXE);
        args.add("-o");
        args.add(String.valueOf(level));
        args.add(image.getPath());
        return new ProcessBuilder(args).start();
    }

    private static boolean verifyOptipngInstallation() throws MojoExecutionException {
        List<String> args = new LinkedList<String>();
        args.add(OPTIPNG_EXE);

        Process p;
        try {
            p = new ProcessBuilder(args).start();
            p.waitFor();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to verify optipng "
                + "installation", e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Failed to verify optipng "
                + "installation", e);
        }

        return p.exitValue() == 0;
    }

    private boolean verifyLevel() {
        return level >= LEVEL_LOWER_BOUND && level <= LEVEL_UPPER_BOUND;
    }
}

