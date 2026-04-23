"""
ResearchFlow Bridge Server
NotebookLM CLI scriptlerinin üstüne ince HTTP katmanı.
Mevcut codex-notebooklm altyapısını kullanır.
"""
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

app = FastAPI(title="ResearchFlow Bridge", version="1.0.0")

# codex-notebooklm dizini - ortam değişkeninden veya varsayılan
CODEX_DIR = os.environ.get("CODEX_NOTEBOOKLM_DIR", str(Path.home() / "Desktop" / "codex-notebooklm"))
SCRIPTS_DIR = os.path.join(CODEX_DIR, "scripts") if os.path.exists(os.path.join(CODEX_DIR, "scripts")) else CODEX_DIR
NOTEBOOKLM_BIN = os.environ.get("NOTEBOOKLM_BIN") or shutil.which("notebooklm")
LOGIN_PROCESS: subprocess.Popen | None = None


def run_notebooklm_script(script: str, args: list[str]) -> dict:
    """NotebookLM CLI script'ini çalıştır ve JSON çıktısını döndür."""
    cmd = [sys.executable, "scripts/run.py", script] + args
    try:
        result = subprocess.run(
            cmd,
            cwd=CODEX_DIR,
            capture_output=True,
            text=True,
            timeout=120
        )
        if result.returncode != 0:
            raise HTTPException(status_code=500, detail=f"Script error: {result.stderr[:500]}")

        # JSON çıktısını parse etmeye çalış
        output = result.stdout.strip()
        try:
            return json.loads(output)
        except json.JSONDecodeError:
            return {"raw_output": output}
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=504, detail="Script timeout")
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail=f"Script not found at {CODEX_DIR}")


def run_notebooklm_cli(args: list[str], timeout: int = 30) -> subprocess.CompletedProcess[str]:
    """Run the installed notebooklm CLI directly."""
    if not NOTEBOOKLM_BIN:
        raise HTTPException(status_code=500, detail="notebooklm CLI not found in PATH")
    try:
        return subprocess.run(
            [NOTEBOOKLM_BIN] + args,
            cwd=str(Path.home()),
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=504, detail="notebooklm CLI timeout")


def notebooklm_auth_status() -> dict:
    """Return a UI-friendly NotebookLM auth status."""
    result = run_notebooklm_cli(["status"], timeout=20)
    output = (result.stdout + "\n" + result.stderr).strip()
    authenticated = result.returncode == 0
    return {
        "authenticated": authenticated,
        "status": "authenticated" if authenticated else "not_authenticated",
        "detail": output[:1000],
    }


# --- Models ---

class CreateNotebookRequest(BaseModel):
    title: str

class SourceUrl(BaseModel):
    url: str
    title: str
    type: str

class AddSourcesRequest(BaseModel):
    urls: list[SourceUrl]

class GenerateArtifactRequest(BaseModel):
    artifactType: str

class SearchRequest(BaseModel):
    query: str
    provider: str
    model: str
    apiKey: str
    maxResults: int = 15


# --- Endpoints ---

@app.get("/health")
async def health():
    """Bridge server health check."""
    # Ayrıca NotebookLM auth durumunu kontrol et
    try:
        result = run_notebooklm_script("auth_manager.py", ["status"])
        return {"status": "ok", "version": "1.0.0", "auth": result}
    except Exception:
        return {"status": "ok", "version": "1.0.0", "auth": "unknown"}


@app.get("/auth/status")
async def auth_status():
    """NotebookLM auth durumunu UI için döndür."""
    return notebooklm_auth_status()


