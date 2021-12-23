package jp.id.model;

import android.os.Parcel;
import android.os.Parcelable;


public class LectureItem implements Parcelable {
    private final int id;
    private final int seqNo;
    private final String topic;
    private final String professorName;
    private final String date;
    private final int numTracks;
    private final int duration;
    private final String subjectName;
    private final boolean flipped;
    private int downloadPercent;
    private boolean downloading;
    private final int viewPosition;

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public int getViewPosition() {
        return viewPosition;
    }

    public LectureItem(int id, int seqNo, String topic, String professorName, String date, int numTracks, int duration, String subjectName, boolean flipped, int viewPosition) {
        this.id = id;
        this.seqNo = seqNo;
        this.topic = topic;
        this.professorName = professorName;
        this.date = date;
        this.numTracks = numTracks;
        this.duration = duration;
        this.subjectName = subjectName;
        this.flipped = flipped;

        this.downloadPercent = 0;

        this.downloading = false;
        this.viewPosition = viewPosition;
    }

    public int getId() {
        return id;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public String getTopic() {
        return topic;
    }

    public String getProfessorName() {
        return professorName;
    }

    public String getDate() {
        return date;
    }

    public int getNumTracks() {
        return numTracks;
    }

    public int getDuration() {
        return duration;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public int getDownloadPercent() { return this.downloadPercent; }

    public void setDownloadPercent(int value) {
        if (value < 0) {
            this.downloadPercent = 0;
        } else {
            this.downloadPercent = Math.min(value, 100);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(seqNo);
        dest.writeString(topic);
        dest.writeString(professorName);
        dest.writeString(date);
        dest.writeInt(numTracks);
        dest.writeInt(duration);
        dest.writeString(subjectName);
        dest.writeString(Boolean.toString(flipped));
        dest.writeInt(downloadPercent);
        dest.writeString(Boolean.toString(downloading));
        dest.writeInt(viewPosition);
    }

    public static final Parcelable.Creator<LectureItem> CREATOR = new Parcelable.Creator<LectureItem>() {
        public LectureItem createFromParcel(Parcel in) {
            return new LectureItem(in);
        }

        public LectureItem[] newArray(int size) {
            return new LectureItem[size];
        }
    };

    private LectureItem(Parcel in) {
        id = in.readInt();
        seqNo = in.readInt();
        topic = in.readString();
        professorName = in.readString();
        date = in.readString();
        numTracks = in.readInt();
        duration = in.readInt();
        subjectName = in.readString();
        flipped = Boolean.parseBoolean(in.readString());
        downloadPercent = in.readInt();
        downloading = Boolean.getBoolean(in.readString());
        viewPosition = in.readInt();
    }
}
