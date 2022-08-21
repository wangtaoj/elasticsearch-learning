package com.wangtao.msgsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangtao.msgsearch.constant.MsgTypeEnum;
import com.wangtao.msgsearch.entity.Msg;
import com.wangtao.msgsearch.vo.MsgSearchVO;
import com.wangtao.msgsearch.vo.TradeVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author wangtao
 * Created at 2022/8/21 17:30
 */
@Slf4j
@RequestMapping("/api/msg")
@RestController
public class MsgController {

    private static final String INDEX_NAME = "msg";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 准备数据
     */
    private List<Msg> generateData() {
        Random random = new Random();
        List<Msg> msgList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            TradeVO tradeVO = new TradeVO();
            tradeVO.setTradeNo("T20220821" + String.format("%03d", i));
            tradeVO.setMatchAmt(random.nextInt(10000));
            tradeVO.setMatchQty(tradeVO.getMatchAmt());

            Msg msg = new Msg();
            msg.setTraceNo("M20220821" + String.format("%03d", i));
            msg.setType(MsgTypeEnum.ofOrdinal(i % 3).getValue());
            try {
                msg.setContent(objectMapper.writeValueAsString(tradeVO));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            msgList.add(msg);
        }
        return msgList;
    }

    /**
     * 创建索引并指定映射
     */
    @GetMapping("/createIndex")
    public void createIndex() throws IOException {
        TypeMapping typeMapping = new TypeMapping.Builder()
                .properties("type", p -> p.keyword(k -> k))
                .properties("traceNo", p -> p.keyword(k -> k))
                .properties("content", p -> p.text(t -> t))
                .build();
        CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                .index(INDEX_NAME)
                .mappings(typeMapping)
                .build();
        BooleanResponse booleanResponse = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME));
        if (booleanResponse.value()) {
            // 如果存在则删除
            elasticsearchClient.indices().delete(d -> d.index(INDEX_NAME));
        }
        elasticsearchClient.indices().create(createIndexRequest);
    }

    /**
     * 批量插入数据
     */
    @GetMapping("/addData")
    public List<Msg> addData() throws IOException {
        List<Msg> msgList = generateData();
        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (int i = 1; i <= msgList.size(); i++) {
            Msg msg = msgList.get(i - 1);
            String finalI = i + "";
            BulkOperation bulkOperation = new BulkOperation.Builder()
                    .index(o -> o.id(finalI).document(msg))
                    .build();
            bulkOperations.add(bulkOperation);
        }
        BulkRequest bulkRequest = new BulkRequest.Builder()
                .index(INDEX_NAME)
                .operations(bulkOperations)
                .build();
        BulkResponse response = elasticsearchClient.bulk(bulkRequest);
        if (response.errors()) {
            log.error("batch insert has error!");
        }
        return msgList;
    }

    @GetMapping("/searchAll")
    public List<Msg> searchAll() throws IOException {
        /*
         * from: 默认为0
         * size: 默认为10，最大不能超过10000
         */
        SearchResponse<Msg> response = elasticsearchClient.search(
                s -> s.index(INDEX_NAME).from(0).size(50),
                Msg.class
        );
        List<Hit<Msg>> hits = response.hits().hits();
        log.info("page count: {}", hits.size());
        assert response.hits().total() != null;
        log.info("total count: {}", response.hits().total().value());
        return hits.stream().map(Hit::source).collect(Collectors.toList());
    }

    @PostMapping("/searchByCondition")
    public List<Msg> searchByCondition(@RequestBody MsgSearchVO msgSearchVO) throws IOException {
        /*
         * 类型为keyword时, 记录建立倒排索引时不会进行分词
         * 查询时使用term关键字, 条件不会被分词
         *
         * 类型为text时, 记录建立倒排索引时会进行分词
         * 查询时使用match关键字, 条件也会被分词
         */
        log.info("args: {}", msgSearchVO);
        Integer from = (msgSearchVO.getPageNo() - 1) * msgSearchVO.getPageSize();
        List<Query> andQueryList = new ArrayList<>();
        if (StringUtils.isNotBlank(msgSearchVO.getType())) {
            Query byType = new TermQuery.Builder()
                    .field("type")
                    .value(msgSearchVO.getType())
                    .build()._toQuery();
            andQueryList.add(byType);
        }
        if (StringUtils.isNotBlank(msgSearchVO.getTraceNo())) {
            Query byTraceNo = new TermQuery.Builder()
                    .field("traceNo")
                    .value(msgSearchVO.getTraceNo())
                    .build()._toQuery();
            andQueryList.add(byTraceNo);
        }
        if (StringUtils.isNotBlank(msgSearchVO.getKeyword())) {
            Query byContent = new MatchQuery.Builder()
                    .field("content")
                    .query(msgSearchVO.getKeyword())
                    .build()._toQuery();
            andQueryList.add(byContent);
        }
        Query query = new BoolQuery.Builder()
                .must(andQueryList)
                .build()._toQuery();
        SearchResponse<Msg> response = elasticsearchClient.search(s -> s
                        .index(INDEX_NAME).from(from)
                        .size(msgSearchVO.getPageSize())
                        .query(query),
                Msg.class
        );
        List<Hit<Msg>> hits = response.hits().hits();
        assert response.hits().total() != null;
        log.info("page count: {}", hits.size());
        log.info("total count: {}", response.hits().total().value());
        return hits.stream().map(Hit::source).collect(Collectors.toList());
    }
}
