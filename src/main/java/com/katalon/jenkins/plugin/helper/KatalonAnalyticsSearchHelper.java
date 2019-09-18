package com.katalon.jenkins.plugin.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.jenkins.plugin.Entity.Plan;
import com.katalon.jenkins.plugin.Entity.Project;
import com.katalon.jenkins.plugin.helper.HttpHelper;
import com.katalon.jenkins.plugin.search.SearchCondition;
import com.katalon.jenkins.plugin.search.SearchPagination;
import com.katalon.jenkins.plugin.search.SearchParameter;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KatalonAnalyticsSearchHelper {

  private static final String SEARCH_URL = "/api/v1/search";

  private ObjectMapper objectMapper;

  public KatalonAnalyticsSearchHelper() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private List<Object> search(String token, String serverUrl, SearchParameter searchParameter) {
    String url = serverUrl + SEARCH_URL;
    try {
      URIBuilder uriBuilder = new URIBuilder(url);

      HttpPost httpPost = new HttpPost(uriBuilder.build());
      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      String requestContent = objectMapper.writeValueAsString(searchParameter);
      HttpResponse httpResponse = HttpHelper.sendRequest(
          httpPost,
          token,
          null,
          null,
          IOUtils.toInputStream(requestContent),
          null,
          null);

      InputStream responseContent = httpResponse.getEntity().getContent();
      Map map = objectMapper.readValue(responseContent, Map.class);
      List<Object> content = (List) map.get("content");
      return content;
    } catch (Exception e) {
//      log.error("Resources not found", e);
      return null;
    }
  }

  public Project[] getProjects(String token, String serverUrl) {
    SearchParameter searchParameter = new SearchParameter();
    searchParameter.setType("Project");
    searchParameter.setConditions(Collections.emptyList());
    SearchPagination pagination = new SearchPagination(0L, 30L, null);
    searchParameter.setPagination(pagination);

    List<Object> content = search(token, serverUrl, searchParameter);
    return objectMapper.convertValue(content, Project[].class);
  }

  public Plan[] getPlan(String token, String serverUrl, String projectId) {
    SearchParameter searchParameter = new SearchParameter();
    searchParameter.setType("RunConfiguration");
    searchParameter.setConditions(Arrays.asList(
        new SearchCondition("Project.id", "=", projectId)
    ));
    SearchPagination pagination = new SearchPagination(0L, 30L, null);
    searchParameter.setPagination(pagination);

    List<Object> content = search(token, serverUrl, searchParameter);
    return objectMapper.convertValue(content, Plan[].class);
  }
}
