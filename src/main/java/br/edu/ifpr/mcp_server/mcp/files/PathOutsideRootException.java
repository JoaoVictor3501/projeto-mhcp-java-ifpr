package br.edu.ifpr.mcp_server.mcp.files;


public class PathOutsideRootException extends RuntimeException {
	public PathOutsideRootException(String mensagem) {
		super(mensagem);
	}

}
