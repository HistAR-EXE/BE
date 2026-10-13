#!/usr/bin/env python3
"""Loopback-only acceptance harness for two patched HistAR BE instances.

The harness never starts services or resets a database. It creates isolated random
test identities/conversations in an explicitly named disposable database, calls
both sync and SSE APIs, and compares supported facts plus source overlap rather
than exact model wording.
"""
from __future__ import annotations

import argparse
import json
import re
import secrets
import subprocess
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

HISTORY = (
    ("user", "Tôi đang tìm hiểu Chiến dịch Cedar Falls tại Củ Chi."),
    ("assistant", "Chiến dịch Cedar Falls diễn ra vào tháng 01/1967; tôi sẽ chỉ dùng tư liệu đã truy xuất để hỗ trợ bạn."),
)
QUESTIONS = {
    "cedar_short": "chiến dịch cedar fall",
    "cedar_full": "Trận đánh Cedar Falls diễn ra như thế nào?",
    "location": "Địa đạo Củ Chi ở đâu?",
    "follow_up": "Chiến dịch đó diễn ra vào năm nào?",
    "no_source": "Mã hồ sơ tưởng tượng ZXQ-9941 được lập khi nào?",
}
CEDAR_TERMS = ("cedar", "falls", "1967", "củ chi", "cu chi", "tam giác sắt", "tam giac sat")
NO_SOURCE_TERMS = ("chưa đủ", "không đủ", "không tìm thấy", "không có tư liệu", "chưa có tư liệu")
GENERIC_TERMS = ("xin chào", "tôi có thể giúp", "bạn muốn tìm hiểu")


def local_url(value: str, *, backend: bool = False) -> str:
    parsed = urllib.parse.urlsplit(value)
    if (parsed.scheme != "http" or parsed.username or parsed.password or parsed.query or parsed.fragment
            or parsed.path not in ("", "/") or parsed.hostname not in ("localhost", "127.0.0.1", "::1")):
        raise ValueError(f"loopback URL required: {value}")
    if parsed.port is None:
        raise ValueError(f"explicit port required: {value}")
    if backend and not 18000 <= parsed.port <= 18999:
        raise ValueError("acceptance backends must use an isolated port in 18000..18999")
    host = f"[{parsed.hostname}]" if ":" in parsed.hostname else parsed.hostname
    return f"http://{host}:{parsed.port}"


def database_name(value: str) -> str:
    if not re.fullmatch(r"timelens_rag_test_[a-z0-9][a-z0-9_]{0,42}", value or "") or len(value) > 63:
        raise ValueError("database must be a disposable timelens_rag_test_* name")
    return value


def _literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


