# DPC ЛР4 — пакетний звіт (Java 21 + Apache Spark)

Тема **C11 «Поштомати»**, робота за О4.2 і О4.3 з методички. **Увесь програмний код модуля написано Java**; збірка Maven. Цей модуль є частиною `dpc/`.

## Вхідні дані й модель

- `fixtures/history.csv`: `parcel_id, locker_id, size, dropped, picked, sender_id`.
- `fixtures/lockers.csv`: `locker_id, address, tz`.
- Малий контрольний набір: 8 посилок і 2 поштомати; великі дані з О4.1 для ядра О4.2 не потрібні.
- Перед Spark-розрахунком контрольні дані перетворюються в **Parquet** за допомогою `SparkFixtureToParquet`.
- Ключ групування: `locker_id`, `day`, `size` із приєднанням `address`.
- `parcel_count` — кількість посилок, закладених у цей день.
- `picked_count` — кількість посилок із цієї групи, які згодом забрано (не кількість видач саме цього дня).
- **Поки що навчальне припущення:** `day` — дата закладання в UTC. Потрібно узгодити це з командою для остаточної реалізації.
- Зразок обмежений простим CSV без ком та лапок у полях; для повноцінних даних призначений Parquet.

## Збирання та контрольний тест

Потрібні Java 21 і Maven 3.9+, а для запуску Spark-задачі — сумісна інсталяція Apache Spark **4.2.0** (таку версію використовує каркас викладача).

Команди з кореня спільного репозиторію:

```bash
mvn -f dpc/analytics/pom.xml test
mvn -f dpc/analytics/pom.xml package
```

Тести порівнюють Spark DataFrame та незалежний Java-еталон на тих самих даних. Очікується 7 груп, сума `parcel_count` = 8 і сума `picked_count` = 6. Ці значення підтверджені автоматичним тестом: **4 тести, 0 помилок, 0 невдалих**. [Результат GitHub Actions](https://github.com/elllenoshka/parcel_locker_system/actions/runs/45883600659) (Java 21, Maven, локальна Spark-сесія у CI).

## Запуск Spark із готовим JAR

Нижче `spark-submit` треба виконувати в середовищі зі Spark 4.2.0 і Java 21; шляхи — приклад для поточного каталогу:

```bash
spark-submit --master local[2] \
  --class com.parcellocker.analytics.SparkFixtureToParquet \
  dpc/analytics/target/dpc-analytics-0.1.0-SNAPSHOT.jar \
  dpc/analytics/fixtures/history.csv dpc/analytics/fixtures/lockers.csv \
  data/dpc-history data/dpc-lockers

spark-submit --master local[2] \
  --class com.parcellocker.analytics.SparkBatchReport \
  dpc/analytics/target/dpc-analytics-0.1.0-SNAPSHOT.jar \
  data/dpc-history data/dpc-lockers data/dpc-report
```

Незалежний Java-еталон запускається окремо, без Spark, після Maven-збірки:

```bash
java -cp dpc/analytics/target/classes \
  com.parcellocker.analytics.ReferenceReport \
  dpc/analytics/fixtures/history.csv dpc/analytics/fixtures/lockers.csv
```

## Межі готовності за методичкою

- **О4.2 (Ядро):** реалізовано Java Spark DataFrame-з'єднання й групування; **ще потрібно** зв'язати запуск із `POST /v1/reports/main/refresh` та стан/результат із `GET /v1/reports/main` у Spring Boot DPC API.
- **О4.3 (Ядро):** є незалежний Java-еталон і тест порівняння; **ще потрібно** реалізувати `GET /v1/reports/main/reference` і перевірити наскрізне виконання в Docker.
- О4.1, О4.4, О4.5 не входять у «Ядро»; їх поки не реалізовуємо.

Ця гілка базується на `dpc-lab1`. **Не об'єднувати без узгодження з Владом**, оскільки він паралельно розробляє DPC API.
