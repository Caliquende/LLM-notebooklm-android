# QA Auditor Raporu

Tarih: 2026-04-23
Kapsam: `C:\Users\Can\work\LLM-notebooklm-android`
Rol: `qa-auditor`
Durum: `sanity -> regression -> debug -> sanity -> hotfix -> regression -> debug -> sanity -> hotfix -> regression -> debug -> sanity -> hotfix -> regression` döngüsü uygulandı.

## Kullanılan skiller

- `qa-review-tr`: statik review, bulgu sıralama, test kanıtı değerlendirme.
- `risk-kontrol-tr`: güvenlik, veri akışı, config ve regresyon riski kontrolü.
- `test-automator`: Gradle test/lint/build, manifest doğrulama, emulator smoke akışı.
- `test-writer`: regresyon testi eklenecek davranışın seçimi ve test kapsamı değerlendirmesi.
- `a11y-audit`: Compose UI erişilebilirlik risklerinin statik ve UI hierarchy üzerinden kontrolü.
- `agent-device`: Android adb/emulator install, launch, hierarchy ve logcat smoke testi. Not: skill içindeki `references/bootstrap-install.md` ve `references/exploration.md` dosyaları bu kurulumda bulunamadığı için adb/uiautomator/logcat ile ilerlenmiştir.

## Statik review bulguları

### BUG-001 - Critical - Fixed - `/search` endpoint stub olmaktan çıkarıldı

```yaml
id: BUG-001
severity: Critical
type: functional
status: fixed
files:
  - bridge-server/server.py:275
  - bridge-server/server.py:286
  - bridge-server/server.py:288
  - bridge-server/server.py:290
evidence:
  - /search artık provider'a göre OpenAI, Gemini, Anthropic, Groq ve OpenRouter yollarına ayrılıyor.
  - API key boşsa 401, desteklenmeyen provider varsa 400 dönüyor.
validation:
  - python -m py_compile bridge-server\server.py: pass
residual_risk:
  - Gerçek provider entegrasyonu API anahtarı gerektirdiği için uçtan uca canlı arama doğrulanmadı.
  - LLM yanıtından JSON parse etme kırılgan; contract test hâlâ gerekli.
```

### BUG-002 - High - Fixed - Bridge URL artık runtime request'e uygulanıyor

```yaml
id: BUG-002
severity: High
type: configuration
status: fixed
files:
  - app/src/main/java/com/researchflow/di/NetworkModule.kt:48
  - app/src/main/java/com/researchflow/data/preferences/SettingsDataStore.kt:27
evidence:
  - OkHttp interceptor her request öncesi SettingsDataStore.bridgeUrl değerini okuyup scheme/host/port değerlerini request URL'ine uyguluyor.
validation:
  - ./gradlew.bat testDebugUnitTest lintDebug assembleDebug :app:processReleaseManifest: pass
residual_risk:
  - Interceptor içinde runBlocking kullanımı request başına blocking maliyet yaratır.
  - Invalid URL sessizce default Retrofit base URL'e düşer; UI-level hata mesajı yok.
```

### BUG-003 - High - Fixed - Release build'de debug cleartext config yok

```yaml
id: BUG-003
severity: High
type: security
status: fixed
files:
  - app/src/debug/AndroidManifest.xml:3
  - app/src/debug/res/xml/network_security_config.xml:5
  - app/src/main/AndroidManifest.xml:7
evidence:
  - networkSecurityConfig yalnız debug manifest altında.
  - release merged manifest içinde networkSecurityConfig veya usesCleartextTraffic bulunmadı.
validation:
  - rg -n "networkSecurityConfig|usesCleartextTraffic" app/build/intermediates/merged_manifests/release: no match
```

### BUG-004 - Medium - Fixed - Debug BODY logging kapatıldı

```yaml
id: BUG-004
severity: Medium
type: security
status: fixed
files:
  - app/src/main/java/com/researchflow/di/NetworkModule.kt:67
  - app/src/main/java/com/researchflow/di/NetworkModule.kt:71
evidence:
  - HttpLoggingInterceptor.Level.BASIC kullanılıyor; BODY logging yok.
validation:
  - rg -n "Level.BODY" app/src/main: no match
residual_risk:
  - SearchRequest hâlâ apiKey'i body içinde taşıyor; BASIC logging bunu basmıyor ama transport contract daha güvenli hale getirilebilir.
```

### BUG-005 - Medium - Partially fixed - Artifact taskId persist ediliyor, polling eksik

