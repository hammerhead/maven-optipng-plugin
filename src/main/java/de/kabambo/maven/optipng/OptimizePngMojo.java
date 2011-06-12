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
     * List of directories to consider.
     *
     * @parameter
     * @required
     */
    private List<String> pngDirectories;

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
    public void execute() throws MojoExecutionException {
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

    private static class OptimizeTask implements Runnable {
        private File image;
        private Log log;

        public OptimizeTask(final File image, final Log log) {
            this.image = image;
            this.log = log;
        }

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

            long sizeOptimized = image.length();
            log.info("Optimized " + image.getPath() + " by "
                + (sizeUnoptimized - sizeOptimized) + " bytes");
        }
    }

    private static Process startProcess(final File image) throws IOException {
        List<String> args = new LinkedList<String>();
        args.add(OPTIPNG_EXE);
        args.add(image.getPath());
        return new ProcessBuilder(args).start();
    }
}
