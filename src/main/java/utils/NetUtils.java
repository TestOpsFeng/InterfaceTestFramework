package utils;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.TextUtils;
import org.testng.Reporter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

public class NetUtils {
    public static boolean isUseSSR = false;
    public static boolean isUseFidder = false;
    static {
        //开启日志
//        System.setProperty("javax.net.debug", "all");
    }

    public static CloseableHttpResponse get(String url, HashMap headers) {
        CloseableHttpClient client = createClient();
        logGetUrl(url);
        HttpGet httpGet = new HttpGet(url);
        setHeaders(httpGet, headers);
        return getResponse(client,httpGet);
    }

    public static String getResponseString(CloseableHttpResponse response){
        try {
            String s = EntityUtils.toString(response.getEntity(), "utf-8");
            Reporter.log("返回数据为: " + s,true);
            return s;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getResponseStatusCode(CloseableHttpResponse response){
        int statusCode = response.getStatusLine().getStatusCode();
        Reporter.log("返回码:" + statusCode,true);
        return response.getStatusLine().getStatusCode();
    }

    public static CloseableHttpResponse get(String url) {
        CloseableHttpClient client = createClient();
        logGetUrl(url);
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        return getResponse(client,httpGet);
    }

    private static void logGetUrl(String url) {
        Reporter.log("调用GET接口: " + url,true);
    }

    public static InputStream getIS(String url) {
        CloseableHttpClient client = createClient();
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpGet);
            return response.getEntity().getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream getIS(String url, HashMap<String, String> headers) {
        CloseableHttpClient client = createClient();
        HttpGet httpGet = new HttpGet(url);
        setHeaders(httpGet, headers);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpGet);
            return response.getEntity().getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据isUseSSR判断HttpClient的创建
     *
     * @return
     */
    public static CloseableHttpClient createClient() {
        CloseableHttpClient client = null;
        SSLContext ignoreVerifySSL = null;
        try {
            ignoreVerifySSL = createIgnoreVerifySSL();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(ignoreVerifySSL)).build();
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

            HttpClients.custom().setConnectionManager(connManager);
            client = proxy().setConnectionManager(connManager).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return client;
    }

    /**
     * 绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
//        SSLContext sc = SSLContext.getInstance("SSLv3");
        SSLContext sc = SSLContext.getInstance("TLSv1.2");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sc.init(null, new TrustManager[]{trustManager}, null);
        return sc;
    }

    public static HttpClientBuilder proxy() {
        // 依次是代理地址，代理端口号，协议类型
        //设置翻墙代理
        HttpClientBuilder httpClientBuilder = null;
        if (isUseSSR) {
            HttpHost proxy = new HttpHost("127.0.0.1",1080, "http");
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClientBuilder = HttpClients.custom().setRoutePlanner(routePlanner);
        }else {
            httpClientBuilder = HttpClients.custom();
        }
        //设置cookie默认规则
        setCookiesConfig(httpClientBuilder);

        //设置Fiddler
        if (isUseFidder) {
            setFiddler(httpClientBuilder);
        }
        return httpClientBuilder;
    }

    /**
     * 传入httpGet或者httpPost
     *
     * @param httpClientBuilder
     */
    private static void setFiddler(HttpClientBuilder httpClientBuilder) {
        HttpHost proxy = new HttpHost("127.0.0.1", 11111);
        RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(proxy)
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(3000)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
    }

    /**
     * 设置cookie默认规则.IGNORE_COOKIES 或者 .DEFAULT
     *
     * @param httpClientBuilder
     */
    public static void setCookiesConfig(HttpClientBuilder httpClientBuilder) {
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();
        httpClientBuilder.setDefaultRequestConfig(globalConfig);
    }

    /**
     * x-www-form-urlencoded格式的post请求
     *
     * @param url
     * @param headers
     * @param body
     * @return
     * @throws IOException
     */
    public static CloseableHttpResponse post(String url, HashMap headers, HashMap body) {
        CloseableHttpClient client = createClient();
        HttpPost httpPost = new HttpPost(url);
        logPostUrl(url);
        setHeaders(httpPost, headers);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        logBody(body);
        for (Iterator iter = body.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String value = String.valueOf(body.get(name));
            nvps.add(new BasicNameValuePair(name, value));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return getResponse(client, httpPost);
    }

    private static void logPostUrl(String url) {
        Reporter.log("调用POST接口: " + url,true);
    }

    private static void logBody(HashMap body) {
        Reporter.log("Body:",true);
        for (Iterator iter = body.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String value = String.valueOf(body.get(name));
            Reporter.log(name + ": " + value,true);
        }
    }

    /**
     * json格式的post请求
     *
     * @param url
     * @param json
     * @return
     */
    public static CloseableHttpResponse post(String url, String json) {
        CloseableHttpClient client = createClient();
        HttpPost httpPost = new HttpPost(url);
        logPostUrl(url);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Content-Type", "application/json");
        setHeaders(httpPost, map);
        setJson(json, httpPost);
        return getResponse(client, httpPost);
    }

    private static void setJson(String json, HttpPost httpPost) {
        String charSet = "UTF-8";
        Reporter.log("Set Json: " , true);
        Reporter.log(json ,true);
        if (!TextUtils.isEmpty(json)){
            StringEntity stringEntity = new StringEntity(json, charSet);
            httpPost.setEntity(stringEntity);
        }
    }

    private static CloseableHttpResponse getResponse(CloseableHttpClient client, HttpRequestBase http) {
        CloseableHttpResponse response = null;
        try {
            response = client.execute(http);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CloseableHttpResponse post(String url, HashMap headers, String json){
        CloseableHttpClient client = createClient();
        logPostUrl(url);
        HttpPost httpPost = new HttpPost(url);
        setHeaders(httpPost, headers);
        setJson(json,httpPost);
        return getResponse(client,httpPost);
    }


    private static void setHeaders(HttpRequestBase http, HashMap headers) {
        /*
        Content-Type:application/json
        Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJSN05oWTBueXVOY1BNbHZrRjJlNXJCS0VpLU9yVFZfak52R2RIUVZEMG9JIn0.eyJqdGkiOiI5MmJkMTNkNi1jOTg3LTQ0YmEtODM5Zi1iZTJkNjQ4MWZlMGMiLCJleHAiOjE1MzgzMDE4NjcsIm5iZiI6MCwiaWF0IjoxNTM4MjgwMjY3LCJpc3MiOiJodHRwczovL2hhdHN0LmNpc3lzdGVtc29sdXRpb25zLmNvbS9hdXRoL3JlYWxtcy9wdGFoIiwiYXVkIjoicHRhaCIsInN1YiI6ImMwNjMyMjI4LTkwNjctNDI0NC05ZWM3LWE5NzljMWM2OWRjMiIsInR5cCI6IkJlYXJlciIsImF6cCI6InB0YWgiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiI0N2EwNDcxOS04NTM4LTQ0YTctYjZhZC1jZDg2OWRiNDYwY2IiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19LCJwdGFoIjp7InJvbGVzIjpbIlNpdGVfT3BlcmF0aW9uIiwiU2l0ZV9QZXJzb25uZWwiLCJBY2NvdW50X0FkbWluIiwiSW50ZXJuYWwiLCJTaXRlX0FkbWluIiwiQ1dSU19BZG1pbiJdfX0sInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluQGludGVybmFsLmNvbSIsImVtYWlsIjoibGl6aXpoZW5AY2lzeXN0ZW1zb2x1dGlvbnMuY29tIn0.b1dVLD59H4unWGpnUCg6yDwHCgGLxc0DHF8VTYYpTBlU0CT5j-oWAcuREt6zbM9el9fB8dnW7ob3xfsObDJGjjngWXhLfy_Q04FBv_Et12-j8NhIyX73RMqDJfUScovJ2S-Kvw-eOV6EJ87EHWiSqzl2S41Zcgjy-rT9eUEH4FXip3825Ep8Vj3TT33Ip46jtm3HBu4nJ7Ad9lkH5uZV2kjkzW6gSoTo1E15muNl9ZQFxuebs1QGCdY858B_Dw2k_4_VkIJKLLEYru_Z0ZOCQe9d55qC7mj2S3TyasxQsCA5JfQO8AftKrY6rJHNHNhW58McBNU-rFhG1v_wRgeBnQ
         */
        Reporter.log("Set Headers:",true);
        if (headers != null) {
            Set set = headers.keySet();
            Iterator setIterator = set.iterator();
            Collection values = headers.values();
            Iterator valuesIterator = values.iterator();
            while (setIterator.hasNext() && valuesIterator.hasNext()) {
                Object key = setIterator.next();
                Object value = valuesIterator.next();
                Reporter.log(key + ": " + value,true);
                http.setHeader((String) key, (String) value);
            }
        }
    }
}
