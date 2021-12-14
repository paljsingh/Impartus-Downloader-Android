package jp.id.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


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
    private boolean selected;
    private int downloadPercent;
    private File offlinePath;
    private boolean downloading;
    private final int viewPosition;
    private List<String> logs;

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public int getViewPosition() {
        return viewPosition;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void appendLog(final String log) {
        this.logs.add(log);
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

        this.selected = false;
        this.downloadPercent = 0;

        this.offlinePath = null;
        this.downloading = false;
        this.viewPosition = viewPosition;

        this.logs = new ArrayList<>();
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

    public void setSelected(final boolean value) {
        this.selected = value;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public boolean getSelected() {
        return this.selected;
    }

    public int getDownloadPercent() { return this.downloadPercent; }

    public void setDownloadPercent(int value) {
        if (value < 0) {
            this.downloadPercent = 0;
        } else {
            this.downloadPercent = Math.min(value, 100);
        }
    }

    public File getOfflinePath() {
        return offlinePath;
    }

    public void setOfflinePath(File offlinePath) {
        this.offlinePath = offlinePath;
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
        dest.writeString(Boolean.toString(selected));
        dest.writeInt(downloadPercent);
        dest.writeString(Boolean.toString(downloading));
        dest.writeInt(viewPosition);

        if (offlinePath != null) {
            dest.writeString(offlinePath.getAbsolutePath());
        } else {
            dest.writeString("null");
        }

        dest.writeStringList(logs);

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
        selected = Boolean.parseBoolean(in.readString());
        downloadPercent = in.readInt();
        downloading = Boolean.getBoolean(in.readString());
        viewPosition = in.readInt();

        String offlinePathStr = in.readString();
        if (offlinePathStr.equals("null")) {
            offlinePath = null;
        } else {
            offlinePath = new File(offlinePathStr);
        }

        logs = new ArrayList<>();
        in.readStringList(logs);
    }
}
