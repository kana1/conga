/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.wcm.devops.conga.generator.spi.FileHeaderPlugin;
import io.wcm.devops.conga.generator.spi.ImplicitApplyOptions;
import io.wcm.devops.conga.generator.spi.context.FileContext;
import io.wcm.devops.conga.generator.spi.context.FileHeaderContext;
import io.wcm.devops.conga.generator.spi.context.PluginContextOptions;
import io.wcm.devops.conga.generator.spi.context.UrlFilePluginContext;
import io.wcm.devops.conga.generator.spi.context.ValueProviderGlobalContext;
import io.wcm.devops.conga.generator.spi.export.context.GeneratedFileContext;
import io.wcm.devops.conga.generator.util.PluginManager;
import io.wcm.devops.conga.generator.util.VariableMapResolver;
import io.wcm.devops.conga.model.role.RoleFile;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FileGeneratorFileHeaderTest {

  private File destDir;
  private File file;
  private RoleFile roleFile;
  private UrlFileManager urlFileManager;
  private FileGenerator underTest;
  private Map<String, FileHeaderPlugin> fileHeaderPlugins = new HashMap<>();

  @Mock
  private Template template;
  @Mock
  private PluginManager pluginManager;

  @Before
  public void setUp() {
    destDir = new File("target/generation-test/" + getClass().getSimpleName());
    file = new File(destDir, "test.txt");
    roleFile = new RoleFile();
    UrlFilePluginContext urlFilePluginContext = new UrlFilePluginContext();
    urlFileManager = new UrlFileManager(pluginManager, urlFilePluginContext);

    when(pluginManager.getAll(FileHeaderPlugin.class)).thenAnswer(new Answer<List<FileHeaderPlugin>>() {
      @Override
      public List<FileHeaderPlugin> answer(InvocationOnMock invocation) throws Throwable {
        return ImmutableList.copyOf(fileHeaderPlugins.values());
      }
    });
    when(pluginManager.get(anyString(), eq(FileHeaderPlugin.class))).thenAnswer(new Answer<FileHeaderPlugin>() {
      @Override
      public FileHeaderPlugin answer(InvocationOnMock invocation) throws Throwable {
        return fileHeaderPlugins.get(invocation.getArgument(0));
      }
    });

    GeneratorOptions options = new GeneratorOptions()
        .pluginManager(pluginManager)
        .version("1.0");
    PluginContextOptions pluginContextOptions = new PluginContextOptions()
        .pluginManager(pluginManager)
        .urlFileManager(urlFileManager)
        .logger(options.getLogger());
    VariableMapResolver variableMapResolver = new VariableMapResolver(
        new ValueProviderGlobalContext().pluginContextOptions(pluginContextOptions));
    underTest = new FileGenerator(options, "env1",
        "role1", ImmutableList.of("variant1"), "template1",
        destDir, file, null, roleFile, ImmutableMap.<String, Object>of(), template,
        variableMapResolver, urlFileManager, pluginContextOptions, ImmutableList.of(
            "version1/1.0.0",
            "version2/2.0.0-SNAPSHOT",
            "version3/1.2.0-20180116.233128-5",
            "version4/2.1.2-20180125.094723-16/suffix"
            ));
  }

  @Test
  public void testWithoutFileHeader() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.NEVER);

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    verify(one, never()).apply(any(FileContext.class), any(FileHeaderContext.class));
  }

  @Test
  public void testExplicitFileHeader() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.NEVER);
    roleFile.setFileHeader("one");

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    verify(one, times(1)).apply(any(FileContext.class), any(FileHeaderContext.class));
  }

  @Test
  public void testImplicitFileHeader() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.WHEN_UNCONFIGURED);

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    verify(one, times(1)).apply(any(FileContext.class), any(FileHeaderContext.class));
  }

  @Test
  public void testAlwaysFileHeader() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.ALWAYS);

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    verify(one, times(1)).apply(any(FileContext.class), any(FileHeaderContext.class));
  }

  @Test
  public void testImplicitAndAlwaysFileHeader() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.WHEN_UNCONFIGURED);
    FileHeaderPlugin two = mockFileHeader("two", "txt", ImplicitApplyOptions.ALWAYS);

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    verify(one, times(1)).apply(any(FileContext.class), any(FileHeaderContext.class));
    verify(two, times(1)).apply(any(FileContext.class), any(FileHeaderContext.class));
  }

  @Test
  public void testVersions() throws Exception {
    FileHeaderPlugin one = mockFileHeader("one", "txt", ImplicitApplyOptions.NEVER);
    roleFile.setFileHeader("one");

    List<GeneratedFileContext> result = ImmutableList.copyOf(underTest.generate());

    assertEquals(1, result.size());
    assertItem(result.get(0), "test.txt");

    ArgumentCaptor<FileHeaderContext> contextCaptor = ArgumentCaptor.forClass(FileHeaderContext.class);
    verify(one, times(1)).apply(any(FileContext.class), contextCaptor.capture());
    FileHeaderContext context = contextCaptor.getValue();

    String fileHeader = StringUtils.join(context.getCommentLines(), "\n");
    assertTrue(StringUtils.contains(fileHeader, "version1/1.0.0\n"));
    assertTrue(StringUtils.contains(fileHeader, "version2/2.0.0-SNAPSHOT\n"));
    assertTrue(StringUtils.contains(fileHeader, "version3/1.2.0-SNAPSHOT\n"));
    assertTrue(StringUtils.contains(fileHeader, "version4/2.1.2-SNAPSHOT/suffix\n"));
  }

  private void assertItem(GeneratedFileContext item, String expectedFileName) {
    assertEquals(expectedFileName, item.getFileContext().getFile().getName());
  }

  private FileHeaderPlugin mockFileHeader(String pluginName, String extension, ImplicitApplyOptions implicitApply) {
    FileHeaderPlugin plugin = mock(FileHeaderPlugin.class);
    when(plugin.getName()).thenReturn(pluginName);
    when(plugin.accepts(any(FileContext.class), any(FileHeaderContext.class))).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        FileContext input = invocation.getArgument(0);
        return StringUtils.endsWith(input.getFile().getName(), "." + extension);
      }
    });
    when(plugin.implicitApply(any(FileContext.class), any(FileHeaderContext.class))).thenReturn(implicitApply);
    fileHeaderPlugins.put(pluginName, plugin);
    return plugin;
  }

}
