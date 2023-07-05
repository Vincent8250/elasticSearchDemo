package cn.vincent.tool.es.entity.index;

import cn.vincent.tool.es.entity.ESObject;
import cn.vincent.tool.es.annotation.ESField;
import cn.vincent.tool.es.annotation.ESIndex;
import cn.vincent.tool.es.enums.ESAnalyzerEnum;
import cn.vincent.tool.es.enums.ESTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * 索引类型 - 示例
 */
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ESIndex(indexName = "article") // 标注索引名称
public class ArticleIndex extends ESObject {

    // 标注字段类型
    @ESField(type = ESTypeEnum.KEYWORD)
    String id;

    // 标注字段类型 分词器
    @ESField(type = ESTypeEnum.TEXT, analyzer = ESAnalyzerEnum.IK_MAX_WORD)
    String author;

    // 标注字段类型 分词器
    @ESField(type = ESTypeEnum.TEXT, analyzer = ESAnalyzerEnum.IK_MAX_WORD)
    String title;

    // 标注字段类型 分词器
    @ESField(type = ESTypeEnum.TEXT, analyzer = ESAnalyzerEnum.IK_MAX_WORD)
    String body;

    // 标注字段类型 时间格式
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ESField(type = ESTypeEnum.DATE)
    Date postDate;
}
