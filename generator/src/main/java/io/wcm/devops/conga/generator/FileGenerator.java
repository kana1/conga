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
package io.wcm.devops.conga.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableList;

import io.wcm.devops.conga.generator.plugins.fileheader.NoneFileHeader;
import io.wcm.devops.conga.generator.plugins.validator.NoneValidator;
import io.wcm.devops.conga.generator.spi.FileHeaderPlugin;
import io.wcm.devops.conga.generator.spi.PostProcessorPlugin;
import io.wcm.devops.conga.generator.spi.ValidatorPlugin;
import io.wcm.devops.conga.generator.spi.context.FileContext;
import io.wcm.devops.conga.generator.spi.context.FileHeaderContext;
import io.wcm.devops.conga.generator.spi.context.PostProcessorContext;
import io.wcm.devops.conga.generator.spi.context.ValidatorContext;
import io.wcm.devops.conga.generator.spi.export.context.GeneratedFileContext;
import io.wcm.devops.conga.generator.util.FileUtil;
import io.wcm.devops.conga.generator.util.LineEndingConverter;
import io.wcm.devops.conga.generator.util.PluginManager;
import io.wcm.devops.conga.generator.util.VariableMapResolver;
import io.wcm.devops.conga.model.role.RoleFile;
import io.wcm.devops.conga.model.util.MapMerger;

/**
 * Generates file for one environment.
 */
class FileGenerator {

  private final String environmentName;
  private final String roleName;
  private final String roleVariantName;
  private final String templateName;
  private final File nodeDir;
  private final File file;
  private final String url;
  private final RoleFile roleFile;
  private final Map<String, Object> config;
  private final Template template;
  private final PluginManager pluginManager;
  private final UrlFileManager urlFileManager;
  private final Logger log;
  private final FileContext fileContext;
  private final FileHeaderContext fileHeaderContext;
  private final ValidatorContext validatorContext;
  private final PostProcessorContext postProcessorContext;

  //CHECKSTYLE:OFF
  FileGenerator(String environmentName, String roleName, String roleVariantName, String templateName,
      File nodeDir, File file, String url, RoleFile roleFile, Map<String, Object> config,
      Template template, PluginManager pluginManager, UrlFileManager urlFileManager,
      String version, List<String> dependencyVersions, Logger log) {
    //CHECKSTYLE:ON
    this.environmentName = environmentName;
    this.roleName = roleName;
    this.roleVariantName = roleVariantName;
    this.templateName = templateName;
    this.nodeDir = nodeDir;
    this.file = file;
    this.url = url;
    this.roleFile = roleFile;
    this.template = template;
    this.pluginManager = pluginManager;
    this.urlFileManager = urlFileManager;
    this.log = log;
    this.fileContext = new FileContext()
        .file(file)
        .charset(roleFile.getCharset())
        .modelOptions(roleFile.getModelOptions());

    this.fileHeaderContext = new FileHeaderContext()
        .commentLines(buildFileHeaderCommentLines(version, dependencyVersions));

    Logger pluginLogger = new MessagePrefixLoggerFacade(log, "    ");

    this.validatorContext = new ValidatorContext()
        .options(VariableMapResolver.resolve(MapMerger.merge(roleFile.getValidatorOptions(), config)))
        .logger(pluginLogger);

    this.postProcessorContext = new PostProcessorContext()
        .options(VariableMapResolver.resolve(MapMerger.merge(roleFile.getPostProcessorOptions(), config)))
        .pluginManager(pluginManager)
        .logger(pluginLogger);

    this.config = VariableMapResolver.deescape(config);
  }

  /**
   * Generate comment lines for file header added to all files for which a {@link FileHeaderPlugin} is registered.
   * @param dependencyVersions List of artifact versions to include
   * @return Formatted comment lines
   */
  private List<String> buildFileHeaderCommentLines(String version, List<String> dependencyVersions) {
    List<String> lines = new ArrayList<>();

    lines.add("This file is AUTO-GENERATED by CONGA. Please do no change it manually.");
    lines.add("");
    if (version != null) {
      lines.add("Version " + version);
    }

    // add information how this file was generated
    lines.add("Environment: " + environmentName);
    lines.add("Role: " + roleName);
    if (StringUtils.isNotBlank(roleVariantName)) {
      lines.add("Variant: " + roleVariantName);
    }
    lines.add("Template: " + templateName);

    if (dependencyVersions != null && !dependencyVersions.isEmpty()) {
      lines.add("");
      lines.add("Dependencies:");
      lines.addAll(dependencyVersions);
    }

    return formatFileHeaderCommentLines(lines);
  }

