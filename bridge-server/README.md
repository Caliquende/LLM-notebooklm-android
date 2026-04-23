# Bridge Server

NotebookLM CLI scriptlerinin üstüne ince HTTP katmanı.

## Kurulum

```bash
cd bridge-server
pip install -r requirements.txt
```

## Çalıştırma

```bash
# codex-notebooklm dizinini belirt
export CODEX_NOTEBOOKLM_DIR="C:/Users/Can/Desktop/codex-notebooklm"

# Sunucuyu başlat
python server.py
```

Sunucu `http://localhost:8080` adresinde başlar.

## Android Emulator Notu

Android emulator'dan host makinaya erişim için `10.0.2.2` IP adresi kullanılır.
Uygulama varsayılan olarak `http://10.0.2.2:8080` adresini kullanır.

## Endpoints

| Method | Path | Açıklama |
|--------|------|----------|
| GET | /health | Sağlık kontrolü |
| POST | /notebook/create | Notebook oluştur |
| POST | /notebook/{id}/sources | Kaynak ekle |
| POST | /notebook/{id}/artifacts | Artifact üret |
| GET | /notebook/{id}/artifacts/{taskId}/status | Durum sorgula |
| POST | /search | Web araması |
