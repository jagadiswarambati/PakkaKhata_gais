# PakkaKhata — "The Ledger That Settles Itself"

**A phone-first, offline-first credit ledger for small shopkeepers.** PakkaKhata
records credit given to customers by voice or manual entry, then automatically
reconciles it against a photographed payment (a UPI screenshot or similar) —
recognizing the amount, matching it to the right open credit, and updating the
balance — without the shopkeeper manually marking anything as paid.

*iQOO Hackathon 2026 submission.*

---

## The problem

Small shopkeepers extend credit (*udhaar*) constantly and track it however
they can — a paper notebook, or a basic digital ledger app. Both share the
same gap: **someone has to manually reconcile every payment against the
original credit.** Digital ledger apps like Khatabook or OkCredit record what
you type; they don't verify it against anything. Payments arrive later, are
often partial or in excess, and matching them back to the right entry by
memory is exactly where balances go wrong.

## Core workflow

```
Voice/Manual Credit Entry → Open Obligation
        ↓
Payment arrives later → Photograph the evidence (UPI screenshot)
        ↓
On-device OCR extracts amount, sender name, UTR/reference, payment app
        ↓
Reconciliation engine scores every open obligation for this customer
(name similarity + amount fit + timing) and proposes a match
        ↓
Auto-Match (high confidence) → settles immediately
Suggested Match (medium confidence) → shopkeeper confirms first
No Match → nothing applied, evidence stays unlinked
        ↓
Obligation balance, payment record, reconciliation link, and customer
total outstanding all update atomically in one local transaction
```

**Worked example:** ₹500 credit to Ramesh Kumar → later, a photo of a ₹300
UPI payment → OCR reads ₹300 and "Ramesh Kumar" → matched against the open
₹500 obligation → **Partially Settled, ₹200 remaining**, updated everywhere
in the app immediately.

## Key features

- **Voice credit entry** — speak a credit in English, Hindi, or Hinglish
  ("*Ramesh ko paanch sau rupaye udhar*"); a local parser extracts the
  customer name, amount (including spoken number words like *paanch sau* →
  500 or *hazaar* → 1000), and an optional note, with a manual-edit
  confirmation step before saving.
- **Camera-based payment evidence** — an in-app CameraX preview captures a
  screenshot or photo of the payment; on-device OCR reads it.
- **Automatic, explainable reconciliation** — every match candidate is scored
  on name similarity, amount fit, and transaction timing, with the reasoning
  behind the score shown to the shopkeeper, not just the final decision.
- **Confidence-tiered matching** — high-confidence matches settle
  automatically; medium-confidence matches ask the shopkeeper to confirm
  before anything is applied; low-confidence evidence is left unmatched
  rather than guessed at.
- **Duplicate payment detection** — the same UTR/reference number, or an
  identical amount + sender + timestamp fingerprint, is caught before it can
  be reconciled twice.
- **Partial payment and overpayment handling** — a payment can leave a
  remaining balance, settle an obligation exactly, or exceed it, and each
  case is calculated and labeled distinctly.
- **Per-customer ledger view** — running balance and full obligation/payment
  history for each customer.
- **Local sharing/export** — a Markdown-formatted ledger summary or a single
  settlement receipt can be copied to the clipboard or sent through the
  Android share sheet, for handing data off to a laptop or another app.
- **Built-in demo scaffolding** — a one-call function seeds the canonical
  demo scenario (₹500 credit to "Ramesh Kumar"), and another clears the
  ledger back to empty, so the app can be reset to a known state between
  demo runs.

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3) for the entire UI
- **Room** for local persistence, with Kotlin coroutines/Flow throughout
- **Navigation Compose** for the app's screen graph
- **CameraX** for the in-app payment-evidence camera preview and capture
- **ML Kit Text Recognition** (on-device, bundled model) for OCR
- **Android `SpeechRecognizer`** (platform API, offline-preferring) for voice
  input
- **Coil** for image loading
- **JUnit4**, **Robolectric**, and **Roborazzi** for unit, database
  integration, and Compose screenshot testing

No backend, no external database, and no third-party AI/LLM API are called
by the app at runtime — everything above runs on-device. (The project's build
configuration, inherited from its Android Studio AI-Studio scaffold, still
lists some unused cloud-related dependencies such as Firebase and Retrofit;
none of them are referenced by any source file or wired into the app, and the
manifest declares no `INTERNET` permission.)

