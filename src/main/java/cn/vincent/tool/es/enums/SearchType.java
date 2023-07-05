package cn.vincent.tool.es.enums;

/**
 * 查询类型
 */
public enum SearchType {
    MATCH, // 关键字匹配
    TERM, // 精确匹配
    RANGE, // 范围匹配
    WILDCARD, // 通配符匹配
    REGEXP, // 正则匹配
    PREFIX, // 前缀匹配
    FUZZY // 模糊匹配
}
