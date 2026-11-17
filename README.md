# 🔎 MCP Globbing: servidor MCP em Java para buscar arquivos e contar páginas de PDF

Trabalho final da disciplina **Serviços Web** (IFPR). Projeto Maven único com um **servidor** e um **cliente** [MCP](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro) (*Model Context Protocol*) escritos em Java com o [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk), sem Spring e sem Lombok. A comunicação entre cliente e servidor usa o transporte **STDIO**.

O servidor trabalha sobre uma **pasta raiz** (por padrão, `repositorio/`) e oferece ferramentas (*tools*) para que uma IA possa:

- **encontrar arquivos** por padrão *glob* (`*.pdf`, `**/*.txt`, ...);
- **contar as páginas** de arquivos PDF.

> Exemplo de uso com a IA: *"Quantas páginas tem cada PDF do repositório?"*. A IA chama `glob_search` para descobrir os PDFs e depois `pdf_page_count` para cada um.



---

## Sumário

1. [Ferramentas disponíveis](#ferramentas-disponíveis)
2. [A pasta raiz e a variável `GLOB_ROOT`](#a-pasta-raiz-e-a-variável-glob_root)
3. [Requisitos](#requisitos)
4. [Gerar as aplicações (build)](#gerar-as-aplicações-build)
5. [Executar com o cliente MCP Java](#executar-com-o-cliente-mcp-java)
6. [Usar no Claude Desktop](#usar-no-claude-desktop)
7. [Usar no MCP Inspector (Docker)](#usar-no-mcp-inspector-docker)
8. [Usar em outros assistentes de IA](#usar-em-outros-assistentes-de-ia)
9. [Testes](#testes)
10. [Arquitetura e boas práticas](#arquitetura-e-boas-práticas)
11. [Segurança](#segurança)
12. [Referências](#referências)

---

## Ferramentas disponíveis

O servidor registra três ferramentas. Todas devolvem texto. Quando algo dá errado, o servidor **não lança exceção ao cliente**: devolve um resultado com `isError = true` e uma mensagem começando com `Erro:`.

### `glob_search`

Procura arquivos dentro da pasta raiz usando um padrão *glob*.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `pattern` | string | sim | Padrão *glob* aplicado ao caminho **relativo** à raiz (ex.: `*.pdf`) |
| `maxResults` | integer | não (padrão `100`) | Quantidade máxima de resultados |

**Resultados possíveis**

| Situação | `isError` | Texto devolvido |
|---|---|---|
| Há arquivos que combinam | `false` | Um caminho relativo por linha, sempre com `/` (ex.: `manual-do-mcp.pdf`) |
| Nenhum arquivo combina | `false` | `Nenhum arquivo encontrado para o padrão: <padrão>` |
| Padrão ausente ou inválido, ou falha ao percorrer a pasta | `true` | `Erro: <mensagem>` |

**Padrões úteis**

| Padrão | O que encontra |
|---|---|
| `*.pdf` | PDFs diretamente na raiz |
| `**/*.pdf` | PDFs dentro de subpastas |
| `{*.pdf,**/*.pdf}` | PDFs em qualquer lugar (raiz e subpastas) |
| `**/*.txt` | Arquivos de texto em subpastas |

> ⚠️ **Atenção:** `**/*.pdf` **não** encontra um PDF que esteja diretamente na raiz. Para pegar a raiz e as subpastas, use `{*.pdf,**/*.pdf}`.

Somente **arquivos** são listados (pastas não). A ordem dos resultados não é garantida.

### `pdf_page_count`

Conta as páginas de um arquivo PDF da pasta raiz (usa o [Apache PDFBox](https://pdfbox.apache.org/)).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `path` | string | sim | Caminho do PDF **relativo** à raiz (ex.: `manual-do-mcp.pdf`) |

**Resultados possíveis**

| Situação | `isError` | Texto devolvido |
|---|---|---|
| PDF lido com sucesso | `false` | `manual-do-mcp.pdf: 3 página(s)` |
| Arquivo inexistente ou que não pode ser lido como PDF | `true` | `Erro: Não foi possível ler o PDF: <path>` |
| Caminho fora da pasta raiz (ex.: `../fora.pdf`) | `true` | `Erro: Caminho fora da raiz: <path>` |
| Parâmetro `path` ausente | `true` | `Erro: ...` |

### `obter_documento`

Ferramenta de **exemplo** que veio com o projeto base do professor, mantida para demonstrar notificações de *logging* ao cliente. Não lê arquivos.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | integer | sim | ID do documento |

**Resultado:** texto fixo no formato `id[123], titulo['Relatório de Documentos'], status['finalizado'], data[<data/hora atual>]`.

---

## A pasta raiz e a variável `GLOB_ROOT`

Todas as buscas acontecem **dentro** de uma pasta raiz. O servidor a define assim:

1. se existir a variável de ambiente `GLOB_ROOT`, usa o caminho dela;
2. caso contrário, usa a pasta relativa `repositorio` (relativa a **onde o servidor é executado**).

O projeto traz a pasta `repositorio/` com três PDFs de exemplo:

| Arquivo | Páginas |
|---|---|
| `guia-rapido-java.pdf` | 1 |
| `manual-do-mcp.pdf` | 3 |
| `relatorio-de-estudos.pdf` | 5 |

Em assistentes de IA (Claude Desktop e similares) o servidor não roda a partir da pasta do projeto. Por isso, **configure `GLOB_ROOT` com um caminho absoluto**.

---

## Requisitos

- **Java 25** (JDK) instalado e no `PATH`
- **Maven** (ou o Maven embutido do Eclipse IDE)
- (Opcional) **Docker Desktop**, para o MCP Inspector

Principais dependências (ver `pom.xml`): MCP Java SDK 2.0.1, Apache PDFBox 3.0.5, SLF4J + Logback, JUnit 5.

---

## Gerar as aplicações (build)

O projeto é gerado por **perfis Maven**:

| Perfil | Gera |
|---|---|
| `server` | `server/mcp_server01_server.jar` e `server/libs/` (as bibliotecas ficam ao lado do JAR) |
| `client` | `target/dist-client/mcp_server01_client.jar` e `target/dist-client/libs/` |

### Linha de comando

```
# servidor
mvn clean package -Pserver -DskipTests

# cliente
mvn clean package -Pclient -DskipTests
```

### Eclipse IDE

1. Botão direito no projeto > **Run As** > **Maven build...**
2. Aba **Main**, campo **Goals**: `clean package -Pserver -DskipTests` (ou `-Pclient`)
3. Aba **JRE**, campo **VM arguments**: `--enable-native-access=ALL-UNNAMED`
4. Clicar em **Run**

> 🗣️ **Importante:** o `clean` de um perfil apaga a saída do outro. Gerar o **servidor** apaga o **cliente** (em `target/`), então o cliente precisa ser gerado de novo. Feche o Claude Desktop antes de gerar o servidor: ele mantém o JAR aberto e o `clean` não consegue apagá-lo.

---

## Executar com o cliente MCP Java

O cliente inicia o servidor automaticamente (`java -jar`) e mostra um menu interativo.

1. Gerar o **servidor** e, em seguida, o **cliente** (nessa ordem, veja a observação acima).
2. Executar:

```
cd target/dist-client
java -jar mcp_server01_client.jar
```

Para usar outra pasta raiz, defina `GLOB_ROOT` antes de executar:

```
# Windows (Prompt de Comando)
set GLOB_ROOT=C:\caminho\da\pasta
java -jar mcp_server01_client.jar
```

O cliente localiza o JAR do servidor nesta ordem: propriedade `-Dmcp.server.jar=<caminho>`; `server/mcp_server01_server.jar`; `../../server/mcp_server01_server.jar` (caso da execução a partir de `target/dist-client`).

**Menu do cliente:** digite o número ou o nome da ferramenta, informe os parâmetros pedidos (os opcionais podem ficar em branco) e veja o resultado. Comandos: `listar` (atualiza a lista de ferramentas) e `sair`.

---

## Usar no Claude Desktop

1. Gerar o servidor (`mvn clean package -Pserver -DskipTests`).
2. No Claude Desktop, abrir **Settings > Developer > Edit Config** (o arquivo `claude_desktop_config.json` fica em `%APPDATA%\Claude\` ou na pasta de dados do aplicativo).
3. Adicionar a entrada `mcpServers`, **sem apagar** o restante do arquivo, com os caminhos **absolutos** da sua máquina:

```json
{
  "mcpServers": {
    "globbing": {
      "command": "java",
      "args": [
        "--enable-native-access=ALL-UNNAMED",
        "-jar",
        "C:\\caminho\\do\\projeto\\server\\mcp_server01_server.jar"
      ],
      "env": {
        "GLOB_ROOT": "C:\\caminho\\do\\projeto\\repositorio"
      }
    }
  }
}
```

4. Salvar e **encerrar completamente** o Claude Desktop (pelo ícone da bandeja, **Quit**; fechar só a janela não basta) e abri-lo novamente.
5. Em **Settings > Developer**, o servidor `globbing` deve aparecer como *running*. No chat, as ferramentas `glob_search`, `pdf_page_count` e `obter_documento` ficam disponíveis.

**Pedidos para testar**

- *"Liste todos os PDFs do repositório."*
- *"Quantas páginas tem o relatorio-de-estudos.pdf?"* (resposta: 5)
- *"Quantas páginas tem o arquivo ../fora.pdf?"* (erro: caminho fora da raiz)

Se o servidor aparecer com falha, confira se o `java` está no `PATH` (em caso negativo, use o caminho completo do `java.exe` em `command`) e leia o log em `%APPDATA%\Claude\logs\mcp-server-globbing.log`. Em barras de caminho no JSON, use `\\`.

---

## Usar no MCP Inspector (Docker)

O [MCP Inspector](https://modelcontextprotocol.io/docs/2026-07-28/tools/inspector) é a ferramenta da Anthropic para testar e depurar servidores MCP. Aqui ele roda em um contêiner Docker, definido pelos arquivos `Dockerfile` e `compose.yaml` da raiz do projeto.

O `compose.yaml` monta duas pastas no contêiner, ambas somente leitura:

```yaml
volumes:
  - ./server:/opt/mcp/server:ro
  - ./repositorio:/home/node/repositorio:ro
```

O contêiner executa o servidor a partir de `/home/node`, por isso a pasta `repositorio` montada ali é encontrada pelo padrão do servidor, sem precisar da variável `GLOB_ROOT`.

**Passos**

1. Gerar o servidor (`mvn clean package -Pserver -DskipTests`).
2. Garantir que o **Docker Desktop** esteja em execução.
3. Na raiz do projeto: `docker compose up -d --build`
4. Abrir `http://127.0.0.1:6274` no navegador.
5. Conectar ao servidor, abrir a aba **Tools** e executar `glob_search` e `pdf_page_count`.

Para encerrar: `docker compose down`.

---

## Usar em outros assistentes de IA

Qualquer cliente MCP que execute servidores locais por STDIO (Gemini CLI, Cursor, entre outros) precisa das mesmas três informações usadas no Claude Desktop:

| Informação | Valor |
|---|---|
| Comando | `java` |
| Argumentos | `--enable-native-access=ALL-UNNAMED`, `-jar`, `<caminho absoluto>/server/mcp_server01_server.jar` |
| Variável de ambiente | `GLOB_ROOT=<caminho absoluto da pasta raiz>` |

Esses clientes normalmente usam um arquivo de configuração com a mesma estrutura `mcpServers` mostrada na seção do Claude Desktop (consulte a documentação do assistente para saber o nome e a localização do arquivo).

### Instruções para a IA que usar este servidor

- Para descobrir quais arquivos existem, use `glob_search` **antes** de qualquer outra ferramenta.
- Para encontrar PDFs em qualquer lugar da raiz, use o padrão `{*.pdf,**/*.pdf}`. O padrão `**/*.pdf` **não** inclui a raiz.
- Os caminhos são sempre **relativos** à raiz. Não use caminhos absolutos nem `..`: serão recusados.
- Para contar as páginas de todos os PDFs: chame `glob_search` e depois `pdf_page_count` para cada caminho devolvido.
- Se uma ferramenta devolver `isError = true`, leia a mensagem (começa com `Erro:`), corrija o parâmetro e tente de novo.

---

## Testes

Os testes ficam em `src/test/java`: classes com sufixo `Test` são **unitárias**; com sufixo `IT` são de **integração**.

| Classe | Tipo | O que verifica |
|---|---|---|
| `SafePathTest` | unitário | resolução de caminhos dentro da raiz e recusa de `../`, caminhos absolutos e fugas da raiz |
| `GlobServiceTest` | unitário | busca por padrão *glob* em uma pasta temporária |
| `PdfServiceTest` | unitário | contagem de páginas de PDFs gerados em uma pasta temporária |
| `McpServer01IT` | integração | sobe o JAR do servidor, conecta um cliente MCP e executa as ferramentas: listagem, sucesso e erros de `glob_search` e `pdf_page_count`, além da `obter_documento` |

Os testes de integração usam o JAR de `server/mcp_server01_server.jar` e a pasta `repositorio/` do projeto.

**Executar tudo** (testes unitários e de integração, gerando o servidor):

```
mvn clean verify -Pserver
```

No Eclipse: **Run As > Maven build...**, Goals `clean verify -Pserver`. Também é possível executar as classes de teste individualmente com **Run As > JUnit Test** (para os `*IT`, gere o JAR do servidor antes).

---

## Arquitetura e boas práticas

```
br.edu.ifpr.mcp_server
├── main
│   ├── McpServer01SyncStdio     # servidor: configura o MCP e registra as ferramentas
│   └── McpClient01SyncStdio     # cliente interativo de console
└── mcp
    ├── utils
    │   └── McpToolDefinition    # interface de toda ferramenta (register + run)
    ├── tools
    │   ├── GlobSearchTool       # adaptador MCP → GlobService
    │   ├── PdfPageCountTool     # adaptador MCP → PdfService
    │   └── SimpleReportTool     # exemplo do professor (obter_documento)
    └── files
        ├── SafePath             # raiz segura (objeto de valor imutável)
        ├── PathOutsideRootException
        ├── GlobService          # regra de negócio: busca por glob
        └── PdfService           # regra de negócio: contagem de páginas
```

- **Separação de responsabilidades:** as *tools* só traduzem o protocolo MCP (ler argumentos, montar o resultado); a lógica fica nos *services*, que não conhecem o MCP e podem ser testados sozinhos.
- **Adapter:** `GlobSearchTool` e `PdfPageCountTool` adaptam o protocolo MCP aos serviços.
- **Strategy / polimorfismo (aberto para extensão):** toda ferramenta implementa `McpToolDefinition`. Para criar uma nova, basta implementar a interface e incluí-la na lista de `registerTools`, sem alterar o restante do servidor.
- **Injeção de dependência pelo construtor:** os serviços recebem o `SafePath` e as ferramentas recebem os serviços, sem *singletons* nem estado global.
- **Objeto de valor imutável:** `SafePath` guarda a raiz em um campo `final` e tem uma única responsabilidade: resolver caminhos com segurança.
- **Tratamento de erros:** falhas viram resultados `isError = true` com mensagem clara, em vez de derrubar o servidor.
- **Logs:** vão para `stderr`, porque a saída padrão é reservada às mensagens do protocolo MCP.

---

## Segurança

Como a IA informa caminhos e padrões, o servidor impede o acesso a arquivos fora da pasta raiz. O `SafePath` normaliza cada caminho recebido (resolvendo `..`) e confere se o resultado continua **dentro** da raiz. Caso contrário, lança `PathOutsideRootException`, que a ferramenta converte em erro:

| Entrada | Resultado |
|---|---|
| `manual-do-mcp.pdf` | aceito |
| `../fora.pdf` | recusado |
| `src/../../fora.pdf` | recusado |
| caminho absoluto (ex.: `C:\Windows\algo.pdf`) | recusado |

As buscas por *glob* percorrem apenas a pasta raiz.

---

## Referências

- [What is the Model Context Protocol (MCP)? (documentação oficial)](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [MCP Inspector](https://modelcontextprotocol.io/docs/2026-07-28/tools/inspector)
- [Apache PDFBox](https://pdfbox.apache.org/)
- [Documentação de `FileSystem.getPathMatcher` (sintaxe glob)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))