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

package com.goide.project.migration;

import com.goide.GoConstants;
import com.goide.project.GoApplicationLibrariesService;
import com.goide.project.GoProjectLibrariesService;
import com.goide.sdk.GoSdkType;
import com.goide.sdk.GoSdkUtil;
import com.intellij.conversion.*;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

public class GoProjectModelConverterProvider extends ConverterProvider {
  private static final String PROJECT_ROOT_MANAGER = "ProjectRootManager";

  protected GoProjectModelConverterProvider() {
    super();
  }

  @NotNull
  @Override
  public String getConversionDescription() {
    return "Go project model has been changed so project and its modules need to be updated";
  }

  @NotNull
  @Override
  public ProjectConverter createConverter(@NotNull ConversionContext context) {
    return new ProjectConverter() {
      private final Collection<Path> additionalCreatedFiles = new ArrayList<>();
      private final Collection<Path> additionalAffectedFiles = new ArrayList<>();

      @Nullable
      @Override
      public ConversionProcessor<ComponentManagerSettings> createProjectFileConverter() {
        return new ProjectFileConverter();
      }

      @Nullable
      @Override
      public ConversionProcessor<ModuleSettings> createModuleFileConverter() {
        return new ModuleFileConverter();
      }

      @Nullable
      @Override
      public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
        return new RunConfigurationsConverter();
      }

      @Override
      public boolean isConversionNeeded() {
        Element component = getProjectRootManager(context);
        return component != null && isGoSdkType(component.getAttributeValue("projectJdkType"));
      }

      @Override
      public void preProcessingFinished() throws CannotConvertException {
        Element component = getProjectRootManager(context);
        if (component != null) {
          try {
            File miscFile = miscFile(context);
            updateSdkType(miscFile, component);
            additionalAffectedFiles.add(miscFile.toPath());

            File oldGoSettings = context.getSettingsBaseDir().resolve("go_settings.xml").toFile();
            if (oldGoSettings.exists()) {
              Element oldGoSettingsRoot = rootElement(oldGoSettings);
              if (isAttachProjectDirToLibraries(oldGoSettingsRoot)) {
                File librariesConfigFile = context.getSettingsBaseDir().resolve(GoConstants.GO_LIBRARIES_CONFIG_FILE).toFile();
                if (librariesConfigFile.exists()) {
                  //noinspection ResultOfMethodCallIgnored
                  librariesConfigFile.delete();
                  additionalAffectedFiles.add(librariesConfigFile.toPath());
                }
                else {
                  additionalCreatedFiles.add(librariesConfigFile.toPath());
                }
                FileUtil.writeToFile(librariesConfigFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project></project>");
                addProjectDirToLibraries(librariesConfigFile, rootElement(librariesConfigFile));
              }
            }
            //noinspection ResultOfMethodCallIgnored
            oldGoSettings.delete();
          }
          catch (IOException e) {
            throw new CannotConvertException("Cannot convert go project", e);
          }
        }
        convertSdks();
      }

      @NotNull
      @Override
      public Collection<Path> getAdditionalAffectedFiles() {
        return additionalAffectedFiles;
      }

      @NotNull
      @Override
      public Collection<Path> getCreatedFiles() {
        return additionalCreatedFiles;
      }
    };
  }

  @NotNull
  private static Element rootElement(@NotNull File file) throws CannotConvertException {
    try {
      return JDOMUtil.load(file);
    }
    catch (Exception e) {
      throw new CannotConvertException("Cannot load " + file.getPath(), e);
    }
  }

  private static class ProjectFileConverter extends ConversionProcessor<ComponentManagerSettings> {
    @Override
    public boolean isConversionNeeded(@NotNull ComponentManagerSettings settings) {
      Element projectRootManager = getProjectRootManager(settings.getRootElement());
      return projectRootManager != null && isGoSdkType(projectRootManager.getAttributeValue("projectJdkType"));
    }

    @Override
    public void process(@NotNull ComponentManagerSettings settings) throws CannotConvertException {
      Element projectRootManager = getProjectRootManager(settings.getRootElement());
      if (projectRootManager != null) {
        updateSdkType(settings.getPath().toFile(), projectRootManager);
      }
      if (isAttachProjectDirToLibraries(settings.getRootElement())) {
        addProjectDirToLibraries(settings.getPath().toFile(), settings.getRootElement());
      }
      convertSdks();
    }
  }

