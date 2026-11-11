package br.edu.ifpr.mcp_server.mcp.files;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.UncheckedIOException;

public class GlobService {
	private final SafePath safePath;
	
	public GlobService(SafePath safePath) {
		this.safePath = safePath;
	}

	public List<Path> search(String padrao, int maxResultados) {
		Path raiz = safePath.getRoot();
		PathMatcher matcher = FileSystems.getDefault()
								  .getPathMatcher("glob:" + padrao);
		
		try(Stream<Path> arquivos = Files.walk(raiz)){
			return arquivos
				.filter(Files::isRegularFile)
				.map(raiz::relativize)
				.filter(matcher::matches)
				.limit(maxResultados)
				.toList();
			
		}catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	
	} 

}
