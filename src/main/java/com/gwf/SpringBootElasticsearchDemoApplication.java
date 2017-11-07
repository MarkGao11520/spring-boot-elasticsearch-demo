package com.gwf;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class SpringBootElasticsearchDemoApplication {

    @Autowired
    private TransportClient client;

    @GetMapping("/get/book/novel")
    @ResponseBody
    public ResponseEntity get(@RequestParam(name = "id", defaultValue = "") String id) {
        if (id.isEmpty())
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        GetResponse result = this.client.prepareGet("book", "novel", id)
                .get();
        if (!result.isExists())
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        return new ResponseEntity(result.getSource(), HttpStatus.OK);
    }


    @PostMapping("/add/book/novel")
    @ResponseBody
    public ResponseEntity add(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "word_count") int wordCount,
            @RequestParam(name = "publish_date")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date publishDate
    ) {
        try {
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("author", author)
                    .field("word_count", wordCount)
                    .field("publishDate", publishDate.getTime())
                    .endObject();
            IndexResponse result = this.client.prepareIndex("book", "novel")
                    .setSource(content)
                    .get();
            return new ResponseEntity(result.getId(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @DeleteMapping("/delete/book/novel")
    @ResponseBody
    public ResponseEntity delete(@RequestParam(name = "id") String id) {
        DeleteResponse result = this.client.prepareDelete("book", "novel", id).get();

        return new ResponseEntity(result.getResult().toString(), HttpStatus.OK);
    }

    @PutMapping("/update/book/novel")
    @ResponseBody
    public ResponseEntity update(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "word_count", required = false) Integer wordCount,
            @RequestParam(name = "publish_date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date publishDate
    ) {
        UpdateRequest update = new UpdateRequest("book", "novel", id);
        try {
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject();
            if (title != null)
                content.field("title", title);
            if (author != null)
                content.field("author", author);
            if (wordCount != null)
                content.field("word_count", wordCount);
            if (publishDate != null)
                content.field("publishDate", publishDate.getTime());
            content.endObject();
            update.doc(content);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            UpdateResponse result = this.client.update(update).get();
            return new ResponseEntity(result.getResult().toString(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("query/book/novel")
    @ResponseBody
    public ResponseEntity quert(
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "gt_work_count", defaultValue = "0") int gtWorkCount,
            @RequestParam(name = "lt_work_count", required = false) Integer ltWorkCount) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if(author!=null)
            boolQuery.must(QueryBuilders.matchQuery("author",author));
        if(title!=null)
            boolQuery.must(QueryBuilders.matchQuery("title",title));

        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count")
                .from(gtWorkCount);
        if(ltWorkCount!=null&&ltWorkCount>0){
            rangeQuery.to(ltWorkCount);
        }

        boolQuery.filter(rangeQuery);

        SearchRequestBuilder builder =  this.client.prepareSearch("book")
                .setTypes("novel")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolQuery)
                .setFrom(0)
                .setSize(10);

        System.out.println(builder);

        SearchResponse response = builder.get();

        List<Map<String,Object>> result = new ArrayList<>();

        for(SearchHit hit:response.getHits()){
            result.add(hit.getSource());
        }

        return new ResponseEntity(result,HttpStatus.OK);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootElasticsearchDemoApplication.class, args);
    }
}
