package com.demojava01;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * API文档预览和编辑对话框
 * 左侧Markdown编辑器，右侧使用JCEF实现现代化实时预览
 * 支持IDEA主题适配和自定义预览主题
 *
 * @author peach
 * @since 2025/12/26 | V1.0.0
 */
public class ApiDocPreviewDialog extends DialogWrapper {

    /** 文本编辑区域 */
    private JTextArea textArea;

    /** JCEF浏览器组件 */
    private JBCefBrowser browser;

    /** 编辑器滚动面板 */
    private JBScrollPane editorScrollPane;

    /** 生成的Markdown内容 */
    private String content;

    /** 当前项目 */
    private final Project project;

    /** 更新定时器，用于防抖 */
    private Timer updateTimer;

    /** 当前预览主题 */
    private PreviewTheme currentTheme = PreviewTheme.FOLLOW_IDE;

    /** 主题选择下拉框 */
    private JComboBox<PreviewTheme> themeComboBox;

    /** 接口列表（Controller模式） */
    private java.util.List<ApiInfo> apiList;

    /** 是否为Controller模式（显示左侧接口列表） */
    private final boolean isControllerMode;

    /** 接口列表组件 */
    private JList<ApiInfo> apiListComponent;

    /** 接口列表模型 */
    private DefaultListModel<ApiInfo> apiListModel;

    /** 全部复制按钮 */
    private JButton copyAllButton;

    /** 导出全部按钮 */
    private JButton exportAllButton;

    /** Controller名称（用于导出文件命名） */
    private String controllerName;

    /** 是否为程序触发的文本变更 */
    private boolean isProgrammaticChange;

    /** 是否有未保存的编辑 */
    private boolean isDirty;

    /** 选择切换保护，防止递归触发 */
    private boolean suppressSelectionChange;

    /** 上一次选中的索引 */
    private int[] lastSelectedIndices = new int[0];

    /** 预览开关是否开启 */
    private boolean previewEnabled = true;

    /** 预览开关 */
    private PreviewToggleButton previewToggle;

    /** 预览容器 */
    private JPanel previewContainer;

    /** 预览容器卡片布局 */
    private CardLayout previewCardLayout;

    /** 默认字体名称 */
    private static final String DEFAULT_FONT_NAME = "Microsoft YaHei";

    /**
     * 预览主题枚举
     * 使用更浅、更简约的配色方案
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public enum PreviewTheme {
        /** 跟随IDEA主题（默认） */
        FOLLOW_IDE("跟随IDEA", null, null, null, null, null, null),
        /** 简约蓝主题 */
        SIMPLE_BLUE("简约蓝", "#f5f7fa", "#3b82f6", "#60a5fa", "#1e40af", "#2563eb", "#3b82f6"),
        /** 清新绿主题 */
        FRESH_GREEN("清新绿", "#f0fdf4", "#22c55e", "#4ade80", "#166534", "#16a34a", "#22c55e"),
        /** 深色经典 */
        DARK_CLASSIC("深色经典", "#1e1e1e", "#569cd6", "#4ec9b0", "#dcdcaa", "#c586c0", "#ce9178"),
        /** 深海蓝主题 */
        DARK_OCEAN("深海蓝", "#0f172a", "#38bdf8", "#0ea5e9", "#38bdf8", "#818cf8", "#fbbf24"),
        /** 暖色主题 */
        WARM_LIGHT("温暖橙", "#fffbeb", "#f59e0b", "#fbbf24", "#b45309", "#d97706", "#f59e0b");

        private final String displayName;
        private final String bgColor;
        private final String primaryColor;
        private final String secondaryColor;
        private final String h1Color;
        private final String h2Color;
        private final String h3Color;

        PreviewTheme(String displayName, String bgColor, String primaryColor, String secondaryColor,
                String h1Color, String h2Color, String h3Color) {
            this.displayName = displayName;
            this.bgColor = bgColor;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.h1Color = h1Color;
            this.h2Color = h2Color;
            this.h3Color = h3Color;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public String getBgColor() {
            return bgColor;
        }

        public String getPrimaryColor() {
            return primaryColor;
        }

        public String getSecondaryColor() {
            return secondaryColor;
        }

        public String getH1Color() {
            return h1Color;
        }

        public String getH2Color() {
            return h2Color;
        }

        public String getH3Color() {
            return h3Color;
        }
    }

