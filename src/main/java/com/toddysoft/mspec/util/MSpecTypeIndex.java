package com.toddysoft.mspec.util;

import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cached extraction of MSpec type definitions.
 *
 * Type extraction was previously done by full-file regex scans on every annotator invocation,
 * completion trigger, and goto-declaration lookup. This class caches the per-file result keyed
 * to {@link PsiModificationTracker#MODIFICATION_COUNT}, and caches the scope-wide merged set
 * additionally against {@link ProjectRootModificationTracker} so module/root changes invalidate it.
 */
public final class MSpecTypeIndex {

    private static final Pattern TYPE_DEFINITION_PATTERN =
            Pattern.compile("\\[\\s*(?:type|dataIo|discriminatedType)\\s+([A-Za-z][A-Za-z0-9_-]*)");

    private static final Pattern ENUM_DEFINITION_PATTERN =
            Pattern.compile("\\[\\s*enum\\s+(?:(?:(?:bit|byte|vint|vuint|time|date|dateTime|vstring)\\s+)|(?:(?:int|uint|float|ufloat|string)\\s+\\d+\\s+))?([A-Za-z][A-Za-z0-9_-]*)");

    private static final Pattern ASTERISK_CASE_PATTERN =
            Pattern.compile("\\[\\s*'[^']*'\\s+\\*([A-Za-z][A-Za-z0-9_-]*)");

    private static final Pattern PARENT_TYPE_PATTERN =
            Pattern.compile("\\[\\s*(?:type|discriminatedType|dataIo)\\s+([A-Za-z][A-Za-z0-9_-]*)");

    private MSpecTypeIndex() {
    }

    /**
     * Type name -> file offset of the name (for goto-declaration). Cached per file.
     */
    public static Map<String, Integer> getTypeOffsetsInFile(PsiFile file) {
        return CachedValuesManager.getCachedValue(file, () -> {
            Map<String, Integer> result = new LinkedHashMap<>();
            String fileText = file.getText();

            Matcher matcher = TYPE_DEFINITION_PATTERN.matcher(fileText);
            while (matcher.find()) {
                result.putIfAbsent(matcher.group(1), matcher.start(1));
            }

            Matcher enumMatcher = ENUM_DEFINITION_PATTERN.matcher(fileText);
            while (enumMatcher.find()) {
                result.putIfAbsent(enumMatcher.group(1), enumMatcher.start(1));
            }

            Matcher asteriskMatcher = ASTERISK_CASE_PATTERN.matcher(fileText);
            while (asteriskMatcher.find()) {
                String caseName = asteriskMatcher.group(1);
                String parent = findParentTypeName(fileText, asteriskMatcher.start());
                if (parent != null) {
                    result.putIfAbsent(parent + caseName, asteriskMatcher.start(1));
                }
            }

            return CachedValueProvider.Result.create(
                    Collections.unmodifiableMap(result),
                    file);
        });
    }

    /**
     * Type names defined in the given file. Cached.
     */
    public static Set<String> getTypesInFile(PsiFile file) {
        return getTypeOffsetsInFile(file).keySet();
    }

    /**
     * Type names defined in the file plus all related files (same dir, same package across roots).
     * Cached.
     */
    public static Set<String> getTypesInScope(PsiFile file) {
        return CachedValuesManager.getCachedValue(file, () -> {
            Set<String> all = new HashSet<>(getTypesInFile(file));
            for (PsiFile related : MSpecPackageUtil.findRelatedMSpecFiles(file)) {
                all.addAll(getTypesInFile(related));
            }
            return CachedValueProvider.Result.create(
                    Collections.unmodifiableSet(all),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    ProjectRootModificationTracker.getInstance(file.getProject()));
        });
    }

    /**
     * Looks up where a type is defined across the file and related files. Returns null if not found.
     */
    public static TypeLocation findTypeDefinition(PsiFile file, String typeName) {
        Integer localOffset = getTypeOffsetsInFile(file).get(typeName);
        if (localOffset != null) {
            return new TypeLocation(file, localOffset);
        }
        for (PsiFile related : MSpecPackageUtil.findRelatedMSpecFiles(file)) {
            Integer offset = getTypeOffsetsInFile(related).get(typeName);
            if (offset != null) {
                return new TypeLocation(related, offset);
            }
        }
        return null;
    }

    private static String findParentTypeName(String fileText, int offset) {
        Matcher matcher = PARENT_TYPE_PATTERN.matcher(fileText);
        String lastMatch = null;
        while (matcher.find()) {
            if (matcher.start() >= offset) {
                break;
            }
            lastMatch = matcher.group(1);
        }
        return lastMatch;
    }

    public static final class TypeLocation {
        public final PsiFile file;
        public final int offset;

        TypeLocation(PsiFile file, int offset) {
            this.file = file;
            this.offset = offset;
        }
    }
}
