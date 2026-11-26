# Web ЛР1 — перевірка контракту OpenAPI (О1.3)

**Тема:** C11 «Поштомати»  
**Дата огляду:** 26.11.2026  
**Контракт:** [`contracts/openapi.yaml`](../../contracts/openapi.yaml)  
**Гілка:** `feature/openapi-olena`  
**Pull Request:** [№3 — доповнення OpenAPI-контракту](https://github.com/elllenoshka/parcel_locker_system/pull/3)  
**Версія специфікації:** OpenAPI 3.1.0

## 1. Мета перевірки

Перевірити OpenAPI-контракт для першої лабораторної роботи: чи правильно описані маршрути, запити, відповіді та можливі помилки. Також перевірити відображення документації у Swagger Editor.

## 2. Що перевірено

| Перевірка | Результат | Свідчення |
|---|---|---|
| Версія OpenAPI | У файлі вказано `3.1.0` | Початок `contracts/openapi.yaml` |
| Відображення документації | Контракт імпортовано до Swagger Editor, маршрути відображаються | Ручний перегляд у Swagger Editor |
| `GET /health` | Маршрут описано | Розділ Health |
| `GET /v1/lockers` | Описано параметр `size` зі значеннями S, M, L та прикладом для M | Розділ Lockers → Parameters |
| `GET /v1/parcels` | Описано список посилок із `limit` та `cursor` | Розділ Parcels |
| `POST /v1/parcels` | Описано `X-User-Id`, обов'язковий `Idempotency-Key`, приклади `normal` (S), `scarce` (L, `x-capacity: 1`), `missing` (404) | Контракт → Parcels → Request body; приклади відображено в Swagger Editor |
| `POST /v1/parcels` — успіх | Описано `201`, JSON-приклад і заголовок `Location` | Parcels → Responses |
| `POST /v1/parcels` — помилки | Описано `400`, `404`, `409`, `422`; для помилок використано `application/problem+json` | Parcels → Responses, контракт |
| `GET /v1/parcels/{id}` | Додано маршрут і приклади відповідей `200`, `400`, `404` | Parcels → Get parcel by ID |
| Посилання на компоненти `$ref` | Виявлені посилання на `#/components/schemas` та `#/components/headers` відповідають оголошеним компонентам (перевірка за текстом файлу) | `contracts/openapi.yaml` |

**Зміни в межах PR №3:** додано `GET /v1/parcels/{id}`, документовано фільтр `size` у списку поштоматів, оновлено застарілі одиничні `example` у схемах на масиви `examples` для OpenAPI 3.1.

## 3. Як відтворити ручну перевірку

1. Відкрити [Swagger Editor](https://editor.swagger.io/).
2. Обрати **File → Import URL**.
3. Імпортувати:
   `https://raw.githubusercontent.com/elllenoshka/parcel_locker_system/feature/openapi-olena/contracts/openapi.yaml`.
4. Перевірити список маршрутів.
5. Розгорнути `GET /v1/lockers` і перевірити параметр `size` та значення S, M, L.
6. Розгорнути `POST /v1/parcels`, перевірити приклади тіла запиту й відповіді `201`, `400`, `404`, `409`, `422`.
7. Перевірити, що `GET /v1/parcels/{id}` має приклади успішної відповіді та помилок.

## 3.1. Узгодження з засобом перевірки викладача

- Для `POST /v1/parcels` додано `x-course: main` і заголовок `Idempotency-Key`. Їх підтримка сервером **поки не підтверджена**.
- Приклади `normal`, `scarce` і `missing` беруть ідентифікатори з `web/src/main/resources/db/migration/V2__insert_test_data.sql`: `locker-0001` має S; у `locker-0002` є рівно одна вільна комірка L; `locker-does-not-exist` відсутній у демоданих.
- У корені додано попередній `checks.yaml` за структурою `joint/checks-template.yaml` викладача. Наразі він містить лише Web і посилається на `contracts/openapi.yaml`. Налаштування DPC та спільний сценарій залишаються незавершеними.
- Після цих змін контракт повторно імпортовано у Swagger Editor; іменовані приклади та заголовок `Idempotency-Key` відображаються. Автоматичну перевірку Redocly наведено нижче.

## 3.2. Автоматична перевірка OpenAPI

- **Засіб:** Redocly CLI `2.83.0`, стандартні правила `recommended`.
- **Команда:** `npx --yes @redocly/cli@2.83.0 lint contracts/openapi.yaml` (виконується з кореня репозиторію).
- **Середовище:** GitHub Actions, Node.js 22; конфігурація — [`.github/workflows/openapi-lint.yml`](../../.github/workflows/openapi-lint.yml).
- **Дата:** 26.11.2026.
- **Результат:** **успішно (exit code 0); 0 помилок, 2 попередження**.
- **Свідчення:** [GitHub Actions, запуск №45881801036](https://github.com/elllenoshka/parcel_locker_system/actions/runs/45881801036).
- **Попередження:** `info-license` — у `info` не задана ліцензія; `no-server-example.com` — сервер вказаний як `http://localhost:8080`. Для навчального локального стенда це не блокувало валідацію.
- **Виправлена помилка:** попередній запуск [№45881720675](https://github.com/elllenoshka/parcel_locker_system/actions/runs/45881720675) виявив чотири `security-defined`. Для стану до ЛР3 додано явне `security: []` на рівні контракту; `X-User-Id` залишається навчальним заголовком, а не механізмом автентифікації.

## 4. Що поки не підтверджено

- **Робота HTTP API:** запити `Try it out` не виконувалися, тому відповідність Java-реалізації контракту не підтверджена.
- **Узгодження поля `recipientPhone`:** у контракті й поточній схемі БД воно обов'язкове, тоді як приклад бронювання в картці C11 містить `locker_id`, `size`, `weight_g`. Потрібне командне рішення з Іриною Чех.
- **Фінальне прийняття PR №3:** PR відкритий; зміни ще не об'єднані з `main`.

## 5. Висновок

OpenAPI-контракт для Web ЛР1 доповнено та перевірено у Swagger Editor. Також виконано перевірку через Redocly, яка завершилася без помилок, але з двома попередженнями.

Надалі потрібно узгодити контракт із базою даних та перевірити його роботу разом із Java-сервером.
