# Laboratorium 8: Aplikacja webowa w Javalin — refaktoryzacja, obsługa błędów i testy Selenium

## Informacje organizacyjne

**Data:** 28.04.2026, 8:00–9:30  
**Temat:** Rozbudowa aplikacji webowej w Javalin: podział na kontrolery, obsługa błędów przez wyjątki, zdarzenia cyklu życia serwera, testy end-to-end z Selenium.

**Repozytorium:**  
> https://github.com/starcatter/Java2025-Lab-08

Projekt startowy to prosta aplikacja Javalin z rejestracją, logowaniem i listą użytkowników dostępną po zalogowaniu. Wszystkie handlery są zdefiniowane w jednej klasie `JavalinApp`. Na zajęciach kolejno wyodrębniamy kontrolery, dodajemy obsługę błędów i analizujemy działanie gotowych testów Selenium weryfikujących aplikację w przeglądarce.

**Struktura zajęć:**
- **8:00–8:15** — Wejściówka (3 pytania): powtórka z Lab 7 — HTTP, Javalin, Thymeleaf
- **8:15–8:30** — Omówienie projektu startowego, uruchomienie aplikacji i testów
- **8:30–9:00** — Część 1 (30 min): wyodrębnienie kontrolerów, obsługa błędów, zdarzenia cyklu życia
- **9:00–9:30** — Część 2 (30 min): testy end-to-end z Selenium

**Uwagi techniczne:**
- Wymagana przeglądarka Firefox lub Chrome z odpowiednim sterownikiem (WebDriverManager pobiera automatycznie).
- JDK 17+, Maven — po sklonowaniu wykonaj `mvn clean package`.
- Aby uruchomić testy, użyj panelu Maven w IntelliJ IDEA (`Maven → Lifecycle → test`) lub komendy terminala: `mvn test`.
- Port domyślny: **8089**. Przed uruchomieniem kolejnego wariantu zatrzymaj poprzedni.
- Projekt zawiera gotowe szablony Thymeleaf i plik CSS — skupiamy się na kodzie Javy i testach.

---

## Wejściówka (8:00–8:15)

> **Czas:** 15 minut  
> **Format:** 3 pytania jednokrotnego wyboru.

---

## Omówienie projektu startowego (8:15–8:30)

### Co robi aplikacja startowa?

Aplikacja to prosta witryna z systemem kont użytkowników:

| Ścieżka | Metoda | Opis |
|---|---|---|
| `/` | GET | Strona główna |
| `/about` | GET | Informacje o aplikacji |
| `/contact` | GET | Dane kontaktowe |
| `/register` | GET | Formularz rejestracji |
| `/register` | POST | Obsługa rejestracji |
| `/login` | GET | Formularz logowania |
| `/login` | POST | Obsługa logowania |
| `/logout` | GET | Wylogowanie |
| `/members` | GET | Lista zarejestrowanych użytkowników (wymaga zalogowania) |

Dane użytkowników są przechowywane **w pamięci** (`UserRepository`) — brak bazy danych, co jest celowe
ze względu na prostotę.

### Kluczowe klasy

```
Main.java           — punkt wejścia, tworzy JavalinApp i dodaje konto admina
JavalinApp.java     — konfiguracja Javalin, definicja ścieżek i handlerów
UserRepository.java — przechowywanie i wyszukiwanie użytkowników
User.java / UserType.java — model domenowy
```

### Co jest nie tak z kodem startowym?

Uruchom aplikację i przejrzyj `JavalinApp.java`. Zauważ:

1. **Wszystkie handlery są metodami prywatnymi jednej klasy** — `JavalinApp` robi zbyt wiele.
2. **Brak obsługi błędów** — wpisanie nieistniejącego URL daje domyślną stronę błędu Jetty.
3. **Kontrola dostępu jest zapisana bezpośrednio w handlerze** — sprawdzenie sesji znajduje się wewnątrz `showMembers`.
4. **Brak komunikatów startowych** — nie wiadomo, czy serwer się uruchomił.

Te problemy naprawimy w Części 1.

### Zadanie wstępne (obowiązkowe)

Przed rozpoczęciem refaktoryzacji uruchom testy projektu startowego, aby upewnić się, że środowisko jest poprawnie skonfigurowane.

W IntelliJ IDEA: otwórz panel **Maven** (`View → Tool Windows → Maven` lub kliknij zakładkę Maven po prawej stronie), rozwiń Lifecycle, kliknij dwukrotnie `test`.

Alternatywnie w terminalu (w katalogu projektu):
```bash
mvn clean test
```

Wszystkie testy w klasie `ApplicationTests` powinny przejść. Jeśli nie — sprawdź, czy Firefox jest zainstalowany (lub zmień konfigurację sterownika w `SeleniumTestBase` na Chrome) i czy WebDriverManager może pobrać sterownik.

---

## Część 1 (8:30–9:00): Refaktoryzacja i obsługa błędów

> **Szacowany czas:** 30 minut  
> **Zadania obowiązkowe:** 1.1a, 1.1b, 1.1c, 1.2a, 1.2b, 1.4b  
> **Zadania opcjonalne (jeśli starczy czasu):** 1.3a, 1.3b

---

### 1.1 Wyodrębnienie kontrolerów

Dobrze zaprojektowana aplikacja webowa rozdziela odpowiedzialności między klasy. Wzorzec **MVC** (*Model–View–Controller*)
mówi, że logika obsługi żądań (kontrolery) powinna być oddzielona od konfiguracji serwera.

W Javalin handlery to metody przyjmujące `Context` — naturalnym rozwiązaniem są klasy grupujące powiązane
handlery i przyjmujące zależności przez konstruktor.

Po refaktoryzacji `JavalinApp` powinien odpowiadać głównie za konfigurację serwera, rejestrację ścieżek
i składanie zależności. Logika obsługi żądań powinna trafić do kontrolerów.