## Architecture

Layered, roughly domain-driven:

```
presentation/          Compose screens, ViewModels, navigation
  screens/home                Dashboard, voice-credit bottom sheet, office-bridge sheet
  screens/evidence             Camera capture, review, saved-confirmation
  screens/reconciliation      Match review (candidate scores), settlement result
  screens/customer              Per-customer ledger detail
  navigation                  Screen routes + NavHost

domain/                 Pure Kotlin — no Android dependencies
  model                        Customer, Obligation, PaymentEvidence, Reconciliation,
                                 Money (integer-paise value type), status enums
  reconciliation               DefaultReconciliationEngine, FuzzyMatcher,
                                 NameNormalizer, DuplicateDetector, SettlementCalculator,
                                 ReconciliationConfig (scoring weights/thresholds)
  perception                   SpeechProvider / OCRProvider interfaces,
                                 VoiceCreditParser, PaymentEvidenceParser
  usecase                      EvaluateReconciliationUseCase, ExecuteSettlementUseCase
  officebridge                 OfficeBridgeService (clipboard/share/report generation)

data/                   Android-facing implementations
  local                        Room database, DAOs, entities, type converters
  perception                   MlKitOcrProvider, AndroidSpeechRecognizerProvider
  repository                    CustomerRepository, ObligationRepository,
                                 PaymentEvidenceRepository, ReconciliationRepository,
                                 LedgerRepository (facade over all four + atomic settlement)
```

The reconciliation engine and both parsers live in `domain/` with zero
Android imports, so their logic is tested as plain JVM unit tests. Screens
construct their own `PakkaKhataDatabase`/repository/provider instances
directly (via `AndroidViewModel`) — there is no dependency-injection
framework in this project.

## Offline and on-device AI capabilities

- **OCR is genuinely offline.** ML Kit's Text Recognition here uses the
  bundled (not cloud) model, so it works immediately with no network and no
  model download.
- **Reconciliation logic is 100% local and deterministic** — no network
  call, no cloud model. It is a hand-written scoring and rules engine (see
  below), not a trained machine-learning model.
- **Voice input is offline-preferring, not offline-guaranteed.** It uses
  Android's standard `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE` set,
  which is a hint the OS honors when an offline language pack is installed
  on the device — it is not a guarantee on every device/OS combination.
  Manual entry is always available as a fallback.
- **No data leaves the device.** There is no backend and no network
  permission; all customer, credit, and payment data stays in the local
  Room database.

## Reconciliation logic

`DefaultReconciliationEngine.evaluate(...)` runs, in order:

1. **Duplicate check** — `DuplicateDetector` rejects evidence matching an
   existing UTR/reference number, or an identical amount + sender +
   timestamp fingerprint.
