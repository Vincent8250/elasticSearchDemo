package cn.vincent.tool.es.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.vincent.tool.es.entity.ESObject;
import cn.vincent.tool.es.entity.vo.ESPage;
import cn.vincent.tool.es.entity.vo.SearchVo;
import cn.vincent.tool.es.annotation.ESField;
import cn.vincent.tool.es.annotation.ESIndex;
import cn.vincent.tool.es.enums.ESTypeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ElasticsearchUtil {

    @Resource
    RestHighLevelClient client;
    @Resource
    RequestOptions requestOptions;

    //region 索引操作

    /**
     * 创建索引
     *
     * @param indexName
     * @return
     */
    public boolean checkIndex(String indexName) {
        boolean exists = false;
        try {
            GetIndexRequest getIndex = new GetIndexRequest(indexName);
            exists = client.indices().exists(getIndex, requestOptions);
        } catch (IOException e) {
            log.error("checkIndex ==》 校验异常");
        }
        return exists;
    }

    /**
     * 创建索引
     *
     * @param indexClass
     * @return
     */
    public boolean createIndex(Class indexClass) {
        Annotation annotation = indexClass.getAnnotation(ESIndex.class);
        if (annotation != null)
            return createIndex(((ESIndex) annotation).indexName(), mappingBuilder(indexClass));
        else
            return false;
    }

    /**
     * 创建索引
     *
     * @param indexName
     * @param builder
     * @return
     */
    public boolean createIndex(String indexName, XContentBuilder builder) {
        boolean flag = false;
        if (checkIndex(indexName))
            return flag;
        try {
            CreateIndexRequest createIndex = new CreateIndexRequest(indexName);
            createIndex.mapping(builder);
            Settings settings = Settings.builder()
                    .put("number_of_replicas", 0)
                    .build();
            createIndex.settings(settings);

            CreateIndexResponse response = client.indices().create(createIndex, requestOptions);
            flag = response.isAcknowledged();
        } catch (IOException e) {
            log.error("createIndex ==》 创建异常");
        }
        return flag;
    }

    /**
     * 删除索引
     *
     * @param indexName
     * @return
     */
    public boolean deleteIndex(String indexName) {
        boolean flag = false;
        if (!checkIndex(indexName))
            return flag;
        try {
            DeleteIndexRequest deleteIndex = new DeleteIndexRequest(indexName);
            AcknowledgedResponse response = client.indices().delete(deleteIndex, requestOptions);
            flag = response.isAcknowledged();
        } catch (IOException e) {
            log.error("deleteIndex ==》 删除异常");
        }
        return flag;
    }

    /**
     * 获取映射对象
     *
     * @param prop
     * @return
     */
    public XContentBuilder mappingBuilder(Map<String, String> prop) {
        XContentBuilder xContentBuilder = null;
        try {
            xContentBuilder = XContentFactory.jsonBuilder();
            xContentBuilder.startObject();
            {
                xContentBuilder.startObject("properties");
                for (String name : prop.keySet()) {
                    xContentBuilder.startObject(name);
                    xContentBuilder.field("type", prop.get(name));
                    xContentBuilder.endObject();
                }
                xContentBuilder.endObject();
            }
            xContentBuilder.endObject();
        } catch (IOException e) {
            log.error("mappingBuilder ==》 映射关系构建异常");
        }
        return xContentBuilder;
    }

    /**
     * 获取映射对象
     *
     * @param esIndex
     * @return
     */
    public XContentBuilder mappingBuilder(Class esIndex) {
        // 判断类型是否标注索引注解
        Annotation annotation = esIndex.getAnnotation(ESIndex.class);
        if (annotation == null)// 没有索引注解 不生效
            return null;
        else {
            XContentBuilder xContentBuilder = null;
            //取所有字段
            Field[] declaredFields = esIndex.getDeclaredFields();
            try {
                xContentBuilder = XContentFactory.jsonBuilder();
                xContentBuilder.startObject();
                {
                    //开始构建映射属性
                    xContentBuilder.startObject("properties");
                    //循环字段
                    for (Field field : declaredFields) {
                        //判断字段是否标注ESField注解
                        ESField fieldAnnotation = field.getAnnotation(ESField.class);
                        if (fieldAnnotation != null) {
                            //默认取字段名 并开启 startObject
                            if (StrUtil.isNotBlank(fieldAnnotation.fieldName()))
                                xContentBuilder.startObject(fieldAnnotation.fieldName());
                            else
                                xContentBuilder.startObject(field.getName());
                            //字段类型
                            xContentBuilder.field("type", fieldAnnotation.type().getType());
                            //分析器
                            if (fieldAnnotation.analyzer().getAnalyzer() != null)
                                xContentBuilder.field("analyzer", fieldAnnotation.analyzer().getAnalyzer());
                            //时间格式
                            if (fieldAnnotation.type() == ESTypeEnum.DATE && StrUtil.isNotBlank(fieldAnnotation.format()))
                                xContentBuilder.field("format", fieldAnnotation.format());
                            //结束 endObject
                            xContentBuilder.endObject();
                        }
                    }
                    xContentBuilder.endObject();
                }
                xContentBuilder.endObject();
            } catch (IOException e) {
                log.error("mappingBuilder ==》 映射关系构建异常");
            }
            return xContentBuilder;
        }
    }
    //endregion

    //region 文档操作

    /**
     * 判断文档是否存在
     *
     * @param indexName
     * @param id
     * @return
     */
    public boolean checkDocumentByID(String indexName, String id) {
        boolean exists = false;
        try {
            GetRequest get = new GetRequest();
            get.index(indexName);
            get.id(id);
            exists = client.exists(get, requestOptions);
        } catch (IOException e) {
            log.error("checkDocument ==》 校验异常");
        }
        return exists;
    }

    //region doc 新增

    /**
     * 文档添加
     *
     * @param indexName 索引名称
     * @param es        添加对象
     * @param <ES>
     */
    public <ES extends ESObject> void insertDoc(String indexName, ES es) {
        try {
            client.index(docIndexRequest(indexName, es), requestOptions);
        } catch (IOException e) {
            log.info("insertDoc ==》 文档新增异常");
        }
    }

    /**
     * 文档添加
     *
     * @param indexName 索引名称
     * @param es        添加对象
     * @param <ES>
     * @return
     * @throws JsonProcessingException
     */
    private <ES extends ESObject> IndexRequest docIndexRequest(String indexName, ES es) throws JsonProcessingException {
        ObjectMapper json = new ObjectMapper();
        IndexRequest doc = new IndexRequest(indexName);
        doc.id(es.getId());
        doc.source(json.writeValueAsString(es), XContentType.JSON);
        return doc;
    }

    /**
     * 文档添加 - 批处理
     *
     * @param indexName 索引名称
     * @param esList    文档集合
     * @param <ES>
     */
    public <ES extends ESObject> void insertDocBatch(String indexName, List<ES> esList) {
        try {
            BulkRequest bulk = new BulkRequest();
            for (ES es : esList)
                bulk.add(docIndexRequest(indexName, es));
            client.bulk(bulk, requestOptions);
        } catch (IOException e) {
            log.info("insertDoc ==》 文档批量新增异常");
        }
    }
    //endregion

    //region doc 更新

    /**
     * 文档更新
     *
     * @param indexName 索引名称
     * @param es        更新对象
     * @param <ES>
     */
    public <ES extends ESObject> void updateDoc(String indexName, ES es) {
        try {
            client.update(docUpdateRequest(indexName, es), requestOptions);
        } catch (IOException e) {
            log.info("updateDoc ==》 文档更新异常");
        }
    }

    /**
     * 文档更新
     *
     * @param indexName 索引名称
     * @param es        更新对象
     * @param <ES>
     */
    private <ES extends ESObject> UpdateRequest docUpdateRequest(String indexName, ES es) throws JsonProcessingException {
        ObjectMapper json = new ObjectMapper();
        UpdateRequest doc = new UpdateRequest(indexName, es.getId());
        doc.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        doc.doc(json.writeValueAsString(es), XContentType.JSON);
        return doc;
    }

    /**
     * 文档更新 - 批处理
     *
     * @param indexName 索引名称
     * @param esList    更新对象集合
     * @param <ES>
     */
    public <ES extends ESObject> void updateDocBatch(String indexName, List<ES> esList) {
        try {
            BulkRequest bulk = new BulkRequest();
            for (ES es : esList)
                bulk.add(docUpdateRequest(indexName, es));
            client.bulk(bulk, requestOptions);
        } catch (IOException e) {
            log.info("insertDoc ==》 文档批量更新异常");
        }
    }
    //endregion

    //region doc 删除

    /**
     * 文档删除
     *
     * @param indexName 索引名称
     * @param id
     */
    public void deleteDoc(String indexName, String id) {
        try {
            client.delete(new DeleteRequest(indexName, id), requestOptions);
        } catch (IOException e) {
            log.info("deleteDoc ==》 文档删除异常");
        }
    }

    /**
     * 文档删除 - 批处理
     *
     * @param indexName 索引名称
     * @param idList    id集合
     */
    public void deleteDocBatch(String indexName, List<String> idList) {
        try {
            BulkRequest bulk = new BulkRequest();
            for (String id : idList)
                bulk.add(new DeleteRequest(indexName, id));
            client.bulk(bulk, requestOptions);
        } catch (IOException e) {
            log.info("insertDoc ==》 文档批量新增异常");
        }
    }
    //endregion

    //endregion

    //region 数据查询

    //region 基础查询
    /**
     * 查询 - 高亮处理
     *
     * @param indexName           索引名称
     * @param objClass            索引 对象类
     * @param searchSourceBuilder 查询构建器
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> search(String indexName, Class<ES> objClass, SearchSourceBuilder searchSourceBuilder) {
        ESPage<ES> esPage = new ESPage<ES>() {
            {
                setPageNum(searchSourceBuilder.from());
                setPageSize(searchSourceBuilder.size());
            }
        };
        List<ES> hits = new ArrayList<>();
        //高亮字段 只处理一个字段
        List<String> higFields = searchSourceBuilder.highlighter().fields().stream()
                .map(field -> field.name()).collect(Collectors.toList());
        String higField = higFields.size() > 0 ? higFields.get(0) : null;
        try {
            //构建查询请求对象
            SearchRequest request = new SearchRequest(indexName);
            request.source(searchSourceBuilder);
            //执行查询
            SearchResponse response = client.search(request, requestOptions);
            //取总数
            long total = response.getHits().getTotalHits().value;
            esPage.setTotal(total);
            //计算总页数
            esPage.setTotalPage((int) Math.ceil((double) total / esPage.getPageSize()));
            //循环处理结果
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> item = hit.getSourceAsMap();
                // 转换成索引对象实例
                ES es = BeanUtil.mapToBean(item, objClass, false, new CopyOptions());
                es.setId(hit.getId());// 设置id
                es.setScore(hit.getScore());// 设置评分
                hits.add(es);
                //region 高亮处理 保留高亮内容
                if (StrUtil.isNotEmpty(higField) && hit.getHighlightFields().size() > 0) {
                    HighlightField hField = hit.getHighlightFields().get(higField);
                    if (hField != null)// 设置高亮字段
                        es.setHigtext(Arrays.stream(hField.fragments()).map(
                                h -> h.toString()
                        ).collect(Collectors.toList()));
                }
                //endregion
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        esPage.setList(hits);
        return esPage;
    }

    /**
     * 查询 不处理高亮字段
     *
     * @param indexName
     * @param objClass
     * @param searchSourceBuilder
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMore(String indexName, Class<ES> objClass, SearchSourceBuilder searchSourceBuilder) {
        ESPage<ES> esPage = new ESPage<ES>() {
            {
                setPageNum(searchSourceBuilder.from());
                setPageSize(searchSourceBuilder.size());
            }
        };
        List<ES> hits = new ArrayList<>();
        try {
            SearchRequest request = new SearchRequest(indexName);
            request.source(searchSourceBuilder);
            SearchResponse response = client.search(request, requestOptions);
            long total = response.getHits().getTotalHits().value;
            esPage.setTotal(total);
            esPage.setTotalPage((int) Math.ceil((double) total / esPage.getPageSize()));
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> item = hit.getSourceAsMap();
                ES es = BeanUtil.mapToBean(item, objClass, false, new CopyOptions());
                es.setId(hit.getId());
                es.setScore(hit.getScore());
                hits.add(es);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        esPage.setList(hits);
        return esPage;
    }
    //endregion

    //region 单条件查询
    //单条件查询 使用高亮查询
    /**
     * 查询 - 关键字匹配
     * @param indexName
     * @param objClass
     * @param search
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMatch(String indexName, Class<ES> objClass, SearchVo search) {
        return search(indexName, objClass, new SearchSourceBuilder()
                // 设置查询字段 查询值
                .query(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()))
                // 设置高亮字段 高亮处理（高亮内容前后添加span标签）
                .highlighter(new HighlightBuilder().field(search.getField())
                        .preTags("<span style=\"color:red\">")
                        .postTags("</span>")
                )
                .from(search.getFrom())
                .size(search.getSize())
        );
    }

    /**
     * 查询 - 精确匹配
     * @param indexName
     * @param objClass
     * @param search
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchTerm(String indexName, Class<ES> objClass, SearchVo search) {
        return search(indexName, objClass, new SearchSourceBuilder()
                .query(QueryBuilders.termQuery(search.getField(), search.getValue().getValue()))
                .highlighter(new HighlightBuilder().field(search.getField())
                        .preTags("<span style=\"color:red\">")
                        .postTags("</span>")
                )
                .from(search.getFrom())
                .size(search.getSize())
        );
    }

    /**
     * 查询 - 范围匹配
     * @param indexName
     * @param objClass
     * @param search
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchRanges(String indexName, Class<ES> objClass, SearchVo search) {
        return search(indexName, objClass, new SearchSourceBuilder()
                .query(QueryBuilders.rangeQuery(search.getField())
                        .gte(search.getValue().getGte())
                        .gt(search.getValue().getGt())
                        .lte(search.getValue().getLte())
                        .lt(search.getValue().getLt()))
                .highlighter(new HighlightBuilder().field(search.getField())
                        .preTags("<span style=\"color:red\">")
                        .postTags("</span>")
                )
                .from(search.getFrom())
                .size(search.getSize())
        );
    }
    //endregion

    //region 多条件查询

    /**
     * 多条件组合查询
     *
     * @param indexName      索引名称
     * @param objClass       索引对象
     * @param searchsMust    must条件集合
     * @param searchsMustNot mustnot 条件集合
     * @param searchsShould  should 条件集合
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> search(String indexName, Class<ES> objClass,
                                                   List<SearchVo> searchsMust,
                                                   List<SearchVo> searchsMustNot,
                                                   List<SearchVo> searchsShould,
                                                   int from,
                                                   int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchBuilderMust(boolQueryBuilder, searchsMust);
        searchBuilderMustNot(boolQueryBuilder, searchsMustNot);
        searchBuilderShould(boolQueryBuilder, searchsShould);
        searchSourceBuilder.query(boolQueryBuilder)
                .from(from)
                .size(size);
        return searchMore(indexName, objClass, searchSourceBuilder);
    }

    //region searchMust
    // 多条件中的must 相当于sql中的and查询

    /**
     * 多条件查询 must
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMust(String indexName, Class<ES> objClass, List<SearchVo> searchs) {
        return searchMust(indexName, objClass, searchs, searchs.get(0).getFrom(), searchs.get(0).getSize());
    }

    /**
     * 多条件查询 must
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param from
     * @param size
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMust(String indexName, Class<ES> objClass, List<SearchVo> searchs, int from, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(searchBuilderMust(QueryBuilders.boolQuery(), searchs))
                .from(from)
                .size(size);
        return searchMore(indexName, objClass, searchSourceBuilder);
    }
    //endregion

    //region searchMustNot
    // 多条件中的mustNot 相当于sql中所有条件不等于

    /**
     * 多条件查询 mustnot
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMustNot(String indexName, Class<ES> objClass, List<SearchVo> searchs) {
        return searchMustNot(indexName, objClass, searchs, searchs.get(0).getFrom(), searchs.get(0).getSize());
    }

    /**
     * 多条件查询 mustnot
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchMustNot(String indexName, Class<ES> objClass, List<SearchVo> searchs, int from, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(searchBuilderMustNot(QueryBuilders.boolQuery(), searchs))
                .from(from)
                .size(size);
        return searchMore(indexName, objClass, searchSourceBuilder);
    }
    //endregion

    //region searchShould
    // 多条件中的should 相当于sql中的or查询 满足一个就行

    /**
     * 多条件查询 should
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchShould(String indexName, Class<ES> objClass, List<SearchVo> searchs) {
        return searchShould(indexName, objClass, searchs, searchs.get(0).getFrom(), searchs.get(0).getSize());
    }

    /**
     * 多条件查询 should
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchShould(String indexName, Class<ES> objClass, List<SearchVo> searchs, int from, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(searchBuilderShould(QueryBuilders.boolQuery(), searchs))
                .from(from)
                .size(size);
        return searchMore(indexName, objClass, searchSourceBuilder);
    }
    //endregion

    //region searchFilter
    // 多条件中的filter 过滤

    /**
     * 多条件查询 filter
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchFilter(String indexName, Class<ES> objClass, List<SearchVo> searchs) {
        return searchFilter(indexName, objClass, searchs, searchs.get(0).getFrom(), searchs.get(0).getSize());
    }

    /**
     * 多条件查询 filter
     *
     * @param indexName
     * @param objClass
     * @param searchs
     * @param <ES>
     * @return
     */
    public <ES extends ESObject> ESPage<ES> searchFilter(String indexName, Class<ES> objClass, List<SearchVo> searchs, int from, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(searchBuilderFilter(QueryBuilders.boolQuery(), searchs))
                .from(from)
                .size(size);
        return searchMore(indexName, objClass, searchSourceBuilder);
    }
    //endregion

    //endregion

    //region 布尔查询构建器封装

    /**
     * 封装boolQueryBuilder对象 - Must
     *
     * @param boolQueryBuilder
     * @param searchs
     * @return
     */
    private BoolQueryBuilder searchBuilderMust(BoolQueryBuilder boolQueryBuilder, List<SearchVo> searchs) {
        for (SearchVo search : searchs) {
            if (ObjUtil.isNull(search))
                continue;
            switch (search.getSearchType()) {
                case MATCH:
                    boolQueryBuilder.must(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()));
                    break;
                case TERM:
                    boolQueryBuilder.must(QueryBuilders.termQuery(search.getField(), search.getValue().getValue()));
                    break;
                case RANGE:
                    boolQueryBuilder.must(QueryBuilders.rangeQuery(search.getField())
                            .gt(search.getValue().getGt())
                            .gte(search.getValue().getGte())
                            .lt(search.getValue().getLt())
                            .lte(search.getValue().getLte()));
                    break;
                case WILDCARD:
                    boolQueryBuilder.must(QueryBuilders.wildcardQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case REGEXP:
                    boolQueryBuilder.must(QueryBuilders.regexpQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case PREFIX:
                    boolQueryBuilder.must(QueryBuilders.prefixQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case FUZZY:
                    boolQueryBuilder.must(QueryBuilders.fuzzyQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                default:
                    break;
            }
        }
        if (searchs.size() > 0 && boolQueryBuilder.must().size() > 0)
            return boolQueryBuilder;
        else
            return boolQueryBuilder;
    }

    /**
     * 封装boolQueryBuilder对象 - MustNot
     *
     * @param boolQueryBuilder
     * @param searchs
     * @return
     */
    private BoolQueryBuilder searchBuilderMustNot(BoolQueryBuilder boolQueryBuilder, List<SearchVo> searchs) {
        for (SearchVo search : searchs) {
            if (ObjUtil.isNull(search))
                continue;
            switch (search.getSearchType()) {
                case MATCH:
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()));
                    break;
                case TERM:
                    boolQueryBuilder.mustNot(QueryBuilders.termQuery(search.getField(), search.getValue().getValue()));
                    break;
                case RANGE:
                    boolQueryBuilder.mustNot(QueryBuilders.rangeQuery(search.getField())
                            .gt(search.getValue().getGt())
                            .gte(search.getValue().getGte())
                            .lt(search.getValue().getLt())
                            .lte(search.getValue().getLte()));
                    break;
                case WILDCARD:
                    boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case REGEXP:
                    boolQueryBuilder.mustNot(QueryBuilders.regexpQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                default:
                    break;
            }
        }
        if (searchs.size() > 0 && boolQueryBuilder.must().size() > 0)
            return boolQueryBuilder;
        else
            return boolQueryBuilder;
    }

    /**
     * 封装boolQueryBuilder对象 - Should
     *
     * @param boolQueryBuilder
     * @param searchs
     * @return
     */
    private BoolQueryBuilder searchBuilderShould(BoolQueryBuilder boolQueryBuilder, List<SearchVo> searchs) {
        for (SearchVo search : searchs) {
            if (ObjUtil.isNull(search))
                continue;
            switch (search.getSearchType()) {
                case MATCH:
                    boolQueryBuilder.should(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()));
                    break;
                case TERM:
                    boolQueryBuilder.should(QueryBuilders.termQuery(search.getField(), search.getValue().getValue()));
                    break;
                case RANGE:
                    boolQueryBuilder.should(QueryBuilders.rangeQuery(search.getField())
                            .gt(search.getValue().getGt())
                            .gte(search.getValue().getGte())
                            .lt(search.getValue().getLt())
                            .lte(search.getValue().getLte()));
                    break;
                case WILDCARD:
                    boolQueryBuilder.should(QueryBuilders.wildcardQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case REGEXP:
                    boolQueryBuilder.should(QueryBuilders.regexpQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                default:
                    break;
            }
        }
        if (searchs.size() > 0 && boolQueryBuilder.must().size() > 0)
            return boolQueryBuilder;
        else
            return boolQueryBuilder;
    }

    /**
     * 封装boolQueryBuilder对象 - Filter
     *
     * @param boolQueryBuilder
     * @param searchs
     * @return
     */
    private BoolQueryBuilder searchBuilderFilter(BoolQueryBuilder boolQueryBuilder, List<SearchVo> searchs) {
        for (SearchVo search : searchs) {
            if (ObjUtil.isNull(search))
                continue;
            switch (search.getSearchType()) {
                case MATCH:
                    boolQueryBuilder.filter(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()));
                    break;
                case TERM:
                    boolQueryBuilder.filter(QueryBuilders.termQuery(search.getField(), search.getValue().getValue()));
                    break;
                case RANGE:
                    boolQueryBuilder.filter(QueryBuilders.rangeQuery(search.getField())
                            .gt(search.getValue().getGt())
                            .gte(search.getValue().getGte())
                            .lt(search.getValue().getLt())
                            .lte(search.getValue().getLte()));
                    break;
                case WILDCARD:
                    boolQueryBuilder.filter(QueryBuilders.wildcardQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                case REGEXP:
                    boolQueryBuilder.filter(QueryBuilders.regexpQuery(search.getField(), search.getValue().getValue().toString()));
                    break;
                default:
                    break;
            }
        }
        if (searchs.size() > 0 && boolQueryBuilder.must().size() > 0)
            return boolQueryBuilder;
        else
            return boolQueryBuilder;
    }

    //endregion

    //endregion
}
