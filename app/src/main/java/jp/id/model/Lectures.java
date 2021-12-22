package jp.id.model;

import java.util.ArrayList;
import java.util.List;

public class Lectures {
    private static final List<LectureItem> lectureItems = new ArrayList<>();

    private Lectures() {}

    public static List<LectureItem> getLectures() {
        return lectureItems;
    }

    public static void setLectures(List<LectureItem> items) {
        lectureItems.addAll(items);
    }

}
