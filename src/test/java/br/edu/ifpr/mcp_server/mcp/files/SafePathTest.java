package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class SafePathTest {
	
	@Test
	void aceitarCaminhoDentroDaRaiz() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		
		SafePath safePath = new SafePath(root);
		
		Path resultado = safePath.resolve("src/Main.java");
		assertTrue(resultado.startsWith(root));
	}
	
}
