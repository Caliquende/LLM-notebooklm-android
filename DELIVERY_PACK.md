# ResearchFlow Android V1 — Teslim Paketi

> Selin (CEO-Dispatcher) koordinasyonunda, tek koşuda üretilmiş teslim paketi.
> Tarih: 2026-04-23

---

## 1. Kısa Ürün Tanımı

**ResearchFlow**: Android native, chat benzeri research orchestrator. Kullanıcı konu girer → LLM ile web araştırması yapılır → kaynaklar otomatik olarak NotebookLM notebook'una eklenir → istenirse artifact üretilir. CLI'deki 3 aşamalı akışı (search → handoff → artifacts) mobilde tek akışa indirir.

---

## 2. Hedef Kullanıcı

AI araçlarını aktif kullanan, araştırma yapan power user. NotebookLM'i zaten bilen veya öğrenmeye açık, teknik seviyesi orta-üst.

---

## 3. Çözdüğü Ana Problem

CLI'de sırayla `search <query>` → `notebooklm-handoff` → `notebooklm-artifacts` çalıştırmak zahmetli. Mobilde tek mesajla tüm pipeline tetiklenmeli ve arka planda tamamlanmalı.

---

## 4. V1 Kapsamı

| Kapsam İçi | Detay |
|---|---|
| Google Sign-In | Tek hesap, Firebase Auth |
| Provider/API Key yönetimi | Settings'te BYOK (Bring Your Own Key) |
| Chat benzeri ana ekran | "Neyi araştırmak istersin?" |
| Web research pipeline | Seçilen provider ile arama |
| NotebookLM bridge | Kaynakları notebook'a ekleme |
| Artifact seçimi ve üretimi | Research sonrası kutucuklar |
| Background job | WorkManager ile arka plan işlem |
| Bildirim | İş bitince app notification |
| Thread arşivi | Read-only geçmiş |
| Hata durumu | Basit hata mesajı |
| Kaynak limiti | Default 15, kullanıcı ayarlayabilir |
| Duplicate engelleme | URL bazlı kontrol |

---

## 5. V1 Dışı Kalanlar

