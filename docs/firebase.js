// Firebase (misma configuración del proyecto de la app Android: gen-lang-client-0590253686)
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-auth.js";
import { getFirestore, doc, setDoc, getDoc, getDocs, updateDoc, deleteDoc, collection, serverTimestamp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-firestore.js";
import { getAI, getGenerativeModel, GoogleAIBackend } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-ai.js";

const firebaseConfig = {
  apiKey: "AIzaSyDEgj3kNB--XoGU9urXSdittW-Q9b-nmP8",
  authDomain: "gen-lang-client-0590253686.firebaseapp.com",
  projectId: "gen-lang-client-0590253686",
  storageBucket: "gen-lang-client-0590253686.firebasestorage.app",
  messagingSenderId: "723529928496",
  appId: "1:723529928496:web:ad8ce28b35570234e4c2f5"
};
// Misma base de datos Firestore nombrada que usa la app Android (firebase.json)
const FIRESTORE_DB_ID = "ai-studio-android-bravova-a4e573d9-b4f2-44a4-af7f-b9667f2faaf3";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DB_ID);
const ai = getAI(app, { backend: new GoogleAIBackend() });
const models = {
  primary: getGenerativeModel(ai, { model: "gemini-3.8-flash" }),
  fallback: getGenerativeModel(ai, { model: "gemini-3.5-flash-lite" })
};

async function* streamFrom(model, parts) {
  const result = await model.generateContentStream(parts);
  for await (const chunk of result.stream) {
    const t = chunk.text();
    if (t) yield t;
  }
}

window.BV_FB = {
  ready: true,
  currentUser: () => auth.currentUser,
  onUser: cb => onAuthStateChanged(auth, cb),
  signInGoogle: () => signInWithPopup(auth, new GoogleAuthProvider()),
  signOut: () => signOut(auth),
  // Igual que la app: primero gemini-3.8-flash y, si falla, gemini-3.5-flash-lite
  async *generate(prompt, imageBase64, mime) {
    const parts = imageBase64 ? [{ inlineData: { data: imageBase64, mimeType: mime || "image/jpeg" } }, prompt] : [prompt];
    try {
      yield* streamFrom(models.primary, parts);
    } catch (e) {
      console.warn("Modelo principal falló, usando respaldo", e);
      yield* streamFrom(models.fallback, parts);
    }
  },
  // Mismo formato que BravoFirestoreRepository.setCartItemQuantity: users/{uid}/cart/{SKU}
  async setCartQty(uid, sku, qty) {
    const id = sku.trim().toUpperCase().slice(0, 40);
    const ref = doc(db, "users", uid, "cart", id);
    if (qty <= 0) { await deleteDoc(ref); return; }
    const q = Math.min(999, Math.max(1, qty));
    const storeMode = id.startsWith("ARC-") ? "ARCA" : "BRAVO";
    const snap = await getDoc(ref);
    if (!snap.exists()) {
      await setDoc(ref, { userId: uid, sku: id, qty: q, storeMode, createdAt: serverTimestamp(), updatedAt: serverTimestamp() });
    } else {
      await updateDoc(ref, { qty: q, storeMode, updatedAt: serverTimestamp() });
    }
  },
  async loadCart(uid) {
    const snap = await getDocs(collection(db, "users", uid, "cart"));
    const out = {};
    snap.forEach(d => { const v = d.data(); if (v.sku && v.qty > 0) out[v.sku] = v.qty; });
    return out;
  }
};
window.dispatchEvent(new Event("bvfb-ready"));
