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
package io.wcm.devops.conga.generator.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.wcm.devops.conga.generator.GeneratorException;
import io.wcm.devops.conga.model.role.Role;
import io.wcm.devops.conga.model.role.RoleFile;
import io.wcm.devops.conga.model.role.RoleInherit;
import io.wcm.devops.conga.model.role.RoleVariant;

public class RoleUtilTest {

  private Map<String, Role> roleMap;

  @Before
  public void setUp() throws Exception {
    roleMap = new HashMap<>();

    /*
     * Inheritance example:
     *
     * role1 - file1.1, file1.2
     *   |
     *   +- role2 (variant1, variant2) - file2.1, file2.2
     *        |
     *        +- role3 (variant1, variant2, variant3) - file3.1, file3.2, file1.1
     *        |
     *        +- role4 (no variants - illegal) - file 4.1
     *
     * role5 (inherits from itself - illegal) - file 5.1
     *
     */

    Role role1 = new Role();
    role1.setTemplateDir("role1Dir");
    role1.setConfig(ImmutableMap.<String, Object>of(
        "param1", "value1.1",
        "param2", 123,
        "param3", true));
    role1.setFiles(ImmutableList.of(
        buildFile("role1", "file1.1"),
        buildFile("role1", "file1.2"),
        buildUrlFile("role1", "http://file1.3")));
    roleMap.put("role1", role1);

    Role role2 = new Role();
    role2.setVariants(ImmutableList.of(
        buildVariant("variant1"),
        buildVariant("variant2", ImmutableMap.<String, Object>of(
            "varparam1", "varvalue2.1",
            "varparam2", 888))));
    role2.setTemplateDir("role2Dir");
    role2.setConfig(ImmutableMap.<String, Object>of(
        "param1", "value2.1",
        "param4", "value2.4"));
    role2.setFiles(ImmutableList.of(
        buildFile("role2", "file2.1", "variant1"),
        buildFile("role2", "file2.2", "variant2"),
        buildUrlFile("role2", "http://file2.3", "variant1")));
    role2.setInherits(ImmutableList.of(
        buildInherit("role1")));
    roleMap.put("role2", role2);

    Role role3 = new Role();
    role3.setVariants(ImmutableList.of(
        buildVariant("variant1"),
        buildVariant("variant2", ImmutableMap.<String, Object>of(
            "varparam1", "varvalue3.1")),
        buildVariant("variant3", ImmutableMap.<String, Object>of(
            "varparam1", "varvalue3.2"))));
    role3.setTemplateDir("role3Dir");
    role3.setConfig(ImmutableMap.<String, Object>of(
        "param1", "value3.1",
        "param5", "value3.5"));
    role3.setFiles(ImmutableList.of(
        buildFile("role3", "file3.1", "variant1"),
        buildFile("role3", "file3.2", "variant2"),
        buildUrlFile("role3", "http://file3.3", "variant2"),
        buildFile("role3", "file1.1"),
        buildUrlFile("role3", "http://file1.3")));
    role3.setInherits(ImmutableList.of(
        buildInherit("role2")));
    roleMap.put("role3", role3);

    Role role4 = new Role();
    role4.setTemplateDir("role4Dir");
    role4.setConfig(ImmutableMap.<String, Object>of(
        "param1", "value4.1"));
    role4.setFiles(ImmutableList.of(
        buildFile("role4", "file4.1")));
    role4.setInherits(ImmutableList.of(
        buildInherit("role2")));
    roleMap.put("role4", role4);

    Role role5 = new Role();
    role5.setTemplateDir("role5Dir");
    role5.setConfig(ImmutableMap.<String, Object>of(
        "param1", "value5.1"));
    role5.setFiles(ImmutableList.of(
        buildFile("role5", "file5.1")));
    role5.setInherits(ImmutableList.of(
        buildInherit("role5")));
    roleMap.put("role5", role5);
  }

