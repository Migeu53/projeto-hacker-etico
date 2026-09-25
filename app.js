const defaultUsers = [
  { id: 1, name: "Administrador", email: "admin@biblioteca.local", password: "Admin123!", role: "ADMIN" },
  { id: 2, name: "Leonardo", email: "leo@biblioteca.local", password: "Leo12345!", role: "USER" },
  { id: 3, name: "Beatriz", email: "bia@biblioteca.local", password: "Bia12345!", role: "USER" }
];

const books = [
  { id: 1, title: "Dom Casmurro", author: "Machado de Assis", category: "Literatura", available: true },
  { id: 2, title: "O Cortiço", author: "Aluísio Azevedo", category: "Literatura", available: true },
  { id: 3, title: "Introdução ao Java", author: "Biblioteca Técnica", category: "Programação", available: true },
  { id: 4, title: "Redes de Computadores", author: "Coleção Técnica", category: "Tecnologia", available: true },
  { id: 5, title: "Segurança da Informação", author: "Coleção Cyber", category: "Cibersegurança", available: true },
  { id: 6, title: "Algoritmos e Lógica", author: "Coleção Acadêmica", category: "Programação", available: true }
];

const defaultLoans = [
  { userId: 2, bookId: 4, status: "EMPRESTADO" },
  { userId: 3, bookId: 3, status: "EMPRESTADO" }
];

function loadData() {
  if (!localStorage.getItem("biblioteca_users")) {
    localStorage.setItem("biblioteca_users", JSON.stringify(defaultUsers));
  }

  if (!localStorage.getItem("biblioteca_loans")) {
    localStorage.setItem("biblioteca_loans", JSON.stringify(defaultLoans));
  }
}

function getUsers() {
  return JSON.parse(localStorage.getItem("biblioteca_users") || "[]");
}

function setUsers(users) {
  localStorage.setItem("biblioteca_users", JSON.stringify(users));
}

function getLoans() {
  return JSON.parse(localStorage.getItem("biblioteca_loans") || "[]");
}

function setLoans(loans) {
  localStorage.setItem("biblioteca_loans", JSON.stringify(loans));
}

function getCurrentUser() {
  const id = Number(sessionStorage.getItem("biblioteca_session"));
  return getUsers().find(user => user.id === id) || null;
}

function setSession(user) {
  sessionStorage.setItem("biblioteca_session", user.id);
}

function clearSession() {
  sessionStorage.removeItem("biblioteca_session");
}

const views = [...document.querySelectorAll(".view")];

function showView(id) {
  views.forEach(view => view.classList.remove("active"));
  const view = document.getElementById(id);
  if (view) view.classList.add("active");
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function updateNavigation() {
  const user = getCurrentUser();
  document.getElementById("navGuest").classList.toggle("hidden", !!user);
  document.getElementById("navUser").classList.toggle("hidden", !user);
  document.getElementById("adminNavBtn").classList.toggle("hidden", !user || user.role !== "ADMIN");
}

function toast(message) {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 2400);
}

function renderCatalog(filter = "") {
  const grid = document.getElementById("catalogGrid");
  const query = filter.trim().toLowerCase();

  const filtered = books.filter(book =>
    book.title.toLowerCase().includes(query) ||
    book.author.toLowerCase().includes(query) ||
    book.category.toLowerCase().includes(query)
  );

  grid.innerHTML = filtered.map((book, index) => `
    <article class="book-card">
      <div class="book-cover" style="background: linear-gradient(145deg, hsl(${220 + index * 18} 80% 50%), hsl(${240 + index * 18} 75% 68%));">
        ${book.category}
      </div>
      <div>
        <h4>${escapeHtml(book.title)}</h4>
        <div class="book-meta">${escapeHtml(book.author)} • ${escapeHtml(book.category)}</div>
      </div>
      <div class="book-actions">
        <span class="badge">Disponível</span>
        <button class="secondary" onclick="borrowBook(${book.id})">Emprestar</button>
      </div>
    </article>
  `).join("");

  if (!filtered.length) {
    grid.innerHTML = `<div class="panel">Nenhum livro encontrado.</div>`;
  }
}

function borrowBook(bookId) {
  const user = getCurrentUser();

  if (!user) {
    toast("Faça login para realizar um empréstimo.");
    showView("loginView");
    return;
  }

  const loans = getLoans();
  const exists = loans.some(loan => loan.userId === user.id && loan.bookId === bookId);

  if (exists) {
    toast("Você já possui este livro emprestado.");
    return;
  }

  loans.push({ userId: user.id, bookId, status: "EMPRESTADO" });
  setLoans(loans);
  toast("Empréstimo realizado.");
  renderDashboard();
}

function returnBook(bookId) {
  const user = getCurrentUser();
  if (!user) return;

  const loans = getLoans().filter(loan => !(loan.userId === user.id && loan.bookId === bookId));
  setLoans(loans);
  toast("Livro devolvido.");
  renderDashboard();
}

