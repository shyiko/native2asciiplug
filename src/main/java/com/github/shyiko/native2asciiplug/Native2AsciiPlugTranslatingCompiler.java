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

import com.intellij.compiler.impl.javaCompiler.OutputItemImpl;
import com.intellij.compiler.make.MakeUtil;
import com.intellij.lang.properties.charset.Native2AsciiCharset;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Native2AsciiPlugTranslatingCompiler implements TranslatingCompiler {

    private Project project;

    public Native2AsciiPlugTranslatingCompiler(Project project) {
        this.project = project;
    }

    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        return StdFileTypes.PROPERTIES.equals(virtualFile.getFileType());
    }

    public void compile(final CompileContext compileContext, Chunk<Module> moduleChunk, final VirtualFile[] virtualFiles, OutputSink outputSink) {
        ProgressIndicator progressIndicator = compileContext.getProgressIndicator();
        progressIndicator.pushState();
        progressIndicator.setText2(Native2AsciiBundle.message("native2asciiplug.title"));
        progressIndicator.setText(Native2AsciiBundle.message("native2asciiplug.start.message"));

        final Map<String, List<OutputItem>> output = new HashMap<String, List<OutputItem>>();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {

                ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

                for (VirtualFile sourceFile : virtualFiles) {
                    Module module = fileIndex.getModuleForFile(sourceFile);
                    VirtualFile sourceRoot = MakeUtil.getSourceRoot(compileContext, module, sourceFile);
                    if (module == null || sourceRoot == null) {
                        continue;
                    }
                    boolean inTestSourceContent = fileIndex.isInTestSourceContent(sourceFile);
                    String outputPath = CompilerPaths.getModuleOutputPath(module, inTestSourceContent);

                    String packagePrefix = fileIndex.getPackageNameByDirectory(sourceRoot);
                    String relativePath = VfsUtil.getRelativePath(sourceFile, sourceRoot, '/');
                    String outputFilePath = outputPath + "/" +
                            (packagePrefix != null && !packagePrefix.isEmpty() ? packagePrefix.replace('.', '/') + "/" : "")
                            + relativePath;
                    OutputItem outputItem = new OutputItemImpl(outputFilePath, sourceFile);

                    List<OutputItem> outputItemList = output.get(outputPath);
                    if (outputItemList == null) {
                        outputItemList = new ArrayList<OutputItem>();
                        output.put(outputPath, outputItemList);
                    }
                    outputItemList.add(outputItem);
                }
            }
        });

        root: for (Map.Entry<String, List<OutputItem>> entry : output.entrySet()) {
            List<OutputItem> outputItems = entry.getValue();
            for (OutputItem outputItem : outputItems) {
                VirtualFile sourceFile = outputItem.getSourceFile();
                progressIndicator.setText(Native2AsciiBundle.message("native2asciiplug.compile.message", sourceFile.getUrl()));
                try {
                    native2ascii(sourceFile, outputItem.getOutputPath());
                } catch (IOException ex) {
                    compileContext.addMessage(CompilerMessageCategory.ERROR,
                            ex.getLocalizedMessage(),
                            sourceFile.getUrl(), -1, -1);
                    break root;
                }
            }
            outputSink.add(entry.getKey(), outputItems, VirtualFile.EMPTY_ARRAY);
        }

        progressIndicator.popState();
    }

    @NotNull
    public String getDescription() {
        return "Native2AsciiPlugTranslatingCompiler";
    }

    public boolean validateConfiguration(CompileScope compileScope) {
        return true;
    }

    private void native2ascii(VirtualFile sourceFile, String outputFile) throws IOException {
        WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(outputFile));
        try {
            Charset charset = sourceFile.getCharset();
            String native2AsciiEncodingName = Native2AsciiCharset.makeNative2AsciiEncodingName(charset.name());
            CharsetEncoder charsetEncoder = Native2AsciiCharset.forName(native2AsciiEncodingName).newEncoder();
            ByteBuffer sourceBuffer = ByteBuffer.wrap(sourceFile.contentsToByteArray());
            ByteBuffer targetBuffer = charsetEncoder.encode(charset.decode(sourceBuffer));
            wbc.write(targetBuffer);
        } finally {
            wbc.close();
        }
    }
}
