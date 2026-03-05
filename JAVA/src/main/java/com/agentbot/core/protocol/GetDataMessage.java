package com.agentbot.core.protocol;

import java.util.List;

public class GetDataMessage {
  private List<String> dataIds;

  public List<String> getDataIds() {
    return dataIds;
  }

  public void setDataIds(List<String> dataIds) {
    this.dataIds = dataIds;
  }
}
