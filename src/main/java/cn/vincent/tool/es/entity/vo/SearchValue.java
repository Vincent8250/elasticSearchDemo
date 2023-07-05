package cn.vincent.tool.es.entity.vo;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SearchValue<T>{
    T value;

    /**
     * 大于等于值
     */
    T gte;
    /**
     * 大于值
     */
    T gt;

    /**
     * 小于等于值
     */
    T lte;
    /**
     * 小于值
     */
    T lt;
}
