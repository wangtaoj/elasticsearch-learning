package com.wangtao.es.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author wangtao
 * Created at 2024-01-12
 */
@Slf4j
@SpringBootTest
public class IndexApiTest {

    private static final String INDEX = "user";

    @Autowired
    private ElasticsearchClient esClient;

    /**
     * 所有的API提供两种形式, Builder以及lambda
     */
    @Test
    public void testCreate() throws IOException {
        BooleanResponse existsResponse = esClient.indices()
                .exists(builder -> builder.index(INDEX));
        if (existsResponse.value()) {
            // 存在则先删除
            DeleteIndexResponse deleteResponse = esClient.indices()
                    .delete(builder -> builder.index(INDEX));
            if (deleteResponse.acknowledged()) {
                log.info("index '{}' is deleted!", INDEX);
            }
        }
        // 新增索引, 其中birthday、createTime不能作为搜索条件
        TypeMapping typeMapping = new TypeMapping.Builder()
                .dynamic(DynamicMapping.Strict)
                .properties("userId", p -> p.integer(i -> i))
                .properties("name", p -> p.text(t -> t))
                .properties("age", p -> p.integer(i -> i))
                .properties("birthday", p -> p.date(d -> d.index(false).format("yyyy-MM-dd")))
                .properties("createTime", p -> p.date(d -> d.index(false).format("yyyy-MM-dd HH:mm:ss")))
                .build();
        esClient.indices().create(new CreateIndexRequest.Builder()
                .index(INDEX)
                .mappings(typeMapping)
                .build()
        );
    }

    /**
     * 根据JSON内容创建索引
     */
    @Test
    public void testCreateByJson() throws IOException {
        BooleanResponse existsResponse = esClient.indices()
                .exists(builder -> builder.index(INDEX));
        if (existsResponse.value()) {
            // 存在则先删除
            DeleteIndexResponse deleteResponse = esClient.indices()
                    .delete(builder -> builder.index(INDEX));
            if (deleteResponse.acknowledged()) {
                log.info("index '{}' is deleted!", INDEX);
            }
        }
        String mappings = """
                {
                  "mappings" : {
                    "dynamic" : "strict",
                    "properties" : {
                      "age" : {
                        "type" : "integer"
                      },
                      "birthday" : {
                        "type" : "date",
                        "index" : false,
                        "format" : "yyyy-MM-dd"
                      },
                      "createTime" : {
                        "type" : "date",
                        "index" : false,
                        "format" : "yyyy-MM-dd HH:mm:ss"
                      },
                      "name" : {
                        "type" : "text"
                      },
                      "userId" : {
                        "type" : "integer"
                      }
                    }
                  }
                }
                """;
        esClient.indices().create(new CreateIndexRequest.Builder()
                .index(INDEX)
                .withJson(new StringReader(mappings))
                .build()
        );
    }

    /**
     * mapping操作
     * getMapping() 查询
     * putMapping() 添加
     */
    @Test
    public void testGetMapping() throws IOException {
        GetMappingResponse mappingResponse = esClient.indices().getMapping(builder -> builder.index(INDEX));
        IndexMappingRecord indexMappingRecord = mappingResponse.get(INDEX);
        TypeMapping typeMapping = indexMappingRecord.mappings();
        log.info("dynamic: {}", typeMapping.dynamic());
        log.info("mappings: {}", typeMapping);
    }

    /**
     * 分词
     */
    @Test
    public void testAnalyze() throws IOException {
        AnalyzeResponse analyzeResponse = esClient.indices().analyze(
                new AnalyzeRequest.Builder()
                        .index(INDEX)
                        .field("name")
                        .text("wang tao")
                        .build()
        );
        analyzeResponse.tokens().forEach(token -> log.info("{}", token));
    }
}
