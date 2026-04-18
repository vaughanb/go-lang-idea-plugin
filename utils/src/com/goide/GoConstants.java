/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import java.util.Set;

public class GoConstants {
  public static final String MODULE_TYPE_ID = "GO_MODULE";
  public static final String SDK_TYPE_ID = "Go SDK";
  public static final String PATH = "PATH";
  public static final String GO_PATH = "GOPATH";
  public static final String GO_ROOT = "GOROOT";
  public static final String GO_VENDORING_EXPERIMENT = "GO15VENDOREXPERIMENT";
  public static final String GO_LIBRARIES_SERVICE_NAME = "GoLibraries";
  public static final String GO_LIBRARIES_CONFIG_FILE = "goLibraries.xml";
  public static final String GO_MODULE_SESTTINGS_SERVICE_NAME = "Go";

  private static final String IDENTIFIER_REGEX = "[\\p{javaLetter}_][\\p{javaLetterOrDigit}_]*";
  public static final String TEST_NAME_REGEX = IDENTIFIER_REGEX + "(/\\S*)?";
  public static final String TESTDATA_NAME = "testdata";
  public static final String TEST_SUFFIX = "_test";
  public static final String TEST_SUFFIX_WITH_EXTENSION = "_test.go";
  public static final String TEST_PREFIX = "Test";
  public static final String BENCHMARK_PREFIX = "Benchmark";
  public static final String EXAMPLE_PREFIX = "Example";
  public static final String FUZZ_PREFIX = "Fuzz";
  public static final String TEST_MAIN = "TestMain";
  public static final String MAIN = "main";
  public static final String INIT = "init";
  public static final String IOTA = "iota";
  public static final String DOCUMENTATION = "documentation";
  public static final String C_PATH = "C";
  public static final String TESTING_PATH = "testing";
  public static final String VENDOR = "vendor";
  public static final String INTERNAL = "internal";
  public static final String INTERFACE_TYPE = "interface{}";
  public static final String ANY_TYPE = "any";

  public static final String GO_NOTIFICATION_GROUP_ID = "Go plugin notifications";
  public static final String GO_EXECUTION_NOTIFICATION_GROUP_ID = "Go Execution";

  @NonNls public static final String LIB_EXEC_DIRECTORY = "libexec";
  @NonNls public static final String GO_VERSION_FILE_PATH = "runtime/zversion.go";
  @NonNls public static final String GO_VERSION_NEW_FILE_PATH = "runtime/internal/sys/zversion.go";
  @NonNls public static final String GO_VERSION_NEWEST_FILE_PATH = "internal/runtime/sys/zversion.go";
  public static final String BUILTIN_FILE_NAME = "builtin.go";
  public static final String BUILTIN_PACKAGE_NAME = "builtin";
  public static final String BUILTIN_FILE_PATH = BUILTIN_PACKAGE_NAME + "/" + BUILTIN_FILE_NAME;

  @NonNls public static final String APP_ENGINE_MARKER_FILE = "appcfg.py";
  @NonNls public static final String APP_ENGINE_GO_ROOT_DIRECTORY_PATH = "/goroot";
  @NonNls public static final String GCLOUD_APP_ENGINE_DIRECTORY_PATH = "/platform/google_appengine";
  @NonNls public static final String GAE_EXECUTABLE_NAME = "goapp";
  @NonNls public static final String GAE_BAT_EXECUTABLE_NAME = "goapp.bat";
  @NonNls public static final String GAE_CMD_EXECUTABLE_NAME = "goapp.cmd";
  @NonNls public static final String DELVE_EXECUTABLE_NAME = "dlv";

  @NonNls public static final String GO_EXECUTABLE_NAME = "go";
  public static final String BUILD_FLAG = "+build";

  public static final String LINUX_OS = "linux";
  public static final String ANDROID_OS = "android";

  // see "$GOROOT/src/go/build/syslist.go
  public static final Set<String> KNOWN_OS = ContainerUtil.immutableSet(
      "aix", "android", "darwin", "dragonfly", "freebsd", "illumos", "ios",
      "js", "linux", "nacl", "netbsd", "openbsd", "plan9", "solaris",
      "wasip1", "windows");
  public static final Set<String> KNOWN_ARCH = ContainerUtil.immutableSet(
      "386", "amd64", "amd64p32", "arm", "arm64", "arm64be", "armbe",
      "loong64", "mips", "mips64", "mips64le", "mips64p32", "mips64p32le",
      "mipsle", "ppc", "ppc64", "ppc64le", "riscv64", "s390", "s390x",
      "sparc", "sparc64", "wasm");
  public static final Set<String> KNOWN_VERSIONS = ContainerUtil.immutableSet(
      "go1.1", "go1.2", "go1.3", "go1.4", "go1.5", "go1.6", "go1.7",
      "go1.8", "go1.9", "go1.10", "go1.11", "go1.12", "go1.13",
      "go1.14", "go1.15", "go1.16", "go1.17", "go1.18", "go1.19",
      "go1.20", "go1.21", "go1.22", "go1.23", "go1.24");
  public static final Set<String> KNOWN_CGO = ContainerUtil.immutableSet("darwin/386", "darwin/amd64", "dragonfly/386", "dragonfly/amd64",
                                                                         "freebsd/386", "freebsd/amd64", "freebsd/arm", "linux/386",
                                                                         "linux/amd64", "linux/arm", "linux/arm64", "android/386", 
                                                                         "android/amd64", "android/arm", "netbsd/386", "netbsd/amd64", 
                                                                         "netbsd/arm", "openbsd/386", "openbsd/amd64", "windows/386", 
                                                                         "windows/amd64", "linux/ppc64le");
  public static final Set<String> KNOWN_COMPILERS = ContainerUtil.immutableSet("gc", "gccgo");

  public static final String GO_MOD_FILENAME = "go.mod";
  public static final String GO_SUM_FILENAME = "go.sum";
  public static final String GO_WORK_FILENAME = "go.work";
  public static final String GO_MODULE_ENV = "GO111MODULE";

  @NonNls public static final String NIL = "nil";

  @NonNls public static final String GO = "Go";
  @NonNls public static final String AMD64 = "amd64";

  private GoConstants() {

  }
}
