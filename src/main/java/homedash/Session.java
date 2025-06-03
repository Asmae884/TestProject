package homedash;

public class Session {
    private static int userId;
    private static String login;
    private static boolean isAdmin;

    public static void setUser(int id, String username, boolean admin) {
        userId = id;
        login = username;
        isAdmin = admin;
    }

    public static int getUserId() {
        return userId;
    }

    public static String getLogin() {
        return login;
    }

    public static boolean isAdmin() {
        return isAdmin;
    }

    public static void clear() {
        userId = 0;
        login = null;
        isAdmin = false;
    }
}

