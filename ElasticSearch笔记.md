# ElasticSearch

### 基础认知：

#### 数据分类

- 结构化数据
- 非结构化数据

#### 搜索方式

- 顺序扫描法
- 全文搜索

#### 常见搜索引擎

- Lucene：是一个Java全文搜索引擎 完全用Java编写
  Lucene不是一个完整的应用程序，而是一个代码库和API，可以很容易地用于向应用程序添加搜索功能
- Solr：Solr是基于Lucene的Java库构建的开源搜索平台
  它能提供分布式索引，复制，负载均衡以及自动故障转移和恢复。如果它被正确部署然后管理的好，他就能够成为一个高可用，可扩展且容错的搜索引擎
- ElasticSearch：elasticsearch 基于 lucene，隐藏了 lucene 的复杂性，提供了简单易用的 restful api / Java api 接口
  它提供了一个分布式，多租户能力的全文搜索引擎，具有HTTP Web页面和无架构JSON文档。



### 环境安装



### 基础概念

##### 索引(index)：

索引可以理解为关系型数据库中的库

##### 类型(type)：

类型可以看作关系型数据库中的表
需要注意的时候ES 7之后没有类型的概念了（ES 5一个索引中可以有多个类型、ES 6一个索引只能有一个类型）

##### 映射(mapping)：

映射定义了所有的字段信息 相当与关系型数据库中的表结构

##### 文档(document)：

相当于关系型数据库中的一行记录

##### 字段(field)：

基本相当于关系型数据库的字段

##### 分片和副本(shard)：

分片有主分片(primary Shard)和副本分片(replica Shard)之分 副本是分片的副本
一个Index数据在屋里上被分布在多个主分片中 每个主分片只存放部分数据
每个主分片可以有多个副本 叫副本分片 是主分片的复制

### 脚本记录

~~~json
条件查询:
GET sec_report/_search
{
    "query":{
      "match":{
        "字段名称": ""
        }
    }
}

ID删除:
DELETE sec_report/_doc/1309

条件删除:
POST sec_report_element/_delete_by_query
{
    "query":{
      "match":{
        "字段名称": ""
        }
    }
}

添加：
POST sec_report/_doc/8185
{
  "id":8185,
  "industryName":"制造业",
  "fileName":"江海股份深度报告20130819.pdf",
  "docType":"research_report","rating":"推荐",
  "fileCode":"5b6715130d414d1c17c122f14d186bfb.pdf",
  "postOrg":"长江证券",
  "remark" : "",
  "message":"成功",
  "scanFlag":"0",
  "reportType":"1",
  "parseDate":"2013-08-19 04:08:04",
  "analystName":"枯木逢春犹再发"
}

分词：
GET _analyze
{
    "text":"分词内容",
    "analyzer":"分词器名称"
}

添加映射：
PUT sec_report/_doc/_mapping
{
  "properties": {
    "authorization" : {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer" : "aliws"
    }
  }
}

~~~

### 常用命令

~~~bash
# 修改 最大结果集：
curl -k --header "Content-Type: application/json;charset=UTF-8" --user elastic:Elastic#123 -XPUT http://29.118.128.239:59200/sec_report/_settings -d '{ "index" : { "max_result_window" : "10000"}}'

# 修改 测试mapping：
curl -H "Content-Type: application/json;charset=UTF-8" --user elastic:Elastic#123 -XPUT "http://29.118.128.239:59200/sec_test/_doc/_mapping?pretty" -d '
{
  "properties": {
    "deviceKeyword": {
        "type": "text", 
        "analyzer": "ik_max_word", 
        "search_analyzer": "aliws"
    }
}}'

# 修改 sec_report mapping：
curl -H "Content-Type: application/json;charset=UTF-8" --user elastic:Elastic#123 -XPUT "http://29.118.128.239:59200/sec_report/_doc/_mapping?pretty" -d '
{
  "properties": {
    "analystName" : {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer" : "aliws"
    },
    "remark" : {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer" : "aliws"
    },
    "fileParse": {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer": "aliws"
    }
  }
}'
# 查看sec_report映射
curl --user elastic:Elastic#123 -GET "http://29.118.128.239:59200/sec_report/_doc/_mapping?pretty"

