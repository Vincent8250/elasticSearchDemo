package cn.vincent.tool.es.enums;

/**
 * ES分词类型
 */
public enum ESAnalyzerEnum {

    NULL(null),

    // ik
    IK_SMART("ik_smart"), // 简单分词
    IK_MAX_WORD("ik_max_word"), // 详细分词

    // 官方自带
    STANDARD("standard"),
    SIMPLE("simple"),
    WHITESPACE("whitespace"),
    KEYWORD("keyword"),
    PATTERN("pattern"),
    LANGUAGE("language"),
    SNOWBALL("snowball"),
    STOP("stop"),
    EDGE_NGRAM("edge_ngram")
    ;

    String analyzer;

    ESAnalyzerEnum(String value){
        analyzer = value;
    }

    public String getAnalyzer(){
        return analyzer;
    }
}