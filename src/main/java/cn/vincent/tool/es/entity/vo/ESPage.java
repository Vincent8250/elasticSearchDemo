package cn.vincent.tool.es.entity.vo;

import cn.vincent.tool.es.entity.ESObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ES分页结果对象
 * @param <ES>
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ESPage <ES extends ESObject> {

    List<ES> list;

    int pageNum;

    int pageSize;

    long total;

    int totalPage;
}
