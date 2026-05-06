# Ripple CI - Developer & Refactoring Guide

This document serves as the primary technical reference for the Ripple CI Android project. It explains the current architecture, the reason behind the recent major refactor, and the standard process for adding new features.

---

## 1. The "Great Refactor": Resolving Package Conflicts

### The Issue
Previously, the project suffered from `Conflicting overloads` and "Duplicate Class" errors. This happened because source files were placed in two locations:
1. `src/main/java/[feature]/` (Incorrect - missing package namespace)
2. `src/main/java/com/example/rippleci/[feature]/` (Correct)

### The Solution
We consolidated all code into the `com.example.rippleci` namespace. 
* **Note to Team:** Never create folders directly under `java/`. Always nest them inside `com/example/rippleci/`.

---

## 2. Project Architecture & Directory Structure

We use the **MVVM (Model-View-ViewModel)** pattern combined with a simplified **Clean Architecture** approach.

### Directory Tree Breakdown
```text
app/src/main/java/com/example/rippleci/
├── data/               # DATA LAYER
│   ├── SchoolEvent.kt       # Data Models (POJOs/DTOs)
│   └── TrumbaApiService.kt  # Network logic (Retrofit interfaces)
├── ui/                 # UI LAYER
│   ├── components/          # Reusable UI widgets (Cards, Buttons, Loaders)
│   │   └── EventCard.kt     # Stateless UI for displaying a single event
│   └── events/              # Feature-specific UI (Events Feature)
│       ├── EventsScreen.kt  # The "View" (Compose screen)
│       └── EventsViewModel.kt # The "Logic" (State management)
└── MainActivity.kt     # Entry point & Navigation setup
```

### File Purposes
*   **`data/`**: Your source of truth. If we switch from a Web API to a Local Database, only this folder should change.
*   **`ViewModel`**: The brain of the screen. It handles network calls and converts raw data into "UI State" (e.g., show a loading spinner or an error message).
*   **`Screen.kt`**: Purely visual. It "observes" the ViewModel and draws whatever the state says.
*   **`components/`**: Atomic UI pieces. Keeping `EventCard` separate from `EventsScreen` allows us to reuse that card in a "Search" screen or "Favorites" screen later.

---

## 3. Tech Stack Reference
*   **UI**: Jetpack Compose (Declarative UI)
*   **Networking**: Retrofit + OkHttp
*   **JSON Parsing**: KotlinX Serialization (Faster and safer than GSON)
*   **Images**: Coil (Loading images from URLs)
*   **Asynchrony**: Kotlin Coroutines & Flow (For non-blocking UI)

---

## 4. How to Implement a New Feature (Step-by-Step)

To add a new feature (e.g., "Campus News"), follow this checklist:

### Step 1: Define the Data Model (`data/`)
Create `NewsArticle.kt` with `@Serializable`.
```kotlin
@Serializable
data class NewsArticle(val id: Int, val title: String, val content: String)
```

### Step 2: Update the API Service (`data/`)
Add a function to `TrumbaApiService.kt` (or create a new service interface):
```kotlin
@GET("news-endpoint")
suspend fun getNews(): List<NewsArticle>
```

### Step 3: Create the ViewModel (`ui/news/`)
Handle the three states: **Loading**, **Success**, and **Error**.
```kotlin
class NewsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    // Logic to fetch data...
}
```

### Step 4: Build the UI (`ui/news/`)
Create the Compose screen and use `collectAsState()` to listen to the ViewModel.

---

## 5. Maintenance Best Practices
1. **Package Consistency**: Every new file must start with `package com.example.rippleci...`.
2. **Build Checks**: Before pushing code, run `./gradlew :app:compileDebugKotlin` to ensure no overlapping definitions exist.
3. **Strings & Formatting**: Use `HtmlCompat` for any strings coming from the Trumba API, as they often contain HTML tags.
