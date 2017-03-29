package com.jcrawler.recipe.dto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.jcrawler.recipe.controller.RecipeCrawlController;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

public class RecipeCrawler extends WebCrawler {
	private static final Pattern filters = Pattern.compile(
			".*(\\.(ico|gif|png|jpg|css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	private static final Pattern htmlPatterns = Pattern.compile(".*");

	private static File storageFolder;

	private static int counter = 0;
	
	private static Proxy proxy;
	private Map<String, String> cookies = null;

	public static void configure(String[] domain, String storageFolderName) {

		storageFolder = new File(storageFolderName);
		if (!storageFolder.exists()) {
			storageFolder.mkdirs();
		}
		proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(RecipeCrawlController.PROXY_HOST, RecipeCrawlController.PROXY_PORT));
		
		final String authUser = RecipeCrawlController.PROXY_USER_NAME;
		final String authPassword = RecipeCrawlController.PROXY_PASSWORD;
		Authenticator.setDefault(
		   new Authenticator() {
		      @Override
		      public PasswordAuthentication getPasswordAuthentication() {
		         return new PasswordAuthentication(
		               authUser, authPassword.toCharArray());
		      }
		   }
		);

		System.setProperty("http.proxyUser", authUser);
		System.setProperty("http.proxyPassword", authPassword);


	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		if (filters.matcher(href).matches()) {
			return false;
		}
		
		return true;
	}

	@Override
	public void visit(final Page page) {
		final String url = page.getWebURL().getURL();
		if (!htmlPatterns.matcher(url).matches()) {
			return;
		}
		new Thread() {
			public void run() {
				parseIngredients(page);
			}
		}.start();
	}

	private void parseIngredients(final Page page) {
		try {
			URL url = new URL(page.getWebURL().getURL());
			System.out.println(page.getWebURL().getURL());
			URLConnection conn = null;
			if(RecipeCrawlController.PROXY_AVAILABLE) {
				conn = url.openConnection(proxy);
			} else {
				conn = url.openConnection();
			}
			
			//Set Cookies if we received already
			if(cookies != null) {
				setCookies(conn, cookies);
			}
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			conn.connect();
			
			//Retrieve cookies if any available and use it for next connection
			cookies = retrieveCoockies(conn);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			PrintWriter out = new PrintWriter("temp2/"+(++counter)+".txt");
			System.out.println("Writing file "+(counter)+".txt");
			String line = null;
			
			while((line = br.readLine()) != null) {
				out.println(line);
			}
			br.close();
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, String> retrieveCoockies(URLConnection conn) {
		String headerName=null;
		Map<String, String> cookies = new HashMap<String, String>();
		for (int i=1; (headerName = conn.getHeaderFieldKey(i))!=null; i++) {
			if (headerName.equals("Set-Cookie")) {                  
				String cookie = conn.getHeaderField(i);      
				cookie = cookie.substring(0, cookie.indexOf(";"));
				String cookieName = cookie.substring(0, cookie.indexOf("="));
				String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
				cookies.put(cookieName, cookieValue);
			}
		}
		return cookies;
	}
	
	private void setCookies(URLConnection conn, Map<String, String> cookies) {
		String myCookies = "";
		for(Map.Entry<String, String> entry : cookies.entrySet()) {
			myCookies += entry.getKey()+"="+entry.getValue()+";";
		}
		conn.setRequestProperty("Cookie", myCookies);
	}
}



