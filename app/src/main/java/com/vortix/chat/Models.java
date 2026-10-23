package com.vortix.chat;

public class Models {
    public static class User {
        public String id, username, display, bio, avatar;
        public int status, accent, banner, xp;
        public boolean bot;
        public long created;
    }
    public static class Server { public String id, name, owner, invite; public int color; }
    public static class Channel { public String id, serverId, name, topic; public int kind; }
    public static class Message {
        public String id, channelId, author, content, replyTo, attach;
        public long ts;
        public boolean edited, pinned;
    }
    public static class Dm { public String id; public User other; public Message last; public int unread; }
}
