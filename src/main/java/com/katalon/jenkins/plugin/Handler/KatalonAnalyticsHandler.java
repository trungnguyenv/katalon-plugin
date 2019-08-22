package com.katalon.jenkins.plugin.Handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.jenkins.plugin.Entity.BuildInfo;
import com.katalon.jenkins.plugin.Entity.Job;
import com.katalon.jenkins.plugin.Entity.JobStatus;
import com.katalon.jenkins.plugin.Entity.TestProject;
import com.katalon.jenkins.plugin.Helper.HttpHelper;
import com.katalon.utils.Logger;
import hidden.jth.org.apache.http.HttpHeaders;
import hidden.jth.org.apache.http.HttpResponse;
import hidden.jth.org.apache.http.NameValuePair;
import hidden.jth.org.apache.http.client.HttpResponseException;
import hidden.jth.org.apache.http.client.methods.HttpGet;
import hidden.jth.org.apache.http.client.methods.HttpPost;
import hidden.jth.org.apache.http.client.methods.HttpPut;
import hidden.jth.org.apache.http.client.utils.URIBuilder;
import hidden.jth.org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KatalonAnalyticsHandler {

  private static String TOKEN_URI = "/oauth/token";

  private static  String EXECUTE_JOB = "/api/v1/run-configurations/%s/execute";

  private static String GET_LOG = "/api/v1/jobs/%s";

  private static String LOG_INFOR = "/team/%s/project/%s/grid/plan/%s/job/%s";

  private static String serverApiOAuth2GrantType = "password";

  private static String serverApiOAuth2ClientId = "kit";

  private static String serverApiOAuth2ClientSecret = "kit";

  private ObjectMapper objectMapper;

  private Logger logger;

  public KatalonAnalyticsHandler() {
    init();
  }

  public KatalonAnalyticsHandler(Logger logger) {
    this.logger = logger;
    init();
  }

  private void init() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public boolean run(String serverUrl, String apiKey, String planId) {
    logger.info("apiKey: " + apiKey);
    logger.info("serverUrl: " + serverUrl);
    logger.info("planId: " + planId);

    try {
      String token = requestToken(serverUrl, apiKey);

      if (token != null) {
        BuildInfo buildInfo = runJob(token, serverUrl, planId);

        if (buildInfo == null) {
          logger.info("Cannot create job");
          return false;
        }

        long jobId = buildInfo.getJob_id();
        logger.info("Job ID: " + buildInfo.getBuild_num());
        TimeUnit.SECONDS.sleep(1);

        //Wait for execute job is done.
        JobStatus jobStatus = null;
        while (true) {
          Job job = getJob(token, serverUrl, jobId);
          if (job == null) {
            logger.info("Cannot get job from KA");
            break;
          }
          if (jobStatus == null) {
            logger.info("Job Detail: " + getJobUrl(serverUrl, job, planId));
          }

          JobStatus jobStatusCurrent = job.getStatus();
          if (jobStatusCurrent == jobStatus) {
            jobStatus = jobStatusCurrent;
            continue;
          }
          jobStatus = jobStatusCurrent;
          if (JobStatus.getRunningStatuses().contains(jobStatus)) {
            logger.info("Job Status: " + jobStatus);
          } else {
            logger.info("Job Status: " + jobStatus);
            logger.info("Execute done");
            if (jobStatus == JobStatus.SUCCESS) {
              return true;
            }
            return false;
          }
          TimeUnit.SECONDS.sleep(10);
        }
      }
    } catch (Exception e) {
      logger.info(e.getMessage());
      return false;
    }
    return false;
  }

  private String getJobUrl(String serverUrl, Job job, String plainId) {
    TestProject testProject = job.getTestProject();
    return String.format(serverUrl + LOG_INFOR, testProject.getId(), testProject.getProjectId(), plainId ,job.getId());
  }

  private Job getJob(String token, String serverUrl, long jobId) {
     String url = String.format(serverUrl + GET_LOG, jobId);
    try {
      URIBuilder uriBuilder = new URIBuilder(url);
      HttpGet httpGet = new HttpGet(uriBuilder.build());
      httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      HttpResponse httpResponse = HttpHelper.sendRequest(
          httpGet,
          token,
          null,
          null,
          null,
          null,
          null);
      InputStream responseContent = httpResponse.getEntity().getContent();

      return objectMapper.readValue(responseContent, Job.class);
    } catch (Exception e) {
      logger.info(e.getMessage());
      return null;
    }
  }

  private BuildInfo runJob(String token, String serverUrl, String planId) {
    String url = String.format(serverUrl + EXECUTE_JOB, planId);
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
      BuildInfo[] buildInfos = objectMapper.readValue(responseContent, BuildInfo[].class);
      return buildInfos[0];
    } catch (Exception e) {
      logger.info(e.getMessage());
      return null;
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
    Map map = objectMapper.readValue(content, Map.class);
    int statusCode = httpResponse.getStatusLine().getStatusCode();
    if (statusCode >= 300) {
      throw new HttpResponseException(statusCode, (String) map.get("error_description"));
    }
    return (String) map.get("access_token");
  }
}
