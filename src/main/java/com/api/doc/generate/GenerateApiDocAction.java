package com.api.doc.generate;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 生成接口文档Action
 * 在Spring MVC Controller的类或方法上使用，生成Markdown格式的接口文档
 *
 * @author peach
 * @since 2025/12/25 | V1.0.0
 */
public class GenerateApiDocAction extends AnAction {

    /** 日志记录器 */
    private static final Logger LOG = Logger.getInstance(GenerateApiDocAction.class);

    /** 向上搜索配置文件的最大目录层数，避免无限循环 */
    private static final int MAX_SEARCH_LEVEL = 20;

    private static final class DocContext {
        private final String dateStr;
        private final String author;
        private final String version;
        private final String productVersion;
        private final boolean hasProductVersion;
        private final String controllerName;

        private DocContext(String dateStr, String author, String version, String productVersion,
                boolean hasProductVersion, String controllerName) {
            this.dateStr = dateStr;
            this.author = author;
            this.version = version;
            this.productVersion = productVersion;
            this.hasProductVersion = hasProductVersion;
            this.controllerName = controllerName;
        }
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            Messages.showErrorDialog("无法获取编辑器信息", "错误");
            return;
        }

        // 获取光标位置的元素
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);

        if (element == null) {
            Messages.showErrorDialog("请将光标放在Controller类或方法上", "错误");
            return;
        }

        // 查找方法或类
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (psiClass == null) {
            Messages.showErrorDialog("请将光标放在Controller类或方法上", "错误");
            return;
        }

        // 检查是否是Controller类
        if (!isControllerClass(psiClass)) {
            Messages.showErrorDialog("当前类不是Spring MVC Controller类", "错误");
            return;
        }

        // 生成文档前验证并清理过期的配置
        // 确保使用最新的类和字段信息，避免因配置过期导致的问题
        validateConfigBeforeGenerate(project);

        // 获取应用名称（传入psiFile用于定位正确的模块）
        String applicationName = getApplicationName(project, psiFile);

        if (method != null && hasRequestMappingAnnotation(method)) {
            // 单个方法模式 - 生成单个方法的文档
            String markdown = generateMethodDoc(psiClass, method, applicationName, psiFile);
            ApiDocPreviewDialog dialog = new ApiDocPreviewDialog(project, markdown);
            dialog.showAndGet();
            // 复制操作由用户点击"复制到剪贴板"按钮主动触发，不再自动弹窗
        } else {
            // Controller模式 - 生成接口列表
            List<ApiInfo> apiList = generateApiList(psiClass, applicationName, psiFile);
            if (apiList.isEmpty()) {
                Messages.showInfoMessage("该Controller没有找到HTTP接口方法", "提示");
                return;
            }
            // 获取Controller名称（从类注释中提取）
            String controllerName = getControllerName(psiClass);
            ApiDocPreviewDialog dialog = new ApiDocPreviewDialog(project, apiList, controllerName);
            dialog.showAndGet();
            // 复制操作由用户点击"复制到剪贴板"按钮主动触发，不再自动弹窗
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 使用VirtualFile判断文件类型，避免PSI访问的线程限制
        com.intellij.openapi.vfs.VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isJavaFile = virtualFile != null && "java".equals(virtualFile.getExtension());
        e.getPresentation().setEnabled(isJavaFile);
        e.getPresentation().setVisible(true);
    }

    /**
     * 检查类是否是Controller类
     *
     * @param psiClass 类
     * @return 是否是Controller类
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean isControllerClass(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.endsWith("Controller") ||
                    qualifiedName.endsWith("RestController"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查方法是否有RequestMapping相关注解
     *
     * @param method 方法
     * @return 是否有RequestMapping注解
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private boolean hasRequestMappingAnnotation(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.contains("Mapping"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成整个Controller类的文档
     *
     * @param psiClass        Controller类
     * @param applicationName 应用名称
     * @param psiFile         当前编辑的文件（用于获取Git分支）
     * @return Markdown文档
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private DocContext buildDocContext(PsiClass psiClass, PsiFile psiFile, ApiDocSettings settings) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String author = settings.getDefaultAuthor();
        String version = settings.isUseGitBranchAsVersion()
                ? GitUtils.getCurrentBranchName(psiClass.getProject(), psiFile)
                : "V1.0.0";
        String productVersion = settings.getProductVersion();
        String trimmedProductVersion = productVersion == null ? "" : productVersion.trim();
        boolean hasProductVersion = !trimmedProductVersion.isEmpty();
        String controllerName = getControllerName(psiClass);
        return new DocContext(dateStr, author, version, trimmedProductVersion, hasProductVersion, controllerName);
    }

    private String generateClassDoc(PsiClass psiClass, String applicationName, PsiFile psiFile) {
        StringBuilder sb = new StringBuilder();
        String classPath = getClassRequestPath(psiClass);
        ApiDocGenerator generator = new ApiDocGenerator();
        ApiDocSettings settings = ApiDocSettings.getInstance();
        DocContext context = buildDocContext(psiClass, psiFile, settings);

        for (PsiMethod method : psiClass.getMethods()) {
            if (hasRequestMappingAnnotation(method)) {
                sb.append(generateMethodDoc(psiClass, method, classPath, applicationName, psiFile,
                        generator, settings, context, null));
                sb.append("\n---\n\n");
            }
        }

        return sb.toString();
    }


    /**
     * 生成接口信息列表
     * 用于Controller模式，返回所有接口的名称和内容列表
     *
     * @param psiClass        Controller类
     * @param applicationName 应用名称
     * @param psiFile         当前编辑的文件（用于获取Git分支）
     * @return 接口信息列表
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private List<ApiInfo> generateApiList(PsiClass psiClass, String applicationName, PsiFile psiFile) {
        List<ApiInfo> apiList = new ArrayList<>();
        String classPath = getClassRequestPath(psiClass);
        ApiDocGenerator generator = new ApiDocGenerator();
        ApiDocSettings settings = ApiDocSettings.getInstance();
        DocContext context = buildDocContext(psiClass, psiFile, settings);

        for (PsiMethod method : psiClass.getMethods()) {
            if (hasRequestMappingAnnotation(method)) {
                String title = generator.getMethodTitle(method);
                String content = generateMethodDoc(psiClass, method, classPath, applicationName, psiFile,
                        generator, settings, context, title);
                apiList.add(new ApiInfo(title, content));
            }
        }

        return apiList;
    }


    /**
     * 生成单个方法的文档
     *
     * @param psiClass        Controller类
     * @param method          方法
     * @param applicationName 应用名称
     * @param psiFile         当前编辑的文件（用于获取Git分支）
     * @return Markdown文档
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String generateMethodDoc(PsiClass psiClass, PsiMethod method, String applicationName, PsiFile psiFile) {
        String classPath = getClassRequestPath(psiClass);
        return generateMethodDoc(psiClass, method, classPath, applicationName, psiFile);
    }

    /**
     * 生成单个方法的文档
     *
     * @param psiClass        Controller类
     * @param method          方法
     * @param classPath       类级别的请求路径
     * @param applicationName 应用名称
     * @param psiFile         当前编辑的文件（用于获取Git分支）
     * @return Markdown文档
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String generateMethodDoc(PsiClass psiClass, PsiMethod method, String classPath, String applicationName,
            PsiFile psiFile) {
        ApiDocGenerator generator = new ApiDocGenerator();
        ApiDocSettings settings = ApiDocSettings.getInstance();
        DocContext context = buildDocContext(psiClass, psiFile, settings);
        return generateMethodDoc(psiClass, method, classPath, applicationName, psiFile, generator, settings, context, null);
    }
    private String generateMethodDoc(PsiClass psiClass, PsiMethod method, String classPath, String applicationName,
            PsiFile psiFile, ApiDocGenerator generator, ApiDocSettings settings, DocContext context, String title) {
        StringBuilder sb = new StringBuilder();
        ApiDocGenerator docGenerator = generator != null ? generator : new ApiDocGenerator();
        ApiDocSettings docSettings = settings != null ? settings : ApiDocSettings.getInstance();
        DocContext docContext = context != null ? context : buildDocContext(psiClass, psiFile, docSettings);

        // 1. ?? - ???????????
        String methodTitle = title != null ? title : docGenerator.getMethodTitle(method);
        sb.append("# ").append(methodTitle).append("\n\n");

        // 2. ?????????????Markdown?????
        String dateStr = docContext.dateStr;
        String author = docContext.author;
        String version = docContext.version;
        String trimmedProductVersion = docContext.productVersion;
        boolean hasProductVersion = docContext.hasProductVersion;
        // 3. 接口调用位置（可配置是否显示）
        if (docSettings.isShowCallLocation()) {
            String controllerName = docContext.controllerName != null ? docContext.controllerName : getControllerName(psiClass);
            sb.append("**接口调用位置：**\n");
            sb.append("- ").append(controllerName).append(" -> ").append(methodTitle).append("\n\n");
        }

        // 4. 请求URL（包含应用名称）
        String methodPath = docGenerator.getMethodRequestPath(method);
        String apiPath = combinePath(classPath, methodPath);
        String fullPath = "/" + applicationName + apiPath;
        sb.append("**请求URL：** \n");
        sb.append("- `").append(fullPath).append("`\n\n");

        // 5. 请求方式
        String httpMethod = docGenerator.getHttpMethod(method);
        String contentType = docGenerator.getContentType(method);
        sb.append("**请求方式：**\n");
        sb.append("- ").append(httpMethod).append("\n");
        sb.append("- ").append(contentType).append("\n\n");

        // 6. 请求参数表格
        sb.append("### 请求参数<业务参数>\n \n");
        sb.append("|参数名|必选|类型|说明|\n");
        sb.append("|:----    |:---|:----- |-----   |\n");
        sb.append(docGenerator.generateRequestParamsTable(method));
        sb.append("\n");

        // 7. 请求参数JSON格式（可配置）
        if (docSettings.isShowRequestJson()) {
            sb.append("### 请求参数Json格式\n \n");
            sb.append("```json\n");
            sb.append(docGenerator.generateRequestParamsJson(method));
            sb.append("\n```\n\n");
        }

        // 8. 返回参数表格
        sb.append("### 返回参数\n \n");
        sb.append("|参数名|必选|类型|说明|\n");
        sb.append("|:----    |:---|:----- |-----   |\n");
        sb.append(docGenerator.generateResponseParamsTable(method));
        sb.append("\n");

        // 9. 返回参数JSON格式（可配置）
        if (docSettings.isShowResponseJson()) {
            sb.append("### 返回参数Json格式\n \n");
            sb.append("```json\n");
            sb.append(docGenerator.generateResponseParamsJson(method, docSettings.isShowResponseJsonComment()));
            sb.append("\n```\n");
        }

        return sb.toString();
    }

    /**
     * 获取类级别的请求路径
     *
     * @param psiClass Controller类
     * @return 请求路径
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getClassRequestPath(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("Mapping")) {
                return extractPathFromAnnotation(annotation);
            }
        }
        return "";
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
        // 尝试获取value属性
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("path");
        }
        if (value != null) {
            String text = value.getText();
            // 处理数组形式 {"path1", "path2"}
            if (text.startsWith("{")) {
                text = text.substring(1, text.length() - 1);
                if (text.contains(",")) {
                    text = text.split(",")[0].trim();
                }
            }
            // 去除引号
            text = text.replace("\"", "").trim();
            return text;
        }
        return "";
    }

    /**
     * 合并类路径和方法路径
     *
     * @param classPath  类路径
     * @param methodPath 方法路径
     * @return 完整路径
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String combinePath(String classPath, String methodPath) {
        if (classPath.isEmpty()) {
            return methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        }
        if (methodPath.isEmpty()) {
            return classPath.startsWith("/") ? classPath : "/" + classPath;
        }

        String path = classPath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/") && !methodPath.startsWith("/")) {
            path = path + "/";
        } else if (path.endsWith("/") && methodPath.startsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path + methodPath;
    }

    /**
     * 获取应用名称
     * 从当前文件位置向上逐层搜索配置文件，找到第一个有效的配置即返回
     *
     * @param project 项目
     * @param psiFile 当前编辑的文件
     * @return 应用名称，如果未找到则返回默认值
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String getApplicationName(Project project, PsiFile psiFile) {
        try {
            // 从当前文件位置向上逐层搜索配置文件
            if (psiFile != null) {
                VirtualFile currentFile = psiFile.getVirtualFile();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[ApiDoc] 当前文件: " + (currentFile != null ? currentFile.getPath() : "null"));
                }
                if (currentFile != null) {
                    String appName = findApplicationNameInModule(currentFile);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[ApiDoc] 找到的应用名: " + appName);
                    }
                    if (appName != null && !appName.isEmpty()) {
                        return appName;
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[ApiDoc] psiFile 为 null");
                }
            }
        } catch (Exception e) {
            LOG.warn("[ApiDoc] 获取应用名异常: " + e.getMessage(), e);
        }
        // 返回明确的占位符，提示用户需要手动填写
        return "[待填写应用名]";
    }

    /**
     * 向上逐层遍历目录查找配置文件中的应用名称
     * 从当前文件位置开始，向上遍历每一层目录，查找 src/main/resources 下的配置文件
     *
     * @param currentFile 当前文件
     * @return 应用名称，如果未找到返回null
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String findApplicationNameInModule(VirtualFile currentFile) {
        // 从当前文件向上遍历，查找包含 src/main/resources 的模块根目录
        VirtualFile current = currentFile.isDirectory() ? currentFile : currentFile.getParent();
        boolean debugEnabled = LOG.isDebugEnabled();
        if (debugEnabled) {
            LOG.debug("[ApiDoc] 开始从目录向上搜索: " + (current != null ? current.getPath() : "null"));
        }

        int level = 0;
        while (current != null && level < MAX_SEARCH_LEVEL) {
            level++;
            if (debugEnabled) {
                LOG.debug("[ApiDoc] 检查目录 [" + level + "]: " + current.getPath());
            }

            // 检查是否有 src/main/resources 目录
            VirtualFile src = current.findChild("src");
            if (src != null) {
                if (debugEnabled) {
                    LOG.debug("[ApiDoc]   找到 src 目录: " + src.getPath());
                }
                VirtualFile main = src.findChild("main");
                if (main != null) {
                    if (debugEnabled) {
                        LOG.debug("[ApiDoc]   找到 main 目录: " + main.getPath());
                    }
                    VirtualFile resources = main.findChild("resources");
                    if (resources != null) {
                        if (debugEnabled) {
                            LOG.debug("[ApiDoc]   找到 resources 目录: " + resources.getPath());
                        }
                        // 列出 resources 目录下的文件
                        VirtualFile[] children = resources.getChildren();
                        if (debugEnabled) {
                            LOG.debug("[ApiDoc]   resources 目录下的文件:");
                            for (VirtualFile child : children) {
                                LOG.debug("[ApiDoc]     - " + child.getName());
                            }
                        }
                        // 尝试读取配置文件
                        String appName = tryReadAppNameFromResources(resources);
                        if (debugEnabled) {
                            LOG.debug("[ApiDoc]   从 resources 读取的应用名: " + appName);
                        }
                        if (appName != null && !appName.isEmpty()) {
                            return appName;
                        }
                    }
                }
            }
            current = current.getParent();
        }
        if (debugEnabled) {
            LOG.debug("[ApiDoc] 未找到有效的配置文件");
        }
        return null;
    }

    /**
     * 从resources目录尝试读取应用名称
     * 先在resources直接目录下搜索，如果未找到则递归搜索子目录
     *
     * @param resources resources目录
     * @return 应用名称，如果未找到返回null
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String tryReadAppNameFromResources(VirtualFile resources) {
        // 首先在resources目录直接搜索配置文件
        String appName = tryReadAppNameFromDirectory(resources);
        if (appName != null && !appName.isEmpty()) {
            return appName;
        }

        // 如果未找到，递归搜索子目录
        VirtualFile[] children = resources.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[ApiDoc]   搜索子目录: " + child.getName());
                }
                appName = tryReadAppNameFromDirectory(child);
                if (appName != null && !appName.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[ApiDoc]   在子目录 " + child.getName() + " 中找到应用名: " + appName);
                    }
                    return appName;
                }
            }
        }

        return null;
    }

    /**
     * 从指定目录尝试读取应用名称
     * 搜索该目录下的配置文件（application.properties/yml, bootstrap.properties/yml）
     *
     * @param directory 目标目录
     * @return 应用名称，如果未找到返回null
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String tryReadAppNameFromDirectory(VirtualFile directory) {
        // 尝试 application.properties
        VirtualFile propsFile = directory.findChild("application.properties");
        if (propsFile != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[ApiDoc]     找到配置文件: " + propsFile.getPath());
            }
            String appName = readApplicationNameFromProperties(propsFile);
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }
        }

        // 尝试 bootstrap.properties
        VirtualFile bootstrapProps = directory.findChild("bootstrap.properties");
        if (bootstrapProps != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[ApiDoc]     找到配置文件: " + bootstrapProps.getPath());
            }
            String appName = readApplicationNameFromProperties(bootstrapProps);
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }
        }

        // 尝试 application.yml
        VirtualFile ymlFile = directory.findChild("application.yml");
        if (ymlFile != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[ApiDoc]     找到配置文件: " + ymlFile.getPath());
            }
            String appName = readApplicationNameFromYml(ymlFile);
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }
        }

        // 尝试 bootstrap.yml
        VirtualFile bootstrapYml = directory.findChild("bootstrap.yml");
        if (bootstrapYml != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[ApiDoc]     找到配置文件: " + bootstrapYml.getPath());
            }
            String appName = readApplicationNameFromYml(bootstrapYml);
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }
        }

        return null;
    }

    /**
     * 从properties文件读取应用名称
     * 优先读取 server.servlet.context-path，其次读取 spring.application.name
     *
     * @param file properties文件
     * @return 应用名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String readApplicationNameFromProperties(VirtualFile file) {
        try (InputStream is = file.getInputStream()) {
            Properties props = new Properties();
            props.load(is);

            // 优先读取 context-path
            String contextPath = props.getProperty("server.servlet.context-path");
            if (contextPath != null && !contextPath.isEmpty()) {
                // 去除前缀的斜杠
                return contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
            }

            // 其次读取 spring.application.name
            String appName = props.getProperty("spring.application.name");
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }

            // 最后尝试 spring.main.application.name
            return props.getProperty("spring.main.application.name");
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 从yml文件读取应用名称
     * 优先读取 server.servlet.context-path，其次读取 spring.application.name
     *
     * @param file yml文件
     * @return 应用名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String readApplicationNameFromYml(VirtualFile file) {
        try (InputStream is = file.getInputStream()) {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            String line;
            String contextPath = null;
            String applicationName = null;

            boolean inServer = false;
            boolean inServlet = false;
            boolean inSpring = false;
            boolean inApplication = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                int indent = line.length() - line.stripLeading().length();

                // 检查 server: 节点
                if (trimmed.equals("server:") || trimmed.startsWith("server:")) {
                    inServer = true;
                    inServlet = false;
                    inSpring = false;
                    inApplication = false;
                    continue;
                }

                // 检查 servlet: 节点（在 server 下）
                if (inServer && (trimmed.equals("servlet:") || trimmed.startsWith("servlet:"))) {
                    inServlet = true;
                    continue;
                }

                // 检查 context-path
                if (inServer && inServlet && trimmed.startsWith("context-path:")) {
                    String path = trimmed.substring(13).trim();
                    // 去除可能的引号
                    path = removeQuotes(path);
                    // 去除前缀的斜杠
                    contextPath = path.startsWith("/") ? path.substring(1) : path;
                }

                // 检查 spring: 节点
                if (trimmed.equals("spring:") || trimmed.startsWith("spring:")) {
                    inSpring = true;
                    inApplication = false;
                    inServer = false;
                    inServlet = false;
                    continue;
                }

                // 检查 application: 节点（在 spring 下）
                if (inSpring && (trimmed.equals("application:") || trimmed.startsWith("application:"))) {
                    inApplication = true;
                    continue;
                }

                // 检查 name
                if (inSpring && inApplication && trimmed.startsWith("name:")) {
                    String name = trimmed.substring(5).trim();
                    applicationName = removeQuotes(name);
                }

                // 如果遇到非缩进行，重置状态
                if (!trimmed.isEmpty() && indent == 0 && !trimmed.startsWith("#")) {
                    if (!trimmed.startsWith("server") && !trimmed.startsWith("spring")) {
                        inServer = false;
                        inServlet = false;
                        inSpring = false;
                        inApplication = false;
                    }
                }
            }

            // 优先返回 context-path
            if (contextPath != null && !contextPath.isEmpty()) {
                return contextPath;
            }
            // 其次返回 application name
            return applicationName;

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 去除字符串两端的引号
     *
     * @param value 原始字符串
     * @return 去除引号后的字符串
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private String removeQuotes(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * 获取Controller名称
     * 优先从类注释中提取第一行描述，如果没有注释则使用类名
     *
     * @param psiClass Controller类
     * @return Controller名称
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String getControllerName(PsiClass psiClass) {
        if (psiClass == null) {
            return "Controller";
        }

        PsiDocComment docComment = psiClass.getDocComment();
        if (docComment != null) {
            String summary = DocCommentUtils.extractSummary(docComment);
            if (!summary.isEmpty()) {
                return summary;
            }
        }

        String className = psiClass.getName();
        if (className != null && className.endsWith("Controller")) {
            return className.substring(0, className.length() - "Controller".length());
        }

        return className != null ? className : "Controller";
    }


    /**
     * 生成文档前验证并清理过期的配置
     * 静默清理无效的类和字段配置，确保文档生成使用最新的信息
     *
     * @param project 项目
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void validateConfigBeforeGenerate(Project project) {
        if (project == null) {
            return;
        }

        try {
            // 静默验证并清理配置
            ApiDocSettings.ConfigValidationResult result = ApiDocSettings.getInstance()
                    .validateAndCleanConfig(project);

            // 如果有配置被清理，记录日志（不打扰用户）
            if (result.hasChanges) {
                LOG.info("[ApiDoc] 已自动清理过期的配置: " + result.changeDetails);
            }
        } catch (Exception e) {
            // 配置验证失败不应该阻止文档生成
            LOG.warn("[ApiDoc] 配置验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 静态方法：生成Controller的接口文档列表
     * 供 ApiDocLineMarkerProvider 在图标点击时调用
     *
     * @param project  项目
     * @param psiClass Controller类
     * @param psiFile  包含该类的文件
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public static void generateControllerDoc(Project project, PsiClass psiClass, PsiFile psiFile) {
        if (project == null || psiClass == null) {
            return;
        }

        GenerateApiDocAction action = new GenerateApiDocAction();

        // 生成文档前验证并清理过期的配置
        action.validateConfigBeforeGenerate(project);

        // 获取应用名称
        String applicationName = action.getApplicationName(project, psiFile);

        // 生成接口列表
        java.util.List<ApiInfo> apiList = action.generateApiList(psiClass, applicationName, psiFile);
        if (apiList.isEmpty()) {
            Messages.showInfoMessage("该Controller没有找到HTTP接口方法", "提示");
            return;
        }

        // 获取Controller名称
        String controllerName = action.getControllerName(psiClass);

        // 显示Controller模式的预览对话框
        ApiDocPreviewDialog dialog = new ApiDocPreviewDialog(project, apiList, controllerName);
        dialog.showAndGet();
    }

    /**
     * 静态方法：生成单个方法的接口文档
     * 供 ApiDocLineMarkerProvider 在图标点击时调用
     *
     * @param project  项目
     * @param psiClass 方法所在的Controller类
     * @param method   目标方法
     * @param psiFile  包含该类的文件
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public static void generateMethodDoc(Project project, PsiClass psiClass, PsiMethod method, PsiFile psiFile) {
        if (project == null || psiClass == null || method == null) {
            return;
        }

        GenerateApiDocAction action = new GenerateApiDocAction();

        // 生成文档前验证并清理过期的配置
        action.validateConfigBeforeGenerate(project);

        // 获取应用名称
        String applicationName = action.getApplicationName(project, psiFile);

        // 生成单个方法的文档
        String markdown = action.generateMethodDoc(psiClass, method, applicationName, psiFile);

        // 显示单方法模式的预览对话框
        ApiDocPreviewDialog dialog = new ApiDocPreviewDialog(project, markdown);
        dialog.showAndGet();
    }
}
