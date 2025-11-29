The app has a basic form used to upload files to a s3 bucket. Has some pause/resume functionality that works to some extent.
Mocking the db. and everythings on local


# 📱 Android Media Upload App + 🖥️ Node.js + TUS + S3 Backend

This repository contains:

* **Android Kotlin app** using Jetpack Compose
* **Resumable uploads** using `tus-android-client`
* **Express.js backend** using `@tus/server` + `@tus/s3-store`
* **AWS S3 storage** for uploaded media
* **Upload progress**, pause/resume, compression-ready architecture
* **Form creation** + media attachment
* **History screen** with images/video previews

---

## ✨ Features

### ▶ Android App (Jetpack Compose)

* Create a form (title + description)
* Attach images/videos
* Resumable uploads via **TUS protocol**
* Shows:

  * Upload progress per file
  * Pause / Resume
  * Pending / Completed states
  * Video playback (ExoPlayer)
  * Image viewing
* Supports future compression step
* Uses Retrofit for API calls

---

### 🖥️ Backend (Node.js + Express)

* `/forms` → Create a form
* TUS endpoint `/uploads` for resumable uploads
* Uploads stored directly into **AWS S3**
* Automatic DB record creation:

  * Upload started
  * Upload completed
* `/forms/all` → returns all forms with media + S3 URLs
* In-memory database (replaceable with SQL later)

---

## 🗂 Project Structure

```
root/
│
├── android-app/
│   ├── app/src/main/java/com/example/myapplication/
│   ├── UploadManager.kt
│   ├── CompressionManager.kt (optional)
│   ├── network/
│   ├── ui/
│   └── ...
│
└── backend/
    ├── server.js
    ├── package.json
    ├── .env.example
    └── ...
```

---

# 🚀 Getting Started

## 1. Clone the repo

```
git clone <repo-url>
cd <repo-folder>
```

---

# 🛠 Backend Setup

## 1. Install dependencies

```
cd backend
npm install
```

---

## 2. Configure environment variables

Create `.env`:

```
HOST=192.168.x.x //wifi ip if testing on a real phone.
PORT=1080

AWS_ACCESS_KEY_ID=xxxx
AWS_SECRET_ACCESS_KEY=xxxx
AWS_REGION=eu-north-1

S3_BUCKET=your-bucket-name
S3_PREFIX=uploads/
```

A template is available at `.env.example`.

---

## 3. Run the backend server

```
npm start
```

Server runs at:

```
http://HOST:PORT
```

TUS endpoint:

```
http://HOST:PORT/uploads
```

---

# 📱 Android App Setup

1. Open `android-app/` in **Android Studio**
2. Create a file:
   `AppConfig.kt`

```kotlin
object AppConfig {
    const val BASE_API = "http://192.168.x.x:1080/"
    const val TUS_ENDPOINT = "http://192.168.x.x:1080/uploads"
}
```

3. Build & run the app on a real device
   (Ensure device + backend are on same WiFi)

---

# 🧪 API Summary

## Create form

```
POST /forms
{
  "title": "...",
  "description": "..."
}
```

Response:

```
{ "id": "<formId>" }
```

---

## TUS Upload

* Endpoint: `/uploads`
* Metadata required:

  * formId
  * fileName
  * mimeType

Server handles:

* onIncomingRequest → creates DB entry
* onUploadFinish → marks completed & generates S3 URL

---

## List forms + media

```
GET /forms/all
```

Response includes:

* Form details
* Media:

  * status
  * size
  * S3 URL (if complete)

---

# 🧱 Local Development Notes

### Android

* Uses ExoPlayer for video playback
* Uses Coil for images
* Resumable uploads via `TusClient`

### Backend

* In-memory DB → replace later with PostgreSQL (Prisma)
* Optional: switch to signed S3 URLs for private access
* Debug logs printed for TUS flow

---

# 🔒 Security Notes

* `.env` is **ignored** in `.gitignore`
* Never commit AWS credentials
* For production:

  * Use IAM roles
  * Private S3 bucket with presigned URLs
  * Add validation middleware

---

# 📌 Future Improvements (Optional)

* Media compression before upload
* Secure signed S3 URLs
* Backend DB migration (Postgres / Prisma)
* Video thumbnails
* Multi-file uploads queue
* Background uploads (WorkManager)
* Authentication (JWT)




