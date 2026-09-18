# PakkaKhata — "The Ledger That Settles Itself"

A phone-first, offline-first digital credit ledger (khata) for small
shopkeepers. This repository now contains the **Android MVP foundation plus
the full Payment Evidence flow**: capture, OCR, matching, and a clear
settlement result — all wired together and running fully on-device.

## Core flow (target end state) — now end-to-end

```
Voice Credit → Open Obligation → Payment Later → Payment Evidence →
OCR + Intelligent Matching → Full / Partial / Overpaid / No Match →
Updated Balance
```

Worked example, live in the app: ₹500 credit to Ramesh → later, a photo of a
₹300 UPI payment → OCR reads ₹300 → matched against the open obligation →
**Partially Settled, ₹200 remaining** → Room and the ledger UI update
immediately. Voice credit capture is still a stub (see below); every other
step in this pipeline is real and working.

## What's implemented

### Foundation (previous phase)
- Android project: Kotlin + Jetpack Compose, Gradle Kotlin DSL, Material 3.
- Navigation via `navigation-compose`.
- Room database, offline-first, three tables (`customers`, `obligations`,
  `payments`).
- Domain models, repository layer, Dashboard / Customer Ledger / Add Credit
  screens.
- Manual `ServiceLocator` DI container.

### Payment Evidence flow (this phase)
- **`PaymentEvidenceScreen`** — a single screen with three phases:
  1. **Capture** — "Take Photo" (camera) or "Choose from Gallery" (Android
     Photo Picker — no storage permission needed).
  2. **Review** — shows the captured image, runs OCR automatically, and
     pre-fills an **editable** paid-amount field. If OCR can't confidently
     read an amount, the field is simply left blank with a hint — the
     shopkeeper (or a demo presenter) always has a manual fallback, so a
     misread never blocks the flow.
  3. **Result** — a clear, color-coded outcome card: **Fully Settled**
     (teal), **Partially Settled** (gold), **Overpaid** (navy), or
     **No Match** (red), with the credit/paid/remaining figures shown
     plainly.
- **Evidence storage** — `EvidenceFileStore` saves every captured/picked
  image to app-private local storage (`filesDir/evidence/`). Never written
  anywhere another app or the OS media store can see it.
- **Real, on-device OCR** — `MlKitOcrService` implements `OcrService` using
  ML Kit's **bundled** Text Recognition model (works fully offline
  immediately, no model download). The raw recognized text is parsed by
  `UpiReceiptParser` — a small, dependency-free, independently unit-tested
  regex parser that pulls out an amount (₹ / Rs / INR-prefixed, with a
  bare-number fallback) and a payer/reference line (a UPI handle, or a
  "paid to ..." line). No image or extracted text ever leaves the device.
- **Matching wired end-to-end** — `LedgerRepository.recordPaymentEvidence(...)`
  takes the confirmed amount, resolves it against the customer's oldest open
  obligation (FIFO — see scope note below), calls the existing
  `RuleBasedMatchingService`, updates that obligation's `amountPaid` in
  Room, and records a `Payment` row with the outcome. This is the same
  matcher and the same unit tests from the foundation phase — nothing about
  the matching *logic* changed, it's now just actually called from a screen.
- **"Record Payment" entry point** added to the Customer Ledger screen (a
  gold FAB, next to viewing that customer's history — the natural place to
  start recording a payment for them).
- New unit tests: `UpiReceiptParserTest` (8 cases covering ₹/Rs/INR formats,
  repeated amounts, bare-number fallback, UPI handle extraction, and the
  "nothing found" case).

## Scope notes — read before assuming more was built than was

- **Matching still targets a single obligation per customer (FIFO).**
  "No Match" in this phase specifically means *"this customer has no open
  obligation at all to apply a payment to."* Deciding *which* of several
  open obligations a payment belongs to (fuzzy matching across candidates
  when a customer has multiple open entries) is still the next-phase
  upgrade — it was explicitly out of scope for this step and remains so.
- **Voice capture is unchanged and still a stub.** The Add Credit screen's
  "Speak" button still reports "not available." This phase only touched the
  Payment Evidence side of the pipeline.
- **Camera capture uses `TakePicturePreview`**, which returns a downsized
  preview bitmap (not a full-resolution photo). This keeps the flow simple
  (no `FileProvider`/manifest provider entry needed) and is sufficient for
  OCR on a phone screen photographed at a normal distance. If OCR accuracy
  on low-light or angled photos turns out to be a problem in testing, the
  fix is swapping to full-resolution `TakePicture` + `FileProvider` — a
  contained change inside `PaymentEvidenceScreen`'s camera launcher only.
- **OCR is genuinely best-effort by design.** The editable confirm field in
  the Review step isn't a placeholder — it's the deliberate safety net so a
  misread (or a phone with OCR issues) never blocks recording a payment.

## Project structure (additions this phase in **bold**)

```
app/src/main/java/com/pakkakhata/app/
├── data/
│   ├── local/
│   │   ├── **EvidenceFileStore.kt**    saves captured/picked images locally
│   │   └── ...
│   └── repository/
│       ├── **PaymentRepository.kt**    wraps PaymentDao
│       ├── LedgerRepository.kt         **+ recordPaymentEvidence(...)**
│       └── ...
├── services/
│   ├── ocr/
│   │   ├── **MlKitOcrService.kt**      real on-device OCR implementation
│   │   ├── **UpiReceiptParser.kt**     pure regex text parser (unit tested)
│   │   ├── OcrService.kt               result model refined
│   │   └── StubOcrService.kt           kept for tests/tooling
│   └── matching/                       unchanged — now actually called
├── ui/
│   ├── **paymentevidence/**
│   │   ├── **PaymentEvidenceScreen.kt**
│   │   └── **PaymentEvidenceViewModel.kt**
│   └── ledger/CustomerLedgerScreen.kt  **+ "Record Payment" FAB**
└── navigation/                          **+ PaymentEvidence route**
```

## Building the project

As with the foundation phase, this was authored in an environment without
network access, so it has **not been compiled or run** — there was no way to
download the new dependencies (ML Kit, the coroutines-Play-Services bridge)
to verify the build. Open it in a recent Android Studio and let Gradle sync;
in addition to the foundation phase's dependencies it will now also pull:

- `com.google.mlkit:text-recognition:16.0.0` (bundled on-device OCR model)
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3` (bridges
  ML Kit's `Task<T>` to a suspend function via `.await()`)

Everything was reviewed by hand for balanced braces/parens across all 46
Kotlin files and cross-checked so every `R.string.*` reference used in code
has a matching entry in `strings.xml` — but Android Studio's first real
Gradle sync is still the actual first compile.

## Suggested next implementation order

1. **Multi-obligation fuzzy matching** — when a customer has several open
   obligations, decide which one a payment belongs to (today it's always
   the oldest).
2. **Real `VoiceCaptureService`** — on-device ASR + a small NLU parser to
   fill the Add Credit form from a spoken sentence.
3. **Payment history in the Customer Ledger screen** — currently only
   obligations are listed there; showing matched payments alongside them
   (using the already-built `PaymentRepository.observeForCustomer`) would
   close the loop visually.
4. If OCR testing surfaces accuracy problems: swap `TakePicturePreview` for
   full-resolution `TakePicture` + `FileProvider`.