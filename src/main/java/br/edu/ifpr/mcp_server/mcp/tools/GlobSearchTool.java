package br.edu.ifpr.mcp_server.mcp.tools;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.mcp.files.GlobService;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Ferramenta MCP "glob_search": pesquisa arquivos na pasta raiz por padrão
 * glob (por exemplo, "*.pdf").
 *
 * Esta classe é um ADAPTADOR: ela "traduz" o protocolo MCP (argumentos JSON
 * vindos do cliente e resultado devolvido a ele) para chamadas ao
 * {@link GlobService}, que contém a lógica da busca e não conhece o MCP.
 *
 * Implementa {@link McpToolDefinition}, a interface comum a todas as
 * ferramentas do servidor: o servidor só precisa chamar register() em cada
 * uma, sem saber o que cada ferramenta faz (polimorfismo).
 */
public class GlobSearchTool implements McpToolDefinition {
   // serviço que realiza a busca; recebido pelo construtor (injeção de
   // dependência), o que permite trocá-lo, por exemplo, em testes
   private final GlobService globService;

   /**
    * @param globService serviço de busca já configurado com a pasta raiz
    */
   public GlobSearchTool(GlobService globService) {
      this.globService = globService;
   }

   /**
    * Descreve a ferramenta ao servidor MCP: nome, título, descrição e o
    * esquema (JSON Schema) dos parâmetros aceitos. É esta descrição que o
    * cliente (por exemplo, o Claude) lê para saber como usar a ferramenta.
    */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      // esquema JSON dos parâmetros de entrada:
      // - "properties": os parâmetros aceitos, com tipo e descrição;
      // - "required": os parâmetros obrigatórios (aqui, somente "pattern");
      // - "additionalProperties": false recusa parâmetros não declarados
      Map<String, Object> inputSchema =
         Map.of("type", "object",
                "properties", Map.of(
                   "pattern", Map.of("type", "string",
                                     "description",
                                     "Padrao glob, por exemplo *.pdf"),
                   "maxResults", Map.of("type", "integer",
                                        "description",
                                        "Quantidade máxima de resultados " +
                                        "(padrão: 100)")),
                "required", List.of("pattern"),
                "additionalProperties", false);
      // define a ferramenta: o nome ("glob_search") é o identificador usado
      // pelo cliente para chamá-la
      Tool tool = Tool.builder("glob_search", inputSchema)
                      .title("Pesquisar arquivos")
                      .description("Pesquisar arquivos por padrão glob " +
                                   "dentro da pasta raiz")
                      .build();
      // vincula o objeto ferramenta (acima) à implementação da ferramenta
      SyncToolSpecification toolSpec =
                            SyncToolSpecification.builder()
                                                 .tool(tool)
                                                 // quando o cliente chamar a
                                                 // ferramenta, o método "run"
                                                 // (abaixo) será invocado
                                                 .callHandler(this::run)
                                                 .build();
      // acrescenta a ferramenta ao servidor
      mcp.tools(toolSpec);
   }

   /**
    * Executa a busca quando o cliente chama a ferramenta.
    *
    * Qualquer falha é convertida em um resultado com isError = true, em vez de
    * propagar a exceção, para que o cliente (a IA) receba uma mensagem clara e
    * possa corrigir o pedido.
    *
    * @param exchange canal de comunicação com o cliente (não usado aqui)
    * @param request  pedido do cliente, com os argumentos em JSON
    * @return texto com os caminhos encontrados, uma mensagem de "nada
    *         encontrado" ou uma mensagem de erro
    */
   @Override
   public CallToolResult run(McpSyncServerExchange exchange,
                             CallToolRequest request) {
      try {
         // lê o parâmetro obrigatório "pattern" (o JSON chega como Map)
         String pattern = (String) request.arguments().get("pattern");
         // limite padrão de resultados, usado quando o cliente não informa
         int    max     = 100;
         // lê o parâmetro opcional "maxResults": se ausente, vem null
         Object maxArg  = request.arguments().get("maxResults");
         // números vindos de JSON podem ser Integer, Long ou Double, por isso
         // o cast para Number antes de converter para int
         if (maxArg != null)
            max = ((Number) maxArg).intValue();
         // delega a busca ao serviço: devolve caminhos relativos à raiz
         List<Path> achados = globService.search(pattern, max);
         // prepara o texto de resposta
         String texto = achados.isEmpty()
                 // nada encontrado não é erro: é uma resposta válida
                 ? "Nenhum arquivo encontrado para o padrão: " + pattern
                 // um caminho por linha; replace troca "\" (Windows) por "/",
                 // para o resultado ser igual em qualquer sistema
                 : achados.stream()
                          .map(p -> p.toString().replace("\\", "/"))
                          .collect(Collectors.joining("\n"));
         // monta o resultado de sucesso (isError fica falso por padrão)
         return CallToolResult.builder()
                              .content(List.of(TextContent.builder(texto)
                                                          .build()))
                              .build();
      }
      catch (Exception ex) {
         // padrão inválido, falha ao ler a pasta etc.: devolve ao cliente um
         // resultado marcado como erro, com a mensagem da exceção
         return CallToolResult.builder()
                              .content(List.of(TextContent.builder(
                                          "Erro: " + ex.getMessage()).build()))
                              .isError(true)
                              .build();
      }
   }
}