```yaml
id: BUG-005
severity: Medium
type: functional
status: partially_fixed
files:
  - app/src/main/java/com/researchflow/ui/chat/ChatViewModel.kt:210
  - app/src/main/java/com/researchflow/ui/chat/ChatViewModel.kt:212
  - app/src/main/java/com/researchflow/data/repository/ResearchRepository.kt:121
evidence:
  - generateArtifact response'u resp değişkenine alınıyor.
  - resp.taskId ArtifactDao.updateTaskInfo ile persist ediliyor.
remaining_issue:
  - getArtifactStatus endpoint/client metodu var ama uygulama içinde polling veya Worker akışı hâlâ yok.
recommended_fix:
  - ArtifactStatus polling veya WorkManager job eklenmeli.
```

### BUG-006 - Medium - Fixed - Backup/data extraction sertleştirildi

```yaml
id: BUG-006
severity: Medium
type: security
status: fixed
files:
  - app/src/main/AndroidManifest.xml:9
  - app/src/main/AndroidManifest.xml:10
  - app/src/main/AndroidManifest.xml:11
  - app/src/main/res/xml/data_extraction_rules.xml:1
  - app/src/main/res/xml/backup_rules.xml:1
evidence:
  - allowBackup=false.
  - dataExtractionRules ve fullBackupContent file/database/sharedpref için exclude tanımlıyor.
validation:
  - lintDebug: DataExtractionRules uyarısı kapandı.
```

### BUG-007 - Medium - Partially fixed - Unit test kaynağı eklendi, kapsam hâlâ dar

```yaml
id: BUG-007
severity: Medium
type: test-gap
status: partially_fixed
files:
  - app/src/test/java/com/researchflow/data/repository/SourceSelectionTest.kt:1
evidence:
  - testDebugUnitTest artık NO-SOURCE değil.
  - SourceSelectionTest iki regresyon senaryosu çalıştırıyor.
validation:
  - TEST-com.researchflow.data.repository.SourceSelectionTest.xml: tests=2, failures=0, errors=0
remaining_issue:
  - ViewModel, Retrofit contract, Room DAO integration, Compose UI ve bridge provider error path testleri hâlâ eksik.
```

### BUG-008 - Low - Fixed - API key alanları provider'a özel label taşıyor

```yaml
id: BUG-008
severity: Low
type: accessibility
status: fixed
files:
  - app/src/main/java/com/researchflow/ui/settings/SettingsScreen.kt:158
evidence:
  - OutlinedTextField label değeri "$name API Anahtarı".
validation:
  - Statik Compose kontrolü yapıldı; dinamik UI hierarchy tekrar smoke içinde kontrol edilmeli.
```

### BUG-009 - Low - Open - İlk render performans uyarısı yeniden ölçülmeli

```yaml
id: BUG-009
severity: Low
type: performance
status: open
commands:
  - adb logcat -d -t 600
evidence:
  - Son emulator smoke testte "Skipped 202 frames", "Displayed com.researchflow/.MainActivity for user 0: +8s717ms" ve "Davey duration=3483ms" görüldü.
  - Ayarlar ekranına geçişte "Skipped 41 frames" ve Davey 708ms/859ms kayıtları görüldü.
remaining_issue:
  - Fatal crash yok; ancak startup/main-thread performansı hâlâ ölçülüp iyileştirilmeli.
  - Release/profile startup benchmark yok.
```

### BUG-010 - Medium - Fixed - Duplicate kaynaklar limit slotlarını tüketiyordu

```yaml
id: BUG-010
severity: Medium
type: functional
status: fixed
files:
  - app/src/main/java/com/researchflow/data/repository/ResearchRepository.kt:75
  - app/src/main/java/com/researchflow/data/repository/ResearchRepository.kt:162
  - app/src/main/java/com/researchflow/data/local/dao/SourceDao.kt:16
  - app/src/test/java/com/researchflow/data/repository/SourceSelectionTest.kt:8
root_cause:
  - Önceki akışta results.take(remaining) duplicate filtreden önce uygulanıyordu.
  - İlk sonuçlar duplicate ise sonraki benzersiz sonuçlar değerlendirilmeden atılıyordu.
fix:
  - Existing URL'ler tek sorguyla alınıyor.
  - Duplicate ve mevcut URL filtrelemesi limitten önce yapılıyor.
  - Gelen sonuçların kendi içindeki duplicate URL'leri de tekilleştiriliyor.
validation:
  - SourceSelectionTest: 2 tests, 0 failures, 0 errors.
```

