package com.vortix.chat;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Single data layer for the whole app (SQLite on-device).
 * Every screen talks ONLY to this class, so a Firestore version can replace the
 * internals later without touching any UI code. See docs/CHATGPT_FIRESTORE_PROMPT.md.
 */
public class Repo extends SQLiteOpenHelper {
    public static Repo I;
    public static String openChannel = "";

    public interface Listener { void onChange(String key); }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> ls = new CopyOnWriteArrayList<>();
    private final Random rnd = new Random();
    private final SharedPreferences sp;

    public static void init(Context c) { I = new Repo(c.getApplicationContext()); }

    private Repo(Context c) {
        super(c, "vortix.db", null, 1);
        sp = c.getSharedPreferences("vortix_session", 0);
        getWritableDatabase();
    }

    // ---------------------------------------------------------------- listeners
    public void addListener(Listener l) { if (!ls.contains(l)) ls.add(l); }
    public void removeListener(Listener l) { ls.remove(l); }
    private void notifyKey(final String key) {
        main.post(() -> { for (Listener l : ls) l.onChange(key); });
    }

    // ---------------------------------------------------------------- schema + seed
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id TEXT PRIMARY KEY, username TEXT UNIQUE, display TEXT, hash TEXT, salt TEXT, bio TEXT, status INTEGER, avatar TEXT, accent INTEGER, banner INTEGER, xp INTEGER, bot INTEGER, created INTEGER)");
        db.execSQL("CREATE TABLE servers(id TEXT PRIMARY KEY, name TEXT, owner TEXT, invite TEXT UNIQUE, color INTEGER, created INTEGER)");
        db.execSQL("CREATE TABLE members(server_id TEXT, user_id TEXT, role TEXT, PRIMARY KEY(server_id, user_id))");
        db.execSQL("CREATE TABLE channels(id TEXT PRIMARY KEY, server_id TEXT, name TEXT, topic TEXT, kind INTEGER, pos INTEGER)");
        db.execSQL("CREATE TABLE messages(id TEXT PRIMARY KEY, channel_id TEXT, author TEXT, content TEXT, reply_to TEXT, attach TEXT, ts INTEGER, edited INTEGER, pinned INTEGER)");
        db.execSQL("CREATE INDEX idx_msg ON messages(channel_id, ts)");
        db.execSQL("CREATE TABLE reactions(message_id TEXT, user_id TEXT, kind INTEGER, PRIMARY KEY(message_id, user_id, kind))");
        db.execSQL("CREATE TABLE reads(channel_id TEXT, user_id TEXT, ts INTEGER, PRIMARY KEY(channel_id, user_id))");
        db.execSQL("CREATE TABLE dms(id TEXT PRIMARY KEY, a TEXT, b TEXT)");
        long now = System.currentTimeMillis();
        seedUser(db, "u_vortix", "vortix", "Vortix", "Official Vortix bot. Try /help", 0, 0xFF7C4DFF, now);
        seedUser(db, "u_nova", "nova", "Nova", "Demo contact. Replies automatically.", 0, 0xFF2AABEE, now);
        seedUser(db, "u_pixel", "pixel", "Pixel", "Demo contact. Replies automatically.", 1, 0xFFEB459E, now);
        ContentValues s = new ContentValues();
        s.put("id", "s_hq"); s.put("name", "Vortix HQ"); s.put("owner", "u_vortix");
        s.put("invite", "VORTIX"); s.put("color", 0xFF7C4DFF); s.put("created", now);
        db.insert("servers", null, s);
        String[][] ch = {{"c_welcome", "welcome", "Start here", "1"}, {"c_general", "general", "Talk about anything", "0"},
                {"c_news", "announcements", "News from the Vortix team", "1"}, {"c_off", "off-topic", "Memes, music and random stuff", "0"}};
        for (int i = 0; i < ch.length; i++) {
            ContentValues c = new ContentValues();
            c.put("id", ch[i][0]); c.put("server_id", "s_hq"); c.put("name", ch[i][1]);
            c.put("topic", ch[i][2]); c.put("kind", Integer.parseInt(ch[i][3])); c.put("pos", i);
            db.insert("channels", null, c);
        }
        for (String u : new String[]{"u_vortix", "u_nova", "u_pixel"}) addMember(db, "s_hq", u, u.equals("u_vortix") ? "owner" : "member");
        putMessage(db, "c_welcome", "u_vortix", "Welcome to Vortix HQ. Pick a channel from the left panel and say hi.", now - 60000);
        putMessage(db, "c_general", "u_nova", "Hey everyone, this is a local demo server running fully on your phone.", now - 30000);
        putMessage(db, "c_general", "u_vortix", "Type /help in any channel to see what I can do.", now - 20000);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) { }

    private void seedUser(SQLiteDatabase db, String id, String un, String disp, String bio, int status, int accent, long now) {
        ContentValues v = new ContentValues();
        v.put("id", id); v.put("username", un); v.put("display", disp); v.put("hash", ""); v.put("salt", "");
        v.put("bio", bio); v.put("status", status); v.put("accent", accent); v.put("banner", accent);
        v.put("xp", 0); v.put("bot", 1); v.put("created", now);
        db.insert("users", null, v);
    }

    private void addMember(SQLiteDatabase db, String sid, String uid, String role) {
        ContentValues v = new ContentValues();
        v.put("server_id", sid); v.put("user_id", uid); v.put("role", role);
        db.insertWithOnConflict("members", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void putMessage(SQLiteDatabase db, String ch, String author, String text, long ts) {
        ContentValues v = new ContentValues();
        v.put("id", id("m_")); v.put("channel_id", ch); v.put("author", author); v.put("content", text);
        v.put("ts", ts); v.put("edited", 0); v.put("pinned", 0);
        db.insert("messages", null, v);
    }

    private String id(String p) {
        return p + Long.toString(System.nanoTime(), 36) + Integer.toString(rnd.nextInt(1296), 36);
    }

    // ---------------------------------------------------------------- accounts
    public String me() { return sp.getString("uid", null); }
    public Models.User meUser() { String id = me(); return id == null ? null : user(id); }
    public void logout() {
        FirebaseAuth.getInstance().signOut();
        sp.edit().remove("uid").apply();
        notifyKey("profile");
    }

    private String hash(String salt, String pass) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest((salt + pass).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder();
            for (byte x : b) s.append(String.format("%02x", x));
            return s.toString();
        } catch (Exception e) { return ""; }
    }

    /** Returns null on success, otherwise an error message. */
    public String register(String username, String display, String pass) {
        username = username.trim().toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9_]{3,20}")) return "Username: 3-20 letters, numbers or _";
        if (pass.length() < 6) return "Password needs at least 6 characters";
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM users WHERE username=?", new String[]{username});
        boolean taken = c.moveToFirst();
        c.close();
        if (taken) return "That username is already taken";
        String uid = id("u_"), salt = id("s_");
        int[] ac = Theme.ACCENTS;
        ContentValues v = new ContentValues();
        v.put("id", uid); v.put("username", username);
        v.put("display", display == null || display.trim().isEmpty() ? username : display.trim());
        v.put("hash", hash(salt, pass)); v.put("salt", salt); v.put("bio", "");
        v.put("status", 0); v.put("accent", ac[rnd.nextInt(ac.length)]); v.put("banner", ac[rnd.nextInt(4)]);
        v.put("xp", 0); v.put("bot", 0); v.put("created", System.currentTimeMillis());
        db.insert("users", null, v);
        addMember(db, "s_hq", uid, "member");
        sp.edit().putString("uid", uid).apply();
        putMessage(db, "c_welcome", "u_vortix", "Welcome @" + username + ". Your profile levels up as you chat.", System.currentTimeMillis());
        notifyKey("servers"); notifyKey("profile");
        return null;
    }

    public String login(String username, String pass) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,hash,salt FROM users WHERE username=?",
                new String[]{username.trim().toLowerCase(Locale.ROOT)});
        try {
            if (!c.moveToFirst()) return "Wrong username or password";
            String h = c.getString(1);
            if (h == null || h.isEmpty() || !h.equals(hash(c.getString(2), pass))) return "Wrong username or password";
            sp.edit().putString("uid", c.getString(0)).apply();
            return null;
        } finally { c.close(); }
    }


    // Firebase Authentication bridge. SQLite remains a local cache for now.
    public interface AuthCallback {
        void onComplete(String error);
    }

    private String firebaseEmail(String username) {
        return username.trim().toLowerCase(Locale.ROOT) + "@vortix.app";
    }

    public void registerFirebase(
            String username, String display, String pass, AuthCallback callback) {
        final String un = username == null ? "" :
                username.trim().toLowerCase(Locale.ROOT);
        final String name = display == null || display.trim().isEmpty()
                ? un : display.trim();

        if (!un.matches("[a-z0-9_]{3,20}")) {
            callback.onComplete("Username: 3-20 letters, numbers or _");
            return;
        }
        if (pass == null || pass.length() < 6) {
            callback.onComplete("Password needs at least 6 characters");
            return;
        }

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM users WHERE username=?", new String[]{un});
        boolean taken;
        try {
            taken = c.moveToFirst();
        } finally {
            c.close();
        }
        if (taken) {
            callback.onComplete("That username is already taken");
            return;
        }

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(firebaseEmail(un), pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        callback.onComplete(ex == null
                                ? "Registration failed. Please try again."
                                : "Registration failed: " + ex.getLocalizedMessage());
                        return;
                    }

                    String uid = FirebaseAuth.getInstance().getUid();
                    String error = uid == null ? "Firebase did not return a user ID."
                            : cacheFirebaseUser(uid, un, name);

                    if (error != null) {
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            FirebaseAuth.getInstance().getCurrentUser()
                                    .delete().addOnCompleteListener(cleanup ->
                                            callback.onComplete(error));
                        } else {
                            callback.onComplete(error);
                        }
                        return;
                    }
                    callback.onComplete(null);
                });
    }

    public void loginFirebase(String username, String pass, AuthCallback callback) {
        final String un = username == null ? "" :
                username.trim().toLowerCase(Locale.ROOT);

        if (!un.matches("[a-z0-9_]{3,20}")) {
            callback.onComplete("Enter a valid username");
            return;
        }
        if (pass == null || pass.isEmpty()) {
            callback.onComplete("Enter your password");
            return;
        }

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(firebaseEmail(un), pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onComplete("Wrong username or password");
                        return;
                    }

                    String uid = FirebaseAuth.getInstance().getUid();
                    Models.User cached = uid == null ? null : user(uid);
                    String display = cached == null ? un : cached.display;
                    String error = uid == null ? "Firebase did not return a user ID."
                            : cacheFirebaseUser(uid, un, display);

                    if (error != null) {
                        FirebaseAuth.getInstance().signOut();
                        callback.onComplete(error);
                        return;
                    }
                    callback.onComplete(null);
                });
    }

    private String cacheFirebaseUser(String uid, String username, String display) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor existing = db.rawQuery(
                "SELECT 1 FROM users WHERE id=?", new String[]{uid});
        boolean found;
        try {
            found = existing.moveToFirst();
        } finally {
            existing.close();
        }

        if (!found) {
            Cursor nameCheck = db.rawQuery(
                    "SELECT 1 FROM users WHERE username=?", new String[]{username});
            boolean nameTaken;
            try {
                nameTaken = nameCheck.moveToFirst();
            } finally {
                nameCheck.close();
            }
            if (nameTaken) return "That username conflicts with a local profile.";

            int[] accents = Theme.ACCENTS;
            int accent = accents[rnd.nextInt(accents.length)];
            long now = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("id", uid);
            values.put("username", username);
            values.put("display", display);
            values.put("hash", "");
            values.put("salt", "");
            values.put("bio", "");
            values.put("status", 0);
            values.put("avatar", "");
            values.put("accent", accent);
            values.put("banner", accent);
            values.put("xp", 0);
            values.put("bot", 0);
            values.put("created", now);

            long inserted = db.insertWithOnConflict(
                    "users", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (inserted == -1) return "Could not save the local profile.";
        }

        sp.edit().putString("uid", uid).apply();

        Cursor server = db.rawQuery(
                "SELECT 1 FROM servers WHERE id='s_hq'", null);
        boolean hasServer;
        try {
            hasServer = server.moveToFirst();
        } finally {
            server.close();
        }
        if (hasServer) addMember(db, "s_hq", uid, "member");

        notifyKey("profile");
        notifyKey("servers");
        return null;
    }

    private static final String UCOLS = "id,username,display,bio,avatar,status,accent,banner,xp,bot,created";

    private Models.User readUser(Cursor c) {
        Models.User u = new Models.User();
        u.id = c.getString(0); u.username = c.getString(1); u.display = c.getString(2); u.bio = c.getString(3);
        u.avatar = c.getString(4); u.status = c.getInt(5); u.accent = c.getInt(6); u.banner = c.getInt(7);
        u.xp = c.getInt(8); u.bot = c.getInt(9) == 1; u.created = c.getLong(10);
        return u;
    }

    public Models.User user(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + UCOLS + " FROM users WHERE id=?", new String[]{id});
        try { return c.moveToFirst() ? readUser(c) : null; } finally { c.close(); }
    }

    public List<Models.User> searchUsers(String q) {
        String like = "%" + q.trim() + "%";
        Cursor c = getReadableDatabase().rawQuery("SELECT " + UCOLS + " FROM users WHERE id<>? AND (username LIKE ? OR display LIKE ?) ORDER BY display LIMIT 50",
                new String[]{me(), like, like});
        List<Models.User> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readUser(c));
        c.close();
        return l;
    }

    public void updateProfile(String display, String bio, int status, String avatar, int banner) {
        ContentValues v = new ContentValues();
        v.put("display", display); v.put("bio", bio); v.put("status", status); v.put("banner", banner);
        if (avatar != null) v.put("avatar", avatar);
        getWritableDatabase().update("users", v, "id=?", new String[]{me()});
        notifyKey("profile");
    }

    public void setStatus(int s) {
        ContentValues v = new ContentValues();
        v.put("status", s);
        getWritableDatabase().update("users", v, "id=?", new String[]{me()});
        notifyKey("profile");
    }

    // ---------------------------------------------------------------- levels
    public static int level(int xp) { return 1 + (int) Math.floor(Math.sqrt(xp / 30.0)); }
    public static int xpFor(int level) { return 30 * (level - 1) * (level - 1); }

    private void addXp(String uid, int n) {
        getWritableDatabase().execSQL("UPDATE users SET xp=xp+? WHERE id=?", new Object[]{n, uid});
        notifyKey("profile");
    }

    public int messageCount(String uid) { return scalar("SELECT COUNT(*) FROM messages WHERE author=?", uid); }
    public int serversOwned(String uid) { return scalar("SELECT COUNT(*) FROM servers WHERE owner=?", uid); }

    private int scalar(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    // ---------------------------------------------------------------- servers + channels
    public List<Models.Server> servers() {
        Cursor c = getReadableDatabase().rawQuery("SELECT s.id,s.name,s.owner,s.invite,s.color FROM servers s JOIN members m ON m.server_id=s.id WHERE m.user_id=? ORDER BY s.created",
                new String[]{me()});
        List<Models.Server> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readServer(c));
        c.close();
        return l;
    }

    private Models.Server readServer(Cursor c) {
        Models.Server s = new Models.Server();
        s.id = c.getString(0); s.name = c.getString(1); s.owner = c.getString(2); s.invite = c.getString(3); s.color = c.getInt(4);
        return s;
    }

    public Models.Server server(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,owner,invite,color FROM servers WHERE id=?", new String[]{id});
        try { return c.moveToFirst() ? readServer(c) : null; } finally { c.close(); }
    }

    public String createServer(String name) {
        SQLiteDatabase db = getWritableDatabase();
        String sid = id("s_");
        String code = "";
        String abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 6; i++) code += abc.charAt(rnd.nextInt(abc.length()));
        ContentValues v = new ContentValues();
        v.put("id", sid); v.put("name", name); v.put("owner", me()); v.put("invite", code);
        v.put("color", Theme.ACCENTS[rnd.nextInt(Theme.ACCENTS.length)]); v.put("created", System.currentTimeMillis());
        db.insert("servers", null, v);
        addMember(db, sid, me(), "owner");
        createChannelRaw(db, sid, "general", "General chat", 0, 0);
        createChannelRaw(db, sid, "announcements", "Server news", 1, 1);
        addXp(me(), 20);
        notifyKey("servers");
        return sid;
    }

    public Models.Server joinServer(String code) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,owner,invite,color FROM servers WHERE invite=?",
                new String[]{code.trim().toUpperCase(Locale.ROOT)});
        Models.Server s = null;
        if (c.moveToFirst()) s = readServer(c);
        c.close();
        if (s != null) { addMember(getWritableDatabase(), s.id, me(), "member"); notifyKey("servers"); }
        return s;
    }

    private void createChannelRaw(SQLiteDatabase db, String sid, String name, String topic, int kind, int pos) {
        ContentValues v = new ContentValues();
        v.put("id", id("c_")); v.put("server_id", sid); v.put("name", name); v.put("topic", topic);
        v.put("kind", kind); v.put("pos", pos);
        db.insert("channels", null, v);
    }

    public void createChannel(String sid, String name, String topic, int kind) {
        String n = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        createChannelRaw(getWritableDatabase(), sid, n, topic, kind, channels(sid).size());
        notifyKey("channels");
    }

    private Models.Channel readChannel(Cursor c) {
        Models.Channel ch = new Models.Channel();
        ch.id = c.getString(0); ch.serverId = c.getString(1); ch.name = c.getString(2); ch.topic = c.getString(3); ch.kind = c.getInt(4);
        return ch;
    }

    public List<Models.Channel> channels(String sid) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,server_id,name,topic,kind FROM channels WHERE server_id=? ORDER BY pos", new String[]{sid});
        List<Models.Channel> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readChannel(c));
        c.close();
        return l;
    }

    public Models.Channel channel(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,server_id,name,topic,kind FROM channels WHERE id=?", new String[]{id});
        try { return c.moveToFirst() ? readChannel(c) : null; } finally { c.close(); }
    }

    public List<Models.User> members(String sid) {
        Cursor c = getReadableDatabase().rawQuery("SELECT u.id,u.username,u.display,u.bio,u.avatar,u.status,u.accent,u.banner,u.xp,u.bot,u.created FROM users u JOIN members m ON m.user_id=u.id WHERE m.server_id=? ORDER BY u.status, u.display",
                new String[]{sid});
        List<Models.User> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readUser(c));
        c.close();
        return l;
    }

    // ---------------------------------------------------------------- messages
    private static final String MCOLS = "id,channel_id,author,content,reply_to,attach,ts,edited,pinned";

    private Models.Message readMsg(Cursor c) {
        Models.Message m = new Models.Message();
        m.id = c.getString(0); m.channelId = c.getString(1); m.author = c.getString(2); m.content = c.getString(3);
        m.replyTo = c.getString(4); m.attach = c.getString(5); m.ts = c.getLong(6);
        m.edited = c.getInt(7) == 1; m.pinned = c.getInt(8) == 1;
        return m;
    }

    public List<Models.Message> messages(String ch) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + MCOLS + " FROM messages WHERE channel_id=? ORDER BY ts DESC LIMIT 300", new String[]{ch});
        List<Models.Message> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readMsg(c));
        c.close();
        Collections.reverse(l);
        return l;
    }

    public List<Models.Message> pinned(String ch) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + MCOLS + " FROM messages WHERE channel_id=? AND pinned=1 ORDER BY ts DESC", new String[]{ch});
        List<Models.Message> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readMsg(c));
        c.close();
        return l;
    }

    public Models.Message message(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + MCOLS + " FROM messages WHERE id=?", new String[]{id});
        try { return c.moveToFirst() ? readMsg(c) : null; } finally { c.close(); }
    }

    private void insertMessage(String ch, String author, String text, String replyTo, String attach) {
        ContentValues v = new ContentValues();
        v.put("id", id("m_")); v.put("channel_id", ch); v.put("author", author); v.put("content", text);
        v.put("reply_to", replyTo); v.put("attach", attach); v.put("ts", System.currentTimeMillis());
        v.put("edited", 0); v.put("pinned", 0);
        getWritableDatabase().insert("messages", null, v);
        notifyKey("msg:" + ch);
        notifyKey("dms");
        if (!author.equals(me())) {
            final String chf = ch;
            main.post(() -> Sfx.play(chf.equals(openChannel) ? Sfx.RECV : Sfx.NOTIFY));
        }
    }

    public void send(String ch, String text, String replyTo, String attach) {
        insertMessage(ch, me(), text, replyTo, attach);
        addXp(me(), attach != null ? 8 : 5);
        markRead(ch);
        maybeBot(ch, text);
    }

    public void edit(String mid, String text) {
        ContentValues v = new ContentValues();
        v.put("content", text); v.put("edited", 1);
        getWritableDatabase().update("messages", v, "id=?", new String[]{mid});
        notifyKey("msg:" + channelOf(mid));
    }

    public void delete(String mid) {
        String ch = channelOf(mid);
        getWritableDatabase().delete("messages", "id=?", new String[]{mid});
        getWritableDatabase().delete("reactions", "message_id=?", new String[]{mid});
        notifyKey("msg:" + ch);
        notifyKey("dms");
    }

    public void togglePin(String mid) {
        getWritableDatabase().execSQL("UPDATE messages SET pinned=1-pinned WHERE id=?", new Object[]{mid});
        notifyKey("msg:" + channelOf(mid));
    }

    private String channelOf(String mid) {
        Cursor c = getReadableDatabase().rawQuery("SELECT channel_id FROM messages WHERE id=?", new String[]{mid});
        try { return c.moveToFirst() ? c.getString(0) : ""; } finally { c.close(); }
    }

    /** result[0..4] = count per reaction kind, result[5] = bitmask of kinds I used. */
    public int[] reactions(String mid) {
        int[] r = new int[6];
        Cursor c = getReadableDatabase().rawQuery("SELECT kind,user_id FROM reactions WHERE message_id=?", new String[]{mid});
        while (c.moveToNext()) {
            int k = c.getInt(0);
            if (k < 0 || k > 4) continue;
            r[k]++;
            if (c.getString(1).equals(me())) r[5] |= 1 << k;
        }
        c.close();
        return r;
    }

    public void react(String mid, int kind) {
        SQLiteDatabase db = getWritableDatabase();
        int n = db.delete("reactions", "message_id=? AND user_id=? AND kind=?", new String[]{mid, me(), String.valueOf(kind)});
        if (n == 0) {
            ContentValues v = new ContentValues();
            v.put("message_id", mid); v.put("user_id", me()); v.put("kind", kind);
            db.insert("reactions", null, v);
        }
        notifyKey("msg:" + channelOf(mid));
    }

    public List<Models.Message> mentions() {
        Models.User u = meUser();
        Cursor c = getReadableDatabase().rawQuery("SELECT " + MCOLS + " FROM messages WHERE author<>? AND (content LIKE ? OR reply_to IN (SELECT id FROM messages WHERE author=?)) ORDER BY ts DESC LIMIT 60",
                new String[]{me(), "%@" + u.username + "%", me()});
        List<Models.Message> l = new ArrayList<>();
        while (c.moveToNext()) l.add(readMsg(c));
        c.close();
        return l;
    }

    // ---------------------------------------------------------------- direct messages
    public String startDm(String other) {
        String m = me();
        String a = m.compareTo(other) < 0 ? m : other, b = m.compareTo(other) < 0 ? other : m;
        String id = "dm_" + a + "_" + b;
        ContentValues v = new ContentValues();
        v.put("id", id); v.put("a", a); v.put("b", b);
        getWritableDatabase().insertWithOnConflict("dms", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        notifyKey("dms");
        return id;
    }

    public String dmOther(String dmId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT a,b FROM dms WHERE id=?", new String[]{dmId});
        try {
            if (!c.moveToFirst()) return null;
            return c.getString(0).equals(me()) ? c.getString(1) : c.getString(0);
        } finally { c.close(); }
    }

    public List<Models.Dm> dms() {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,a,b FROM dms WHERE a=? OR b=?", new String[]{me(), me()});
        List<Models.Dm> l = new ArrayList<>();
        while (c.moveToNext()) {
            Models.Dm d = new Models.Dm();
            d.id = c.getString(0);
            d.other = user(c.getString(1).equals(me()) ? c.getString(2) : c.getString(1));
            d.last = lastMessage(d.id);
            d.unread = unread(d.id);
            if (d.other != null) l.add(d);
        }
        c.close();
        Collections.sort(l, (x, y) -> Long.compare(y.last == null ? 0 : y.last.ts, x.last == null ? 0 : x.last.ts));
        return l;
    }

    private Models.Message lastMessage(String ch) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + MCOLS + " FROM messages WHERE channel_id=? ORDER BY ts DESC LIMIT 1", new String[]{ch});
        try { return c.moveToFirst() ? readMsg(c) : null; } finally { c.close(); }
    }

    // ---------------------------------------------------------------- unread tracking
    public void markRead(String ch) {
        ContentValues v = new ContentValues();
        v.put("channel_id", ch); v.put("user_id", me()); v.put("ts", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("reads", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        notifyKey("reads");
    }

    public int unread(String ch) {
        return scalar("SELECT COUNT(*) FROM messages WHERE channel_id=? AND author<>? AND ts>COALESCE((SELECT ts FROM reads WHERE channel_id=? AND user_id=?),0)",
                ch, me(), ch, me());
    }

    public int serverUnread(String sid) {
        return scalar("SELECT COUNT(*) FROM messages m JOIN channels c ON c.id=m.channel_id WHERE c.server_id=? AND m.author<>? AND m.ts>COALESCE((SELECT ts FROM reads r WHERE r.channel_id=m.channel_id AND r.user_id=?),0)",
                sid, me(), me());
    }

    public int dmUnreadTotal() {
        int t = 0;
        for (Models.Dm d : dms()) t += d.unread;
        return t;
    }

    // ---------------------------------------------------------------- local bots (demo only)
    private static final String[] VORTIX = {"Got it.", "I am just a small local bot. Type /help.", "Noted. Connect Firestore to chat with real people.", "Nice one."};
    private static final String[] NOVA = {"Haha, tell me more.", "That is a good point.", "I was just thinking the same.", "Wait, really? Go on.", "Love the vibe of this app."};
    private static final String[] PIXEL = {"Okay okay, I hear you.", "Brb, grabbing a snack.", "Send me a photo of that.", "That sounds fun.", "Totally agree."};

    private void maybeBot(final String ch, String text) {
        String bot = null;
        if (ch.startsWith("dm_")) {
            String o = dmOther(ch);
            Models.User u = o == null ? null : user(o);
            if (u != null && u.bot) bot = u.id;
        } else if (text.startsWith("/") || text.toLowerCase(Locale.ROOT).contains("@vortix")) {
            bot = "u_vortix";
        }
        if (bot == null) return;
        final String b = bot;
        final String reply = botSay(b, text);
        main.postDelayed(() -> insertMessage(ch, b, reply, null, null), 700 + rnd.nextInt(800));
    }

    private String botSay(String bot, String in) {
        String t = in.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("/help")) return "Commands: /help  /ping  /time  /roll  /about";
        if (t.startsWith("/ping")) return "Pong. Offline mode, so latency is 0 ms.";
        if (t.startsWith("/time")) return "It is " + new SimpleDateFormat("HH:mm, EEE d MMM", Locale.getDefault()).format(new Date());
        if (t.startsWith("/roll")) return "You rolled " + (1 + rnd.nextInt(100)) + " out of 100.";
        if (t.startsWith("/about")) return "Vortix local build. Messages live in this phone's database until Firestore is connected.";
        String[] a = bot.equals("u_nova") ? NOVA : bot.equals("u_pixel") ? PIXEL : VORTIX;
        return a[rnd.nextInt(a.length)];
    }
}
