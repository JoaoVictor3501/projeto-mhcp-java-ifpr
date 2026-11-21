package br.edu.ifpr.mcp_server.mcp.tools;

import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.mcp.files.PdfService;
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
 * Ferramenta MCP "pdf_page_count": conta as páginas de um arquivo PDF que
 * esteja dentro da pasta raiz.
 *
 * Esta classe é um ADAPTADOR: ela "traduz" o protocolo MCP (argumentos JSON
 * vindos do cliente e resultado devolvido a ele) para chamadas ao
 * {@link PdfService}, que contém a lógica da contagem e não conhece o MCP.
 *
 * Implementa {@link McpToolDefinition}, a interface comum a todas as
 * ferramentas do servidor (polimorfismo): o servidor só chama register() em
 * cada uma, sem saber o que cada ferramenta faz.
 */
public class PdfPageCountTool implements McpToolDefinition {
   // serviço que conta as páginas; recebido pelo construtor (injeção de
   // dependência), o que permite trocá-lo, por exemplo, em testes
   private final PdfService pdfService;

   /**
    * @param pdfService serviço de contagem já configurado com a pasta raiz
    */
   public PdfPageCountTool(PdfService pdfService) {
      this.pdfService = pdfService;
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
      // - "properties": o parâmetro "path" (texto) com sua descrição;
      // - "required": "path" é obrigatório;
      // - "additionalProperties": false recusa parâmetros não declarados
      Map<String, Object> inputSchema =
         Map.of("type", "object",
                "properties", Map.of(
                   "path", Map.of("type", "string",
                                  "description",
                                  "Caminho do PDF dentro da raiz, por " +
                                  "exemplo manual-do-mcp.pdf")),
                "required", List.of("path"),
                "additionalProperties", false);
      // define a ferramenta: o nome ("pdf_page_count") é o identificador
      // usado pelo cliente para chamá-la
      Tool tool = Tool.builder("pdf_page_count", inputSchema)
                      .title("Contador de Páginas")
                      .description("Conta as páginas de um arquivo PDF " +
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
    * Executa a contagem quando o cliente chama a ferramenta.
    *
    * Qualquer falha (arquivo inexistente, caminho fora da raiz, PDF inválido)
    * é convertida em um resultado com isError = true, em vez de propagar a
    * exceção, para que o cliente (a IA) receba uma mensagem clara.
    *
    * @param exchange canal de comunicação com o cliente (não usado aqui)
    * @param request  pedido do cliente, com os argumentos em JSON
    * @return texto "arquivo: N página(s)" ou uma mensagem de erro
    */
   @Override
   public CallToolResult run(McpSyncServerExchange exchange,
                             CallToolRequest request) {
      try {
         // lê o parâmetro obrigatório "path" (o JSON chega como Map)
         String path    = (String) request.arguments().get("path");
         // delega a contagem ao serviço, que valida o caminho (SafePath) e
         // abre o PDF; qualquer problema vira exceção e cai no catch abaixo
         int    paginas = pdfService.countPages(path);
         // texto de resposta, por exemplo: "manual-do-mcp.pdf: 3 página(s)"
         String texto   = path + ": " + paginas + " página(s)";
         // monta o resultado de sucesso (isError fica falso por padrão)
         return CallToolResult.builder()
                              .content(List.of(TextContent.builder(texto)
                                                          .build()))
                              .build();
      }
      catch (Exception ex) {
         // arquivo inexistente, caminho fora da raiz etc.: devolve ao cliente
         // um resultado marcado como erro, com a mensagem da exceção
         return CallToolResult.builder()
                              .content(List.of(TextContent.builder(
                                          "Erro: " + ex.getMessage()).build()))
                              .isError(true)
                              .build();
      }
   }
}
