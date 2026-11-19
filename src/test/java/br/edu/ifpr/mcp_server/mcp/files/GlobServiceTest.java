package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
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
	
	@Test
	void pdfNaRaizSoApareceComPadraoSemPasta(@TempDir Path raiz) throws IOException {
	   Files.createFile(raiz.resolve("a.pdf"));
	   GlobService service = new GlobService(new SafePath(raiz));

	   // o padrão sem pasta ACHA o PDF solto na raiz
	   assertEquals(List.of(Path.of("a.pdf")), service.search("*.pdf", 10));
	   // o padrão com "**/" NÃO acha
	   assertEquals(List.of(), service.search("**/*.pdf", 10));
	}
	
	@Test
	void arquivoInexistenteLancaErro(@TempDir Path raiz) {
	   PdfService service = new PdfService(new SafePath(raiz));

	   assertThrows(UncheckedIOException.class,
	                () -> service.countPages("nao-existe.pdf"));
	}

	@Test
	void caminhoForaDaRaizLancaErro(@TempDir Path raiz) {
	   PdfService service = new PdfService(new SafePath(raiz));

	   assertThrows(PathOutsideRootException.class,
	                () -> service.countPages("../fora.pdf"));
	}
}