- iOS sürümü
- Web sürümü
- Admin panel / billing panel
- Multi-tenant / takım collaboration
- Advanced analytics / dashboard
- Notebook içi sohbet (NotebookLM'in kendi chat'i)
- Çoklu Google hesap desteği
- Offline araştırma
- Kaynak önizleme / reader modu
- Thread devam ettirme (eski thread'ler read-only)
- Özel LLM model fine-tuning

---

## 6. Ana Kullanıcı Akışı

```
1. Uygulama açılır
2. Google Sign-In (ilk kez)
3. API key yoksa → Settings'e yönlendir
4. Ana ekran: "Neyi araştırmak istersin?"
5. Kullanıcı konu yazar
6. Provider/model seçimi (input altı chip)
7. "Araştır" butonuna basar
8. Loading state: "Araştırılıyor..."
9. Web research tamamlanır
10. Kaynaklar listelenir (kaynak kartları)
11. NotebookLM'e kaynaklar eklenir (arka plan)
12. Artifact kutucukları gösterilir
    - Audio Overview, Briefing Doc, Quiz, vb.
13a. Kullanıcı artifact seçer → üretim başlar (background)
13b. Kullanıcı seçmez → notebook + kaynaklar zaten oluşmuş
14. Kullanıcı uygulamadan çıkabilir
15. İş bitince push notification
16. Thread arşive düşer (read-only)
```

**Follow-up akışı:**
```
- Aynı thread'de yeni mesaj → yeni research
- Duplicate kaynak kontrolü (URL match)
- Yeni kaynaklar mevcut notebook'a eklenir
- Kaynak limiti aşılırsa kullanıcıya uyarı
```

**Hata akışı:**
```
- Pipeline patlarsa → "Bu işlem tamamlanamadı. Yeni chat açıp tekrar dene."
- API key geçersizse → "API anahtarın geçersiz. Settings'ten kontrol et."
- İnternet yoksa → "Bağlantı yok. İnternete bağlanıp tekrar dene."
```

---

## 7. Ekran Listesi

| # | Ekran | Amaç |
|---|---|---|
| 1 | Splash | Logo + auth kontrolü |
| 2 | Google Sign-In | Firebase Auth |
| 3 | Settings | API key ekleme, provider yönetimi, kaynak limiti |
| 4 | Chat (Ana Ekran) | Araştırma başlatma, sonuç görüntüleme |
| 5 | Thread Arşivi | Geçmiş araştırmalar listesi |
| 6 | Thread Detay | Eski araştırmanın read-only görünümü |

**Olmayan ekranlar:** Ayrı notebook görünümü, admin panel, profil sayfası.

---

## 8. Thread / Session Mantığı

- Her yeni araştırma = 1 thread = 1 NotebookLM notebook session
- Thread içinde follow-up mesaj gönderilebilir
- Follow-up yeni research tetikler, aynı notebook'a eklenir
- Duplicate kaynak URL bazlı engellenir
- Thread tamamlandığında arşive düşer
- Arşivdeki thread'ler read-only, devam ettirilemez
- Yeni araştırma = yeni thread

**Veri yapısı:**
```
Thread {
  id: UUID
  userId: String
  title: String (ilk mesajdan türetilir)
  notebookId: String? (NotebookLM notebook ID)
  status: ACTIVE | COMPLETED | FAILED
  createdAt: Timestamp
  messages: List<Message>
  sources: List<Source>
  artifacts: List<Artifact>
}
```

---

## 9. Provider ve Settings Mantığı

- Kullanıcı kendi API key'lerini ekler (BYOK modeli)
- V1 desteklenen provider'lar: OpenAI, Google Gemini, Anthropic
- Her provider için key encrypted olarak local'de saklanır
- Provider/model seçimi chat ekranında input altında chip ile yapılır
- Kullanıcı yalnızca kendi eklediği provider'ları görür
- Key yoksa Settings'e yönlendirme

**Settings ekranı:**
- API Keys bölümü (provider listesi, key ekleme/silme)
- Kaynak limiti (slider, default 15, max 50)
- Google hesap bilgisi (sign-out)

---

## 10. NotebookLM Bridge Rolü

### Mevcut CLI Akışı (doğrulanmış çekirdek)
```
search-shortlist → last_search_results.json
notebooklm-handoff → notebook oluştur + kaynakları ekle
notebooklm-artifacts → artifact üret (async, non-blocking)
```

### Android'de Bridge Yaklaşımı

**KARAR: Companion/Bridge Server modeli.**

NotebookLM'in public API'si yok. CLI, Playwright browser automation kullanıyor. Android'den doğrudan browser automation yapılamaz.

**Çözüm:** Kullanıcının kendi makinasında çalışan minimal bridge server.

```
Android App ←→ Bridge Server (localhost/LAN) ←→ NotebookLM (browser automation)
```

**Bridge Server sorumlulukları:**
1. `POST /notebook/create` → notebook oluştur, ID döndür
2. `POST /notebook/{id}/sources` → URL listesi ekle
3. `POST /notebook/{id}/artifacts` → artifact üretimi başlat
4. `GET /notebook/{id}/artifacts/{taskId}/status` → durum sorgula
5. `GET /health` → bağlantı kontrolü

**Bridge Server = mevcut codex-notebooklm scriptlerinin üstüne ince HTTP katmanı.**

### Kırılganlık Uyarısı (açık söylüyorum)

> **NotebookLM bridge bu ürünün en zayıf halkasıdır.**
> - NotebookLM'in public API'si yok, browser automation'a dayanıyor
> - Google UI değişirse bridge kırılır
> - Rate limit var (50 query/gün free hesapta)
> - Her işlem browser açıp kapatıyor (yavaş)
> - Auth cookie'si expire olabilir
>
> **Risk azaltma:** Bridge server health check + retry + kullanıcıya açık hata mesajı. Google NotebookLM API açarsa bridge kaldırılır.

---

## 11. Android V1 Mimari Özeti

```
┌─────────────────────────────────────┐
│           Presentation              │
│  Jetpack Compose + Material 3       │
│  Screens → ViewModels → UiState     │
├─────────────────────────────────────┤
│            Domain                   │
│  UseCases: Research, Notebook,      │
│  Artifact, Thread                   │
├─────────────────────────────────────┤
│             Data                    │
│  Repository pattern                 │
│  Room DB (threads, sources, cache)  │
│  DataStore (settings, keys)         │
│  Retrofit (bridge server, search)   │
├─────────────────────────────────────┤
│          Infrastructure             │
│  WorkManager (background jobs)      │
│  Firebase Auth (Google Sign-In)     │
│  Hilt (DI)                          │
│  EncryptedSharedPreferences (keys)  │
└─────────────────────────────────────┘
```

**Tech Stack:**
- Kotlin, min SDK 26 (Android 8+)
- Jetpack Compose + Material 3
- Navigation Compose (type-safe)
- Hilt (DI)
- Room (local DB)
- DataStore (preferences)
- Retrofit + OkHttp (network)
- WorkManager (background jobs)
- Firebase Auth (Google Sign-In)
- EncryptedSharedPreferences (API keys)
- Kotlin Coroutines + Flow

**Mimari pattern:** MVVM + Clean Architecture (basitleştirilmiş, 3 katman)

---

## 12. Minimal Backend Gerekip Gerekmediği

**KARAR: Ayrı cloud backend GEREKMİYOR.**

Gerekçe:
- Kullanıcı verisi cihazda kalır (Room DB)
- Auth Firebase client-side
- API key'ler local encrypted storage
- NotebookLM bridge kullanıcının kendi makinasında çalışır
- Push notification için Firebase Cloud Messaging (FCM) kullanılabilir ama V1'de local notification yeterli

**Tek dış bağımlılık:** Kullanıcının kendi bridge server'ı + LLM provider API'leri.

---

## 13. Veri Modeli Özeti

```kotlin
// Room Entities
@Entity
data class ThreadEntity(
    @PrimaryKey val id: String, // UUID
    val userId: String,
    val title: String,
    val notebookId: String?,
    val status: ThreadStatus, // ACTIVE, COMPLETED, FAILED
    val providerId: String,
    val modelId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity
data class MessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: MessageRole, // USER, SYSTEM
    val content: String,
    val createdAt: Long
)

@Entity
data class SourceEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val url: String,
    val title: String,
    val type: SourceType, // WEB, YOUTUBE, ARTICLE, DOCS
    val reason: String,
    val addedToNotebook: Boolean,
    val createdAt: Long
)

@Entity
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val type: ArtifactType, // AUDIO, REPORT, QUIZ, etc.
    val taskId: String?,
    val status: ArtifactStatus, // PENDING, GENERATING, READY, FAILED
    val createdAt: Long
)

// DataStore
data class UserSettings(
    val sourceLimit: Int = 15,
    val bridgeServerUrl: String = "http://localhost:8080",
    val providers: List<ProviderConfig>
)

data class ProviderConfig(
    val id: String, // openai, gemini, anthropic
    val apiKey: String, // encrypted
    val selectedModel: String
)
```

---

## 14. Background Job ve Bildirim Akışı

```
Kullanıcı "Araştır" der
    ↓
ViewModel → ResearchUseCase.execute()
    ↓
Phase 1: Web Search (provider API ile)
    → Sonuçlar UI'da gösterilir
    → Room'a kaydedilir
    ↓
Phase 2: NotebookLM Handoff (WorkManager)
    → OneTimeWorkRequest
    → Bridge server'a POST /notebook/create
    → POST /notebook/{id}/sources
    → Constraint: CONNECTED network
    ↓
Phase 3: Artifact Generation (WorkManager, opsiyonel)
    → Kullanıcı artifact seçtiyse
    → POST /notebook/{id}/artifacts
    → Periodic status check (backoff)
    ↓
Tamamlandığında:
    → Local notification (NotificationManager)
    → Thread status → COMPLETED
    → UI güncellenir (Flow ile observe)
```

**WorkManager chain:**
```kotlin
val work = OneTimeWorkRequestBuilder<NotebookHandoffWorker>()
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .setInputData(workDataOf("threadId" to threadId))
    .build()

WorkManager.getInstance(context).enqueueUniqueWork(
    "notebook_$threadId",
    ExistingWorkPolicy.KEEP,
    work
)
```

---

## 15. Hata Durumları ve Kullanıcıya Gösterilecek Davranış

| Hata | Kullanıcıya Mesaj | Sistem Davranışı |
|---|---|---|
| API key geçersiz | "API anahtarın geçersiz. Settings'ten kontrol et." | Settings'e deep link |
| API key yok | "Araştırma yapmak için API anahtarı gerekli." | Settings'e yönlendir |
| İnternet yok | "Bağlantı yok. İnternete bağlanıp tekrar dene." | Retry butonu |
| Bridge server ulaşılamaz | "NotebookLM bağlantısı kurulamadı. Bridge server'ın çalıştığından emin ol." | Settings'te bridge URL kontrolü |
| Bridge server hata | "Bu işlem tamamlanamadı. Yeni chat açıp tekrar dene." | Thread → FAILED |
| Rate limit (50/gün) | "Günlük NotebookLM limiti doldu. Yarın tekrar dene." | Thread → FAILED |
| NotebookLM auth expire | "NotebookLM oturumun sona erdi. Bridge server'da yeniden giriş yap." | Bridge health check |
| Search sonuç yok | "Bu konuda kaynak bulunamadı. Farklı bir arama dene." | Thread açık kalır |
| Kaynak limiti aşıldı | "Kaynak limiti (15) doldu. Settings'ten artırabilirsin." | Uyarı, devam seçeneği |
| Artifact üretim hatası | "Artifact üretilemedi. Tekrar deneyebilirsin." | Retry butonu |

---

## 16. Kritik Riskler

| # | Risk | Şiddet | Olasılık | Azaltma |
|---|---|---|---|---|
| R1 | NotebookLM UI değişir, bridge kırılır | YÜKSEK | ORTA | Bridge server versiyonlama, hızlı patch süreci, selector'ları configurable yap |
| R2 | Rate limit (50/gün) kullanıcıyı kısıtlar | YÜKSEK | YÜKSEK | Kullanıcıya açık limit göster, kaynak sayısını optimize et, batch ekleme |
| R3 | Bridge server kurulumu karmaşık | ORTA | YÜKSEK | Tek komutla çalışan setup script, detaylı dokümantasyon |
| R4 | Browser automation yavaş (~5-15sn/işlem) | ORTA | KESİN | Background job + bildirim, kullanıcı beklemesin |
| R5 | Google Sign-In token expire | DÜŞÜK | DÜŞÜK | Firebase otomatik refresh |
| R6 | API key güvenliği (cihazda) | ORTA | DÜŞÜK | EncryptedSharedPreferences, root detection |
| R7 | LLM provider API maliyeti kullanıcıda | ORTA | KESİN | Kullanıcıya maliyet bilgisi göster (isteğe bağlı) |

---

## 17. Acımasız Eleştiri: Bu Fikrin Zayıf Yerleri

### 🔴 Kritik Zayıflıklar

1. **NotebookLM bridge = tek nokta arıza.** Public API yok. Browser automation fragile. Google UI'ı değiştirirse ürün çalışmaz. Bu ürünün varoluşsal riski.

2. **Bridge server = friction.** Kullanıcının kendi makinasında server çalıştırması gerekiyor. "Mobilde tek akış" vaadi, aslında "mobil + masaüstü bridge" anlamına geliyor. Power user bile bu setup'tan kaçabilir.

3. **Rate limit ciddi.** 50 query/gün, 15 kaynak/araştırma → günde ~3 araştırma. Power user için kısıtlayıcı.

### 🟡 Orta Zayıflıklar

4. **Değer önerisi dar.** CLI zaten çalışıyor. Mobil wrapper'ın ek değeri "convenience" ama bridge server kurma zahmetini düşünce net fayda sorgulanır.

5. **Artifact üretim süresi belirsiz.** NotebookLM artifact'leri dakikalar sürebilir. Kullanıcı beklentisi ile gerçek süre arasında uçurum olabilir.

6. **Hedef kitle çok niş.** NotebookLM + LLM API + bridge server kuracak kullanıcı sayısı çok küçük.

### 🟢 Kabul Edilebilir Zayıflıklar

7. **Thread devam ettirilemez.** İlk aşama için kabul edilebilir ama kullanıcılar isteyecek.

8. **Tek Google hesap.** V1 için yeterli.

### Selin'in Değerlendirmesi

> Bu ürün, bridge server kurulum zahmetine rağmen kullanıcıya değer katıyorsa yaşar. Aksi halde CLI wrapper olarak kalır. V1'in gerçek testi: "Bridge server kurup mobilde kullanmak, CLI'den gerçekten daha mı kolay?" sorusudur. Cevap "evet" ise devam, "hayır" ise ürünü pivot et.

---

## 18. Uygulanabilir V1 Backlog

### Sprint 1: Temel (2 hafta)
- [ ] Android proje kurulumu (Compose, Hilt, Room, Navigation)
- [ ] Google Sign-In (Firebase Auth)
- [ ] Settings ekranı (API key CRUD, bridge URL)
- [ ] Room DB + veri modeli
- [ ] Chat ekranı UI (boş state, input bar, provider chip)

### Sprint 2: Research Pipeline (2 hafta)
- [ ] Web search entegrasyonu (provider API)
- [ ] Sonuç kartları UI
- [ ] Thread oluşturma ve kaydetme
- [ ] Duplicate URL kontrolü
- [ ] Kaynak limiti kontrolü

### Sprint 3: NotebookLM Bridge (2 hafta)
- [ ] Bridge server HTTP katmanı (Flask/FastAPI)
- [ ] Android → Bridge Retrofit client
- [ ] Notebook oluşturma + kaynak ekleme
- [ ] WorkManager background job
- [ ] Bridge health check

### Sprint 4: Artifacts + Polish (2 hafta)
- [ ] Artifact seçim UI
- [ ] Artifact üretim (bridge üzerinden)
- [ ] Local notification
- [ ] Thread arşivi ekranı
- [ ] Hata durumları ve edge case'ler
- [ ] UI polish ve animasyonlar

---

## 19. İlk İmplementasyon Paketi

### Dosya Yapısı
```
app/
├── src/main/
│   ├── java/com/researchflow/
│   │   ├── di/                    # Hilt modules
│   │   ├── data/
│   │   │   ├── local/             # Room DB, DAOs
│   │   │   ├── remote/            # Retrofit services
│   │   │   ├── repository/        # Repository implementations
│   │   │   └── preferences/       # DataStore, EncryptedPrefs
│   │   ├── domain/
│   │   │   ├── model/             # Domain models
│   │   │   └── usecase/           # Use cases
│   │   ├── ui/
│   │   │   ├── auth/              # Sign-in screen
│   │   │   ├── chat/              # Main chat screen
│   │   │   ├── settings/          # Settings screen
│   │   │   ├── archive/           # Thread archive
│   │   │   ├── components/        # Shared composables
│   │   │   └── theme/             # Material 3 theme
│   │   ├── worker/                # WorkManager workers
│   │   ├── notification/          # Notification helpers
│   │   └── ResearchFlowApp.kt     # Application class
│   └── res/
├── build.gradle.kts
└── libs.versions.toml

bridge-server/
├── server.py                      # FastAPI thin HTTP layer
├── requirements.txt
├── Dockerfile (opsiyonel)
└── README.md
```

### Anahtar Bağımlılıklar (libs.versions.toml)
```toml
[versions]
kotlin = "2.0.0"
composeBom = "2024.06.00"
hilt = "2.51"
room = "2.6.1"
retrofit = "2.9.0"
workmanager = "2.9.0"
firebaseAuth = "23.0.0"
navigation = "2.8.0"
datastore = "1.1.1"
security-crypto = "1.1.0-alpha06"

[libraries]
# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
# WorkManager
work-runtime = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }
# Firebase
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx", version.ref = "firebaseAuth" }
# Security
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
```

---

## 20. Son Karar Özeti

| # | Karar | Gerekçe |
|---|---|---|
| K1 | Android-first, iOS yok | V1 kapsam sınırı |
| K2 | Cloud backend yok | Veri cihazda, auth Firebase client-side |
| K3 | Bridge server (kullanıcı makinası) | NotebookLM public API yok, tek yol browser automation |
| K4 | BYOK (Bring Your Own Key) | Kullanıcı kendi provider key'ini girer |
| K5 | WorkManager background job | Android best practice, constraint destekli |
| K6 | Local notification (FCM değil) | Backend yok, local yeterli |
| K7 | Room + DataStore | Offline thread arşivi + encrypted key storage |
| K8 | Thread devam ettirilemez | V1 karmaşıklık sınırı |
| K9 | 3 provider (OpenAI, Gemini, Anthropic) | En yaygın, genişletilebilir |
| K10 | MVVM + Clean Arch (3 katman) | Overengineering yok ama test edilebilir |

---

## 21. Yeni Ajan Atanması

**İnsan Kaynakları değerlendirmesi:** Mevcut ajan havuzu bu iş için YETERLİ.

- **native-builder** + **android-jetpack-compose-expert** → Android implementasyon
- **solution-architect** + **architecture-note-tr** → Mimari kararlar
- **backend-services-engineer** + **backend-design-tr** → Bridge server
- **integration-automation-engineer** → Pipeline entegrasyonu
- **qa-auditor** + **risk-kontrol-tr** → Test ve risk

**Yeni ajan gerekmedi.** Mevcut roller tüm çıktıları kapsar.

---

## Run Log (ops-archivist)

```
2026-04-23 01:45 | Selin: İş alındı. task-intake-tr çalıştırıldı.
2026-04-23 01:45 | brief-netlestirme-tr: Kapsam net, kritik bloklayıcı soru yok.
2026-04-23 01:45 | Varsayım: Bridge server FastAPI, kullanıcı makinasında çalışır.
2026-04-23 01:45 | Varsayım: V1'de tek Google hesap yeterli.
2026-04-23 01:45 | Varsayım: NotebookLM CLI scriptleri (search-shortlist, notebooklm-handoff, notebooklm-artifacts) çalışır durumda.
2026-04-23 01:46 | sw-pm + product-spec-tr: Ürün spec tamamlandı.
2026-04-23 01:46 | ux-design-tr: Ekran akışı ve hata durumları tanımlandı.
2026-04-23 01:46 | solution-architect + architecture-note-tr: Mimari karar verildi (MVVM + Clean + Bridge).
2026-04-23 01:46 | karar-memosu-tr: 10 karar belgelendi.
2026-04-23 01:46 | backend-services-engineer: Cloud backend gerekmez kararı.
2026-04-23 01:46 | integration-automation-engineer: Pipeline akışı tanımlandı.
2026-04-23 01:46 | native-builder: Dosya yapısı ve bağımlılıklar belirlendi.
2026-04-23 01:46 | qa-auditor + risk-kontrol-tr: 7 risk tanımlandı, acımasız eleştiri yapıldı.
2026-04-23 01:46 | scope-guard: V1 dışı taşma tespit edilmedi.
2026-04-23 01:46 | validation-check: Tüm kararlar çelişki kontrolünden geçti.
2026-04-23 01:46 | İnsan Kaynakları: Yeni ajan gerekmedi.
2026-04-23 01:46 | delivery-docs + handoff-pack-tr: Teslim paketi hazırlandı.
```

---

## Sonraki Adım

Bu doküman onaylanırsa:
1. Android projesini oluştur (`npx` veya Android Studio template)
2. Bridge server'ı mevcut codex-notebooklm scriptleri üstüne kur
3. Sprint 1 backlog'unu başlat
4. Android emulator'da test senaryolarını çalıştır
