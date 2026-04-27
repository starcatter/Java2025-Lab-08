# Laboratorium 9: Wstrzykiwanie zależności (DI) w Javie z Google Guice

## Informacje organizacyjne

**Data:** 28.04.2026, 8:00–9:30  
**Temat:** Wstrzykiwanie zależności (Dependency Injection, DI), kontener IoC, podstawy Google Guice — moduły, bindingi, wstrzykiwanie przez konstruktor, cykl życia obiektów.

**Repozytorium:**  
> **https://github.com/starcatter/Java2025-Lab-08**

Projekt startowy to rozszerzona wersja aplikacji z Lab 8 — sklep internetowy z rejestracją, logowaniem, katalogiem produktów, koszykiem, składaniem zamówień i historią zamówień.

Na zajęciach wprowadzamy Google Guice i krok po kroku przenosimy ręczne tworzenie obiektów (`new`) z klasy `JavalinApp` do kontenera DI.

## Struktura zajęć

- **8:00–8:15** — Wejściówka (3 pytania): powtórka z Lab 8
- **8:15–8:40** — Wprowadzenie: DI, IoC, warstwy aplikacji, krótkie ćwiczenie z Guice
- **8:40–9:05** — Część 1: dodanie Guice, konfiguracja Mavena, `UserRepository`, `AppModule`, `Main`
- **9:05–9:20** — Część 2: repozytoria i serwisy jako zależności zarządzane przez Guice
- **9:20–9:30** — Część 3: kontrolery — przykład, omówienie, zadanie domowe

## Uwagi techniczne

- **JDK 17+**, Maven — po sklonowaniu repozytorium wykonaj `mvn clean package`.
- Guice dodajemy jako zależność Mavena w części praktycznej.
- Po każdym etapie refaktoryzacji uruchamiaj `mvn test`, aby upewnić się, że aplikacja nadal działa poprawnie.
- Szablony HTML i pliki CSS **nie wymagają zmian** — pracujemy wyłącznie nad architekturą kodu Javy.