function renderDashboard() {
  const user = getCurrentUser();
  if (!user) return;

  document.getElementById("welcomeTitle").textContent = `Olá, ${user.name}!`;
  document.getElementById("roleStat").textContent = user.role;

  const loans = getLoans().filter(loan => loan.userId === user.id);
  document.getElementById("loanCount").textContent = loans.length;

  const container = document.getElementById("myLoans");

  if (!loans.length) {
    container.innerHTML = `<div class="panel">Você ainda não possui empréstimos.</div>`;
    return;
  }

  container.innerHTML = loans.map(loan => {
    const book = books.find(b => b.id === loan.bookId);
    return `
      <article class="loan-card">
        <h4>${escapeHtml(book?.title || "Livro")}</h4>
        <p>${escapeHtml(book?.author || "")}</p>
        <span class="badge">${escapeHtml(loan.status)}</span>
        <div style="margin-top:14px">
          <button class="secondary" onclick="returnBook(${loan.bookId})">Devolver</button>
        </div>
      </article>
    `;
  }).join("");
}

function renderProfile() {
  const user = getCurrentUser();
  if (!user) return;

  document.getElementById("profileName").textContent = user.name;
  document.getElementById("profileEmail").textContent = user.email;
  document.getElementById("profileRole").textContent = user.role;
  document.getElementById("profileId").textContent = user.id;
}

function renderAdmin() {
  const user = getCurrentUser();
  if (!user || user.role !== "ADMIN") {
    toast("Área exclusiva do administrador.");
    showView("dashboardView");
    return false;
  }

  const users = getUsers();
  const loans = getLoans();

  document.getElementById("adminUsersTable").innerHTML = users.map(u => `
    <tr>
      <td>${u.id}</td>
      <td>${escapeHtml(u.name)}</td>
      <td>${escapeHtml(u.email)}</td>
      <td>${escapeHtml(u.role)}</td>
    </tr>
  `).join("");

  document.getElementById("adminLoansTable").innerHTML = loans.map(loan => {
    const loanUser = users.find(u => u.id === loan.userId);
    const book = books.find(b => b.id === loan.bookId);

    return `
      <tr>
        <td>${escapeHtml(loanUser?.name || "Desconhecido")}</td>
        <td>${escapeHtml(book?.title || "Livro")}</td>
        <td>${escapeHtml(loan.status)}</td>
      </tr>
    `;
  }).join("");

  return true;
}

function handleAction(action) {
  const user = getCurrentUser();

  switch (action) {
    case "show-home":
      showView("homeView");
      break;

    case "show-login":
      showView("loginView");
      break;

    case "show-register":
      showView("registerView");
      break;

    case "show-dashboard":
      if (!user) return showView("loginView");
      renderDashboard();
      showView("dashboardView");
      break;

    case "show-profile":
      if (!user) return showView("loginView");
      renderProfile();
      showView("profileView");
      break;

    case "show-admin":
      if (renderAdmin()) showView("adminView");
      break;

    case "show-lab":
      if (!user) return showView("loginView");
      showView("labView");
      break;

    case "logout":
      clearSession();
      updateNavigation();
      toast("Sessão encerrada.");
      showView("homeView");
      break;
  }
}

document.addEventListener("click", event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  handleAction(button.dataset.action);
});

document.getElementById("loginForm").addEventListener("submit", event => {
  event.preventDefault();

  const form = new FormData(event.currentTarget);
  const email = String(form.get("email")).trim().toLowerCase();
  const password = String(form.get("password"));

  const user = getUsers().find(u => u.email.toLowerCase() === email && u.password === password);

  if (!user) {
    toast("E-mail ou senha inválidos.");
    return;
  }

  setSession(user);
  updateNavigation();
  renderDashboard();
  toast("Login realizado.");
  showView("dashboardView");
  event.currentTarget.reset();
});

document.getElementById("registerForm").addEventListener("submit", event => {
  event.preventDefault();

  const form = new FormData(event.currentTarget);
  const name = String(form.get("name")).trim();
  const email = String(form.get("email")).trim().toLowerCase();
  const password = String(form.get("password"));

  const users = getUsers();

  if (users.some(u => u.email.toLowerCase() === email)) {
    toast("Este e-mail já está cadastrado.");
    return;
  }

  const nextId = Math.max(...users.map(u => u.id), 0) + 1;
  const newUser = { id: nextId, name, email, password, role: "USER" };

  users.push(newUser);
  setUsers(users);
  setSession(newUser);

  updateNavigation();
  renderDashboard();
  toast("Conta criada com sucesso.");
  showView("dashboardView");
  event.currentTarget.reset();
});

document.getElementById("bookSearch").addEventListener("input", event => {
  renderCatalog(event.target.value);
});

document.getElementById("labUserBtn").addEventListener("click", () => {
  const id = Number(document.getElementById("labUserId").value);
  const user = getUsers().find(u => u.id === id);
  const result = document.getElementById("labUserResult");

  if (!user) {
    result.textContent = "Usuário não encontrado.";
    return;
  }

  result.innerHTML = `
    <strong>${escapeHtml(user.name)}</strong><br>
    ${escapeHtml(user.email)}<br>
    Perfil: ${escapeHtml(user.role)}
  `;
});

document.getElementById("labSearchBtn").addEventListener("click", () => {
  const text = document.getElementById("labSearchInput").value;
  document.getElementById("labSearchResult").textContent = `Você pesquisou por: ${text}`;
});

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

loadData();
renderCatalog();
updateNavigation();

const initialUser = getCurrentUser();
if (initialUser) {
  renderDashboard();
}
