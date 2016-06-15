/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.conga.tooling.maven.plugin;

import static io.wcm.devops.conga.tooling.maven.plugin.BuildConstants.CLASSPATH_ENVIRONMENTS_DIR;
import static io.wcm.devops.conga.tooling.maven.plugin.BuildConstants.CLASSPATH_ROLES_DIR;
import static io.wcm.devops.conga.tooling.maven.plugin.BuildConstants.CLASSPATH_TEMPLATES_DIR;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.ImmutableList;

import io.wcm.devops.conga.generator.Generator;
import io.wcm.devops.conga.generator.GeneratorException;
import io.wcm.devops.conga.generator.util.FileUtil;
import io.wcm.devops.conga.resource.ResourceCollection;
import io.wcm.devops.conga.resource.ResourceLoader;
import io.wcm.devops.conga.tooling.maven.plugin.util.ClassLoaderUtil;

/**
 * Generates configuration using CONGA generator.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true, threadSafe = true,
requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractCongaMojo {

  /**
   * Selected environments to generate.
   */
  @Parameter
  private String[] environments;

  /**
   * Delete folders of environments before generating the new files.
   */
  @Parameter(defaultValue = "false")
  private boolean deleteBeforeGenerate;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Component
  private ArtifactResolver resolver;
  @Component
  private ArtifactHandlerManager artifactHandlerManager;
  @Parameter(property = "session", readonly = true, required = true)
  private MavenSession mavenSession;

  private ResourceLoader resourceLoader;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    resourceLoader = new ResourceLoader(ClassLoaderUtil.buildDependencyClassLoader(project));

    List<ResourceCollection> roleDirs = ImmutableList.of(getRoleDir(),
        getResourceLoader().getResourceCollection(ResourceLoader.CLASSPATH_PREFIX + CLASSPATH_ROLES_DIR));
    List<ResourceCollection> templateDirs = ImmutableList.of(getTemplateDir(),
        getResourceLoader().getResourceCollection(ResourceLoader.CLASSPATH_PREFIX + CLASSPATH_TEMPLATES_DIR));
    List<ResourceCollection> environmentDirs = ImmutableList.of(getEnvironmentDir(),
        getResourceLoader().getResourceCollection(ResourceLoader.CLASSPATH_PREFIX + CLASSPATH_ENVIRONMENTS_DIR));

    Generator generator = new Generator(roleDirs, templateDirs, environmentDirs, getTargetDir());
    generator.setLogger(new MavenSlf4jLogFacade(getLog()));
    generator.setDeleteBeforeGenerate(deleteBeforeGenerate);
    generator.setVersion(project.getVersion());
    generator.setDependencyVersions(buildDependencyVersionList());
    generator.generate(environments);
  }

  /**
   * Build list of referenced dependencies to be included in file header of generated files.
   * @return Version list
   */
  @SuppressWarnings("deprecation")
  private List<String> buildDependencyVersionList() {
    getLog().info("Scanning dependencies for CONGA definitions...");
    return project.getCompileDependencies().stream()
        // include only dependencies with a CONGA-INF/ directory
        .filter(this::hasCongaDefinitions)
        // transform to string
        .map(dependency -> dependency.getGroupId() + "/" + dependency.getArtifactId() + "/" + dependency.getVersion()
            + (dependency.getClassifier() != null ? "/" + dependency.getClassifier() : ""))
            .collect(Collectors.toList());
  }

  /**
   * Checks if the JAR file of the given dependency has a CONGA-INF/ directory.
   * @param dependency Dependency
   * @return true if configuration definitions found
   */
  private boolean hasCongaDefinitions(Dependency dependency) {
    if (!StringUtils.equals(dependency.getType(), "jar")) {
      return false;
    }
    String fileInfo = dependency.toString();
    try {
      Artifact artifact = getArtifact(dependency);
      fileInfo = FileUtil.getCanonicalPath(artifact.getFile());
      try (ZipFile zipFile = new ZipFile(artifact.getFile())) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (StringUtils.startsWith(entry.getName(), BuildConstants.CLASSPATH_PREFIX)) {
            return true;
          }
        }
      }
    }
    catch (IOException ex) {
      throw new GeneratorException("Unable to read from JAR file: " + fileInfo, ex);
    }
    return false;
  }

  /**
   * Get a resolved Artifact from the coordinates provided
   * @return the artifact, which has been resolved.
   */
  @SuppressWarnings("deprecation")
  private Artifact getArtifact(Dependency dependency) throws IOException {
    Artifact artifact = new DefaultArtifact(dependency.getGroupId(),
        dependency.getArtifactId(),
        VersionRange.createFromVersion(dependency.getVersion()),
        dependency.getScope(),
        dependency.getType(),
        dependency.getClassifier(),
        artifactHandlerManager.getArtifactHandler(dependency.getType()));
    try {
      this.resolver.resolve(artifact, this.project.getRemoteArtifactRepositories(), this.mavenSession.getLocalRepository());
    }
    catch (final ArtifactResolutionException ex) {
      throw new IOException("Unable to get artifact for " + dependency, ex);
    }
    catch (ArtifactNotFoundException ex) {
      throw new IOException("Unable to get artifact for " + dependency, ex);
    }
    return artifact;
  }

  @Override
  protected ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

}
