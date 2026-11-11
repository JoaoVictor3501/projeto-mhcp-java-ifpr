package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


public class GlobServiceTest {
	
	@Test
	void acharPdfs(@TempDir Path pasta) throws IOException{
		Files.createFile(pasta.resolve("bob_o_bobo.txt"));
		Files.createDirectory(pasta.resolve("desenho"));
		Files.createFile(pasta.resolve("desenho/pequeno_principe.txt"));
		Files.createFile(pasta.resolve("desenho/vinlandsaga.pdf"));
		
		GlobService service = new GlobService(new SafePath(pasta));
		List<Path> resultado = service.search("**/*.pdf", 100);
		
		assertEquals(List.of(Path.of("desenho/vinlandsaga.pdf")), resultado);
	}
}