class Database:
    def __init__(self, name: str, container: str, user: str):
        self.name = database_name(name)
        self.container = container
        self.user = user
        if container != "histar-postgres" or user != "timelens":
            raise ValueError("only the guarded local histar-postgres/timelens target is allowed")

    def sql(self, statement: str) -> str:
        command = ["docker", "exec", "-i", self.container, "psql", "-U", self.user,
                   "-d", self.name, "-v", "ON_ERROR_STOP=1", "-At"]
        payload = ("SET client_encoding TO 'UTF8';\n" + statement).encode("utf-8")
        result = subprocess.run(command, input=payload, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            raise RuntimeError("psql failed; inspect the local PostgreSQL container logs")
        return result.stdout.decode("utf-8", errors="strict").strip()

    def profile(self):
        user_id = str(uuid.uuid4())
        email = f"rag-{secrets.token_hex(8)}@example.invalid"
        password = secrets.token_urlsafe(32)
        self.sql(f"""
            INSERT INTO profiles(id,email,password_hash,role,tier,email_verified,display_name,email_verified_at)
            VALUES ({_literal(user_id)}::uuid,{_literal(email)},crypt({_literal(password)},gen_salt('bf')),
                    'ADMIN', 'FREE', TRUE, 'RAG acceptance', now());
        """)
        return user_id, email, password

    def character(self) -> str:
        value = self.sql("""
            SELECT id FROM characters
            WHERE location_id='11111111-1111-1111-1111-111111111111'::uuid AND chat_enabled=TRUE
            ORDER BY id LIMIT 1;
        """).splitlines()
        if not value:
            raise RuntimeError("disposable database has no enabled Củ Chi chat character")
        uuid.UUID(value[-1])
        return value[-1]

    def conversation(self, user_id: str, character_id: str, seed_history: bool = False) -> str:
        conversation_id = str(uuid.uuid4())
        statements = [
            f"INSERT INTO conversations(id,user_id,character_id,created_at) VALUES "
            f"({_literal(conversation_id)}::uuid,{_literal(user_id)}::uuid,{_literal(character_id)}::uuid,"
            "'2000-01-01T00:00:00Z');"
        ]
        if seed_history:
            for index, (role, content) in enumerate(HISTORY, start=1):
                statements.append(
                    "INSERT INTO messages(conversation_id,role,content,created_at) VALUES "
                    f"({_literal(conversation_id)}::uuid,{_literal(role)},{_literal(content)},"
                    f"'2000-01-01T00:00:0{index}Z');")
        self.sql("\n".join(statements))
        return conversation_id


def login_token(payload: dict) -> str:
    data = payload.get("data") if isinstance(payload, dict) else None
    token = data.get("accessToken") or data.get("token") if isinstance(data, dict) else None
    if not token:
        raise ValueError("login response did not contain a token")
    return token


def _source_key(source: dict) -> str:
    return str(source.get("sourceId") or source.get("title") or source.get("url") or "").strip().casefold()


def _dedupe_sources(sources) -> list[dict]:
    result, seen = [], set()
    for source in sources or []:
        if not isinstance(source, dict):
            continue
        key = _source_key(source) or json.dumps(source, ensure_ascii=False, sort_keys=True)
        if key not in seen:
            seen.add(key)
            result.append(source)
    return result


def parse_sync(raw: str) -> dict:
    try:
        payload = json.loads(raw)
        data = payload.get("data")
        if not isinstance(data, dict):
            raise ValueError
        return {"reply": str(data.get("reply") or ""), "sources": _dedupe_sources(data.get("sources")),
                "conversationId": data.get("conversationId"), "requestId": None, "complete": True, "errors": []}
    except Exception:
        return {"reply": "", "sources": [], "requestId": None, "complete": False,
                "errors": ["invalid_sync_response"]}


def parse_sse(raw: str) -> dict:
    result = {"reply": "", "sources": [], "requestId": None, "complete": False, "errors": []}
    normalized = raw.replace("\r\n", "\n").strip()
    for frame in re.split(r"\n\s*\n", normalized) if normalized else []:
        event, data_lines = "message", []
        for line in frame.splitlines():
            if line.startswith(":"):
                continue
            if line.startswith("event:"):
                event = line[6:].strip()
            elif line.startswith("data:"):
                data_lines.append(line[5:].lstrip())
        if not data_lines:
            continue
        try:
            data = json.loads("\n".join(data_lines))
        except Exception:
            result["errors"].append("invalid_sse_json")
            continue
        if event == "meta" and isinstance(data, dict):
            result["requestId"] = data.get("requestId") or result["requestId"]
        elif event == "delta":
            if isinstance(data, dict):
                result["reply"] += str(data.get("content") or "")
                result["sources"].extend(data.get("sources") or [])
            elif isinstance(data, str):
                result["reply"] += data
        elif event == "sources":
            result["sources"].extend(data if isinstance(data, list) else [])
        elif event == "complete":
            result["complete"] = True
        elif event == "error":
            result["serverError"] = data
            if isinstance(data, dict):
                result["requestId"] = data.get("requestId") or result["requestId"]
            result["errors"].append("server_error")
    result["sources"] = _dedupe_sources(result["sources"])
    if result["reply"] and not result["complete"] and "server_error" not in result["errors"]:
        result["errors"].append("stream_ended_without_complete")
    return result


def _fold(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKD", text or "") if not unicodedata.combining(c)).casefold()


def _source_text(result: dict) -> str:
    return _fold(" ".join(" ".join(str(source.get(k) or "") for k in ("title", "excerpt", "sourceId", "url"))
                                  for source in result.get("sources", [])))


def supported_facts(result: dict) -> set[str]:
    text, sources = _fold(result.get("reply", "")), _source_text(result)
    facts = set()
    if "1967" in text and "1967" in sources:
        facts.add("year_1967")
    if "cedar" in text and "cedar" in sources:
        facts.add("cedar_falls")
    if ("cu chi" in text and "cu chi" in sources):
        facts.add("cu_chi")
    return facts


def validate(case: str, result: dict) -> list[str]:
    errors = list(result.get("errors") or [])
    reply = _fold(result.get("reply", ""))
    if case == "no_source":
        if result.get("sources"):
            errors.append("unexpected_sources")
        if not any(_fold(term) in reply for term in NO_SOURCE_TERMS):
            errors.append("missing_honest_no_source_answer")
        if re.search(r"\b(19|20)\d{2}\b", reply):
            errors.append("unsupported_fabricated_fact")
        return errors
    if not reply:
        errors.append("empty_reply")
    if any(_fold(term) in reply for term in GENERIC_TERMS) and case.startswith("cedar"):
        errors.append("generic_greeting")
    if not result.get("sources"):
        errors.append("missing_sources")
    if case.startswith("cedar"):
        if "cedar" not in reply:
            errors.append("missing_cedar_supported_fact")
        years = re.findall(r"\b(?:19|20)\d{2}\b", reply)
        if years and "1967" not in years:
            errors.append("unsupported_cedar_year")
        if "cedar" not in _source_text(result):
            errors.append("irrelevant_cedar_source")
    if case == "location" and "cu chi" not in reply:
        errors.append("missing_location_fact")
    return errors


def compare(left: dict, right: dict) -> dict:
    left_sources, right_sources = {_source_key(s) for s in left.get("sources", [])}, {_source_key(s) for s in right.get("sources", [])}
    left_sources.discard(""); right_sources.discard("")
    union = left_sources | right_sources
    both_honest_no_source = (not union and all(
        any(_fold(term) in _fold(item.get("reply", "")) for term in NO_SOURCE_TERMS)
        for item in (left, right)))
    overlap = len(left_sources & right_sources) / len(union) if union else (1.0 if both_honest_no_source else 0.0)
    shared = sorted(supported_facts(left) & supported_facts(right))
    return {"sourceOverlap": overlap, "sharedSupportedFacts": shared,
            "passed": (both_honest_no_source or (bool(shared) and overlap > 0))
                      and not left.get("errors") and not right.get("errors")}


def diagnostics(raw: str, request_id: str) -> dict:
    for line in raw.splitlines():
        if f"request_id={request_id}" not in line and f"requestId={request_id}" not in line:
            continue
        match = re.search(r"diagnostics=(\{.*\})", line)
        if match:
            try:
                value = json.loads(match.group(1))
                missing = [key for key in ("model", "sourceIds") if not value.get(key)]
                return {**value, "missing": missing}
            except Exception:
                break
    return {"missing": ["diagnostics", "model", "sourceIds"]}


def validate_error(result: dict, status: int | None, code: str, request_id: str) -> list[str]:
    errors = []
    payload = result.get("serverError") if isinstance(result, dict) else None
    if status is None or status not in (200, 503, 504): errors.append("missing_http_status")
    if not isinstance(payload, dict) or payload.get("code") != code: errors.append("wrong_error_code")
    if not isinstance(payload, dict) or payload.get("retryable") is not True: errors.append("not_retryable")
    if not isinstance(payload, dict) or payload.get("requestId") != request_id: errors.append("wrong_request_id")
    if result.get("reply") or result.get("complete"): errors.append("false_success")
    return errors


def _request(url: str, path: str, body: dict, *, token: str | None = None,
             request_id: str | None = None, accept: str = "application/json"):
    headers = {"Content-Type": "application/json", "Accept": accept}
    if token: headers["Authorization"] = "Bearer " + token
    if request_id: headers["X-Request-Id"] = request_id
    request = urllib.request.Request(url + path, data=json.dumps(body).encode(), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=130) as response:
            return response.status, response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode("utf-8")


def _provision_http(base: str):
    email = f"rag-{secrets.token_hex(8)}@example.invalid"
    password = "RagAcceptance!" + secrets.token_hex(12)
    status, raw = _request(base, "/api/auth/register",
                           {"email": email, "password": password, "displayName": "RAG acceptance"})
    if status != 200:
        raise RuntimeError(f"fixture registration failed with HTTP {status}")
    payload = json.loads(raw)
    data = payload.get("data") or {}
    user_id = data.get("userId")
    if not user_id:
        raise RuntimeError("fixture registration omitted userId")
    verify = urllib.request.Request(
        base + f"/api/test/auth/{user_id}/verify-email", data=b"{}", method="POST",
        headers={"Content-Type": "application/json", "X-Test-Hook-Secret": "dev-test-hook-secret"})
    try:
        with urllib.request.urlopen(verify, timeout=15) as response:
            if response.status != 200:
                raise RuntimeError("fixture verification failed")
    except urllib.error.HTTPError as exc:
        raise RuntimeError(f"fixture verification failed with HTTP {exc.code}") from None
    status, raw = _request(base, "/api/auth/login", {"email": email, "password": password})
    if status != 200:
        raise RuntimeError(f"fixture login failed with HTTP {status}")
    token = login_token(json.loads(raw))
    status, _ = _request(base, "/api/billing/b2c/subscribe", {"paymentMethod": "DEMO"}, token=token)
    if status != 200:
        raise RuntimeError(f"fixture Premium activation failed with HTTP {status}")
    return user_id, token


def _seed_history_http(base: str, token: str, character: str) -> str:
    status, raw = _request(base, "/api/chat/sync", {
        "characterId": character, "conversationId": None,
        "userMessage": HISTORY[0][1], "assistantReply": HISTORY[1][1],
    }, token=token, request_id=f"accept-history-{uuid.uuid4()}")
    if status != 200:
        raise RuntimeError(f"history fixture failed with HTTP {status}")
    parsed = parse_sync(raw)
    conversation = parsed.get("conversationId")
    if not conversation:
        raise RuntimeError("history fixture omitted conversationId")
    return conversation


def _run_case(base: str, token: str, character: str, conversation: str, case: str, *, stream: bool):
    request_id = f"accept-{case}-{uuid.uuid4()}"
    body = {"characterId": character, "message": QUESTIONS[case], "conversationId": conversation,
            "siteCode": "cu-chi"}
    if stream:
        body["mode"] = "LIGHT_HINT"
    started = time.perf_counter()
    status, raw = _request(base, "/api/chat/messages/stream" if stream else "/api/chat/messages",
                           body, token=token, request_id=request_id,
                           accept="text/event-stream" if stream else "application/json")
    parsed = parse_sse(raw) if stream else parse_sync(raw)
    parsed.update({"status": status, "requestIdSent": request_id,
                   "durationMs": round((time.perf_counter() - started) * 1000)})
    parsed["validation"] = validate(case, parsed)
    return parsed


def arguments(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--main", required=True)
    parser.add_argument("--snapshot")
    parser.add_argument("--ai", required=True)
    parser.add_argument("--db", required=True)
    parser.add_argument("--docker-container", default="histar-postgres")
    parser.add_argument("--db-user", default="timelens")
    parser.add_argument("--output", required=True)
    parser.add_argument("--warm-passes", type=int, default=1)
    parser.add_argument("--expected-error", choices=("RAG_UNAVAILABLE", "RAG_TIMEOUT"))
    parser.add_argument("--http-fixtures", action="store_true",
                        help="provision isolated H2 test-profile fixtures through guarded local APIs")
    args = parser.parse_args(argv)
    args.main = local_url(args.main, backend=True)
    args.ai = local_url(args.ai)
    args.db = database_name(args.db)
    if args.snapshot:
        args.snapshot = local_url(args.snapshot, backend=True)
        if urllib.parse.urlsplit(args.main).port == urllib.parse.urlsplit(args.snapshot).port:
            raise ValueError("main and snapshot must be distinct backend ports")
    elif not args.expected_error:
        raise ValueError("--snapshot is required unless validating an injected expected error")
    if args.warm_passes < 1 or args.warm_passes > 5:
        raise ValueError("--warm-passes must be 1..5")
    return args


def main(argv=None) -> int:
    args = arguments(argv)
    database = None if args.http_fixtures else Database(args.db, args.docker_container, args.db_user)
    character = "a1000001-0000-4000-8000-00000000c001" if args.http_fixtures else database.character()
    targets = {"main": args.main}
    if args.snapshot: targets["snapshot"] = args.snapshot
    credentials = {}
    for name, base in targets.items():
        if args.http_fixtures:
            credentials[name] = _provision_http(base)
        else:
            user, email, password = database.profile()
            status, raw = _request(base, "/api/auth/login", {"email": email, "password": password})
            if status != 200:
                raise RuntimeError(f"{name} login failed with HTTP {status}")
            credentials[name] = (user, login_token(json.loads(raw)))
    report = {"schemaVersion": 1, "ai": args.ai, "database": args.db, "targets": targets,
              "cases": [], "passed": True}
    for pass_index in range(args.warm_passes + 1):
        phase = "cold" if pass_index == 0 else f"warm-{pass_index}"
        for case in QUESTIONS:
            row = {"phase": phase, "case": case, "results": {}}
            for name, base in targets.items():
                user, token = credentials[name]
                for stream in (False, True):
                    if case == "follow_up" and args.http_fixtures:
                        conv = _seed_history_http(base, token, character)
                    elif args.http_fixtures:
                        conv = None
                    else:
                        conv = database.conversation(user, character, seed_history=(case == "follow_up"))
                    key = "sse" if stream else "sync"
                    row["results"].setdefault(name, {})[key] = _run_case(
                        base, token, character, conv, case, stream=stream)
            if args.snapshot:
                row["comparison"] = {mode: compare(row["results"]["main"][mode], row["results"]["snapshot"][mode])
                                     for mode in ("sync", "sse")}
            row["passed"] = all(not result["validation"] for target in row["results"].values() for result in target.values())
            if args.snapshot:
                row["passed"] = row["passed"] and all(item["passed"] for item in row["comparison"].values())
            report["passed"] = report["passed"] and row["passed"]
            report["cases"].append(row)
    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"passed": report["passed"], "report": str(output), "cases": len(report["cases"])}, ensure_ascii=False))
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
