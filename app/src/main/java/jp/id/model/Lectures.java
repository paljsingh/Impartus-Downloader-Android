package jp.id.model;

import java.util.ArrayList;
import java.util.List;

public class Lectures {
    private static final List<LectureItem> lectureItems = new ArrayList<>();
    private static int numDownloadsInProgress = 0;
    private static int lastPosition = 0;

    public static int getLastPosition() {
        return lastPosition;
    }

    public static void setLastPosition(int lastPosition) {
        Lectures.lastPosition = lastPosition;
    }

    private Lectures() {}

    public static List<LectureItem> getLectures() {
        return lectureItems;
    }

    public static void setLectures(List<LectureItem> items) {
        lectureItems.clear();
        lectureItems.addAll(items);
    }

    public static void incrementDownloads() {
        numDownloadsInProgress += 1;
    }

    public static void decrementDownloads() {
        numDownloadsInProgress -= 1;
        numDownloadsInProgress = Math.max(0, numDownloadsInProgress);
    }

    public static boolean isDownloadInProgress() {
        return numDownloadsInProgress > 0;
    }
}