# 修改 sec_report_element mapping：
curl -H "Content-Type: application/json;charset=UTF-8" --user elastic:Elastic#123 -XPUT "http://29.118.128.239:59200/sec_report_element/_doc/_mapping?pretty" -d '
{
  "properties": {
    "analystName" : {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer" : "aliws"
    },
    "elementInfo": {
      "type" : "text",
      "analyzer" : "aliws",
      "search_analyzer": "aliws"
    }
  }
}'

# 查看sec_report_element映射
curl --user elastic:Elastic#123 -GET "http://29.118.128.239:59200/sec_report_element/_doc/_mapping?pretty"
~~~



### SCORE评分

Lucene（或 Elasticsearch）使用 [*布尔模型（Boolean model）*](http://en.wikipedia.org/wiki/Standard_Boolean_model) 查找匹配文档，并用一个名为 [*实用评分函数（practical scoring function）*](https://www.elastic.co/guide/cn/elasticsearch/guide/2.x/practical-scoring-function.html) 的公式来计算相关度。这个公式借鉴了 [*词频/逆向文档频率（term frequency/inverse document frequency）*](http://en.wikipedia.org/wiki/Tfidf) 和 [*向量空间模型（vector space model）*](http://en.wikipedia.org/wiki/Vector_space_model)，同时也加入了一些现代的新特性，如协调因子（coordination factor），字段长度归一化（field length normalization），以及词或查询语句权重提升。

> ##### 词频/逆向文档频率

当匹配到一组文档后，需要根据相关度排序这些文档，不是所有的文档都包含所有词，有些词比其他的词更重要。**一个文档的相关度评分部分取决于每个查询词在文档中的权重** 。

词的权重由三个因素决定

Elasticsearch 的相似度算法被定义为检索词频率/反向文档频率 TF/IDF

- **检索词频率**

  检索词在该字段出现的频率？出现频率越高，相关性也越高。 字段中出现过 5 次要比只出现过 1 次的相关性高。

- **反向文档频率**

  每个检索词在索引中出现的频率？频率越高，相关性越低。检索词出现在多数文档中会比出现在少数文档中的权重更低。

- **字段长度准则**

  字段的长度是多少？长度越长，相关性越低。 检索词出现在一个短的 title 要比同样的词出现在一个长的 content 字段权重更大。

















### springboot 整合

1. 引入依赖

   ~~~xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
   </dependency>
   ~~~

2. 配置文件

   ~~~yaml
   # es配置
   es:
     username: elastic
     password: elasticsearch123
     host: localhost
     port: 9200
   ~~~

3. 配置类

   ~~~java
   @Configuration
   public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {
   
       @Value("${es.username}")
       String userName;
   
       @Value("${es.password}")
       String password;
   
       @Value("${es.host}")
       String host;
   
       @Value("${es.port}")
       int port;
   
       //设置ES官方的高级客户端
       @Bean
       @Override
       public RestHighLevelClient elasticsearchClient() {
           //设置用户名密码
           CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
           credentialsProvider.setCredentials(
                   AuthScope.ANY, new UsernamePasswordCredentials(userName, password)
           );
   
           //设置连接地址
           HttpHost[] httpHosts = new HttpHost[1];
           httpHosts[0] = new HttpHost(host, port);
   
           //设置连接客户端
           RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
                   RestClient.builder(httpHosts)
                           .setHttpClientConfigCallback(
                                   httpAsyncClientBuilder ->
                                           httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                           )
           );
           return restHighLevelClient;
       }
   
       @Bean
       public RequestOptions requestOptions() {
           return RequestOptions.DEFAULT;
       }
   }
   ~~~

