/*
 * Copyright 2011 Stanley Shyiko
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
package com.github.shyiko.native2asciiplug;

import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Native2AsciiPlugProjectComponent implements ProjectComponent {

    private static final Logger logger = Logger.getInstance("#" + Native2AsciiPlugProjectComponent.class.getName());

    private final CompilerManager compilerManager;
    private final Compiler compiler;
    private final Project project;

    public Native2AsciiPlugProjectComponent(ProjectEx project, CompilerManager compilerManager) {
        this.project = project;
        this.compilerManager = compilerManager;
        this.compiler = new Native2AsciiPlugCompiler(project);
    }

    public void initComponent() {}

    public void disposeComponent() {}

    @NotNull
    public String getComponentName() {
        return "Native2AsciiPlugProjectComponent";
    }

    public void projectOpened() {
        if (isOutOfProcessBuildTurnedOn()) {
            injectCompilerForOutOfProcessBuild();
        } else {
            compilerManager.addCompiler(compiler);
        }
        compilerManager.addCompilableFileType(StdFileTypes.PROPERTIES);
    }

    private boolean isOutOfProcessBuildTurnedOn() {
        CompilerWorkspaceConfiguration configuration = CompilerWorkspaceConfiguration.getInstance(project);
        try {
            Method useOutOfProcessBuild = configuration.getClass().getMethod("useOutOfProcessBuild");
            return (Boolean) useOutOfProcessBuild.invoke(configuration);
        } catch (NoSuchMethodException e) {
            // expected on IDEA releases less than 12
        } catch (Exception e) {
            logger.error("Failed to determine whether \"Use external build\" is turned on", e);
        }
        return false;
    }

    private void injectCompilerForOutOfProcessBuild() {
        try {
            Class<?> compilerAdapter = Class.forName("com.intellij.compiler.impl.FileProcessingCompilerAdapterTask");
            Constructor<?> constructor = compilerAdapter.getConstructor(FileProcessingCompiler.class);
            compilerManager.addAfterTask((CompileTask) constructor.newInstance(compiler));
        } catch (Exception e) {
            logger.error("Failed to inject compiler with \"Use external build\" being turned on", e);
        }
    }

    public void projectClosed() {
        compilerManager.removeCompilableFileType(StdFileTypes.PROPERTIES);
        compilerManager.removeCompiler(compiler);
    }
}
