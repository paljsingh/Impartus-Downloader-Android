package jp.id.core;

public class VideoStream {
    private final int fileNumber;
    private final double duration;
    private final String encryptionKeyUrl;
    private final int keyId;
    private final String encryptionMethod;
    private final String url;

    public VideoStream(int fileNumber, double duration, String encryptionKeyUrl, int keyId, String encryptionMethod, final String url) {
        this.fileNumber = fileNumber;
        this.duration = duration;
        this.encryptionKeyUrl = encryptionKeyUrl;
        this.keyId = keyId;
        this.encryptionMethod = encryptionMethod;
        this.url = url;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public double getDuration() {
        return duration;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public String getEncryptionMethod() {
        return encryptionMethod;
    }

    public int getKeyId() {
        return keyId;
    }

    public String getUrl() {
        return url;
    }

}
