# TMDB Catalog

Fondasi aplikasi Android native: Kotlin, Compose Material 3, Navigation Compose dengan route `@Serializable`, Hilt/KSP, serta pemisahan data/domain/presentation.

## Menjalankan

1. Gunakan JDK 17 dan Android SDK Platform 35.
2. Isi `TMDB_API_TOKEN` di `local.properties` dengan API Read Access Token (Bearer) dari TMDB. Sesuaikan `sdk.dir` dengan instalasi Android SDK lokal. File ini diabaikan Git.
3. Jalankan `./gradlew tasks`, lalu `./gradlew assembleDebug testDebugUnitTest`. Pada PowerShell gunakan `./gradlew.bat`.

Token dibaca oleh Gradle ke `BuildConfig.TMDB_API_TOKEN`.

## Struktur

- `app/src/main/java/com/example/tmdbapp/data`: Retrofit, DTO, mapper, PagingSource, dan implementasi repository.
- `app/src/main/java/com/example/tmdbapp/domain`: model Kotlin dan kontrak repository; aliran pagination memakai `Flow<PagingData<T>>` dari Paging common tanpa impor Android atau Retrofit.
- `app/src/main/java/com/example/tmdbapp/di`: modul Hilt untuk jaringan dan binding repository.
- `app/src/main/java/com/example/tmdbapp/presentation`: destination, ViewModel, state, dan komponen Compose untuk genre, discover, detail, trailer, serta reviews. Setiap file dibatasi maksimal 250 baris.
- `app/src/test/java/com/example/tmdbapp`: unit test JVM.

Layer data mendukung genre, discover berdasarkan filter `with_genres`, detail film, reviews dengan pagination, dan trailer YouTube. Kedua PagingSource mulai dari halaman 1, berhenti pada `total_pages` atau hasil kosong, serta mengembalikan load error untuk kegagalan jaringan, HTTP, dan decoding JSON. Pager memakai ukuran halaman 20 tanpa placeholder.

Operasi non-paging mengembalikan `Result`; pembatalan coroutine tetap dilempar. `getMovieTrailer()` mengembalikan `Result<MovieVideo?>`: memilih trailer YouTube dengan key valid, memprioritaskan video resmi, dan menggunakan trailer non-resmi sebagai fallback. Jika tidak ada trailer yang valid, hasil sukses bernilai `null`; kegagalan jaringan tetap `Result.failure`. Semua request menggunakan base URL `https://api.themoviedb.org/3/`; interceptor memasang Bearer token hanya ke origin HTTPS TMDB.

Navigasi menggunakan route `@Serializable`: genre → discover berdasarkan `genreId` → detail berdasarkan `movieId`. ViewModel membaca argumen melalui `SavedStateHandle.toRoute()`. Layar genre mendukung loading/success/empty/error dan retry. Discover serta reviews menggunakan `collectAsLazyPagingItems()` dengan initial/append loading, empty, dan retry untuk load error.

Halaman detail menampilkan backdrop, poster, rating, tanggal rilis, durasi, genre, dan sinopsis. Trailer dan reviews tetap memiliki state error terpisah. Trailer pertama dengan prioritas video resmi ditampilkan memakai Android YouTube Player melalui `AndroidView`, terdaftar pada `LocalLifecycleOwner`, dan dilepas saat komposisi berakhir. Video disiapkan tanpa autoplay dan posisi terakhir disimpan ke `SavedStateHandle`.

State layar berada di ViewModel; flow Paging memakai `cachedIn(viewModelScope)`. State scroll list/grid disimpan oleh `rememberLazyListState`/`rememberLazyGridState`, dan NavController memulihkan back stack. Komponen tampilan menerima data dan callback, sedangkan destination menghubungkan lifecycle dan ViewModel. Tidak ada data film dummy atau UseCase passthrough di `src/main`.

Versi dependency dikunci dalam `gradle/libs.versions.toml` agar kompatibel dengan compileSdk 35. Wrapper menggunakan Gradle 8.9 dengan checksum SHA-256 distribusi resmi.

## Verifikasi fondasi

- `chmod +x gradlew && ./gradlew tasks`: `BUILD SUCCESSFUL`.
- `gradlew.bat compileDebugKotlin assembleDebug testDebugUnitTest --console=plain`: `BUILD SUCCESSFUL`.
- Layer data memiliki `DiscoverMoviesPagingSourceTest`, `MovieReviewsPagingSourceTest`, `MovieRepositoryImplTest`, dan `NetworkContractTest` untuk pagination, error, cancellation, pemilihan trailer nullable, dan kontrak request/JSON tanpa koneksi API langsung.
- Verifikasi layer data: `./gradlew compileDebugKotlin testDebugUnitTest --console=plain` menghasilkan `BUILD SUCCESSFUL`.
- Verifikasi presentation: `./gradlew assembleDebug --console=plain` menghasilkan `BUILD SUCCESSFUL`.
- Test ViewModel memakai Turbine, MockK, JUnit4, dan test dispatcher coroutine untuk memeriksa urutan loading/success/error/empty dan retry. Flow discover/reviews dikonsumsi melalui presenter Paging dengan status muat nyata. Test juga mencakup pembatalan request lama, argumen type-safe dari saved state, koleksi ulang Paging, pemuatan detail/trailer secara independen, dan penyimpanan posisi trailer. Robolectric dipakai untuk test yang membutuhkan Android `Bundle`.
- Verifikasi terbaru: `./gradlew testDebugUnitTest assembleDebug lintDebug --console=plain` menghasilkan `BUILD SUCCESSFUL`; seluruh 55 test lulus, tanpa failure, error, atau test yang dilewati. Laporan HTML tersedia di `app/build/reports/tests/testDebugUnitTest/index.html`.
- `NavigationFlowTest` menjalankan MainActivity, layar Compose, navigasi type-safe, dan ViewModel Hilt melalui Robolectric dengan repository simulasi. Skenarionya mencakup genre → discover → detail → Back, error/retry setiap tahap, genre/film/reviews kosong, trailer tidak tersedia, nama reviewer kosong, serta recreation Activity dengan state dan scroll yang dipulihkan.
- UI dan runtime YouTube secara langsung belum diuji pada perangkat/emulator; tidak ada perangkat ADB terhubung saat verifikasi.
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Audit source

Teks UI berada di string/plural resource, warna placeholder dan ikon berada di resource siang/malam. Source dan test bebas komentar yang tidak perlu, unused import, `TODO()`, `Log.d`, fungsi kosong, serta data film dummy di `src/main`. Event navigasi dibatasi ke destination yang berada pada lifecycle `RESUMED`. Seluruh file Kotlin dibatasi maksimal 250 baris.

Audit mencakup 61 file di `src/main` dan `src/test`. Lint selesai dengan 0 error dan 0 temuan pada source/test; saran pembaruan AGP/dependency dalam version catalog tetap dilaporkan. Versi yang dikunci dipertahankan untuk kompatibilitas compileSdk 35. Laporan tersedia di `app/build/reports/lint-results-debug.html`.
