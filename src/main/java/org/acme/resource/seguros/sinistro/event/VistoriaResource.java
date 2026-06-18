package org.acme.resource.seguros.sinistro.event;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@Path("/api/v1/vistoria")
@Authenticated
public class VistoriaResource {

    @POST
    @Path("/{id}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RunOnVirtualThread // O grande segredo: descarrega o I/O bloqueante do disco nas Virtual Threads do Java 21
    public Response uploadFoto(@RestPath String id, @RestForm("file") FileUpload file) {

        if (file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Arquivo de imagem não enviado ou chave incorreta.")
                    .build();
        }

        try {
            // Simulando o processamento/gravação do arquivo em disco
            // Com @RunOnVirtualThread, essa cópia de arquivos pesados não gela o Event Loop do Netty
            java.nio.file.Path pastaDestino = Files.createTempDirectory("vistorias-arquivos_");
            java.nio.file.Path arquivoFinal = pastaDestino.resolve("vistoria_" + id + "_" + file.fileName());

            // Move o arquivo temporário do Quarkus para o destino definitivo
            Files.copy(file.uploadedFile(), arquivoFinal);

            System.out.println("====== [VIRTUAL THREAD: " + Thread.currentThread() + "] ======");
            System.out.println("Foto salva com sucesso em: " + arquivoFinal.toAbsolutePath());

            return Response.ok("Upload da vistoria " + id + " processado com sucesso!").build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Falha ao salvar a imagem no servidor: " + e.getMessage())
                    .build();
        }
    }
}