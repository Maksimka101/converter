#!/data/data/com.termux/files/usr/bin/env python3
"""Проверяет файлы задач на расхождения. ./lint.sh — все проверки; ./lint.sh N — кто ссылается на #N."""
import glob, os, re, sys, datetime, collections

SECTIONS = ["## Суть", "## Обсуждение / варианты", "## Решение", "## Как реализовано"]
STATUSES = {"idea", "discuss", "todo", "doing", "done", "dropped"}
FIELDS = ["id", "title", "status", "priority", "tags", "depends", "created", "updated"]
NOISE = [(r"^\s*- \[[ x]\]", "чеклист-пункт: инструкция агенту, а не знание"),
         (r"ожидается от Claude|сабагент|агент подготовит", "процессная ремарка"),
         (r"\bTODO\b|\bFIXME\b|\?\?\?", "заглушка")]

os.chdir(os.path.dirname(os.path.abspath(__file__)))
files = sorted(glob.glob("[0-9]*.md"))
tasks, warns = {}, []
def warn(f, m): warns.append((f, m))

for p in files:
    raw = open(p, encoding="utf-8").read()
    parts = raw.split("---", 2)
    if len(parts) < 3:
        warn(p, "нет frontmatter"); continue
    fm = {k.strip(): v.strip() for k, _, v in
          (l.partition(":") for l in parts[1].strip().splitlines()) if k.strip()}
    secs, cur = collections.OrderedDict(), None
    for line in parts[2].splitlines():
        if line.startswith("## "):
            cur = line.strip(); secs[cur] = []
        elif cur is not None:
            secs[cur].append(line)
    secs = {k: "\n".join(v).strip() for k, v in secs.items()}
    tasks[p] = (fm, secs, parts[2])

ids = {fm["id"]: p for p, (fm, _, _) in tasks.items() if "id" in fm}

for p, (fm, secs, body) in tasks.items():
    for f in FIELDS:
        if f not in fm: warn(p, f"нет поля {f}")
    if fm.get("status") not in STATUSES: warn(p, f"статус {fm.get('status')!r} вне набора")
    if fm.get("id") != str(int(p[:4])): warn(p, f"id={fm.get('id')} не совпадает с именем файла")
    if ":" in fm.get("title", "") and not fm.get("title", "").startswith('"'):
        pass  # двоеточие в title допустимо, index.sh его переживает
    for s in SECTIONS:
        if s not in secs: warn(p, f"нет секции «{s}»")
    if not secs.get("## Суть"): warn(p, "пустая «Суть»")
    if fm.get("status") == "done" and not secs.get("## Как реализовано"):
        warn(p, "status done, но «Как реализовано» пустой")
    if fm.get("status") == "discuss" and secs.get("## Решение"):
        warn(p, "решение записано, а статус всё ещё discuss")
    for d in re.findall(r"\d+", fm.get("depends", "")):
        if d not in ids: warn(p, f"depends на несуществующий #{d}")
    for r in set(re.findall(r"#(\d+)", body)):
        if r not in ids: warn(p, f"ссылка на несуществующий #{r}")
    for link in set(re.findall(r"\(research/([^)]+)\)", body)):
        if not os.path.exists("research/" + link): warn(p, f"нет файла research/{link}")
    for line in body.splitlines():
        for pat, why in NOISE:
            if re.search(pat, line):
                warn(p, f"{why}: {line.strip()[:55]}"); break
    mt = datetime.date.fromtimestamp(os.path.getmtime(p)).isoformat()
    if fm.get("updated") and fm["updated"] < mt:
        warn(p, f"updated={fm['updated']}, а файл правлен {mt}")

# кто на кого ссылается
refs = collections.defaultdict(set)
for p, (fm, _, body) in tasks.items():
    for r in set(re.findall(r"#(\d+)", body)):
        if r in ids and r != fm.get("id"): refs[r].add(fm.get("id"))

if len(sys.argv) > 1:
    n = sys.argv[1].lstrip("#")
    if n not in ids: sys.exit(f"нет задачи #{n}")
    print(f"#{n} {ids[n]}")
    print("  ссылаются:", ", ".join("#" + x for x in sorted(refs[n], key=int)) or "никто")
    print("  правя решение #%s, пройди по ним" % n)
    sys.exit(0)

for p, m in warns: print(f"{p}: {m}")
print(f"\n{len(files)} задач, {len(warns)} замечаний")
