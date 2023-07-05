package cn.vincent.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Slf4j
@Configuration
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${es.username}")
    String userName;

    @Value("${es.password}")
    String password;

    @Value("${es.host}")
    String host;

    @Value("${es.port}")
    int port;

    /**
     * 连接ES客户端
     * @return
     */
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

        //
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