  /**
   * Format comment lines.
   * @param lines Unformatted comment lines
   * @return Formatted comment lines
   */
  private List<String> formatFileHeaderCommentLines(List<String> lines) {
    List<String> formattedLines = new ArrayList<>();

    // create separator with same length as longest comment entry
    int maxLength = lines.stream()
        .map(entry -> entry.length())
        .max(Integer::compare).get();
    String separator = StringUtils.repeat("*", maxLength + 4);

    formattedLines.add(separator);
    formattedLines.add("");
    lines.forEach(line -> formattedLines.add("  " + line));
    formattedLines.add("");
    formattedLines.add(separator);

    return formattedLines;
  }

  /**
   * Generate file(s).
   * @return List of files that where generated directly or indirectly (by post processors).
   * @throws IOException
   */
  public Collection<GeneratedFileContext> generate() throws IOException {
    File dir = file.getParentFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }

    Collection<GeneratedFileContext> postProcessedFiles;
    if (template != null) {
      log.info("Generate file {}", getFilenameForLog(fileContext));

      // generate with template
      generateWithTemplate();

      // add file header, validate and post-process generated file
      applyFileHeader(fileContext, roleFile.getFileHeader());
      applyValidation(fileContext, roleFile.getValidators());
      postProcessedFiles = applyPostProcessor(fileContext);

    }
    else if (StringUtils.isNotBlank(url)) {
      log.info("Copy file {} from {}", getFilenameForLog(fileContext), url);

      // generate by downloading/copying from URL, and post-process downloaded file
      generateFromUrlFile();
      postProcessedFiles = applyPostProcessor(fileContext);
    }
    else {
      throw new IOException("No template and nor URL defined for file: " + FileUtil.getFileInfo(roleName, roleFile));
    }

