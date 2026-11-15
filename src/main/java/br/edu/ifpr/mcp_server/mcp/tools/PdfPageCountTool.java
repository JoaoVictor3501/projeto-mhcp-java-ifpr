package br.edu.ifpr.mcp_server.mcp.tools;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.mcp.files.PdfService;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PdfPageCountTool implements McpToolDefinition {

	private final PdfService pdfService;
	
	public PdfPageCountTool (PdfService pdfService) {
		this.pdfService = pdfService;
	}
	
	@Override
	public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
				throws Exception{
		Map<String, Object> inputSchema =
				Map.of("type", "object",
						"properties", Map.of(
								"path", Map.of("type", "string",
										"description", "Caminho do PDF dentro da raiz, por exemplo manual-do-mcp.pdf")),
								"required", List.of("path"),
								"additionalProperties", false);
		 Tool tool = Tool.builder("pdf_page_count",inputSchema)
			      .title("Contador de Páginas")
			      .description("Contar páginas dentro de um pdf")
			      .build();
		 // vincula o objeto ferramenta (acima) à implementação da ferramenta
		 SyncToolSpecification toolSpec =
		                       SyncToolSpecification.builder()
		                                            .tool(tool)
		                                            // quando a ferramenta for
		                                            // selecionada, seu método
		                                            // "run" (abaixo) será
		                                            // invocado para realizar a
		                                            // tarefa programada
		                                            .callHandler(this::run)
		                                            .build();
		 // acrescenta a ferramenta ao servidor
		 mcp.tools(toolSpec);
	}
	@Override
	 public CallToolResult run(McpSyncServerExchange exchange,
			 CallToolRequest request) {
		 try {
			 String path = (String) request.arguments().get("path");
			 int paginas = pdfService.countPages(path);
			 String texto = path + ": " + paginas + "página(s)";
			return CallToolResult.builder()
					.content(List.of(TextContent.builder(texto).build()))
					.build();
		 }catch (Exception ex){
			 return CallToolResult.builder()
					 .content(List.of(TextContent.builder(
							 "Erro: " + ex.getMessage()).build()))
					 .isError(true)
					 .build();
		 }
	 }
	
}