## Dinamik emulator test sonucu

```yaml
device:
  adb: C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe
  serial: emulator-5554
  model: sdk_gphone64_x86_64
latest_pre_hotfix_smoke:
  install: pass
  launch: pass
  crash: none_observed
  home_screen: pass
  settings_screen: pass
  archive_screen: pass
  bridge_health: pass_on_current_machine
  research_flow: blocked_by_missing_api_key
current_post_hotfix_status:
  adb_devices: pass
  install: pass
  launch: pass
  pid: 6039
  crash: none_observed
  home_screen: pass
  settings_screen: pass
  bridge_health: pass_on_current_machine
  research_flow: blocked_by_missing_api_key
credential_policy:
  google_mail_password: not_requested_not_used
```

## Kanıt/komutlar

```powershell
Get-Content C:\Users\Can\.agents\skills\qa-review-tr\SKILL.md
Get-Content C:\Users\Can\.agents\skills\risk-kontrol-tr\SKILL.md
Get-Content C:\Users\Can\.agents\skills\test-automator\SKILL.md
Get-Content C:\Users\Can\.agents\skills\test-writer\SKILL.md
Get-Content C:\Users\Can\.agents\skills\a11y-audit\SKILL.md
Get-Content C:\Users\Can\.agents\skills\agent-device\SKILL.md

$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug :app:processReleaseManifest
python -m py_compile bridge-server\server.py
rg -n "networkSecurityConfig|usesCleartextTraffic|allowBackup|dataExtractionRules|fullBackupContent|icon|label" app\build\intermediates\merged_manifests\debug app\build\intermediates\merged_manifests\release
rg -n "testsuite|testcase|failures|errors" app\build\test-results\testDebugUnitTest
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' logcat -c
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am force-stop com.researchflow
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am start -n com.researchflow/.MainActivity
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell pidof com.researchflow
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell uiautomator dump /sdcard/window.xml
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell cat /sdcard/window.xml
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell input tap 1007 221
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell uiautomator dump /sdcard/window_settings.xml
& 'C:\Users\Can\AppData\Local\Android\Sdk\platform-tools\adb.exe' logcat -d -t 800
```

Son regresyon özeti:

- Gradle: `BUILD SUCCESSFUL`.
- Unit test: `SourceSelectionTest`, `tests=2`, `failures=0`, `errors=0`.
- Lint: `0 errors, 69 warnings`; kalan uyarılar dependency version uyarıları.
- Release manifest: `networkSecurityConfig` yok; `allowBackup=false`, `dataExtractionRules`, `fullBackupContent`, `icon`, `label=@string/app_name` var.
- Bridge Python syntax: `python -m py_compile bridge-server\server.py` pass.
- Emulator: `emulator-5554` bağlı; APK install pass; launch pass; Home ve Settings hierarchy doğrulandı; fatal crash görülmedi.
- Bridge health: logcat `GET http://10.0.2.2:8080/health` için `200 OK`.
- Performans: logcat startup için `Displayed ... +8s717ms`, `Skipped 202 frames`, `Davey duration=3483ms`; BUG-009 açık kaldı.

## Regresyon riskleri

- `SourceDao.getExistingUrls(... IN (:urls))` boş liste ile çağrılmıyor; repository erken dönüş yapıyor. Gelecekte doğrudan DAO çağrısı eklenirse boş liste davranışı ayrıca korunmalı.
- Source seçimi artık aynı response içindeki duplicate URL'leri de eliyor; bu doğru davranış ama UI'daki "kaç kaynak bulundu" mesajı provider'ın döndürdüğü ham sonuç sayısını göstermeye devam ediyor.
- `NetworkModule` dynamic URL çözümü request başına DataStore okuyor; sık request altında latency yaratabilir.
- Bridge `/search` provider yanıtlarından JSON parse ediyor; provider prompt drift'i boş sonuç veya 502 üretebilir.
- Artifact `taskId` persist ediliyor ancak status polling yok; artifact GENERATING durumunda kalabilir.
- Backup/data extraction kuralları veriyi bilinçli olarak dışarıda bırakıyor; kullanıcı cihaz değiştirirken lokal araştırma geçmişi taşınmayabilir.
- Lintte dependency update uyarıları kaldı; toplu dependency upgrade ayrı regresyon matrisi gerektirir.
