# Roteiro de correção

## IDOR
Em `/lab/loan`, verificar se o `userId` do empréstimo corresponde ao usuário da sessão. Administrador pode ter acesso global.

## XSS
Em `lab-search.html`, trocar `th:utext` por `th:text`.

## Broken Access Control
Em `/lab/internal-report`, verificar se `role == ADMIN` antes de retornar a página.

## Reteste
Repita os testes depois de corrigir e registre novas evidências.
