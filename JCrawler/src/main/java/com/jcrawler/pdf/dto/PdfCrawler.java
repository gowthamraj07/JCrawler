package com.jcrawler.pdf.dto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.common.io.Files;
import com.jcrawler.pdf.controller.PdfCrawlController;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

public class PdfCrawler  extends WebCrawler {

    private static final Pattern filters = Pattern.compile(
        ".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v" +
        "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    private static final Pattern pdfPatterns = Pattern.compile(".*(\\.(pdf))$");

    private static File storageFolder;
    private static String[] crawlDomains;

    public static void configure(String[] domain, String storageFolderName) {
        crawlDomains = domain;

        storageFolder = new File(storageFolderName);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        if (filters.matcher(href).matches()) {
            return false;
        }

        if (pdfPatterns.matcher(href).matches()) {
        	downloadPDF(href);
            return true;
        }

        for (String domain : crawlDomains) {
            if (href.startsWith(domain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        String hashedName = UUID.randomUUID() + url.substring(url.lastIndexOf("."));
        String filename = storageFolder.getAbsolutePath() + "/" + hashedName;
        try {
            Files.write(page.getContentData(), new File(filename));
            logger.info("Stored: {}", url);
        } catch (IOException iox) {
            logger.error("Failed to write file: " + filename, iox);
        }
    }
    
    public void downloadPDF(final String url) {
    	Thread tempThread = new Thread() {
    		@Override
    		public void run() {
    			System.out.println("Downloading "+url);
    			String fileName = "temp2/"+url.substring(url.lastIndexOf('/')+1);
				try {
					HttpClient httpclient = null;
					if(PdfCrawlController.PROXY_AVAILABLE) {
						HttpHost proxy = new HttpHost(PdfCrawlController.PROXY_HOST, PdfCrawlController.PROXY_PORT, "http");
						Credentials credentials = new UsernamePasswordCredentials(PdfCrawlController.PROXY_USER_NAME, PdfCrawlController.PROXY_PASSWORD);
						AuthScope authScope = new AuthScope(PdfCrawlController.PROXY_HOST, PdfCrawlController.PROXY_PORT);
						CredentialsProvider credsProvider = new BasicCredentialsProvider();
						credsProvider.setCredentials(authScope, credentials);

						httpclient = HttpClientBuilder.create().setProxy(proxy).setDefaultCredentialsProvider(credsProvider).build();
					}
					httpclient = HttpClientBuilder.create().build();
					HttpResponse response = httpclient.execute(new HttpGet(url));
					System.out.println("Response Code : "+response.getStatusLine().getStatusCode());
					if(response.getStatusLine().getStatusCode() == 200) {
						byte b[] = EntityUtils.toByteArray(response.getEntity());
						FileOutputStream fos = new FileOutputStream(new File(fileName));
						fos.write(b);
						fos.close();
					}
				} catch (Exception e) {
					System.out.println("Error in downloading file : "+fileName+", Exception : "+e.getMessage());
				}
    		}
    	};
    	tempThread.run();
    }
}