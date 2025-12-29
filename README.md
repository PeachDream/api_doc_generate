# API文档生成器插件 - 使用说明

> **版本**: V1.0.0  
> **作者**: peach  
> **更新日期**: 2025-12-26

---

## 📌 一、插件简介

**API文档生成器**是一款专为 **Spring MVC / Spring Boot** 项目开发的 IntelliJ IDEA 插件，能够快速为 Controller 层的接口方法生成规范的 **Markdown 格式** 接口文档。

### 主要特性

| 功能 | 说明 |
|:---|:---|
| 🚀 **一键生成文档** | 在 Controller 类或方法上即可快速生成完整的接口文档 |
| 📝 **Markdown 格式** | 生成标准 Markdown 格式，可直接用于 Wiki、Confluence 等平台 |
| 🔍 **智能解析** | 自动解析请求参数、返回值、泛型类型、嵌套对象 |
| 👁️ **实时预览** | 内置 JCEF 浏览器实时预览 Markdown 渲染效果 |
| 🎨 **主题支持** | 支持多种预览主题，可跟随 IDEA 深色/浅色主题 |
| ⚙️ **灵活配置** | 支持排除指定父类/字段、自定义作者名等配置 |
| 📋 **便捷操作** | 支持复制到剪贴板、导出为文件 |

---

## 📌 二、安装方式

### 方式一：从源码构建

1. 克隆或下载本项目源码
2. 使用 IntelliJ IDEA 打开项目
3. 执行 Gradle 任务：`buildPlugin`
4. 在 `build/distributions/` 目录下找到生成的 `.zip` 插件包
5. 在 IDEA 中：`File` → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
6. 选择生成的插件包安装并重启 IDEA

### 方式二：开发模式运行

1. 使用 IntelliJ IDEA 打开项目
2. 使用预定义的 Run Configuration：`Run Plugin`
3. 将自动启动一个带有插件的 IDEA 沙盒实例

---

## 📌 三、快速开始

### 3.1 使用入口

插件提供了 **三种** 触发方式：

| 方式 | 操作说明 |
|:---|:---|
| **右键菜单** | 在 Controller 类或方法上右键 → 选择 `生成接口文档` |
| **快捷键** | 光标定位到 Controller 类或方法 → 按 `Alt + Home` |
| **Gutter 图标** | 点击代码左侧边栏的文档图标 |

### 3.2 两种生成模式

#### 模式一：单接口模式

将光标放在某个 **HTTP 接口方法** 上触发，将只生成该接口的文档。

```java
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {  // ← 光标在此方法上
        // ...
    }
}
```

#### 模式二：Controller 模式

将光标放在 **Controller 类名** 上触发，将生成该 Controller 下所有接口的文档列表。

```java
@RestController
@RequestMapping("/user")
public class UserController {  // ← 光标在类名上
    // 将生成该类下所有接口的文档
}
```

---

## 📌 四、预览与编辑界面

生成文档后会弹出预览对话框，界面分为以下几个区域：

```
┌─────────────────────────────────────────────────────────────────────┐
│                        接口文档编辑与预览                              │
├───────────┬─────────────────────────────────────────────────────────┤
│           │                                                         │
│  接口列表  │    Markdown 源码编辑区      │     实时预览区            │
│  (可多选)  │                             │                          │
│           │                             │                          │
├───────────┴─────────────────────────────────────────────────────────┤
│  [复制] [复制全部] [导出] [导出全部]                    [确定] [关闭] │
└─────────────────────────────────────────────────────────────────────┘
```

### 界面功能说明

| 区域/按钮 | 功能说明 |
|:---|:---|
| **接口列表** | Controller 模式下显示，支持单选/多选（`Ctrl+点击`） |
| **Markdown 源码** | 可直接编辑生成的文档内容 |
| **实时预览** | 使用 JCEF 渲染 Markdown，支持主题切换和开关 |
| **预览主题** | 支持：跟随IDEA、简约蓝、清新绿、深色经典、深海蓝、温暖橙 |
| **预览开关** | 可关闭预览以提升编辑性能 |
| **复制** | 复制当前选中接口的文档到剪贴板 |
| **复制全部** | 复制所有接口的文档到剪贴板 |
| **导出** | 将当前选中接口导出为 `.md` 文件 |
| **导出全部** | 将所有接口导出为单个 `.md` 文件 |

---

## 📌 五、生成的文档格式

生成的 Markdown 文档包含以下内容：