**Materiały:**
- [Javalin Documentation — Handlers](https://javalin.io/documentation#handlers)
- [Javalin Samples — MVC example](https://github.com/javalin/javalin-samples)

**Zadanie 1.1a (obowiązkowe)**  
Utwórz klasę `PageController.java` z metodami dla stron statycznych:

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;

class PageController {
    void home(Context ctx)    { ctx.render("home.html"); }
    void about(Context ctx)   { ctx.render("about.html"); }
    void contact(Context ctx) { ctx.render("contact.html"); }
}
```

Podepnij kontroler do ścieżek w `JavalinApp`, zastępując lambdy referencjami do metod:

```java
var pages = new PageController();

config.routes.get("/",        pages::home);
config.routes.get("/about",   pages::about);
config.routes.get("/contact", pages::contact);
```

Uruchom aplikację i sprawdź, że strony nadal działają.

**Zadanie 1.1b (obowiązkowe)**  
Utwórz `AuthController.java` z metodami obsługującymi rejestrację i logowanie.
Kontroler przyjmuje `UserRepository` przez konstruktor:

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.Map;

class AuthController {
    private final UserRepository users;

    AuthController(UserRepository users) {
        this.users = users;
    }

    void showRegister(Context ctx) {
        ctx.render("register.html", errorModel(ctx.queryParam("error")));
    }

    void handleRegister(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            ctx.redirect("/register?error=empty_fields");
            return;
        }
        if (users.existsByUsername(username)) {
            ctx.redirect("/register?error=username_taken");
            return;
        }

        users.add(new User(username, password));
        ctx.redirect("/login");
    }

    void showLogin(Context ctx) {
        ctx.render("login.html", errorModel(ctx.queryParam("error")));
    }

    void handleLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (users.authenticate(username, password)) {
            ctx.sessionAttribute("user", username);
            ctx.redirect("/");
        } else {
            ctx.redirect("/login?error=invalid_credentials");
        }
    }

    void handleLogout(Context ctx) {
        ctx.sessionAttribute("user", null);
        ctx.redirect("/");
    }

    static Map<String, Object> errorModel(String error) {
        return error != null ? Map.of("error", error) : Map.of();
    }
}
```

**Zadanie 1.1c (obowiązkowe)**  
Utwórz `MemberController.java` z metodą `showMembers`. Na razie zostaw
sprawdzenie sesji bezpośrednio w handlerze — zmienimy to w następnym kroku (jeśli starczy czasu):

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.Map;

class MemberController {
    private final UserRepository users;

    MemberController(UserRepository users) {
        this.users = users;
    }

    void showMembers(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            ctx.status(403).result("Forbidden: login required");
            return;
        }
        ctx.render("members.html", Map.of("users", users.allUsernames()));
    }
}
```

Zaktualizuj `JavalinApp` tak, aby konfigurował ścieżki za pomocą trzech kontrolerów. Sprawdź, czy
aplikacja działa identycznie jak przed refaktoryzacją.

> **Sprawdzenie:** Czy plik `JavalinApp.java` nie zawiera już logiki biznesowej — tylko konfigurację?

### 1.2 Obsługa błędów HTTP

Domyślne strony błędów Jetty są mało przyjazne dla użytkownika i mogą ujawniać szczegóły techniczne. Javalin 7 pozwala zdefiniować
własne handlery błędów i wyjątków wewnątrz bloku konfiguracyjnego.

W tej części rozróżniamy dwa mechanizmy:
- **handler błędu HTTP** (`error`) — gdy znamy już kod odpowiedzi, np. 404,
- **handler wyjątku** (`exception`) — gdy w trakcie obsługi żądania został zgłoszony wyjątek.

**Materiały:**
- [Javalin Documentation — Error Mapping](https://javalin.io/documentation#error-mapping)
- [Javalin Documentation — Exception Handling](https://javalin.io/documentation#exception-handling)
- [Javalin 7 Migration Guide — Exception handlers are now configured upfront](https://javalin.io/migration-guide-javalin-6-to-7)

> **Uwaga Javalin 7:** Handlery błędów i wyjątków definiuje się przez `config.routes.error(...)` i
> `config.routes.exception(...)` — tak samo jak ścieżki. Nie ma już `app.error(...)` poza blokiem konfiguracyjnym.

**Zadanie 1.2a (obowiązkowe)**  
Dodaj szablon `error.html` do katalogu `templates/`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="'Błąd ' + ${code}">Błąd</title>
    <link rel="stylesheet" th:href="@{/style/style.css}">
</head>
<body>
    <h1 th:text="'Błąd ' + ${code}">Błąd</h1>
    <p th:text="${message}">Coś poszło nie tak.</p>
    <a href="/">Powrót na stronę główną</a>
</body>
</html>
```

**Zadanie 1.2b (obowiązkowe)**  
Dodaj handler błędu 404 i ogólny handler wyjątków w bloku konfiguracyjnym `JavalinApp`:

```java
config.routes.error(404, ctx ->
    ctx.render("error.html", Map.of(
        "code",    "404",
        "message", "Nie znaleziono strony: " + ctx.path()
    ))
);

config.routes.exception(Exception.class, (e, ctx) -> {
    e.printStackTrace();
    ctx.status(500).render("error.html", Map.of(
        "code",    "500",
        "message", "Wewnętrzny błąd serwera"
    ));
});
```

Przetestuj: wpisz nieistniejący URL w przeglądarce. Powinna się wyświetlić własna strona 404.

---

### 1.3 Kontrola dostępu przez wyjątek (opcjonalne)

> **Wykonaj to zadanie tylko jeśli masz jeszcze czas przed 9:00.**

Sprawdzenie `if (session == null)` wewnątrz handlera działa, ale nie skaluje się — przy wielu chronionych
stronach trzeba powielać tę logikę. Lepszym podejściem jest zgłoszenie wyjątku, który zostanie przechwycony przez
centralny handler.

Javalin oferuje rodzinę gotowych klas wyjątków odpowiadających kodom HTTP, m.in.:
- `BadRequestResponse` (400)
- `UnauthorizedResponse` (401)
- `ForbiddenResponse` (403)
- `NotFoundResponse` (404)
- `InternalServerErrorResponse` (500)

Każdy z nich można przechwycić osobno albo zbiorczo przez nadklasę
`HttpResponseException`. Wykorzystamy `UnauthorizedResponse` jako sygnał „wymagane logowanie" rzucany
z handlerów chronionych zasobów.

**Materiały:**
- [Javalin Documentation — HTTP exceptions](https://javalin.io/documentation#http-exceptions)

**Zadanie 1.3a (zalecane)**  
Dodaj do `MemberController` statyczną metodę pomocniczą `requireLogin`:

```java
import io.javalin.http.UnauthorizedResponse;

static void requireLogin(Context ctx) {
    if (ctx.sessionAttribute("user") == null) {
        throw new UnauthorizedResponse("Login required");
    }
}
```

Zaktualizuj `showMembers`, aby używał tej metody zamiast ręcznego sprawdzenia:

```java
void showMembers(Context ctx) {
    requireLogin(ctx);
    ctx.render("members.html", Map.of("users", users.allUsernames()));
}
```

**Zadanie 1.3b (zalecane)**  
Dodaj dedykowany handler dla `UnauthorizedResponse` w bloku konfiguracyjnym,
przed ogólnym handlerem `Exception.class`:

```java
config.routes.exception(UnauthorizedResponse.class, (e, ctx) ->
    ctx.redirect("/login?error=login_required")
);
```

Przetestuj: wejdź na `/members` bez logowania. Powinieneś zostać przekierowany na `/login`.

> **Dyskusja:** Dlaczego dedykowany handler `UnauthorizedResponse` warto zdefiniować przed ogólnym
> handlerem `Exception.class`? Co mogłoby się stać, gdyby wyjątek został przechwycony przez zbyt ogólną obsługę?

### 1.4 Zdarzenia cyklu życia serwera

W bloku konfiguracyjnym `Javalin.create()` można zdefiniować zdarzenia cyklu życia serwera, takie jak `serverStarting` i `serverStarted`, a także wszystkie ustawienia infrastruktury przed uruchomieniem aplikacji.

Zwróć uwagę, że w projekcie startowym port jest przekazywany w konstruktorze `JavalinApp`, a metoda `start()` nie wymaga argumentów — wewnętrznie wywołuje `app.start(port)`.

**Materiały:**
- [Javalin Documentation — Configuration](https://javalin.io/documentation#configuration)

**Zadanie 1.4a (obowiązkowe)**  
Dodaj zdarzenia cyklu życia do bloku konfiguracyjnego w `JavalinApp`:

```java
config.events.serverStarting(() ->
    System.out.println("Uruchamianie serwera na porcie " + port + "..."));
config.events.serverStarted(() ->
    System.out.println("Serwer gotowy: http://localhost:" + port));
```

Uruchom aplikację i sprawdź komunikaty w konsoli.

---

## Część 2 (9:00–9:30): Testy end-to-end z Selenium

> **Szacowany czas:** 30 minut  
> **Zadania obowiązkowe:** 2.3a, 2.3b, 2.3c, 2.3d  
> **Zadania opcjonalne:** 2.4 (Page Object)

---

### 2.1 Czym jest Selenium i po co go używamy?

Testy jednostkowe (JUnit) sprawdzają pojedyncze klasy. Testy integracyjne sprawdzają współpracę
komponentów. **Testy end-to-end** (E2E) sprawdzają całą aplikację z perspektywy użytkownika — uruchamiają
prawdziwą przeglądarkę i symulują kliknięcia.

**Selenium WebDriver** steruje przeglądarką programowo: otwiera strony, wypełnia formularze, klika przyciski
i odczytuje treść. `WebDriverManager` automatycznie pobiera i konfiguruje odpowiedni sterownik przeglądarki.

**`JavalinTest`** to narzędzie testowe wbudowane w Javalin. Wywołanie
`JavalinTest.test(server, (srv, client) -> {...})` uruchamia podany serwer na losowym, wolnym porcie,
wykonuje lambdę z dostępem do klienta HTTP (`client.getOrigin()` zwraca bazowy URL serwera), a po
zakończeniu lambdy zatrzymuje serwer. Dzięki temu każdy test działa na świeżym, niezależnym serwerze —
testy nie wymagają ręcznego sprzątania oraz nie występuje ryzyko kolizji portów.

**Materiały:**
- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [Selenium WebDriver API](https://www.selenium.dev/documentation/webdriver/)
- [WebDriverManager](https://bonigarcia.dev/webdrivermanager/)
- [Javalin Testing](https://javalin.io/documentation#testing)

### 2.2 Klasa bazowa testów

Projekt startowy zawiera klasę `SeleniumTestBase`. Zwróć uwagę, że `JavalinApp` udostępnia instancję `Javalin` przez getter — jest on wykorzystywany w testach:

```java
// W JavalinApp:
public Javalin getServer() { return app; }
```

> **Uwaga:** W testach `JavalinApp` tworzy instancję `Javalin`, ale nie uruchamia jej samodzielnie
> na stałym porcie. Uruchomieniem i zatrzymaniem serwera na potrzeby testu zarządza `JavalinTest.test(...)`.

Klasa bazowa z projektu startowego:

```java
package pl.edu.uksw.java;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Klasa bazowa dla testów Selenium aplikacji Javalin.
 *
 * Aby użyć Chrome zamiast Firefox, zmień:
 *   WebDriverManager.firefoxdriver() → WebDriverManager.chromedriver()
 *   FirefoxOptions         → ChromeOptions
 *   FirefoxDriver          → ChromeDriver
 * i w pom.xml:
 *   selenium-firefox-driver → selenium-chrome-driver
 */
class SeleniumTestBase {
    WebDriver driver;

    final JavalinApp app;
    final Javalin server;
    final User admin;

    public SeleniumTestBase() {
        app = new JavalinApp(8080);
        server = app.getServer();
        admin = app.setupAdminAccount("admin", "admin");
    }

    @BeforeAll
    static void setupAll() {
        WebDriverManager.firefoxdriver().setup();
    }

    @BeforeEach
    void setupDriver() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        driver = new FirefoxDriver(options);
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

### 2.3 Analiza istniejących testów

Projekt startowy zawiera gotowe testy w klasie `ApplicationTests`. Każdy z nich ilustruje inny aspekt
działania Selenium z Javalin.

**Zadanie 2.3a (obowiązkowe)**  
Uruchom gotowe testy z projektu startowego. Sprawdź, które przechodzą, a które nie.

W IntelliJ IDEA: otwórz klasę `ApplicationTests`, kliknij zieloną ikonę obok nazwy klasy lub pojedynczej metody `▶ Run`.
Alternatywnie użyj panelu Maven → Lifecycle → `test`.

**Zadanie 2.3b (obowiązkowe)**  
Przeanalizuj istniejący test sprawdzający tytuł strony głównej:

```java
@Test
public void testMainPageTitle() {
    JavalinTest.test(server, (server, client) -> {
        driver.get(client.getOrigin() + "/");
        String title = driver.getTitle();
        assertEquals("Javalin App", title, "Title mismatch on main page");
    });
}
```

Zwróć uwagę na:
- `JavalinTest.test(server, ...)` — uruchamia serwer na losowym porcie i zapewnia `client.getOrigin()`.
- `driver.get(url)` — otwiera stronę w przeglądarce.
- `driver.getTitle()` — odczytuje tytuł strony z tagu `<title>`.

**Zadanie 2.3c (obowiązkowe)**  
Przeanalizuj test sprawdzający poprawne logowanie:

```java
@Test
public void testLoginWithValidCredentials() {
    JavalinTest.test(server, (server, client) -> {
        driver.get(client.getOrigin() + "/login");

        driver.findElement(By.name("username")).sendKeys(admin.getUsername());
        driver.findElement(By.name("password")).sendKeys(admin.getPassword());
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals(
            client.getOrigin() + "/",
            driver.getCurrentUrl(),
            "Po zalogowaniu oczekiwano przekierowania na stronę główną"
        );
    });
}
```

Zwróć uwagę na:
- `findElement(By.name("..."))` — wyszukiwanie elementów po atrybucie `name`.
- `sendKeys(...)` — wpisywanie tekstu do pól formularza.
- `click()` — symulacja kliknięcia przycisku.
- `getCurrentUrl()` — odczyt aktualnego adresu przeglądarki po przekierowaniu.

**Zadanie 2.3d (obowiązkowe)**  
Przeanalizuj test pełnego przepływu rejestracji:

```java
@Test
public void testRegistrationFlow() {
    JavalinTest.test(server, (server, client) -> {
        driver.get(client.getOrigin() + "/register");

        String uniqueUsername = "testuser" + System.currentTimeMillis();
        driver.findElement(By.name("username")).sendKeys(uniqueUsername);
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals(
            client.getOrigin() + "/login",
            driver.getCurrentUrl(),
            "Po rejestracji oczekiwano przekierowania na stronę logowania"
        );
    });
}
```

Zwróć uwagę na:
- `System.currentTimeMillis()` — generowanie unikalnej nazwy użytkownika, dzięki czemu test można uruchamiać wielokrotnie bez konfliktów.

### 2.4 Page Object Pattern (opcjonalne)

> **Uwaga:** Ta sekcja jest **opcjonalna** i raczej nie zmieści się w czasie zajęć.
> Pełny przewodnik znajduje się w **Dodatku B.6**.

Powielanie selektorów CSS w wielu testach jest kruche — zmiana jednego elementu HTML wymaga aktualizacji
wszystkich testów. **Page Object Model** to klasa reprezentująca jedną stronę: enkapsuluje lokatory i udostępnia
metody opisujące akcje użytkownika.

W projekcie startowym znajdziesz już przykład `RegisterPage` (klasa wewnętrzna w `ApplicationTests`).

**Materiały:**
- [Selenium — Page Object Model](https://www.selenium.dev/documentation/test_practices/encouraged/page_object_models/)

**Zadanie 2.4a (opcjonalne)**  
Utwórz `LoginPage` analogicznie do `RegisterPage` i napisz test logowania korzystający z Page Object.

**Zadanie 2.4b (opcjonalne)**  
Napisz test sprawdzający, że po zalogowaniu strona `/members` jest dostępna i zawiera nazwę
zalogowanego użytkownika.

---

## Podsumowanie

Na laboratorium przekształciliśmy monolityczną klasę `JavalinApp` w bardziej modularną strukturę.
Kluczowe wnioski:

- **Podział na kontrolery** sprawia, że każda klasa ma jedną odpowiedzialność — łatwiej ją czytać, testować i modyfikować.
- **Obsługa błędów przez wyjątki** (`UnauthorizedResponse`, `config.routes.exception`) centralizuje logikę — jeden handler dla wielu ścieżek.
- **Zdarzenia cyklu życia** pozwalają śledzić stan serwera i wypisywać diagnostykę.
- **Testy Selenium** weryfikują aplikację z perspektywy przeglądarki — pozwalają wykryć błędy, których nie ujawniają testy jednostkowe.
- **Page Object** eliminuje powielanie selektorów i sprawia, że testy są odporniejsze na zmiany HTML.

**Git:** Po zakończeniu zajęć wykonaj commit z komunikatem: *„Lab 8 complete: controllers, error handling, Selenium tests"*.

---

## Dodatek A: Pozostałe funkcje rozszerzonej aplikacji

> Ten dodatek opisuje funkcje, które możesz zaimplementować samodzielnie
> po zajęciach lub traktować jako materiał referencyjny.

### A.1 Walidacja danych wejściowych z Javalin 7

Javalin 7 zmienił API walidacji — metoda `.get()` zwraca teraz wartość nullable, a `.required().get()` gwarantuje
wartość niepustą. Nieudana walidacja rzuca `ValidationException`, który można przechwycić centralnie.

**Materiały:**
- [Javalin 7 Migration Guide — Validation API now returns nullable types](https://javalin.io/migration-guide-javalin-6-to-7)
- [Javalin Documentation — Validation](https://javalin.io/documentation#validation)

```java
// Javalin 7 — wymagana wartość
String username = ctx.formParamAsClass("username", String.class)
    .check(s -> !s.isBlank(), "Nazwa użytkownika nie może być pusta")
    .required()
    .get();

// Handler walidacji w bloku konfiguracyjnym:
config.routes.exception(ValidationException.class, (e, ctx) ->
    ctx.redirect("/register?error=validation")
);
```

Porównaj z poprzednim podejściem ręcznego sprawdzania `if (username == null || username.isBlank())`.
Walidator Javalina wyraźnie opisuje **co** jest wymagane i **dlaczego** wartość jest niepoprawna.

### A.2 Zróżnicowane strony błędów

Zamiast jednego szablonu dla wszystkich błędów, można zarejestrować osobne handlery dla różnych kodów:

```java
config.routes.error(404, ctx ->
    ctx.render("error.html", Map.of("code", "404", "message", "Strona nie istnieje"))
);
config.routes.error(403, ctx ->
    ctx.render("error.html", Map.of("code", "403", "message", "Brak dostępu"))
);
config.routes.error(500, ctx ->
    ctx.render("error.html", Map.of("code", "500", "message", "Błąd serwera"))
);
```

### A.3 Rozszerzenie `requireLogin` na wiele ścieżek

Metodę `requireLogin` można wywołać z dowolnego handlera. Przy większej liczbie chronionych ścieżek
warto rozważyć użycie `config.routes.before(path, handler)` — handlera wykonywanego przed każdym
żądaniem pasującym do wzorca:

```java
// Wykonaj requireLogin przed każdym żądaniem do /members/*
config.routes.before("/members/*", ctx -> MemberController.requireLogin(ctx));
```

**Materiały:**
- [Javalin Documentation — Before handlers](https://javalin.io/documentation#before-handlers)

---

## Dodatek B: Selenium — szczegółowy przewodnik

### B.1 Architektura testów Selenium

```
Test JUnit
    └── JavalinTest.test()        — uruchamia serwer na losowym porcie 
            └── WebDriver         — steruje przeglądarką
                    └── Przeglądarka (Firefox/Chrome) — wykonuje akcje
```

`JavalinTest.test()` zapewnia izolację testów — każdy test dostaje świeży serwer, co eliminuje konflikty między testami.

**Materiały:**
- [Javalin Testing Documentation](https://javalin.io/documentation#testing)
- [Selenium WebDriver — Getting Started](https://www.selenium.dev/documentation/webdriver/getting_started/)
- [WebDriverManager — Dokumentacja](https://bonigarcia.dev/webdrivermanager/)

### B.2 Podstawowe operacje WebDriver

```java
// Nawigacja
driver.get("http://localhost:8089/");
driver.navigate().back();
driver.navigate().refresh();

// Znajdowanie elementów
WebElement button = driver.findElement(By.cssSelector("button[type='submit']"));
WebElement input  = driver.findElement(By.name("username"));
WebElement header = driver.findElement(By.tagName("h1"));
WebElement byId   = driver.findElement(By.id("error-message"));

// Interakcje
input.sendKeys("jan");           // wpisz tekst
input.clear();                   // wyczyść pole
button.click();                  // kliknij
String text = header.getText();  // odczytaj tekst

// Odczyt stanu
String url   = driver.getCurrentUrl();
String title = driver.getTitle();
String body  = driver.findElement(By.tagName("body")).getText();
```

**Materiały:**
- [Selenium — Finding Web Elements](https://www.selenium.dev/documentation/webdriver/elements/finders/)
- [Selenium — Web Element Interactions](https://www.selenium.dev/documentation/webdriver/elements/interactions/)

### B.3 Czekanie na elementy

Przeglądarki ładują strony asynchronicznie. Jeśli test szuka elementu zanim strona się załaduje, rzuca
`NoSuchElementException`. Rozwiązanie: **jawne czekanie** (`WebDriverWait`).

```java
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

// Czekaj, aż element będzie widoczny
WebElement element = wait.until(
    ExpectedConditions.visibilityOfElementLocated(By.id("error-message"))
);

// Czekaj, aż URL będzie zawierał wskazany fragment
wait.until(ExpectedConditions.urlContains("/login"));
```

**Unikaj** `Thread.sleep()` — jest wolny i kruchy. Jawne czekanie kończy się natychmiast po spełnieniu warunku.

**Materiały:**
- [Selenium — Waits](https://www.selenium.dev/documentation/webdriver/waits/)

### B.4 Tryb headless vs. tryb okienkowy

W testach CI (np. GitHub Actions) przeglądarka nie ma dostępu do ekranu — wymagany jest tryb headless.
Podczas debugowania warto wyłączyć `--headless`, żeby zobaczyć, co robi test:

```java
FirefoxOptions options = new FirefoxOptions();
// options.addArguments("--headless");  // zakomentuj podczas debugowania
options.addArguments("--disable-gpu");
driver = new FirefoxDriver(options);
```

### B.5 Debugowanie nieudanych testów

Gdy test nie przechodzi, zrzut ekranu pomaga zrozumieć, co przeglądarka „widziała”:

```java
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import java.io.File;
import org.apache.commons.io.FileUtils;

// W @AfterEach lub w catch:
File screenshot = ((TakesScreenshot) driver)
    .getScreenshotAs(OutputType.FILE);
FileUtils.copyFile(screenshot, new File("target/screenshot.png"));
```

**Materiały:**
- [Selenium — Taking Screenshots](https://www.selenium.dev/documentation/webdriver/interactions/windows/#takescreenshot)

### B.6 Page Object Pattern — szczegółowo

Page Object Model to klasa Javy reprezentująca jedną stronę lub komponent UI. Enkapsuluje:
- **Lokatory** (`@FindBy`) — jak znaleźć elementy
- **Akcje** — co użytkownik może zrobić na tej stronie
- **Asercje** — opcjonalnie, co strona powinna zawierać

W projekcie startowym znajdziesz przykład `RegisterPage`. Oto ogólny wzorzec na `LoginPage`:

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage {
    private final WebDriver driver;

    @FindBy(name = "username")
    private WebElement usernameField;

    @FindBy(name = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(css = ".error")
    private WebElement errorMessage;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void login(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        submitButton.click();
    }

    public boolean hasError() {
        try {
            return errorMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getErrorText() {
        return errorMessage.getText();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}

// Użycie w teście:
@Test
public void testLoginWithInvalidCredentials() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/login");

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("nieistniejacy", "bledne_haslo");

        assertTrue(
            loginPage.getCurrentUrl().contains("/login"),
            "Nieudane logowanie powinno pozostać na stronie /login"
        );
    });
}
```

**Zalety Page Object Model:**
- Zmiana selektora CSS wymaga edycji tylko jednej klasy
- Testy opisują *co* robi użytkownik, a nie *jak* jest to technicznie zrealizowane
- Łatwiejsze ponowne użycie między testami

**Materiały:**
- [Selenium — Page Object Models](https://www.selenium.dev/documentation/test_practices/encouraged/page_object_models/)
- [Selenium — PageFactory](https://www.selenium.dev/documentation/webdriver/support_features/page_factory/)

## Dodatek C: Rozwiązania zadań

### Ćwiczenie 1.1a: Wyodrębnienie PageController

#### Kod: `PageController.java`

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;

class PageController {
    void home(Context ctx) {
        ctx.render("home.html");
    }

    void about(Context ctx) {
        ctx.render("about.html");
    }

    void contact(Context ctx) {
        ctx.render("contact.html");
    }
}
```

#### Kod: Zaktualizowana część `JavalinApp.java` (blok routes)

```java
var pages = new PageController();

config.routes.get("/",        pages::home);
config.routes.get("/about",   pages::about);
config.routes.get("/contact", pages::contact);
```

#### Status przejścia
- ✓ Aplikacja uruchamia się bez zmian w zachowaniu
- ✓ Strony główna, o aplikacji i kontakt wyświetlają się prawidłowo
- ✓ `JavalinApp` ma mniej kodu — zaczyna przypominać klasę konfiguracyjną

---

### Ćwiczenie 1.1b: Wyodrębnienie AuthController

#### Kod: `AuthController.java`

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.Map;

class AuthController {
    private final UserRepository users;

    AuthController(UserRepository users) {
        this.users = users;
    }

    void showRegister(Context ctx) {
        ctx.render("register.html", errorModel(ctx.queryParam("error")));
    }

    void handleRegister(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            ctx.redirect("/register?error=empty_fields");
            return;
        }
        if (users.existsByUsername(username)) {
            ctx.redirect("/register?error=username_taken");
            return;
        }

        users.add(new User(username, password));
        ctx.redirect("/login");
    }

    void showLogin(Context ctx) {
        ctx.render("login.html", errorModel(ctx.queryParam("error")));
    }

    void handleLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (users.authenticate(username, password)) {
            ctx.sessionAttribute("user", username);
            ctx.redirect("/");
        } else {
            ctx.redirect("/login?error=invalid_credentials");
        }
    }

    void handleLogout(Context ctx) {
        ctx.sessionAttribute("user", null);
        ctx.redirect("/");
    }

    private static Map<String, Object> errorModel(String error) {
        return error != null ? Map.of("error", error) : Map.of();
    }
}
```

#### Kod: Zaktualizowana konfiguracja routes w `JavalinApp`

```java
var auth = new AuthController(users);

config.routes.get("/register",  auth::showRegister);
config.routes.post("/register", auth::handleRegister);
config.routes.get("/login",     auth::showLogin);
config.routes.post("/login",    auth::handleLogin);
config.routes.get("/logout",    auth::handleLogout);
```

#### Zmiana w `JavalinApp.java`
Usuń dawne prywatne metody `showRegister`, `handleRegister`, `showLogin`, `handleLogin`, `handleLogout`.

#### Status przejścia
- ✓ Rejestracja i logowanie działają jak poprzednio
- ✓ `AuthController` zawiera całą logikę autentykacji
- ✓ Zależność od `UserRepository` jest jawna (wstrzykiwana przez konstruktor)

---

### Ćwiczenie 1.1c: Wyodrębnienie MemberController

#### Kod: `MemberController.java`

```java
package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.Map;

class MemberController {
    private final UserRepository users;

    MemberController(UserRepository users) {
        this.users = users;
    }

    void showMembers(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            ctx.status(403).result("Forbidden: login required");
            return;
        }
        ctx.render("members.html", Map.of("users", users.allUsernames()));
    }
}
```

#### Kod: Zaktualizowana konfiguracja w `JavalinApp`

```java
var members = new MemberController(users);

config.routes.get("/members", members::showMembers);
```

#### Zmiana w `JavalinApp.java`
Usuń metodę `showMembers` z `JavalinApp`.

#### Status przejścia
- ✓ Lista użytkowników (`/members`) wyświetla się po zalogowaniu
- ✓ Brak zalogowania powoduje błąd 403
- ✓ Wszystkie handlery logiki biznesowej wyodrębnione z `JavalinApp`
- ✓ `JavalinApp` zawiera teraz głównie konfigurację infrastruktury

---

### Ćwiczenie 1.2a: Szablon błędu `error.html`

#### Plik: `src/main/resources/templates/error.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="'Błąd ' + ${code}">Błąd</title>
    <link rel="stylesheet" th:href="@{/style/style.css}">
</head>
<body>
    <header>
        <h1>Javalin App</h1>
        <nav>
            <a href="/">Strona główna</a>
            <a href="/about">O aplikacji</a>
            <a href="/contact">Kontakt</a>
        </nav>
    </header>

    <main>
        <section class="error-container">
            <h1 th:text="'Błąd ' + ${code}">Błąd</h1>
            <p th:text="${message}">Coś poszło nie tak.</p>
            <a href="/" class="btn">Powrót na stronę główną</a>
        </section>
    </main>

    <footer>
        <p>&copy; 2026 Javalin App. Wszystkie prawa zastrzeżone.</p>
    </footer>
</body>
</html>
```

#### Status przejścia
- ✓ Szablon gotowy do użycia w handlerach błędów

---

### Ćwiczenie 1.2b: Handlery błędów w `JavalinApp`

#### Kod: Zaktualizowany blok konfiguracyjny w `JavalinApp.create()`

```java
// ── Error handlers ──
config.routes.error(404, ctx ->
    ctx.render("error.html", Map.of(
        "code",    "404",
        "message", "Nie znaleziono strony: " + ctx.path()
    ))
);

config.routes.exception(Exception.class, (e, ctx) -> {
    e.printStackTrace();
    ctx.status(500).render("error.html", Map.of(
        "code",    "500",
        "message", "Wewnętrzny błąd serwera"
    ));
});
```

#### Gdzie umieścić
Te handlery powinny być umieszczone w bloku `Javalin.create(config -> { ... })`
**przed** konfiguracją routes, ale po skonfigurowaniu Thymeleaf.

#### Status przejścia
- ✓ Wpisanie nieistniejącego URL wyświetla własną stronę 404 (nie Jetty)
- ✓ Wyrzucenie wyjątku gdziekolwiek w aplikacji wyświetla stronę 500
- ✓ Stack trace wyjątku jest drukowany do konsoli dla debugowania

---

### Ćwiczenie 1.3a: Metoda `requireLogin` w `MemberController`

#### Kod: Zaktualizowany `MemberController.java`

```java
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
        requireLogin(ctx);  // ← Nowe podejście
        ctx.render("members.html", Map.of("users", users.allUsernames()));
    }

    static void requireLogin(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            throw new UnauthorizedResponse("Login required");
        }
    }
}
```

#### Import wymagany
```java
import io.javalin.http.UnauthorizedResponse;
```

#### Status przejścia
- ✓ `showMembers` teraz rzuca `UnauthorizedResponse` zamiast zwracać 403
- ✓ Metoda `requireLogin` może być ponownie używana w wielu handlerach
- ✓ Logika kontroli dostępu jest scentralizowana

---

### Ćwiczenie 1.3b: Handler dla `UnauthorizedResponse`

#### Kod: Zaktualizowany blok konfiguracyjny w `JavalinApp`

```java
// ── Error handlers ──
config.routes.exception(UnauthorizedResponse.class, (e, ctx) ->
    ctx.redirect("/login?error=login_required")
);

config.routes.error(404, ctx ->
    ctx.render("error.html", Map.of(
        "code",    "404",
        "message", "Nie znaleziono strony: " + ctx.path()
    ))
);

config.routes.exception(Exception.class, (e, ctx) -> {
    e.printStackTrace();
    ctx.status(500).render("error.html", Map.of(
        "code",    "500",
        "message", "Wewnętrzny błąd serwera"
    ));
});
```

#### Ważna kolejność
`UnauthorizedResponse` musi być obsługiwany **przed** ogólnym `Exception.class`,
bo wyjątki są przechwytywane w kolejności rejestracji.

#### Status przejścia
- ✓ Wejście na `/members` bez logowania przekierowuje na `/login?error=login_required`
- ✓ Inne wyjątki są obsługiwane przez ogólny handler

---

### Ćwiczenie 1.4a: Konfiguracja portu w bloku `config`

#### Kod: Zaktualizowana metoda `JavalinApp`

```java
class JavalinApp {
    private final int port;
    private final Javalin app;
    private final UserRepository users = new UserRepository();

    JavalinApp(int port) {
        this.port = port;
        this.app = Javalin.create(config -> {
            // ── Port configuration (NEW) ──
            config.jetty.port = port;

            // ── Infrastructure ──
            config.staticFiles.add(sf -> {
                sf.hostedPath = "/img";
                sf.directory  = "/www/static/images";
            });
            config.staticFiles.add(sf -> {
                sf.hostedPath = "/style";
                sf.directory  = "/www/static/css";
            });
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinThymeleaf(buildThymeleaf()));

            // ── Routes ──
            // ... (pozostała konfiguracja)
        });
    }

    // Metoda start bez argumentu:
    void start() {
        app.start();  // ← Port już skonfigurowany powyżej
    }

    // ... (reszta kodu)
}
```

#### Zmiana w `Main.java`

```java
public class Main {
    public static void main(String[] args) {
        var app = new JavalinApp(8089);
        app.setupAdminAccount("admin", "admin");
        app.start();  // ← Brak argumentu
    }
}
```

#### Status przejścia
- ✓ Port jest konfigurowany przed uruchomieniem serwera (Javalin 7 style)
- ✓ `start()` nie przyjmuje argumentów

---

### Ćwiczenie 1.4b: Zdarzenia cyklu życia serwera

#### Kod: Zaktualizowany blok konfiguracyjny w `JavalinApp`

```java
this.app = Javalin.create(config -> {
    // ── Port configuration ──
    config.jetty.port = port;

    // ── Lifecycle events ──
    config.events.serverStarting(() ->
        System.out.println("Uruchamianie serwera na porcie " + port + "...")
    );
    config.events.serverStarted(() ->
        System.out.println("Serwer gotowy: http://localhost:" + port)
    );

    // ── Infrastructure & Routes ──
    // ... (reszta konfiguracji)
});
```

#### Spodziewany output w konsoli
```
Uruchamianie serwera na porcie 8089...
Serwer gotowy: http://localhost:8089
```

#### Status przejścia
- ✓ Komunikat o uruchomieniu serwera pojawia się w konsoli
- ✓ Zdarzenia cyklu życia są konfigurowane w bloku `config`

---

### Ćwiczenie 2.3a: Uruchomienie gotowych testów

#### Polecenie
```bash
mvn test
```

#### Oczekiwane wyniki
```
ApplicationTests > testMainPageTitle() PASSED
ApplicationTests > testMembersPageAccessDenied() PASSED
ApplicationTests > testLoginWithValidCredentials() PASSED
ApplicationTests > testRegistrationFlow() PASSED
ApplicationTests > testRegisterPageTitle() PASSED
```

#### Status przejścia
- ✓ Wszystkie 5 testów powinno przejść
- ✓ WebDriver pobiera się automatycznie (WebDriverManager)
- ✓ Każdy test jest izolowany dzięki `JavalinTest.test(...)`

---

### Ćwiczenie 2.3b: Test tytułu strony głównej

#### Kod: Dodać do `ApplicationTests.java`

```java
@Test
public void testMainPageTitle() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/");
        String title = driver.getTitle();
        assertEquals("Javalin App", title,
            "Tytuł strony głównej niezgodny z oczekiwanym");
    });
}
```

#### Uwagi
- Test jest już w kodzie startowym
- Aby przeszedł, plik `home.html` musi zawierać `<title>Javalin App</title>`

#### Status przejścia
- ✓ Test przechodzi

---

### Ćwiczenie 2.3c: Test logowania ze zwalidowanymi danymi

#### Kod: Dodać do `ApplicationTests.java`

```java
@Test
public void testLoginWithValidCredentials() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/login");

        driver.findElement(By.name("username")).sendKeys(admin.getUsername());
        driver.findElement(By.name("password")).sendKeys(admin.getPassword());
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals(
            client.getOrigin() + "/",
            driver.getCurrentUrl(),
            "Po zalogowaniu oczekiwano przekierowania na stronę główną"
        );
    });
}
```

#### Uwagi
- Test jest już w kodzie startowym
- Używa pola `admin`, które jest przygotowywane w konstruktorze `SeleniumTestBase`

#### Status przejścia
- ✓ Test przechodzi

---

### Ćwiczenie 2.3d: Test pełnego przepływu rejestracji

#### Kod: Dodać do `ApplicationTests.java`

```java
@Test
public void testRegistrationFlow() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/register");

        String uniqueUsername = "testuser" + System.currentTimeMillis();
        driver.findElement(By.name("username")).sendKeys(uniqueUsername);
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals(
            client.getOrigin() + "/login",
            driver.getCurrentUrl(),
            "Po rejestracji oczekiwano przekierowania na stronę logowania"
        );
    });
}
```

#### Uwagi
- `System.currentTimeMillis()` generuje unikalną nazwa użytkownika — testy mogą być uruchamiane wielokrotnie bez kolizji
- Test jest już w kodzie startowym

#### Status przejścia
- ✓ Test przechodzi

---

### Ćwiczenie 2.4a: Page Object `LoginPage`

#### Kod: Dodać do `ApplicationTests.java` lub do oddzielnego pliku

```java
public static class LoginPage {
    private WebDriver driver;

