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

import com.goide.GoIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GoModFileType extends LanguageFileType {
  public static final GoModFileType INSTANCE = new GoModFileType();

  private GoModFileType() {
    super(GoModLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "Go Module";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Go module file";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "mod";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GoIcons.ICON;
  }
}
