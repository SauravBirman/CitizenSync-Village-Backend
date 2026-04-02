# CitizenSync Backend Deployment Guide

This project is a Spring Boot 3.5 backend with PostgreSQL and encrypted media upload support.

## 1) Deploy The Backend

Recommended hosting options:
- Render (simple for Docker + managed PostgreSQL)
- Railway (simple UI, fast setup)
- Azure App Service / Container Apps (if you want Azure stack)

The steps below use a generic Docker deployment flow.

### Build image locally (optional check)

```powershell
docker build -t citizensync-backend ./backend
```

### Required environment variables

Set these in your hosting platform:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require&options=-c%20TimeZone=Asia/Kolkata`
- `DB_USERNAME=<db_user>`
- `DB_PASSWORD=<db_password>`
- `APP_MEDIA_ENCRYPTION_SECRET=<very-strong-random-secret>`
- `APP_CORS_ALLOWED_ORIGINS=https://<your-firebase-app-domain>,https://<your-custom-frontend-domain>`
- `MAIL_USERNAME=<smtp_user_if_needed>`
- `MAIL_PASSWORD=<smtp_password_if_needed>`
- `JPA_DDL_AUTO=update` (for initial deploy only, switch to `validate` after schema stabilizes)

If your host injects `PORT`, add:
- `SERVER_PORT=${PORT:8080}` in platform config or a startup command using the same env var.

### Health check

After deployment:

- Verify API docs: `/swagger-ui/index.html`
- Verify location API: `/api/locations/states`

## 2) Deploy The Database

Use managed PostgreSQL (recommended):
- Render PostgreSQL
- Railway PostgreSQL
- Neon PostgreSQL
- Supabase PostgreSQL

Checklist:
- Create database named `citizensync_userdetails` (or any name, update `DB_URL`).
- Enable SSL and use `sslmode=require` in JDBC URL.
- Restrict inbound access to backend only when possible.
- Turn on automated backups.
- Store credentials only in platform secrets, never in source files.

## 3) Store Images And Videos

Your current code stores encrypted files on local disk (`uploads/issues`). This is not reliable for most cloud runtimes because local filesystem can be ephemeral.

Recommended production approach:
- Keep database metadata in PostgreSQL.
- Store encrypted file objects in cloud object storage.

Good options:
- AWS S3
- Cloudflare R2
- Firebase Storage (fits your frontend stack)
- Azure Blob Storage

### Migration approach for media (minimal behavior change)

1. Keep encryption logic exactly as-is in backend.
2. Replace local file write/read with object-storage upload/download.
3. Save object key (instead of local path) in `mediaStoragePath`.
4. Keep `/api/issues/{id}/media` endpoint behavior unchanged for frontend.

### Short-term fallback

If you need immediate go-live and cannot integrate object storage this week:
- Deploy backend on a platform with persistent disk support.
- Mount persistent volume to `APP_MEDIA_UPLOAD_DIR`.
- Plan migration to object storage in next iteration.

## 4) Frontend Integration

Since frontend is on Firebase Hosting:
- Set backend base URL in frontend environment config.
- Add that Firebase domain to `APP_CORS_ALLOWED_ORIGINS`.
- Ensure backend supports HTTPS only in production.

## 5) Security Must-Do

- Rotate and remove any old hardcoded secrets.
- Use long random `APP_MEDIA_ENCRYPTION_SECRET`.
- Use least-privilege DB user.
- Enable logs and alerts in hosting platform.

