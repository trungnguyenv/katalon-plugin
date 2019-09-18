package com.katalon.jenkins.plugin.helper;

import com.katalon.utils.Logger;
import hudson.model.BuildListener;

import java.time.LocalDateTime;

public class JenkinsLogger implements Logger {

    private BuildListener buildListener;

    public JenkinsLogger(BuildListener buildListener) {
        this.buildListener = buildListener;
    }

    @Override
    public void info(String message) {
        String timeNow = LocalDateTime.now().toString();
        buildListener.getLogger().println('[' + timeNow + "] " + message);
    }
}
