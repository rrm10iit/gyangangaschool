# Gyan Ganga School website

Public website and staff admin panel for Gyan Ganga School, Prayagraj.

| Part | Technology |
| --- | --- |
| Website and admin screens | Next.js 16, React 19, Tailwind CSS (English and Hindi, light and dark mode) |
| API | Java 21, Spring Boot 3.5, Spring Security (JWT in an HttpOnly cookie) |
| Database | PostgreSQL 16 with Flyway migrations |
| Email | Brevo transactional API |
| Hosting | Docker Compose: Caddy (automatic HTTPS), frontend, backend, database, nightly backups |

```
.
├── backend/            Spring Boot API (src/main/java/com/gyanganga/school/...)
├── frontend/           Next.js app (src/app/(public) = website, src/app/admin = admin panel)
├── Caddyfile           Reverse proxy and HTTPS
├── docker-compose.yml
└── .env.example        Settings template
```

## What is in it

**Public website (no login):** home page with hero, key numbers, announcements, facilities, videos and testimonials;
About (story, vision, mission, principal's message, management); Classes (Nursery to Class 8);
Facilities; Photo gallery with albums, categories and a lightbox; News; Contact page with map and an
admission enquiry form that emails the school through Brevo. English and Hindi toggle, dark mode,
SEO metadata, sitemap, responsive layout.

**Admin panel (`/admin`):**

| Area | Who can use it |
| --- | --- |
| Dashboard with totals and charts | Everyone (finance figures: Admin and Accountant) |
| Students and Teachers: add, edit, delete, search, photos, documents | Admin edits. Teacher and Accountant can view |
| Classes | Admin |
| Finance: fee structure, fee payments with printable PDF receipts, pending and overdue list, teacher salaries, expenses, monthly and yearly reports (Excel and PDF) | Admin and Accountant |
| Website content: page text, facilities, photo gallery, announcements, testimonials, management team, admission enquiries | Admin |
| Staff accounts and activity log (who changed what) | Admin |

Fees follow an April to March academic year. A student's yearly fee is worked out from the fee heads of their
class (monthly, quarterly, yearly or one-time) minus any concession. "Overdue" is what should have been paid
by today, "pending" is what remains for the whole year.

## Run it on any machine with Docker

1. Copy the settings file and edit it:

   ```
   cp .env.example .env
   ```

   Set at least `DB_PASSWORD`, `JWT_SECRET` (32 or more random characters) and `ADMIN_PASSWORD`.

2. Start everything:

   ```
   docker compose up -d --build
   ```

3. Open <http://localhost> (or the `HTTP_PORT` you chose) and sign in at `/admin/login` with `ADMIN_EMAIL` and
   `ADMIN_PASSWORD`. If `ADMIN_PASSWORD` was left empty, a one-time password is printed in
   `docker compose logs backend`.

With `SEED_DEMO_DATA=true` the site starts with sample students, teachers, fees, expenses and gallery photos so you
can try every screen. **Set it to `false` before the real launch**, and delete the sample records first if you
already started with `true` (the simplest way on a fresh install is `docker compose down -v`, which erases all data).

## Put it on a server with HTTPS

1. Point your domain's DNS A record at the server and open ports 80 and 443.
2. In `.env` set:

   ```
   SITE_ADDRESS=www.your-school-domain.in
   PUBLIC_URL=https://www.your-school-domain.in
   COOKIE_SECURE=true
   SEED_DEMO_DATA=false
   ```

3. `docker compose up -d --build`. Caddy gets and renews the certificate by itself.

Only the proxy is exposed to the internet. The database and the API are reachable only inside the Docker network.

## Host it from your own PC (no server needed)

To serve the site on your own domain from a Windows PC through a free Cloudflare Tunnel, with the admin area locked
behind an email code, follow [deploy/HOSTING-ON-THIS-PC.md](deploy/HOSTING-ON-THIS-PC.md). It uses
`docker-compose.tunnel.yml` on top of the normal compose file.

## Email with Brevo

1. In Brevo, create an API key (SMTP & API, API keys) and verify the sender address.
2. Put `BREVO_API_KEY` and `BREVO_SENDER_EMAIL` in `.env`.
3. Enquiries from the Contact page are saved in the admin panel (Website, Admission enquiries) and emailed to
   `ENQUIRY_NOTIFY_EMAIL`, or to the school email in Admin, Page content when that is empty. A confirmation
   goes to the parent when they give an email address.

Without a Brevo key the enquiry is still saved and shown in the admin panel.

## Backups

The `backup` service writes a database dump and a copy of the uploaded photos into `./backups` every 24 hours
and deletes files older than 14 days. Copy that folder off the server regularly. To restore a dump:

```
docker compose exec -T db pg_restore -U gyanganga -d gyanganga --clean --if-exists < backups/db-YYYYMMDD-HHMMSS.dump
```

## Security notes

- Passwords are hashed with BCrypt. The login token lives in an HttpOnly, SameSite=Strict cookie (Secure over HTTPS).
- Login is rate limited per account and per address. The enquiry form has rate limiting and a spam trap field.
- Every API route is checked by role on the server, not only hidden in the menu.
- Uploaded photos are re-encoded (metadata removed). Student documents are private and are only served to signed-in staff.
- The activity log records logins, failed logins and every create, update and delete.
- Two-factor sign-in is not built yet. The sign-in flow (`AuthControllers.AuthController.login`) is the place to add
  a TOTP step.

## Local development without Docker

Needs Java 21, Maven, Node 22 and a PostgreSQL database.

```
# backend (http://localhost:8080)
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/gyanganga DB_USER=gyanganga DB_PASSWORD=gyanganga
export JWT_SECRET=dev-only-secret-dev-only-secret-0123456789 ADMIN_PASSWORD=Admin@12345 SEED_DEMO_DATA=true
mvn spring-boot:run

# frontend (http://localhost:3000)
cd frontend
npm install
BACKEND_URL=http://localhost:8080 npm run dev
```

## Tests

Three layers. All of them run automatically on every push through GitHub Actions (`.github/workflows/ci.yml`).

| Layer | What it checks | How to run | Needs |
| --- | --- | --- | --- |
| Backend unit tests (`*Test`) | Fee calculation (monthly, quarterly, yearly heads, April to March year), amount in words on receipts, login rate limiter | `cd backend && mvn test` | Java 21 |
| Backend integration tests (`*IT`) | The real API against a real PostgreSQL: login and lockout, who may call which URL (a table of role by URL), student and teacher rules, fee ledger, dues, receipts, salaries, reports and Excel/PDF exports, enquiry spam protection, public content visibility, image and document uploads | `cd backend && mvn verify` | Java 21 and Docker |
| Browser tests (`e2e/`) | Every public and admin page, the language and dark mode toggles, the enquiry form, the gallery lightbox, phone layouts, accessibility (axe, light and dark), complete admin workflows, Teacher and Accountant restrictions, security headers | see below | Docker, Node 22 |

Browser tests run against a started stack with sample data:

```
# .env: SEED_DEMO_DATA=true, ENQUIRY_RATE_LIMIT=1000, and an ADMIN_PASSWORD you choose
docker compose up -d --build
cd e2e
npm install
npx playwright install chromium        # or set E2E_CHANNEL=msedge / chrome to use a browser you already have
E2E_BASE_URL=http://localhost E2E_ADMIN_PASSWORD=<your admin password> npx playwright test
```

`ENQUIRY_RATE_LIMIT` is 5 per hour per address in normal use. Raise it only in a test stack, otherwise repeated test
runs hit the spam limit. The tests create their own records (names start with "E2E") and remove most of them again.
Do not run the browser tests against the live school site.

On Docker 29 or newer, Testcontainers needs the Docker API version set. The backend `pom.xml` already passes
`api.version=1.44` to the integration tests, so no extra setup is needed.

## Things to update after the first start

Sign in and open **Admin, Page content**. These values are placeholders taken from the old site or guessed:
phone number, email, year founded (drives "years of service"), principal's name, photo and message, the history
text, class seat counts, and the board name (currently "UP Board").
Upload real photos under **Facilities**, **Photo gallery** and **Management team**.
