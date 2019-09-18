package com.katalon.jenkins.plugin;

import com.katalon.jenkins.plugin.Entity.Plan;
import com.katalon.jenkins.plugin.Entity.Project;
import com.katalon.jenkins.plugin.helper.KatalonAnalyticsHelper;
import com.katalon.jenkins.plugin.helper.KatalonAnalyticsSearchHelper;
import com.katalon.jenkins.plugin.helper.JenkinsLogger;
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
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class ExecuteKatalonAnalyticsPlan extends Builder {

  private String plan;

  private String apiKey;

  private String serverUrl;

  private String projectId;

  @DataBoundConstructor
  public ExecuteKatalonAnalyticsPlan(
      String apiKey,
      String serverUrl,
      String projectId,
      String plan) {
    this.plan = plan;
    this.apiKey = apiKey;
    this.serverUrl = serverUrl;
    this.projectId = projectId;
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

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener)
      throws InterruptedException, IOException {
    Logger logger = new JenkinsLogger(buildListener);
    KatalonAnalyticsHelper katalonAnalyticsHandler = new KatalonAnalyticsHelper(logger);
    return katalonAnalyticsHandler.run(serverUrl, apiKey, plan, projectId);
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

    private String projectId;

    private String plan;

    public DescriptorImpl() {
      super(ExecuteKatalonAnalyticsPlan.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindParameters(this);
      this.projectId = formData.getString("projectId");
      this.apiKey = formData.getString("apiKey");
      this.serverUrl = formData.getString("serverUrl");
      this.plan = formData.getString("planKA");
      save();
      return super.configure(req, formData);
    }

    public FormValidation doTestConnection(@QueryParameter("serverUrl") final String url,
                                           @QueryParameter("apiKey") final String apiKey) {
      KatalonAnalyticsHelper katalonAnalyticsHandler = new KatalonAnalyticsHelper();
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

    public ListBoxModel doFillProjectIdItems(@QueryParameter("serverUrl") final String url,
                                             @QueryParameter("apiKey") final String apiKey) {
      ListBoxModel options = new ListBoxModel();
      KatalonAnalyticsHelper katalonAnalyticsHandler = new KatalonAnalyticsHelper();
      try {
        String token = katalonAnalyticsHandler.requestToken(url, apiKey);
        if (token != null) {
          KatalonAnalyticsSearchHelper katalonAnalyticsSearchHandler = new KatalonAnalyticsSearchHelper();
          Project[] projects = katalonAnalyticsSearchHandler.getProjects(token, url);
          for (Project project : projects) {
            options.add(project.getName(), String.valueOf(project.getId()));
          }
        }
      } catch (Exception e) {

      }
      return options;
    }

    public ListBoxModel doFillPlanKAItems(@QueryParameter("serverUrl") final String url,
                                          @QueryParameter("apiKey") final String apiKey,
                                          @QueryParameter("projectId") final String projectId) {
      ListBoxModel options = new ListBoxModel();
      KatalonAnalyticsHelper katalonAnalyticsHandler = new KatalonAnalyticsHelper();
      try {
        String token = katalonAnalyticsHandler.requestToken(url, apiKey);
        if (token != null) {
          KatalonAnalyticsSearchHelper katalonAnalyticsSearchHandler = new KatalonAnalyticsSearchHelper();
          Plan[] plans = katalonAnalyticsSearchHandler.getPlan(token, url, projectId);
          for (Plan plan : plans) {
            options.add(plan.getName(), String.valueOf(plan.getId()));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return options;
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

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    public String getProjectId() {
      return projectId;
    }

    public void setPlan(String plan) {
      this.plan = plan;
    }

    public String getPlan() {
      return plan;
    }

    @Override
    public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
      return new ExecuteKatalonAnalyticsPlan(
          formData.getString("apiKey"),
          formData.getString("serverUrl"),
          formData.getString("projectId"),
          formData.getString("planKA"));
    }
  }
}