    /**
     * 构造函数 - 单接口模式
     *
     * @param project 项目
     * @param content 生成的Markdown内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public ApiDocPreviewDialog(@Nullable Project project, String content) {
        super(project, true);
        this.project = project;
        this.content = content;
        this.isControllerMode = false;
        this.apiList = null;
        // 从设置加载主题
        loadThemeFromSettings();
        setTitle("接口文档预览");
        setOKButtonText("复制到剪贴板");
        setCancelButtonText("关闭");
        init();
    }

    /**
     * 构造函数 - Controller模式（显示接口列表）
     *
     * @param project        项目
     * @param apiList        接口信息列表
     * @param controllerName Controller名称（用于导出文件命名）
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public ApiDocPreviewDialog(@Nullable Project project, java.util.List<ApiInfo> apiList, String controllerName) {
        super(project, true);
        this.project = project;
        this.apiList = apiList;
        this.isControllerMode = true;
        this.controllerName = controllerName != null ? controllerName : "Controller";
        // 默认显示第一个接口的内容
        this.content = apiList.isEmpty() ? "" : apiList.get(0).getContent();
        // 从设置加载主题
        loadThemeFromSettings();
        setTitle("接口文档预览 - 共 " + apiList.size() + " 个接口");
        setOKButtonText("复制到剪贴板");
        setCancelButtonText("关闭");
        init();
    }

    /**
     * 从设置中加载主题
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void loadThemeFromSettings() {
        ApiDocSettings settings = ApiDocSettings.getInstance();
        String themeName = settings.getPreviewTheme();
        try {
            currentTheme = PreviewTheme.valueOf(themeName);
        } catch (Exception e) {
            currentTheme = PreviewTheme.FOLLOW_IDE;
        }
        previewEnabled = settings.isPreviewEnabled();
    }

    /**
     * 保存主题到设置
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void saveThemeToSettings() {
        ApiDocSettings.getInstance().setPreviewTheme(currentTheme.name());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 获取IDEA主题颜色
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color ideaBgColor = scheme.getDefaultBackground();
        // 通过背景颜色亮度判断是否为深色主题
        boolean isDarkTheme = isDarkTheme(ideaBgColor);
        Color ideaFgColor = scheme.getDefaultForeground();

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        // 使用屏幕相对尺寸，宽度为屏幕的70%，高度为屏幕的75%，限制最大最小值
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(Math.max((int) (screenSize.width * 0.7), 900), 1600);
        int dialogHeight = Math.min(Math.max((int) (screenSize.height * 0.75), 600), 1000);
        mainPanel.setPreferredSize(new Dimension(dialogWidth, dialogHeight));
        mainPanel.setBorder(JBUI.Borders.empty(10));
        mainPanel.setBackground(ideaBgColor);

        // 顶部提示栏
        JPanel headerPanel = createHeaderPanel(ideaBgColor, ideaFgColor, isDarkTheme);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 中间分栏区域（编辑器 + 预览）
        JSplitPane splitPane = createSplitPane(ideaBgColor, ideaFgColor, isDarkTheme);

        // 底部按钮栏
        JPanel bottomPanel = createBottomPanel(ideaBgColor, ideaFgColor);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 中间内容区域
        if (isControllerMode && apiList != null && !apiList.isEmpty()) {
            // Controller模式：左侧接口列表 + 右侧编辑预览区
            JSplitPane outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            outerSplitPane.setLeftComponent(createApiListPanel(ideaBgColor, ideaFgColor, isDarkTheme));
            outerSplitPane.setRightComponent(splitPane);
            outerSplitPane.setDividerLocation(200);
            outerSplitPane.setDividerSize(3);
            outerSplitPane.setBackground(ideaBgColor);
            mainPanel.add(outerSplitPane, BorderLayout.CENTER);
        } else {
            // 单接口模式：直接使用编辑预览区
            mainPanel.add(splitPane, BorderLayout.CENTER);
        }

        // 初始化预览内容
        SwingUtilities.invokeLater(this::updatePreview);

        return mainPanel;
    }

    /**
     * 创建左侧接口列表面板
     *
     * @param bgColor     背景色
     * @param fgColor     前景色
     * @param isDarkTheme 是否为深色主题
     * @return 接口列表面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createApiListPanel(Color bgColor, Color fgColor, boolean isDarkTheme) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // 标题
        JLabel titleLabel = new JLabel("接口列表");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        titleLabel.setForeground(new Color(91, 143, 249));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 接口列表
        apiListModel = new DefaultListModel<>();
        for (ApiInfo api : apiList) {
            apiListModel.addElement(api);
        }

        apiListComponent = new JList<>(apiListModel);
        // 使用多选模式，支持 Ctrl+点击 和 Shift+点击
        apiListComponent.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        apiListComponent.setSelectedIndex(0);
        lastSelectedIndices = apiListComponent.getSelectedIndices();
        apiListComponent.setBackground(isDarkTheme ? bgColor.brighter() : new Color(250, 251, 252));
        apiListComponent.setForeground(fgColor);
        apiListComponent.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        apiListComponent.setCellRenderer(new ApiListCellRenderer(isDarkTheme, bgColor, fgColor));

        // 选择监听 - 支持多选
        apiListComponent.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !suppressSelectionChange) {
                if (isDirty) {
                    int result = Messages.showYesNoDialog(
                            project,
                            "当前文档有未保存修改，是否继续切换？",
                            "未保存修改",
                            "继续切换",
                            "取消",
                            Messages.getWarningIcon());
                    if (result != Messages.YES) {
                        suppressSelectionChange = true;
                        apiListComponent.setSelectedIndices(lastSelectedIndices);
                        suppressSelectionChange = false;
                        return;
                    }
                }
                java.util.List<ApiInfo> selectedApis = apiListComponent.getSelectedValuesList();
                if (!selectedApis.isEmpty()) {
                    // 根据选中数量更新内容
                    if (selectedApis.size() == 1) {
                        // 单选：显示该接口内容
                        content = selectedApis.get(0).getContent();
                    } else {
                        // 多选：合并所有选中接口的内容
                        StringBuilder mergedContent = new StringBuilder();
                        for (int i = 0; i < selectedApis.size(); i++) {
                            mergedContent.append(selectedApis.get(i).getContent());
                            if (i < selectedApis.size() - 1) {
                                mergedContent.append("\n---\n\n");
                            }
                        }
                        content = mergedContent.toString();
                    }
                    setEditorContent(content);
                    updatePreview();
                    // 更新标题显示选中数量
                    updateTitle(selectedApis.size());
                    // 更新"导出全部"和"复制全部"按钮数字（多选时显示选中数量，单选时显示全部数量）
                    if (selectedApis.size() > 1) {
                        updateAllButtonsCount(selectedApis.size());
                    } else {
                        updateAllButtonsCount(apiList.size());
                    }
                    lastSelectedIndices = apiListComponent.getSelectedIndices();
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(apiListComponent);
        listScrollPane.setBorder(BorderFactory.createLineBorder(
                isDarkTheme ? new Color(70, 75, 85) : new Color(220, 222, 225), 1));
        panel.add(listScrollPane, BorderLayout.CENTER);

        // 底部提示面板
        JPanel tipPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        tipPanel.setBackground(bgColor);

        // 提示标签
        JLabel tipLabel = new JLabel("提示: Ctrl+点击多选");
        tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        tipLabel.setForeground(new Color(140, 145, 155));
        tipPanel.add(tipLabel);

        panel.add(tipPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 接口列表单元格渲染器
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private static class ApiListCellRenderer extends DefaultListCellRenderer {
        private final boolean isDarkTheme;
        private final Color bgColor;
        private final Color fgColor;

        public ApiListCellRenderer(boolean isDarkTheme, Color bgColor, Color fgColor) {
            this.isDarkTheme = isDarkTheme;
            this.bgColor = bgColor;
            this.fgColor = fgColor;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ApiInfo) {
                setText((index + 1) + ". " + ((ApiInfo) value).getName());
            }

            setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            if (isSelected) {
                setBackground(new Color(91, 143, 249));
                setForeground(Color.WHITE);
            } else {
                setBackground(isDarkTheme ? bgColor.brighter() : new Color(250, 251, 252));
                setForeground(fgColor);
            }

            return this;
        }
    }

    private static class PreviewToggleButton extends JToggleButton {
        private final Color onColor;
        private final Color offColor;
        private final Color knobColor;

        public PreviewToggleButton(Color onColor, Color offColor, Color knobColor) {
            this.onColor = onColor;
            this.offColor = offColor;
            this.knobColor = knobColor;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setPreferredSize(new Dimension(48, 22));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = height;

            g2.setColor(isSelected() ? onColor : offColor);
            g2.fillRoundRect(0, 0, width, height, arc, arc);

            int knobSize = height - 4;
            int knobX = isSelected() ? width - knobSize - 2 : 2;
            g2.setColor(knobColor);
            g2.fillOval(knobX, 2, knobSize, knobSize);

            g2.dispose();
        }
    }

    /**
     * 创建顶部标题栏
     * 使用现代简约风格，浅色主题使用蓝色强调色
     *
     * @param bgColor     背景色
     * @param fgColor     前景色
     * @param isDarkTheme 是否为深色主题
     * @return 标题面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createHeaderPanel(Color bgColor, Color fgColor, boolean isDarkTheme) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        // 使用统一的背景色，不再变深/变浅
        headerPanel.setBackground(bgColor);

        // 使用柔和的蓝色作为强调色
        Color accentBlue = new Color(91, 143, 249); // #5B8FF9
        Color borderColor = isDarkTheme ? new Color(70, 75, 85) : new Color(230, 232, 235);

        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JLabel titleLabel = new JLabel("接口文档编辑与预览");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        titleLabel.setForeground(accentBlue);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel tipLabel = new JLabel("左侧编辑 Markdown，右侧实时预览效果");
        tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        tipLabel.setForeground(isDarkTheme ? new Color(150, 155, 165) : new Color(140, 145, 155));
        headerPanel.add(tipLabel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * 创建左右分栏面板
     *
     * @param bgColor     背景色
     * @param fgColor     前景色
     * @param isDarkTheme 是否为深色主题
     * @return 分栏面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JSplitPane createSplitPane(Color bgColor, Color fgColor, boolean isDarkTheme) {
        // 左侧编辑器面板
        JPanel editorPanel = createEditorPanel(bgColor, fgColor, isDarkTheme);

        // 右侧JCEF预览面板
        JPanel previewPanel = createPreviewPanel(bgColor, fgColor, isDarkTheme);

        // 分栏
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, previewPanel);
        splitPane.setDividerLocation(600);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setBackground(bgColor);

        return splitPane;
    }

    /**
     * 创建编辑器面板
     *
     * @param bgColor     背景色
     * @param fgColor     前景色
     * @param isDarkTheme 是否为深色主题
     * @return 编辑器面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createEditorPanel(Color bgColor, Color fgColor, boolean isDarkTheme) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(bgColor);
        panel.setBorder(JBUI.Borders.empty(10, 0, 0, 5));

        // 编辑器标题
        JLabel editorTitle = new JLabel("  ■ Markdown 源码");
        editorTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        editorTitle.setForeground(isDarkTheme ? new Color(78, 154, 232) : new Color(0, 102, 153));
        editorTitle.setBorder(JBUI.Borders.emptyBottom(5));
        panel.add(editorTitle, BorderLayout.NORTH);

        // 文本编辑区域 - 使用支持中文的字体
        textArea = new JTextArea(content);
        // 使用系统支持中文的字体，按优先级选择
        Font editorFont = getChineseFont(13);
        textArea.setFont(editorFont);
        Color editorBg = isDarkTheme ? new Color(28, 28, 28) : new Color(250, 250, 250);
        textArea.setBackground(editorBg);
        textArea.setForeground(fgColor);
        textArea.setCaretColor(isDarkTheme ? new Color(86, 156, 214) : new Color(0, 102, 204));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setTabSize(4);
        textArea.setBorder(JBUI.Borders.empty(15));

        // 设置Caret更新策略，防止编辑时自动滚动
        javax.swing.text.DefaultCaret caret = (javax.swing.text.DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);

        // 使用防抖机制监听文本变化
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleEditorChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleEditorChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleEditorChange();
            }
        });

        editorScrollPane = new JBScrollPane(textArea);
        Color borderColor = isDarkTheme ? new Color(50, 50, 50) : new Color(200, 200, 200);
        editorScrollPane.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        editorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(editorScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 获取支持中文的字体
     *
     * @param size 字体大小
     * @return 中文字体
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private Font getChineseFont(int size) {
        // 按优先级尝试字体
        String[] fontNames = {
                // 微软雅黑
                "Microsoft YaHei",
                // 黑体
                "SimHei",
                // 宋体
                "SimSun",
                // 新宋体
                "NSimSun",
                // 楷体
                "KaiTi",
                // 仿宋
                "FangSong",
                // 默认对话框字体
                "Dialog"
        };

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        java.util.Set<String> availableSet = new java.util.HashSet<>(java.util.Arrays.asList(availableFonts));

        for (String fontName : fontNames) {
            if (availableSet.contains(fontName)) {
                return new Font(fontName, Font.PLAIN, size);
            }
        }

        // 如果都没有，使用默认字体
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    /**
     * 处理编辑器内容变化
     * 标记内容为已修改状态并调度预览更新
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void handleEditorChange() {
        if (isProgrammaticChange) {
            return;
        }
        isDirty = true;
        scheduleUpdate();
    }

    /**
     * 设置编辑器内容
     * 使用程序标记避免触发不必要的更新
     *
     * @param newContent 新内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void setEditorContent(String newContent) {
        if (textArea == null) {
            content = newContent;
            return;
        }
        isProgrammaticChange = true;
        textArea.setText(newContent);
        isProgrammaticChange = false;
        isDirty = false;
    }

    /**
     * 使用防抖机制调度更新
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void scheduleUpdate() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        // 300ms防抖延迟
        updateTimer = new Timer(300, e -> updatePreview());
        updateTimer.setRepeats(false);
        updateTimer.start();
    }

    /**
     * 创建预览面板（使用JCEF）
     *
     * @param bgColor     背景色
     * @param fgColor     前景色
     * @param isDarkTheme 是否为深色主题
     * @return 预览面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createPreviewPanel(Color bgColor, Color fgColor, boolean isDarkTheme) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(bgColor);
        panel.setBorder(JBUI.Borders.empty(10, 5, 0, 0));

        // 预览标题栏（包含主题选择）
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(bgColor);
        titlePanel.setBorder(JBUI.Borders.emptyBottom(5));

        JLabel previewTitle = new JLabel("  ● 实时预览");
        previewTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        previewTitle.setForeground(isDarkTheme ? new Color(152, 195, 121) : new Color(40, 167, 69));
        titlePanel.add(previewTitle, BorderLayout.WEST);

        // 预览开关
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        togglePanel.setBackground(bgColor);
        JLabel toggleLabel = new JLabel("预览开关");
        toggleLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        toggleLabel.setForeground(fgColor);
        togglePanel.add(toggleLabel);

        Color toggleOn = isDarkTheme ? new Color(82, 196, 26) : new Color(40, 167, 69);
        Color toggleOff = isDarkTheme ? new Color(90, 95, 105) : new Color(210, 215, 220);
        previewToggle = new PreviewToggleButton(toggleOn, toggleOff, Color.WHITE);
        previewToggle.setSelected(previewEnabled);
        previewToggle.setToolTipText("开启/关闭预览");
        previewToggle.addActionListener(e -> setPreviewEnabled(previewToggle.isSelected()));
        togglePanel.add(previewToggle);

        titlePanel.add(togglePanel, BorderLayout.CENTER);

        // 主题选择面板
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        themePanel.setBackground(bgColor);

        JLabel themeLabel = new JLabel("预览主题:");
        themeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        themeLabel.setForeground(fgColor);
        themePanel.add(themeLabel);

        themeComboBox = new JComboBox<>(PreviewTheme.values());
        themeComboBox.setSelectedItem(currentTheme);
        themeComboBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        themeComboBox.setPreferredSize(new Dimension(120, 28));
        themeComboBox.addActionListener(e -> {
            currentTheme = (PreviewTheme) themeComboBox.getSelectedItem();
            saveThemeToSettings();
            updatePreview();
        });
        themePanel.add(themeComboBox);

        titlePanel.add(themePanel, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);

        // JCEF浏览器
        browser = new JBCefBrowser();
        JComponent browserComponent = browser.getComponent();
        Color borderColor = isDarkTheme ? new Color(50, 50, 50) : new Color(200, 200, 200);

        previewCardLayout = new CardLayout();
        previewContainer = new JPanel(previewCardLayout);
        previewContainer.setBackground(bgColor);
        previewContainer.setBorder(BorderFactory.createLineBorder(borderColor, 1));

        JPanel placeholderPanel = new JPanel();
        placeholderPanel.setBackground(bgColor);
        placeholderPanel.setLayout(new BoxLayout(placeholderPanel, BoxLayout.Y_AXIS));

        JLabel placeholderTitle = new JLabel("预览已关闭");
        placeholderTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        placeholderTitle.setForeground(isDarkTheme ? new Color(150, 155, 165) : new Color(120, 130, 140));
        placeholderTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel placeholderTip = new JLabel("打开上方开关即可恢复预览");
        placeholderTip.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        placeholderTip.setForeground(isDarkTheme ? new Color(120, 125, 135) : new Color(150, 160, 170));
        placeholderTip.setAlignmentX(Component.CENTER_ALIGNMENT);

        placeholderPanel.add(Box.createVerticalGlue());
        placeholderPanel.add(placeholderTitle);
        placeholderPanel.add(Box.createVerticalStrut(6));
        placeholderPanel.add(placeholderTip);
        placeholderPanel.add(Box.createVerticalGlue());

        previewContainer.add(browserComponent, "preview");
        previewContainer.add(placeholderPanel, "placeholder");
        previewCardLayout.show(previewContainer, "preview");

        panel.add(previewContainer, BorderLayout.CENTER);

        setPreviewEnabled(previewEnabled);

        return panel;
    }

    /**
     * 创建底部面板
     * 左侧：设置按钮和提示
     * 右侧：操作按钮（复制全部、导出、导出全部、关闭）
     *
     * @param bgColor 背景色
     * @param fgColor 前景色
     * @return 底部面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createBottomPanel(Color bgColor, Color fgColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 左侧：设置按钮和提示
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(bgColor);

        // 设置按钮（灰色 - 配置类）
        JButton settingsButton = createUnifiedButton("设置", new Color(108, 117, 125));
        settingsButton.addActionListener(e -> {
            ApiDocSettingsDialog settingsDialog = new ApiDocSettingsDialog(project);
            settingsDialog.show();
        });
        leftPanel.add(settingsButton);

        JLabel settingsTip = new JLabel("  修改设置后需重新生成文档");
        settingsTip.setForeground(new Color(140, 145, 155));
        settingsTip.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        leftPanel.add(settingsTip);

        panel.add(leftPanel, BorderLayout.WEST);

        // 右侧：操作按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setBackground(bgColor);

        // Controller模式下显示复制全部和导出全部按钮
        if (isControllerMode && apiList != null && !apiList.isEmpty()) {
            // 复制全部按钮（蓝色 - 复制类操作）
            copyAllButton = createUnifiedButton("复制全部 (" + apiList.size() + ")", new Color(91, 143, 249));
            copyAllButton.addActionListener(e -> onCopyAll());
            rightPanel.add(copyAllButton);

            // 导出全部按钮（蓝色渐变 - 导出类操作）
            exportAllButton = createUnifiedButton("导出全部 (" + apiList.size() + ")", new Color(0, 123, 255));
            exportAllButton.addActionListener(e -> onExportAll());
            rightPanel.add(exportAllButton);
        }

        // 导出按钮（绿色 - 单个导出）
        JButton exportButton = createUnifiedButton("导出", new Color(40, 167, 69));
        exportButton.addActionListener(e -> onExport());
        rightPanel.add(exportButton);

        // 复制到剪贴板按钮（主要操作 - 蓝色）
        JButton copyButton = createUnifiedButton("复制到剪贴板", new Color(59, 130, 246));
        copyButton.addActionListener(e -> {
            String editedContent = textArea.getText();
            StringSelection selection = new StringSelection(editedContent);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            showNotification("已复制到剪贴板", NotificationType.INFORMATION);
        });
        rightPanel.add(copyButton);

        // 关闭按钮（灰色 - 取消类）
        JButton closeButton = createUnifiedButton("关闭", new Color(108, 117, 125));
        closeButton.addActionListener(e -> close(0));
        rightPanel.add(closeButton);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建统一样式的按钮
     * 按钮样式统一，用颜色区分功能
     *
     * @param text    按钮文本
     * @param bgColor 背景颜色
     * @return 按钮
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JButton createUnifiedButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        // 根据文本长度动态调整宽度
        int width = Math.max(80, text.length() * 12 + 24);
        button.setPreferredSize(new Dimension(width, 32));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        // 悬停效果
        Color originalBg = bgColor;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        return button;
    }

    /**
     * 显示非模态通知
     * 使用 IntelliJ Notifications API 显示不需要用户执行操作的提示
     *
     * @param message 通知消息
     * @param type    通知类型
     * @author peach
     * @since 2025/12/26 | V3.1.5
     */
    private void showNotification(String message, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("ApiDoc Notifications")
                .createNotification(message, type)
                .notify(project);
    }

