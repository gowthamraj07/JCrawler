package com.jcrawler.pdf.controller;


import com.jcrawler.pdf.dto.PdfCrawler;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class PdfCrawlController {
	
	public static boolean PROXY_AVAILABLE = false;
	public static String PROXY_HOST = "proxy_address";
	public static Integer PROXY_PORT = 80;
	public static String PROXY_USER_NAME = "user_name";
	public static String PROXY_PASSWORD = "password";

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
        	args = new String[3];
        	args[0] = "temp";
        	args[1] = "2";
        	args[2] = "output";
        }

        String rootFolder = args[0];
        int numberOfCrawlers = Integer.parseInt(args[1]);
        String storageFolder = args[2];

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(rootFolder);
        config.setIncludeBinaryContentInCrawling(true);
        config.setMaxDepthOfCrawling(7);
        
        if(PROXY_AVAILABLE) {
        	config.setProxyHost(PROXY_HOST);
        	config.setProxyPort(PROXY_PORT);
        	config.setProxyUsername(PROXY_USER_NAME);
        	config.setProxyPassword(PROXY_PASSWORD);
        }

        String[] crawlDomains = {"http://www.alldatasheet.com/","http://pdf1.alldatasheet.com/"};

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        for (String domain : crawlDomains) {
            controller.addSeed(domain);
        }

        PdfCrawler.configure(crawlDomains, storageFolder);

        controller.start(PdfCrawler.class, numberOfCrawlers);
    }
}