  @Test(expected = GeneratorException.class)
  public void testUnknownRole() {
    RoleUtil.resolveRole("roleX", "context", roleMap);
  }

  @Test
  public void testRole1() {
    Map<String, Role> resolvedRoles = RoleUtil.resolveRole("role1", "context", roleMap);
    assertEquals(1, resolvedRoles.size());
    Iterator<Role> resolvedRoleIterator = resolvedRoles.values().iterator();
    Role role = resolvedRoleIterator.next();

    assertEquals("role1Dir", role.getTemplateDir());
    assertEquals("value1.1", role.getConfig().get("param1"));
    assertEquals(123, role.getConfig().get("param2"));
    assertEquals(true, role.getConfig().get("param3"));

    assertFile(role, "role1", "file1.1");
    assertFile(role, "role1", "file1.2");
    assertUrlFile(role, "role1", "http://file1.3");
  }

  @Test
  public void testRole2() {
    Map<String, Role> resolvedRoles = RoleUtil.resolveRole("role2", "context", roleMap);
    assertEquals(2, resolvedRoles.size());
    Iterator<Role> resolvedRoleIterator = resolvedRoles.values().iterator();
    Role role1 = resolvedRoleIterator.next();
    Role role2 = resolvedRoleIterator.next();

    // common for all roles
    for (Role role : resolvedRoles.values()) {
      assertEquals("value2.1", role.getConfig().get("param1"));
      assertEquals(123, role.getConfig().get("param2"));
      assertEquals(true, role.getConfig().get("param3"));
    }

    assertEquals("role1Dir", role1.getTemplateDir());
    assertEquals("role2Dir", role2.getTemplateDir());

    assertFile(role1, "role1", "file1.1");
    assertFile(role1, "role1", "file1.2");
    assertUrlFile(role1, "role1", "http://file1.3");
    assertFile(role2, "role2", "file2.1", "variant1");
    assertFile(role2, "role2", "file2.2", "variant2");
    assertUrlFile(role2, "role2", "http://file2.3", "variant1");

    assertVariant(role2, "variant1");
    assertVariant(role2, "variant2", ImmutableMap.<String, Object>of(
        "varparam1", "varvalue2.1",
        "varparam2", 888));
  }

  @Test
  public void testRole3() {
    Map<String, Role> resolvedRoles = RoleUtil.resolveRole("role3", "context", roleMap);
    assertEquals(3, resolvedRoles.size());
    Iterator<Role> resolvedRoleIterator = resolvedRoles.values().iterator();
    Role role1 = resolvedRoleIterator.next();
    Role role2 = resolvedRoleIterator.next();
    Role role3 = resolvedRoleIterator.next();

    // common for all roles
    for (Role role : resolvedRoles.values()) {
      assertEquals("value3.1", role.getConfig().get("param1"));
      assertEquals(123, role.getConfig().get("param2"));
      assertEquals(true, role.getConfig().get("param3"));
    }

    assertEquals("role1Dir", role1.getTemplateDir());
    assertEquals("role2Dir", role2.getTemplateDir());
    assertEquals("role3Dir", role3.getTemplateDir());

    assertNotFile(role1, "role1", "file1.1");
    assertFile(role1, "role1", "file1.2");
    assertNotUrlFile(role1, "role1", "http://file1.3");
    assertFile(role2, "role2", "file2.1", "variant1");
    assertFile(role2, "role2", "file2.2", "variant2");
    assertUrlFile(role2, "role2", "http://file2.3", "variant1");
    assertFile(role3, "role3", "file3.1", "variant1");
    assertFile(role3, "role3", "file3.2", "variant2");
    assertUrlFile(role3, "role3", "http://file3.3", "variant2");
    assertFile(role3, "role3", "file1.1");
    assertUrlFile(role3, "role3", "http://file1.3");

    assertVariant(role2, "variant1");
    assertVariant(role2, "variant2", ImmutableMap.<String, Object>of(
        "varparam1", "varvalue3.1",
        "varparam2", 888));

    assertVariant(role3, "variant1");
    assertVariant(role3, "variant2", ImmutableMap.<String, Object>of(
        "varparam1", "varvalue3.1",
        "varparam2", 888));
    assertVariant(role3, "variant3", ImmutableMap.<String, Object>of(
        "varparam1", "varvalue3.2"));
  }

