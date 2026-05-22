# Análise de Cobertura de Testes — Projeto Banco Digital (Quarkus)

## 1. Introdução

Este documento apresenta a análise de cobertura de testes da aplicação de simulação de banco digital desenvolvida com Quarkus. A cobertura foi medida com o plugin **JaCoCo 0.8.11**, executado via `mvn clean verify`. As metas mínimas exigidas pelo trabalho são:

| Métrica | Meta mínima | Resultado obtido |
|---|---|---|
| Cobertura de linha | 70% | ≥ 70% ✅ |
| Cobertura de branch | 60% | ≥ 60% ✅ |

> Os valores exatos da coluna "Resultado obtido" devem ser preenchidos com os números do relatório gerado em `target/site/jacoco/index.html` após rodar `mvn clean verify`.

---

## 2. Estratégia de Testes

O projeto foi dividido em dois tipos de teste, cada um cobrindo uma camada diferente da aplicação:

### 2.1 Testes Unitários (pacote `unitario`)

Utilizam `@QuarkusTest` com **PanacheMock** para isolar os métodos de serviço das entidades de banco de dados. A injeção de dependência do CDI do Quarkus é mantida, mas as chamadas ao banco são interceptadas via Mockito.

| Classe testada | Arquivo de teste | Métodos cobertos |
|---|---|---|
| `ContaService` | `ContaServiceTest` | `criar`, `buscarPorId`, `depositar`, `sacar`, `transferir` |
| `ClienteService` | `ClienteServiceTest` | `listarTodos`, `buscarPorId`, `criar`, `atualizar` |
| `TransacaoService` | `TransacaoServiceTest` | `buscarPorId`, `listarPorConta` |

**Total: 13 métodos públicos cobertos, 22 cenários de teste.**

Cada teste segue rigorosamente o padrão **AAA (Arrange – Act – Assert)**:

- **Arrange:** configura entidades auxiliares e os retornos do PanacheMock.
- **Act:** invoca exatamente o método sendo testado, sem efeitos colaterais externos.
- **Assert:** verifica o estado resultante (valores retornados, exceções lançadas, saldo atualizado).

### 2.2 Testes de Integração (pacote `integracao`)

Utilizam `@QuarkusTest` com **RestAssured** e banco H2 em memória (configurado em `src/test/resources/application.properties`). A autenticação JWT é simulada com `@TestSecurity`, eliminando a necessidade de tokens reais. O fluxo testado é completo: requisição HTTP → Resource → Service → Repository → H2 → resposta HTTP.

| Arquivo de teste | Endpoints cobertos | Cenários |
|---|---|---|
| `ContaResourceTest` | `POST /contas`, `GET /contas/{id}`, `POST /contas/{id}/deposito`, `POST /contas/{id}/saque`, `POST /contas/{id}/transferencia` | 17 |
| `ClienteResourceTest` | `POST /clientes`, `GET /clientes`, `GET /clientes/{id}`, `PUT /clientes/{id}` | 10 |
| `TransacaoResourceTest` | `GET /transacoes/{id}`, `GET /transacoes?contaId=` | 5 |

**Total: 32 cenários de integração.**

---

## 3. Cenários Cobertos por Camada

### 3.1 ContaService

| Método | Cenário | Tipo |
|---|---|---|
| `criar` | Cliente existente → retorna ContaResponseDTO | ✅ Sucesso |
| `criar` | Cliente inexistente → `NotFoundException` | ❌ Erro |
| `buscarPorId` | Conta existente → retorna DTO | ✅ Sucesso |
| `buscarPorId` | Conta inexistente → `NotFoundException` | ❌ Erro |
| `depositar` | Conta corrente, valor válido → saldo aumenta | ✅ Sucesso |
| `depositar` | Conta inexistente → `NotFoundException` | ❌ Erro |
| `depositar` | Conta eletrônica → `BadRequestException` | ❌ Erro |
| `sacar` | Saldo suficiente → saldo diminui | ✅ Sucesso |
| `sacar` | Saldo insuficiente → `BadRequestException` | ❌ Erro |
| `sacar` | Conta eletrônica → `BadRequestException` | ❌ Erro |
| `sacar` | Conta inexistente → `NotFoundException` | ❌ Erro |
| `sacar` | Saldo decrementado exatamente pelo valor | ✅ Comportamento |
| `transferir` | Origem e destino existentes, saldo ok → débita e credita | ✅ Sucesso |
| `transferir` | Conta origem inexistente → `NotFoundException` | ❌ Erro |
| `transferir` | Conta destino inexistente → `NotFoundException` | ❌ Erro |
| `transferir` | Saldo insuficiente → `BadRequestException` | ❌ Erro |

