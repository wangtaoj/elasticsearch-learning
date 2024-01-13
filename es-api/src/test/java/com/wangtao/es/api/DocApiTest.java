package com.wangtao.es.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CreateRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateAction;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.wangtao.es.api.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2024-01-12
 */
@Slf4j
@SpringBootTest
public class DocApiTest {

    private static final String INDEX = "user";

    @Autowired
    private ElasticsearchClient esClient;

    /**
     * 新增或者修改
     * 修改时会修改所有字段
     * POST /user/_doc/1
     */
    @Test
    public void testCreateOrUpdate() throws IOException {
        User user = new User()
                .setUserId(1)
                .setName("zhang san")
                .setAge(30)
                .setBirthday(LocalDate.of(1997, 5, 2))
                .setCreateTime(LocalDateTime.now());
        esClient.index(new IndexRequest.Builder<User>()
                .index(INDEX)
                .id(String.valueOf(user.getUserId()))
                .document(user)
                .build()
        );
    }

    /**
     * 新增文档, 存在则报错
     * POST /user/_create/1
     */
    @Test
    public void testInsert() throws IOException {
        User user = new User()
                .setUserId(1)
                .setName("zhang san")
                .setAge(30)
                .setBirthday(LocalDate.of(1997, 5, 2))
                .setCreateTime(LocalDateTime.now());
        esClient.create(new CreateRequest.Builder<User>()
                .index(INDEX)
                .id(String.valueOf(user.getUserId()))
                .document(user)
                .build()
        );
    }

    /**
     * 修改文档, 不存在则报错
     * 支持只修改指定字段
     * POST /user/_update/1
     */
    @Test
    public void testUpdate() throws IOException {
        // 将年龄改成35
        Map<String, Object> updateDoc = Map.of("age", 35);
        esClient.update(
                new UpdateRequest.Builder<User, Map<String, Object>>()
                        .index(INDEX)
                        .id("1")
                        .doc(updateDoc)
                        .build(),
                User.class
        );
    }

    /**
     * 删除文档
     * DELETE /user/_doc/1
     */
    @Test
    public void testDelete() throws IOException {
        esClient.delete(new DeleteRequest.Builder()
                .index(INDEX)
                .id("1")
                .build()
        );
    }

    /**
     * 批量操作(_bulk api)
     * index: 新增或修改
     * create: 新增
     * update: 修改
     * delete: 删除
     * 注意: _bulk api批量操作每个操作互不影响, 报错不会影响别的记录, 会返回每个操作的结果是成功还是失败
     */
    @Test
    public void testBatch() throws IOException {
        List<User> userList = generateData();
        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (User user : userList) {
            BulkOperation bulkOperation;
            // 当id=2时, 为update操作, 其余都为index操作
            if (user.getUserId().equals(2)) {
                UpdateAction<User, User> updateAction = new UpdateAction.Builder<User, User>()
                        .doc(user)
                        .build();
                UpdateOperation<User, User> updateOperation = new UpdateOperation.Builder<User, User>()
                        .id(String.valueOf(user.getUserId()))
                        .action(updateAction)
                        .build();
                bulkOperation = new BulkOperation.Builder()
                        .update(updateOperation)
                        .build();
            } else {
                bulkOperation = new BulkOperation.Builder()
                        .index(i -> i.id(String.valueOf(user.getUserId())).document(user))
                        .build();
            }
            bulkOperations.add(bulkOperation);
        }
        BulkRequest bulkRequest = new BulkRequest.Builder()
                .index(INDEX)
                .operations(bulkOperations)
                .build();
        BulkResponse response = esClient.bulk(bulkRequest);
        // 是否存在错误, 只要有一个操作出错则返回true
        if (response.errors()) {
            log.error("batch operations has error!");
        }
    }

    private List<User> generateData() {
        List<User> userList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User user = new User()
                    .setUserId(i)
                    .setName("user_" + i)
                    .setAge(30)
                    .setBirthday(LocalDate.of(1997, 5, 2))
                    .setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        return userList;
    }

    /**
     * 根据文档id查询
     */
    @Test
    public void testGetByDocId() throws IOException {
        final String docId = "1";
        GetResponse<User> reponse = esClient.get(
                g -> g.index(INDEX).id(docId),
                User.class
        );
        if (reponse.found()) {
            User user = reponse.source();
            System.out.println(user);
        } else {
            throw new IllegalArgumentException("not found " + docId);
        }
    }

    /**
     * 根据文档id查询
     */
    @Test
    public void testGetByDocIdFluent() throws IOException {
        final String docId = "1";
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(docId)
                .build();
        GetResponse<User> reponse = esClient.get(getRequest, User.class);
        if (reponse.found()) {
            User user = reponse.source();
            System.out.println(user);
        } else {
            throw new IllegalArgumentException("not found " + docId);
        }
    }

    /**
     * 复杂搜索
     */
    @Test
    public void testSearch() throws IOException {
        Query byName = new MatchQuery.Builder()
                .field("name")
                .query("zhang")
                .build()._toQuery();
        TermsQueryField termsQueryField = new TermsQueryField.Builder()
                .value(Arrays.asList(FieldValue.of(25), FieldValue.of(20)))
                .build();
        Query inAge = new TermsQuery.Builder()
                .field("age")
                .terms(termsQueryField)
                .build()._toQuery();
        Query query = new BoolQuery.Builder()
                .must(byName, inAge)
                .build()._toQuery();
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX)
                .query(query)
                .build();
        SearchResponse<User> response = esClient.search(searchRequest, User.class);
        List<Hit<User>> hits = response.hits().hits();
        List<User> users = hits.stream().map(Hit::source).toList();
        users.forEach(System.out::println);
    }

    /**
     * 通过JSON构造查询条件
     */
    @Test
    public void testSearchByJson() throws IOException {
        String queryJson = """
                {
                  "query": {
                    "match": {
                      "name": "user_1"
                    }
                  }
                }
                """;
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX)
                .withJson(new StringReader(queryJson))
                .build();
        SearchResponse<User> response = esClient.search(searchRequest, User.class);
        List<Hit<User>> hits = response.hits().hits();
        List<User> users = hits.stream().map(Hit::source).toList();
        users.forEach(System.out::println);
    }
}
