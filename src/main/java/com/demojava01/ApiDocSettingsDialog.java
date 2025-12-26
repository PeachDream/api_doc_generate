package com.demojava01;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API文档生成器设置对话框
 * 支持IDEA主题自适应
 *
 * @author peach
 * @since 2025/12/26 | V1.0.0
 */
public class ApiDocSettingsDialog extends DialogWrapper {

    /** 当前项目 */
    private final Project project;
    /** 显示接口调用位置复选框 */
    private JCheckBox showCallLocationCheckBox;
    /** 使用Git分支作为版本号复选框 */
    private JCheckBox useGitBranchCheckBox;
    /** 默认作者输入框 */
    private JTextField authorField;
    /** 显示请求JSON复选框 */
    private JCheckBox showRequestJsonCheckBox;
    /** 显示返回JSON复选框 */
    private JCheckBox showResponseJsonCheckBox;
    /** 排除父类列表模型 */
    private DefaultListModel<String> excludedClassesListModel;
    /** 排除父类列表 */
    private JList<String> excludedClassesList;
    /** 默认导出路径输入框 */
    private JTextField exportPathField;

    // ========== 主题颜色（根据IDEA主题动态设置）==========
    /** 是否为深色主题 */
    private boolean isDarkTheme;
    /** 主背景色 */
    private Color bgColor;
    /** 卡片背景色 */
    private Color cardBgColor;
    /** 边框颜色 */
    private Color borderColor;
    /** 主文字颜色 */
    private Color textColor;
    /** 次要文字颜色 */
    private Color secondaryTextColor;
    /** 强调色（蓝色系） */
    private Color accentBlue;
    /** 强调色（橙色系） */
    private Color accentOrange;
    /** 强调色（绿色系） */
    private Color accentGreen;
    /** 强调色（红色系） */
    private Color accentRed;
    /** 输入框背景色 */
    private Color inputBgColor;
    /** 列表选中色 */
    private Color listSelectionColor;
    /** 列表交替行颜色 */
    private Color listAlternateColor;

