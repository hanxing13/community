package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticTemplate;

    public void saveDiscussPost(DiscussPost post){
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id){
        discussRepository.deleteById(id);
    }

    public Map<String, Object> searchDiscussPost(String keyword, int current, int limit) throws IOException {
        // 查询的字段
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("content", keyword))
                .should(QueryBuilders.matchQuery("title", keyword));
        // 构建高亮查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 排序
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withQuery(boolQueryBuilder)
                // 分页
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title"),
                        new HighlightBuilder.Field("content")
                )
                // 高亮
                .withHighlightBuilder(new HighlightBuilder().preTags("<em>").postTags("</em>"))
//                .withHighlightBuilder(new HighlightBuilder().preTags("<strong>").postTags("</strong>"))     // 加粗
                .build();
        // 查询
        SearchHits<DiscussPost> search = elasticTemplate.search(searchQuery, DiscussPost.class);
        // 得到查询返回的内容
        List<SearchHit<DiscussPost>> searchHits = search.getSearchHits();
        // 设置一个最后需要返回的实体类集合，和帖子总个数
        Map<String, Object> map = new HashMap<>();
        int rows = (int) search.getTotalHits();
        map.put("rows", rows);
        List<DiscussPost> posts = new ArrayList<>();
        // 遍历返回的内容进行处理
        for (SearchHit<DiscussPost> searchHit : searchHits){
            // 高亮的内容
            Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
            // 将高亮的内容填充到content中
            searchHit.getContent().setTitle(highlightFields.get("title") == null ? searchHit.getContent().getTitle():highlightFields.get("title").get(0));
            searchHit.getContent().setContent(highlightFields.get("content") == null ? searchHit.getContent().getContent():highlightFields.get("content").get(0));
            // 放到实体类中
            posts.add(searchHit.getContent());
        }
        map.put("posts",posts);
        return map;
    }
}
