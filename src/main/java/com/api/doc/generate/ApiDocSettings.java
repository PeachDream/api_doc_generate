package com.api.doc.generate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API文档生成器配置
 * 持久化存储用户的配置选项
 *
 * @author peach
 * @since 2025/12/25 | V1.0.0
 */
@Service
@State(name = "ApiDocGeneratorSettings", storages = @Storage("ApiDocGeneratorSettings.xml"))
public final class ApiDocSettings implements PersistentStateComponent<ApiDocSettings.State> {

    /**
     * 配置状态类
     *
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public static class State {
        /** 是否显示接口调用位置 */
        public boolean showCallLocation = true;
        /** 是否使用Git分支作为版本号 */
        public boolean useGitBranchAsVersion = true;
        /** 默认作者名 */
        public String defaultAuthor = "peach";
        /** 是否显示请求参数JSON */
        public boolean showRequestJson = true;
        /** 是否显示返回参数JSON */
        public boolean showResponseJson = true;
        /** 排除的父类名称列表（多个类名用逗号分隔） */
        public String excludedParentClasses = "";
        /**
         * 排除的字段映射
         * 格式: 类名1:字段1,字段2,字段3;类名2:字段1,字段2
         * 如果某个类的字段列表为空或为*，表示排除该类的所有字段
         *
         * @since 2025/12/26 | V1.0.0
         */
        public String excludedFieldsMap = "";
        /** 预览主题名称 */
        public String previewTheme = "FOLLOW_IDE";
        /** 预览开关是否开启 */
        public boolean previewEnabled = true;
        /** 导出文件路径（记住上次导出位置） */
        public String exportPath = "";
    }

    private State myState = new State();

    /**
     * 获取配置实例
     *
     * @return 配置实例
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public static ApiDocSettings getInstance() {
        return ApplicationManager.getApplication().getService(ApiDocSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    /**
     * 是否显示接口调用位置
     *
     * @return 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public boolean isShowCallLocation() {
        return myState.showCallLocation;
    }

    /**
     * 设置是否显示接口调用位置
     *
     * @param show 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setShowCallLocation(boolean show) {
        myState.showCallLocation = show;
    }

    /**
     * 是否使用Git分支作为版本号
     *
     * @return 是否使用
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public boolean isUseGitBranchAsVersion() {
        return myState.useGitBranchAsVersion;
    }

    /**
     * 设置是否使用Git分支作为版本号
     *
     * @param use 是否使用
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setUseGitBranchAsVersion(boolean use) {
        myState.useGitBranchAsVersion = use;
    }

    /**
     * 获取默认作者名
     *
     * @return 作者名
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getDefaultAuthor() {
        return myState.defaultAuthor;
    }

    /**
     * 设置默认作者名
     *
     * @param author 作者名
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setDefaultAuthor(String author) {
        myState.defaultAuthor = author;
    }

    /**
     * 是否显示请求参数JSON
     *
     * @return 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public boolean isShowRequestJson() {
        return myState.showRequestJson;
    }

    /**
     * 设置是否显示请求参数JSON
     *
     * @param show 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setShowRequestJson(boolean show) {
        myState.showRequestJson = show;
    }

    /**
     * 是否显示返回参数JSON
     *
     * @return 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public boolean isShowResponseJson() {
        return myState.showResponseJson;
    }

    /**
     * 设置是否显示返回参数JSON
     *
     * @param show 是否显示
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setShowResponseJson(boolean show) {
        myState.showResponseJson = show;
    }

    /**
     * 获取排除的父类名称列表
     *
     * @return 排除的父类名称（多个用逗号分隔）
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getExcludedParentClasses() {
        return myState.excludedParentClasses;
    }

    /**
     * 设置排除的父类名称列表
     *
     * @param excludedClasses 排除的父类名称（多个用逗号分隔）
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public void setExcludedParentClasses(String excludedClasses) {
        myState.excludedParentClasses = excludedClasses;
    }

    /**
     * 检查类名是否在排除列表中
     *
     * @param className 类名（可以是简单类名或全限定名）
     * @return 是否应该排除
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public boolean isClassExcluded(String className) {
        if (className == null || myState.excludedParentClasses == null || myState.excludedParentClasses.isEmpty()) {
            return false;
        }
        String[] excludedList = myState.excludedParentClasses.split(",");
        for (String excluded : excludedList) {
            String trimmed = excluded.trim();
            if (!trimmed.isEmpty()) {
                // 支持简单类名或全限定名匹配
                if (className.equals(trimmed) || className.endsWith("." + trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取排除字段映射字符串
     *
     * @return 排除字段映射
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getExcludedFieldsMap() {
        return myState.excludedFieldsMap == null ? "" : myState.excludedFieldsMap;
    }

    /**
     * 设置排除字段映射字符串
     *
     * @param excludedFieldsMap 排除字段映射
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void setExcludedFieldsMap(String excludedFieldsMap) {
        myState.excludedFieldsMap = excludedFieldsMap;
    }

    /**
     * 获取指定类的排除字段列表
     *
     * @param className 类名（全限定名）
     * @return 排除的字段集合，如果返回null表示排除所有字段，空集合表示不排除任何字段
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public java.util.Set<String> getExcludedFieldsForClass(String className) {
        if (className == null || myState.excludedFieldsMap == null || myState.excludedFieldsMap.isEmpty()) {
            return null; // 没有配置，走默认逻辑（排除所有）
        }

        // 解析格式: 类名1:字段1,字段2;类名2:字段1,字段2
        String[] classEntries = myState.excludedFieldsMap.split(";");
        for (String entry : classEntries) {
            if (entry.trim().isEmpty()) {
                continue;
            }

            int colonIndex = entry.indexOf(':');
            if (colonIndex <= 0) {
                continue;
            }

            String entryClassName = entry.substring(0, colonIndex).trim();
            // 匹配类名（支持简单名和全限定名）
            if (className.equals(entryClassName) ||
                    className.endsWith("." + entryClassName) ||
                    entryClassName.endsWith("." + getSimpleClassName(className))) {

                String fieldsStr = entry.substring(colonIndex + 1).trim();
                // * 表示排除所有字段
                if ("*".equals(fieldsStr) || fieldsStr.isEmpty()) {
                    return null;
                }

                java.util.Set<String> fields = new java.util.HashSet<>();
                for (String field : fieldsStr.split(",")) {
                    String trimmedField = field.trim();
                    if (!trimmedField.isEmpty()) {
                        fields.add(trimmedField);
                    }
                }
                return fields;
            }
        }

        return null; // 没有找到该类的配置，走默认逻辑
    }

    /**
     * 检查指定类的指定字段是否应该排除
     *
     * @param className 类名（全限定名）
     * @param fieldName 字段名
     * @return 是否应该排除该字段
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public boolean isFieldExcluded(String className, String fieldName) {
        // 首先检查类是否在排除列表中
        if (!isClassExcluded(className)) {
            return false;
        }

        // 获取该类的排除字段列表
        java.util.Set<String> excludedFields = getExcludedFieldsForClass(className);

        // null 表示排除所有字段
        if (excludedFields == null) {
            return true;
        }

        // 检查字段是否在排除列表中
        return excludedFields.contains(fieldName);
    }

    /**
     * 获取简单类名
     *
     * @param fullClassName 全限定类名
     * @return 简单类名
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }

    /**
     * 更新指定类的排除字段列表
     *
     * @param className      类名（全限定名）
     * @param excludedFields 排除的字段集合，null或空表示排除所有
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void updateExcludedFieldsForClass(String className, java.util.Set<String> excludedFields) {
        java.util.Map<String, String> classFieldsMap = parseExcludedFieldsMap();

        if (excludedFields == null || excludedFields.isEmpty()) {
            classFieldsMap.put(className, "*");
        } else {
            classFieldsMap.put(className, String.join(",", excludedFields));
        }

        // 重新构建字符串
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : classFieldsMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        myState.excludedFieldsMap = sb.toString();
    }

    /**
     * 解析排除字段映射为Map
     *
     * @return 类名到字段列表的映射
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private java.util.Map<String, String> parseExcludedFieldsMap() {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        if (myState.excludedFieldsMap == null || myState.excludedFieldsMap.isEmpty()) {
            return result;
        }

        String[] classEntries = myState.excludedFieldsMap.split(";");
        for (String entry : classEntries) {
            if (entry.trim().isEmpty()) {
                continue;
            }

            int colonIndex = entry.indexOf(':');
            if (colonIndex > 0) {
                String className = entry.substring(0, colonIndex).trim();
                String fields = entry.substring(colonIndex + 1).trim();
                result.put(className, fields);
            }
        }
        return result;
    }

    /**
     * 移除指定类的排除字段配置
     *
     * @param className 类名
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void removeExcludedFieldsForClass(String className) {
        java.util.Map<String, String> classFieldsMap = parseExcludedFieldsMap();
        classFieldsMap.remove(className);

        // 重新构建字符串
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : classFieldsMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        myState.excludedFieldsMap = sb.toString();
    }

    /**
     * 获取预览主题名称
     *
     * @return 主题名称
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getPreviewTheme() {
        return myState.previewTheme == null ? "FOLLOW_IDE" : myState.previewTheme;
    }

    /**
     * 设置预览主题名称
     *
     * @param themeName 主题名称
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void setPreviewTheme(String themeName) {
        myState.previewTheme = themeName;
    }

    /**
     * 预览开关是否开启
     *
     * @return 是否开启预览
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public boolean isPreviewEnabled() {
        return myState.previewEnabled;
    }

    /**
     * 设置预览开关状态
     *
     * @param enabled 是否开启预览
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void setPreviewEnabled(boolean enabled) {
        myState.previewEnabled = enabled;
    }

    /**
     * 获取导出路径
     *
     * @return 导出路径
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getExportPath() {
        return myState.exportPath == null ? "" : myState.exportPath;
    }

    /**
     * 设置导出路径
     *
     * @param exportPath 导出路径
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public void setExportPath(String exportPath) {
        myState.exportPath = exportPath;
    }

    /**
     * 验证并清理无效的配置
     * 检查配置的类是否存在，检查配置的字段是否仍然存在于类中
     * 如果发现无效配置，自动清理
     *
     * @param project 项目
     * @return 配置验证结果，包含是否有修改和修改详情
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public ConfigValidationResult validateAndCleanConfig(Project project) {
        if (project == null) {
            return new ConfigValidationResult(false, "项目为空，无法验证配置");
        }

        StringBuilder changes = new StringBuilder();
        boolean hasChanges = false;

        // 验证排除的父类列表
        String excludedClasses = myState.excludedParentClasses;
        if (excludedClasses != null && !excludedClasses.isEmpty()) {
            java.util.List<String> validClasses = new java.util.ArrayList<>();
            java.util.List<String> invalidClasses = new java.util.ArrayList<>();

            String[] classNames = excludedClasses.split(",");
            for (String className : classNames) {
                String trimmed = className.trim();
                if (!trimmed.isEmpty()) {
                    if (validateClass(project, trimmed)) {
                        validClasses.add(trimmed);
                    } else {
                        invalidClasses.add(trimmed);
                        hasChanges = true;
                    }
                }
            }

            // 如果有无效的类，更新配置
            if (!invalidClasses.isEmpty()) {
                changes.append("移除不存在的类: ").append(String.join(", ", invalidClasses)).append("\n");
                myState.excludedParentClasses = String.join(",", validClasses);

                // 同时移除这些类的字段配置
                for (String invalidClass : invalidClasses) {
                    removeExcludedFieldsForClass(invalidClass);
                }
            }

            // 验证有效类的字段配置
            for (String validClass : validClasses) {
                String fieldValidation = validateAndCleanFieldsForClass(project, validClass);
                if (fieldValidation != null && !fieldValidation.isEmpty()) {
                    changes.append(fieldValidation);
                    hasChanges = true;
                }
            }
        }

        return new ConfigValidationResult(hasChanges, changes.toString());
    }

    /**
     * 验证单个类是否存在
     *
     * @param project   项目
     * @param className 类名（全限定名）
     * @return 类是否存在
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public boolean validateClass(Project project, String className) {
        if (project == null || className == null || className.isEmpty()) {
            return false;
        }
        try {
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));
            return psiClass != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证并清理指定类的字段配置
     * 检查配置的字段是否仍然存在于类及其父类中
     *
     * @param project   项目
     * @param className 类名（全限定名）
     * @return 修改说明，如果没有修改返回null
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String validateAndCleanFieldsForClass(Project project, String className) {
        if (project == null || className == null) {
            return null;
        }

        java.util.Set<String> excludedFields = getExcludedFieldsForClass(className);
        // 如果是排除所有字段（null），不需要验证具体字段
        if (excludedFields == null) {
            return null;
        }

        // 获取类的所有有效字段
        java.util.Set<String> validFieldNames = getValidFieldNames(project, className);
        if (validFieldNames == null) {
            return null;
        }

        // 检查配置的字段是否有效
        java.util.Set<String> validExcludedFields = new java.util.LinkedHashSet<>();
        java.util.List<String> invalidFields = new java.util.ArrayList<>();

        for (String fieldName : excludedFields) {
            if (validFieldNames.contains(fieldName)) {
                validExcludedFields.add(fieldName);
            } else {
                invalidFields.add(fieldName);
            }
        }

        // 如果有无效的字段，更新配置
        if (!invalidFields.isEmpty()) {
            updateExcludedFieldsForClass(className, validExcludedFields);
            return "类 " + getSimpleClassName(className) + " 移除不存在的字段: " +
                    String.join(", ", invalidFields) + "\n";
        }

        return null;
    }

    /**
     * 获取类及其父类的所有有效字段名
     *
     * @param project   项目
     * @param className 类名（全限定名）
     * @return 字段名集合，如果类不存在返回null
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private java.util.Set<String> getValidFieldNames(Project project, String className) {
        try {
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return null;
            }

            java.util.Set<String> fieldNames = new java.util.LinkedHashSet<>();
            collectFieldNames(psiClass, fieldNames, new java.util.HashSet<>());
            return fieldNames;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 递归收集类及其父类的字段名
     *
     * @param psiClass       类
     * @param fieldNames     字段名集合
     * @param visitedClasses 已访问的类（防止循环继承）
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void collectFieldNames(PsiClass psiClass, java.util.Set<String> fieldNames,
            java.util.Set<String> visitedClasses) {
        if (psiClass == null) {
            return;
        }

        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null || visitedClasses.contains(qualifiedName)) {
            return;
        }
        if (qualifiedName.startsWith("java.") || qualifiedName.startsWith("javax.")) {
            return;
        }

        visitedClasses.add(qualifiedName);

        // 收集当前类的字段
        for (PsiField field : psiClass.getFields()) {
            // 跳过静态字段
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            fieldNames.add(field.getName());
        }

        // 递归处理父类
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            collectFieldNames(superClass, fieldNames, visitedClasses);
        }
    }

    /**
     * 获取类及其父类的所有字段信息（包含类型和描述）
     * 用于配置界面实时显示字段
     *
     * @param project   项目
     * @param className 类名（全限定名）
     * @return 字段信息列表，如果类不存在返回空列表
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public java.util.List<FieldInfo> getFieldsForClass(Project project, String className) {
        java.util.List<FieldInfo> fields = new java.util.ArrayList<>();
        if (project == null || className == null) {
            return fields;
        }

        try {
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return fields;
            }

            collectFieldsInfo(psiClass, fields, new java.util.HashSet<>());
        } catch (Exception e) {
            // 忽略异常，返回空列表
        }

        return fields;
    }

    /**
     * 递归收集类及其父类的字段信息
     *
     * @param psiClass       类
     * @param fields         字段信息列表
     * @param visitedClasses 已访问的类（防止循环继承）
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void collectFieldsInfo(PsiClass psiClass, java.util.List<FieldInfo> fields,
            java.util.Set<String> visitedClasses) {
        if (psiClass == null) {
            return;
        }

        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null || visitedClasses.contains(qualifiedName)) {
            return;
        }
        if (qualifiedName.startsWith("java.") || qualifiedName.startsWith("javax.")) {
            return;
        }

        visitedClasses.add(qualifiedName);

        // 收集当前类的字段
        for (PsiField field : psiClass.getFields()) {
            // 跳过静态字段
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            FieldInfo info = new FieldInfo();
            info.name = field.getName();
            info.type = field.getType().getPresentableText();
            info.fromClass = psiClass.getName();
            fields.add(info);
        }

        // 递归处理父类
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            collectFieldsInfo(superClass, fields, visitedClasses);
        }
    }

    /**
     * 配置验证结果类
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public static class ConfigValidationResult {
        /** 是否有变更 */
        public final boolean hasChanges;
        /** 变更详情 */
        public final String changeDetails;

        public ConfigValidationResult(boolean hasChanges, String changeDetails) {
            this.hasChanges = hasChanges;
            this.changeDetails = changeDetails;
        }
    }

    /**
     * 字段信息类
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public static class FieldInfo {
        /** 字段名 */
        public String name;
        /** 字段类型 */
        public String type;
        /** 来源类名 */
        public String fromClass;
    }
}
