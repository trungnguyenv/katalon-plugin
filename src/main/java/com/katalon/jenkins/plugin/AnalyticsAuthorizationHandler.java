package com.katalon.jenkins.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hidden.jth.org.apache.http.HttpHeaders;
import hidden.jth.org.apache.http.HttpResponse;
import hidden.jth.org.apache.http.NameValuePair;
import hidden.jth.org.apache.http.client.HttpResponseException;
import hidden.jth.org.apache.http.client.methods.HttpPost;
import hidden.jth.org.apache.http.client.methods.HttpPut;
import hidden.jth.org.apache.http.client.utils.URIBuilder;
import hidden.jth.org.apache.http.message.BasicNameValuePair;
import org.apache.tools.ant.taskdefs.condition.Http;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class AnalyticsAuthorizationHandler {

  private static String TOKEN_URI = "/oauth/token";

  private static  String EXECUTE_JOB = "/api/v1/run-configurations/%s/execute";

  private static String serverApiOAuth2GrantType = "password";

  private static String serverApiOAuth2ClientId = "kit";

  private static String serverApiOAuth2ClientSecret = "kit";

  public String runJob(String token, String servreUrl, String planId) {
    String url = String.format(servreUrl + EXECUTE_JOB, planId);

    try {
      URIBuilder uriBuilder = new URIBuilder(url);

      HttpPut httpPut = new HttpPut(uriBuilder.build());
      httpPut.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      HttpResponse httpResponse = HttpHelper.sendRequest(
          httpPut,
          token,
          null,
          null,
          null,
          null,
          null);
      InputStream responseContent = httpResponse.getEntity().getContent();
      return responseContent.toString();
    } catch (Exception e) {
      return "Resource not found " + e.getMessage();
    }
  }

  public String requestToken(String serverUrl, String apiKey) throws Exception {
    String url = serverUrl + TOKEN_URI;
    URIBuilder uriBuilder = new URIBuilder(url);

    List<NameValuePair> pairs = Arrays.asList(
        new BasicNameValuePair("username", ""),
        new BasicNameValuePair("password", apiKey),
        new BasicNameValuePair("grant_type", serverApiOAuth2GrantType)
    );

    HttpPost httpPost = new HttpPost(uriBuilder.build());

    String clientCredentials = serverApiOAuth2ClientId + ":" + serverApiOAuth2ClientSecret;
    httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " +
        Base64.getEncoder().encodeToString(clientCredentials.getBytes()));

    HttpResponse httpResponse = HttpHelper.sendRequest(
        httpPost,
        null,
        serverApiOAuth2ClientId,
        serverApiOAuth2ClientSecret,
        null,
        null,
        pairs);

    InputStream content = httpResponse.getEntity().getContent();
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> map = objectMapper.readValue(content, Map.class);
    int statusCode = httpResponse.getStatusLine().getStatusCode();
    if (statusCode >= 300) {
      throw new HttpResponseException(statusCode, (String) map.get("error_description"));
    }
    return (String) map.get("access_token");
  }
}
