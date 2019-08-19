package com.katalon.jenkins.plugin;

import hidden.jth.org.apache.http.HttpEntity;
import hidden.jth.org.apache.http.HttpHeaders;
import hidden.jth.org.apache.http.HttpResponse;
import hidden.jth.org.apache.http.NameValuePair;
import hidden.jth.org.apache.http.client.HttpClient;
import hidden.jth.org.apache.http.client.config.RequestConfig;
import hidden.jth.org.apache.http.client.entity.UrlEncodedFormEntity;
import hidden.jth.org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import hidden.jth.org.apache.http.client.methods.HttpUriRequest;
import hidden.jth.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import hidden.jth.org.apache.http.entity.InputStreamEntity;
import hidden.jth.org.apache.http.impl.NoConnectionReuseStrategy;
import hidden.jth.org.apache.http.impl.client.HttpClientBuilder;
import hidden.jth.org.apache.http.ssl.SSLContextBuilder;
import hidden.jth.org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class HttpHelper {


  private static final int DEFAULT_CONNECT_TIMEOUT = Integer.MAX_VALUE;

  private static final int DEFAULT_SOCKET_TIMEOUT = Integer.MAX_VALUE;


  private static HttpClient getHttpClient() {
    return getHttpClient(DEFAULT_CONNECT_TIMEOUT);
  }

  public static HttpResponse sendRequest(
      HttpUriRequest httpRequest,
      String bearerToken,
      String username,
      String password,
      InputStream content,
      Long contentLength,
      List<NameValuePair> pairs) throws IOException {

    HttpClient httpClient = getHttpClient();

    if (bearerToken != null) {
      httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
    }

    if (username != null) {
      String basicToken = username + ":" + password;
      String encodedBasicToken = Base64.getEncoder().encodeToString(basicToken.getBytes());
      httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedBasicToken);
    }

    HttpEntity entity = null;

    if (content != null) {
      entity = new InputStreamEntity(content, contentLength != null ? contentLength : -1);
    }

    if (pairs != null) {
      entity = new UrlEncodedFormEntity(pairs);
    }

    if (entity != null) {
      ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
    }

    HttpResponse httpResponse = httpClient.execute(httpRequest);

    return httpResponse;
  }

  private static HttpClientBuilder getHttpClientBuilder() {
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    SSLConnectionSocketFactory sslSocketFactory = getSslSocketFactory();
    httpClientBuilder.setSSLSocketFactory(sslSocketFactory)
        .setConnectionReuseStrategy(new NoConnectionReuseStrategy());
    return httpClientBuilder;
  }

  private static HttpClient getHttpClient(int connectTimeout) {

    RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(connectTimeout)
        .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
        .build();
    HttpClientBuilder httpClientBuilder = getHttpClientBuilder();
    httpClientBuilder.setDefaultRequestConfig(config);
    return httpClientBuilder.build();
  }

  private static SSLConnectionSocketFactory getSslSocketFactory() {
    SSLContext sslContext = getSslContext();
    HostnameVerifier skipHostnameVerifier = new SkipHostnameVerifier();
    SSLConnectionSocketFactory sslSocketFactory =
        new SSLConnectionSocketFactory(sslContext, skipHostnameVerifier);
    return sslSocketFactory;
  }

  private static SSLContext getSslContext() {
    try {
      SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      TrustStrategy trustStrategy = new TrustAllStrategy();
      sslContextBuilder.loadTrustMaterial(keyStore, trustStrategy);
      sslContextBuilder.useProtocol("TLSv1.2");
      SSLContext sslContext = sslContextBuilder.build();
      return sslContext;
    } catch (Exception e) {
//            return exceptionHelper.wrap(e);
      return null;
    }
  }

  /**
   * Trust all certificates.
   */
  private static class TrustAllStrategy implements TrustStrategy {

    @Override
    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      return true;
    }
  }

  private static class SkipHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String s, SSLSession sslSession) {
      return true;
    }

  }
}
