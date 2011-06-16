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

import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class Native2AsciiPlugProjectComponent implements ProjectComponent {

    private final CompilerManager compilerManager;
    private final Compiler compiler;

    public Native2AsciiPlugProjectComponent(Project project, CompilerManager compilerManager) {
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
        compilerManager.addCompiler(compiler);
        compilerManager.addCompilableFileType(StdFileTypes.PROPERTIES);
    }

    public void projectClosed() {
        compilerManager.removeCompilableFileType(StdFileTypes.PROPERTIES);
        compilerManager.removeCompiler(compiler);
    }
}
