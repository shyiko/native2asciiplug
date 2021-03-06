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
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

public class Native2AsciiPlugCompiler implements ClassPostProcessingCompiler {

    private Project project;

    public Native2AsciiPlugCompiler(Project project) {
        this.project = project;
    }

    @NotNull
    public ProcessingItem[] getProcessingItems(final CompileContext compileContext) {
        final List<ProcessingItem> result = new ArrayList<ProcessingItem>();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                for (VirtualFile sourceFile : compileContext.getCompileScope().getFiles(StdFileTypes.PROPERTIES, true)) {
                    String targetFilePath = getTargetFilePath(compileContext, fileIndex, sourceFile);
                    if (targetFilePath != null) {
                        result.add(new Native2AsciiPlugProcessingItem(sourceFile, targetFilePath));
                    }
                }
            }
        });
        return result.toArray(new ProcessingItem[result.size()]);
    }

    private String getTargetFilePath(CompileContext compileContext, ProjectFileIndex fileIndex, VirtualFile sourceFile) {
        Module module = fileIndex.getModuleForFile(sourceFile);
        VirtualFile sourceRoot = getSourceRoot(compileContext, module, sourceFile);
        if (module == null || sourceRoot == null) {
            return null;
        }
        boolean inTestSourceContent = fileIndex.isInTestSourceContent(sourceFile);
        String outputPath = CompilerPaths.getModuleOutputPath(module, inTestSourceContent);
        String packagePrefix = fileIndex.getPackageNameByDirectory(sourceRoot);
        String relativePath = VfsUtil.getRelativePath(sourceFile, sourceRoot, '/');
        return outputPath + "/" +
                (packagePrefix != null && !packagePrefix.isEmpty() ? packagePrefix.replace('.', '/') + "/" : "")
                + relativePath;
    }

    public ProcessingItem[] process(CompileContext compileContext, ProcessingItem[] items) {
        ProgressIndicator progressIndicator = compileContext.getProgressIndicator();
        progressIndicator.pushState();
        progressIndicator.setText2(Native2AsciiPlugBundle.message("native2asciiplug.title"));
        progressIndicator.setText(Native2AsciiPlugBundle.message("native2asciiplug.start.message"));

        List<ProcessingItem> successfullyProcessedItems = new ArrayList<ProcessingItem>();
        for (ProcessingItem item : items) {
            Native2AsciiPlugProcessingItem processingItem = (Native2AsciiPlugProcessingItem) item;
            String file = processingItem.getFile().getUrl();
            try {
                progressIndicator.setText(Native2AsciiPlugBundle.message("native2asciiplug.compile.message", file));
                processingItem.process();
                successfullyProcessedItems.add(processingItem);
            } catch (IOException ex) {
                compileContext.addMessage(CompilerMessageCategory.ERROR,
                        ex.getLocalizedMessage(),
                        file, -1, -1);
            }
        }

        progressIndicator.popState();
        return successfullyProcessedItems.toArray(new ProcessingItem[successfullyProcessedItems.size()]);
    }

    @NotNull
    public String getDescription() {
        return "Native2AsciiPlugCompiler";
    }

    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public ValidityState createValidityState(DataInput in) throws IOException {
        return new  EmptyValidityState();
    }

    public static VirtualFile getSourceRoot(CompileContext context, Module module, VirtualFile file) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
        VirtualFile root = fileIndex.getSourceRootForFile(file);
        if(root == null) {
            VirtualFile[] sourceRoots = context.getSourceRoots(module);
            for(int i = 0; i < sourceRoots.length; ++i) {
                VirtualFile sourceRoot = sourceRoots[i];
                if(!fileIndex.isInSourceContent(sourceRoot) &&
                        VfsUtil.isAncestor(sourceRoot, file, false)) {
                    root = sourceRoot;
                    break;
                }
            }
        }
        return root;
    }

    private class Native2AsciiPlugProcessingItem implements ProcessingItem {

        private VirtualFile sourceFile;
        private String targetFile;

        private Native2AsciiPlugProcessingItem(VirtualFile sourceFile, String targetFile) {
            this.sourceFile = sourceFile;
            this.targetFile = targetFile;
        }

        @NotNull
        public VirtualFile getFile() {
            return sourceFile;
        }

        public ValidityState getValidityState() {
            return new  EmptyValidityState();
        }

        public void process() throws IOException {
            WritableByteChannel wbc = Channels.newChannel(new FileOutputStream(targetFile));
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
}