**Materiały referencyjne:**
- [Google Guice — GitHub](https://github.com/google/guice)
- [Guice — Getting Started](https://github.com/google/guice/wiki/GettingStarted)
- [Guice — Bindings](https://github.com/google/guice/wiki/Bindings)
- [Guice — Injections](https://github.com/google/guice/wiki/Injections)
- [Guice — Scopes](https://github.com/google/guice/wiki/Scopes)
- [JSR 330: `javax.inject`](https://javax-inject.github.io/javax-inject/)

---

## Wejściówka (8:00–8:15)

> **Czas:** 15 minut  

---

## Wprowadzenie teoretyczne (8:15–8:40): Dependency Injection — po co i gdzie w projekcie

### 1. Problem: ręczne składanie grafu obiektów

W projekcie startowym klasa `JavalinApp` tworzy większość obiektów aplikacji ręcznie:

```java
var productRepo  = new ProductRepository();
var cartService  = new CartService(users);
var orderService = new OrderService(users);

var pages    = new PageController();
var auth     = new AuthController(users);
var members  = new MemberController(users);
var products = new ProductController(productRepo, cartService);
var cart     = new CartController(cartService, productRepo);
var checkout = new CheckoutController(cartService, orderService, productRepo);
var orders   = new OrderController(orderService, productRepo);
```

Ten fragment kodu nie zawiera logiki biznesowej, ale odpowiada za utworzenie i połączenie wielu obiektów. Im większa aplikacja, tym bardziej rozrasta się taki blok inicjalizacji. Dodanie nowej zależności, np. `EmailService`, wymaga zmian w kilku miejscach: w konstruktorze klasy, która tej zależności potrzebuje, w `JavalinApp`, często również w testach.

To jest problem, który rozwiązuje **Dependency Injection** wspierane przez kontener.

### 2. Warstwy aplikacji: repozytoria, serwisy, kontrolery

W projekcie sklepu występują trzy główne rodzaje klas:

| Warstwa | Przykłady | Odpowiedzialność |
|---|---|---|
| **Repozytoria** | `UserRepository`, `ProductRepository` | Przechowywanie i wyszukiwanie danych. W tym projekcie dane są w pamięci, ale w większej aplikacji byłaby tu baza danych. |
| **Serwisy** | `CartService`, `OrderService` | Logika aplikacyjna: operacje na koszyku, składanie zamówień, koordynacja repozytoriów. Serwis nie powinien znać szczegółów HTTP. |
| **Kontrolery** | `AuthController`, `ProductController`, `CartController` | Warstwa HTTP: odczyt danych z `Context`, wywołanie serwisów/repozytoriów, renderowanie szablonu lub przekierowanie. |

`JavalinApp` powinien docelowo zajmować się głównie konfiguracją techniczną:
- konfiguracją Javalina,
- rejestracją ścieżek,
- obsługą błędów,
- plikami statycznymi,
- silnikiem szablonów.

Nie powinien ręcznie składać całego grafu zależności aplikacji.

### 3. DI i IoC — podstawowe pojęcia

**Dependency Injection (DI)** — wstrzykiwanie zależności. Klasa nie tworzy swoich zależności przez `new`, tylko otrzymuje je z zewnątrz, zwykle przez konstruktor.

Przykład z Lab 8:

```java
public class AuthController {
    private final UserRepository users;

    public AuthController(UserRepository users) {
        this.users = users;
    }
}
```

To już jest DI przez konstruktor. Problem polega na tym, że dotąd sami tworzyliśmy `UserRepository` i ręcznie przekazywaliśmy go do kontrolera.

**Inversion of Control (IoC)** — odwrócenie sterowania. Klasa deklaruje, czego potrzebuje, a zewnętrzny kontener tworzy obiekty i dostarcza zależności.

DI jest techniką, a IoC jest szerszą zasadą. Kontener DI, taki jak Guice, jest narzędziem realizującym tę zasadę.

### 4. Co daje kontener DI?

| Aspekt | Bez kontenera | Z kontenerem DI |
|---|---|---|
| Tworzenie obiektów | Ręczne `new` w `JavalinApp` i testach | Kontener tworzy obiekty automatycznie |
| Przekazywanie zależności | Ręczne przekazywanie przez konstruktory | Kontener analizuje konstruktory z `@Inject` |
| Współdzielenie instancji | Trzeba pilnować samodzielnie | `@Singleton` wymusza jedną instancję |
| Testowanie | Trzeba odtworzyć cały blok inicjalizacji | Można użyć osobnego modułu testowego |
| Podmiana implementacji | Zmiany w wielu miejscach | Zmiana bindingu w module |

Koszt: dodatkowa warstwa abstrakcji i konieczność zrozumienia działania kontenera. W zamian dostajemy bardziej uporządkowane składanie aplikacji.

### 5. Google Guice — mechanizm działania

Podstawowy przepływ pracy z Guice:

1. Oznaczamy konstruktor adnotacją `@Inject`.
2. Tworzymy moduł (`AbstractModule`) z konfiguracją bindingów.
3. Tworzymy `Injector`.
4. Pobieramy obiekt główny, np. `JavalinApp`.
5. Guice rekurencyjnie tworzy wszystkie wymagane zależności.

```text
Injector
  └── JavalinApp
        ├── UserRepository
        ├── ProductRepository
        ├── CartService
        │     └── UserRepository
        ├── OrderService
        │     └── UserRepository
        └── ...
```

Adnotacja `@Inject` może pochodzić z pakietu `com.google.inject` albo ze standardowych pakietów `javax.inject` / `jakarta.inject`. Na tym laboratorium używamy `com.google.inject.Inject`, ponieważ pracujemy bezpośrednio z Guice.

---

## Ćwiczenie wstępne: minimalny przykład Guice

> **Cel:** Zobaczyć pełny cykl: interfejs, implementacja, klasa zależna, moduł, injector.

### Zadanie A.1 — Dodanie Guice

W `pom.xml` dodaj zależność:

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>7.0.0</version>
</dependency>
```

### Uwaga: ostrzeżenie CVE w IntelliJ

Po dodaniu Guice IntelliJ może zgłosić ostrzeżenie bezpieczeństwa dotyczące biblioteki **Guava**. Guice korzysta z Guavy jako zależności przechodniej (*transitive dependency*). Jeżeli wersja Guavy pobrana przez Maven znajduje się w bazie podatności, IDE wyświetli ostrzeżenie.

Aby wymusić nowszą wersję Guavy, dodaj w `pom.xml` sekcję `<dependencyManagement>`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>33.5.0-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Jak to działa?**

`dependencyManagement` nie dodaje biblioteki do projektu samodzielnie. Ta sekcja mówi Mavenowi: „jeśli którakolwiek zależność w projekcie potrzebuje `com.google.guava:guava`, użyj tej wersji”. Guice nadal wprowadza Guavę jako zależność przechodnią, ale Maven wybiera wersję wskazaną w `dependencyManagement`.

Po zmianie odśwież projekt Maven w IntelliJ.

### Zadanie A.2 — Interfejs i implementacja

```java
package pl.edu.uksw.di;

public interface Greeter {
    String greet(String name);
}
```

```java
package pl.edu.uksw.di;

public class ConsoleGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Cześć, " + name + "!";
    }
}
```

### Zadanie A.3 — Klasa z zależnością

```java
package pl.edu.uksw.di;

import com.google.inject.Inject;

public class WelcomeService {
    private final Greeter greeter;

    @Inject
    public WelcomeService(Greeter greeter) {
        this.greeter = greeter;
    }

    public void run() {
        System.out.println(greeter.greet("Studenci"));
    }
}
```

`WelcomeService` zależy od interfejsu `Greeter`, a nie od klasy `ConsoleGreeter`.

### Zadanie A.4 — Moduł Guice

```java
package pl.edu.uksw.di;

import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Greeter.class).to(ConsoleGreeter.class);
    }
}
```

### Zadanie A.5 — Uruchomienie

```java
package pl.edu.uksw.di;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AppModule());
        WelcomeService service = injector.getInstance(WelcomeService.class);
        service.run();
    }
}
```

Wynik: `Cześć, Studenci!`

W klasie `WelcomeService` nie ma `new ConsoleGreeter()`. Obiekt został utworzony i wstrzyknięty przez Guice.

---

# Część praktyczna (8:40–9:30): Wprowadzenie Guice do aplikacji sklepu

> **Cel obowiązkowy:** przenieść tworzenie repozytoriów i serwisów z `JavalinApp` do Guice.  
> **Cel dodatkowy:** przygotować kontrolery do wstrzykiwania przez Guice.

Po każdym etapie uruchom:

```bash
mvn test
```

---

## Część 1 (8:40–9:05): Guice, `UserRepository`, `AppModule`, `Main`

### Kontekst

W `JavalinApp` istnieje pole:

```java
private final UserRepository users = new UserRepository();
```

Ten obiekt jest używany w kilku miejscach. Powinien istnieć jako jedna wspólna instancja w aplikacji, dlatego oznaczymy go jako `@Singleton`.

### Zadanie 1.1 — Przygotowanie `UserRepository`

W `UserRepository.java`:

```java
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserRepository {
    private final List<User> users = new ArrayList<>();

    @Inject
    public UserRepository() {
    }

    // reszta metod bez zmian
}
```

**Dlaczego `@Singleton`?**  
Bez tej adnotacji Guice mógłby tworzyć osobne instancje repozytorium dla różnych klas. Wtedy użytkownik zarejestrowany przez `AuthController` mógłby nie być widoczny w `CartService` lub `OrderService`.

### Zadanie 1.2 — Utworzenie `AppModule`

```java
package pl.edu.uksw.java;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class AppModule extends AbstractModule {
    private final int port;

    public AppModule(int port) {
        this.port = port;
    }

    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named("port")).to(port);

        bind(UserRepository.class);
    }
}
```

Binding `UserRepository.class` nie jest technicznie konieczny, jeśli klasa ma konstruktor `@Inject`, ale zostawiamy go w module dla czytelności konfiguracji.

### Zadanie 1.3 — Zmiana `JavalinApp`

Usuń ręczną inicjalizację:

```java
private final UserRepository users = new UserRepository();
```

Zmień konstruktor:

```java
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class JavalinApp {
    private final int port;
    private final Javalin app;
    private final UserRepository users;

    @Inject
    public JavalinApp(@Named("port") int port,
                      UserRepository users) {
        this.port = port;
        this.users = users;

        // pozostała część konstruktora na razie bez zmian
    }
}
```

Port jest prostą wartością (`int`), dlatego oznaczamy go nazwą `@Named("port")`.

### Zadanie 1.4 — Zmiana `Main`

```java
package pl.edu.uksw.java;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AppModule(8089));

        JavalinApp app = injector.getInstance(JavalinApp.class);
        app.setupAdminAccount("admin", "admin");
        app.start();
    }
}
```

**Sprawdzenie:** aplikacja powinna uruchomić się jak wcześniej. Testy powinny przechodzić.

---

## Część 2 (9:05–9:20): Repozytoria i serwisy

### Kontekst

W `JavalinApp` nadal znajdują się ręczne inicjalizacje:

```java
var productRepo  = new ProductRepository();
var cartService  = new CartService(users);
var orderService = new OrderService(users);
```

Te klasy nie powinny być tworzone przez `JavalinApp`. Repozytoria przechowują dane, a serwisy wykonują operacje aplikacyjne. Przenosimy ich tworzenie do Guice.

### Zadanie 2.1 — `ProductRepository`

```java
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProductRepository {
    private final Map<String, Product> products = new LinkedHashMap<>();

    @Inject
    public ProductRepository() {
        seed();
    }

    // reszta metod bez zmian
}
```

### Zadanie 2.2 — `CartService` i `OrderService`

```java
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CartService {
    private final UserRepository userRepo;

    @Inject
    public CartService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // reszta metod bez zmian
}
```

Analogicznie dla `OrderService`:

```java
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OrderService {
    private final UserRepository userRepo;

    @Inject
    public OrderService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // reszta metod bez zmian
}
```

Serwisy oznaczamy jako `@Singleton`, ponieważ są bezstanowe — przechowują tylko zależności, a nie dane konkretnego użytkownika.

### Zadanie 2.3 — Aktualizacja `AppModule`

```java
@Override
protected void configure() {
    bindConstant().annotatedWith(Names.named("port")).to(port);

    bind(UserRepository.class);
    bind(ProductRepository.class);
    bind(CartService.class);
    bind(OrderService.class);
}
```

### Zadanie 2.4 — Aktualizacja konstruktora `JavalinApp`

```java
@Inject
public JavalinApp(@Named("port") int port,
                  UserRepository users,
                  ProductRepository productRepo,
                  CartService cartService,
                  OrderService orderService) {
   this.port = port;
   this.users = users;

   var pages    = new PageController();
   var auth     = new AuthController(users);
   var members  = new MemberController(users);
   var products = new ProductController(productRepo, cartService);
   var cart     = new CartController(cartService, productRepo);
   var checkout = new CheckoutController(cartService, orderService, productRepo);
   var orders   = new OrderController(orderService, productRepo);

   this.app = Javalin.create(config -> {
      // konfiguracja Javalin bez zmian
   });
}
```

**Sprawdzenie:** po tej zmianie `JavalinApp` nie tworzy już ręcznie repozytoriów ani serwisów. Nadal tworzy kontrolery — to zostawiamy na zadanie dodatkowe.

---

## Część 3 (9:20–9:30): Kontrolery — przykład i dalsza refaktoryzacja

### Kontekst

Kontrolery również można tworzyć przez Guice. Wymaga to dodania `@Inject` do konstruktorów i przekazania gotowych kontrolerów do `JavalinApp`.

Na zajęciach wykonujemy przykład na jednym kontrolerze. Pełną refaktoryzację wszystkich kontrolerów zostawiamy jako zadanie domowe.

### Zadanie 3.1 — Przykład: `AuthController`

```java
import com.google.inject.Inject;

