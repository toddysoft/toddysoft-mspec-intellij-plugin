package com.toddysoft.mspec.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for finding related mspec files across source roots based on package structure.
 *
 * Results are cached: per-file via {@link CachedValuesManager} (invalidated on PSI changes or
 * project root structure changes), and the all-source-roots list is cached per project.
 */
public class MSpecPackageUtil {

    /**
     * Finds all related mspec files that should be in scope for type resolution.
     * Cached against PSI and project root modifications.
     */
    public static List<PsiFile> findRelatedMSpecFiles(PsiFile file) {
        return CachedValuesManager.getCachedValue(file, () -> {
            List<PsiFile> related = computeRelatedMSpecFiles(file);
            return CachedValueProvider.Result.create(
                    Collections.unmodifiableList(related),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    ProjectRootModificationTracker.getInstance(file.getProject()));
        });
    }

    private static List<PsiFile> computeRelatedMSpecFiles(PsiFile file) {
        List<PsiFile> relatedFiles = new ArrayList<>();

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return relatedFiles;
        }

        Project project = file.getProject();
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        PsiManager psiManager = PsiManager.getInstance(project);

        addSiblingFiles(file, relatedFiles);

        VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
        if (sourceRoot == null) {
            return relatedFiles;
        }

        String relativePath = getRelativePath(sourceRoot, virtualFile.getParent());
        if (relativePath == null || relativePath.isEmpty()) {
            return relatedFiles;
        }

        for (VirtualFile otherRoot : getAllSourceRoots(project)) {
            if (otherRoot.equals(sourceRoot)) {
                continue;
            }

            VirtualFile packageDir = otherRoot.findFileByRelativePath(relativePath);
            if (packageDir != null && packageDir.isDirectory()) {
                for (VirtualFile child : packageDir.getChildren()) {
                    if (child.getName().endsWith(".mspec") && !child.equals(virtualFile)) {
                        PsiFile psiFile = psiManager.findFile(child);
                        if (psiFile != null && !relatedFiles.contains(psiFile)) {
                            relatedFiles.add(psiFile);
                        }
                    }
                }
            }
        }

        return relatedFiles;
    }

    private static void addSiblingFiles(PsiFile file, List<PsiFile> relatedFiles) {
        if (file.getParent() != null) {
            for (com.intellij.psi.PsiElement child : file.getParent().getChildren()) {
                if (child instanceof PsiFile) {
                    PsiFile siblingFile = (PsiFile) child;
                    if (!siblingFile.equals(file) && siblingFile.getName().endsWith(".mspec")) {
                        relatedFiles.add(siblingFile);
                    }
                }
            }
        }
    }

    private static String getRelativePath(VirtualFile sourceRoot, VirtualFile directory) {
        if (directory == null || sourceRoot == null) {
            return null;
        }

        StringBuilder path = new StringBuilder();
        VirtualFile current = directory;

        while (current != null && !current.equals(sourceRoot)) {
            if (path.length() > 0) {
                path.insert(0, "/");
            }
            path.insert(0, current.getName());
            current = current.getParent();
        }

        if (current == null) {
            return null;
        }

        return path.toString();
    }

    /**
     * All source/resource roots in the project. Cached against project root modifications.
     */
    private static Set<VirtualFile> getAllSourceRoots(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
            Set<VirtualFile> roots = new LinkedHashSet<>();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                Collections.addAll(roots, rootManager.getSourceRoots());
                Collections.addAll(roots, rootManager.getSourceRoots(false));
            }
            return CachedValueProvider.Result.create(
                    Collections.unmodifiableSet(roots),
                    ProjectRootModificationTracker.getInstance(project));
        });
    }
}
