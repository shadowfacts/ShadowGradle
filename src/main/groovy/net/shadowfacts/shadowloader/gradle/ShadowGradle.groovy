package net.shadowfacts.shadowloader.gradle

import groovy.json.JsonSlurper
import net.shadowfacts.shadowlib.util.InternetUtils
import net.shadowfacts.shadowlib.util.os.OperatingSystem
import org.apache.maven.artifact.repository.MavenArtifactRepository
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author shadowfacts
 */
class ShadowGradle implements Plugin<Project> {

	Project project;

	@Override
	void apply(Project project) {
		this.project = project;

		project.extensions.create("shadowloader", ShadowLoaderExtension)

		project.task("downloadMC") << {
			File mcDir = new File(project.gradle.gradleUserHomeDir.getPath() + "/caches/ShadowLoader/", project.shadowloader.mcVersion)
			if (!mcDir.exists()) mcDir.mkdirs()


			File mcJar = new File(mcDir, project.shadowloader.mcVersion + ".jar")
			if (!mcJar.exists()) {
				InternetUtils.downloadFile("http://s3.amazonaws.com/Minecraft.Download/versions/" + project.shadowloader.mcVersion + "/" + project.shadowloader.mcVersion + ".jar", mcJar)
			}

			File mcJson = new File(mcDir, project.shadowloader.mcVersion + ".json")
			if (!mcJson.exists()) {
				InternetUtils.downloadFile("http://s3.amazonaws.com/Minecraft.Download/versions/" + project.shadowloader.mcVersion + "/" + project.shadowloader.mcVersion + ".json", mcJson)
			}

			project.repositories.with {
				add(mavenCentral())
				add(maven { MavenArtifactRepository repo ->
					repo.name = "Minecraft"
					repo.url = "http://libraries.minecraft.net/"
				})
			}

			def spec = new JsonSlurper().parse(mcJson)

			for (int i = 0; i < spec.libraries.size(); i++) {
				def lib = spec.libraries.get(i)

//				Rules
				boolean skip;
				if (lib.rules != null) {
					for (int j = 0; j < lib.rules.size; j++) {
						def rule = lib.rules.get(j)
						if (rule.action.equalsIgnoreCase("disallow")) {
							if (OperatingSystem.getOS(rule.os) == OperatingSystem.systemOS) {
								skip = true;
								break;
							}
						} else if (rule.action.equalsIgnoreCase("allow")) {
							if (OperatingSystem.getOS(rule.os) != OperatingSystem.systemOS) {
								skip = true;
								break;
							}
						}
					}
					if (skip) continue
				}

//				TODO: Natives
//				TODO: Extract

				project.dependencies.add("compile", lib.name)

			}
		}
	}

	static void main(String[] args) {

		File mcJson = new File(System.getProperty("user.home") + "/.gradle/caches/ShadowLoader/15w41b/15w41b.json")

		def result = new JsonSlurper().parse(mcJson)

		println("done")
	}
}