    /**
     * 更新"导出全部"和"复制全部"按钮的数字
     *
     * @param count 数量
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void updateAllButtonsCount(int count) {
        if (exportAllButton != null) {
            exportAllButton.setText("导出全部 (" + count + ")");
        }
        if (copyAllButton != null) {
            copyAllButton.setText("复制全部 (" + count + ")");
        }
    }

    /**
     * 复制全部接口文档
     * 当有多选时复制选中的接口，否则复制全部
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onCopyAll() {
        if (apiList == null || apiList.isEmpty()) {
            return;
        }

        // 获取要复制的接口列表
        java.util.List<ApiInfo> targetApis;
        java.util.List<ApiInfo> selectedApis = apiListComponent.getSelectedValuesList();
        if (selectedApis.size() > 1) {
            // 多选时复制选中的接口
            targetApis = selectedApis;
        } else {
            // 单选或无选中时复制全部
            targetApis = apiList;
        }

        // 合并所有接口内容
        StringBuilder allContent = new StringBuilder();
        for (int i = 0; i < targetApis.size(); i++) {
            allContent.append(targetApis.get(i).getContent());
            if (i < targetApis.size() - 1) {
                allContent.append("\n---\n\n");
            }
        }

        // 复制到剪贴板
        StringSelection selection = new StringSelection(allContent.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        showNotification("已复制 " + targetApis.size() + " 个接口文档到剪贴板", NotificationType.INFORMATION);
    }

    /**
     * 导出全部接口文档
     * 如果已设置导出路径，直接导出到该目录（不弹窗）
     * 如果未设置路径，弹出文件选择对话框
     * 当有多选时导出选中的接口，否则导出全部
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onExportAll() {
        if (apiList == null || apiList.isEmpty()) {
            return;
        }

        ApiDocSettings settings = ApiDocSettings.getInstance();
        String exportDir = settings.getExportPath();

        // 获取要导出的接口列表
        java.util.List<ApiInfo> targetApis;
        java.util.List<ApiInfo> selectedApis = apiListComponent.getSelectedValuesList();
        if (selectedApis.size() > 1) {
            // 多选时导出选中的接口
            targetApis = selectedApis;
        } else {
            // 单选或无选中时导出全部
            targetApis = apiList;
        }

        // 合并所有接口内容
        StringBuilder allContent = new StringBuilder();
        for (int i = 0; i < targetApis.size(); i++) {
            allContent.append(targetApis.get(i).getContent());
            if (i < targetApis.size() - 1) {
                allContent.append("\n---\n\n");
            }
        }

        // 生成默认文件名
        // 单个接口：Controller名称_接口名称.md
        // 多个接口：Controller名称_（N）个接口.md
        String defaultFileName;
        String baseFileName;
        if (targetApis.size() == 1) {
            // 单个接口
            String apiName = targetApis.get(0).getName();
            // 清理文件名中的非法字符
            apiName = sanitizeFileName(apiName);
            baseFileName = controllerName + "_" + apiName;
            defaultFileName = baseFileName + ".md";
        } else {
            // 多个接口
            baseFileName = controllerName + "_（" + targetApis.size() + "）个接口";
            defaultFileName = baseFileName + ".md";
        }

        // 检查是否已设置导出目录
        java.io.File exportDirFile = (exportDir != null && !exportDir.isEmpty()) ? new java.io.File(exportDir) : null;

        if (exportDirFile != null && exportDirFile.exists() && exportDirFile.isDirectory()) {
            // 已设置导出目录，直接导出（不弹窗）
            java.io.File file = new java.io.File(exportDirFile, defaultFileName);

            // 如果文件已存在，添加序号
            int counter = 1;
            while (file.exists()) {
                file = new java.io.File(exportDirFile, baseFileName + "_" + counter + ".md");
                counter++;
            }

            try {
                // 写入文件
                java.nio.file.Files.writeString(file.toPath(), allContent.toString(),
                        java.nio.charset.StandardCharsets.UTF_8);

                // 显示成功消息
                showNotification("已成功导出 " + targetApis.size() + " 个接口文档到：" + file.getAbsolutePath(),
                        NotificationType.INFORMATION);
            } catch (java.io.IOException ex) {
                Messages.showErrorDialog(project,
                        "导出失败: " + ex.getMessage(),
                        "导出错误");
            }
        } else {
            // 未设置导出目录，弹出文件选择对话框
            String dialogTitle = targetApis.size() == apiList.size()
                    ? "导出全部接口文档（共 " + targetApis.size() + " 个）"
                    : "导出选中接口文档（共 " + targetApis.size() + " 个）";
            com.intellij.openapi.fileChooser.FileSaverDescriptor descriptor = new com.intellij.openapi.fileChooser.FileSaverDescriptor(
                    dialogTitle,
                    "选择保存位置",
                    "md");

            com.intellij.openapi.fileChooser.FileSaverDialog dialog = com.intellij.openapi.fileChooser.FileChooserFactory
                    .getInstance()
                    .createSaveFileDialog(descriptor, project);

            // 使用项目目录作为默认目录
            com.intellij.openapi.vfs.VirtualFile baseDir = project.getBaseDir();

            // 显示对话框
            com.intellij.openapi.vfs.VirtualFileWrapper wrapper = dialog.save(baseDir, defaultFileName);

            if (wrapper != null) {
                java.io.File file = wrapper.getFile();

                try {
                    // 写入文件
                    java.nio.file.Files.writeString(file.toPath(), allContent.toString(),
                            java.nio.charset.StandardCharsets.UTF_8);

                    // 保存导出目录（只保存目录路径，不保存文件名）
                    java.io.File parentDir = file.getParentFile();
                    if (parentDir != null) {
                        settings.setExportPath(parentDir.getAbsolutePath());
                    }
                    // 显示成功消息
                    showNotification("已成功导出 " + targetApis.size() + " 个接口文档到：" + file.getAbsolutePath(),
                            NotificationType.INFORMATION);
                } catch (java.io.IOException ex) {
                    Messages.showErrorDialog(project,
                            "导出失败: " + ex.getMessage(),
                            "导出错误");
                }
            }
        }
    }

    /**
     * 导出Markdown文件
     * 如果已设置导出路径，直接导出到该目录（不弹窗）
     * 如果未设置路径，弹出文件选择对话框
     * 支持导出当前选中的接口（单个或多个）
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onExport() {
        ApiDocSettings settings = ApiDocSettings.getInstance();
        String exportDir = settings.getExportPath();

        // 获取要导出的内容
        String exportContent = getExportContent();
        int exportCount = getExportApiCount();

        // 生成默认文件名
        // 单个接口：Controller名称_接口名称.md
        // 多个接口：Controller名称_（N）个接口.md
        String defaultFileName;
        String baseFileName;
        if (isControllerMode && controllerName != null) {
            if (exportCount == 1 && apiListComponent != null) {
                // 单个接口
                java.util.List<ApiInfo> selectedApis = apiListComponent.getSelectedValuesList();
                String apiName = !selectedApis.isEmpty() ? selectedApis.get(0).getName() : "接口";
                apiName = sanitizeFileName(apiName);
                baseFileName = controllerName + "_" + apiName;
            } else {
                // 多个接口
                baseFileName = controllerName + "_（" + exportCount + "）个接口";
            }
            defaultFileName = baseFileName + ".md";
        } else {
            // 非Controller模式，使用通用文件名
            baseFileName = "接口文档_"
                    + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            defaultFileName = baseFileName + ".md";
        }

        // 检查是否已设置导出目录
        java.io.File exportDirFile = (exportDir != null && !exportDir.isEmpty()) ? new java.io.File(exportDir) : null;

        if (exportDirFile != null && exportDirFile.exists() && exportDirFile.isDirectory()) {
            // 已设置导出目录，直接导出（不弹窗）
            java.io.File file = new java.io.File(exportDirFile, defaultFileName);

            // 如果文件已存在，添加序号
            int counter = 1;
            while (file.exists()) {
                file = new java.io.File(exportDirFile, baseFileName + "_" + counter + ".md");
                counter++;
            }

            try {
                // 写入文件
                java.nio.file.Files.writeString(file.toPath(), exportContent, java.nio.charset.StandardCharsets.UTF_8);

                // 显示成功消息
                String successMsg = isControllerMode && exportCount > 1
                        ? "已成功导出 " + exportCount + " 个接口文档到：\n" + file.getAbsolutePath()
                        : "文档已成功导出到：\n" + file.getAbsolutePath();
                Messages.showInfoMessage(project, successMsg, "导出成功");
            } catch (java.io.IOException ex) {
                Messages.showErrorDialog(
                        project,
                        "导出失败: " + ex.getMessage(),
                        "导出错误");
            }
        } else {
            // 未设置导出目录，弹出文件选择对话框
            String dialogTitle = isControllerMode && exportCount > 1
                    ? "导出接口文档（已选 " + exportCount + " 个接口）"
                    : "导出接口文档";

            com.intellij.openapi.fileChooser.FileSaverDescriptor descriptor = new com.intellij.openapi.fileChooser.FileSaverDescriptor(
                    dialogTitle,
                    "选择保存位置",
                    "md");

            com.intellij.openapi.fileChooser.FileSaverDialog dialog = com.intellij.openapi.fileChooser.FileChooserFactory
                    .getInstance()
                    .createSaveFileDialog(descriptor, project);

            // 使用项目目录作为默认目录
            com.intellij.openapi.vfs.VirtualFile baseDir = project.getBaseDir();

            // 显示对话框
            com.intellij.openapi.vfs.VirtualFileWrapper wrapper = dialog.save(baseDir, defaultFileName);

            if (wrapper != null) {
                java.io.File file = wrapper.getFile();

                try {
                    // 写入文件
                    java.nio.file.Files.writeString(file.toPath(), exportContent,
                            java.nio.charset.StandardCharsets.UTF_8);

                    // 保存导出目录（只保存目录路径，不保存文件名）
                    java.io.File parentDir = file.getParentFile();
                    if (parentDir != null) {
                        settings.setExportPath(parentDir.getAbsolutePath());
                    }

                    // 显示成功消息
                    String successMsg = isControllerMode && exportCount > 1
                            ? "已成功导出 " + exportCount + " 个接口文档到：\n" + file.getAbsolutePath()
                            : "文档已成功导出到：\n" + file.getAbsolutePath();
                    Messages.showInfoMessage(project, successMsg, "导出成功");
                } catch (java.io.IOException ex) {
                    Messages.showErrorDialog(
                            project,
                            "导出失败: " + ex.getMessage(),
                            "导出错误");
                }
            }
        }
    }

    /**
     * 获取要导出的内容
     * 如果是Controller模式且有选中项，返回选中项的内容
     * 否则返回编辑器中的内容
     *
     * @return 要导出的内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String getExportContent() {
        return textArea.getText();
    }

    /**
     * 获取要导出的接口数量
     *
     * @return 接口数量
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private int getExportApiCount() {
        if (isControllerMode && apiListComponent != null) {
            int[] selectedIndices = apiListComponent.getSelectedIndices();
            return selectedIndices.length;
        }
        return 1;
    }

    /**
     * 更新对话框标题，显示选中数量
     *
     * @param selectedCount 选中的接口数量
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void updateTitle(int selectedCount) {
        if (isControllerMode && apiList != null) {
            if (selectedCount > 1) {
                setTitle("接口文档预览 - 已选 " + selectedCount + "/" + apiList.size() + " 个接口");
            } else {
                setTitle("接口文档预览 - 共 " + apiList.size() + " 个接口");
            }
        }
    }

    private void setPreviewEnabled(boolean enabled) {
        previewEnabled = enabled;
        ApiDocSettings.getInstance().setPreviewEnabled(enabled);
        if (themeComboBox != null) {
            themeComboBox.setEnabled(enabled);
        }
        if (previewCardLayout != null && previewContainer != null) {
            previewCardLayout.show(previewContainer, enabled ? "preview" : "placeholder");
        }
        if (enabled) {
            updatePreview();
        }
    }

    /**
     * 更新Markdown预览
     * 使用JCEF渲染现代化HTML
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void updatePreview() {
        if (browser == null) {
            return;
        }

        if (!previewEnabled) {
            if (previewCardLayout != null && previewContainer != null) {
                previewCardLayout.show(previewContainer, "placeholder");
            }
            return;
        }
        if (previewCardLayout != null && previewContainer != null) {
            previewCardLayout.show(previewContainer, "preview");
        }

        String markdown = textArea != null ? textArea.getText() : content;
        String html = generateModernHtml(markdown);
        browser.loadHTML(html);
    }

    /**
     * 生成现代化的HTML内容
     * 包含完整的CSS样式和Markdown渲染
     *
     * @param markdown Markdown内容
     * @return 完整的HTML文档
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String generateModernHtml(String markdown) {
        String convertedHtml = convertMarkdownToHtml(markdown);

        // 获取主题颜色
        String bgColor, primaryColor, secondaryColor, h1Color, h2Color, h3Color, textColor;

        if (currentTheme == PreviewTheme.FOLLOW_IDE) {
            // 跟随IDEA主题
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color bg = scheme.getDefaultBackground();
            Color fg = scheme.getDefaultForeground();
            boolean isDark = isDarkTheme(bg);

            bgColor = colorToHex(bg);
            textColor = colorToHex(fg);
            if (isDark) {
                primaryColor = "#569cd6";
                secondaryColor = "#4ec9b0";
                h1Color = "#dcdcaa";
                h2Color = "#c586c0";
                h3Color = "#ce9178";
            } else {
                primaryColor = "#0066cc";
                secondaryColor = "#008080";
                h1Color = "#795e26";
                h2Color = "#af00db";
                h3Color = "#a31515";
            }
        } else {
            bgColor = currentTheme.getBgColor();
            primaryColor = currentTheme.getPrimaryColor();
            secondaryColor = currentTheme.getSecondaryColor();
            h1Color = currentTheme.getH1Color();
            h2Color = currentTheme.getH2Color();
            h3Color = currentTheme.getH3Color();
            Color themeBg = Color.decode(bgColor);
            textColor = isDarkTheme(themeBg) ? "#e4e4e4" : "#1f2937";
        }

        boolean compactLayout = isControllerMode;
        String lineHeight = compactLayout ? "1.7" : "1.8";
        String bodyPadding = compactLayout ? "16px" : "24px";
        String bodyPaddingSmall = compactLayout ? "12px" : "16px";
        String containerPadding = compactLayout ? "26px" : "40px";
        String containerPaddingSmall = compactLayout ? "18px" : "22px";
        String containerMaxWidth = compactLayout ? "100%" : "900px";
        String containerRadius = compactLayout ? "12px" : "16px";
        String containerRadiusSmall = compactLayout ? "10px" : "12px";
        String containerShadow = compactLayout
                ? "0 6px 20px rgba(0, 0, 0, 0.22)"
                : "0 8px 32px rgba(0, 0, 0, 0.3)";
        String h1Size = compactLayout ? "26px" : "28px";
        String h2Size = compactLayout ? "20px" : "22px";
        String h3Size = compactLayout ? "17px" : "18px";
        String h1MarginBottom = compactLayout ? "18px" : "24px";
        String h1PaddingBottom = compactLayout ? "12px" : "16px";
        String h2Margin = compactLayout ? "20px 0 12px 0" : "28px 0 16px 0";
        String h3Margin = compactLayout ? "18px 0 10px 0" : "24px 0 12px 0";
        String pMargin = compactLayout ? "10px 0" : "12px 0";
        String tableMargin = compactLayout ? "12px 0" : "16px 0";
        String cellPadding = compactLayout ? "6px 10px" : "8px 12px";
        String listMargin = compactLayout ? "10px 0 10px 22px" : "12px 0 12px 24px";
        String liMargin = compactLayout ? "6px 0" : "8px 0";
        String liPadding = compactLayout ? "6px" : "8px";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        \n" +
                "        body {\n" +
                "            font-family: 'Microsoft YaHei', 'SimHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;\n"
                +
                "            background: " + bgColor + ";\n" +
                "            color: " + textColor + ";\n" +
                "            line-height: " + lineHeight + ";\n" +
                "            padding: " + bodyPadding + ";\n" +
                "            overflow-x: hidden;\n" +
                "            min-height: 100vh;\n" +
                "        }\n" +
                "        \n" +
                "        .container {\n" +
                "            width: 100%;\n" +
                "            max-width: " + containerMaxWidth + ";\n" +
                "            margin: 0 auto;\n" +
                "            background: rgba(255, 255, 255, 0.03);\n" +
                "            backdrop-filter: blur(10px);\n" +
                "            border-radius: " + containerRadius + ";\n" +
                "            padding: " + containerPadding + ";\n" +
                "            border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                "            box-shadow: " + containerShadow + ";\n" +
                "        }\n" +
                "        \n" +
                "        @media (max-width: 980px) {\n" +
                "            body {\n" +
                "                padding: " + bodyPaddingSmall + ";\n" +
                "            }\n" +
                "            .container {\n" +
                "                padding: " + containerPaddingSmall + ";\n" +
                "                border-radius: " + containerRadiusSmall + ";\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        h1 {\n" +
                "            font-size: " + h1Size + ";\n" +
                "            color: " + h1Color + ";\n" +
                "            margin-bottom: " + h1MarginBottom + ";\n" +
                "            padding-bottom: " + h1PaddingBottom + ";\n" +
                "            border-bottom: 2px solid " + h1Color + "40;\n" +
                "            text-shadow: 0 0 20px " + h1Color + "30;\n" +
                "        }\n" +
                "        \n" +
                "        h2 {\n" +
                "            font-size: " + h2Size + ";\n" +
                "            color: " + h2Color + ";\n" +
                "            margin: " + h2Margin + ";\n" +
                "            padding-left: 12px;\n" +
                "            border-left: 4px solid " + h2Color + ";\n" +
                "        }\n" +
                "        \n" +
                "        h3 {\n" +
                "            font-size: " + h3Size + ";\n" +
                "            color: " + h3Color + ";\n" +
                "            margin: " + h3Margin + ";\n" +
                "        }\n" +
                "        \n" +
                "        p {\n" +
                "            margin: " + pMargin + ";\n" +
                "            color: " + textColor + ";\n" +
                "        }\n" +
                "        \n" +
                "        table {\n" +
                "            width: 100%;\n" +
                "            table-layout: fixed;\n" +
                "            border-collapse: collapse;\n" +
                "            margin: " + tableMargin + ";\n" +
                "            background: " + bgColor + ";\n" +
                "            border-radius: 6px;\n" +
                "            overflow: hidden;\n" +
                "            border: 1px solid " + primaryColor + "30;\n" +
                "        }\n" +
                "        \n" +
                "        th {\n" +
                "            background: " + primaryColor + ";\n" +
                "            color: white;\n" +
                "            padding: " + cellPadding + ";\n" +
                "            text-align: left;\n" +
                "            font-weight: 500;\n" +
                "            font-size: 13px;\n" +
                "            word-break: break-word;\n" +
                "            overflow-wrap: anywhere;\n" +
                "        }\n" +
                "        \n" +
                "        td {\n" +
                "            padding: " + cellPadding + ";\n" +
                "            border-bottom: 1px solid " + primaryColor + "15;\n" +
                "            color: " + textColor + ";\n" +
                "            font-size: 13px;\n" +
                "            word-break: break-word;\n" +
                "            overflow-wrap: anywhere;\n" +
                "        }\n" +
                "        \n" +
                "        tr:hover td {\n" +
                "            background: " + primaryColor + "08;\n" +
                "        }\n" +
                "        \n" +
                "        code {\n" +
                "            background: " + primaryColor + "30;\n" +
                "            color: " + secondaryColor + ";\n" +
                "            padding: 3px 8px;\n" +
                "            border-radius: 6px;\n" +
                "            font-family: 'JetBrains Mono', 'Consolas', 'Courier New', monospace;\n" +
                "            font-size: 13px;\n" +
                "            word-break: break-word;\n" +
                "            overflow-wrap: anywhere;\n" +
                "        }\n" +
                "        \n" +
                "        pre {\n" +
                "            background: linear-gradient(135deg, #0d1117 0%, #161b22 100%);\n" +
                "            border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                "            border-radius: 12px;\n" +
                "            padding: 20px;\n" +
                "            max-width: 100%;\n" +
                "            overflow-x: auto;\n" +
                "            margin: 16px 0;\n" +
                "            box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.3);\n" +
                "        }\n" +
                "        \n" +
                "        pre code {\n" +
                "            background: transparent;\n" +
                "            color: #98c379;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        \n" +
                "        ul, ol {\n" +
                "            margin: " + listMargin + ";\n" +
                "            color: " + textColor + ";\n" +
                "        }\n" +
                "        \n" +
                "        li {\n" +
                "            margin: " + liMargin + ";\n" +
                "            padding-left: " + liPadding + ";\n" +
                "        }\n" +
                "        \n" +
                "        li::marker {\n" +
                "            color: " + primaryColor + ";\n" +
                "        }\n" +
                "        \n" +
                "        hr {\n" +
                "            border: none;\n" +
                "            height: 1px;\n" +
                "            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);\n"
                +
                "            margin: 24px 0;\n" +
                "        }\n" +
                "        \n" +
                "        strong {\n" +
                "            color: " + h3Color + ";\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        \n" +
                "        a {\n" +
                "            color: " + primaryColor + ";\n" +
                "            text-decoration: none;\n" +
                "            transition: color 0.3s ease;\n" +
                "        }\n" +
                "        \n" +
                "        a:hover {\n" +
                "            color: " + secondaryColor + ";\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        \n" +
                "        /* 滚动条样式 */\n" +
                "        ::-webkit-scrollbar {\n" +
                "            width: 8px;\n" +
                "            height: 8px;\n" +
                "        }\n" +
                "        \n" +
                "        ::-webkit-scrollbar-track {\n" +
                "            background: rgba(255, 255, 255, 0.05);\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        \n" +
                "        ::-webkit-scrollbar-thumb {\n" +
                "            background: linear-gradient(135deg, " + primaryColor + " 0%, " + secondaryColor
                + " 100%);\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        \n" +
                "        ::-webkit-scrollbar-thumb:hover {\n" +
                "            background: linear-gradient(135deg, " + secondaryColor + " 0%, " + primaryColor
                + " 100%);\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                convertedHtml +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * 将Color转换为十六进制颜色字符串
     *
     * @param color Color对象
     * @return 十六进制颜色字符串
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 判断是否为深色主题
     * 通过计算背景颜色的亮度来判断
     *
     * @param bgColor 背景颜色
     * @return 是否为深色主题
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private boolean isDarkTheme(Color bgColor) {
        // 使用相对亮度公式：L = 0.299*R + 0.587*G + 0.114*B
        double brightness = 0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue();
        // 亮度低于128认为是深色主题
        return brightness < 128;
    }

