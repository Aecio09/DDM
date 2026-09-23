# BUILD_LOG — Mobile To-Do App (Kotlin Multiplatform)

Registro contínuo do desenvolvimento. Cada entrada documenta prompt, decisões,
ações, resultado, problemas e correções.

---

## Entrada 1 — Inicialização do repositório e planejamento do ambiente

### Prompt / Request
> "leia o ambiente seu objetivo esta nele, lembrese de criar um repositorio para esse nosso projeto e fazer commits sempre que ver que for relevante."

### Decision Summary
- O objetivo do projeto está descrito em `agent-Project-Scope.md`: construir um app móvel de tarefas (To-Do) em **Kotlin Multiplatform** com persistência SQLite, múltiplas telas, categorias, filtros, data/hora de vencimento e notificações locais agendadas.
- Decidiu-se usar **Compose Multiplatform (CMP)** com alvos **Android** e **Desktop (JVM)**:
  - Android é o alvo "móvel" principal do requisito.
  - Desktop permite executar e testar o app nesta máquina (sem emulador Android instalado).
  - A estrutura KMP fica preparada para adicionar iOS no futuro.
- Stack de versões escolhida (todas estáveis até Set/2026):
  - Kotlin 2.3.21
  - Compose Multiplatform 1.12.0
  - Gradle 9.7.1
  - AGP 8.13.2
  - SQLDelight 2.3.2 (SQLite tipado, multiplataforma)
  - kotlinx-datetime 0.8.0-x-compat, kotlinx-coroutines 1.11.0
  - Lifecycle ViewModel multiplataforma 2.11.0 (`org.jetbrains.androidx.lifecycle`)
- Criado repositório git local em `/home/aecio/Repositories/DDM` (branch `master`).
- Ambiente: não há Android SDK instalado; será baixado (cmdline-tools + platform-36 + build-tools) e `ANDROID_HOME` definido.

### Actions Performed
- `git init` no diretório do projeto; `git config user.name/user.email` locais.
- Download em segundo plano do Gradle 9.7.1 e das command line tools do Android SDK.
- Criado este `BUILD_LOG.md`.

### Result
- Repositório inicializado; downloads em andamento.

### Problems / Errors
- Download das ferramentas excedeu 120s no primeiro teste; movido para plano de fundo (nohup).

### Fixes Attempted
- Execução dos downloads com `nohup ... &` e registro em log.

### Current Status
- Em progresso (setup de ambiente).

---

## Entrada 2 — Ambiente configurado, stack de versões estabilizada e código do app criado

### Prompt / Request
- Continuar o desenvolvimento e corrigir o primeiro build.

### Context / Decisiones
- Android SDK instalado em `/home/aecio/Android/Sdk` (platform-36, build-tools 36.0.0, platform-tools); `ANDROID_HOME`/`ANDROID_SDK_ROOT` exportados e `local.properties` criado.
- Wrapper do Gradle gerado para o projeto; versão alvo do Gradle: **8.14.3**.
- Depois de investigar erros de compatibilidade (AGP 9.1+ exigido pela CMP 1.12 / compileSdk 37, e AGP 8.x sendo rejeitado pelo Gradle ≥ 9.6), a stack **estável** foi escolhida:
  - Kotlin **2.2.20**, Compose Multiplatform **1.10.3**, AGP **8.13.2**, Gradle **8.14.3**, compileSdk/targetSdk **36**, minSdk **26**.
  - SQLDelight **2.3.2**, kotlinx-datetime **0.6.2** (em vez de 0.8.0-0.6.x-compat, cuja `Instant` legada era separada de `kotlin.time.Instant` e causava mismatch), coroutines **1.11.0**, lifecycle multiplataforma **2.10.0**, activity-compose **1.10.1**, androidx-core **1.16.0**.
- Estrutura de módulo único `:composeApp` (androidTarget + `jvm("desktop")`), pacote `com.aecio.todo`, SQLDelight gerando em `com.aecio.todo.db` (dialeto `sqlite_3_18`).

### Actions Performed
- Escrito todo o código do app (schema `.sq`, modelos, repositórios, `AppContainer`, expect/actual de `NotificationScheduler`,
  `TodoViewModel`, navegação, telas TaskList/TaskEditor/Category, `MainActivity`, `AndroidManifest.xml`, `main.kt` desktop, ícone de notificação).
- `PRAGMA foreign_keys = ON` moveu para os drivers (Android/Desktop) via `driver.execute`, pois o parser do SQLDelight rejeita `ON`.
- Primeiro `assembleDebug` chegou até a geração do código SQLDelight; parou em erros de compilação Kotlin.

### Problems / Errors
- `AssembleDebug` falha com: ternário em Kotlin (`? :`) em `TodoViewModel.kt`; `lastInsertRowId()` inexistente no código gerado do SQLDelight 2.3;
  mismatch de `kotlin.time.Instant` vs `kotlinx.datetime.Instant` (resolvido com datetime 0.6.2);
  `TimePickerDialog` sem parâmetro `title`; `TopAppBar` experimental sem `@OptIn`.

### Fixes Applied
- `TodoDatabase.sq`: `insertCategory`/`insertTask` mantidos como INSERT simples; id retornado via queries `selectLastTaskId`/`selectLastCategoryId`
  (`SELECT id ... ORDER BY id DESC LIMIT 1`, padrão compatível com o dialeto sqlite_3_18; sem `lastInsertRowId`/`RETURNING`,
  que o dialeto 3.18 não suporta e o SQLite antigo do Android não executa).
- `TaskRepository.kt`/`CategoryRepository.kt`: inserts seguidos de `selectLast...Id().executeAsOne()`.
- `TodoViewModel.kt:130`: expressão `? :` substituída por `if (...) else ...`.
- `TaskEditorScreen.kt`: `title` adicionado aos diálogos de data e hora; tipos `Instant` unificados.
- `TaskListScreen.kt`/`CategoryScreen.kt`: `@OptIn(ExperimentalMaterial3Api::class)` e import de `Modifier.weight` adicionados.

### Errors (2ª tentativa de build)
- `TodoDatabase.sq:20` — dialeto `sqlite_3_18` rejeitou `RETURNING id`; solução: descartado `RETURNING` (também inviável em runtime Android < API 34) em favor das queries `selectLastTaskId`/`selectLastCategoryId`.
- `TaskQueries`/`CategoryQueries` inexistentes no código gerado (a geração produz uma única classe `TodoDatabaseQueries`); repositórios migrados para `TodoDatabaseQueries`.
- `database.taskQueries`/`categoryQueries` → `database.todoDatabaseQueries`.
- `driver` do construtor de `AppContainer` não acessível em função membro; `initSchema()` removido (os drivers Android/Desktop já criam o schema).
- `DatePickerDialog` do CMP material3 não tem parâmetro `title` (removido); `TimePickerDialog` exige `title` (mantido).
- Import de `androidx.compose.foundation.layout.weight` resolvia símbolo `internal` no CMP 1.10.3; removido — `Modifier.weight` é membro de `RowScope`/`ColumnScope`.

### Current Status
- Código corrigido; build pendente de reexecução pelo usuário (`./gradlew :composeApp:assembleDebug` e/ou `./gradlew :composeApp:run`).

---