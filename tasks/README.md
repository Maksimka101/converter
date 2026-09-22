# Задачи

Один md-файл на задачу: `NNNN-slug.md`. Готовые задачи не удаляются, это база знаний.

## Поля
- `status`: idea | discuss | todo | doing | done | dropped
- `priority`: high | medium | low
- `tags`: например parser, ui, core, rates, decision, backlog
- `depends`: id задач
- `updated`: обновлять при каждом изменении

## Разделы
Суть → Обсуждение / варианты → Решение → Как реализовано

## Индекс
`./tasks/index.sh` пересобирает `tasks/INDEX.md`.
