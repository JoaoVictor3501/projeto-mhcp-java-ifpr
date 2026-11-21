package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Testes Unitários do {@link SafePath
  * O SafePath é a "porta de segurança" do servidor: toda vez que a IA informa
 * um caminho de arquivo, ele é convertido por esta classe. Se o caminho tentar
 * sair da pasta raiz autorizada (ataque conhecido como "path traversal"), uma
 * {@link PathOutsideRootException} é lançada.
 *
 * Métodos de java.nio.file.Path usados nestes testes:
 * - Path.of(texto): cria um objeto Path a partir de um texto;
 * - toAbsolutePath(): transforma um caminho relativo em absoluto, usando como
 *   base a pasta de execução (ex.: "projeto" vira "C:\...\projeto");
 * - normalize(): elimina os trechos "." (pasta atual) e ".." (pasta pai);
 *   por exemplo, "a/b/../c" vira "a/c";
 * - resolve(texto): "junta" o texto ao caminho atual; se o texto for um
 *   caminho absoluto, ele substitui o caminho atual;
 * - startsWith(outro): verifica se o caminho começa por outro caminho,
 *   comparando pasta por pasta (e não letra por letra).
 *
 * A pasta "projeto" usada aqui NÃO precisa existir: o SafePath apenas
 * manipula caminhos, sem acessar o sistema de arquivos.
 */

public class SafePathTest {
	
	/**
	    * Caminho aceito.
	    *
	    * Cenário:   a raiz é ".../projeto" e o arquivo pedido é "src/Main.java".
	    * Ação:      resolve("src/Main.java") junta a raiz ao texto, resultando em
	    *            ".../projeto/src/Main.java".
	    * Esperado:  o resultado é aceito e continua começando pela raiz.
	    */
	@Test
	void aceitarCaminhoDentroDaRaiz() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath = new SafePath(root);
		Path resultado = safePath.resolve("src/Main.java");
		assertTrue(resultado.startsWith(root));
	}
	
	/**
	    * Rejeitar de fora da raiz.
	    *
	    * Cenário:   a IA pede "../fora.txt", ou seja, um arquivo na pasta PAI da
	    *            raiz.
	    * Ação:      resolve junta raiz + texto e normalize() resolve o "..",
	    *            resultando em ".../fora.txt", que NÃO começa pela raiz.
	    * Esperado:  assertThrows confirma que a exceção PathOutsideRootException
	    *            foi lançada. O trecho "() -> ..." é uma expressão lambda: o
	    *            código só é executado dentro do assertThrows, que captura a
	    *            exceção e verifica o tipo.
	    */
	
	@Test 
	void rejeitarCaminhoForaDaRaiz() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath = new SafePath(root);
		assertThrows(PathOutsideRootException.class, 
				() -> safePath.resolve("../fora.txt"));
	}
	
	/**
	    * O ".." escondido no meio do caminho também precisa ser barrado.
	    *
	    * Cenário:   "src/../../fora.txt" parece começar em "src", mas o primeiro
	    *            ".." volta para a raiz e o segundo sai dela.
	    * Ação:      o normalize() calcula o caminho final, e o startsWith revela
	    *            que ele está fora da raiz.
	    * Esperado:  PathOutsideRootException. Isto prova que a verificação é feita
	    *            sobre o caminho FINAL, e não sobre o texto digitado.
	    */	
	@Test
	void rejeitarPontoPontoNoMeioDoCaminho() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath = new SafePath(root);
		assertThrows(PathOutsideRootException.class,
				() -> safePath.resolve("src/../../fora.txt"));
	}
	
	/**
	    * Caminho absoluto informado pelo cliente.
	    *
	    * Cenário:   a IA tenta "pular" a raiz informando um caminho completo, por
	    *            exemplo "C:\...\outro".
	    * Ação:      Path.resolve tem uma regra: se o texto recebido já é um caminho
	    *            ABSOLUTO, ele é devolvido no lugar da raiz. Assim, o resultado
	    *            não começa pela raiz.
	    * Esperado:  PathOutsideRootException.
	    */
	@Test 
	void caminhoAbsoluto() {
		Path root = Path.of("projeto").toAbsolutePath().normalize();
		SafePath safePath= new SafePath(root);
		String absoluto =  Path.of("outro").toAbsolutePath().toString();
		assertThrows(PathOutsideRootException.class, 
				() -> safePath.resolve(absoluto));
		
	}
	/**
	    * A própria raiz também é normalizada pelo construtor.
	    *
	    * Cenário:   a raiz recebida é "projeto/../projeto" (que, resolvida,
	    *            equivale simplesmente a "projeto").
	    * Ação:      o construtor aplica toAbsolutePath() e normalize(), então a
	    *            raiz guardada é ".../projeto".
	    * Esperado:  os arquivos continuam sendo aceitos e o resultado começa pela
	    *            raiz normalizada. O nome é escrito em minúsculas porque em
	    *            Linux e macOS as pastas diferenciam maiúsculas de minúsculas.
	    */
	@Test 
	void aceitarRaizRelativaComPontoPonto() {
		SafePath safePath = new SafePath(Path.of("projeto/../projeto"));
		Path resultado = safePath.resolve("src/Main.java");
		assertTrue(resultado.startsWith(Path.of("projeto").toAbsolutePath()));
		
	}
	
	
}