    @FindBy(name = "username")
    private WebElement usernameField;

    @FindBy(name = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(css = ".error")
    private WebElement errorMessage;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void login(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        submitButton.click();
    }

    public boolean hasError() {
        try {
            return errorMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getErrorText() {
        return errorMessage.getText();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
```

#### Import wymagany
```java
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.NoSuchElementException;
```

#### Status przejścia
- ✓ Klasa `LoginPage` enkapsuluje logikę strony logowania
- ✓ Wielokrotnie używana w testach (patrz ćwiczenie 2.4b)

---

### Ćwiczenie 2.4b: Test logowania z użyciem Page Object

#### Kod: Dodać do `ApplicationTests.java`

```java
@Test
public void testLoginWithPageObject() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/login");

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(admin.getUsername(), admin.getPassword());

        assertEquals(
            client.getOrigin() + "/",
            loginPage.getCurrentUrl(),
            "Po zalogowaniu oczekiwano przekierowania na stronę główną"
        );
    });
}
```

#### Opcjonalnie: Test z błędnymi danymi

```java
@Test
public void testLoginWithInvalidCredentials() {
    JavalinTest.test(server, (srv, client) -> {
        driver.get(client.getOrigin() + "/login");

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("nieistniejacy", "blednehaslo");

        assertTrue(
            loginPage.getCurrentUrl().contains("/login"),
            "Nieudane logowanie powinno pozostać na stronie /login"
        );
        assertTrue(
            loginPage.hasError(),
            "Powinien być wyświetlony komunikat o błędzie"
        );
    });
}
```

#### Status przejścia
- ✓ Test używa `LoginPage` zamiast bezpośrednio manipulować Selenium API
- ✓ Kod testu jest bardziej czytelny i zrozumiały
- ✓ Zmiana selektora CSS wymaga edycji tylko `LoginPage`, nie każdego testu

---

### Końcowy stan projektu (`src/main/java`)

```
├── Main.java                 (punkt wejścia)
├── JavalinApp.java           (konfiguracja serwera, ścieżki, obsługa błędów)
├── PageController.java       (strony statyczne)
├── AuthController.java       (rejestracja, logowanie, wylogowanie)
├── MemberController.java     (lista użytkowników)
├── UserRepository.java       (przechowywanie użytkowników)
├── User.java                 (model)
└── UserType.java             (enum)
```

### Końcowy stan projektu (`src/test/java`)

```
├── SeleniumTestBase.java     (klasa bazowa — setup WebDriver)
└── ApplicationTests.java     (testy Selenium + Page Objects)
```
