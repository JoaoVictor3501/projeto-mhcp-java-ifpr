package br.edu.ifpr.mcp_server.mcp.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfService {
	public final SafePath safePath;
	
	public PdfService(SafePath safePath) {
		this.safePath = safePath;
	}

	public Object countPages(String caminho) {
		Path arquivo = safePath.resolve(caminho);
		try(PDDocument doc = Loader.loadPDF(arquivo.toFile())){
			return doc.getNumberOfPages();
		}catch(IOException error) {
			throw new UncheckedIOException("Não foi possível ler o PDF: " + caminho, error);
		}
		
	}
	
}
