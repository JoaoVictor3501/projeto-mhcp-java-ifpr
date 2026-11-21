package br.edu.ifpr.mcp_server.mcp.files;

import java.nio.file.Path;

/**
 * Garante que qualquer caminho informado pelo cliente (a IA) fique DENTRO da
 * pasta raiz autorizada, impedindo ataques de "path traversal" (tentativas de
 * sair da raiz usando "..", por exemplo).
 *
 * É um objeto imutável: a raiz é definida uma única vez, no construtor, e
 * nunca mais muda. Por isso pode ser compartilhado com segurança entre os
 * serviços do servidor.
 */
public class SafePath {
   // pasta raiz autorizada, sempre em caminho absoluto e normalizado.
   // 'final' = o valor é atribuído uma única vez (no construtor)
   private final Path root;

   /**
    * @param root pasta raiz autorizada (pode ser relativa ou absoluta)
    */
   public SafePath(Path root) {
      // toAbsolutePath(): transforma um caminho relativo em absoluto, usando
      // como base a pasta onde o programa foi executado
      // normalize(): elimina os trechos "." (pasta atual) e ".." (pasta pai),
      // por exemplo, "projeto/../projeto" vira "projeto"
      this.root = root.toAbsolutePath().normalize();
   }

   /**
    * Converte um texto, informado pelo cliente, em um caminho absoluto
    * garantidamente dentro da raiz.
    *
    * @param texto caminho relativo à raiz (ex.: "docs/manual.pdf")
    * @return caminho absoluto dentro da raiz
    * @throws PathOutsideRootException se o caminho final ficar fora da raiz
    */
   public Path resolve(String texto) {
      // junta a raiz ao texto recebido. Atenção: se o texto for um caminho
      // ABSOLUTO, o resolve devolve o próprio texto e ignora a raiz
      Path path = root.resolve(texto)
                      // elimina "." e "..": "raiz/../fora.txt" vira
                      // ".../fora.txt", que está fora da raiz
                      .normalize();
      // startsWith compara pasta por pasta (e não letra por letra). Se o
      // caminho final não começa pela raiz, o cliente tentou escapar dela
      if (!path.startsWith(root))
         // recusa o acesso; a ferramenta converterá esta exceção em um erro
         // MCP (isError = true) para o cliente
         throw new PathOutsideRootException("Caminho fora da raiz: " + texto);
      // chegou aqui: o caminho está dentro da raiz e pode ser usado
      return path;
   }

   /**
    * @return a pasta raiz autorizada (absoluta e normalizada)
    */
   public Path getRoot() {
      return root;
   }
}