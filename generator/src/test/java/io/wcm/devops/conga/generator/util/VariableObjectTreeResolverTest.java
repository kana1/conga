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
package io.wcm.devops.conga.generator.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.wcm.devops.conga.generator.util.testmodel.ConfScope1;
import io.wcm.devops.conga.generator.util.testmodel.ConfScope2;
import io.wcm.devops.conga.generator.util.testmodel.ConfScope3;
import io.wcm.devops.conga.generator.util.testmodel.Root;
import io.wcm.devops.conga.generator.util.testmodel.SampleNode;
import io.wcm.devops.conga.generator.util.testmodel.SimpleConf;


public class VariableObjectTreeResolverTest {

  @Test
  public void testResolve() {
    /*
     object tree:

     Root
       +- scope1
          +- [
            scope21
              +- scope31
              +- scope32
            scope22
          ]
          +- simple1
          +- sample
             +- simple2
     */

    Root root = new Root();
    ConfScope1 scope1 = new ConfScope1();
    ConfScope2 scope21 = new ConfScope2();
    ConfScope3 scope31 = new ConfScope3();
    ConfScope3 scope32 = new ConfScope3();
    scope21.setScope31(scope31);
    scope21.setScope32(scope32);
    ConfScope2 scope22 = new ConfScope2();
    scope1.setScope2(ImmutableList.of(scope21, scope22));
    root.setScope1(scope1);
    SimpleConf simple1 = new SimpleConf();
    scope1.setMap(ImmutableMap.of("simple", simple1));
    SampleNode sample = new SampleNode();
    SimpleConf simple2 = new SimpleConf();
    sample.setSimple(simple2);
    scope1.setSample(sample);

    scope1.setConfig(ImmutableMap.of("var1", "v1", "conf", "${var1}"));

    scope21.setConfig(ImmutableMap.of("var21", "v21", "conf21", "${var21}"));

    scope22.setConfig(ImmutableMap.of("var22", "v22", "conf22", "${var22}"));

    scope31.setConfig(ImmutableMap.of("var31", "v31", "conf31", "${var31}"));

    scope32.setConfig(ImmutableMap.of("var32", "v32", "conf32", "${var32}"));

    simple1.setConfig(ImmutableMap.of("varS1", "vS1", "confS1", "${varS1}"));

    simple2.setConfig(ImmutableMap.of("varS2", "vS2", "confS2", "${varS2}"));


    VariableObjectTreeResolver.resolve(root);


    assertEquals(ImmutableMap.of("var1", "v1", "conf", "v1"), scope1.getConfig());

    assertEquals(ImmutableMap.of("var21", "v21", "conf21", "v21"), scope21.getConfig());

    assertEquals(ImmutableMap.of("var22", "v22", "conf22", "v22"), scope22.getConfig());

    assertEquals(ImmutableMap.of("var31", "v31", "conf31", "v31"), scope31.getConfig());

    assertEquals(ImmutableMap.of("var32", "v32", "conf32", "v32"), scope32.getConfig());

    assertEquals(ImmutableMap.of("varS1", "vS1", "confS1", "vS1"), simple1.getConfig());

    assertEquals(ImmutableMap.of("varS2", "vS2", "confS2", "vS2"), simple2.getConfig());
  }

}
