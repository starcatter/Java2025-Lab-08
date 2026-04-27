package pl.edu.uksw.java;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import java.util.Map;

class MemberController {
    private final UserRepository users;

    MemberController(UserRepository users) {
        this.users = users;
    }

    void showMembers(Context ctx) {
        requireLogin(ctx);
        ctx.render("members.html", Map.of("users", users.allUsernames()));
    }

    static void requireLogin(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            throw new UnauthorizedResponse("Login required");
        }
    }
}