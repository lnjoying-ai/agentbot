package com.agentbot.core.protocol;

import java.util.List;

public class InvMessage {
  private java.util.List<InvItem> items;

  public List<InvItem> getItems() {
    return items;
  }

  public void setItems(List<InvItem> items) {
    this.items = items;
  }
}
