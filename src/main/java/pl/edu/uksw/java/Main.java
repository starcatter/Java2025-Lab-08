package pl.edu.uksw.java;

public class Main {
    public static void main(String[] args) {
        JavalinApp app = new JavalinApp(8089);
        app.setupAdminAccount("admin","admin");
        app.startServer();
    }  
}  