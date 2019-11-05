package com.mingyin;

import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisDemo1 {

    private static final int ARTICLES_PER_PAGE = 5;

    public static void main(String[] args) {
        new RedisDemo1().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(10);

//        String articleId = postArticle(
//                conn, "username3", "A title3", "http://www.google.com");
//        System.out.println("We posted a new article with id: " + articleId);

        String articleId = "2";
        articleVote(conn, "other_user", "article:" + articleId);

        List<Map<String,String>> articles = getArticlesscore(conn, 1);
        printArticles(articles);
    }

    public String postArticle(Jedis conn, String user, String title, String link) {
        String articleId = String.valueOf(conn.incr("article:"));

        String voted = "voted:" + articleId;
        conn.sadd(voted, user);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String,String> articleData = new HashMap<String,String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);
        conn.zadd("time:", now, article);
        conn.zadd("score:", now + 1, article);

        return articleId;
    }

    public void articleVote(Jedis conn, String user, String article) {
        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1) {
            conn.zincrby("score:", 1, article);
            conn.hincrBy(article, "votes", 1);
        }
    }
    public List<Map<String,String>> getArticles1(Jedis conn, int page) {
        return getArticles(conn,page,"time:");
    }
    public List<Map<String,String>> getArticlesscore(Jedis conn, int page) {
        return getArticles(conn,page,"score:");
    }
    public List<Map<String,String>> getArticles(Jedis conn, int page,String order) {
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }

    private void printArticles(List<Map<String,String>> articles){
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}