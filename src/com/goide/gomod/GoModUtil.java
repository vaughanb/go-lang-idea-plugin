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

package com.goide.gomod;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoModUtil {
  public static final String GO_MOD = "go.mod";
  public static final String GO_SUM = "go.sum";
  public static final String GO_WORK = "go.work";

  private GoModUtil() {}

  @Nullable
  public static VirtualFile findGoModFile(@NotNull VirtualFile directory) {
    VirtualFile current = directory;
    while (current != null) {
      VirtualFile goMod = current.findChild(GO_MOD);
      if (goMod != null && !goMod.isDirectory()) {
        return goMod;
      }
      current = current.getParent();
    }
    return null;
  }

  public static boolean isModuleMode(@NotNull VirtualFile directory) {
    return findGoModFile(directory) != null;
  }
}
