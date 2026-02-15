# Stock Prediction System

Financial news analysis pipeline that scrapes CNBC articles, runs them through a local LLM (Ollama), and persists stock/market predictions to PostgreSQL.

## Tech Stack

- **Java 25**, **Spring Boot 4.0.1**
- **PostgreSQL** — article storage, predictions, entity relationships
- **Ollama** — local LLM inference (qwen3:14b)
- **Rome** — RSS feed parsing
- **Jsoup** — HTML scraping
- **Maven** — multi-module build

## Project Structure

```
stock-prediction-system/
├── common/                          # Shared library (entities, DTOs)
│   └── src/main/java/kz/kbtu/common/
│       ├── entity/
│       │   ├── Article.java         # Core entity: news article + analysis results
│       │   ├── Prediction.java      # LLM prediction (scope, direction, confidence)
│       │   ├── Company.java         # Stock company (ticker, country, sectors)
│       │   ├── Country.java         # Country reference data
│       │   ├── EconomySector.java   # Sector reference data
│       │   └── BaseEntity.java      # id, createdAt, updatedAt
│       └── dto/
│           ├── RssArticleDto.java       # RSS feed entry
│           ├── ArticleAnalysisDto.java  # LLM response structure
│           └── CompanyInfoDto.java      # LLM-generated company info
│
├── news-service/                    # Main application
│   └── src/main/java/kz/kbtu/newsservice/
│       ├── service/
│       │   ├── NewsProcessingService.java   # Pipeline orchestrator
│       │   ├── RssFeedService.java          # RSS parsing (Rome)
│       │   ├── ArticleScraperService.java   # HTML scraping + paywall detection
│       │   ├── OllamaAnalysisService.java   # LLM prompt building + response parsing
│       │   ├── ArticleService.java          # Article CRUD + prediction persistence
│       │   ├── CompanyService.java          # Company auto-creation via LLM
│       │   └── FileStorageService.java      # File backup of articles
│       ├── repository/                      # Spring Data JPA repositories
│       ├── controller/
│       │   └── NewsController.java          # POST /api/news/scrape, GET /api/news/health
│       └── config/
│           └── DataInitializer.java         # Seeds sectors and countries on startup
│
├── docker-compose.yml               # Kafka + Zookeeper (infrastructure)
└── .env                             # DB, Ollama, server config
```

## Pipeline

```
RSS Feed (CNBC)
  │
  ├─ 1. Fetch & parse RSS entries (RssFeedService)
  ├─ 2. Batch dedup check against DB — single IN query (ArticleService)
  ├─ 3. Create article record (ArticleService)
  ├─ 4. Scrape article HTML with Jsoup (ArticleScraperService)
  │      ├─ Paywall detected → skip
  │      ├─ Strip all attributes, remove scripts/nav/ads
  │      └─ Save cleaned HTML to Article.content
  ├─ 5. Send title + content to Ollama (OllamaAnalysisService)
  │      └─ Returns: summary, sentiment, companies, sectors, countries, predictions
  ├─ 6. Persist analysis results (ArticleService)
  │      ├─ Link mentioned companies/countries/sectors (M:M)
  │      ├─ Create predictions with scope-specific targets
  │      └─ Auto-create unknown companies via LLM
  └─ 7. File backup (FileStorageService)
```

## Entity Model

```
Article ──1:N──▸ Prediction
   │                 │
   ├──M:M──▸ Company ◂── Prediction (scope=COMPANY)
   ├──M:M──▸ Country ◂── Prediction (scope=COUNTRY)
   └──M:M──▸ EconomySector ◂── Prediction (scope=SECTOR)
```

**Prediction scopes**: COMPANY, MULTI_TICKER, SECTOR, COUNTRY
**Directions**: BULLISH, BEARISH, NEUTRAL, MIXED, VOLATILE
**Time horizons**: SHORT_TERM, MID_TERM, LONG_TERM

## LLM Analysis

- **Model**: qwen3:14b (configurable via `OLLAMA_MODEL`)
- **Temperature**: 0.1 (deterministic)
- **Prompt**: Includes article content, available sector codes from DB, scope selection rules, confidence guidelines
- **Output**: JSON with summary, sentiment, mentioned entities, and predictions with rationale/evidence

## Reference Data (seeded on startup)

**8 sectors**: TECH, HEALTHCARE, FINANCE, CONSUMER, ENERGY, INDUSTRIALS, MATERIALS, REAL_ESTATE

**21 countries**: US, CN, JP, KR, TW, DE, GB, FR, NL, CH, IE, IN, CA, AU, SG, HK, IL, SA, AE, BR, KZ

## Configuration

Environment variables (`.env`):
```
DB_URL=jdbc:postgresql://localhost:5432/predictions
DB_USERNAME=postgres
DB_PASSWORD=<password>
SERVER_PORT=8080
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:14b
```

`ddl-auto: create` — schema is recreated from entities on each startup.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/news/scrape` | Trigger RSS feed processing (async) |
| GET | `/api/news/health` | Health check |
