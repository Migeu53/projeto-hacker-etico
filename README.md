# Biblioteca Escolar — Desafio Hacker Ético

Projeto diferente do primeiro, feito em **Java + HTML**.

## Tema
Sistema de biblioteca escolar.

## Requisitos atendidos
- Página inicial
- Login
- Cadastro
- 3 usuários iniciais
- Área autenticada
- Perfil
- Área administrativa
- Informações restritas
- Banco H2
- Logout

## Contas
- admin@biblioteca.local / Admin123!
- leo@biblioteca.local / Leo12345!
- bia@biblioteca.local / Bia12345!

## Executar
1. Abra a pasta no IntelliJ como projeto Maven.
2. Execute `BibliotecaApplication.java`.
3. Abra `http://localhost:8082`.

Ou use: `mvn spring-boot:run`

## Laboratório
Depois do login, acesse `http://localhost:8082/lab`.

Vulnerabilidades intencionais deste exemplo:
1. IDOR na consulta de empréstimo.
2. XSS refletido na busca.
3. Broken Access Control em relatório interno.

Há uma flag escondida no laboratório.

**Não publique este projeto vulnerável na Internet.** Por padrão ele aceita conexão apenas do próprio computador (`127.0.0.1`).