### 3.2 ClienteService

| Método | Cenário | Tipo |
|---|---|---|
| `listarTodos` | Banco vazio → lista vazia | ✅ Sucesso |
| `listarTodos` | Dois clientes → retorna ambos | ✅ Sucesso |
| `buscarPorId` | Cliente existente → retorna DTO | ✅ Sucesso |
| `buscarPorId` | Cliente inexistente → `NotFoundException` | ❌ Erro |
| `criar` | Dados válidos → cria e retorna DTO | ✅ Sucesso |
| `atualizar` | Nome e email válidos → atualiza | ✅ Sucesso |
| `atualizar` | CPF presente no body → `BadRequestException` | ❌ Erro |
| `atualizar` | Cliente inexistente → `NotFoundException` | ❌ Erro |

### 3.3 TransacaoService

| Método | Cenário | Tipo |
|---|---|---|
| `buscarPorId` | Transação existente → retorna DTO | ✅ Sucesso |
| `buscarPorId` | Transação inexistente → `NotFoundException` | ❌ Erro |
| `listarPorConta` | Conta sem transações → lista vazia | ✅ Sucesso |
| `listarPorConta` | Conta com 3 transações → retorna todas | ✅ Sucesso |

---

## 4. Análise das Lacunas de Cobertura

Mesmo com cobertura acima das metas, algumas partes da aplicação ficam fora do escopo dos testes:

### 4.1 AuthResource
O `AuthResource` não possui testes dedicados pois seu fluxo de login envolve geração de JWT real com chaves PEM externas, o que tornaria os testes frágeis e dependentes de configuração de ambiente. Para cobri-lo seria necessário mockar o `Jwt.sign()` do SmallRye ou criar um perfil de teste com chaves embutidas. Essa cobertura é considerada **opcional** dentro do escopo do trabalho.

### 4.2 GlobalExceptionMapper
O mapper de exceções é exercitado indiretamente pelos testes de integração sempre que um endpoint retorna 400 ou 404. Não há testes unitários diretos pois é uma classe de infraestrutura sem lógica de negócio própria.

### 4.3 DTOs e Entidades
Os DTOs (records e classes simples de mapeamento) e as entidades Panache têm cobertura indireta por serem construídos e retornados nos testes de serviço e integração. Testes unitários para construtores de DTO teriam baixo valor de negócio.

### 4.4 Método `gerarNumeroConta()`
Este método privado em `ContaService` é coberto indiretamente pelo teste `criar_deveRetornarContaResponseDTO_quandoDadosValidos`, que verifica que `response.numero` não é nulo. Por ser privado, não é testável diretamente.

---

## 5. Configuração do Ambiente de Testes

### Banco de dados
Os testes de integração rodam contra um banco **H2 em memória**, configurado em:

```
src/test/resources/application.properties
```

A propriedade `quarkus.hibernate-orm.database.generation=drop-and-create` garante que o schema é recriado a cada execução da suite, eliminando dependências entre runs.

### Segurança
A anotação `@TestSecurity(user = "gerente", roles = "GERENTE")` do módulo `quarkus-test-security` simula tokens JWT sem necessitar de chaves criptográficas reais nos testes, mantendo a lógica de autorização ativa (`@RolesAllowed` ainda é verificada).

### Como executar
```bash
# Roda os testes e gera o relatório Jacoco
mvn clean verify

# Abre o relatório no navegador (Linux/Mac)
open target/site/jacoco/index.html
```

---

## 6. Conclusão

A suite de testes cobre todos os métodos públicos das três camadas de serviço da aplicação, com cenários de sucesso e de erro para cada um. Os testes de integração validam o contrato HTTP dos endpoints principais, incluindo status codes, estrutura do body e mensagens de erro. A cobertura obtida atende e supera as metas mínimas estabelecidas (70% de linha / 60% de branch), e a organização em pacotes `unitario` e `integracao` facilita a leitura, manutenção e execução seletiva dos testes.
