package com.demojava01;

/**
 * 接口信息数据类
 * 用于存储单个接口的名称和文档内容
 *
 * @author peach
 * @since 2025/12/26 | V1.0.0
 */
public class ApiInfo {

    /** 接口名称（方法标题） */
    private final String name;

    /** 接口文档内容（Markdown格式） */
    private final String content;

    /**
     * 构造函数
     *
     * @param name    接口名称
     * @param content 接口文档内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public ApiInfo(String name, String content) {
        this.name = name;
        this.content = content;
    }

    /**
     * 获取接口名称
     *
     * @return 接口名称
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getName() {
        return name;
    }

    /**
     * 获取接口文档内容
     *
     * @return 接口文档内容
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return name;
    }
}
