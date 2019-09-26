package com.katalon.jenkins.plugin;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.katalon.jenkins.plugin.entity.Plan;
import com.katalon.jenkins.plugin.entity.Project;
import com.katalon.jenkins.plugin.helper.KatalonTestOpsHelper;
import com.katalon.jenkins.plugin.helper.KatalonTestOpsSearchHelper;
import com.katalon.jenkins.plugin.helper.JenkinsLogger;
import com.katalon.utils.Logger;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

public class ExecuteKatalonTestOpsPlan extends Builder {

  private String apiKey;

  private String plan;

  private String serverUrl;

  private String projectId;

  @DataBoundConstructor
  public ExecuteKatalonTestOpsPlan(
      String apiKey,
      String serverUrl,
      String projectId,
      String plan) {
    this.apiKey = apiKey;
    this.plan = plan;
    this.serverUrl = serverUrl;
    this.projectId = projectId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String credentialsId) {
    this.apiKey = credentialsId;
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
    KatalonTestOpsHelper katalonAnalyticsHandler = new KatalonTestOpsHelper(logger);
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
      return "Execute Katalon TestOps Plan";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true; // We are always OK with someone adding this  as a build step for their job
    }

    private String credentialsId;

    private String apiKey;

    private String serverUrl;

    private String projectId;

    private String plan;

    public DescriptorImpl() {
      super(ExecuteKatalonTestOpsPlan.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindParameters(this);
      this.credentialsId = formData.getString("credentialsId");
      this.apiKey = getApiKey(this.credentialsId);
      this.projectId = formData.getString("projectId");
      this.serverUrl = formData.getString("serverUrl");
      this.plan = formData.getString("planId");
      save();
      return super.configure(req, formData);
    }

    private String getApiKey(String credentialsId) {
      if (credentialsId == null) {
        return null;
      }
      List<StringCredentials> creds = CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM);
      StringCredentials credentials = null;
      for (StringCredentials c : creds) {
        if (credentialsId.matches(c.getId())) {
          credentials = c;
        }
      }
      return credentials == null ? null : credentials.getSecret().getPlainText();
    }

    public FormValidation doTestConnection(@QueryParameter("serverUrl") final String url,
                                           @QueryParameter("credentialsId") final String credentialsId) {

      if (url.isEmpty()) {
        return FormValidation.error("Please input server url.\n Example: https://analytics.katalon.com");
      }

      if (credentialsId.isEmpty()) {
        return FormValidation.error("Please select credentials.");
      }

      String apiKey = getApiKey(credentialsId);
      if (apiKey == null) {
        return FormValidation.error("Cannot get API key.");
      }

      try {
        KatalonTestOpsHelper katalonTestOpsHelper = new KatalonTestOpsHelper();
        String token = katalonTestOpsHelper.requestToken(url, apiKey);
        if (token != null) {
          return FormValidation.ok("Success!");
        } else {
          return FormValidation.error("Cannot connect to Katalon TestOps.");
        }
      } catch (Exception e) {
        return FormValidation.error("Error " + e.getMessage() + "\nCause: " + e.getCause());
      }
    }

    public ListBoxModel doFillProjectIdItems(@QueryParameter("serverUrl") final String url,
                                             @QueryParameter("credentialsId") final String credentialsId) {
      ListBoxModel options = new ListBoxModel();

      if (url.isEmpty()) {
        return options;
      }

      if (credentialsId.isEmpty()) {
        return options;
      }

      String apiKey = getApiKey(credentialsId);
      if (apiKey != null) {
        KatalonTestOpsHelper katalonTestOpsHelper = new KatalonTestOpsHelper();
        try {
          String token = katalonTestOpsHelper.requestToken(url, apiKey);
          if (token != null) {
            KatalonTestOpsSearchHelper katalonTestOpsSearchHelper = new KatalonTestOpsSearchHelper();
            Project[] projects = katalonTestOpsSearchHelper.getProjects(token, url);
            for (Project project : projects) {
              options.add(project.getName(), String.valueOf(project.getId()));
            }
          }
        } catch (Exception e) {
          //Do nothing here
        }
      }
      return options;
    }

    public ListBoxModel doFillPlanIdItems(@QueryParameter("serverUrl") final String url,
                                          @QueryParameter("credentialsId") final String credentialsId,
                                          @QueryParameter("projectId") final String projectId) {
      ListBoxModel options = new ListBoxModel();

      if (url.isEmpty()) {
        return options;
      }

      if (credentialsId.isEmpty()) {
        return options;
      }

      if (projectId.isEmpty()) {
        return options;
      }

      String apiKey = getApiKey(credentialsId);
      if (apiKey == null) {
        return options;
      }
      KatalonTestOpsHelper katalonTestOpsHelper = new KatalonTestOpsHelper();
      try {
        String token = katalonTestOpsHelper.requestToken(url, apiKey);
        if (token != null) {
          KatalonTestOpsSearchHelper katalonTestOpsSearchHelper = new KatalonTestOpsSearchHelper();
          Plan[] plans = katalonTestOpsSearchHelper.getPlan(token, url, projectId);
          for (Plan plan : plans) {
            options.add(plan.getName(), String.valueOf(plan.getId()));
          }
        }
      } catch (Exception e) {
        //Do nothing here
      }
      return options;
    }

    public ListBoxModel doFillCredentialsIdItems(
        @AncestorInPath Item item,
        @QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();
      if (item == null) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(credentialsId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(credentialsId);
        }
      }
      return result.includeEmptyValue()
          .includeMatchingAs(
              ACL.SYSTEM,
              Jenkins.getInstance(),
              StringCredentials.class,
              URIRequirementBuilder.fromUri("").build(),
              CredentialsMatchers.always()
          )
          .includeCurrentValue(credentialsId);
    }

    public void setCredentialsId(String credentialsId) {
      this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
      return credentialsId;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setServerUrl(String serverUrl) {
      this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
      return serverUrl;
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
      String credentialsId = formData.getString("credentialsId");
      String apiKey = getApiKey(credentialsId);
      return new ExecuteKatalonTestOpsPlan(
          apiKey,
          formData.getString("serverUrl"),
          formData.getString("projectId"),
          formData.getString("planId"));
    }
  }
}
