package br.edu.ifpr.mcp_server.mcp.files;
import java.nio.file.Path;

public class SafePath {

	private final Path root;

	public SafePath(Path root) {
		this.root = root.toAbsolutePath().normalize();
	}

	public Path resolve(String texto){
		Path path = root.resolve(texto).normalize();
		if(!path.startsWith(root)) {
			throw new PathOutsideRootException ("Caminho fora da raiz: " + texto);
		}
		
		return path;
	}

	public Path getRoot() {
		// TODO Auto-generated method stub
		return root;
	}

}
