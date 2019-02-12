package com.katalon.jenkins.plugin;

import com.katalon.utils.Logger;
import hudson.model.BuildListener;

class JenkinsLogger implements Logger {

    private BuildListener buildListener;

    JenkinsLogger(BuildListener buildListener) {
        this.buildListener = buildListener;
    }

    @Override
    public void info(String message) {
        buildListener.getLogger().println(message);
    }
}
