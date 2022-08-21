package com.wangtao.msgsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.wangtao.msgsearch.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangtao
 * Created at 2022/8/21 15:19
 */
@Slf4j
@RequestMapping("/api/user")
@RestController
public class UserController {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 根据文档id查询
     */
    @GetMapping("/{docId}")
    public User getByDocId(@PathVariable String docId) throws IOException {
        GetResponse<User> reponse = elasticsearchClient.get(
                g -> g.index("user").id(docId),
                User.class
        );
        if (reponse.found()) {
            return reponse.source();
        } else {
            throw new IllegalArgumentException("not found " + docId);
        }
    }

    /**
     * 根据文档id查询
     */
    @GetMapping("/getByDocIdFluent/{docId}")
    public User getByDocIdFluent(@PathVariable String docId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index("user")
                .id(docId)
                .build();
        GetResponse<User> reponse = elasticsearchClient.get(getRequest, User.class);
        if (reponse.found()) {
            return reponse.source();
        } else {
            throw new IllegalArgumentException("not found " + docId);
        }
    }

    /**
     *
     * 复杂搜索
     */
    @GetMapping("/search")
    public List<User> search() throws IOException {
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
        return hits.stream().map(Hit::source).collect(Collectors.toList());
    }
}
