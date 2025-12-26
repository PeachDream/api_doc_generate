package com.demojava01;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

/**
 * Git工具类
 * 用于获取Git相关信息
 *
 * @author peach
 * @since 2025/12/25 | V1.0.0
 */
public class GitUtils {

    /** 日志记录器 */
    private static final Logger LOG = Logger.getInstance(GitUtils.class);

    /**
     * 获取当前Git分支名称
     *
     * @param project 项目
     * @return 分支名称，如果无法获取则返回默认值
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public static String getCurrentBranchName(Project project) {
        return getCurrentBranchName(project, null);
    }

    /**
     * 根据当前编辑的文件获取Git分支名称
     * 从文件所在位置向上查找.git目录，解决多模块项目中分支获取错误的问题
     *
     * @param project     项目
     * @param currentFile 当前编辑的文件
     * @return 分支名称，如果无法获取则返回默认值
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    public static String getCurrentBranchName(Project project, PsiFile currentFile) {
        if (project == null) {
            return "V1.0.0";
        }

        // 优先从当前文件向上查找.git目录
        if (currentFile != null) {
            VirtualFile virtualFile = currentFile.getVirtualFile();
            if (virtualFile != null) {
                String gitBasePath = findGitRootPath(virtualFile);
                if (gitBasePath != null) {
                    // 方法1: 尝试从.git/HEAD文件读取
                    String branchFromHead = readBranchFromGitHead(gitBasePath);
                    if (branchFromHead != null) {
                        return formatBranchName(branchFromHead);
                    }

                    // 方法2: 尝试执行git命令
                    String branchFromCommand = readBranchFromCommand(gitBasePath);
                    if (branchFromCommand != null) {
                        return formatBranchName(branchFromCommand);
                    }
                }
            }
        }

        // 回退到使用项目路径
        String basePath = project.getBasePath();
        if (basePath == null) {
            return "V1.0.0";
        }

        // 方法1: 尝试从.git/HEAD文件读取
        String branchFromHead = readBranchFromGitHead(basePath);
        if (branchFromHead != null) {
            return formatBranchName(branchFromHead);
        }

        // 方法2: 尝试执行git命令
        String branchFromCommand = readBranchFromCommand(basePath);
        if (branchFromCommand != null) {
            return formatBranchName(branchFromCommand);
        }

        return "V1.0.0";
    }

    /**
     * 从文件向上查找.git目录所在的根路径
     *
     * @param file 当前文件
     * @return Git仓库根路径，如果未找到返回null
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private static String findGitRootPath(VirtualFile file) {
        if (file == null) {
            return null;
        }

        VirtualFile current = file.isDirectory() ? file : file.getParent();
        while (current != null) {
            VirtualFile gitDir = current.findChild(".git");
            if (gitDir != null) {
                return current.getPath();
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 从.git/HEAD文件读取分支名称
     *
     * @param basePath 项目根路径
     * @return 分支名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private static String readBranchFromGitHead(String basePath) {
        try {
            File headFile = new File(basePath, ".git/HEAD");
            if (!headFile.exists()) {
                // 可能是子模块或worktree，尝试查找.git文件
                File gitFile = new File(basePath, ".git");
                if (gitFile.isFile()) {
                    // 读取.git文件内容获取实际的git目录
                    try (BufferedReader reader = new BufferedReader(new FileReader(gitFile))) {
                        String line = reader.readLine();
                        if (line != null && line.startsWith("gitdir:")) {
                            String gitDir = line.substring(7).trim();
                            headFile = new File(gitDir, "HEAD");
                        }
                    }
                }
            }

            if (headFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(headFile))) {
                    String line = reader.readLine();
                    if (line != null) {
                        // 格式: ref: refs/heads/branch-name
                        if (line.startsWith("ref: refs/heads/")) {
                            return line.substring(16).trim();
                        }
                        // 可能是detached HEAD状态，返回commit hash的前几位
                        if (line.matches("[a-f0-9]{40}")) {
                            return line.substring(0, 7);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 读取Git HEAD文件失败，记录调试日志便于排查
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GitUtils] 读取Git HEAD文件失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 通过执行git命令获取分支名称
     *
     * @param basePath 项目根路径
     * @return 分支名称
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private static String readBranchFromCommand(String basePath) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pb.directory(new File(basePath));
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // 执行Git命令失败，记录调试日志便于排查
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GitUtils] 执行Git命令获取分支失败: " + e.getMessage());
            }
        } finally {
            // 确保进程被正确销毁，避免资源泄漏
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    /**
     * 格式化分支名称
     * 如果分支名称已经是版本号格式，直接返回
     * 否则尝试提取版本号或直接返回分支名
     *
     * @param branchName 原始分支名称
     * @return 格式化后的版本号
     * @author peach
     * @since 2025/12/25 | V1.0.0
     */
    private static String formatBranchName(String branchName) {
        if (branchName == null || branchName.isEmpty()) {
            return "V1.0.0";
        }

        // 如果分支名已经是版本号格式(V1.0.0 或 v1.0.0)
        if (branchName.matches("[Vv]?\\d+\\.\\d+(\\.\\d+)?.*")) {
            if (!branchName.toUpperCase().startsWith("V")) {
                return "V" + branchName;
            }
            return branchName.toUpperCase().charAt(0) + branchName.substring(1);
        }

        // 尝试从分支名中提取版本号
        // 例如: release/1.0.0, feature/v2.0.0, hotfix-1.2.3
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+(\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(branchName);
        if (matcher.find()) {
            return "V" + matcher.group(1);
        }

        // 无法提取版本号，返回分支名本身
        return branchName;
    }
}
