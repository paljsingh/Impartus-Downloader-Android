package jp.id.core;

import java.util.ArrayList;
import java.util.List;

public class Track {
    final List<VideoStream> streams;
    final int number;

    public int getNumber() {
        return number;
    }

    public Track(int number) {
        streams = new ArrayList<>();
        this.number = number;
    }

    public void addStream(VideoStream stream) {
        streams.add(stream);
    }

    public List<VideoStream> getStreams() {
        return streams;
    }
}
