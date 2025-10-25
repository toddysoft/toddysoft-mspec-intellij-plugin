package com.toddysoft.mspec.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for finding related mspec files across source roots based on package structure.
 */
public class MSpecPackageUtil {

    /**
     * Finds all related mspec files that should be in scope for type resolution.
     * This includes:
     * 1. Files in the same directory (for backward compatibility)
     * 2. Files in the same package structure across different source roots
     *    (e.g., src/main/resources/protocols/bacnetip and src/main/generated/protocols/bacnetip)
     *
     * @param file The current PsiFile
     * @return List of related PsiFiles (excluding the current file)
     */
    public static List<PsiFile> findRelatedMSpecFiles(PsiFile file) {
        List<PsiFile> relatedFiles = new ArrayList<>();

        // Get the virtual file
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return relatedFiles;
        }

        Project project = file.getProject();
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        PsiManager psiManager = PsiManager.getInstance(project);

        // First, add files from the same directory (backward compatibility)
        addSiblingFiles(file, relatedFiles);

        // Check if the file is in a source or resource root
        VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
        if (sourceRoot == null) {
            // Not in a source root, return just the sibling files
            return relatedFiles;
        }

        // Calculate the relative path from the source root
        String relativePath = getRelativePath(sourceRoot, virtualFile.getParent());
        if (relativePath == null || relativePath.isEmpty()) {
            // File is directly in the source root
            return relatedFiles;
        }

        // Find all source roots in the project
        List<VirtualFile> allSourceRoots = getAllSourceRoots(project, fileIndex);

        // Search for matching package directories in other source roots
        for (VirtualFile otherRoot : allSourceRoots) {
            if (otherRoot.equals(sourceRoot)) {
                continue; // Skip the current source root (already handled)
            }

            // Look for the same relative path in this source root
            VirtualFile packageDir = otherRoot.findFileByRelativePath(relativePath);
            if (packageDir != null && packageDir.isDirectory()) {
                // Add all .mspec files from this package directory
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

    /**
     * Adds sibling .mspec files from the same directory.
     */
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

    /**
     * Gets the relative path from the source root to the given directory.
     */
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

        // If we didn't reach the source root, the directory is not under it
        if (current == null) {
            return null;
        }

        return path.toString();
    }

    /**
     * Gets all source and resource roots in the project.
     */
    private static List<VirtualFile> getAllSourceRoots(Project project, ProjectFileIndex fileIndex) {
        List<VirtualFile> roots = new ArrayList<>();

        // Iterate through all content roots and collect source roots
        com.intellij.openapi.module.Module[] modules = com.intellij.openapi.module.ModuleManager.getInstance(project).getModules();
        for (com.intellij.openapi.module.Module module : modules) {
            com.intellij.openapi.roots.ModuleRootManager rootManager = com.intellij.openapi.roots.ModuleRootManager.getInstance(module);

            // Add source roots
            for (VirtualFile sourceRoot : rootManager.getSourceRoots()) {
                if (!roots.contains(sourceRoot)) {
                    roots.add(sourceRoot);
                }
            }

            // Also include resource roots
            for (VirtualFile sourceRoot : rootManager.getSourceRoots(false)) {
                if (!roots.contains(sourceRoot)) {
                    roots.add(sourceRoot);
                }
            }
        }

        return roots;
    }
}
