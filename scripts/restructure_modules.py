#!/usr/bin/env python3
"""Restructure domain modules into entity/repository/service/impl folders."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "java" / "com" / "histar" / "be"

MODULES = [
    ("profile", "Profile", []),
    ("location", "Location", []),
    ("character", "CharacterEntity", ["findByLocationId"]),
    ("photopair", "PhotoPair", ["findByLocationIdOrderBySortOrder"]),
    ("panorama", "Panorama", []),
    ("hotspot", "Hotspot", []),
    ("quest", "Quest", ["findByLocationId"]),
    ("userquestprogress", "UserQuestProgress", []),
    ("badge", "Badge", []),
    ("userbadge", "UserBadge", []),
    ("conversation", "Conversation", []),
    ("message", "Message", ["findByConversationIdOrderByCreatedAt"]),
    ("checkin", "Checkin", []),
    ("photoframe", "PhotoFrame", []),
    ("usercreation", "UserCreation", []),
    ("campaign", "Campaign", []),
]

SERVICE_EXTRA_METHODS = {
    "profile": [
        ("Optional<Profile> findByEmail(String email);", "return repository.findByEmail(email);"),
    ],
    "character": [
        (
            "List<CharacterEntity> findByLocationId(UUID locationId);",
            "return repository.findByLocationId(locationId);",
        ),
    ],
    "photopair": [
        (
            "List<PhotoPair> findByLocationIdOrderBySortOrder(UUID locationId);",
            "return repository.findByLocationIdOrderBySortOrder(locationId);",
        ),
    ],
    "quest": [
        (
            "List<Quest> findByLocationId(UUID locationId);",
            "return repository.findByLocationId(locationId);",
        ),
    ],
    "message": [
        (
            "List<Message> findByConversationIdOrderByCreatedAt(UUID conversationId);",
            "return repository.findByConversationIdOrderByCreatedAt(conversationId);",
        ),
    ],
}

REPOSITORY_NAME = {
    "character": "Character",
}

SERVICE_NAME = {
    "character": "Character",
    "userquestprogress": "UserQuestProgress",
    "userbadge": "UserBadge",
    "usercreation": "UserCreation",
    "photopair": "PhotoPair",
    "photoframe": "PhotoFrame",
}


def service_base_name(module: str, entity: str) -> str:
    return SERVICE_NAME.get(module, entity)


def repository_base_name(module: str, entity: str) -> str:
    return REPOSITORY_NAME.get(module, entity)


def move_entity(module: str, entity: str) -> None:
    pkg = ROOT / module
    old = pkg / f"{entity}.java"
    entity_dir = pkg / "entity"
    new = entity_dir / f"{entity}.java"
    if new.exists():
        return
    if not old.exists():
        if entity_dir.joinpath(f"{entity}.java").exists():
            return
        raise FileNotFoundError(old)
    entity_dir.mkdir(parents=True, exist_ok=True)
    content = old.read_text(encoding="utf-8")
    content = content.replace(f"package com.histar.be.{module};", f"package com.histar.be.{module}.entity;")
    new.write_text(content, encoding="utf-8")
    old.unlink()


def move_repository(module: str, entity: str) -> None:
    pkg = ROOT / module
    repo_base = repository_base_name(module, entity)
    old = pkg / f"{repo_base}Repository.java"
    repo_dir = pkg / "repository"
    new = repo_dir / f"{repo_base}Repository.java"
    if new.exists():
        return
    if not old.exists():
        if repo_dir.joinpath(f"{entity}Repository.java").exists():
            return
        raise FileNotFoundError(old)
    repo_dir.mkdir(parents=True, exist_ok=True)
    content = old.read_text(encoding="utf-8")
    content = content.replace(f"package com.histar.be.{module};", f"package com.histar.be.{module}.repository;")
    content = re.sub(
        rf"import com\.histar\.be\.{module}\.{entity};",
        f"import com.histar.be.{module}.entity.{entity};",
        content,
    )
    if f"import com.histar.be.{module}.entity.{entity};" not in content and entity in content:
        content = content.replace(
            f"package com.histar.be.{module}.repository;\n\nimport java",
            f"package com.histar.be.{module}.repository;\n\nimport com.histar.be.{module}.entity.{entity};\nimport java",
        )
    new.write_text(content, encoding="utf-8")
    old.unlink()


def create_service(module: str, entity: str) -> None:
    svc = service_base_name(module, entity)
    repo_base = repository_base_name(module, entity)
    pkg = f"com.histar.be.{module}"
    extras = SERVICE_EXTRA_METHODS.get(module, [])
    extra_imports = []
    if any("List<" in m for m, _ in extras):
        extra_imports.append("import java.util.List;")
    if any("Optional<" in m for m, _ in extras):
        extra_imports.append("import java.util.Optional;")

    iface = ROOT / module / "service" / f"{svc}Service.java"
    if iface.exists():
        return
    iface.parent.mkdir(parents=True, exist_ok=True)

    extra_iface = "\n".join(f"    {m}" for m, _ in extras)
    extra_block = f"\n{extra_iface}" if extra_iface else ""

    iface.write_text(
        f"""package {pkg}.service;

import {pkg}.entity.{entity};
import java.util.List;
import java.util.UUID;
{chr(10).join(extra_imports)}

public interface {svc}Service {{

    List<{entity}> findAll();

    {entity} findById(UUID id);

    {entity} save({entity} entity);

    void deleteById(UUID id);

    long count();{extra_block}
}}
""",
        encoding="utf-8",
    )

    impl = ROOT / module / "service" / "impl" / f"{svc}ServiceImpl.java"
    impl.parent.mkdir(parents=True, exist_ok=True)
    extra_methods_impl = ""
    for sig, body in extras:
        method_sig = sig.strip().rstrip(";")
        extra_methods_impl += f"""
    @Override
    public {method_sig} {{
        {body}
    }}
"""

    impl.write_text(
        f"""package {pkg}.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import {pkg}.entity.{entity};
import {pkg}.repository.{repo_base}Repository;
import {pkg}.service.{svc}Service;
import java.util.List;
import java.util.UUID;
{chr(10).join(i for i in extra_imports if i not in ["import java.util.List;", "import java.util.UUID;"])}
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class {svc}ServiceImpl implements {svc}Service {{

    private final {repo_base}Repository repository;

    @Override
    public List<{entity}> findAll() {{
        return repository.findAll();
    }}

    @Override
    public {entity} findById(UUID id) {{
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("{entity} not found: " + id));
    }}

    @Override
    public {entity} save({entity} entity) {{
        return repository.save(entity);
    }}

    @Override
    public void deleteById(UUID id) {{
        repository.deleteById(id);
    }}

    @Override
    public long count() {{
        return repository.count();
    }}{extra_methods_impl}
}}
""",
        encoding="utf-8",
    )


def main() -> None:
    for module, entity, _ in MODULES:
        move_entity(module, entity)
        move_repository(module, entity)
        create_service(module, entity)
        print(f"OK {module}")


if __name__ == "__main__":
    main()
