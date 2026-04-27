package pl.edu.uksw.java;

import io.javalin.http.Context;

class PageController {
    void home(Context ctx)    { ctx.render("home.html"); }
    void about(Context ctx)   { ctx.render("about.html"); }
    void contact(Context ctx) { ctx.render("contact.html"); }
}