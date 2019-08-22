package com.katalon.jenkins.plugin.Entity;

public class TestProject {

  //teamId
  private long id;

  //projectId
  private long projectId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getProjectId() {
    return projectId;
  }

  public void setProjectId(long projectId) {
    this.projectId = projectId;
  }
}