public class AuthController {
    private final UserRepository users;

    @Inject
    public AuthController(UserRepository users) {
        this.users = users;
    }

    public void showRegister(Context ctx) { /* ... */ }
    public void handleRegister(Context ctx) { /* ... */ }
    public void showLogin(Context ctx) { /* ... */ }
    public void handleLogin(Context ctx) { /* ... */ }
    public void handleLogout(Context ctx) { /* ... */ }
}
```

Jeżeli metody kontrolera są używane jako referencje do metod (`auth::showLogin`) z klasy `JavalinApp`, muszą być widoczne z tego miejsca. Najprościej ustawić je jako `public`.

### Zadanie 3.2 — Wstrzyknięcie `AuthController` do `JavalinApp`

Dodaj `AuthController` jako parametr konstruktora:

```java
@Inject
public JavalinApp(@Named("port") int port,
                  UserRepository users,
                  ProductRepository productRepo,
                  CartService cartService,
                  OrderService orderService,
                  AuthController auth) {
    this.port = port;
    this.users = users;

    var pages    = new PageController();
    var members  = new MemberController(users);
    var products = new ProductController(productRepo, cartService);
    var cart     = new CartController(cartService, productRepo);
    var checkout = new CheckoutController(cartService, orderService, productRepo);
    var orders   = new OrderController(orderService, productRepo);

    this.app = Javalin.create(config -> {
        config.routes.get("/register", auth::showRegister);
        config.routes.post("/register", auth::handleRegister);
        config.routes.get("/login", auth::showLogin);
        config.routes.post("/login", auth::handleLogin);
        config.routes.get("/logout", auth::handleLogout);

        // pozostałe ścieżki bez zmian
    });
}
```

**Sprawdzenie:** aplikacja nadal powinna działać, a testy powinny przechodzić.

---

## Zadanie domowe

Dokończ refaktoryzację kontrolerów:

1. Dodaj `@Inject` do konstruktorów:
    - `PageController`
    - `MemberController`
    - `ProductController`
    - `CartController`
    - `CheckoutController`
    - `OrderController`

2. Dodaj kontrolery do konstruktora `JavalinApp`.

3. Usuń z `JavalinApp` wszystkie wywołania `new` dotyczące klas biznesowych i kontrolerów.

4. Uruchom `mvn test`.

Docelowo `JavalinApp` powinien konfigurować Javalin, ścieżki i obsługę błędów, ale nie powinien ręcznie tworzyć repozytoriów, serwisów ani kontrolerów.

---

## Podsumowanie

Na laboratorium wprowadziliśmy Google Guice jako kontener DI:

- `@Inject` na konstruktorze informuje Guice, jak utworzyć obiekt i jakie zależności są wymagane.
- `@Singleton` zapewnia jedną współdzieloną instancję klasy w ramach kontenera.
- `AbstractModule` centralizuje konfigurację kontenera.
- `Injector` tworzy graf zależności i pozwala pobrać obiekt główny aplikacji.
- Repozytoria odpowiadają za dane, serwisy za logikę aplikacyjną, kontrolery za obsługę HTTP.

Po wykonaniu części obowiązkowej Guice zarządza repozytoriami i serwisami. Pełne przeniesienie kontrolerów do Guice jest naturalnym dalszym krokiem i przygotowuje do pracy ze Springiem, gdzie DI i IoC są podstawą całego frameworka.

**Git:** Po zakończeniu zajęć wykonaj commit:  
*„Lab 9: introduce Guice dependency injection"*