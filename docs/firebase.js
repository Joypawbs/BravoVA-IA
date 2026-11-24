// Firebase (misma configuración del proyecto de la app Android: gen-lang-client-0590253686)
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-auth.js";
import { getFirestore, doc, setDoc, getDoc, getDocs, updateDoc, deleteDoc, collection, serverTimestamp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-firestore.js";
import { initializeAppCheck, ReCaptchaEnterpriseProvider } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-app-check.js";
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
// App Check (igual que la app Android, que usa reCAPTCHA Enterprise): clave web propia para joypawbs.github.io
const appCheck = initializeAppCheck(app, {
  provider: new ReCaptchaEnterpriseProvider("6Ld2Uy8uAAAAAH3yA7xR6cGqSW_fm82Nevg_eOCG"),
  isTokenAutoRefreshEnabled: true
});
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DB_ID);
const ai = getAI(app, { backend: new GoogleAIBackend(), useLimitedUseAppCheckTokens: false });
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
    let got = false;
    try {
      // Si el modelo principal no empieza a responder en 20 s (alta demanda), se pasa al respaldo
      const it = streamFrom(models.primary, parts)[Symbol.asyncIterator]();
      const first = await Promise.race([it.next(), new Promise((_, r) => setTimeout(() => r(new Error("timeout primario")), 20000))]);
      if (!first.done) { got = true; yield first.value; }
      for (let r = await it.next(); !r.done; r = await it.next()) { got = true; yield r.value; }
    } catch (e) {
      if (got) throw e;
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
  // Igual que BravoFirestoreRepository.clearUserCart
  async clearCart(uid) {
    const snap = await getDocs(collection(db, "users", uid, "cart"));
    await Promise.all(snap.docs.map(d => deleteDoc(d.ref)));
  },
  // Igual que BravoFirestoreRepository.createOrder: users/{uid}/orders/{ORD-xxxxx}
  async createOrder(uid, o) {
    await setDoc(doc(db, "users", uid, "orders", o.id), {
      userId: uid,
      status: o.status.slice(0, 80),
      badgeNum: Math.min(500, Math.max(0, o.badgeNum)),
      date: o.date.slice(0, 80),
      total: Math.min(1000000, Math.max(0, o.total)),
      branch: o.branch.slice(0, 120),
      skus: o.skus.slice(0, 50),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  },
  // Igual que updateTrackedOrderStatusInFirestore: solo status/date/updatedAt
  async updateOrderStatus(uid, id, o) {
    await updateDoc(doc(db, "users", uid, "orders", id), { status: o.status.slice(0, 80), date: o.date.slice(0, 80), updatedAt: serverTimestamp() });
  },
  // Igual que BravoFirestoreRepository.saveShoppingList / deleteShoppingList
  async saveList(uid, l) {
    const ref = doc(db, "users", uid, "shoppingLists", l.id);
    const base = { name: (l.name.trim() || "Mi Lista").slice(0, 120), skus: [...new Set(l.skus)].slice(0, 50), isAiGenerated: !!l.isAi, updatedAtLabel: (l.upd || "Sincronizada").slice(0, 120), updatedAt: serverTimestamp() };
    if (l.note) base.aiNote = l.note.slice(0, 500);
    const snap = await getDoc(ref);
    if (!snap.exists()) await setDoc(ref, { userId: uid, ...base, createdAt: serverTimestamp() });
    else await updateDoc(ref, base);
  },
  async deleteList(uid, id) { await deleteDoc(doc(db, "users", uid, "shoppingLists", id)); },
  // Igual que saveOrUpdateBravisimaCard: users/{uid}/bravisimaCards/primary
  async saveCard(uid, c) {
    const ref = doc(db, "users", uid, "bravisimaCards", "primary");
    const snap = await getDoc(ref);
    const base = { holderName: (c.holder.trim() || "Cliente Bravísimo").slice(0, 100), balance: Math.min(1000000, Math.max(0, c.balance)), lifetimeEarned: Math.min(5000000, Math.max(0, c.lifetime)), status: c.locked ? "locked" : "active", autoRedeemInCart: !!c.autoRedeem, monthlyGoal: Math.min(50000, Math.max(100, c.goal)), preferredCategory: (c.category || "Todo Bravo").slice(0, 80), claimedPromoCodes: [...new Set(c.codes)].slice(0, 20), updatedAt: serverTimestamp() };
    if (c.email) base.holderEmail = c.email.slice(0, 160);
    if (!snap.exists()) { await setDoc(ref, { userId: uid, cardNumber: c.number.slice(0, 32), ...base, createdAt: serverTimestamp() }); return c.number; }
    const ex = snap.data(); base.lifetimeEarned = Math.max(base.lifetimeEarned, ex.lifetimeEarned || 0);
    await updateDoc(ref, base); return ex.cardNumber || c.number;
  },
  async loadCard(uid) { const s = await getDoc(doc(db, "users", uid, "bravisimaCards", "primary")); return s.exists() ? s.data() : null; },
  async recordMovement(uid, m) {
    const p = { userId: uid, title: m.title.slice(0, 120), subtitle: m.subtitle.slice(0, 120), dateLabel: m.date.slice(0, 80), amount: Math.max(-500000, Math.min(500000, m.amount)), movementType: m.type, createdAt: serverTimestamp(), updatedAt: serverTimestamp() };
    if (m.orderId) p.orderId = m.orderId.slice(0, 80);
    await setDoc(doc(db, "users", uid, "bravisimaMovements", m.id), p);
  },
  // Igual que saveOrUpdateUserProfile: users/{uid}/profile/main
  async saveProfile(uid, pr, branch) {
    const ref = doc(db, "users", uid, "profile", "main");
    const snap = await getDoc(ref);
    const base = { name: (pr.name.trim() || "Cliente Bravísimo").slice(0, 100), email: (pr.email || "cliente@bravova.com.do").slice(0, 160), phone: (pr.phone || "809-555-0192").slice(0, 40), cedula: (pr.cedula || "001-1928374-5").slice(0, 40), rncNit: (pr.rnc || "131-88492-1").slice(0, 40), ncfType: (pr.ncf || "B01 - Crédito Fiscal").slice(0, 60), activeBranch: branch.slice(0, 120), updatedAt: serverTimestamp() };
    if (!snap.exists()) await setDoc(ref, { userId: uid, ...base, bravisimaNumber: pr.bravisimaNumber, createdAt: serverTimestamp() });
    else await updateDoc(ref, base);
  },
  async loadCart(uid) {
    const snap = await getDocs(collection(db, "users", uid, "cart"));
    const out = {};
    snap.forEach(d => { const v = d.data(); if (v.sku && v.qty > 0) out[v.sku] = v.qty; });
    return out;
  }
};
window.dispatchEvent(new Event("bvfb-ready"));
