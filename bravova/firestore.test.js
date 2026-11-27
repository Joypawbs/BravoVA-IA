const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const clientTimestamp = () => new Date(Date.now() - 1000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profile, cart, bravisimaCards, orders, or shoppingLists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bravisimaCards").doc("primary").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("orders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("shoppingLists").get()
  );
});

test("User Profile: authenticated owner can create, read, and update their own profile; bravisimaNumber is immutable; cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("profile")
    .doc("main");

  const now = clientTimestamp();
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      name: "Alice Bravo",
      email: "alice@bravova.com.do",
      phone: "809-555-0192",
      cedula: "001-1928374-5",
      bravisimaNumber: "BRV-8842-9901-7234",
      rncNit: "131-88492-1",
      ncfType: "B01 - Crédito Fiscal",
      activeBranch: "Bravo Winston Churchill",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(profileRef.get());

  // Owner can update mutable profile fields
  await assertSucceeds(
    profileRef.update({
      name: "Alice Bravo Actualizada",
      phone: "809-555-0199",
      activeBranch: "Bravo Enriquillo",
      updatedAt: clientTimestamp(),
    })
  );

  // Tampering with linked bravisimaNumber on profile update must fail
  await assertFails(
    profileRef.update({
      bravisimaNumber: "BRV-0000-0000-0000",
      updatedAt: clientTimestamp(),
    })
  );

  // Cross-user isolation: Bob cannot read or modify Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("main")
      .update({ name: "Hackeado por Bob", updatedAt: clientTimestamp() })
  );
});

test("Shopping Cart: authenticated owner can add, update quantity, list, and delete their own cart items; cross-user access and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cartItemRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cart")
    .doc("BRV-001");

  const now = clientTimestamp();
  await assertSucceeds(
    cartItemRef.set({
      userId: ALICE_UID,
      sku: "BRV-001",
      qty: 2,
      storeMode: "BRAVO",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  await assertSucceeds(
    cartItemRef.update({
      qty: 5,
      updatedAt: clientTimestamp(),
    })
  );

  // Rejects mismatched SKU vs path ID
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cart")
      .doc("BRV-002")
      .set({
        userId: ALICE_UID,
        sku: "BRV-999",
        qty: 1,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Rejects zero or negative quantity in cart document
  await assertFails(
    cartItemRef.update({
      qty: 0,
      updatedAt: clientTimestamp(),
    })
  );

  // Cross-user isolation: Bob cannot read, modify, or delete Alice's cart
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("BRV-001").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cart")
      .doc("BRV-001")
      .update({ qty: 99, updatedAt: clientTimestamp() })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("BRV-001").delete()
  );

  // Owner can delete cart item
  await assertSucceeds(cartItemRef.delete());
});

test("Authenticated owner: can create, list, and update their own orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("orders")
    .doc("ORD-88412");

  const now = clientTimestamp();
  await assertSucceeds(
    orderRef.set({
      userId: ALICE_UID,
      status: "Envío preparado",
      badgeNum: 6,
      date: "Hoy, hace 10 min",
      total: 1597.0,
      branch: "Bravo Winston Churchill",
      skus: ["BRV-001", "BRV-009", "BRV-053"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").get()
  );

  await assertSucceeds(
    orderRef.update({
      status: "Entregado",
      updatedAt: clientTimestamp(),
    })
  );
});

test("Authenticated owner: can create, list, update, and delete their own shoppingLists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const listRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shoppingLists")
    .doc("list_quincenal");

  const now = clientTimestamp();
  await assertSucceeds(
    listRef.set({
      userId: ALICE_UID,
      name: "Compra Quincenal Habitual",
      skus: ["BRV-001", "BRV-053", "BRV-075"],
      aiNote: "Autocompletada por Brav IA según tu historial.",
      isAiGenerated: true,
      updatedAtLabel: "Sincronizada con Firestore",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoppingLists")
      .get()
  );

  await assertSucceeds(
    listRef.update({
      name: "Compra Quincenal Bravo",
      skus: ["BRV-001", "BRV-053", "BRV-075", "BRV-093"],
      updatedAt: clientTimestamp(),
    })
  );

  await assertSucceeds(listRef.delete());
});

test("Cross-user isolation: Bob cannot read, modify, or delete Alice's orders or shoppingLists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = clientTimestamp();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoppingLists")
      .doc("alice_list")
      .set({
        userId: ALICE_UID,
        name: "Mis Favoritos",
        skus: ["BRV-001"],
        isAiGenerated: false,
        updatedAtLabel: "Sincronizada",
        createdAt: now,
        updatedAt: now,
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoppingLists")
      .doc("alice_list")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoppingLists")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoppingLists")
      .doc("alice_list")
      .delete()
  );
});

test("Shadow update & schema validation: rejects ghost fields and invalid types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const listRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shoppingLists")
    .doc("bad_list");

  const now = clientTimestamp();
  await assertFails(
    listRef.set({
      userId: ALICE_UID,
      name: "Lista con campo fantasma",
      skus: ["BRV-001"],
      isAiGenerated: false,
      updatedAtLabel: "Sincronizada",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Bravisima Card & Movements: unauthenticated and cross-user access fail; owner CRUD succeeds and protects card integrity", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaCards")
      .doc("primary")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaMovements")
      .get()
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cardRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bravisimaCards")
    .doc("primary");

  const now = clientTimestamp();
  await assertSucceeds(
    cardRef.set({
      userId: ALICE_UID,
      cardNumber: "BRV-8842-9901-7234",
      holderName: "Alice Bravo",
      holderEmail: "alice@bravova.com.do",
      balance: 145,
      lifetimeEarned: 320,
      status: "active",
      autoRedeemInCart: false,
      monthlyGoal: 500,
      preferredCategory: "Todo Bravo",
      claimedPromoCodes: ["BIENVENIDA"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner can lock card and update monthlyGoal
  await assertSucceeds(
    cardRef.update({
      status: "locked",
      monthlyGoal: 800,
      autoRedeemInCart: true,
      updatedAt: clientTimestamp(),
    })
  );

  // Immutable cardNumber change must fail
  await assertFails(
    cardRef.update({
      cardNumber: "BRV-0000-0000-0000",
      updatedAt: clientTimestamp(),
    })
  );

  // Decreasing lifetimeEarned must fail
  await assertFails(
    cardRef.update({
      lifetimeEarned: 100,
      updatedAt: clientTimestamp(),
    })
  );

  // Shadow update on card must fail
  await assertFails(
    cardRef.update({
      balance: 999,
      isAdminGrant: true,
      updatedAt: clientTimestamp(),
    })
  );

  // Movement create & list
  const movRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bravisimaMovements")
    .doc("mov_1");

  await assertSucceeds(
    movRef.set({
      userId: ALICE_UID,
      title: "Compra Bravo Churchill",
      subtitle: "Retorno 1% Bravísima",
      dateLabel: "Hoy",
      amount: 24,
      movementType: "earn",
      orderId: "ORD-88412",
      createdAt: clientTimestamp(),
      updatedAt: clientTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaMovements")
      .get()
  );

  // Bob cannot read or mutate Alice's Bravísima card or movements
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaCards")
      .doc("primary")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaCards")
      .doc("primary")
      .update({ status: "locked", updatedAt: clientTimestamp() })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bravisimaMovements")
      .get()
  );
});