    /**
     * 构造函数
     *
     * @param project 项目
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public ApiDocSettingsDialog(@Nullable Project project) {
        super(project, true);
        this.project = project;
        initThemeColors();
        setTitle("⚙ 接口文档生成器设置");
        // 验证并清理无效的配置
        validateAndCleanConfigOnOpen();
        init();
    }

    /**
     * 打开对话框时验证并清理无效配置
     * 检查配置的类是否存在，检查配置的字段是否仍然有效
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void validateAndCleanConfigOnOpen() {
        if (project == null) {
            return;
        }

        // 在后台线程中验证配置
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                ApiDocSettings.ConfigValidationResult result = ApiDocSettings.getInstance()
                        .validateAndCleanConfig(project);

                // 如果有变更，在EDT线程中通知用户
                if (result.hasChanges && !result.changeDetails.isEmpty()) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        Messages.showInfoMessage(project,
                                "以下配置项已自动更新（原配置的类或字段已被删除或重命名）：\n\n" + result.changeDetails,
                                "配置已自动更新");
                    });
                }
            });
        });
    }

    /**
     * 初始化主题颜色
     * 根据IDEA当前主题自动选择配色方案
     * 参考现代UI设计，使用柔和的蓝色作为主色调
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void initThemeColors() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color ideaBg = scheme.getDefaultBackground();

        // 通过背景颜色亮度判断是否为深色主题
        double brightness = 0.299 * ideaBg.getRed() + 0.587 * ideaBg.getGreen() + 0.114 * ideaBg.getBlue();
        isDarkTheme = brightness < 128;

        if (isDarkTheme) {
            // 深色主题配色 - 柔和的深色
            bgColor = new Color(40, 44, 52);
            cardBgColor = new Color(50, 54, 62);
            borderColor = new Color(70, 75, 85);
            textColor = new Color(230, 230, 230);
            secondaryTextColor = new Color(150, 155, 165);
            accentBlue = new Color(91, 143, 249); // #5B8FF9 - 主色调
            accentOrange = new Color(250, 173, 20); // 警告色
            accentGreen = new Color(82, 196, 26); // 成功色
            accentRed = new Color(245, 108, 108); // 删除色
            inputBgColor = new Color(45, 49, 57);
            listSelectionColor = new Color(91, 143, 249, 60);
            listAlternateColor = new Color(55, 59, 67);
        } else {
            // 浅色主题配色 - 参考现代UI设计
            bgColor = new Color(247, 248, 250); // 浅灰背景
            cardBgColor = new Color(255, 255, 255); // 纯白卡片
            borderColor = new Color(230, 232, 235); // 浅色边框
            textColor = new Color(51, 51, 51); // 深灰文字
            secondaryTextColor = new Color(140, 145, 155);
            accentBlue = new Color(91, 143, 249); // #5B8FF9 - 主色调
            accentOrange = new Color(250, 140, 22); // 警告色
            accentGreen = new Color(82, 196, 26); // 成功色
            accentRed = new Color(255, 77, 79); // 删除色
            inputBgColor = new Color(255, 255, 255);
            listSelectionColor = new Color(91, 143, 249, 40);
            listAlternateColor = new Color(250, 251, 252);
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 主面板 - 自适应主题
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        // 使用屏幕相对尺寸，宽度为屏幕的40%，高度为屏幕的60%，限制最大最小值
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(Math.max((int) (screenSize.width * 0.4), 550), 800);
        int dialogHeight = Math.min(Math.max((int) (screenSize.height * 0.6), 500), 750);
        mainPanel.setPreferredSize(new Dimension(dialogWidth, dialogHeight));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        ApiDocSettings settings = ApiDocSettings.getInstance();

        // 标题区域
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 内容区域
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(bgColor);

        // 基础设置卡片
        JPanel basicCard = createBasicSettingsCard(settings);
        contentPanel.add(basicCard);
        contentPanel.add(Box.createVerticalStrut(15));

        // 排除类卡片
        JPanel excludeCard = createExcludeClassCard(settings);
        contentPanel.add(excludeCard);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 底部提示
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * 创建标题面板
     *
     * @return 标题面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("接口文档生成器设置");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        titleLabel.setForeground(accentBlue);
        panel.add(titleLabel, BorderLayout.WEST);

        // 主题指示器
        JLabel themeIndicator = new JLabel(isDarkTheme ? "深色模式" : "浅色模式");
        themeIndicator.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        themeIndicator.setForeground(secondaryTextColor);
        panel.add(themeIndicator, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建基础设置卡片
     *
     * @param settings 设置对象
     * @return 基础设置卡片面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createBasicSettingsCard(ApiDocSettings settings) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(cardBgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        // 卡片标题
        JLabel cardTitle = new JLabel("基础设置");
        cardTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        cardTitle.setForeground(accentBlue);
        card.add(cardTitle, BorderLayout.NORTH);

        // 设置内容
        JPanel settingsContent = new JPanel(new GridBagLayout());
        settingsContent.setBackground(cardBgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 10);

        int row = 0;

        // 复选框设置组
        JPanel checkboxPanel = new JPanel(new GridLayout(2, 2, 20, 10));
        checkboxPanel.setBackground(cardBgColor);

        showCallLocationCheckBox = createStyledCheckBox("显示接口调用位置", settings.isShowCallLocation());
        useGitBranchCheckBox = createStyledCheckBox("使用Git分支作为版本号", settings.isUseGitBranchAsVersion());
        showRequestJsonCheckBox = createStyledCheckBox("显示请求参数JSON", settings.isShowRequestJson());
        showResponseJsonCheckBox = createStyledCheckBox("显示返回参数JSON", settings.isShowResponseJson());

        checkboxPanel.add(showCallLocationCheckBox);
        checkboxPanel.add(useGitBranchCheckBox);
        checkboxPanel.add(showRequestJsonCheckBox);
        checkboxPanel.add(showResponseJsonCheckBox);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 15, 0);
        settingsContent.add(checkboxPanel, gbc);

        // 默认作者
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(8, 0, 8, 15);
        JLabel authorLabel = new JLabel("默认作者");
        authorLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        authorLabel.setForeground(textColor);
        settingsContent.add(authorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 0, 8, 0);
        authorField = new JTextField(settings.getDefaultAuthor());
        authorField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        authorField.setPreferredSize(new Dimension(220, 34));
        authorField.setBackground(inputBgColor);
        authorField.setForeground(textColor);
        authorField.setCaretColor(accentBlue);
        authorField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        settingsContent.add(authorField, gbc);

        // 默认导出路径
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.insets = new Insets(8, 0, 8, 15);
        JLabel exportPathLabel = new JLabel("导出路径");
        exportPathLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        exportPathLabel.setForeground(textColor);
        settingsContent.add(exportPathLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 0, 8, 0);
        JPanel exportPathPanel = new JPanel(new BorderLayout(5, 0));
        exportPathPanel.setBackground(cardBgColor);

        exportPathField = new JTextField(settings.getExportPath());
        exportPathField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        exportPathField.setBackground(inputBgColor);
        exportPathField.setForeground(textColor);
        exportPathField.setCaretColor(accentBlue);
        exportPathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        exportPathField.setToolTipText("留空则每次导出时选择路径，导出后会自动记住路径");
        exportPathPanel.add(exportPathField, BorderLayout.CENTER);

        // 浏览按钮
        JButton browseButton = new JButton("浏览");
        browseButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        browseButton.setBackground(accentBlue);
        browseButton.setForeground(Color.WHITE);
        browseButton.setFocusPainted(false);
        browseButton.setBorderPainted(false);
        browseButton.setOpaque(true);
        browseButton.setPreferredSize(new Dimension(60, 28));
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        browseButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        browseButton.addActionListener(e -> onBrowseExportPath());
        exportPathPanel.add(browseButton, BorderLayout.EAST);

        settingsContent.add(exportPathPanel, gbc);

        card.add(settingsContent, BorderLayout.CENTER);
        return card;
    }

    /**
     * 创建样式化的复选框
     *
     * @param text     复选框文本
     * @param selected 是否选中
     * @return 样式化的复选框
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setSelected(selected);
        checkBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        checkBox.setForeground(textColor);
        checkBox.setBackground(cardBgColor);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return checkBox;
    }

    /**
     * 创建排除类卡片
     *
     * @param settings 设置对象
     * @return 排除类卡片面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createExcludeClassCard(ApiDocSettings settings) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(cardBgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        // 卡片标题
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(cardBgColor);

        JLabel cardTitle = new JLabel("排除父类");
        cardTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        cardTitle.setForeground(accentOrange);
        headerPanel.add(cardTitle, BorderLayout.WEST);

        JLabel tipLabel = new JLabel("双击可编辑排除的字段");
        tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        tipLabel.setForeground(secondaryTextColor);
        headerPanel.add(tipLabel, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);

        // 内容区域
        JPanel contentPanel = new JPanel(new BorderLayout(12, 0));
        contentPanel.setBackground(cardBgColor);

        // 列表
        excludedClassesListModel = new DefaultListModel<>();
        String savedClasses = settings.getExcludedParentClasses();
        if (savedClasses != null && !savedClasses.isEmpty()) {
            Arrays.stream(savedClasses.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(excludedClassesListModel::addElement);
        }

        excludedClassesList = new JList<>(excludedClassesListModel);
        excludedClassesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        excludedClassesList.setCellRenderer(new ExcludedClassListCellRenderer());
        excludedClassesList.setFixedCellHeight(42);
        excludedClassesList.setBackground(isDarkTheme ? inputBgColor : listAlternateColor);

        // 双击编辑字段
        excludedClassesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = excludedClassesList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String className = excludedClassesListModel.get(index);
                        onEditFieldsForClass(className);
                    }
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(excludedClassesList);
        listScrollPane.setPreferredSize(new Dimension(450, 220));
        listScrollPane.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        contentPanel.add(listScrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(cardBgColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton addButton = createStyledButton("添加", accentGreen);
        JButton editButton = createStyledButton("编辑", accentBlue);
        JButton removeButton = createStyledButton("删除", accentRed);
        JButton clearButton = createStyledButton("清空", secondaryTextColor);

        addButton.addActionListener(e -> onAddClass());
        editButton.addActionListener(e -> onEditSelectedClass());
        removeButton.addActionListener(e -> onRemoveClass());
        clearButton.addActionListener(e -> onClearClasses());

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(clearButton);

        contentPanel.add(buttonPanel, BorderLayout.EAST);
        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    /**
     * 创建样式化的按钮
     * 按钮使用彩色背景 + 白色文字
     *
     * @param text    按钮文本
     * @param bgColor 背景颜色
     * @return 样式化的按钮
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(80, 32));
        button.setMaximumSize(new Dimension(80, 32));
        button.setBackground(bgColor);
        // 彩色背景始终使用白色文字
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 确保按钮颜色在所有LAF下都能正确显示
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        // 添加hover效果
        Color originalBg = bgColor;
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(originalBg.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        return button;
    }

    /**
     * 创建底部面板
     *
     * @return 底部面板
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JLabel tipLabel = new JLabel("设置将在点击确定后保存，重新生成文档后生效");
        tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        tipLabel.setForeground(secondaryTextColor);
        panel.add(tipLabel, BorderLayout.WEST);

        return panel;
    }

    /**
     * 浏览导出路径按钮点击事件
     * 使用 IntelliJ Platform 的文件选择器选择目录
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onBrowseExportPath() {
        // 使用 IntelliJ Platform 的目录选择器
        com.intellij.openapi.fileChooser.FileChooserDescriptor descriptor = com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
                .createSingleFolderDescriptor();
        descriptor.setTitle("选择导出目录");
        descriptor.setDescription("选择文档导出的默认保存目录");

        // 获取当前路径作为默认目录
        com.intellij.openapi.vfs.VirtualFile currentDir = null;
        String currentPath = exportPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            java.io.File file = new java.io.File(currentPath);
            // 如果是文件路径，获取其父目录
            java.io.File dir = file.isDirectory() ? file : file.getParentFile();
            if (dir != null && dir.exists()) {
                currentDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                        .findFileByIoFile(dir);
            }
        }

        // 显示文件选择对话框
        com.intellij.openapi.vfs.VirtualFile[] selected = com.intellij.openapi.fileChooser.FileChooser
                .chooseFiles(descriptor, project, currentDir);

        if (selected.length > 0) {
            String selectedPath = selected[0].getPath();
            exportPathField.setText(selectedPath);

            // 立即保存到设置
            ApiDocSettings.getInstance().setExportPath(selectedPath);
        }
    }

    /**
     * 现代风格的类列表渲染器
     */
    private static class ModernClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String) {
                String qualifiedName = (String) value;
                int lastDot = qualifiedName.lastIndexOf('.');

                String simpleName = lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
                String packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";

                setText("<html><span style='font-size: 12px; font-weight: bold; color: " +
                        (isSelected ? "#FFFFFF" : "#E8E8E8") + ";'>" + simpleName + "</span>" +
                        (packageName.isEmpty() ? ""
                                : "<span style='font-size: 10px; color: " +
                                        (isSelected ? "#CCCCCC" : "#888888") + ";'> (" + packageName + ")</span>")
                        +
                        "</html>");
            }

            setBackground(isSelected ? new Color(75, 110, 175)
                    : (index % 2 == 0 ? new Color(40, 40, 40) : new Color(45, 45, 45)));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            return this;
        }
    }

    /**
     * 添加类按钮点击事件
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onAddClass() {
        // 获取当前已有的类列表
        java.util.List<String> existingClasses = new java.util.ArrayList<>();
        for (int i = 0; i < excludedClassesListModel.size(); i++) {
            existingClasses.add(excludedClassesListModel.get(i));
        }

        // 使用自定义的类搜索对话框，传入现有类列表
        ClassSearchDialog searchDialog = new ClassSearchDialog(project, existingClasses);
        if (searchDialog.showAndGet()) {
            // 获取所有已选择的类
            java.util.List<String> selectedClasses = searchDialog.getSelectedClasses();

            // 清空并重新添加
            excludedClassesListModel.clear();
            for (String className : selectedClasses) {
                excludedClassesListModel.addElement(className);
            }
        }
    }

    /**
     * 编辑选中的类的排除字段
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onEditSelectedClass() {
        int selectedIndex = excludedClassesList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String className = excludedClassesListModel.get(selectedIndex);
            onEditFieldsForClass(className);
        } else {
            Messages.showInfoMessage(project, "请先选择一个类", "提示");
        }
    }

    /**
     * 编辑指定类的排除字段
     * 如果类不存在，提示用户是否移除该配置
     * 打开编辑对话框时，自动清理已删除的字段配置
     *
     * @param className 类名
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void onEditFieldsForClass(String className) {
        if (project == null) {
            Messages.showErrorDialog("项目为空，无法获取字段信息", "错误");
            return;
        }

        // 查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            // 类已不存在，询问用户是否移除配置
            int result = Messages.showYesNoDialog(
                    project,
                    "类 '" + className + "' 不存在或已被删除/重命名。\n\n是否从排除列表中移除此类？",
                    "类不存在",
                    "移除", "取消",
                    Messages.getWarningIcon());
            if (result == Messages.YES) {
                // 从列表中移除
                for (int i = 0; i < excludedClassesListModel.size(); i++) {
                    if (excludedClassesListModel.get(i).equals(className)) {
                        excludedClassesListModel.remove(i);
                        break;
                    }
                }
                // 移除字段配置
                ApiDocSettings.getInstance().removeExcludedFieldsForClass(className);
            }
            return;
        }

        // 获取所有字段
        java.util.List<FieldItem> allFields = new java.util.ArrayList<>();
        collectFields(psiClass, allFields, new java.util.HashSet<>());

        if (allFields.isEmpty()) {
            Messages.showInfoMessage(project, "该类没有可排除的字段", "提示");
            return;
        }

        // 获取当前已排除的字段
        java.util.Set<String> currentExcluded = ApiDocSettings.getInstance().getExcludedFieldsForClass(className);

        // 验证并清理无效的字段配置
        if (currentExcluded != null) {
            java.util.Set<String> validFieldNames = new java.util.HashSet<>();
            for (FieldItem field : allFields) {
                validFieldNames.add(field.name);
            }

            java.util.List<String> invalidFields = new java.util.ArrayList<>();
            java.util.Set<String> validExcluded = new java.util.LinkedHashSet<>();
            for (String fieldName : currentExcluded) {
                if (validFieldNames.contains(fieldName)) {
                    validExcluded.add(fieldName);
                } else {
                    invalidFields.add(fieldName);
                }
            }

            // 如果有无效的字段，自动清理并提示
            if (!invalidFields.isEmpty()) {
                currentExcluded = validExcluded;
                ApiDocSettings.getInstance().updateExcludedFieldsForClass(className, validExcluded);
                Messages.showInfoMessage(project,
                        "以下字段已不存在，已自动从配置中移除：\n" + String.join(", ", invalidFields),
                        "字段配置已更新");
            }
        }

        // 打开字段选择对话框
        FieldSelectDialog dialog = new FieldSelectDialog(project, className, allFields, currentExcluded);
        if (dialog.showAndGet()) {
            java.util.Set<String> selectedFields = dialog.getSelectedFields();
            if (dialog.isAllSelected()) {
                ApiDocSettings.getInstance().updateExcludedFieldsForClass(className, null);
            } else {
                ApiDocSettings.getInstance().updateExcludedFieldsForClass(className, selectedFields);
            }
            // 刷新列表显示
            excludedClassesList.repaint();
        }
    }

    /**
     * 收集类及其父类的所有字段
     *
     * @param psiClass       类
     * @param fields         字段列表
     * @param visitedClasses 已访问的类（防止循环继承）
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private void collectFields(PsiClass psiClass, java.util.List<FieldItem> fields,
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
        for (com.intellij.psi.PsiField field : psiClass.getFields()) {
            // 跳过静态字段和final常量
            if (field.hasModifierProperty(com.intellij.psi.PsiModifier.STATIC)) {
                continue;
            }

            String fieldName = field.getName();
            String fieldType = field.getType().getPresentableText();
            String description = getFieldDescription(field);
            String fromClass = psiClass.getName();

            fields.add(new FieldItem(fieldName, fieldType, description, fromClass));
        }

        // 递归处理父类
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            collectFields(superClass, fields, visitedClasses);
        }
    }

    /**
     * 获取字段描述（从注释或注解中提取）
     *
     * @param field 字段
     * @return 描述
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private String getFieldDescription(com.intellij.psi.PsiField field) {
        // 尝试从文档注释获取
        com.intellij.psi.javadoc.PsiDocComment docComment = field.getDocComment();
        if (docComment != null) {
            String text = docComment.getText();
            // 简单提取描述
            text = text.replaceAll("/\\*\\*", "").replaceAll("\\*/", "").replaceAll("\\*", "").trim();
            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("@")) {
                    return trimmed;
                }
            }
        }

        // 尝试从@ApiModelProperty注解获取
        com.intellij.psi.PsiAnnotation apiModelProp = field.getAnnotation("io.swagger.annotations.ApiModelProperty");
        if (apiModelProp != null) {
            com.intellij.psi.PsiAnnotationMemberValue value = apiModelProp.findAttributeValue("value");
            if (value != null) {
                String text = value.getText().replace("\"", "");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        // 尝试从@Schema注解获取
        com.intellij.psi.PsiAnnotation schema = field.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        if (schema != null) {
            com.intellij.psi.PsiAnnotationMemberValue desc = schema.findAttributeValue("description");
            if (desc != null) {
                String text = desc.getText().replace("\"", "");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return "";
    }

    /**
     * 字段信息类
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    static class FieldItem {
        final String name;
        final String type;
        final String description;
        final String fromClass;

        FieldItem(String name, String type, String description, String fromClass) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.fromClass = fromClass;
        }
    }

    /**
     * 删除选中的类
     *
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private void onRemoveClass() {
        int selectedIndex = excludedClassesList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String className = excludedClassesListModel.get(selectedIndex);
            // 同时删除字段配置
            ApiDocSettings.getInstance().removeExcludedFieldsForClass(className);
            excludedClassesListModel.remove(selectedIndex);
        }
    }

    /**
     * 清空所有排除的类
     *
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private void onClearClasses() {
        if (excludedClassesListModel.size() > 0) {
            int result = Messages.showYesNoDialog(
                    project,
                    "确定要清空所有排除的类吗？",
                    "确认",
                    Messages.getQuestionIcon());
            if (result == Messages.YES) {
                // 清空字段配置
                ApiDocSettings.getInstance().setExcludedFieldsMap("");
                excludedClassesListModel.clear();
            }
        }
    }

    @Override
    protected void doOKAction() {
        // 保存设置
        ApiDocSettings settings = ApiDocSettings.getInstance();
        settings.setShowCallLocation(showCallLocationCheckBox.isSelected());
        settings.setUseGitBranchAsVersion(useGitBranchCheckBox.isSelected());
        settings.setShowRequestJson(showRequestJsonCheckBox.isSelected());
        settings.setShowResponseJson(showResponseJsonCheckBox.isSelected());
        settings.setDefaultAuthor(authorField.getText().trim());
        if (exportPathField != null) {
            settings.setExportPath(exportPathField.getText().trim());
        }

        // 将列表转换为逗号分隔的字符串
        Set<String> classSet = new LinkedHashSet<>();
        for (int i = 0; i < excludedClassesListModel.size(); i++) {
            classSet.add(excludedClassesListModel.get(i));
        }
        String excludedClasses = classSet.stream().collect(Collectors.joining(","));
        settings.setExcludedParentClasses(excludedClasses);

        super.doOKAction();
    }

    /**
     * 类列表单元格渲染器
     * 显示类名（灰色显示包名）
     *
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private static class ClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String) {
                String qualifiedName = (String) value;
                int lastDot = qualifiedName.lastIndexOf('.');
                if (lastDot > 0) {
                    String packageName = qualifiedName.substring(0, lastDot);
                    String simpleName = qualifiedName.substring(lastDot + 1);
                    // 使用HTML格式显示：类名（包名）
                    setText("<html><b>" + simpleName + "</b> <font color='gray'>(" + packageName + ")</font></html>");
                } else {
                    setText("<html><b>" + qualifiedName + "</b></html>");
                }
            }

            return this;
        }
    }

    /**
     * 类搜索对话框
     * 输入类名实时搜索，支持连续添加多个类
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private static class ClassSearchDialog extends DialogWrapper {

        /** 项目 */
        private final Project project;
        /** 搜索输入框 */
        private JTextField searchField;
        /** 搜索结果列表模型 */
        private DefaultListModel<String> resultListModel;
        /** 搜索结果列表 */
        private JList<String> resultList;
        /** 已选择的类列表模型 */
        private DefaultListModel<String> selectedListModel;
        /** 已选择的类列表 */
        private JList<String> selectedList;
        /** 搜索定时器 */
        private Timer searchTimer;
        /** 状态标签 */
        private JLabel statusLabel;
        /** 已添加数量标签 */
        private JLabel selectedCountLabel;
        /** 是否为深色主题 */
        private boolean isDarkTheme;
        /** 主背景色 */
        private Color bgColor;
        /** 卡片背景色 */
        private Color cardBgColor;
        /** 边框颜色 */
        private Color borderColor;
        /** 主文字颜色 */
        private Color textColor;
        /** 次要文字颜色 */
        private Color secondaryTextColor;
        /** 输入框背景色 */
        private Color inputBgColor;
        /** 列表背景色 */
        private Color listBgColor;
        /** 列表交替行颜色 */
        private Color listAltBgColor;
        /** 列表选中背景色 */
        private Color listSelectionBg;
        /** 列表选中文字颜色 */
        private Color listSelectionFg;
        /** 强调色（蓝色系） */
        private Color accentBlue;
        /** 强调色（绿色系） */
        private Color accentGreen;
        /** 强调色（红色系） */
        private Color accentRed;

        /**
         * 构造函数
         *
         * @param project 项目
         */
        public ClassSearchDialog(@Nullable Project project) {
            super(project, true);
            this.project = project;
            this.selectedListModel = new DefaultListModel<>();
            initThemeColors();
            setTitle("搜索并添加排除类");
            setOKButtonText("确定添加");
            setCancelButtonText("取消");
            init();
        }

        /**
         * 传入已有的排除类列表（用于显示和避免重复添加）
         *
         * @param project         项目
         * @param existingClasses 已存在的类列表
         */
        public ClassSearchDialog(@Nullable Project project, java.util.List<String> existingClasses) {
            super(project, true);
            this.project = project;
            this.selectedListModel = new DefaultListModel<>();
            if (existingClasses != null) {
                for (String className : existingClasses) {
                    selectedListModel.addElement(className);
                }
            }
            initThemeColors();
            setTitle("搜索并添加排除类");
            setOKButtonText("确定添加");
            setCancelButtonText("取消");
            init();
        }

        private void initThemeColors() {
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color ideaBg = scheme.getDefaultBackground();

            double brightness = 0.299 * ideaBg.getRed() + 0.587 * ideaBg.getGreen() + 0.114 * ideaBg.getBlue();
            isDarkTheme = brightness < 128;

            if (isDarkTheme) {
                bgColor = new Color(43, 43, 43);
                cardBgColor = new Color(50, 52, 58);
                borderColor = new Color(70, 75, 85);
                textColor = new Color(220, 220, 220);
                secondaryTextColor = new Color(150, 155, 165);
                inputBgColor = new Color(45, 49, 57);
                listBgColor = new Color(52, 55, 61);
                listAltBgColor = new Color(56, 59, 66);
                listSelectionBg = new Color(91, 143, 249);
                listSelectionFg = Color.WHITE;
                accentBlue = new Color(91, 143, 249);
                accentGreen = new Color(82, 196, 26);
                accentRed = new Color(245, 108, 108);
            } else {
                bgColor = new Color(247, 248, 250);
                cardBgColor = new Color(255, 255, 255);
                borderColor = new Color(230, 232, 235);
                textColor = new Color(51, 51, 51);
                secondaryTextColor = new Color(140, 145, 155);
                inputBgColor = new Color(255, 255, 255);
                listBgColor = new Color(250, 251, 252);
                listAltBgColor = new Color(245, 246, 248);
                listSelectionBg = new Color(91, 143, 249);
                listSelectionFg = Color.WHITE;
                accentBlue = new Color(91, 143, 249);
                accentGreen = new Color(82, 196, 26);
                accentRed = new Color(245, 108, 108);
            }
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setPreferredSize(new Dimension(750, 500));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            mainPanel.setBackground(bgColor);

            // 顶部搜索区域
            JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
            searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            searchPanel.setBackground(bgColor);

            JLabel searchLabel = new JLabel("搜索类名：");
            searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD, 13f));
            searchLabel.setForeground(textColor);
            searchPanel.add(searchLabel, BorderLayout.WEST);

            searchField = new JTextField();
            searchField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            searchField.setToolTipText("输入类名进行实时搜索");
            searchField.setBackground(inputBgColor);
            searchField.setForeground(textColor);
            searchField.setCaretColor(accentBlue);
            searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            searchPanel.add(searchField, BorderLayout.CENTER);

            mainPanel.add(searchPanel, BorderLayout.NORTH);

            // 中间区域：左右两个列表
            JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
            centerPanel.setBackground(bgColor);

            // 左侧：搜索结果
            JPanel leftPanel = createSearchResultPanel();
            centerPanel.add(leftPanel);

            // 右侧：已添加的类
            JPanel rightPanel = createSelectedClassesPanel();
            centerPanel.add(rightPanel);

            mainPanel.add(centerPanel, BorderLayout.CENTER);

            // 底部提示
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            bottomPanel.setBackground(bgColor);
            JLabel tipLabel = new JLabel("提示：双击或点击「添加」按钮将类添加到右侧列表，完成后点击「确定添加」");
            tipLabel.setForeground(secondaryTextColor);
            bottomPanel.add(tipLabel, BorderLayout.WEST);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);

            // 设置搜索监听
            setupSearchListener();
            setupKeyboardNavigation();

            return mainPanel;
        }

        /**
         * 创建搜索结果面板
         */
        private JPanel createSearchResultPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.setBackground(cardBgColor);

            // 标题栏
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(cardBgColor);
            JLabel titleLabel = new JLabel("搜索结果");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLabel.setForeground(accentBlue);
            headerPanel.add(titleLabel, BorderLayout.WEST);

            statusLabel = new JLabel("请输入类名开始搜索");
            statusLabel.setForeground(secondaryTextColor);
            statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
            headerPanel.add(statusLabel, BorderLayout.EAST);
            panel.add(headerPanel, BorderLayout.NORTH);

            // 搜索结果列表
            resultListModel = new DefaultListModel<>();
            resultList = new JList<>(resultListModel);
            resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            resultList.setCellRenderer(new ThemeClassListCellRenderer());
            resultList.setFixedCellHeight(32);
            resultList.setBackground(listBgColor);
            resultList.setForeground(textColor);
            resultList.setSelectionBackground(listSelectionBg);
            resultList.setSelectionForeground(listSelectionFg);

            JScrollPane scrollPane = new JScrollPane(resultList);
            scrollPane.getViewport().setBackground(listBgColor);
            scrollPane.setBorder(BorderFactory.createLineBorder(borderColor, 1));
            panel.add(scrollPane, BorderLayout.CENTER);

            // 添加按钮
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
            buttonPanel.setBackground(cardBgColor);
            JButton addButton = new JButton("添加选中项 →");
            addButton.setFont(addButton.getFont().deriveFont(12f));
            addButton.setPreferredSize(new Dimension(140, 32));
            addButton.setBackground(accentBlue);
            addButton.setForeground(Color.WHITE);
            addButton.setFocusPainted(false);
            addButton.setBorderPainted(false);
            addButton.setOpaque(true);
            addButton.setContentAreaFilled(true);
            addButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            addButton.addActionListener(e -> addSelectedClass());
            buttonPanel.add(addButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            // 双击添加
            resultList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        addSelectedClass();
                    }
                }
            });

            return panel;
        }

        /**
         * 创建已选择类面板
         */
        private JPanel createSelectedClassesPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.setBackground(cardBgColor);

            // 标题栏
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(cardBgColor);
            JLabel titleLabel = new JLabel("已添加的排除类");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLabel.setForeground(accentGreen);
            headerPanel.add(titleLabel, BorderLayout.WEST);

            selectedCountLabel = new JLabel("共 " + selectedListModel.size() + " 个");
            selectedCountLabel.setForeground(secondaryTextColor);
            selectedCountLabel.setFont(selectedCountLabel.getFont().deriveFont(11f));
            headerPanel.add(selectedCountLabel, BorderLayout.EAST);
            panel.add(headerPanel, BorderLayout.NORTH);

            // 已选择列表
            selectedList = new JList<>(selectedListModel);
            selectedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            selectedList.setCellRenderer(new ThemeClassListCellRenderer());
            selectedList.setFixedCellHeight(32);
            selectedList.setBackground(listBgColor);
            selectedList.setForeground(textColor);
            selectedList.setSelectionBackground(listSelectionBg);
            selectedList.setSelectionForeground(listSelectionFg);

            JScrollPane scrollPane = new JScrollPane(selectedList);
            scrollPane.getViewport().setBackground(listBgColor);
            scrollPane.setBorder(BorderFactory.createLineBorder(borderColor, 1));
            panel.add(scrollPane, BorderLayout.CENTER);

            // 删除按钮
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setBackground(cardBgColor);

            JButton removeButton = new JButton("删除选中");
            removeButton.setFont(removeButton.getFont().deriveFont(12f));
            removeButton.setPreferredSize(new Dimension(100, 32));
            removeButton.setBackground(accentRed);
            removeButton.setForeground(Color.WHITE);
            removeButton.setFocusPainted(false);
            removeButton.setBorderPainted(false);
            removeButton.setOpaque(true);
            removeButton.setContentAreaFilled(true);
            removeButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            removeButton.addActionListener(e -> removeSelectedClass());
            buttonPanel.add(removeButton);

            JButton clearButton = new JButton("清空全部");
            clearButton.setFont(clearButton.getFont().deriveFont(12f));
            clearButton.setPreferredSize(new Dimension(100, 32));
            clearButton.setBackground(new Color(108, 117, 125));
            clearButton.setForeground(Color.WHITE);
            clearButton.setFocusPainted(false);
            clearButton.setBorderPainted(false);
            clearButton.setOpaque(true);
            clearButton.setContentAreaFilled(true);
            clearButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            clearButton.addActionListener(e -> clearAllClasses());
            buttonPanel.add(clearButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            return panel;
        }

        /**
         * 设置搜索监听器
         */
        private void setupSearchListener() {
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    scheduleSearch();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    scheduleSearch();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    scheduleSearch();
                }

                private void scheduleSearch() {
                    if (searchTimer != null) {
                        searchTimer.stop();
                    }
                    searchTimer = new Timer(150, evt -> doSearchAsync());
                    searchTimer.setRepeats(false);
                    searchTimer.start();
                }
            });
        }

        /**
         * 设置键盘导航
         */
        private void setupKeyboardNavigation() {
            // 回车添加
            searchField.addActionListener(e -> {
                if (resultList.getSelectedValue() != null) {
                    addSelectedClass();
                } else if (resultListModel.size() > 0) {
                    resultList.setSelectedIndex(0);
                }
            });

            // 上下键选择
            searchField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                        int index = resultList.getSelectedIndex();
                        if (index < resultListModel.size() - 1) {
                            resultList.setSelectedIndex(index + 1);
                            resultList.ensureIndexIsVisible(index + 1);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        int index = resultList.getSelectedIndex();
                        if (index > 0) {
                            resultList.setSelectedIndex(index - 1);
                            resultList.ensureIndexIsVisible(index - 1);
                        }
                        e.consume();
                    }
                }
            });
        }

        /**
         * 添加选中的类到已选列表
         */
        private void addSelectedClass() {
            String selected = resultList.getSelectedValue();
            if (selected == null) {
                return;
            }

            // 检查是否已存在
            for (int i = 0; i < selectedListModel.size(); i++) {
                if (selectedListModel.get(i).equals(selected)) {
                    statusLabel.setText("该类已添加");
                    statusLabel.setForeground(new Color(220, 150, 100));
                    return;
                }
            }

            selectedListModel.addElement(selected);
            updateSelectedCount();
            statusLabel.setText("已添加: " + getSimpleClassName(selected));
            statusLabel.setForeground(new Color(100, 180, 100));
        }

        /**
         * 删除选中的已添加类
         */
        private void removeSelectedClass() {
            int index = selectedList.getSelectedIndex();
            if (index >= 0) {
                selectedListModel.remove(index);
                updateSelectedCount();
            }
        }

        /**
         * 清空所有已添加的类
         */
        private void clearAllClasses() {
            if (selectedListModel.size() > 0) {
                selectedListModel.clear();
                updateSelectedCount();
            }
        }

        /**
         * 更新已添加数量标签
         */
        private void updateSelectedCount() {
            selectedCountLabel.setText("共 " + selectedListModel.size() + " 个");
        }

        /**
         * 获取简单类名
         */
        private String getSimpleClassName(String qualifiedName) {
            int lastDot = qualifiedName.lastIndexOf('.');
            return lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        }

        /**
         * 异步执行搜索
         */
        private void doSearchAsync() {
            String searchText = searchField.getText().trim();

            if (searchText.isEmpty() || project == null) {
                resultListModel.clear();
                statusLabel.setText("请输入类名开始搜索");
                statusLabel.setForeground(new Color(128, 128, 128));
                return;
            }

            if (searchText.length() < 2) {
                statusLabel.setText("请至少输入2个字符");
                statusLabel.setForeground(new Color(180, 180, 100));
                return;
            }

            statusLabel.setText("搜索中...");
            statusLabel.setForeground(new Color(100, 150, 220));

            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    java.util.List<String> results = searchClasses(searchText);

                    SwingUtilities.invokeLater(() -> {
                        resultListModel.clear();
                        for (String className : results) {
                            resultListModel.addElement(className);
                        }
                        if (results.isEmpty()) {
                            statusLabel.setText("未找到匹配的类");
                            statusLabel.setForeground(new Color(180, 100, 100));
                        } else {
                            statusLabel.setText("找到 " + results.size() + " 个类");
                            statusLabel.setForeground(new Color(128, 128, 128));
                            if (resultListModel.size() > 0) {
                                resultList.setSelectedIndex(0);
                            }
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("搜索出错");
                        statusLabel.setForeground(new Color(180, 100, 100));
                    });
                }
            });
        }

        /**
         * 搜索类
         */
        private java.util.List<String> searchClasses(String searchText) {
            java.util.List<String> results = new java.util.ArrayList<>();

            try {
                String lowerSearch = searchText.toLowerCase();

                com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
                        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

                        String[] allClassNames = cache.getAllClassNames();

                        for (String className : allClassNames) {
                            if (results.size() >= 50) {
                                break;
                            }

                            if (className.toLowerCase().contains(lowerSearch)) {
                                PsiClass[] classes = cache.getClassesByName(className, scope);
                                for (PsiClass psiClass : classes) {
                                    if (results.size() >= 50) {
                                        break;
                                    }

                                    String qualifiedName = psiClass.getQualifiedName();
                                    if (qualifiedName != null
                                            && !qualifiedName.startsWith("java.")
                                            && !qualifiedName.startsWith("javax.")
                                            && !qualifiedName.startsWith("sun.")
                                            && !qualifiedName.startsWith("com.sun.")
                                            && !qualifiedName.startsWith("jdk.")
                                            && !results.contains(qualifiedName)) {
                                        results.add(qualifiedName);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                });

                // 排序
                results.sort((a, b) -> {
                    String aSimple = a.substring(a.lastIndexOf('.') + 1).toLowerCase();
                    String bSimple = b.substring(b.lastIndexOf('.') + 1).toLowerCase();

                    boolean aExact = aSimple.equals(lowerSearch);
                    boolean bExact = bSimple.equals(lowerSearch);
                    if (aExact && !bExact) {
                        return -1;
                    }
                    if (!aExact && bExact) {
                        return 1;
                    }

                    boolean aStarts = aSimple.startsWith(lowerSearch);
                    boolean bStarts = bSimple.startsWith(lowerSearch);
                    if (aStarts && !bStarts) {
                        return -1;
                    }
                    if (!aStarts && bStarts) {
                        return 1;
                    }

                    return aSimple.length() - bSimple.length();
                });

            } catch (Exception e) {
                // 忽略
            }

            return results;
        }

        @Override
        protected void doOKAction() {
            super.doOKAction();
        }

        /**
         * 获取所有已选择的类
         */
        public java.util.List<String> getSelectedClasses() {
            java.util.List<String> classes = new java.util.ArrayList<>();
            for (int i = 0; i < selectedListModel.size(); i++) {
                classes.add(selectedListModel.get(i));
            }
            return classes;
        }

        /**
         * 获取选择的类（兼容旧接口，返回第一个）
         */
        public String getSelectedClass() {
            return selectedListModel.size() > 0 ? selectedListModel.get(0) : null;
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return searchField;
        }

        private class ThemeClassListCellRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof String) {
                    String qualifiedName = (String) value;
                    int lastDot = qualifiedName.lastIndexOf('.');

                    String simpleName;
                    String packageName;
                    if (lastDot > 0) {
                        packageName = qualifiedName.substring(0, lastDot);
                        simpleName = qualifiedName.substring(lastDot + 1);
                    } else {
                        simpleName = qualifiedName;
                        packageName = "";
                    }

                    String nameColor = colorToHex(isSelected ? listSelectionFg : textColor);
                    String packageColor = colorToHex(isSelected ? listSelectionFg : secondaryTextColor);
                    String html = "<html><div style='padding: 2px 5px;'>" +
                            "<span style='font-size: 12px; font-weight: bold; color: " +
                            nameColor + ";'>" + simpleName + "</span>" +
                            (packageName.isEmpty() ? ""
                                    : "<span style='font-size: 10px; color: " +
                                            packageColor + ";'> (" + packageName + ")</span>")
                            +
                            "</div></html>";
                    setText(html);
                }

                if (isSelected) {
                    setBackground(listSelectionBg);
                } else {
                    setBackground(index % 2 == 0 ? listBgColor : listAltBgColor);
                }

                setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                return this;
            }

            private String colorToHex(Color color) {
                return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
            }
        }
    }

    /**
     * 增强版类列表单元格渲染器
     * 更好看的样式，显示类名和包名
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private static class EnhancedClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String) {
                String qualifiedName = (String) value;
                int lastDot = qualifiedName.lastIndexOf('.');

                String simpleName;
                String packageName;
                if (lastDot > 0) {
                    packageName = qualifiedName.substring(0, lastDot);
                    simpleName = qualifiedName.substring(lastDot + 1);
                } else {
                    simpleName = qualifiedName;
                    packageName = "";
                }

                // 使用HTML格式，更美观的显示
                String html = "<html><div style='padding: 2px 5px;'>" +
                        "<span style='font-size: 12px; font-weight: bold; color: " +
                        (isSelected ? "#FFFFFF" : "#E0E0E0") + ";'>" + simpleName + "</span>" +
                        (packageName.isEmpty() ? ""
                                : "<span style='font-size: 10px; color: " +
                                        (isSelected ? "#CCCCCC" : "#888888") + ";'> (" + packageName + ")</span>")
                        +
                        "</div></html>";
                setText(html);
            }

            // 设置选中和未选中的颜色
            if (isSelected) {
                setBackground(new Color(75, 110, 175));
            } else {
                setBackground(index % 2 == 0 ? new Color(45, 45, 45) : new Color(50, 50, 50));
            }

            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            return this;
        }
    }

    /**
     * 排除类列表渲染器 - 显示类名和排除字段数量
     * 支持深色/浅色主题自适应
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private class ExcludedClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String) {
                String className = (String) value;
                int lastDot = className.lastIndexOf('.');
                String simpleName = lastDot > 0 ? className.substring(lastDot + 1) : className;
                String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";

                // 获取排除字段信息
                java.util.Set<String> excludedFields = ApiDocSettings.getInstance()
                        .getExcludedFieldsForClass(className);
                String fieldInfo;
                if (excludedFields == null) {
                    fieldInfo = "排除全部字段";
                } else {
                    fieldInfo = "排除 " + excludedFields.size() + " 个字段";
                }

                // 根据主题选择颜色
                String nameColor = isSelected ? "#FFFFFF" : colorToHex(textColor);
                String fieldInfoColor = isSelected ? "#A5D6A7" : colorToHex(accentGreen);
                String packageColor = isSelected ? "#E0E0E0" : colorToHex(secondaryTextColor);

                String html = "<html><div style='padding: 3px;'>" +
                        "<span style='font-size: 12px; font-weight: bold; color: " +
                        nameColor + ";'>" + simpleName + "</span>" +
                        "<span style='font-size: 10px; color: " +
                        fieldInfoColor + ";'> [" + fieldInfo + "]</span><br>" +
                        "<span style='font-size: 9px; color: " +
                        packageColor + ";'>" + packageName + "</span>" +
                        "</div></html>";
                setText(html);
            }

            // 使用主题颜色
            if (isSelected) {
                setBackground(accentBlue);
            } else {
                setBackground(index % 2 == 0 ? inputBgColor : listAlternateColor);
            }
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            return this;
        }

        /**
         * 将颜色转换为十六进制字符串
         */
        private String colorToHex(Color color) {
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /**
     * 字段选择对话框
     * 用于选择要排除的具体字段
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private static class FieldSelectDialog extends DialogWrapper {
        private final String className;
        private final java.util.List<FieldItem> allFields;
        private final java.util.Set<String> currentExcluded;
        private final java.util.Map<String, JCheckBox> checkBoxMap = new java.util.LinkedHashMap<>();
        /** 父类复选框映射，用于全选/取消父类的所有字段 */
        private final java.util.Map<String, JCheckBox> classCheckBoxMap = new java.util.LinkedHashMap<>();
        /** 父类字段映射，记录每个父类包含哪些字段 */
        private final java.util.Map<String, java.util.List<String>> classFieldsMap = new java.util.LinkedHashMap<>();
        private JCheckBox selectAllCheckBox;
        private JLabel countLabel;
        private int totalFieldCount;

        // 主题颜色
        private final boolean isDarkTheme;
        private final Color bgColor;
        private final Color cardBgColor;
        private final Color borderColor;
        private final Color textColor;
        private final Color secondaryTextColor;
        private final Color accentBlue;
        private final Color accentGreen;

        /**
         * 构造函数
         *
         * @param project         项目
         * @param className       类名
         * @param allFields       所有字段
         * @param currentExcluded 当前已排除的字段，null表示排除所有
         * @author peach
         * @since 2025/12/26 | V1.0.0
         */
        public FieldSelectDialog(@Nullable Project project, String className,
                java.util.List<FieldItem> allFields, java.util.Set<String> currentExcluded) {
            super(project, true);
            this.className = className;
            this.allFields = allFields;
            this.currentExcluded = currentExcluded;

            // 初始化主题颜色
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color ideaBgColor = scheme.getDefaultBackground();
            double brightness = 0.299 * ideaBgColor.getRed() + 0.587 * ideaBgColor.getGreen()
                    + 0.114 * ideaBgColor.getBlue();
            this.isDarkTheme = brightness < 128;

            if (isDarkTheme) {
                this.bgColor = new Color(43, 43, 43);
                this.cardBgColor = new Color(50, 50, 50);
                this.borderColor = new Color(60, 60, 60);
                this.textColor = new Color(200, 200, 200);
                this.secondaryTextColor = new Color(120, 120, 120);
                this.accentBlue = new Color(100, 150, 200);
                this.accentGreen = new Color(130, 180, 130);
            } else {
                this.bgColor = new Color(250, 250, 252);
                this.cardBgColor = new Color(255, 255, 255);
                this.borderColor = new Color(210, 215, 220);
                this.textColor = new Color(50, 50, 50);
                this.secondaryTextColor = new Color(100, 105, 110);
                this.accentBlue = new Color(60, 110, 180);
                this.accentGreen = new Color(40, 140, 80);
            }

            setTitle("选择要排除的字段 - " + getSimpleName(className));
            setOKButtonText("确定");
            setCancelButtonText("取消");
            init();
        }

        private String getSimpleName(String fullName) {
            int lastDot = fullName.lastIndexOf('.');
            return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
            mainPanel.setPreferredSize(new Dimension(550, 450));
            mainPanel.setBackground(bgColor);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // 顶部说明和全选
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(bgColor);

            JLabel tipLabel = new JLabel("勾选的字段将不会出现在API文档中");
            tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            tipLabel.setForeground(secondaryTextColor);
            topPanel.add(tipLabel, BorderLayout.WEST);

            // 全选按钮和反选按钮
            JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            buttonGroup.setBackground(bgColor);

            selectAllCheckBox = new JCheckBox("全选");
            selectAllCheckBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            selectAllCheckBox.setForeground(textColor);
            selectAllCheckBox.setBackground(bgColor);
            selectAllCheckBox.setSelected(currentExcluded == null);
            selectAllCheckBox.addActionListener(e -> {
                boolean selected = selectAllCheckBox.isSelected();
                for (JCheckBox cb : checkBoxMap.values()) {
                    cb.setSelected(selected);
                }
                updateSelectAllState();
            });
            buttonGroup.add(selectAllCheckBox);

            JButton invertButton = new JButton("反选");
            invertButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            invertButton.addActionListener(e -> {
                for (JCheckBox cb : checkBoxMap.values()) {
                    cb.setSelected(!cb.isSelected());
                }
                updateSelectAllState();
            });
            buttonGroup.add(invertButton);

            topPanel.add(buttonGroup, BorderLayout.EAST);
            mainPanel.add(topPanel, BorderLayout.NORTH);

            // 字段列表
            JPanel fieldListPanel = new JPanel();
            fieldListPanel.setLayout(new BoxLayout(fieldListPanel, BoxLayout.Y_AXIS));
            fieldListPanel.setBackground(cardBgColor);

            // 预处理：统计每个类包含的字段
            for (FieldItem field : allFields) {
                classFieldsMap.computeIfAbsent(field.fromClass, k -> new java.util.ArrayList<>()).add(field.name);
            }

            String lastFromClass = null;
            for (FieldItem field : allFields) {
                // 添加来源类的分隔标签和复选框
                if (lastFromClass == null || !lastFromClass.equals(field.fromClass)) {
                    if (lastFromClass != null) {
                        fieldListPanel.add(Box.createVerticalStrut(8));
                    }
                    // 创建父类行面板，包含复选框和标签
                    JPanel classRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                    classRow.setBackground(cardBgColor);
                    classRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // 父类复选框（用于全选/取消该父类的所有字段）
                    JCheckBox classCheckBox = new JCheckBox();
                    classCheckBox.setBackground(cardBgColor);
                    final String currentClass = field.fromClass;
                    classCheckBox.addActionListener(e -> {
                        boolean selected = classCheckBox.isSelected();
                        java.util.List<String> fieldNames = classFieldsMap.get(currentClass);
                        if (fieldNames != null) {
                            for (String fieldName : fieldNames) {
                                JCheckBox fieldCb = checkBoxMap.get(fieldName);
                                if (fieldCb != null) {
                                    fieldCb.setSelected(selected);
                                }
                            }
                        }
                        updateSelectAllState();
                    });
                    classCheckBoxMap.put(field.fromClass, classCheckBox);
                    classRow.add(classCheckBox);

                    JLabel classLabel = new JLabel("来自 " + field.fromClass);
                    classLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
                    classLabel.setForeground(accentBlue);
                    classRow.add(classLabel);

                    fieldListPanel.add(classRow);
                    fieldListPanel.add(Box.createVerticalStrut(4));
                    lastFromClass = field.fromClass;
                }

                JPanel fieldRow = createFieldRow(field);
                fieldListPanel.add(fieldRow);
            }

            JScrollPane scrollPane = new JScrollPane(fieldListPanel);
            scrollPane.setBorder(BorderFactory.createLineBorder(borderColor, 1));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // 底部统计
            totalFieldCount = allFields.size();
            countLabel = new JLabel();
            countLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            countLabel.setForeground(secondaryTextColor);
            mainPanel.add(countLabel, BorderLayout.SOUTH);

            updateSelectAllState();

            return mainPanel;
        }

        /**
         * 创建字段行
         */
        private JPanel createFieldRow(FieldItem field) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(cardBgColor);
            row.setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 10));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 复选框
            JCheckBox checkBox = new JCheckBox();
            checkBox.setBackground(cardBgColor);
            // 如果currentExcluded为null，表示全选；否则检查是否在排除列表中
            boolean shouldCheck = (currentExcluded == null) || currentExcluded.contains(field.name);
            checkBox.setSelected(shouldCheck);
            checkBox.addActionListener(e -> updateSelectAllState());
            checkBoxMap.put(field.name, checkBox);
            row.add(checkBox, BorderLayout.WEST);

            // 字段信息
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            infoPanel.setBackground(cardBgColor);

            JLabel nameLabel = new JLabel(field.name);
            nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            nameLabel.setForeground(textColor);
            infoPanel.add(nameLabel);

            JLabel typeLabel = new JLabel(" : " + field.type);
            typeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            typeLabel.setForeground(accentGreen);
            infoPanel.add(typeLabel);

            if (field.description != null && !field.description.isEmpty()) {
                JLabel descLabel = new JLabel("  // " + field.description);
                descLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                descLabel.setForeground(secondaryTextColor);
                infoPanel.add(descLabel);
            }

            row.add(infoPanel, BorderLayout.CENTER);
            return row;
        }

        /**
         * 更新全选复选框状态
         */
        private void updateSelectAllState() {
            boolean allSelected = true;
            int selectedCount = 0;
            for (JCheckBox cb : checkBoxMap.values()) {
                if (cb.isSelected()) {
                    selectedCount++;
                } else {
                    allSelected = false;
                }
            }
            if (selectAllCheckBox != null) {
                selectAllCheckBox.setSelected(allSelected);
            }
            if (countLabel != null) {
                if (allSelected && totalFieldCount > 0) {
                    countLabel.setText("已全选（共 " + totalFieldCount + " 个字段）");
                } else {
                    countLabel.setText("已选 " + selectedCount + " / 共 " + totalFieldCount + " 个字段");
                }
            }

            // 更新每个父类复选框的状态
            for (java.util.Map.Entry<String, JCheckBox> entry : classCheckBoxMap.entrySet()) {
                String fromClass = entry.getKey();
                JCheckBox classCheckBox = entry.getValue();
                java.util.List<String> fieldNames = classFieldsMap.get(fromClass);
                if (fieldNames != null && !fieldNames.isEmpty()) {
                    boolean classAllSelected = true;
                    for (String fieldName : fieldNames) {
                        JCheckBox fieldCb = checkBoxMap.get(fieldName);
                        if (fieldCb != null && !fieldCb.isSelected()) {
                            classAllSelected = false;
                            break;
                        }
                    }
                    classCheckBox.setSelected(classAllSelected);
                }
            }
        }

        /**
         * 获取选中的字段（要排除的字段）
         *
         * @return 选中的字段名集合
         * @author peach
         * @since 2025/12/26 | V1.0.0
         */
        public java.util.Set<String> getSelectedFields() {
            java.util.Set<String> selected = new java.util.LinkedHashSet<>();
            for (java.util.Map.Entry<String, JCheckBox> entry : checkBoxMap.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selected.add(entry.getKey());
                }
            }
            return selected;
        }

        public boolean isAllSelected() {
            return selectAllCheckBox != null && selectAllCheckBox.isSelected();
        }
    }
}
