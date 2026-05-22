# API Bancária — Quarkus

## Pré-requisitos
- Java 21+
- Maven
- PostgreSQL rodando localmente

## Configuração do banco

Crie o banco de dados no PostgreSQL:
```sql
CREATE DATABASE banco_db;
CREATE USER quarkus WITH PASSWORD 'quarkus';
GRANT ALL PRIVILEGES ON DATABASE banco_db TO quarkus;
```

## Geração das chaves JWT

Na raiz do projeto, execute:
```bash
java GenerateKeys.java
```
Isso criará `privateKey.pem` e `publicKey.pem` em `src/main/resources/`.

## Rodando o projeto

```bash
./mvnw quarkus:dev
```

## Endpoints disponíveis

### Autenticação
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | /auth/login | Gera token JWT |

### Clientes (requer role: GERENTE)
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | /clientes | Lista todos |
| GET | /clientes/{id} | Busca por ID |
| POST | /clientes | Cria cliente |
| PUT | /clientes/{id} | Atualiza cliente |

### Contas
| Método | Rota | Role | Descrição |
|--------|------|------|-----------|
| POST | /contas | GERENTE | Cria conta |
| GET | /contas/{id} | GERENTE, CLIENTE | Busca conta |
| POST | /contas/{id}/deposito | GERENTE, CLIENTE | Realiza depósito |
| POST | /contas/{id}/saque | GERENTE, CLIENTE | Realiza saque |
| POST | /contas/{id}/transferencia | GERENTE, CLIENTE | Realiza transferência |

### Transações
| Método | Rota | Role | Descrição |
|--------|------|------|-----------|
| GET | /transacoes/{id} | GERENTE, CLIENTE | Busca transação |
| GET | /transacoes?contaId={id} | GERENTE, CLIENTE | Histórico da conta |

## Usuários de teste

Crie um cliente GERENTE diretamente no banco para o primeiro acesso:
```sql
INSERT INTO cliente (nome, cpf, email, senha, role)
VALUES ('Admin', '000.000.000-00', 'admin@banco.com', 'senha123', 'GERENTE');
```

Depois use `POST /auth/login` com as credenciais acima para obter o token.
