package jp.id.model;

public class SubjectItem {
    private Long id;
    private String name;
    private Long sessionId;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public SubjectItem(Long id, String name, Long sessionId) {
        this.id = id;
        this.name = name;
        this.sessionId = sessionId;
    }
}
