package com.katalon.jenkins.plugin;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.*;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class ExecuteKatalonStudioTask extends Builder {

    private String version;

    private String location;

    private String executeArgs;

    private String x11Display;

    private String xvfbConfiguration;

    private String planId;

    private String apiKey;

    private String serverUrl;

    @DataBoundConstructor
    public ExecuteKatalonStudioTask(
            String version,
            String location,
            String executeArgs,
            String x11Display,
            String xvfbConfiguration,
            String planId,
            String apiKey,
            String serverUrl) {
        this.version = version;
        this.location = location;
        this.executeArgs = executeArgs;
        this.x11Display = x11Display;
        this.xvfbConfiguration = xvfbConfiguration;
        this.planId = planId;
        this.apiKey = apiKey;
        this.serverUrl = serverUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExecuteArgs() {
        return executeArgs;
    }

    public void setExecuteArgs(String executeArgs) {
        this.executeArgs = executeArgs;
    }

    public String getX11Display() {
        return x11Display;
    }

    public void setX11Display(String x11Display) {
        this.x11Display = x11Display;
    }

    public String getXvfbConfiguration() {
        return xvfbConfiguration;
    }

    public void setXvfbConfiguration(String xvfbConfiguration) {
        this.xvfbConfiguration = xvfbConfiguration;
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

        FilePath workspace = abstractBuild.getWorkspace();
        EnvVars buildEnvironment = abstractBuild.getEnvironment(buildListener);
        return ExecuteKatalonStudioUtils.executeKatalon(
                workspace,
                buildEnvironment,
                launcher,
                buildListener,
                version,
                location,
                executeArgs,
                x11Display,
                xvfbConfiguration,
                planId,
                apiKey,
                serverUrl);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher

        @Override
        public String getDisplayName() {
            return "Execute Katalon Studio Tests";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // We are always OK with someone adding this  as a build step for their job
        }

        private String apiKey;

        private String serverUrl;

        public DescriptorImpl() {
            super(ExecuteKatalonStudioTask.class);
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
            AnalyticsAuthorizationHandler analyticsAuthorizationHandler = new AnalyticsAuthorizationHandler();

            try {
                String token = analyticsAuthorizationHandler.requestToken(url, apiKey);

                if (token != null) {
                    return FormValidation.ok("Success!");
                } else {
                    return FormValidation.error("Cannot connect to KA");
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
            return new ExecuteKatalonStudioTask(
                formData.getString("version"),
                formData.getString("location"),
                formData.getString("executeArgs"),
                formData.getString("x11Display"),
                formData.getString("xvfbConfiguration"),
                formData.getString("planId"),
                apiKey,
                serverUrl);
        }
    }
}
