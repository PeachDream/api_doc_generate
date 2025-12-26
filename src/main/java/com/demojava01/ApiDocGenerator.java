package com.demojava01;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;

import java.util.*;

/**
 * API文档生成器
 * 负责解析Spring MVC Controller方法的参数和返回值，生成Markdown格式的文档
 *
 * @author peach
 * @since 2025/12/25 | V1.0.0
 */
public class ApiDocGenerator {

    /**
     * 基本类型映射表
     */
    private static final Map<String, String> PRIMITIVE_TYPE_MAP = new HashMap<>();

    /**
     * 基本类型默认值映射表
     */
    private static final Map<String, String> PRIMITIVE_DEFAULT_VALUE_MAP = new HashMap<>();

    /*
     * 静态初始化块
     * 初始化基本类型映射表和默认值映射表
     * - PRIMITIVE_TYPE_MAP: 将Java类型映射为文档友好的类型名称
     * - PRIMITIVE_DEFAULT_VALUE_MAP: 为各类型提供JSON示例的默认值
     */
    static {
        // 类型映射：Java类型 -> 文档显示的类型名称
        PRIMITIVE_TYPE_MAP.put("int", "Integer");
        PRIMITIVE_TYPE_MAP.put("long", "Long");
        PRIMITIVE_TYPE_MAP.put("double", "Double");
        PRIMITIVE_TYPE_MAP.put("float", "Float");
        PRIMITIVE_TYPE_MAP.put("boolean", "Boolean");
        PRIMITIVE_TYPE_MAP.put("byte", "Byte");
        PRIMITIVE_TYPE_MAP.put("short", "Short");
        PRIMITIVE_TYPE_MAP.put("char", "Character");
        PRIMITIVE_TYPE_MAP.put("java.lang.Integer", "Integer");
        PRIMITIVE_TYPE_MAP.put("java.lang.Long", "Long");
        PRIMITIVE_TYPE_MAP.put("java.lang.Double", "Double");
        PRIMITIVE_TYPE_MAP.put("java.lang.Float", "Float");
        PRIMITIVE_TYPE_MAP.put("java.lang.Boolean", "Boolean");
        PRIMITIVE_TYPE_MAP.put("java.lang.Byte", "Byte");
        PRIMITIVE_TYPE_MAP.put("java.lang.Short", "Short");
        PRIMITIVE_TYPE_MAP.put("java.lang.Character", "Character");
        PRIMITIVE_TYPE_MAP.put("java.lang.String", "String");
        PRIMITIVE_TYPE_MAP.put("java.util.Date", "DateTime");
        PRIMITIVE_TYPE_MAP.put("java.time.LocalDateTime", "DateTime");
        PRIMITIVE_TYPE_MAP.put("java.time.LocalDate", "Date");
        PRIMITIVE_TYPE_MAP.put("java.time.LocalTime", "Time");
        PRIMITIVE_TYPE_MAP.put("java.math.BigDecimal", "BigDecimal");
        PRIMITIVE_TYPE_MAP.put("java.math.BigInteger", "BigInteger");

        // 默认值映射
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Integer", "0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Long", "0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Double", "0.0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Float", "0.0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Boolean", "false");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Byte", "0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Short", "0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Character", "\"\"");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("String", "\"String\"");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("DateTime", "\"DateTime\"");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Date", "\"Date\"");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("Time", "\"Time\"");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("BigDecimal", "0");
        PRIMITIVE_DEFAULT_VALUE_MAP.put("BigInteger", "0");
    }

    /**
     * 获取方法标题
     * 优先从JavaDoc注释获取，如果没有则使用方法名
     *
     * @param method 方法
     * @return 方法标题
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getMethodTitle(PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            PsiElement[] children = docComment.getDescriptionElements();
            StringBuilder sb = new StringBuilder();
            for (PsiElement child : children) {
                String text = child.getText().trim();
                if (!text.isEmpty()) {
                    sb.append(text);
                }
            }
            String description = sb.toString().trim();
            if (!description.isEmpty()) {
                // 取第一行作为标题
                String[] lines = description.split("\n");
                return lines[0].trim();
            }
        }
        return method.getName();
    }

    /**
     * 获取方法的请求路径
     *
     * @param method 方法
     * @return 请求路径
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getMethodRequestPath(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("Mapping")) {
                return extractPathFromAnnotation(annotation);
            }
        }
        return "";
    }

    /**
     * 获取HTTP请求方法类型
     *
     * @param method 方法
     * @return HTTP方法类型
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getHttpMethod(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null) {
                if (qualifiedName.contains("GetMapping")) {
                    return "GET";
                } else if (qualifiedName.contains("PostMapping")) {
                    return "POST";
                } else if (qualifiedName.contains("PutMapping")) {
                    return "PUT";
                } else if (qualifiedName.contains("DeleteMapping")) {
                    return "DELETE";
                } else if (qualifiedName.contains("PatchMapping")) {
                    return "PATCH";
                } else if (qualifiedName.contains("RequestMapping")) {
                    // 从method属性获取
                    PsiAnnotationMemberValue methodValue = annotation.findAttributeValue("method");
                    if (methodValue != null) {
                        String resolved = parseRequestMappingMethods(methodValue.getText());
                        if (!resolved.isEmpty()) {
                            return resolved;
                        }
                    }
                    return "GET/POST";
                }
            }
        }
        return "GET/POST";
    }

    private String parseRequestMappingMethods(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = text.replace("{", "").replace("}", "");
        String[] parts = cleaned.split(",");
        Set<String> methods = new LinkedHashSet<>();
        for (String part : parts) {
            String method = part.trim();
            if (method.isEmpty()) {
                continue;
            }
            int lastDot = method.lastIndexOf('.');
            if (lastDot >= 0) {
                method = method.substring(lastDot + 1);
            }
            method = method.replaceAll("[^A-Za-z]", "").toUpperCase();
            if (isHttpMethodToken(method)) {
                methods.add(method);
            }
        }
        return String.join("/", methods);
    }

    private boolean isHttpMethodToken(String method) {
        return "GET".equals(method)
                || "POST".equals(method)
                || "PUT".equals(method)
                || "DELETE".equals(method)
                || "PATCH".equals(method)
                || "HEAD".equals(method)
                || "OPTIONS".equals(method);
    }

    /**
     * 获取请求内容类型
     *
     * @param method 方法
     * @return 内容类型
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String getContentType(PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            PsiAnnotation[] annotations = parameter.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null) {
                    if (qualifiedName.contains("RequestBody")) {
                        return "JSON";
                    }
                }
            }
        }
        return "FormData";
    }

    /**
     * 生成请求参数表格
     *
     * @param method 方法
     * @return 表格Markdown
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String generateRequestParamsTable(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        List<FieldInfo> fields = extractRequestParams(method);
        for (FieldInfo field : fields) {
            sb.append("|").append(field.prefix).append(field.name)
                    .append("|").append(field.required ? "是" : "否")
                    .append("|").append(field.type)
                    .append("|").append(field.description)
                    .append("|\n");
        }
        return sb.toString();
    }

    /**
     * 生成请求参数JSON格式
     *
     * @param method 方法
     * @return JSON字符串
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String generateRequestParamsJson(PsiMethod method) {
        List<FieldInfo> fields = extractRequestParams(method);
        return generateJsonFromFields(fields, 0);
    }

    /**
     * 生成返回参数表格
     *
     * @param method 方法
     * @return 表格Markdown
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String generateResponseParamsTable(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        List<FieldInfo> fields = extractResponseParams(method);
        for (FieldInfo field : fields) {
            sb.append("|").append(field.prefix).append(field.name)
                    .append("|").append(field.required ? "是" : "否")
                    .append("|").append(field.type)
                    .append("|").append(field.description)
                    .append("|\n");
        }
        return sb.toString();
    }

    /**
     * 生成返回参数JSON格式
     *
     * @param method 方法
     * @return JSON字符串
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public String generateResponseParamsJson(PsiMethod method) {
        List<FieldInfo> fields = extractResponseParams(method);
        return generateJsonFromFields(fields, 0);
    }

    /**
     * 提取请求参数
     *
     * @param method 方法
     * @return 字段信息列表
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private List<FieldInfo> extractRequestParams(PsiMethod method) {
        List<FieldInfo> fields = new ArrayList<>();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        Map<String, String> paramDocs = getParamDocs(method);

        for (PsiParameter parameter : parameters) {
            PsiType type = parameter.getType();
            String paramName = parameter.getName();

            // 跳过特殊类型参数（如HttpServletRequest等）
            if (isSpecialParam(type)) {
                continue;
            }

            // 检查注解
            boolean isRequestBody = hasAnnotation(parameter, "RequestBody");
            boolean isRequestParam = hasAnnotation(parameter, "RequestParam");
            boolean isPathVariable = hasAnnotation(parameter, "PathVariable");
            boolean isRequestAttribute = hasAnnotation(parameter, "RequestAttribute");

            // 检查是否是POJO类型
            boolean isPojoType = isPojo(type);

            // 如果是POJO类型（无论是@RequestBody、@RequestAttribute还是无注解），都解析其字段
            if (isPojoType) {
                // 复杂类型，需要解析类的字段
                PsiClass paramClass = getPsiClass(type);
                if (paramClass != null) {
                    List<FieldInfo> classFields = extractFieldsFromClass(paramClass, "", new HashSet<>());
                    if (!classFields.isEmpty()) {
                        fields.addAll(classFields);
                    }
                }
            } else if (isRequestBody) {
                // @RequestBody 的简单类型参数
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.name = paramName;
                fieldInfo.type = getSimpleTypeName(type);
                fieldInfo.required = isRequired(parameter);
                fieldInfo.description = paramDocs.getOrDefault(paramName, "");
                fieldInfo.prefix = "";
                fields.add(fieldInfo);
            } else if (isRequestParam || isPathVariable) {
                // 显式标注的基本类型参数
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.name = getParamName(parameter, paramName);
                fieldInfo.type = getSimpleTypeName(type);
                fieldInfo.required = isRequired(parameter);
                fieldInfo.description = paramDocs.getOrDefault(paramName, "");
                fieldInfo.prefix = "";
                fields.add(fieldInfo);
            } else if (!isRequestAttribute) {
                // 未标注的基本类型参数（排除@RequestAttribute的基本类型，因为通常是鉴权信息）
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.name = paramName;
                fieldInfo.type = getSimpleTypeName(type);
                fieldInfo.required = false;
                fieldInfo.description = paramDocs.getOrDefault(paramName, "");
                fieldInfo.prefix = "";
                fields.add(fieldInfo);
            }
        }

        return fields;
    }

    /**
     * 提取返回参数
     *
     * @param method 方法
     * @return 字段信息列表
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private List<FieldInfo> extractResponseParams(PsiMethod method) {
        List<FieldInfo> fields = new ArrayList<>();
        PsiType returnType = method.getReturnType();

        if (returnType == null || returnType.equalsToText("void")) {
            return fields;
        }

        // 处理返回类型，可能是 Result<Page<XXX>> 这样的嵌套泛型
        extractFieldsFromType(returnType, "", fields, new HashSet<>());

        return fields;
    }

    /**
     * 从类型中提取字段信息（支持泛型）
     *
     * @param type           类型
     * @param prefix         前缀
     * @param fields         字段列表
     * @param visitedClasses 已访问的类
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private void extractFieldsFromType(PsiType type, String prefix, List<FieldInfo> fields,
            Set<String> visitedClasses) {
        if (type == null) {
            return;
        }

        // 处理泛型类型
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
            PsiClass psiClass = resolveResult.getElement();
            PsiSubstitutor substitutor = resolveResult.getSubstitutor();

            if (psiClass == null) {
                return;
            }

            String className = psiClass.getQualifiedName();
            if (className == null) {
                return;
            }

            // 检查是否是常见的包装类（如Result, Response, Page等）
            if (isWrapperClass(className)) {
                // 提取包装类自身字段，并在字段位置展开实际泛型
                fields.addAll(
                        extractFieldsFromClass(psiClass, prefix, new HashSet<>(visitedClasses), substitutor, null));
            } else if (isCollection(type)) {
                // 集合类型，处理泛型参数
                PsiType[] typeParameters = classType.getParameters();
                if (typeParameters.length > 0) {
                    PsiType elementType = substitutor.substitute(typeParameters[0]);
                    extractFieldsFromType(elementType != null ? elementType : typeParameters[0], prefix, fields,
                            visitedClasses);
                }
            } else {
                // 普通类型，提取字段
                fields.addAll(extractFieldsFromClass(psiClass, prefix, visitedClasses, substitutor, null));
            }
        }
    }

    /**
     * 检查是否是常见的包装类
     *
     * @param className 类名
     * @return 是否是包装类
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isWrapperClass(String className) {
        // 常见的响应包装类
        return className.contains("Result") ||
                className.contains("Response") ||
                className.contains("Page") ||
                className.contains("PageResult") ||
                className.contains("PageInfo") ||
                className.contains("ApiResult") ||
                className.contains("CommonResult") ||
                className.contains("RestResult") ||
                className.contains("BaseResult");
    }

    /**
     * 从类中提取字段信息（包括父类）
     * 排序规则：子类字段在前，父类字段在后（依次向上）
     *
     * @param psiClass       类
     * @param prefix         前缀（用于嵌套对象）
     * @param visitedClasses 已访问的类（防止循环引用）
     * @return 字段信息列表
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private List<FieldInfo> extractFieldsFromClass(PsiClass psiClass, String prefix, Set<String> visitedClasses) {
        return extractFieldsFromClass(psiClass, prefix, visitedClasses, PsiSubstitutor.EMPTY, null);
    }

    /**
     * 从类中提取字段信息（包括父类）
     * 排序规则：子类字段在前，父类字段在后（依次向上）
     *
     * @param psiClass                类
     * @param prefix                  前缀（用于嵌套对象）
     * @param visitedClasses          已访问的类（防止循环引用）
     * @param substitutor             类型替换器（用于泛型）
     * @param inheritedExcludedFields 从子类继承的排除字段（子类配置的排除字段在父类中也生效）
     * @return 字段信息列表
     * @author peach
     * @since 2025/12/26 | V3.1.5
     */
    private List<FieldInfo> extractFieldsFromClass(PsiClass psiClass, String prefix, Set<String> visitedClasses,
            PsiSubstitutor substitutor, java.util.Set<String> inheritedExcludedFields) {
        List<FieldInfo> fields = new ArrayList<>();

        if (psiClass == null) {
            return fields;
        }

        String className = psiClass.getQualifiedName();
        if (className == null || visitedClasses.contains(className)) {
            return fields;
        }

        // 跳过常见的基础类
        if (className.startsWith("java.") && !className.startsWith("java.util.")) {
            return fields;
        }

        // 检查是否在排除列表中（整个类被排除）
        ApiDocSettings settings = ApiDocSettings.getInstance();
        // 获取当前类被排除的字段（如果类在排除列表中）
        java.util.Set<String> excludedFields = null;
        boolean classExcluded = settings.isClassExcluded(className);
        if (classExcluded) {
            excludedFields = settings.getExcludedFieldsForClass(className);
            // 如果excludedFields为null，表示排除所有字段，直接返回
            if (excludedFields == null) {
                return fields;
            }
        }

        // 合并子类传递过来的排除字段
        // 这样子类配置的排除字段在父类中也会生效
        java.util.Set<String> effectiveExcludedFields = new java.util.HashSet<>();
        if (excludedFields != null) {
            effectiveExcludedFields.addAll(excludedFields);
        }
        if (inheritedExcludedFields != null) {
            effectiveExcludedFields.addAll(inheritedExcludedFields);
        }

        visitedClasses.add(className);

        PsiSubstitutor effectiveSubstitutor = substitutor != null ? substitutor : PsiSubstitutor.EMPTY;

        // 先处理当前类（子类）的字段，确保子类字段排在前面
        for (PsiField field : psiClass.getFields()) {
            // 跳过静态字段和常量
            if (field.hasModifierProperty(PsiModifier.STATIC) ||
                    field.hasModifierProperty(PsiModifier.FINAL)) {
                continue;
            }

            String fieldName = field.getName();

            // 检查字段是否被排除（包括从子类继承的排除配置）
            if (!effectiveExcludedFields.isEmpty() && effectiveExcludedFields.contains(fieldName)) {
                continue; // 跳过被排除的字段
            }

            PsiType fieldType = field.getType();
            PsiType resolvedType = effectiveSubstitutor.substitute(fieldType);
            if (resolvedType == null) {
                resolvedType = fieldType;
            }

            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.name = fieldName;
            fieldInfo.prefix = prefix;
            fieldInfo.type = getSimpleTypeName(resolvedType);
            fieldInfo.required = isFieldRequired(field);
            fieldInfo.description = getFieldDescription(field);
            fields.add(fieldInfo);

            // 处理嵌套对象
            String newPrefix = prefix + "--";
            if (isCollection(resolvedType) && resolvedType instanceof PsiClassType) {
                PsiType[] typeParameters = ((PsiClassType) resolvedType).getParameters();
                if (typeParameters.length > 0) {
                    PsiType elementType = typeParameters[0];
                    PsiClass elementClass = getPsiClass(elementType);
                    if (elementClass != null && !isPrimitiveOrWrapper(elementClass.getQualifiedName())) {
                        extractFieldsFromType(elementType, newPrefix, fields, new HashSet<>(visitedClasses));
                    }
                }
            } else if (isPojo(resolvedType)) {
                extractFieldsFromType(resolvedType, newPrefix, fields, new HashSet<>(visitedClasses));
            }
        }

        // 然后处理父类字段，父类字段排在子类字段后面
        // 将当前类配置的排除字段传递给父类，使得子类配置的排除在父类中也生效
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null && !isObjectClass(superClass)) {
            String superClassName = superClass.getQualifiedName();
            if (superClassName != null) {
                // 将当前有效的排除字段传递给父类
                fields.addAll(extractFieldsFromClass(superClass, prefix, new HashSet<>(visitedClasses),
                        effectiveSubstitutor, effectiveExcludedFields));
            }
        }

        return fields;
    }

    /**
     * 从字段信息列表生成JSON
     *
     * @param fields 字段信息列表
     * @param depth  当前深度
     * @return JSON字符串
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String generateJsonFromFields(List<FieldInfo> fields, int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "   ".repeat(depth);
        String childIndent = "   ".repeat(depth + 1);

        sb.append("{\n");

        // 按层级分组
        Map<Integer, List<FieldInfo>> levelMap = new LinkedHashMap<>();
        for (FieldInfo field : fields) {
            int level = field.prefix.length() / 2; // "--" 长度为2
            levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(field);
        }

        // 只处理当前层级
        List<FieldInfo> currentLevelFields = levelMap.getOrDefault(depth, new ArrayList<>());

        for (int i = 0; i < currentLevelFields.size(); i++) {
            FieldInfo field = currentLevelFields.get(i);
            String value = getJsonValueWithComment(field);

            // 检查是否有子字段
            List<FieldInfo> childFields = getChildFieldsFromAll(fields, field);

            if (field.type.equals("List") || field.type.equals("Array")) {
                // 数组类型
                if (!childFields.isEmpty()) {
                    sb.append(childIndent).append("\"").append(field.name).append("\" : [");
                    sb.append(generateJsonFromChildFieldsRecursive(fields, childFields, depth + 1));
                    sb.append("]");
                } else {
                    sb.append(childIndent).append("\"").append(field.name).append("\" : []");
                }
            } else if (!childFields.isEmpty()) {
                // 对象类型，使用递归方法处理嵌套
                sb.append(childIndent).append("\"").append(field.name).append("\" : ");
                sb.append(generateJsonFromChildFieldsRecursive(fields, childFields, depth + 1));
            } else {
                // 基本类型，添加注释
                sb.append(childIndent).append("\"").append(field.name).append("\" : ").append(value);
            }

            if (i < currentLevelFields.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(indent).append("}");

        return sb.toString();
    }

    /**
     * 从子字段生成JSON（递归处理所有嵌套字段）
     *
     * @param allFields   所有字段列表（用于查找嵌套子字段）
     * @param childFields 当前层级的子字段列表
     * @param depth       当前深度
     * @return JSON字符串
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String generateJsonFromChildFieldsRecursive(List<FieldInfo> allFields, List<FieldInfo> childFields,
            int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "   ".repeat(depth);
        String childIndent = "   ".repeat(depth + 1);

        sb.append("{\n");

        for (int i = 0; i < childFields.size(); i++) {
            FieldInfo field = childFields.get(i);
            String value = getJsonValueWithComment(field);

            // 检查是否有更深层的子字段
            List<FieldInfo> nestedFields = getChildFieldsFromAll(allFields, field);

            if (field.type.equals("List") || field.type.equals("Array")) {
                // 数组类型
                if (!nestedFields.isEmpty()) {
                    sb.append(childIndent).append("\"").append(field.name).append("\" : [");
                    sb.append(generateJsonFromChildFieldsRecursive(allFields, nestedFields, depth + 1));
                    sb.append("]");
                } else {
                    sb.append(childIndent).append("\"").append(field.name).append("\" : []");
                }
            } else if (!nestedFields.isEmpty()) {
                // 对象类型，有嵌套子字段
                sb.append(childIndent).append("\"").append(field.name).append("\" : ");
                sb.append(generateJsonFromChildFieldsRecursive(allFields, nestedFields, depth + 1));
            } else {
                // 基本类型，添加注释
                sb.append(childIndent).append("\"").append(field.name).append("\" : ").append(value);
            }

            if (i < childFields.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(indent).append("}");

        return sb.toString();
    }

    /**
     * 从所有字段中获取指定父字段的直接子字段
     *
     * @param allFields   所有字段
     * @param parentField 父字段
     * @return 直接子字段列表
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private List<FieldInfo> getChildFieldsFromAll(List<FieldInfo> allFields, FieldInfo parentField) {
        List<FieldInfo> childFields = new ArrayList<>();
        String expectedPrefix = parentField.prefix + "--";
        int parentLevel = parentField.prefix.length() / 2;
        int expectedLevel = parentLevel + 1;

        boolean foundParent = false;
        for (FieldInfo field : allFields) {
            if (field == parentField) {
                foundParent = true;
                continue;
            }
            if (foundParent) {
                int fieldLevel = field.prefix.length() / 2;
                if (fieldLevel == expectedLevel && field.prefix.equals(expectedPrefix)) {
                    childFields.add(field);
                } else if (fieldLevel <= parentLevel) {
                    // 已经回到父级或更高层级，停止
                    break;
                }
            }
        }

        return childFields;
    }

    /**
     * 获取默认值
     *
     * @param type 类型名称
     * @return 默认值
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getDefaultValue(String type) {
        return PRIMITIVE_DEFAULT_VALUE_MAP.getOrDefault(type, "0");
    }

    /**
     * 获取带注释的JSON值
     * 格式为: "类型 //描述"
     *
     * @param field 字段信息
     * @return 带注释的JSON值字符串
     * @author peach
     * @since 2025/12/26 | V3.1.5
     */
    private String getJsonValueWithComment(FieldInfo field) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(field.type).append("\"");

        // 如果有描述，添加注释
        if (field.description != null && !field.description.trim().isEmpty()) {
            sb.append(" //").append(field.description.trim());
        }

        return sb.toString();
    }

    /**
     * 从注解中提取路径
     *
     * @param annotation 注解
     * @return 路径
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String extractPathFromAnnotation(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("path");
        }
        if (value != null) {
            String text = value.getText();
            if (text.startsWith("{")) {
                text = text.substring(1, text.length() - 1);
                if (text.contains(",")) {
                    text = text.split(",")[0].trim();
                }
            }
            text = text.replace("\"", "").trim();
            return text;
        }
        return "";
    }

    /**
     * 获取方法参数的JavaDoc注释
     *
     * @param method 方法
     * @return 参数名到描述的映射
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private Map<String, String> getParamDocs(PsiMethod method) {
        Map<String, String> paramDocs = new HashMap<>();
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            PsiDocTag[] tags = docComment.findTagsByName("param");
            for (PsiDocTag tag : tags) {
                PsiElement[] dataElements = tag.getDataElements();
                if (dataElements.length >= 1) {
                    String paramName = dataElements[0].getText().trim();
                    StringBuilder description = new StringBuilder();
                    for (int i = 1; i < dataElements.length; i++) {
                        description.append(dataElements[i].getText().trim()).append(" ");
                    }
                    paramDocs.put(paramName, description.toString().trim());
                }
            }
        }
        return paramDocs;
    }

    /**
     * 获取字段描述
     * 优先从注解获取，其次从JavaDoc获取
     *
     * @param field 字段
     * @return 描述
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getFieldDescription(PsiField field) {
        // 检查常用注解
        String[] annotationNames = { "ApiModelProperty", "Schema", "JsonProperty" };
        for (String annotationName : annotationNames) {
            PsiAnnotation annotation = findAnnotation(field, annotationName);
            if (annotation != null) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value == null) {
                    value = annotation.findAttributeValue("description");
                }
                if (value != null) {
                    String text = value.getText().replace("\"", "").trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }

        // 从JavaDoc获取
        PsiDocComment docComment = field.getDocComment();
        if (docComment != null) {
            PsiElement[] children = docComment.getDescriptionElements();
            StringBuilder sb = new StringBuilder();
            for (PsiElement child : children) {
                String text = child.getText().trim();
                if (!text.isEmpty()) {
                    sb.append(text).append(" ");
                }
            }
            return sb.toString().trim();
        }

        // 从行尾注释获取
        PsiElement nextSibling = field.getNextSibling();
        while (nextSibling != null) {
            if (nextSibling instanceof PsiComment) {
                String commentText = nextSibling.getText();
                if (commentText.startsWith("//")) {
                    return commentText.substring(2).trim();
                }
            } else if (!(nextSibling instanceof PsiWhiteSpace)) {
                break;
            }
            nextSibling = nextSibling.getNextSibling();
        }

        return "";
    }

    /**
     * 检查字段是否必填
     *
     * @param field 字段
     * @return 是否必填
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isFieldRequired(PsiField field) {
        // 检查JSR-303/380注解
        String[] requiredAnnotations = { "NotNull", "NotEmpty", "NotBlank" };
        for (String annotationName : requiredAnnotations) {
            if (findAnnotation(field, annotationName) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查参数是否必填
     *
     * @param parameter 参数
     * @return 是否必填
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isRequired(PsiParameter parameter) {
        // 检查@RequestParam的required属性
        PsiAnnotation requestParam = findAnnotation(parameter, "RequestParam");
        if (requestParam != null) {
            PsiAnnotationMemberValue required = requestParam.findAttributeValue("required");
            if (required != null) {
                String text = required.getText().trim();
                if ("false".equalsIgnoreCase(text) || text.endsWith(".FALSE")) {
                    return false;
                }
                if ("true".equalsIgnoreCase(text) || text.endsWith(".TRUE")) {
                    return true;
                }
            }
            return true; // 默认true
        }

        // 检查@RequestBody的required属性
        PsiAnnotation requestBody = findAnnotation(parameter, "RequestBody");
        if (requestBody != null) {
            PsiAnnotationMemberValue required = requestBody.findAttributeValue("required");
            if (required != null) {
                String text = required.getText().trim();
                if ("false".equalsIgnoreCase(text) || text.endsWith(".FALSE")) {
                    return false;
                }
                if ("true".equalsIgnoreCase(text) || text.endsWith(".TRUE")) {
                    return true;
                }
            }
            return true; // 默认true
        }

        // 检查JSR-303注解
        String[] requiredAnnotations = { "NotNull", "NotEmpty", "NotBlank" };
        for (String annotationName : requiredAnnotations) {
            if (findAnnotation(parameter, annotationName) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取参数名称
     *
     * @param parameter   参数
     * @param defaultName 默认名称
     * @return 参数名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getParamName(PsiParameter parameter, String defaultName) {
        PsiAnnotation requestParam = findAnnotation(parameter, "RequestParam");
        if (requestParam != null) {
            PsiAnnotationMemberValue value = requestParam.findAttributeValue("value");
            if (value == null) {
                value = requestParam.findAttributeValue("name");
            }
            if (value != null) {
                String text = value.getText().replace("\"", "").trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return defaultName;
    }

    /**
     * 获取简化的类型名称
     *
     * @param type 类型
     * @return 简化名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getSimpleTypeName(PsiType type) {
        String typeName = type.getCanonicalText();

        // 检查映射表
        if (PRIMITIVE_TYPE_MAP.containsKey(typeName)) {
            return PRIMITIVE_TYPE_MAP.get(typeName);
        }

        // 处理集合类型
        if (isCollection(type)) {
            return "List";
        }

        // 处理Map类型
        if (typeName.startsWith("java.util.Map") || typeName.contains("Map<")) {
            return "Object";
        }

        // 处理数组
        if (type instanceof PsiArrayType) {
            return "Array";
        }

        // 获取简单类名
        int lastDot = typeName.lastIndexOf(".");
        if (lastDot > 0) {
            typeName = typeName.substring(lastDot + 1);
        }

        // 去除泛型参数
        int genericStart = typeName.indexOf("<");
        if (genericStart > 0) {
            typeName = typeName.substring(0, genericStart);
        }

        return typeName;
    }

    /**
     * 检查是否是集合类型
     *
     * @param type 类型
     * @return 是否是集合
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isCollection(PsiType type) {
        String typeName = type.getCanonicalText();
        return typeName.startsWith("java.util.List") ||
                typeName.startsWith("java.util.Set") ||
                typeName.startsWith("java.util.Collection") ||
                typeName.contains("List<") ||
                typeName.contains("Set<") ||
                typeName.contains("Collection<");
    }

    /**
     * 获取集合元素类型的类
     *
     * @param type 集合类型
     * @return 元素类型的类
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private PsiClass getCollectionElementClass(PsiType type) {
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiType[] parameters = classType.getParameters();
            if (parameters.length > 0) {
                return getPsiClass(parameters[0]);
            }
        }
        return null;
    }

    /**
     * 检查是否是POJO类型
     *
     * @param type 类型
     * @return 是否是POJO
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isPojo(PsiType type) {
        String typeName = type.getCanonicalText();

        // 基本类型和包装类型不是POJO
        if (PRIMITIVE_TYPE_MAP.containsKey(typeName)) {
            return false;
        }

        // 集合和Map不是简单POJO
        if (isCollection(type) || typeName.contains("Map")) {
            return false;
        }

        // 数组不是POJO
        if (type instanceof PsiArrayType) {
            return false;
        }

        // java.lang包下的类不是POJO
        if (typeName.startsWith("java.lang.")) {
            return false;
        }

        return type instanceof PsiClassType;
    }

    /**
     * 检查是否是基本类型或包装类型
     *
     * @param qualifiedName 全限定名
     * @return 是否是基本类型
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isPrimitiveOrWrapper(String qualifiedName) {
        return qualifiedName == null || PRIMITIVE_TYPE_MAP.containsKey(qualifiedName);
    }

    /**
     * 检查是否是Object类
     *
     * @param psiClass 类
     * @return 是否是Object类
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isObjectClass(PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        return "java.lang.Object".equals(qualifiedName);
    }

    /**
     * 检查是否是特殊参数（如HttpServletRequest等）
     *
     * @param type 类型
     * @return 是否是特殊参数
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isSpecialParam(PsiType type) {
        String typeName = type.getCanonicalText();
        // 使用更精确的匹配，避免误判用户自定义的类
        return typeName.equals("javax.servlet.http.HttpServletRequest") ||
                typeName.equals("jakarta.servlet.http.HttpServletRequest") ||
                typeName.equals("javax.servlet.http.HttpServletResponse") ||
                typeName.equals("jakarta.servlet.http.HttpServletResponse") ||
                typeName.equals("javax.servlet.http.HttpSession") ||
                typeName.equals("jakarta.servlet.http.HttpSession") ||
                typeName.endsWith("BindingResult") ||
                typeName.equals("org.springframework.ui.Model") ||
                typeName.equals("org.springframework.ui.ModelMap") ||
                typeName.endsWith("RedirectAttributes") ||
                typeName.contains("MultipartFile") ||
                typeName.startsWith("org.springframework.web.") ||
                typeName.equals("org.springframework.validation.Errors");
    }

    /**
     * 获取类型对应的PsiClass
     *
     * @param type 类型
     * @return PsiClass
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private PsiClass getPsiClass(PsiType type) {
        if (type instanceof PsiClassType) {
            return ((PsiClassType) type).resolve();
        }
        return null;
    }

    /**
     * 检查元素是否有指定注解
     *
     * @param element        元素
     * @param annotationName 注解名称
     * @return 是否有注解
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean hasAnnotation(PsiModifierListOwner element, String annotationName) {
        return findAnnotation(element, annotationName) != null;
    }

    /**
     * 查找指定注解
     *
     * @param element        元素
     * @param annotationName 注解名称（简单名称）
     * @return 注解，如果未找到返回null
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private PsiAnnotation findAnnotation(PsiModifierListOwner element, String annotationName) {
        PsiModifierList modifierList = element.getModifierList();
        if (modifierList == null) {
            return null;
        }
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.endsWith(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 字段信息类
     *
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public static class FieldInfo {
        /** 字段名称 */
        public String name;
        /** 字段类型 */
        public String type;
        /** 是否必填 */
        public boolean required;
        /** 描述 */
        public String description;
        /** 前缀（用于嵌套对象展示层级） */
        public String prefix;
    }
}
