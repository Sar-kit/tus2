import express from "express";
import { Server } from "@tus/server";
import crypto from "node:crypto";
import { S3Store } from "@tus/s3-store";
import { config as loadEnv } from "dotenv";

loadEnv();

// =======================
// Fake DB layer (replace)
// =======================

// In real life, these would be Prisma/TypeORM/SQL queries etc.
const formsTable = new Map();   // formId -> form row
const mediaTable = new Map();   // uploadId -> media row

function createFormEntry(data) {
  const id = crypto.randomUUID();
  const row = {
    id,
    title: data.title ?? null,
    description: data.description ?? null,
    createdAt: new Date().toISOString(),
  };
  formsTable.set(id, row);
  return row;
}

function getFormById(id) {
  return formsTable.get(id) ?? null;
}

async function createMediaEntry({ uploadId, formId, fileName, mimeType, size }) {
  const row = {
    id: uploadId,
    formId,
    fileName,
    mimeType,
    size: size ?? null,
    status: "uploading", // "uploading" | "complete" | "failed"
    url: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  mediaTable.set(uploadId, row);
  return row;
}

async function markMediaComplete({ uploadId, size, url }) {
  const row = mediaTable.get(uploadId);
  if (!row) return null;
  row.status = "complete";
  row.size = size ?? row.size;
  row.url = url;
  row.updatedAt = new Date().toISOString();
  mediaTable.set(uploadId, row);
  return row;
}

function decodeTusMetadata(header) {
  if (!header || typeof header !== "string") return {};

  const metadata = {};
  const pairs = header.split(",");

  for (const pair of pairs) {
    // Each entry is: key base64value
    const spaceIndex = pair.indexOf(" ");
    if (spaceIndex === -1) continue;

    const key = pair.substring(0, spaceIndex).trim();
    const base64value = pair.substring(spaceIndex + 1).trim();
    if (!key || !base64value) continue;

    try {
      // decode base64 → utf8
      metadata[key] = Buffer.from(base64value, "base64").toString("utf8");
    } catch (err) {
      console.error(`Failed decoding metadata for key '${key}':`, err);
    }
  }

  return metadata;
}




// =======================
// Express + TUS server
// =======================

const host = process.env.HOST;
const port = process.env.PORT;

const AWS_ACCESS_KEY_ID = process.env.AWS_ACCESS_KEY_ID;
const AWS_SECRET_ACCESS_KEY = process.env.AWS_SECRET_ACCESS_KEY;
const AWS_REGION = process.env.AWS_REGION;

const S3_BUCKET = process.env.S3_BUCKET;
const S3_PREFIX = process.env.S3_PREFIX;


class DebugS3Store extends S3Store {
  async create(info, req) {
    console.log("🟥 S3Store.create called");
    console.log("info:", info);
    return super.create(info, req);
  }

  async write(upload, chunk, offset) {
    console.log(`🟧 S3Store.write called | chunk = ${chunk.length} bytes`);
    return super.write(upload, chunk, offset);
  }

  async complete(upload) {
    console.log("🟩 S3Store.complete called");
    return super.complete(upload);
  }
}

const datastore = new DebugS3Store({
  bucketName: S3_BUCKET,
  partSize: 5 * 1024 * 1024,
  prefix: S3_PREFIX,

  // NOTE: v2 uses "s3ClientConfig" — not "s3Config"
  s3ClientConfig: {
    bucket: S3_BUCKET,
    region: AWS_REGION,
    credentials: {
      accessKeyId: AWS_ACCESS_KEY_ID,
      secretAccessKey: AWS_SECRET_ACCESS_KEY,
    }
  }
});

const app = express();

// JSON body for your normal REST endpoints
app.use(express.json());

// --------- Normal form endpoint (no files) ----------
// Kotlin app calls this first to create the main form entry
// Body: { title, description, ...anything else... }
app.post("/forms", (req, res) => {
  try {
    const form = createFormEntry(req.body);
    // Return the form id so the client can send it in TUS metadata
    res.status(201).json({ id: form.id });
  } catch (err) {
    console.error("Error creating form:", err);
    res.status(500).json({ error: "Failed to create form" });
  }
});

// --------- TUS file uploads ----------

const uploadApp = express();

// Create the TUS server
const tusServer = new Server({
  // Must match the mount path (`/uploads`) below
  path: "/uploads",

  datastore,

    // Executes for every chunks api call.
    onIncomingRequest: async (req, uploadId) => {
      
            console.log('Upload ongoin')
        try {
            if (req.method === "POST") {
                const metadataHeader = req.headers.get("upload-metadata") || "";
                const metadata = decodeTusMetadata(metadataHeader);
               
                const formId = metadata.formId;
                const fileName = metadata.fileName;
                const mimeType = metadata.mimeType;

                if (!formId) {
                    throw { status_code: 400, body: "Missing formId" };
                }

                const form = getFormById(formId);
                if (!form) {
                    throw { status_code: 400, body: "Invalid formId" };
                }

                await createMediaEntry({
                    uploadId,   // NOW it exists!
                    formId,
                    fileName,
                    mimeType,
                    size: null,  // size not known until upload finishes
                });
            }
            
        } catch (err) {
            throw err; // tus handles status_code + body
        }

    },

    // Called when a new upload is created (POST). Doesn't have uploadId at this point i think.
    onUploadCreate: async (req, upload) => {
        if (!upload.metadata?.formId)
            throw { status_code: 400, body: "formId required" };

        return {}
    },

    // Called when upload is fully finished (last PATCH)
    onUploadFinish: async (req, upload) => {
        try {
            const uploadId = upload.id;
            const size = upload.size;
            const fileUrl = `https://${S3_BUCKET}.s3.${AWS_REGION}.amazonaws.com/${S3_PREFIX}${uploadId}`;
           
            await markMediaComplete({
            uploadId,
            size,
            url: fileUrl,
            });

            console.log('Upload finished:',fileUrl)
            return {}; // optional but clean
        } catch (err) {
            console.error("onUploadFinish error:", err);
            throw err;
        }
    },

});

// ------------------------------------------
// GET ALL FORMS WITH THEIR MEDIA ENTRIES
// ------------------------------------------
app.get("/forms/all", (req, res) => {
  try {
    const forms = [];

    for (const [formId, formRow] of formsTable.entries()) {
      // All media belonging to this form
      const media = [];

      for (const [uploadId, mediaRow] of mediaTable.entries()) {
        if (mediaRow.formId === formId) {
          media.push({
            id: mediaRow.id,
            fileName: mediaRow.fileName,
            mimeType: mediaRow.mimeType,
            status: mediaRow.status,
            url: mediaRow.status === "complete"
              ? mediaRow.url
              : null, // pending uploads return null or placeholder
            size: mediaRow.size,
            createdAt: mediaRow.createdAt,
            updatedAt: mediaRow.updatedAt,
          });
          console.log('url::',mediaRow.url)
        }
      }

      forms.push({
        id: formRow.id,
        title: formRow.title,
        description: formRow.description,
        createdAt: formRow.createdAt,
        media,
      });
    }

    res.json({ forms });

  } catch (err) {
    console.error("Error retrieving forms:", err);
    res.status(500).json({ error: "Failed to retrieve forms" });
  }
});


// Important: do NOT put any body parsers (json/urlencoded) in front of this.
// Using `uploadApp.use(...)` avoids the Express 5 `*` wildcard issue.
uploadApp.use((req, res) => {
  console.log("➡️ TUS handler received:", req.method, req.url);
  tusServer.handle(req, res);
});

app.use("/uploads", (req, res, next) => {
  console.log("\n📥 [UPLOAD REQUEST]", req.method, req.url);
  console.log("Headers:", req.headers);
  next();
});

// Mount the TUS handler under /uploads
app.use("/uploads", uploadApp);

// --------- Start server ----------

app.listen(port, host, () => {
  console.log(`Server listening at http://${host}:${port}`);
  console.log(`TUS endpoint: http://${host}:${port}/uploads`);
});
