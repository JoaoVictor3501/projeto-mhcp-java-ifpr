package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
	
	@Test
	void rejeitarCaminhoForaDaRaiz() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath = new SafePath(root);
		assertThrows(PathOutsideRootException.class, 
				() -> safePath.resolve("../fora.txt"));
	}
	
	@Test
	void rejeitarPontoPontoNoMeioDoCaminho() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath = new SafePath(root);
		assertThrows(PathOutsideRootException.class,
				() -> safePath.resolve("src/../../fora.txt"));
	}
	
	@Test
	void caminhoAbsoluto() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath= new SafePath(root);
		String absoluto =  Path.of("outro").toAbsolutePath().toString();
		assertThrows(PathOutsideRootException.class, 
				() -> safePath.resolve(absoluto));
		
	}
	
	@Test 
	void aceitarRaizRelativaComPontoPonto() {
		SafePath safePath = new SafePath(Path.of("projeto/../projeto"));
		Path resultado = safePath.resolve("src/Main.java");
		assertTrue(resultado.startsWith(Path.of("Projeto").toAbsolutePath()));
		
	}
	
	
}
