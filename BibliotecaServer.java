import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Biblioteca Escolar - Desafio Hacker Ético
 * ------------------------------------------------------------
 * Projeto intencionalmente compacto: 1 Java + 1 HTML + 1 CSS.
 *
 * Execução:
 *   javac --add-modules jdk.httpserver BibliotecaServer.java
 *   java --add-modules jdk.httpserver BibliotecaServer
 *
 * Acesse:
 *   http://localhost:8082
 *
 * O servidor escuta SOMENTE em 127.0.0.1 por padrão.
 * As rotas /api/lab/* contêm falhas intencionais exclusivamente
 * para a atividade acadêmica autorizada.
 */
public class BibliotecaServer {

    private static final int PORT = 8082;
    private static final String HOST = "127.0.0.1";
    private static final Path DB_FILE = Path.of("biblioteca.db");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<Integer, User> usersById = new LinkedHashMap<>();
    private static final Map<String, Integer> userIdByEmail = new HashMap<>();
    private static final Map<Integer, Loan> loansById = new LinkedHashMap<>();
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    private static int nextUserId = 1;
    private static int nextLoanId = 1;

    private record User(int id, String name, String email, String course, String role,
                        String privateNote, String salt, String passwordHash) {}

    private record Book(int id, String title, String shortTitle, String author,
                        String category) {}

    private record Loan(int id, int userId, int bookId, String status, String createdAt) {}

    private static final List<Book> BOOKS = List.of(
            new Book(1, "Java: Fundamentos", "Java", "Ana Ribeiro", "Programação"),
            new Book(2, "Redes de Computadores", "Redes", "Marcos Vieira", "Infraestrutura"),
            new Book(3, "Segurança de Aplicações Web", "WebSec", "Paula Mendes", "Segurança"),
            new Book(4, "Banco de Dados Essencial", "Dados", "Lucas Prado", "Banco de Dados"),
            new Book(5, "Algoritmos e Lógica", "Lógica", "Carla Nunes", "Programação"),
            new Book(6, "Ética e Segurança Digital", "Ética", "Felipe Costa", "Segurança")
    );

    public static void main(String[] args) throws Exception {
        loadDatabase();
        seedIfNeeded();

        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/", BibliotecaServer::handle);
        server.setExecutor(null);
        server.start();

        System.out.println("==============================================");
        System.out.println(" Biblioteca Escolar iniciada com sucesso");
        System.out.println(" http://localhost:" + PORT);
        System.out.println("==============================================");
        System.out.println("Contas de teste:");
        System.out.println(" admin@biblioteca.local / Admin123!");
        System.out.println(" leo@biblioteca.local   / Leo12345!");
        System.out.println(" bia@biblioteca.local   / Bia12345!");
        System.out.println("Ctrl+C para encerrar.");
    }

