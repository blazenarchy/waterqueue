package dev.wnuke.waterqueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Global {
    public static Gson gson = new GsonBuilder().create();
    public static final String INFO_CHANNEL = "QueueInfoChannel";
}
