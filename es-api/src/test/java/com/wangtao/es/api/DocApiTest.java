package com.wangtao.es.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.wangtao.es.api.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author wangtao
 * Created at 2024-01-12
 */
@SpringBootApplication
public class DocApiTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 根据文档id查询
     */
    public void testGetByDocId() throws IOException {
        final String docId = "1";
        GetResponse<User> reponse = elasticsearchClient.get(
                g -> g.index("user").id(docId),
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
    public void testGetByDocIdFluent() throws IOException {
        final String docId = "1";
        GetRequest getRequest = new GetRequest.Builder()
                .index("user")
                .id(docId)
                .build();
        GetResponse<User> reponse = elasticsearchClient.get(getRequest, User.class);
        if (reponse.found()) {
            User user = reponse.source();
            System.out.println(user);
        } else {
            throw new IllegalArgumentException("not found " + docId);
        }
    }

    /**
     *
     * 复杂搜索
     */
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
                .index("user")
                .query(query)
                .build();
        SearchResponse<User> response = elasticsearchClient.search(searchRequest, User.class);
        List<Hit<User>> hits = response.hits().hits();
        List<User> users = hits.stream().map(Hit::source).toList();
        users.forEach(System.out::println);
    }
}
