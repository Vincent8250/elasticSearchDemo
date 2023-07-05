package cn.vincent.tool.es.entity.vo;

import cn.vincent.tool.es.enums.SearchType;
import lombok.*;
import org.elasticsearch.search.sort.SortOrder;

/**
 * ES查询对象
 */
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SearchVo {

    // 查询类型
    SearchType searchType;

    // 查询字段
    String field;

    int from;

    int size;

    // 排序字段
    String sortField;

    // 排序
    SortOrder sortOrder;

    // 索引值（包括范围值）
    SearchValue value;
}
