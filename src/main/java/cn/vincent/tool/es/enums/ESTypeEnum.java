package cn.vincent.tool.es.enums;

/**
 * ES数据类型
 */
public enum ESTypeEnum {

    TEXT("text"),
    KEYWORD("keyword"),
    DATE("date"),

    // 整型
    LONG("long"),
    INTEGER("integer"),
    SHORT("short"),
    BYTE("byte"),

    // 浮点型
    FLOAT("float"),
    DOUBLE("double"),

    BOOLEAN("boolean"),
    BINARY("binary");

    String type;

    ESTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}