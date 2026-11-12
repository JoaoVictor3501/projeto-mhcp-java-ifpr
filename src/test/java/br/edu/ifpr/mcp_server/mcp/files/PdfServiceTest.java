package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PdfServiceTest {

	private void criarPdf(Path arquivo, int paginas) {
		try (PDDocument doc = new PDDocument()){
			for (int i = 0; i < paginas; i++) {
				doc.addPage(new PDPage());
			doc.save(arquivo.toFile());
			}
		}catch (IOException error){
			String caminho = null;
			throw new UncheckedIOException("Não foi possível ler o PDF: " + caminho, error);
		}
	}
	
	@Test
	void contarPaginasDeUmPdf(@TempDir Path pasta) {
		criarPdf(pasta.resolve("tes.pdf"), 3);
		
		PdfService service = new PdfService(new SafePath(pasta));
		
		assertEquals(3, service.countPages("tres.pdf"));
	}
}
