Introduction
------------
This maven plugin invokes [OptiPNG](http://optipng.sourceforge.net/ "OptiPNG Homepage") on a set of images. OptiPNG is a PNG optimizer which reduces the file size of images by running a lossless recompression.

For sufficient performance of your build process, this plugin processes images in parallel.

Requirements
------------
It is assumed that you have `optipng` installed on your system and that the executeble is available with your `$PATH`.

This plugin has only been tested on Linux.

Usage
-----
The following snippet demonstates a sample usage of this plugin.

	<plugin>
		<groupId>de.kabambo</groupId>
		<artifactId>maven-optipng-plugin</artifactId>
		<version>1.0-SNAPSHOT</version>
		<!-- Execute optimize goal of this plugin by default -->
		<executions>
			<execution>
				<goals>
					<goal>optimize</goal>
				</goals>
			</execution>
		</executions>
		<configuration>
			<!-- You can provide a list of directories containing images to be optimized here -->
			<pngDirectories>
				<pngDirectory>${basedir}/src/main/webapp/png</pngDirectory>
			</pngDirectories>
		</configuration>
	</plugin>

