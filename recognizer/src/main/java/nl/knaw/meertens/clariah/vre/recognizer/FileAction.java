package nl.knaw.meertens.clariah.vre.recognizer;

public enum FileAction {

  CREATE("create"),
  UPDATE("update"),
  RENAME("rename"),
  DELETE("delete");

  private final String msgValue;

  FileAction(String msgValue) {
    this.msgValue = msgValue;
  }

  public static FileAction from(String msgValue) {
    for (FileAction fileAction : FileAction.values()) {
      if (fileAction.msgValue.equals(msgValue)) {
        return fileAction;
      }
    }
    throw new IllegalArgumentException(String.format("FileAction with msgValue [%s] does not exist", msgValue));
  }

  public String msgValue() {
    return msgValue;
  }
}
