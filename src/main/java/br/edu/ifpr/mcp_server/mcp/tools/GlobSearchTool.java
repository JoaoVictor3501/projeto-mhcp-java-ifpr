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
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class GlobSearchTool implements McpToolDefinition {
	private final GlobService globService;
	
	public GlobSearchTool (GlobService globService) {
		this.globService = globService;
		
	}
	@Override
	public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
				throws Exception{
		Map<String, Object> inputSchema =
				Map.of("type", "object",
						"properties", Map.of(
								"pattern", Map.of("type", "string",
										"description", "Padrao glob, por exemplo *.pdf"),
								"maxResults", Map.of("type", "integer","description", "máximo de resultados - 100")),
								"required", List.of("pattern"), "additionalProperties", false);
		 Tool tool = Tool.builder("glob_search",inputSchema)
			      .title("Pesquisar arquivos")
			      .description("Pesquisar arquivos por padrão glob dentro da pasta raiz")
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
			 String pattern = (String) request.arguments().get("pattern");
			 int max = 100;
			 Object maxArg = request.arguments().get("maxResults");
			 if (maxArg != null)
				 max = ((Number) maxArg).intValue();
			 List<Path> achados = globService.search(pattern, max);
			 String texto = achados.isEmpty()
					 ? "Nenhum arquivo Encontrado para o padrão!" + pattern
							 :achados.stream()
							 .map(p -> p.toString().replace("\\", "/"))
							 .collect(Collectors.joining("\n"));
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