```markdown
# 接口名称

|作者|创建时间|当时版本|
|:----:|:----:|:----:|
|peach|2025-12-26|V3.1.5|

**接口调用位置：**
- 暂未提供自动检索，可手动补充

**请求URL：** 
- `/应用名/api/user/123`

**请求方式：**
- GET
- FormData

### 请求参数<业务参数>

|参数名|必选|类型|说明|
|:----    |:---|:----- |-----   |
|id|是|Long|用户ID|

### 请求参数Json格式

```json
{
   "id" : "Long" //用户ID
}
```

### 返回参数

|参数名|必选|类型|说明|
|:----    |:---|:----- |-----   |
|code|是|Integer|状态码|
|message|否|String|消息|
|data|是|Object|数据对象|
|--id|是|Long|用户ID|
|--name|是|String|用户名称|

### 返回参数Json格式

```json
{
   "code" : "Integer" //状态码,
   "message" : "String" //消息,
   "data" : {
      "id" : "Long" //用户ID,
      "name" : "String" //用户名称
   }
}
```
```

### 文档特点

- **自动获取应用名称**：从 `application.yml` / `bootstrap.properties` 等配置文件读取
- **自动获取版本号**：可配置使用 Git 分支名作为版本号
- **嵌套对象展示**：使用 `--` 前缀表示层级关系
- **泛型解析**：支持 `Result<Page<UserVO>>` 等复杂泛型
- **注释提取**：从 JavaDoc 和字段注释中提取描述信息

---

## 📌 六、配置设置

### 6.1 打开设置页面

目前没有独立的设置入口，配置会在首次使用时自动初始化，并在 `ApiDocGeneratorSettings.xml` 中持久化保存。

### 6.2 可配置项

| 配置项 | 默认值 | 说明 |
|:---|:---|:---|
| **默认作者** | `peach` | 生成文档时显示的作者名 |
| **使用Git分支作为版本号** | `是` | 关闭则固定显示 `V1.0.0` |
| **显示接口调用位置** | `是` | 是否在文档中显示调用位置区块 |
| **显示请求参数JSON** | `是` | 是否生成请求参数的 JSON 示例 |
| **显示返回参数JSON** | `是` | 是否生成返回参数的 JSON 示例 |
| **排除父类** | 空 | 配置需要排除的父类及其字段 |
| **默认导出路径** | 空 | 导出文件时的默认保存位置 |
| **预览主题** | `跟随IDEA` | 预览区的主题样式 |

### 6.3 排除父类功能

当你的实体类继承了公共基类（如 `BaseEntity`），但不希望在文档中展示基类的字段时，可以使用此功能。

**配置方式**：
1. 在预览对话框中会有设置入口（如果有的话）
2. 或直接编辑 `ApiDocGeneratorSettings.xml` 配置文件

**支持的配置粒度**：
- **排除整个类**：排除该类的所有字段
- **排除指定字段**：只排除该类的部分字段

**示例配置**：
```
排除类: com.example.common.BaseEntity
排除字段: createTime, updateTime, createBy, updateBy
```

---

## 📌 七、快捷键

| 快捷键 | 功能 |
|:---|:---|
| `Alt + Home` | 生成接口文档（光标需在 Controller 类或方法上） |

---

## 📌 八、支持的注解类型

### 类级别注解

| 注解 | 说明 |
|:---|:---|
| `@Controller` | Spring MVC Controller |
| `@RestController` | Spring REST Controller |
| `@RequestMapping` | 类级别路径映射 |

### 方法级别注解

| 注解 | 说明 |
|:---|:---|
| `@RequestMapping` | 通用请求映射 |
| `@GetMapping` | GET 请求 |
| `@PostMapping` | POST 请求 |
| `@PutMapping` | PUT 请求 |
| `@DeleteMapping` | DELETE 请求 |
| `@PatchMapping` | PATCH 请求 |

### 参数级别注解

| 注解 | 说明 |
|:---|:---|
| `@RequestBody` | 请求体参数（JSON格式） |
| `@RequestParam` | URL 查询参数 |
| `@PathVariable` | 路径变量 |
| `@RequestAttribute` | 请求属性（通常为认证信息，会被跳过） |

### 校验注解（用于判断必填）

| 注解 | 效果 |
|:---|:---|
| `@NotNull` | 标记为必填 |
| `@NotEmpty` | 标记为必填 |
| `@NotBlank` | 标记为必填 |
| `@Valid` | 触发嵌套校验 |

---

## 📌 九、常见问题

### Q1: 应用名称显示为 `[待填写应用名]`？

**原因**：插件无法从配置文件中解析出应用名称。

**解决方案**：
1. 确保项目中存在 `application.properties` 或 `application.yml`
2. 配置文件中包含以下任一配置：
   - `server.servlet.context-path=/your-app-name`
   - `spring.application.name=your-app-name`