  private static class ModuleFileConverter extends ConversionProcessor<ModuleSettings> {
    @Override
    public boolean isConversionNeeded(@NotNull ModuleSettings settings) {
      if ("GO_APP_ENGINE_MODULE".equals(settings.getModuleType())) return true;
      for (Element element : settings.getOrderEntries()) {
        if (isGoSdkType(element.getAttributeValue("jdkType"))) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void process(@NotNull ModuleSettings settings) throws CannotConvertException {
      settings.setModuleType(GoConstants.MODULE_TYPE_ID);
      for (Element element : settings.getOrderEntries()) {
        if (isGoSdkType(element.getAttributeValue("jdkType"))) {
          element.setAttribute("jdkType", GoConstants.SDK_TYPE_ID);
        }
      }
      convertSdks();
    }
  }

  private static Element getProjectRootManager(@NotNull ConversionContext context) {
    File miscFile = miscFile(context);
    try {
      if (miscFile.exists()) {
        return getProjectRootManager(rootElement(miscFile));
      }
    }
    catch (CannotConvertException e) {
      return null;
    }
    return null;
  }

  @Nullable
  private static Element getProjectRootManager(@Nullable Element rootElement) {
    return rootElement != null ? JDomSerializationUtil.findComponent(rootElement, PROJECT_ROOT_MANAGER) : null;
  }

  private static void updateSdkType(@NotNull File file, @NotNull Element projectRootManager) throws CannotConvertException {
    projectRootManager.setAttribute("projectJdkType", GoConstants.SDK_TYPE_ID);
    saveFile(file, projectRootManager, "Cannot save sdk type changing");
  }

  @NotNull
  private static File miscFile(@NotNull ConversionContext context) {
    return context.getSettingsBaseDir().resolve("misc.xml").toFile();
  }

  private static void addProjectDirToLibraries(@NotNull File file, @NotNull Element rootElement) throws CannotConvertException {
    GoProjectLibrariesService librariesService = new GoProjectLibrariesService();
    librariesService.setLibraryRootUrls("file://$PROJECT_DIR$");
    Element componentElement = JDomSerializationUtil.findOrCreateComponentElement(rootElement, GoConstants.GO_LIBRARIES_SERVICE_NAME);
    XmlSerializer.serializeInto(librariesService.getState(), componentElement);
    saveFile(file, rootElement, "Cannot save libraries settings");
  }

  
  private static boolean isAttachProjectDirToLibraries(Element rootElement) {
    Element goProjectSettings = JDomSerializationUtil.findComponent(rootElement, "GoProjectSettings");
    if (goProjectSettings != null) {
      for (Element option : goProjectSettings.getChildren("option")) {
        if ("prependGoPath".equals(option.getAttributeValue("name"))) {
          goProjectSettings.detach();
          return "true".equalsIgnoreCase(option.getAttributeValue("value"));
        }
      }
      goProjectSettings.detach();
    }
    return false;
  }

  private static boolean isGoSdkType(String sdkTypeName) {
    return "Google Go SDK".equals(sdkTypeName) || "Google Go App Engine SDK".equals(sdkTypeName);
  }

  private static void saveFile(@NotNull File file, @NotNull Element rootElement, String errorMessage) throws CannotConvertException {
    try {
      JDOMUtil.write(rootElement, file.toPath());
    }
    catch (IOException e) {
      throw new CannotConvertException(errorMessage, e);
    }
  }

  private static void convertSdks() {
    ProjectJdkTable sdkTable = ProjectJdkTable.getInstance();
    Collection<String> globalGoPathUrls = new LinkedHashSet<>();
    Collection<Sdk> sdksToDelete = new ArrayList<>();
    Collection<Sdk> sdksToAdd = new ArrayList<>();
    GoSdkType sdkType = GoSdkType.getInstance();
    for (Sdk sdk : sdkTable.getAllJdks()) {
      String sdkTypeName = sdk.getSdkType().getName();
      if (isGoSdkType(sdkTypeName)) {
        sdksToDelete.add(sdk);
        String sdkHome = sdkType.adjustSelectedSdkHome(sdk.getHomePath());
        if (sdkType.isValidSdkHome(sdkHome)) {
          ProjectJdkImpl newSdk = new ProjectJdkImpl(sdk.getName(), sdkType, sdkHome, sdkType.getVersionString(sdkHome));
          sdkType.setupSdkPaths(newSdk);
          sdksToAdd.add(newSdk);

          for (String classesRoot : sdk.getRootProvider().getUrls(OrderRootType.CLASSES)) {
            if (!classesRoot.equals(sdk.getHomePath())) {
              globalGoPathUrls.add(classesRoot);
            }
          }
        }
      }
    }

    for (VirtualFile file : GoSdkUtil.getGoPathsRootsFromEnvironment()) {
      globalGoPathUrls.remove(file.getUrl());
    }

    WriteAction.run(() -> {
      for (Sdk sdk : sdksToDelete) {
        sdkTable.removeJdk(sdk);
      }
      for (Sdk sdk : sdksToAdd) {
        sdkTable.addJdk(sdk);
      }
      globalGoPathUrls.addAll(GoApplicationLibrariesService.getInstance().getLibraryRootUrls());
      GoApplicationLibrariesService.getInstance().setLibraryRootUrls(globalGoPathUrls);
    });
  }

  private static class RunConfigurationsConverter extends ConversionProcessor<RunManagerSettings> {
    @Override
    public boolean isConversionNeeded(@NotNull RunManagerSettings settings) {
      for (Element element : settings.getRunConfigurations()) {
        String confType = element.getAttributeValue("type");
        if ("GaeLocalAppEngineServer".equals(confType) || "GoTestConfiguration".equals(confType)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void process(@NotNull RunManagerSettings settings) throws CannotConvertException {
      for (Element element : settings.getRunConfigurations()) {
        String confType = element.getAttributeValue("type");
        if ("GaeLocalAppEngineServer".equals(confType) || "GoTestConfiguration".equals(confType)) {
          element.detach();
        }
      }
    }
  }
}