  @Test(expected = GeneratorException.class)
  public void testRole4_InheritWithMissingVariants() {
    RoleUtil.resolveRole("role4", "context", roleMap);
  }

  @Test(expected = GeneratorException.class)
  public void testRole5_CyclicInheritance() {
    RoleUtil.resolveRole("role5", "context", roleMap);
  }


  // ----- test helper methods -----

  private RoleVariant buildVariant(String variantName) {
    return buildVariant(variantName, null);
  }

  private RoleVariant buildVariant(String variantName, Map<String, Object> config) {
    RoleVariant variant = new RoleVariant();
    variant.setVariant(variantName);
    variant.setConfig(config);
    return variant;
  }

  private RoleFile buildFile(String role, String name, String... variants) {
    RoleFile file = new RoleFile();
    file.setFile(name);
    file.setTemplate(role + "-" + name + ".hbs");
    if (variants.length > 0) {
      file.setVariants(ImmutableList.copyOf(variants));
    }
    return file;
  }

  private RoleFile buildUrlFile(String role, String url, String... variants) {
    RoleFile file = new RoleFile();
    file.setUrl(url);
    if (variants.length > 0) {
      file.setVariants(ImmutableList.copyOf(variants));
    }
    return file;
  }

  private RoleInherit buildInherit(String roleName, String... variants) {
    RoleInherit inherit = new RoleInherit();
    inherit.setRole(roleName);
    return inherit;
  }

  private void assertFile(Role role, String roleName, String file, String... variants) {
    String template = roleName + "-" + file + ".hbs";
    for (RoleFile fileItem : role.getFiles()) {
      if (StringUtils.equals(file, fileItem.getFile())
          && StringUtils.equals(template, fileItem.getTemplate())
          && ImmutableList.copyOf(variants).equals(fileItem.getVariants())) {
        // item found
        return;
      }
    }
    fail("File '" + file + "' with template '" + template + "' not found.");
  }

  private void assertNotFile(Role role, String roleName, String file, String... variants) {
    String template = roleName + "-" + file + ".hbs";
    try {
      assertFile(role, roleName, file, variants);
    }
    catch (AssertionError ex) {
      // fails - matches expectation
      return;
    }
    fail("File '" + file + "' with template '" + template + "' found, but expected to not find it.");
  }

  private void assertUrlFile(Role role, String roleName, String url, String... variants) {
    for (RoleFile fileItem : role.getFiles()) {
      if (StringUtils.equals(url, fileItem.getUrl())
          && ImmutableList.copyOf(variants).equals(fileItem.getVariants())) {
        // item found
        return;
      }
    }
    fail("File with URL '" + url + "' not found.");
  }

  private void assertNotUrlFile(Role role, String roleName, String url, String... variants) {
    try {
      assertUrlFile(role, roleName, url, variants);
    }
    catch (AssertionError ex) {
      // fails - matches expectation
      return;
    }
    fail("File with URL '" + url + "' found, but expected to not find it.");
  }

  private void assertVariant(Role role, String variant) {
    assertVariant(role, variant, ImmutableMap.of());
  }

  private void assertVariant(Role role, String variant, Map<String, Object> config) {
    for (RoleVariant variantItem : role.getVariants()) {
      if (StringUtils.equals(variant, variantItem.getVariant())
          && config.equals(variantItem.getConfig())) {
        // item found
        return;
      }
    }
    fail("Variant '" + variant + "' with config '" + config + "' not found.");
  }

}