2. **Candidate scoring** — every open obligation (`OPEN` or
   `PARTIALLY_SETTLED`, with a positive remaining balance) for the relevant
   customers is scored on three signals:
   - **Name similarity** (weight 0.65) — via `FuzzyMatcher`: exact
     normalized match, token containment (e.g. "Ramesh" within "Ramesh
     Kumar"), Levenshtein-distance similarity, and token-level alignment,
     with `NameNormalizer` stripping punctuation, case, and common Indian
     honorifics (Shri, Smt, ji, bhai, etc.) first.
   - **Amount fit** (weight 0.30) — exact match scores highest, a
     compatible partial payment next, an amount exceeding the balance
     (overpayment) lowest of the three positive cases.
   - **Transaction timing** (weight 0.05) — a payment timestamped after the
     obligation was created scores higher than one that predates it.
3. **Identity guardrail** — if the name-similarity score falls below the
   configured minimum, the candidate's overall score is zeroed regardless of
   amount fit, so a payment can never be attributed to the wrong person on
   amount alone.
4. **Decision thresholding** — the highest-scoring candidate becomes
   `AUTO_MATCH` (auto-settle), `SUGGESTED_MATCH` (needs shopkeeper
   confirmation), or `NO_MATCH` (left unlinked), based on configurable
   thresholds in `ReconciliationConfig`.
5. **Settlement calculation** — `SettlementCalculator` does the actual
   arithmetic in integer paise (no floating-point money math anywhere in the
   app) and returns `FULLY_SETTLED`, `PARTIALLY_SETTLED`, `OVERPAID`, or
   `NO_MATCH`.

Every candidate carries a human-readable list of match reasons (e.g. *"Strong
name match (92%) with 'Ramesh Kumar'"*, *"Payment is compatible partial
settlement"*), which the Match Review screen surfaces directly rather than
just showing a final verdict.

Applying a settlement (`LedgerRepositoryImpl.executeAtomicSettlement`) runs
as a single Room `withTransaction` block that inserts/updates the payment
evidence, updates the obligation's remaining amount and status, inserts the
reconciliation link record, and updates the customer's running balance —
all four together, or none of them.

**Scope note:** matching resolves one piece of payment evidence to one open
obligation. Splitting a single payment across multiple obligations is not
implemented.

## Testing

Test suite spans ~2,450 lines across JUnit4 (pure logic), Robolectric
(Android-runtime and Room database integration), and Roborazzi (Compose
screenshot) tests, including:

- `ReconciliationEngineTest` — name/amount/timing scoring, the identity
  guardrail, and all four decision/outcome combinations.
- `PaymentEvidenceParserTest` / `VoiceCreditParserTest` — OCR-text and
  voice-transcript parsing across currency formats, UTR patterns, and
  spoken Hindi/English number words.
- `PakkaKhataDatabaseTest`, `VoiceCreditDatabaseIntegrationTest`,
  `PaymentEvidenceDatabaseIntegrationTest`, `ReconciliationAndSettlementIntegrationTest`
  — Room persistence and the atomic settlement transaction, run against a
  real (in-memory) database via Robolectric.
- `HomeLedgerStateHierarchyTest`, `Phase6FinalLedgerAndCustomerDetailTest` —
  ViewModel/UI state correctness.

Run all tests with:

```bash
./gradlew test
```

## Setup and run

**Requirements:** a recent Android Studio (Ladybug/2024.2 or newer) with JDK
17+, given this project targets `compileSdk` 36 / AGP 9.1.1 / Kotlin 2.2.

1. Open the project root in Android Studio and let Gradle sync.
2. No API keys or `.env` file are required to build or run — the app makes
   no network calls. (The Secrets Gradle Plugin falls back to
   `.env.example`'s placeholder values automatically if no `.env` is
   present, and the Google Services plugin is configured to warn rather
   than fail if `google-services.json` is absent.)
3. Run on a device or emulator with a camera and microphone (`minSdk` 24). A
   physical device is recommended for realistic camera/OCR testing.
4. Grant Camera and Microphone permissions when prompted (used only for
   payment-evidence capture and voice credit entry, respectively).

## Demo flow

1. From the Home dashboard, use the built-in demo seed
   (`loadDemoScenario()`) to load the canonical scenario — a ₹500 open
   credit for "Ramesh Kumar" — or add a credit live via the voice bottom
   sheet.
2. Open payment capture and photograph a UPI-style screenshot showing a
   partial payment (e.g. ₹300). Watch OCR extract the amount, sender, and
   reference live.
3. Land on Match Review: see the scored candidate(s) with their name/amount/
   timing breakdown and match reasons, and the proposed outcome.
4. Confirm the match (or, on a medium-confidence evidence photo, show the
   Suggested-Match confirmation step specifically) to reach Settlement
   Result — **Partially Settled, ₹200 remaining.**
5. Return to Home and Customer Detail to show the balance updated
   everywhere automatically.
6. Optional: re-submit the same screenshot to show duplicate-payment
   detection catch it, or use the Office Bridge sheet to copy/share a
   formatted ledger summary.
7. Use the ledger-reset function to return to a clean state before the next
   run-through.

## Known limitations

- Voice input's offline behavior depends on the device having an installed
  offline speech-recognition language pack; it is not independently
  verified as offline on every device.
- A single piece of payment evidence resolves to a single obligation, not a
  split across several.
- "Office Bridge" sharing uses Android's standard clipboard and share-sheet
  APIs to hand data to another app or device — it is not a proprietary
  iQOO Office Kit SDK integration.
- Reconciliation is a deterministic scoring/rules engine, not a trained
  machine-learning model; the only pre-trained component in the pipeline is
  ML Kit's OCR model.
