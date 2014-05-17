package com.github.dba.html;

import com.github.dba.model.Author;
import com.github.dba.model.Blog;
import com.github.dba.repo.BlogRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.github.dba.util.DbaUtil.*;
import static java.lang.String.format;

@Service
public class CsdnFetcher {
    private static final Log log = LogFactory.getLog(CsdnFetcher.class);
    private static final double CSDN_PAGE_COUNT = 50d;
    private static final String CSDN_KEY_WORD = "csdn";

    @Autowired
    private BlogRepository blogRepository;

    public void fetch(String url) throws Exception {
        Document doc = fetchCsdn(format("%s?viewmode=contents", url));
        double totalPage = getCsdnTotalPage(doc);
        for (int i = 2; i <= totalPage; i++) {
            fetchCsdn(format("%s/article/list/%d?viewmode=contents", url, i));
        }
    }

    private double getCsdnTotalPage(Document doc) {
        Elements statistics = doc.select("#blog_statistics li");
        int total = 0;
        for (int i = 0; i <= 2; i++) {
            total += fetchNumber(statistics.get(i).text());
        }
        return Math.ceil(total / CSDN_PAGE_COUNT);
    }

    private Document fetchCsdn(String url) throws Exception {
        Document doc = fetchUrlDoc(url);
        fetchCsdnBlog(doc, "article_toplist", url);
        fetchCsdnBlog(doc, "article_list", url);
        return doc;
    }

    private void fetchCsdnBlog(Document doc, String elementId, String url) throws Exception {
        Elements blogs = doc.select(format("#%s div.list_item.list_view", elementId));
        log.debug("blog size:" + blogs.size());

        for (Element blog : blogs) {
            Element titleLink = blog.select("div.article_title span.link_title a").get(0);
            String link = titleLink.attr("href");
            link = url.substring(0, url.lastIndexOf("/") + 1) + link;
            log.debug(format("blog detail link:%s", link));

            String title = fetchTitle(titleLink);
            String blogId = fetchBlogId(link);
            String time = blog.select("div.article_manage span.link_postdate").get(0).text();

            int view = fetchNumber(blog.select(
                    "div.article_manage span.link_view").get(0).text());

            int comment = fetchNumber(blog.select(
                    "div.article_manage span.link_comments").get(0).text());

            Document detailDoc = fetchUrlDoc(link);

            Elements tags = detailDoc.select("#article_details div.tag2box a");
            Author author = Author.getAuthorBy(tags);

            if (blogRepository.isBlogExist(CSDN_KEY_WORD, blogId)) break;

            blogRepository.createBlog(new Blog(title, link, view, comment, time, author, blogId, CSDN_KEY_WORD));
        }
    }

}