4. 编写工具类

   ~~~java
   @Service
   public class ElasticsearchUtil {
   
       @Resource
       RestHighLevelClient client;
       @Resource
       RequestOptions requestOptions;
   
       //region 索引操作
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
   
       public boolean createIndex(Class indexClass) {
           Annotation annotation = indexClass.getAnnotation(ESIndex.class);
           if (annotation != null)
               return createIndex(((ESIndex) annotation).indexName(), mappingBuilder(indexClass));
           else
               return false;
       }
   
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
   
       public XContentBuilder mappingBuilder(Class esIndex) {
           Annotation annotation = esIndex.getAnnotation(ESIndex.class);
           if (annotation == null)// 没有索引注解 不生效
               return null;
           else {
               XContentBuilder xContentBuilder = null;
               Field[] declaredFields = esIndex.getDeclaredFields();
               try {
                   xContentBuilder = XContentFactory.jsonBuilder();
                   xContentBuilder.startObject();
                   {
                       xContentBuilder.startObject("properties");
                       for (Field field : declaredFields) {
                           ESField fieldAnnotation = field.getAnnotation(ESField.class);
                           if (fieldAnnotation != null) {
                               if (StrUtil.isNotBlank(fieldAnnotation.fieldName()))
                                   xContentBuilder.startObject(fieldAnnotation.fieldName());
                               else
                                   xContentBuilder.startObject(field.getName());
                               xContentBuilder.field("type", fieldAnnotation.type().getType());
                               if (fieldAnnotation.analyzer().getAnalyzer() != null)
                                   xContentBuilder.field("analyzer", fieldAnnotation.analyzer().getAnalyzer());
                               if (fieldAnnotation.type() == ESTypeEnum.DATE && StrUtil.isNotBlank(fieldAnnotation.format()))
                                   xContentBuilder.field("format", fieldAnnotation.format());
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
       public <ES extends ESObject> void insertDoc(String indexName, ES es) {
           try {
               client.index(docIndexRequest(indexName, es), requestOptions);
           } catch (IOException e) {
               log.info("insertDoc ==》 文档新增异常");
           }
       }
   
       private <ES extends ESObject> IndexRequest docIndexRequest(String indexName, ES es) throws JsonProcessingException {
           ObjectMapper json = new ObjectMapper();
           IndexRequest doc = new IndexRequest(indexName);
           doc.id(es.getId());
           doc.source(json.writeValueAsString(es), XContentType.JSON);
           return doc;
       }
   
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
       public <ES extends ESObject> void updateDoc(String indexName, ES es) {
           try {
               client.update(docUpdateRequest(indexName, es), requestOptions);
           } catch (IOException e) {
               log.info("updateDoc ==》 文档更新异常");
           }
       }
   
       private <ES extends ESObject> UpdateRequest docUpdateRequest(String indexName, ES es) throws JsonProcessingException {
           ObjectMapper json = new ObjectMapper();
           UpdateRequest doc = new UpdateRequest(indexName, es.getId());
           doc.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
           doc.doc(json.writeValueAsString(es), XContentType.JSON);
           return doc;
       }
   
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
   
       //region doc 更新
       public void deleteDoc(String indexName, String id) {
           try {
               client.delete(new DeleteRequest(indexName, id), requestOptions);
           } catch (IOException e) {
               log.info("deleteDoc ==》 文档删除异常");
           }
       }
   
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
        * 查询
        *
        * @param indexName
        * @param objClass
        * @param searchSourceBuilder
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
                   //region 高亮处理 保留高亮内容
                   if (StrUtil.isNotEmpty(higField) && hit.getHighlightFields().size() > 0) {
                       HighlightField hField = hit.getHighlightFields().get(higField);
                       if (hField != null)
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
        * 查询
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
       public <ES extends ESObject> ESPage<ES> searchMatch(String indexName, Class<ES> objClass, SearchVo search) {
           return search(indexName, objClass, new SearchSourceBuilder()
                   .query(QueryBuilders.matchQuery(search.getField(), search.getValue().getValue()))
                   .highlighter(new HighlightBuilder().field(search.getField())
                           .preTags("<span style=\"color:red\">")
                           .postTags("</span>")
                   )
                   .from(search.getFrom())
                   .size(search.getSize())
           );
       }
   
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
   
       //endregion
   
       //region 查询封装
   
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
   
       //endregion
   
       //endregion
   }
   ~~~

   



