@app.post("/auth/login")
async def auth_login():
    """NotebookLM login akışını başlat."""
    global LOGIN_PROCESS
    if LOGIN_PROCESS is not None and LOGIN_PROCESS.poll() is None:
        return {
            "started": True,
            "alreadyRunning": True,
            "message": "NotebookLM login already running",
        }

    if not NOTEBOOKLM_BIN:
        raise HTTPException(status_code=500, detail="notebooklm CLI not found in PATH")

    try:
        LOGIN_PROCESS = subprocess.Popen(
            [NOTEBOOKLM_BIN, "login"],
            cwd=str(Path.home()),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return {
            "started": True,
            "alreadyRunning": False,
            "message": "NotebookLM login started. Complete the Google sign-in in the browser.",
        }
    except OSError as exc:
        raise HTTPException(status_code=500, detail=f"Could not start NotebookLM login: {exc}")


@app.post("/notebook/create")
async def create_notebook(request: CreateNotebookRequest):
    """Yeni NotebookLM notebook oluştur."""
    result = run_notebooklm_cli(["create", request.title, "--json"], timeout=120)
    if result.returncode != 0:
        raise HTTPException(status_code=500, detail=result.stderr[:1000])

    output = result.stdout.strip()
    try:
        created = json.loads(output)
    except json.JSONDecodeError:
        created = {"raw_output": output}

    notebook = created.get("notebook") if isinstance(created.get("notebook"), dict) else {}
    notebook_id = (
        created.get("id")
        or created.get("notebook_id")
        or created.get("notebookId")
        or created.get("uuid")
        or notebook.get("id")
        or notebook.get("notebook_id")
        or notebook.get("notebookId")
        or notebook.get("uuid")
    )
    if not notebook_id:
        raise HTTPException(status_code=500, detail=f"Notebook ID not found in CLI output: {output[:1000]}")
    return {"notebookId": notebook_id, "title": request.title}


@app.post("/notebook/{notebook_id}/sources")
async def add_sources(notebook_id: str, request: AddSourcesRequest):
    """Kaynakları notebook'a ekle."""
    added = 0
    skipped = []
    for source in request.urls:
        try:
            result = run_notebooklm_cli([
                "source",
                "add",
                source.url,
                "--notebook", notebook_id,
                "--type", "youtube" if source.type.lower() == "youtube" else "url",
                "--json",
            ], timeout=20)
            if result.returncode != 0:
                raise RuntimeError(result.stderr[:1000])
            added += 1
        except Exception as e:
            skipped.append(source.url)

    return {
        "addedCount": added,
        "skippedCount": len(skipped),
        "skippedUrls": skipped
    }


@app.post("/notebook/{notebook_id}/artifacts")
async def generate_artifact(notebook_id: str, request: GenerateArtifactRequest):
    """Artifact üretimini başlat (non-blocking)."""
    artifact_map = {
        "audio": "audio",
        "report": "report",
        "quiz": "quiz",
        "flashcards": "flashcards",
        "mind-map": "mind-map",
        "slide-deck": "slide-deck",
        "video": "video"
    }

    artifact_type = artifact_map.get(request.artifactType, request.artifactType)

    args = [
        "generate",
        artifact_type,
        "--notebook",
        notebook_id,
        "--json",
    ]
    if artifact_type != "mind-map":
        args.append("--no-wait")

    result = run_notebooklm_cli(args, timeout=60)
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise HTTPException(status_code=500, detail=detail[:1000])

    output = result.stdout.strip()
    try:
        generated = json.loads(output)
    except json.JSONDecodeError:
        generated = {"raw_output": output}

    task_id = (
        generated.get("task_id")
        or generated.get("taskId")
        or generated.get("id")
        or f"task_{datetime.now().strftime('%Y%m%d%H%M%S')}"
    )
    return {
        "taskId": task_id,
        "artifactType": request.artifactType,
        "status": generated.get("status", "pending")
    }


@app.get("/notebook/{notebook_id}/artifacts/{task_id}/status")
async def artifact_status(notebook_id: str, task_id: str):
    """Artifact üretim durumunu sorgula."""
    result = run_notebooklm_cli([
        "artifact",
        "poll",
        task_id,
        "--notebook",
        notebook_id,
    ], timeout=30)
    if result.returncode == 0:
        output = result.stdout.strip()
        return {
            "taskId": task_id,
            "status": output or "generating",
            "artifactId": None
        }

    return {
        "taskId": task_id,
        "status": "generating",
        "artifactId": None
    }


def _search_openai(query: str, model: str, api_key: str, max_results: int) -> list[dict]:
    """OpenAI ile web araması (responses API + web_search_preview tool)."""
    try:
        import httpx
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        payload = {
            "model": model,
            "tools": [{"type": "web_search_preview"}],
            "input": f"Search the web for: {query}. Return the top {max_results} most relevant results as a JSON array with fields: title, url, reason, type (article/youtube/docs/web)."
        }
        resp = httpx.post("https://api.openai.com/v1/responses", json=payload, headers=headers, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        # responses API çıktısını parse et
        for item in data.get("output", []):
            if item.get("type") == "message":
                for block in item.get("content", []):
                    if block.get("type") == "output_text":
                        text = block["text"]
                        # JSON array'i bul
                        start = text.find("[")
                        end = text.rfind("]") + 1
                        if start >= 0 and end > start:
                            return json.loads(text[start:end])
        return []
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"OpenAI search error: {str(e)}")


def _search_gemini(query: str, model: str, api_key: str, max_results: int) -> list[dict]:
    """Gemini ile web araması (Google Search grounding)."""
    try:
        import httpx
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        payload = {
            "contents": [{"parts": [{"text": f"Search the web for: {query}. Return the top {max_results} most relevant results as a JSON array with fields: title, url, reason, type (article/youtube/docs/web). Only return the JSON array, no extra text."}]}],
            "tools": [{"google_search": {}}]
        }
        resp = httpx.post(url, json=payload, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        text = data["candidates"][0]["content"]["parts"][0]["text"]
        start = text.find("[")
        end = text.rfind("]") + 1
        if start >= 0 and end > start:
            return json.loads(text[start:end])
        return []
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gemini search error: {str(e)}")


def _search_anthropic(query: str, model: str, api_key: str, max_results: int) -> list[dict]:
    """Anthropic Claude ile web araması."""
    try:
        import httpx
        headers = {
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json"
        }
        payload = {
            "model": model,
            "max_tokens": 2048,
            "messages": [{
                "role": "user",
                "content": f"Search the web for: {query}. Based on your knowledge, return the top {max_results} most relevant resources as a JSON array with fields: title (string), url (string), reason (string), type (article/youtube/docs/web). Only return valid JSON array, no extra text."
            }]
        }
        resp = httpx.post("https://api.anthropic.com/v1/messages", json=payload, headers=headers, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        text = data["content"][0]["text"]
        start = text.find("[")
        end = text.rfind("]") + 1
        if start >= 0 and end > start:
            return json.loads(text[start:end])
        return []
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Anthropic search error: {str(e)}")


def _search_openai_compatible(query: str, model: str, api_key: str, max_results: int, base_url: str) -> list[dict]:
    """Groq ve OpenRouter için OpenAI uyumlu endpoint üzerinden arama."""
    try:
        import httpx
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        # HTTP Referer for OpenRouter
        if "openrouter" in base_url:
            headers["HTTP-Referer"] = "https://github.com/researchflow"
            headers["X-Title"] = "ResearchFlow"
            
        payload = {
            "model": model,
            "messages": [{
                "role": "user",
                "content": f"Search the web for: {query}. Based on your knowledge, return the top {max_results} most relevant resources as a JSON array with fields: title (string), url (string), reason (string), type (article/youtube/docs/web). Only return valid JSON array, no extra text."
            }]
        }
        resp = httpx.post(f"{base_url}/chat/completions", json=payload, headers=headers, timeout=60)
        try:
            resp.raise_for_status()
        except httpx.HTTPStatusError as exc:
            body = exc.response.text[:1000]
            raise HTTPException(
                status_code=502,
                detail=(
                    f"Compatible API search error: provider returned "
                    f"{exc.response.status_code} for model '{model}'. {body}"
                )
            )
        data = resp.json()
        text = data["choices"][0]["message"]["content"]
        start = text.find("[")
        end = text.rfind("]") + 1
        if start >= 0 and end > start:
            return json.loads(text[start:end])
        raise HTTPException(
            status_code=502,
            detail=f"Compatible API search error: model '{model}' did not return a JSON array."
        )
    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=502, detail=f"Compatible API search error: {str(e)}")



# BUG-001 fix: Real LLM provider search implementation
@app.post("/search")
async def search(request: SearchRequest):
    """Web search yapar ve sonuçları döndürür."""
    if not request.apiKey:
        raise HTTPException(status_code=401, detail="API key required")

    provider = request.provider.lower()
    results = []

    if provider == "openai":
        results = _search_openai(request.query, request.model, request.apiKey, request.maxResults)
    elif provider == "gemini":
        results = _search_gemini(request.query, request.model, request.apiKey, request.maxResults)
    elif provider == "anthropic":
        results = _search_anthropic(request.query, request.model, request.apiKey, request.maxResults)
    elif provider == "groq":
        results = _search_openai_compatible(request.query, request.model, request.apiKey, request.maxResults, "https://api.groq.com/openai/v1")
    elif provider == "openrouter":
        results = _search_openai_compatible(request.query, request.model, request.apiKey, request.maxResults, "https://openrouter.ai/api/v1")
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported provider: {request.provider}")

    # Sonucu CODEX_DIR'e kaydet (notebooklm-handoff için)
    search_results_path = os.path.join(CODEX_DIR, "last_search_results.json")
    query_path = os.path.join(CODEX_DIR, "last_search_query.txt")
    try:
        Path(CODEX_DIR).mkdir(parents=True, exist_ok=True)
        with open(query_path, "w", encoding="utf-8") as f:
            f.write(request.query)
        with open(search_results_path, "w", encoding="utf-8") as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
    except Exception:
        pass  # Dosya yazma başarısız olsa bile sonuçları döndür

    return {
        "query": request.query,
        "results": results[:request.maxResults]
    }


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