    return postProcessedFiles;
  }

  /**
   * Generate file with handlebars template.
   * Use unix file endings by default.
   */
  private void generateWithTemplate() throws IOException {
    try (FileOutputStream fos = new FileOutputStream(file);
        Writer fileWriter = new OutputStreamWriter(fos, roleFile.getCharset())) {
      StringWriter stringWriter = new StringWriter();
      template.apply(config, stringWriter);
      fileWriter.write(normalizeLineEndings(stringWriter.toString()));
      fileWriter.flush();
    }
  }

  /**
   * Generate file by downloading/copying from URL
   */
  private void generateFromUrlFile() throws IOException {
    try (FileOutputStream fos = new FileOutputStream(file);
        InputStream is = urlFileManager.getFile(url)) {
      IOUtils.copy(is, fos);
      fos.flush();
    }
  }

  private String normalizeLineEndings(String value) {
    // convert/normalize all line endings to unix style
    String normalizedLineEndings = LineEndingConverter.normalizeToUnix(value);
    // and then to the line-ending style as requested in the tempalte definition
    return LineEndingConverter.convertTo(normalizedLineEndings, roleFile.getLineEndings());
  }

  private void applyFileHeader(FileContext fileItem, String pluginName) {
    Stream<FileHeaderPlugin> fileHeaders;
    if (StringUtils.isEmpty(pluginName)) {
      // auto-detect matching file header plugin if none are defined
      fileHeaders = pluginManager.getAll(FileHeaderPlugin.class).stream()
          .filter(plugin -> plugin.accepts(fileItem, fileHeaderContext));
    }
    else {
      // otherwise apply selected file header plugin
      fileHeaders = Stream.of(pluginName)
          .map(name -> pluginManager.get(name, FileHeaderPlugin.class));
    }
    fileHeaders
    .filter(plugin -> !StringUtils.equals(plugin.getName(), NoneFileHeader.NAME))
    .findFirst().ifPresent(plugin -> applyFileHeader(fileItem, plugin));
  }

  private void applyFileHeader(FileContext fileItem, FileHeaderPlugin plugin) {
    log.debug("  Add {} file header to file {}", plugin.getName(), getFilenameForLog(fileItem));
    plugin.apply(fileItem, fileHeaderContext);
  }

  private void applyValidation(FileContext fileItem, List<String> pluginNames) {
    Stream<ValidatorPlugin> validators;
    if (pluginNames.isEmpty()) {
      // auto-detect matching validators if none are defined
      validators = pluginManager.getAll(ValidatorPlugin.class).stream()
          .filter(plugin -> !StringUtils.equals(plugin.getName(), NoneFileHeader.NAME))
          .filter(plugin -> plugin.accepts(fileItem, validatorContext));
    }
    else {
      // otherwise apply selected validators
      validators = pluginNames.stream()
          .map(name -> pluginManager.get(name, ValidatorPlugin.class));
    }
    validators
    .filter(plugin -> !StringUtils.equals(plugin.getName(), NoneValidator.NAME))
    .forEach(plugin -> applyValidation(fileItem, plugin));
  }

  private void applyValidation(FileContext fileItem, ValidatorPlugin plugin) {
    log.info("  Validate {} for file {}", plugin.getName(), getFilenameForLog(fileItem));
    plugin.apply(fileItem, validatorContext);
  }

  private Collection<GeneratedFileContext> applyPostProcessor(FileContext fileItem) {
    // collect distinct list of files returned by each post processor
    // if a file is returned by multiple post processors combine to single entry with multiple plugin names
    Map<String, GeneratedFileContext> consolidatedFiles = new LinkedHashMap<>();

    // start with original file
    consolidatedFiles.put(fileContext.getCanonicalPath(), new GeneratedFileContext().fileContext(fileContext));

    // process all processors. if multiple processors each processor processed the files of the previous one.
    roleFile.getPostProcessors().stream()
        .map(name -> pluginManager.get(name, PostProcessorPlugin.class))
        .forEach(plugin -> applyPostProcessor(consolidatedFiles, plugin));

    return consolidatedFiles.values();
  }

  private void applyPostProcessor(Map<String, GeneratedFileContext> consolidatedFiles, PostProcessorPlugin plugin) {

    // process all files from given map
    ImmutableList.copyOf(consolidatedFiles.values()).forEach(fileItem -> {
      List<FileContext> processedFiles = applyPostProcessor(fileItem.getFileContext(), plugin);
      fileItem.postProcessor(plugin.getName());
      processedFiles.forEach(item -> {
        GeneratedFileContext generatedFileContext = consolidatedFiles.get(item.getCanonicalPath());
        if (generatedFileContext == null) {
          generatedFileContext = new GeneratedFileContext().fileContext(item);
          consolidatedFiles.put(item.getCanonicalPath(), generatedFileContext);
        }
        generatedFileContext.postProcessor(plugin.getName());
      });
    });

    // remove items that do no longer exist
    ImmutableList.copyOf(consolidatedFiles.values()).forEach(fileItem -> {
      if (!fileItem.getFileContext().getFile().exists()) {
        consolidatedFiles.remove(fileItem.getFileContext().getCanonicalPath());
      }
    });

  }

  private List<FileContext> applyPostProcessor(FileContext fileItem, PostProcessorPlugin plugin) {
    log.info("  Post-process {} for file {}", plugin.getName(), getFilenameForLog(fileItem));

    List<FileContext> processedFiles = plugin.apply(fileItem, postProcessorContext);

    // validate processed files
    if (processedFiles != null) {
      processedFiles.forEach(processedFile -> applyFileHeader(processedFile, (String)null));
      processedFiles.forEach(processedFile -> applyValidation(processedFile, ImmutableList.of()));
    }

    return processedFiles;
  }

  private String getFilenameForLog(FileContext fileItem) {
    return StringUtils.substring(fileItem.getCanonicalPath(), FileUtil.getCanonicalPath(nodeDir).length() + 1);
  }

}
