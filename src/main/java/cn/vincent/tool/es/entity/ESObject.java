package cn.vincent.tool.es.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * ES顶级抽象类
 */
@Data
public abstract class ESObject {

    // id
    String id;

    // 评分
    float score;

    // 高亮字段 为null是不处理
    @JsonProperty("hig")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<String> higtext;
}