3. 手动在生成的文档中修改应用名称

### Q2: Git 分支版本号获取失败？

**原因**：项目不是 Git 仓库，或 `.git` 目录不可访问。

**解决方案**：
1. 确保项目已使用 Git 初始化
2. 或在设置中关闭"使用Git分支作为版本号"选项

### Q3: 嵌套对象字段没有显示？

**原因**：可能是循环引用或类型无法解析。

**解决方案**：
1. 确保嵌套类有明确的类型定义（非 `Object`）
2. 检查是否有循环引用导致的终止

### Q4: 字段描述为空？

**原因**：字段没有 JavaDoc 注释。

**解决方案**：为字段添加注释：
```java
/** 用户ID */
private Long id;

// 或使用 Swagger 注解
@ApiModelProperty("用户ID")
private Long id;
```

---

## 📌 十、技术架构

### 核心类说明

| 类名 | 职责 |
|:---|:---|
| `GenerateApiDocAction` | 插件入口，处理 Action 触发和流程控制 |
| `ApiDocGenerator` | 核心解析器，解析方法参数和返回值 |
| `ApiDocPreviewDialog` | 预览对话框，支持编辑和实时预览 |
| `ApiDocSettingsDialog` | 配置对话框 |
| `ApiDocSettings` | 配置持久化服务 |
| `ApiDocLineMarkerProvider` | Gutter 图标提供者 |
| `GitUtils` | Git 分支信息获取工具 |
| `ApiInfo` | 接口信息数据类 |

### 依赖的 IntelliJ Platform API

- **PSI (Program Structure Interface)**：用于解析 Java 代码结构
- **JCEF (Java Chromium Embedded Framework)**：用于 Markdown 实时预览
- **PersistentStateComponent**：用于配置持久化
- **LineMarkerProvider**：用于 Gutter 图标显示

---

## 📌 十一、更新日志

### V1.0.0 (2025-12-26)

- ✨ 初始版本发布
- 🚀 支持单接口/Controller 模式生成
- 📝 支持 Markdown 实时编辑和预览
- 🎨 支持多种预览主题
- ⚙️ 支持排除父类/字段配置
- 📋 支持复制到剪贴板和导出文件
- 🔍 支持从配置文件自动获取应用名
- 🌿 支持使用 Git 分支作为版本号

---

## 📌 十二、反馈与建议

如有问题或建议，欢迎通过以下方式反馈：

- 📧 邮箱：[275092531peach@gmail.com](mailto:275092531peach@gmail.com)

---

**感谢使用 API文档生成器插件！** 🎉



# IntelliJ Platform Plugin Template

[![Twitter Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=twitter)](https://twitter.com/JBPlatform)
[![Developers Forum](https://img.shields.io/badge/JetBrains%20Platform-Join-blue)][jb:forum]

## Plugin template structure

A generated project contains the following content structure:

```
.
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── gradle
│   ├── wrapper/            Gradle Wrapper
├── src                     Plugin sources
│   ├── main
│   │   ├── kotlin/         Kotlin production sources
│   │   └── resources/      Resources - plugin.xml, icons, messages
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Gradle build configuration
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation
and the manifest for our plugin – [plugin.xml][file:plugin.xml].

> [!NOTE]
> To use Java in your plugin, create the `/src/main/java` directory.

## Plugin configuration file

The plugin configuration file is a [plugin.xml][file:plugin.xml] file located in the `src/main/resources/META-INF`
directory.
It provides general information about the plugin, its dependencies, extensions, and listeners.

You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

If you're still not quite sure what this is all about, read our
introduction: [What is the IntelliJ Platform?][docs:intro]

$H$H Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name | Description                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin         | Runs [`:runIde`][gh:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests          | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                 |
| Run Verifications  | Runs [`:verifyPlugin`][gh:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and
> required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses
the `publishPlugin` Gradle task provided by
the [intellij-platform-gradle-plugin][gh:intellij-platform-gradle-plugin-docs].

You can also upload the plugin to the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/upload)
manually via UI.

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][gh:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]
- [JetBrains Marketplace Paid Plugins][jb:paid-plugins]
- [IntelliJ SDK Code Samples][gh:code-samples]

[docs]: https://plugins.jetbrains.com/docs/intellij

[docs:intro]: https://plugins.jetbrains.com/docs/intellij/intellij-platform.html?from=IJPluginTemplate

[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate

[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin

[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

[gh:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde

[gh:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md

[jb:forum]: https://platform.jetbrains.com/

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