    /**
     * 将Markdown转换为HTML
     * 支持标题、表格、代码块、列表、粗体等基本语法
     *
     * @param markdown Markdown内容
     * @return HTML内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String convertMarkdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inTable = false;
        StringBuilder tableContent = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 代码块处理
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>\n");
                    inCodeBlock = false;
                } else {
                    html.append("<pre><code>");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // 表格处理
            if (line.trim().startsWith("|")) {
                if (!inTable) {
                    inTable = true;
                    tableContent = new StringBuilder();
                    tableContent.append("<table>\n");
                }

                // 跳过分隔行
                if (line.contains(":---") || line.contains("---:") || line.matches(".*\\|[-:]+\\|.*")) {
                    continue;
                }

                String[] cells = line.split("\\|");
                boolean isHeader = (i + 1 < lines.length && lines[i + 1].contains("---"));

                tableContent.append("<tr>");
                for (String cell : cells) {
                    String trimmed = cell.trim();
                    if (!trimmed.isEmpty()) {
                        if (isHeader) {
                            tableContent.append("<th>").append(processInline(trimmed)).append("</th>");
                        } else {
                            tableContent.append("<td>").append(processInline(trimmed)).append("</td>");
                        }
                    }
                }
                tableContent.append("</tr>\n");
                continue;
            } else if (inTable) {
                html.append(tableContent).append("</table>\n");
                inTable = false;
            }

            // 标题
            if (line.startsWith("### ")) {
                html.append("<h3>").append(processInline(line.substring(4))).append("</h3>\n");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(processInline(line.substring(3))).append("</h2>\n");
            } else if (line.startsWith("# ")) {
                html.append("<h1>").append(processInline(line.substring(2))).append("</h1>\n");
            }
            // 分隔线
            else if (line.trim().equals("---") || line.trim().equals("***")) {
                html.append("<hr>\n");
            }
            // 无序列表
            else if (line.trim().startsWith("- ")) {
                html.append("<ul><li>").append(processInline(line.trim().substring(2))).append("</li></ul>\n");
            }
            // 普通段落
            else if (!line.trim().isEmpty()) {
                html.append("<p>").append(processInline(line)).append("</p>\n");
            }
        }

        if (inTable) {
            html.append(tableContent).append("</table>\n");
        }

        return html.toString();
    }

    /**
     * 处理行内样式
     *
     * @param text 文本
     * @return 处理后的文本
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String processInline(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String safeText = escapeHtml(text);
        // 处理行内代码
        safeText = safeText.replaceAll("`([^`]+)`", "<code>$1</code>");
        // 处理粗体
        safeText = safeText.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        return safeText;
    }

    /**
     * HTML转义
     *
     * @param text 文本
     * @return 转义后的文本
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    protected void doOKAction() {
        // 直接关闭对话框，不执行任何操作
        // 复制操作由自定义的"复制到剪贴板"按钮处理
        super.doOKAction();
    }

    /**
     * 获取编辑后的内容
     *
     * @return 编辑后的内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getEditedContent() {
        return textArea != null ? textArea.getText() : content;
    }

    @Override
    protected Action[] createActions() {
        // 返回空数组，隐藏默认按钮，使用自定义的底部按钮
        return new Action[0];
    }

    @Override
    protected void dispose() {
        // 清理定时器
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
        // 清理JCEF浏览器
        if (browser != null) {
            browser.dispose();
            browser = null;
        }
        // 清理UI组件引用，帮助GC回收
        textArea = null;
        apiListComponent = null;
        apiListModel = null;
        themeComboBox = null;
        copyAllButton = null;
        exportAllButton = null;
        previewToggle = null;
        editorScrollPane = null;
        previewContainer = null;
        super.dispose();
    }

    /**
     * 清理文件名中的非法字符
     * 移除 Windows 文件名中不允许的字符
     *
     * @param fileName 原始文件名
     * @return 清理后的文件名
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未命名";
        }
        // 移除 Windows 文件名中不允许的字符: \ / : * ? " < > |
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        // 移除前后空格
        sanitized = sanitized.trim();
        // 如果为空，返回默认值
        if (sanitized.isEmpty()) {
            return "未命名";
        }
        // 限制长度（Windows 文件名最大 255 字符）
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized;
    }
}
