package com.katalon.jenkins.plugin;

import com.katalon.jenkins.plugin.Handler.KatalonAnalyticsHandler;
import com.katalon.jenkins.plugin.Utils.JenkinsLogger;
import com.katalon.utils.Logger;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class ExecuteKatalonAnalyticsPlan extends Builder {

  private String planId;

  private String apiKey;

  private String serverUrl;

  @DataBoundConstructor
  public ExecuteKatalonAnalyticsPlan(
      String planId,
      String apiKey,
      String serverUrl) {
    this.planId = planId;
    this.apiKey = apiKey;
    this.serverUrl = serverUrl;
  }

  public String getPlanId() {
    return planId;
  }

  public void setPlanId(String planId) {
    this.planId = planId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener)
      throws InterruptedException, IOException {
    Logger logger = new JenkinsLogger(buildListener);
    KatalonAnalyticsHandler katalonAnalyticsHandler = new KatalonAnalyticsHandler(logger);

    return katalonAnalyticsHandler.run(serverUrl, apiKey, planId);
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher

    @Override
    public String getDisplayName() {
      return "Execute Katalon Analytics Plan";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true; // We are always OK with someone adding this  as a build step for their job
    }

    private String apiKey;

    private String serverUrl;

    public DescriptorImpl() {
      super(ExecuteKatalonAnalyticsPlan.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindParameters(this);
      this.apiKey = formData.getString("apiKey");
      this.serverUrl = formData.getString("serverUrl");
      save();
      return super.configure(req, formData);
    }

    public FormValidation doTestConnection(@QueryParameter("serverUrl") final String url,
                                           @QueryParameter("apiKey") final String apiKey) {
      KatalonAnalyticsHandler katalonAnalyticsHandler = new KatalonAnalyticsHandler();

      try {
        String token = katalonAnalyticsHandler.requestToken(url, apiKey);

        if (token != null) {
          return FormValidation.ok("Success!");
        } else {
          return FormValidation.error("Cannot connect to Katalon Analytics");
        }
      } catch (Exception e) {
        return FormValidation.error("Error " + e.getMessage());
      }
    }

    public void setServerUrl(String serverUrl) {
      this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
      return serverUrl;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getApiKey() {
      return apiKey;
    }

    @Override
    public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
      return new ExecuteKatalonAnalyticsPlan(
          formData.getString("planId"),
          apiKey,
          serverUrl);
    }
  }
}
