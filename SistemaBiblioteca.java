import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/*
 * Projeto educacional - Desafio Hacker Ético
 * Estrutura reduzida: 1 Java + 1 HTML + 1 CSS.
 *
 * IMPORTANTE:
 * - O servidor escuta apenas em localhost (127.0.0.1).
 * - Use somente no laboratório autorizado da atividade.
 * - Os "usuários" ficam em memória para manter exatamente 3 arquivos no projeto.
 */
public class SistemaBiblioteca {

    static class User {
        int id;
        String nome;
        String email;
        String senha;
        String perfil;
        String infoPrivada;

        User(int id, String nome, String email, String senha, String perfil, String infoPrivada) {
            this.id = id;
            this.nome = nome;
            this.email = email;
            this.senha = senha;
            this.perfil = perfil;
            this.infoPrivada = infoPrivada;
        }
    }

    static final Map<String, User> USERS = new LinkedHashMap<>();
    static final Map<String, Integer> SESSIONS = new HashMap<>();

    static {
        USERS.put("admin@biblioteca.local",
                new User(1, "Administrador", "admin@biblioteca.local",
                        "Admin123!", "ADMIN",
                        "FLAG{ACESSO_ADMINISTRATIVO}"));

        USERS.put("leo@biblioteca.local",
                new User(2, "Leonardo", "leo@biblioteca.local",
                        "Leo12345!", "USER",
                        "Empréstimo reservado: livro de Redes."));

        USERS.put("bia@biblioteca.local",
                new User(3, "Beatriz", "bia@biblioteca.local",
                        "Bia12345!", "USER",
                        "Empréstimo reservado: livro de Java."));
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8082), 0);

        server.createContext("/", SistemaBiblioteca::handleHome);
        server.createContext("/style.css", SistemaBiblioteca::handleCss);
        server.createContext("/login", SistemaBiblioteca::handleLogin);
        server.createContext("/cadastro", SistemaBiblioteca::handleCadastro);
        server.createContext("/logout", SistemaBiblioteca::handleLogout);
        server.createContext("/perfil", SistemaBiblioteca::handlePerfil);
        server.createContext("/admin", SistemaBiblioteca::handleAdmin);

        // Área de laboratório autorizada.
        server.createContext("/lab/perfil", SistemaBiblioteca::handleLabPerfil);
        server.createContext("/lab/busca", SistemaBiblioteca::handleLabBusca);

        server.setExecutor(null);
        server.start();

        System.out.println("Sistema iniciado em http://localhost:8082");
        System.out.println("Pressione Ctrl+C para encerrar.");
    }

    static void handleHome(HttpExchange ex) throws IOException {
        String html = Files.readString(Path.of("index.html"), StandardCharsets.UTF_8);
        User user = currentUser(ex);

        html = html.replace("{{MENSAGEM}}",
                user == null
                        ? "Você não está autenticado."
                        : "Olá, <strong>" + escape(user.nome) + "</strong>!");

        html = html.replace("{{CONTEUDO}}", homeContent(user));
        sendHtml(ex, html);
    }

    static String homeContent(User user) {
        if (user == null) {
            return """
                <section class="card">
                    <h2>Login</h2>
                    <form method="post" action="/login">
                        <label>E-mail</label>
                        <input type="email" name="email" required>

                        <label>Senha</label>
                        <input type="password" name="senha" required>

                        <button type="submit">Entrar</button>
                    </form>
                </section>

                <section class="card">
                    <h2>Cadastro</h2>
                    <form method="post" action="/cadastro">
                        <label>Nome</label>
                        <input type="text" name="nome" required>

                        <label>E-mail</label>
                        <input type="email" name="email" required>

                        <label>Senha</label>
                        <input type="password" name="senha" minlength="8" required>

                        <button type="submit">Cadastrar</button>
                    </form>
                </section>
                """;
        }

        String adminLink = "ADMIN".equals(user.perfil)
                ? "<a class='btn' href='/admin'>Área administrativa</a>"
                : "";

        return """
            <section class="card">
                <h2>Área autenticada</h2>
                <p>Usuário: <strong>%s</strong></p>
                <p>Perfil: <strong>%s</strong></p>

                <div class="actions">
                    <a class="btn" href="/perfil">Meu perfil</a>
                    %s
                    <a class="btn" href="/logout">Sair</a>
                </div>
            </section>

            <section class="card">
                <h2>Laboratório autorizado</h2>
                <p>Use apenas no ambiente da atividade.</p>

                <form method="get" action="/lab/perfil">
                    <label>Consultar perfil por ID</label>
                    <input type="number" name="id" min="1" required>
                    <button type="submit">Consultar</button>
                </form>

                <form method="get" action="/lab/busca">
                    <label>Busca</label>
                    <input type="text" name="q">
                    <button type="submit">Pesquisar</button>
                </form>
            </section>
            """.formatted(escape(user.nome), escape(user.perfil), adminLink);
    }

    static void handleCss(HttpExchange ex) throws IOException {
        byte[] data = Files.readAllBytes(Path.of("style.css"));
        ex.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static void handleLogin(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            redirect(ex, "/");
            return;
        }

        Map<String, String> form = readForm(ex);
        User user = USERS.get(form.getOrDefault("email", "").toLowerCase());

        if (user == null || !user.senha.equals(form.get("senha"))) {
            simplePage(ex, "Login inválido", "E-mail ou senha inválidos.", "/");
            return;
        }

        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, user.id);
        ex.getResponseHeaders().add("Set-Cookie", "session=" + token + "; HttpOnly; SameSite=Lax");
        redirect(ex, "/");
    }

    static void handleCadastro(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            redirect(ex, "/");
            return;
        }

        Map<String, String> form = readForm(ex);
        String nome = form.getOrDefault("nome", "").trim();
        String email = form.getOrDefault("email", "").trim().toLowerCase();
        String senha = form.getOrDefault("senha", "");

        if (nome.length() < 2 || !email.contains("@") || senha.length() < 8) {
            simplePage(ex, "Cadastro inválido", "Confira os dados informados.", "/");
            return;
        }

        if (USERS.containsKey(email)) {
            simplePage(ex, "Cadastro", "Este e-mail já está cadastrado.", "/");
            return;
        }

        int id = USERS.values().stream().mapToInt(u -> u.id).max().orElse(0) + 1;
        USERS.put(email, new User(
                id, nome, email, senha, "USER",
                "Nenhuma informação privada cadastrada."
        ));

        simplePage(ex, "Cadastro concluído", "Usuário criado com sucesso.", "/");
    }

    static void handleLogout(HttpExchange ex) throws IOException {
        String token = cookie(ex, "session");
        if (token != null) SESSIONS.remove(token);

        ex.getResponseHeaders().add("Set-Cookie",
                "session=; Max-Age=0; HttpOnly; SameSite=Lax");
        redirect(ex, "/");
    }

    static void handlePerfil(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        if (user == null) {
            redirect(ex, "/");
            return;
        }

        simplePage(ex, "Meu perfil", """
                <strong>ID:</strong> %d<br>
                <strong>Nome:</strong> %s<br>
                <strong>E-mail:</strong> %s<br>
                <strong>Perfil:</strong> %s<br>
                <strong>Informação privada:</strong> %s
                """.formatted(
                user.id,
                escape(user.nome),
                escape(user.email),
                escape(user.perfil),
                escape(user.infoPrivada)
        ), "/");
    }

    static void handleAdmin(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        if (user == null) {
            redirect(ex, "/");
            return;
        }

        if (!"ADMIN".equals(user.perfil)) {
            simplePage(ex, "Acesso negado",
                    "Área exclusiva do administrador.", "/");
            return;
        }

        StringBuilder table = new StringBuilder("""
            <table>
                <tr>
                    <th>ID</th><th>Nome</th><th>E-mail</th><th>Perfil</th>
                </tr>
            """);

        for (User u : USERS.values()) {
            table.append("""
                <tr>
                    <td>%d</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                    u.id,
                    escape(u.nome),
                    escape(u.email),
                    escape(u.perfil)
            ));
        }

        table.append("</table>");
        simplePage(ex, "Área administrativa", table.toString(), "/");
    }

    /*
     * Vulnerabilidade educacional: IDOR.
     * O usuário autenticado consegue consultar outro perfil alterando o ID.
     */
    static void handleLabPerfil(HttpExchange ex) throws IOException {
        User logged = currentUser(ex);
        if (logged == null) {
            redirect(ex, "/");
            return;
        }

        Map<String, String> q = queryParams(ex);
        int id;

        try {
            id = Integer.parseInt(q.getOrDefault("id", "0"));
        } catch (NumberFormatException e) {
            id = 0;
        }

        User target = USERS.values().stream()
                .filter(u -> u.id == id)
                .findFirst()
                .orElse(null);

        if (target == null) {
            simplePage(ex, "Laboratório", "Usuário não encontrado.", "/");
            return;
        }

        simplePage(ex, "Perfil consultado", """
                <strong>ID:</strong> %d<br>
                <strong>Nome:</strong> %s<br>
                <strong>E-mail:</strong> %s<br>
                <strong>Perfil:</strong> %s<br>
                <strong>Informação privada:</strong> %s
                """.formatted(
                target.id,
                escape(target.nome),
                escape(target.email),
                escape(target.perfil),
                escape(target.infoPrivada)
        ), "/");
    }

    /*
     * Vulnerabilidade educacional: XSS refletido.
     * A entrada é devolvida sem escape propositalmente.
     */
    static void handleLabBusca(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        if (user == null) {
            redirect(ex, "/");
            return;
        }

        String q = queryParams(ex).getOrDefault("q", "");
        simplePage(ex, "Resultado da busca",
                "Você pesquisou por: " + q, "/");
    }

    static User currentUser(HttpExchange ex) {
        String token = cookie(ex, "session");
        if (token == null) return null;

        Integer id = SESSIONS.get(token);
        if (id == null) return null;

        return USERS.values().stream()
                .filter(u -> u.id == id)
                .findFirst()
                .orElse(null);
    }

    static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseEncoded(body);
    }

    static Map<String, String> queryParams(HttpExchange ex) {
        String query = ex.getRequestURI().getRawQuery();
        return parseEncoded(query == null ? "" : query);
    }

    static Map<String, String> parseEncoded(String data) {
        Map<String, String> out = new HashMap<>();
        if (data == null || data.isBlank()) return out;

        for (String pair : data.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            out.put(key, value);
        }
        return out;
    }

    static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String cookie(HttpExchange ex, String name) {
        List<String> cookieHeaders = ex.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) return null;

        for (String header : cookieHeaders) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    static void simplePage(HttpExchange ex, String title, String content, String back) throws IOException {
        String html = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <link rel="stylesheet" href="/style.css">
            </head>
            <body>
                <main class="container">
                    <section class="card">
                        <h1>%s</h1>
                        <p>%s</p>
                        <a class="btn" href="%s">Voltar</a>
                    </section>
                </main>
            </body>
            </html>
            """.formatted(
                escape(title),
                escape(title),
                content,
                back
        );

        sendHtml(ex, html);
    }

    static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    static void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static String escape(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
