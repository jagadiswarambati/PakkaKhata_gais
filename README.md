# PakkaKhata — The Ledger That Settles Itself

> **A phone-first, offline-first credit ledger that connects informal credit with real payment evidence.**

PakkaKhata is an Android application designed for small shopkeepers who manage customer credit (`udhaar`). Instead of only recording that a customer owes money, PakkaKhata connects the original credit obligation with payment evidence received later.

A shopkeeper can create a credit entry using **voice or manual input**, capture a **UPI payment screenshot/photo**, extract payment information using **on-device OCR**, and let an **explainable local reconciliation engine** determine which open credit the payment belongs to.
**LATEST UPDATED VERSION:** 
**https://drive.google.com/file/d/1SOnFHur7Mmxn9uGGY7IhfNLm8RqOHC4u/view?usp=sharing**


The result can be:

- **Fully Settled**
- **Partially Settled**
- **Overpaid**
- **No Match**

The core workflow runs locally on the device, with no backend or external AI/LLM API required at runtime.

---

## Table of Contents

- [Problem](#problem)
- [Solution](#solution)
- [Why PakkaKhata](#why-pakkakhata)
- [Core Workflow](#core-workflow)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Reconciliation Engine](#reconciliation-engine)
- [On-Device Perception](#on-device-perception)
- [Data Model](#data-model)
- [Atomic Settlement](#atomic-settlement)
- [Offline-First Design](#offline-first-design)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [User Journey](#user-journey)
- [Demo Flow](#demo-flow)
- [Testing](#testing)
- [Security & Privacy](#security--privacy)
- [Setup & Run](#setup--run)
- [Known Limitations](#known-limitations)
- [Future Scope](#future-scope)
- [Design Principles](#design-principles)
- [Hackathon Context](#hackathon-context)
- [License](#license)

---

## Problem

Small shopkeepers frequently extend informal credit to customers.

The transaction may happen verbally:

> "Ramesh, take these groceries now and pay later."

The payment arrives later, often through UPI.

The difficult part is not recording the original credit. The difficult part is **connecting the later payment to the correct outstanding obligation**.

Common complications include:

- Partial payments
- Overpayments
- Different names between the credit record and UPI payment
- Payment screenshots containing OCR noise
- Multiple outstanding credits
- Duplicate payment evidence
- Manual reconciliation errors
- Dependence on internet-connected accounting systems

Traditional paper ledgers and basic digital khata applications can record transactions, but the payment-evidence reconciliation step still requires manual work.

---

## Solution

PakkaKhata turns the ledger into an evidence-driven reconciliation workflow.

Instead of:

```text
Credit → Wait → Manually remember payment → Manually update balance
```

PakkaKhata uses:

```text
Credit
  ↓
Open Obligation
  ↓
Payment arrives later
  ↓
Capture payment evidence
  ↓
On-device OCR
  ↓
Extract amount + payer + reference + metadata
  ↓
Duplicate check
  ↓
Reconciliation engine
  ↓
Auto Match / Suggested Match / No Match
  ↓
Settlement calculation
  ↓
Atomic ledger update
```

### Example

A customer receives **₹500** worth of goods on credit.

Later, the shopkeeper captures evidence of a **₹300** payment.

PakkaKhata can:

1. Extract the payment amount and payer information.
2. Find the relevant open obligation.
3. Calculate the payment against the ₹500 balance.
4. Mark the obligation as **Partially Settled**.
5. Show **₹200 remaining**.
6. Store the payment evidence and reconciliation relationship locally.

---

## Why PakkaKhata

The differentiator is not simply digital bookkeeping.

The core idea is:

> **Connect the original credit obligation with the payment evidence that arrives later.**

The application combines:

- Voice-first credit creation
- Camera-based payment evidence capture
- On-device OCR
- India/UPI-aware payment parsing
- Fuzzy identity matching
- Amount and timing analysis
- Duplicate detection
- Confidence-tiered reconciliation
- Partial/full/overpayment settlement
- Atomic local transactions
- Customer-level audit history

The reconciliation engine is intentionally deterministic and explainable rather than presenting a black-box prediction.

---

# Core Workflow

```text
┌──────────────────────┐
│  Voice / Manual Credit│
│       Entry           │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│   Open Obligation     │
│   Customer + Amount   │
└──────────┬───────────┘
           ↓
      Payment arrives
           ↓
┌──────────────────────┐
│ Camera / Photo Picker │
│ Payment Evidence      │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│   On-device OCR       │
│ Amount / Name / UTR   │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Payment Evidence      │
│ Parser + Normalizer   │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Duplicate Detection   │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Reconciliation Engine │
│ Name + Amount + Time  │
└──────────┬───────────┘
           ↓
     ┌─────┼──────┐
     ↓     ↓      ↓
   AUTO  SUGGEST  NO MATCH
   MATCH  MATCH
     ↓     ↓
     └──→ Confirmation
           ↓
┌──────────────────────┐
│ Settlement Calculator │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Atomic Room Transaction│
│ Evidence + Obligation │
│ Reconciliation + Balance│
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Updated Customer      │
│ Ledger + Balance      │
└──────────────────────┘
```

---

# Key Features

## 1. Voice Credit Entry

Shopkeepers can create credit entries using spoken input.

Example:

> `Ramesh ko paanch sau rupaye udhar`

The local parser extracts:

- Customer name
- Amount
- Optional note

Supported spoken number patterns include English/Hindi/Hinglish number words such as:

- `five hundred` → ₹500
- `paanch sau` → ₹500
- `hazaar` → ₹1,000

A confirmation/edit step is shown before the obligation is saved.

> **Important:** Android `SpeechRecognizer` is offline-preferring, but offline availability depends on the device and installed speech-recognition language packs. Manual entry remains available as a fallback.

---

## 2. Manual Credit Entry

Credit can also be entered without voice.

This provides a reliable fallback when:

- Speech recognition is unavailable
- The environment is noisy
- The user prefers typing
- The device does not have an appropriate offline language pack

---

## 3. Camera-Based Payment Evidence

PakkaKhata uses **CameraX** for an in-app camera experience.

The shopkeeper can capture payment evidence such as:

- UPI payment screenshots
- Payment confirmation images
- Photographed payment screens

---

## 4. On-Device OCR

Payment evidence is processed using **Google ML Kit Text Recognition** with a bundled on-device model.

The parser can extract information such as:

- Amount
- Sender/payer name
- UPI identifier
- UTR/reference number
- Date/time
- Payment application

The parser is designed around common Indian payment formats and OCR noise.

---

## 5. Explainable Reconciliation

Instead of returning only a match/no-match result, the reconciliation engine exposes the signals behind a candidate.

The engine considers:

| Signal | Weight |
|---|---:|
| Name similarity | 0.65 |
| Amount fit | 0.30 |
| Transaction timing | 0.05 |

The system then applies an identity guardrail so that a weak identity match cannot become a strong match simply because the payment amount happens to fit.

The Match Review screen can show reasons such as:

```text
Strong name match
Payment is compatible with a partial settlement
Transaction occurred after the obligation was created
```

---

## 6. Confidence-Tiered Matching

The engine produces three matching decisions:

### Auto Match

High-confidence evidence can proceed directly toward settlement.

### Suggested Match

Medium-confidence evidence requires shopkeeper confirmation.

### No Match

Low-confidence evidence is not automatically attributed to an obligation.

This reduces the risk of silently applying a payment to the wrong customer.

---

## 7. Duplicate Payment Detection

Before reconciliation, PakkaKhata checks for previously processed payment evidence.

Detection can use:

- Existing UTR/reference number
- Amount + sender + timestamp fingerprint

A detected duplicate is blocked instead of being reconciled again.

---

## 8. Partial Payment

Example:

```text
Original credit: ₹500
Payment:         ₹300
Remaining:       ₹200
Status:          PARTIALLY_SETTLED
```

---

## 9. Full Settlement

Example:

```text
Original credit: ₹500
Payment:         ₹500
Remaining:       ₹0
Status:          FULLY_SETTLED
```

---

## 10. Overpayment Handling

Example:

```text
Original balance: ₹500
Payment:          ₹600
Excess:           ₹100
Status:           OVERPAID
```

The settlement calculator handles this explicitly instead of treating every payment as a simple subtraction.

---

## 11. Manual Payment Recording

The ledger also supports directly recording a payment against an existing customer obligation.

The same settlement and duplicate-protection path is reused so manual payment actions do not bypass the core accounting rules.

---

## 12. Customer Ledger

Each customer can have a detailed history containing:

- Open obligations
- Payments
- Settlement status
- Remaining balance
- Reconciliation information
- Payment evidence history

---

## 13. Local Export & Sharing

The app can generate a formatted ledger or settlement summary and hand it to another application using standard Android capabilities:

- Clipboard
- Android share sheet

This is implemented using standard Android APIs rather than a proprietary iQOO Office Kit SDK.

---

## 14. Demo Mode

The application includes demo scaffolding to make repeated demonstrations predictable.

A canonical scenario can be seeded and the ledger can be reset before another demo run.

---

# Architecture

PakkaKhata follows a layered architecture combining Android presentation patterns with a pure Kotlin domain layer.

```text
                    ┌─────────────────────────┐
                    │      Presentation        │
                    │ Compose + ViewModels     │
                    └────────────┬────────────┘
                                 │
                                 ↓
                    ┌─────────────────────────┐
                    │       Use Cases         │
                    │ Evaluate / Execute       │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    ↓                         ↓
          ┌──────────────────┐      ┌──────────────────┐
          │ Reconciliation  │      │    Perception    │
          │     Engine       │      │ Speech / OCR     │
          └────────┬─────────┘      └────────┬─────────┘
                   │                         │
                   └────────────┬────────────┘
                                ↓
                    ┌─────────────────────────┐
                    │      Repository Layer   │
                    └────────────┬────────────┘
                                 ↓
                    ┌─────────────────────────┐
                    │       Room Database      │
                    │     Local SQLite DB      │
                    └─────────────────────────┘
```

### Presentation Layer

Responsible for:

- Jetpack Compose UI
- ViewModels
- Navigation
- User interaction
- Match review
- Settlement result
- Customer ledger

### Domain Layer

Contains the core business logic without Android dependencies:

- Domain models
- Money value type
- Reconciliation engine
- Fuzzy matching
- Duplicate detection
- Settlement calculation
- Voice parser
- Payment evidence parser
- Use cases

### Data Layer

Responsible for Android-facing implementations:

- Room database
- DAOs
- Entities
- Repository implementations
- ML Kit OCR provider
- Android SpeechRecognizer provider

### Office Bridge Layer

Provides local:

- Clipboard export
- Android share-sheet export
- Report generation

---

# Reconciliation Engine

The reconciliation engine is deliberately deterministic.

## Step 1 — Duplicate Check

The evidence is checked against previously processed evidence.

```text
Existing UTR/reference
        OR
Amount + sender + timestamp fingerprint
        ↓
Duplicate → Reject
```

## Step 2 — Candidate Search

Open obligations with positive remaining balances are considered.

The engine can evaluate obligations that are:

- `OPEN`
- `PARTIALLY_SETTLED`

## Step 3 — Candidate Scoring

### Name Similarity — 65%

Uses:

- Normalized exact matching
- Token containment
- Levenshtein similarity
- Token-level alignment

Common punctuation, case differences, and selected Indian honorifics are normalized.

### Amount Fit — 30%

Considers:

- Exact payment
- Compatible partial payment
- Overpayment

### Transaction Timing — 5%

Payments occurring after the obligation was created receive stronger timing compatibility.

## Step 4 — Identity Guardrail

If the name similarity is below the configured minimum, the overall candidate score is zeroed.

This prevents amount-only matching from attributing money to an unrelated customer.

## Step 5 — Decision Threshold

The highest-scoring candidate is classified as:

```text
AUTO_MATCH
SUGGESTED_MATCH
NO_MATCH
```

based on configurable thresholds.

## Step 6 — Settlement

The settlement calculator determines:

```text
FULLY_SETTLED
PARTIALLY_SETTLED
OVERPAID
NO_MATCH
```

---

# On-Device Perception

PakkaKhata separates perception from business logic through interfaces.

```text
SpeechProvider
      ↓
AndroidSpeechRecognizerProvider
      ↓
VoiceCreditParser
      ↓
Credit Obligation
```

and:

```text
OCRProvider
      ↓
MlKitOcrProvider
      ↓
PaymentEvidenceParser
      ↓
Payment Evidence
```

This separation makes the perception layer replaceable without changing the reconciliation engine.

For example, a future speech or OCR provider can be introduced without rewriting the settlement logic.

---

# Data Model

The local Room database is centered around four major entities:

```text
Customer
   │
   ├──────────< Obligation
   │
   └──────────< PaymentEvidence
                    │
                    └────────── Reconciliation
```

### Customer

Stores customer identity and running outstanding balance.

### Obligation

Represents the original credit transaction.

Typical lifecycle:

```text
OPEN
  ↓
PARTIALLY_SETTLED
  ↓
FULLY_SETTLED
```

An obligation can also participate in an `OVERPAID` settlement outcome.

### PaymentEvidence

Stores extracted payment information such as:

- Amount
- Sender
- UPI/reference information
- Timestamp
- Payment application
- Local evidence information

### Reconciliation

Connects payment evidence to the obligation that it was reconciled against.

---

# Money Handling

Financial amounts are represented using an integer-paise `Money` value type.

For example:

```text
₹500.00
   ↓
50000 paise
```

This avoids floating-point arithmetic for balances and settlement calculations.

---

# Atomic Settlement

A successful settlement is applied inside a Room transaction.

Conceptually:

```text
BEGIN TRANSACTION

Insert / update payment evidence
        ↓
Update obligation remaining amount
        ↓
Update obligation status
        ↓
Insert reconciliation record
        ↓
Update customer running balance

COMMIT
```

If the transaction cannot complete, the related updates do not partially commit.

This protects the consistency of the local ledger.

---

# Offline-First Design

PakkaKhata is designed so that the core accounting workflow does not depend on a backend.

### Local

- Customer data
- Credit obligations
- Payment evidence
- Reconciliation records
- Balances
- Settlement calculations
- OCR processing
- Reconciliation logic

### Network-independent core

```text
UI
 ↓
Domain logic
 ↓
Room
 ↓
Local device
```

There is:

- No backend API required by the core application
- No external database
- No third-party AI/LLM API
- No network-dependent reconciliation service

### Important Voice Limitation

OCR is bundled and genuinely on-device.

Android speech recognition is **offline-preferring**, not universally guaranteed to work offline. Availability depends on the device and installed speech-recognition language packs.

Manual entry is always available as a fallback.

---

# Technology Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| UI Design | Material 3 |
| Architecture | Layered / MVVM-oriented |
| Local Database | Room |
| Database Backend | SQLite via Room |
| Navigation | Navigation Compose |
| Camera | CameraX |
| OCR | Google ML Kit Text Recognition |
| Voice | Android SpeechRecognizer |
| Async | Kotlin Coroutines + Flow |
| Image Loading | Coil |
| Unit Testing | JUnit 4 |
| Android Integration Testing | Robolectric |
| Compose Screenshot Testing | Roborazzi |
| Build | Gradle / Android Gradle Plugin |
| Minimum Android | API 24 |
| Compile SDK | 36 |

---

# Project Structure

A simplified project structure:

```text
app/
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │
    │   ├── presentation/
    │   │   ├── screens/
    │   │   │   ├── home/
    │   │   │   ├── evidence/
    │   │   │   ├── reconciliation/
    │   │   │   └── customer/
    │   │   └── navigation/
    │   │
    │   ├── domain/
    │   │   ├── model/
    │   │   ├── reconciliation/
    │   │   ├── perception/
    │   │   ├── usecase/
    │   │   └── officebridge/
    │   │
    │   └── data/
    │       ├── local/
    │       ├── perception/
    │       └── repository/
    │
    └── test/
        ├── reconciliation/
        ├── perception/
        ├── database/
        └── integration/
```

The exact package names may vary slightly with implementation, but the architectural responsibilities remain separated.

---

# User Journey

## Create Credit

```text
Home
  ↓
Voice Credit / Manual Entry
  ↓
Review
  ↓
Confirm
  ↓
Open Obligation
```

## Record Payment

```text
Payment Capture
  ↓
CameraX
  ↓
OCR
  ↓
Payment Evidence Review
  ↓
Save Evidence
```

## Reconcile

```text
Saved Evidence
  ↓
Duplicate Check
  ↓
Candidate Search
  ↓
Name + Amount + Timing
  ↓
Match Review
```

## Settle

```text
Confirm Match
  ↓
Settlement Calculator
  ↓
Atomic Room Transaction
  ↓
Settlement Result
  ↓
Updated Customer Balance
```

---

# Demo Flow

A representative demonstration can follow this sequence:

### 1. Create a Credit

Create:

```text
Customer: Ramesh Kumar
Credit:   ₹500
Status:   OPEN
```

This can be done through voice or manual entry.

### 2. Capture Payment Evidence

Capture a payment screenshot/photo containing:

```text
Amount
Sender
UPI/reference
```

### 3. Review OCR

Verify or edit the extracted payment information.

### 4. Match

Open Match Review and show:

- Candidate customer
- Name similarity
- Amount compatibility
- Timing
- Match reasons
- Decision tier

### 5. Settle

For a ₹300 payment:

```text
Original credit: ₹500
Payment:         ₹300
Remaining:       ₹200
Status:          PARTIALLY_SETTLED
```

### 6. Verify the Ledger

Return to the customer ledger and show the updated balance.

### 7. Duplicate Demonstration

Submit the same payment evidence again.

The duplicate detector should identify the repeated UTR/reference or fingerprint and prevent another settlement.

---

# Testing

The project includes tests across business logic, parsing, persistence, integration, and UI state.

The test suite covers areas including:

### Reconciliation

- Name similarity
- Amount scoring
- Timing scoring
- Identity guardrail
- Auto-match
- Suggested match
- No-match
- Full settlement
- Partial settlement
- Overpayment
- Duplicate detection

### Payment Evidence Parsing

- Currency formats
- UTR/reference extraction
- Sender extraction
- OCR variations
- Payment-app patterns

### Voice Parsing

- English number words
- Hindi number words
- Hinglish phrases
- Amount extraction
- Customer extraction

### Database

- Room persistence
- Customer records
- Obligations
- Payment evidence
- Reconciliation records
- Atomic settlement

### UI / State

- Ledger state
- Customer detail state
- Settlement flow state

Run the test suite with:

```bash
./gradlew test
```

---

# Setup & Run

## Requirements

- Android Studio
- JDK 17+
- Android SDK with API 36 available
- Android device or emulator
- Minimum Android API 24

A physical Android device is recommended for realistic:

- Microphone testing
- Camera testing
- OCR testing
- Payment-evidence capture

## Steps

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd pakkakhata_gais
```

### 2. Open in Android Studio

Open the project root and allow Gradle to sync.

### 3. Build

```bash
./gradlew assembleDebug
```

### 4. Run

Install the generated debug APK on a compatible Android device.

### 5. Permissions

The application may request:

- Camera permission
- Microphone permission

Camera is used for payment-evidence capture.

Microphone is used for voice credit entry.

---

# Privacy & Data Handling

PakkaKhata follows a local-first data model.

The core application does not require customer or payment information to be uploaded to a backend.

Sensitive financial information such as:

- Customer names
- Credit balances
- Payment evidence
- UTR/reference information
- Reconciliation records

is stored in the application's local Room database.

The payment-evidence workflow is designed around local processing.

---

# Known Limitations

PakkaKhata is a hackathon prototype and intentionally keeps its scope focused.

### Voice Offline Availability

Android SpeechRecognizer can prefer offline recognition, but actual offline availability depends on the device and installed language pack.

### One Evidence → One Obligation

One payment evidence item currently resolves to one open obligation.

Splitting a single payment across multiple obligations is not implemented.

### Deterministic Reconciliation

The reconciliation engine is a hand-written scoring and rules engine.

It is not a trained machine-learning model.

The pre-trained perception component in the core pipeline is ML Kit's OCR model.

### Office Bridge

The export/sharing functionality uses standard Android:

- Clipboard APIs
- Android share sheet

It is **not** a proprietary iQOO Office Kit SDK integration.

### Local-Only Data

Because the application is offline-first and has no backend synchronization, multi-device cloud synchronization is outside the current scope.

---

# Future Scope

The current architecture leaves clear extension points for future versions.

## 1. Smarter On-Device Intelligence

Future versions could introduce a compact on-device model for:

- Better entity extraction
- More robust noisy speech understanding
- Context-aware payment classification
- Improved matching suggestions

The deterministic reconciliation engine can remain as a safety layer around model-generated suggestions.

---

## 2. Better Multilingual Voice

Expand support for:

- Telugu
- Hindi
- English
- Hinglish
- Telugu-English mixed speech

The `SpeechProvider` abstraction allows alternate speech implementations without changing the core ledger logic.

---

## 3. Multi-Obligation Payment Allocation

A future settlement engine could split one payment across multiple outstanding obligations.

Example:

```text
Obligation A: ₹300
Obligation B: ₹400

Payment: ₹500

Allocation:
A → ₹300
B → ₹200
```

---

## 4. Secure Device-to-Device Sync

A future version could support encrypted synchronization between:

- Shopkeeper phone
- Backup phone
- Laptop
- Tablet

while preserving the local-first operating model.

---

## 5. Encrypted Backup & Restore

Possible future capabilities:

- Encrypted local backup
- Export/import
- Device migration
- Recovery after phone replacement

---

## 6. Better Payment Evidence Understanding

Future parsers could support a broader set of:

- UPI applications
- Bank payment formats
- Regional layouts
- OCR noise patterns
- Payment confirmation screens

---

## 7. Advanced Duplicate Detection

Future versions could combine:

- UTR/reference
- Amount
- Sender
- Timestamp
- Image similarity
- Transaction context

to strengthen duplicate protection when explicit references are unavailable.

---

## 8. Audit & Business Insights

Future versions could provide local analytics such as:

- Outstanding credit trends
- Payment frequency
- Partial-payment patterns
- Overpayment history
- Customer settlement timelines

---

## 9. Multi-Store Support

For larger merchants, future versions could introduce:

```text
Local Store
    ↓
Encrypted Sync
    ↓
Multiple Devices / Branches
```

while keeping the single-device offline experience as the baseline.

---

# Design Principles

PakkaKhata is built around a few principles:

### Phone-first

The phone is the primary interaction device because it already provides:

- Microphone
- Camera
- Local storage
- OCR
- Voice input

### Offline-first

The core ledger and reconciliation workflow should not depend on network availability.

### Evidence-driven

A payment is not merely a number entered into the ledger. Payment evidence can be captured and linked to the resulting settlement.

### Explainable

The system should show why a candidate was selected instead of exposing only a hidden score.

### Deterministic Core

Financial settlement should be predictable, testable, and auditable.

### Safe by Default

Ambiguous evidence should not silently settle against an unrelated customer.

### Modular Perception

Speech and OCR providers are abstracted so the core business logic does not depend on one perception implementation.

---

# Hackathon Context

**PakkaKhata** was developed for the **iQOO Hackathon 2026 — Hyderabad City Battle**.

The project focuses on demonstrating how a smartphone can be used as an intelligent, offline-first business tool for real-world merchant workflows.

The implementation emphasizes:

- Real phone interaction
- Camera
- Microphone
- On-device OCR
- Local persistence
- Explainable reconciliation
- Reliable settlement
- A complete end-to-end user journey

---

# What PakkaKhata Does Not Claim

To keep the technical description accurate:

- It does **not** use a cloud LLM at runtime.
- It does **not** use a trained ML model for reconciliation.
- It does **not** guarantee offline speech recognition on every Android device.
- It does **not** use a proprietary iQOO Office Kit SDK.
- It does **not** split one payment across multiple obligations.
- It does **not** replace a complete accounting/GST system.

These boundaries are intentional so that the demonstrated functionality matches the actual implementation.

---

# Project Status

**Prototype — Hackathon Ready**

The core end-to-end workflow is implemented:

```text
Voice / Manual Credit
        ↓
Open Obligation
        ↓
Payment Evidence Capture
        ↓
On-device OCR
        ↓
Evidence Parsing
        ↓
Duplicate Detection
        ↓
Reconciliation
        ↓
Match Review
        ↓
Settlement
        ↓
Updated Customer Ledger
```

---

# License

Add the project's chosen license here before public distribution.

---

## Built with Kotlin, Jetpack Compose, Room, CameraX and on-device ML Kit OCR.

**PakkaKhata — The Ledger That Settles Itself.**
