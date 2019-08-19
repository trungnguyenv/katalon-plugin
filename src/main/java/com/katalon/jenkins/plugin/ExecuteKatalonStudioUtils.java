package com.katalon.jenkins.plugin;

import com.google.common.base.Throwables;
import com.katalon.utils.KatalonUtils;
import com.katalon.utils.Logger;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExecuteKatalonStudioUtils {

    public static boolean executeKatalon(
            FilePath workspace,
            EnvVars buildEnvironment,
            Launcher launcher,
            BuildListener buildListener,
            String version,
            String location,
            String executeArgs,
            String x11Display,
            String xvfbConfiguration,
            String plainId,
            String apiKey,
            String serverUrl) {
        Logger logger = new JenkinsLogger(buildListener);
        try {
            return launcher.getChannel().call(new Callable<Boolean, Exception>() {
                @Override
                public Boolean call() throws Exception {

                    Logger logger = new JenkinsLogger(buildListener);

                    logger.info("plainId " + plainId);
                    logger.info("apiKey " + apiKey);
                    logger.info("server " + serverUrl);

                    if (workspace != null) {
                        String workspaceLocation = workspace.getRemote();

                        if (workspaceLocation != null) {
                            Map<String, String> environmentVariables = new HashMap<>();
                            environmentVariables.putAll(System.getenv());
                            buildEnvironment.entrySet()
                                    .forEach(entry -> environmentVariables.put(entry.getKey(), entry.getValue()));

                            if (plainId != null) {
                                AnalyticsAuthorizationHandler analyticsAuthorizationHandler = new AnalyticsAuthorizationHandler();
                                String token = analyticsAuthorizationHandler.requestToken(serverUrl, apiKey);

                                if (token != null) {
                                    String result = analyticsAuthorizationHandler.runJob(token, serverUrl, plainId);
                                    logger.info("Create job success! Job id is " + result);
                                }
                            } else {
                                return KatalonUtils.executeKatalon(
                                    logger,
                                    version,
                                    location,
                                    workspaceLocation,
                                    executeArgs,
                                    x11Display,
                                    xvfbConfiguration,
                                    environmentVariables);
                            }
                        }
                    }
                    return true;
                }
                @Override
                public void checkRoles(RoleChecker roleChecker) throws SecurityException {
                }
            });
        } catch (Exception e) {
            String stackTrace = Throwables.getStackTraceAsString(e);
            logger.info(stackTrace);
            return false;
        }
    }
}