    private static void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);

            if (path.equals("/") || path.equals("/index.html")) {
                serveFile(ex, Path.of("index.html"), "text/html; charset=UTF-8");
                return;
            }
            if (path.equals("/style.css")) {
                serveFile(ex, Path.of("style.css"), "text/css; charset=UTF-8");
                return;
            }

            if (path.equals("/api/session") && method.equals("GET")) { apiSession(ex); return; }
            if (path.equals("/api/login") && method.equals("POST")) { apiLogin(ex); return; }
            if (path.equals("/api/register") && method.equals("POST")) { apiRegister(ex); return; }
            if (path.equals("/api/logout") && method.equals("POST")) { apiLogout(ex); return; }
            if (path.equals("/api/books") && method.equals("GET")) { apiBooks(ex); return; }
            if (path.equals("/api/loans") && method.equals("GET")) { apiGetLoans(ex); return; }
            if (path.equals("/api/loans") && method.equals("POST")) { apiCreateLoan(ex); return; }
            if (path.equals("/api/admin/users") && method.equals("GET")) { apiAdminUsers(ex); return; }

            // Rotas intencionalmente vulneráveis do laboratório autorizado.
            if (path.equals("/api/lab/profile") && method.equals("GET")) { apiLabProfile(ex); return; }
            if (path.equals("/api/lab/search") && method.equals("GET")) { apiLabSearch(ex); return; }
            if (path.equals("/api/lab/admin-report") && method.equals("GET")) { apiLabAdminReport(ex); return; }

            json(ex, 404, "{\"message\":\"Rota não encontrada.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 500, "{\"message\":\"Erro interno do servidor.\"}");
        }
    }

    private static void apiSession(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        if (user == null) {
            json(ex, 200, "{\"user\":null}");
            return;
        }
        json(ex, 200, "{\"user\":" + userPublicJson(user) + "}");
    }

    private static void apiLogin(HttpExchange ex) throws Exception {
        Map<String, String> body = parseJsonObject(readBody(ex));
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        String password = body.getOrDefault("password", "");

        Integer id = userIdByEmail.get(email);
        User user = id == null ? null : usersById.get(id);
        if (user == null || !verifyPassword(password, user.salt(), user.passwordHash())) {
            json(ex, 401, "{\"message\":\"E-mail ou senha inválidos.\"}");
            return;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user.id());
        ex.getResponseHeaders().add("Set-Cookie", "session=" + token + "; HttpOnly; SameSite=Lax; Path=/");
        json(ex, 200, "{\"user\":" + userPublicJson(user) + "}");
    }

    private static void apiRegister(HttpExchange ex) throws Exception {
        Map<String, String> body = parseJsonObject(readBody(ex));
        String name = body.getOrDefault("name", "").trim();
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        String course = body.getOrDefault("course", "").trim();
        String password = body.getOrDefault("password", "");

        if (name.length() < 2) {
            json(ex, 400, "{\"message\":\"Informe um nome válido.\"}"); return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            json(ex, 400, "{\"message\":\"Informe um e-mail válido.\"}"); return;
        }
        if (userIdByEmail.containsKey(email)) {
            json(ex, 409, "{\"message\":\"Este e-mail já está cadastrado.\"}"); return;
        }
        if (password.length() < 8) {
            json(ex, 400, "{\"message\":\"A senha precisa ter pelo menos 8 caracteres.\"}"); return;
        }

        String salt = randomSalt();
        User user = new User(nextUserId++, name, email,
                course.isBlank() ? "Não informado" : course,
                "USER", "Nenhuma informação privada cadastrada.",
                salt, hashPassword(password, salt));
        addUser(user);
        saveDatabase();
        json(ex, 201, "{\"message\":\"Usuário cadastrado.\"}");
    }

    private static void apiLogout(HttpExchange ex) throws IOException {
        String token = cookie(ex, "session");
        if (token != null) sessions.remove(token);
        ex.getResponseHeaders().add("Set-Cookie", "session=; Max-Age=0; HttpOnly; SameSite=Lax; Path=/");
        json(ex, 200, "{\"message\":\"Sessão encerrada.\"}");
    }

    private static void apiBooks(HttpExchange ex) throws IOException {
        StringJoiner joiner = new StringJoiner(",", "{\"books\":[", "]}");
        for (Book book : BOOKS) {
            boolean available = loansById.values().stream()
                    .noneMatch(l -> l.bookId() == book.id() && !l.status().equals("DEVOLVIDO"));
            joiner.add("{" +
                    "\"id\":" + book.id() + "," +
                    "\"title\":\"" + escJson(book.title()) + "\"," +
                    "\"short\":\"" + escJson(book.shortTitle()) + "\"," +
                    "\"author\":\"" + escJson(book.author()) + "\"," +
                    "\"category\":\"" + escJson(book.category()) + "\"," +
                    "\"available\":" + available +
                    "}");
        }
        json(ex, 200, joiner.toString());
    }

    private static void apiGetLoans(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        if (user == null) return;

        StringJoiner joiner = new StringJoiner(",", "{\"loans\":[", "]}");
        loansById.values().stream()
                .filter(l -> l.userId() == user.id())
                .sorted(Comparator.comparingInt(Loan::id).reversed())
                .forEach(loan -> {
                    Book book = bookById(loan.bookId());
                    joiner.add("{" +
                            "\"id\":" + loan.id() + "," +
                            "\"bookId\":" + loan.bookId() + "," +
                            "\"bookTitle\":\"" + escJson(book == null ? "Livro" : book.title()) + "\"," +
                            "\"status\":\"" + escJson(loan.status()) + "\"" +
                            "}");
                });
        json(ex, 200, joiner.toString());
    }

    private static void apiCreateLoan(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        if (user == null) return;

        Map<String, String> body = parseJsonObject(readBody(ex));
        int bookId;
        try { bookId = Integer.parseInt(body.getOrDefault("bookId", "0")); }
        catch (NumberFormatException e) { bookId = 0; }

        final int selectedBookId = bookId;
        Book book = bookById(selectedBookId);
        if (book == null) {
            json(ex, 404, "{\"message\":\"Livro não encontrado.\"}"); return;
        }
        boolean unavailable = loansById.values().stream()
                .anyMatch(l -> l.bookId() == selectedBookId && !l.status().equals("DEVOLVIDO"));
        if (unavailable) {
            json(ex, 409, "{\"message\":\"Este livro já está emprestado.\"}"); return;
        }

        Loan loan = new Loan(nextLoanId++, user.id(), selectedBookId, "ATIVO", LocalDateTime.now().toString());
        loansById.put(loan.id(), loan);
        saveDatabase();
        json(ex, 201, "{\"message\":\"Empréstimo realizado.\"}");
    }

    private static void apiAdminUsers(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        if (user == null) return;
        if (!"ADMIN".equals(user.role())) {
            json(ex, 403, "{\"message\":\"Acesso restrito ao administrador.\"}"); return;
        }

        StringJoiner joiner = new StringJoiner(",", "{\"users\":[", "]}");
        for (User u : usersById.values()) joiner.add(userPublicJson(u));
        json(ex, 200, joiner.toString());
    }

    /**
     * LAB 1 - IDOR intencional:
     * o endpoint verifica apenas se existe sessão, mas NÃO verifica se o ID
     * solicitado pertence ao usuário atual.
     */
    private static void apiLabProfile(HttpExchange ex) throws IOException {
        User logged = requireUser(ex);
        if (logged == null) return;

        Map<String, String> query = queryParams(ex);
        int id;
        try { id = Integer.parseInt(query.getOrDefault("id", "0")); }
        catch (NumberFormatException e) { id = 0; }

        User target = usersById.get(id);
        if (target == null) {
            json(ex, 404, "{\"message\":\"Usuário não encontrado.\"}"); return;
        }

        json(ex, 200, "{\"user\":{" +
                "\"id\":" + target.id() + "," +
                "\"name\":\"" + escJson(target.name()) + "\"," +
                "\"email\":\"" + escJson(target.email()) + "\"," +
                "\"role\":\"" + escJson(target.role()) + "\"," +
                "\"privateNote\":\"" + escJson(target.privateNote()) + "\"" +
                "}}");
    }

    /** LAB 2 - XSS refletido intencional. A interface usa innerHTML. */
    private static void apiLabSearch(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        if (user == null) return;
        String q = queryParams(ex).getOrDefault("q", "");
        json(ex, 200, "{\"query\":\"" + escJson(q) + "\"}");
    }

    /**
     * LAB 3 - Broken Access Control intencional:
     * qualquer usuário autenticado recebe um relatório que deveria exigir ADMIN.
     */
    private static void apiLabAdminReport(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        if (user == null) return;
        json(ex, 200, "{" +
                "\"title\":\"Relatório administrativo interno\"," +
                "\"flag\":\"FLAG{RELATORIO_ADMIN_EXPOSTO}\"," +
                "\"note\":\"Este recurso deveria validar o papel ADMIN antes de responder.\"" +
                "}");
    }

    private static User requireUser(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        if (user == null) {
            json(ex, 401, "{\"message\":\"Faça login para continuar.\"}");
            return null;
        }
        return user;
    }

    private static User currentUser(HttpExchange ex) {
        String token = cookie(ex, "session");
        Integer id = token == null ? null : sessions.get(token);
        return id == null ? null : usersById.get(id);
    }

    private static String userPublicJson(User user) {
        return "{" +
                "\"id\":" + user.id() + "," +
                "\"name\":\"" + escJson(user.name()) + "\"," +
                "\"email\":\"" + escJson(user.email()) + "\"," +
                "\"course\":\"" + escJson(user.course()) + "\"," +
                "\"role\":\"" + escJson(user.role()) + "\"" +
                "}";
    }

    private static Book bookById(int id) {
        return BOOKS.stream().filter(b -> b.id() == id).findFirst().orElse(null);
    }

    private static void serveFile(HttpExchange ex, Path path, String contentType) throws IOException {
        if (!Files.exists(path)) {
            byte[] msg = ("Arquivo não encontrado: " + path).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            return;
        }
        byte[] bytes = Files.readAllBytes(path);
        Headers headers = ex.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void json(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Parser JSON mínimo, suficiente para objetos simples enviados por este frontend. */
    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> out = new HashMap<>();
        Pattern p = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|(-?\\d+)|true|false|null)");
        Matcher m = p.matcher(json == null ? "" : json);
        while (m.find()) {
            String value = m.group(2) != null ? unescapeJson(m.group(2)) : (m.group(3) != null ? m.group(3) : "");
            out.put(m.group(1), value);
        }
        return out;
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private static Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = decode(kv[0]);
            String value = kv.length > 1 ? decode(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String cookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String header : headers) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) return kv[1];
            }
        }
        return null;
    }

    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String randomSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String saltB64) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltB64);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(String password, String salt, String expected) throws Exception {
        return hashPassword(password, salt).equals(expected);
    }

    private static synchronized void addUser(User user) {
        usersById.put(user.id(), user);
        userIdByEmail.put(user.email().toLowerCase(Locale.ROOT), user.id());
        nextUserId = Math.max(nextUserId, user.id() + 1);
    }

    /**
     * Persistência local simples para manter o projeto com exatamente três arquivos-fonte.
     * O arquivo biblioteca.db é gerado automaticamente em tempo de execução e não precisa
     * ser enviado ao GitHub. Ele funciona como armazenamento persistente do laboratório.
     */
    private static synchronized void saveDatabase() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(DB_FILE, StandardCharsets.UTF_8)) {
            writer.write("# BibliotecaDB v1"); writer.newLine();
            for (User u : usersById.values()) {
                writer.write(String.join("|",
                        "U", String.valueOf(u.id()), enc(u.name()), enc(u.email()), enc(u.course()),
                        enc(u.role()), enc(u.privateNote()), enc(u.salt()), enc(u.passwordHash())));
                writer.newLine();
            }
            for (Loan l : loansById.values()) {
                writer.write(String.join("|",
                        "L", String.valueOf(l.id()), String.valueOf(l.userId()), String.valueOf(l.bookId()),
                        enc(l.status()), enc(l.createdAt())));
                writer.newLine();
            }
        }
    }

    private static synchronized void loadDatabase() throws IOException {
        if (!Files.exists(DB_FILE)) return;
        for (String line : Files.readAllLines(DB_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            try {
                if (p[0].equals("U") && p.length >= 9) {
                    User u = new User(Integer.parseInt(p[1]), dec(p[2]), dec(p[3]), dec(p[4]),
                            dec(p[5]), dec(p[6]), dec(p[7]), dec(p[8]));
                    addUser(u);
                } else if (p[0].equals("L") && p.length >= 6) {
                    Loan l = new Loan(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            dec(p[4]), dec(p[5]));
                    loansById.put(l.id(), l);
                    nextLoanId = Math.max(nextLoanId, l.id() + 1);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void seedIfNeeded() throws Exception {
        if (!usersById.isEmpty()) return;

        addSeedUser("Administrador", "admin@biblioteca.local", "Admin123!", "Administração",
                "ADMIN", "FLAG{ACESSO_ADMINISTRATIVO}");
        addSeedUser("Leonardo Alves", "leo@biblioteca.local", "Leo12345!", "Desenvolvimento de Sistemas",
                "USER", "Reserva particular: Redes de Computadores.");
        addSeedUser("Beatriz Lima", "bia@biblioteca.local", "Bia12345!", "Desenvolvimento de Sistemas",
                "USER", "Reserva particular: Java: Fundamentos.");
        saveDatabase();
    }

    private static void addSeedUser(String name, String email, String password, String course,
                                    String role, String note) throws Exception {
        String salt = randomSalt();
        addUser(new User(nextUserId++, name, email, course, role, note, salt, hashPassword(password, salt)));
    }

    private static String enc(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
