# Expense Tracker API

## 🎯 Project Overview
A **full‑stack** personal finance manager built with **Spring Boot 4.1**, **Java 21**, **MySQL 8** and **JWT** authentication.  It demonstrates:
- Clean JPA entity design with Lombok and HikariCP
- Secure registration & login with BCrypt & JWT
- CRUD for categories, transactions and monthly budgets
- Dynamic filtering, pagination & sorting
- AI‑driven monthly analysis using **Google Gemini**
- Multi‑tenant data isolation per user

The codebase is intentionally **compact** for a final‑year student portfolio, yet it contains many production‑ready patterns that recruiters love to see.

---

## ✨ Highlights that impress recruiters
| Feature | Why it matters |
|---------|----------------|
| **README with badges** | Shows you understand documentation & self‑promotion. |
| **CI pipeline (GitHub Actions)** | Automated build, unit tests, code‑coverage and static analysis. |
| **Docker & Docker‑Compose** | Guarantees the app runs the same locally and in production. |
| **OpenAPI (SpringDoc) + versioning** | Gives a ready‑to‑use API spec for front‑ends. |
| **Security hardening** (CORS, CSRF disabled, refresh token, rate limiting) | Demonstrates awareness of OWASP basics. |
| **Internationalisation (i18n)** | Shows you can build multi‑language apps. |
| **Cache layer (Redis) for Gemini reports** | Improves latency and reduces external‑API cost. |
| **Test coverage ≥ 80 %** (unit + integration) | Evidence of test‑driven development. |
| **Static analysis (SpotBugs, Checkstyle)** | Guarantees clean, maintainable code. |

---

## 📦 Getting Started
```bash
# clone the repo
git clone https://github.com/your‑username/Expense-Tracker.git
cd Expense-Tracker/Expense-Tracker

# copy example env file and adjust values
cp .env.example .env   # set DB credentials, JWT secret, Gemini API key

# start everything with Docker Compose (MySQL + Redis + the app)
docker compose up -d
```
The API will be available at `http://localhost:8080/api/v1/`.

---

## 🛠️ Development
```bash
# run the app locally (requires JDK 21)
./mvnw spring-boot:run

# run tests with coverage report
./mvnw verify

# open the generated JaCoCo HTML report
open target/site/jacoco/index.html
```

---

## 📄 API Documentation
Swagger UI is exposed at **`http://localhost:8080/swagger-ui.html`** thanks to SpringDoc OpenAPI.

---

## 🚀 CI / CD (GitHub Actions)
*The workflow files are in `.github/workflows/`.*  They automatically:
- compile the project
- run unit & integration tests (with Testcontainers)
- generate a JaCoCo coverage badge
- run SpotBugs, Checkstyle and PMD
- build a Docker image and push it to GitHub Packages on every tag.

---

## 📚 License
This project is released under the **MIT License** – feel free to fork, extend, or use it as a reference.

