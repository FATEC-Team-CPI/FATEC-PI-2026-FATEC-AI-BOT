package org.acme.aibot;

import org.acme.aibot.dto.UploadDocRequest;
import org.acme.aibot.dto.UploadDocResponse;
import org.acme.aibot.service.IAIBotService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/aibot")
// @Produces(MediaType.APPLICATION_JSON)
// @Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Doc", description = "Gerenciamento de documentos para o IA Bot")
public class AIBotResource {
    // private static final Logger logger = LoggerFactory.getLogger(AIBotResource.class);
    
    @Inject
    IAIBotService service;

      
    /**
     * POST /aibot/doc-upload
        *Descrição: Endpoint para upload de documentos
        *Recebe um arquivo via multipart/form-data, valida o tipo do documento, faz upload para o localstack e salva os metadados no DynamoDB
     */
    @POST
    @Path("/doc-upload")
    @Operation(summary = "Upload arquivo", description = "Fazer upload de um arquivo para o localstack e salva metadados no DynamoDB")

    @APIResponse(responseCode = "201", description = "Arquivo enviado com sucesso")
    @APIResponse(responseCode = "400", description = "Arquivo inválido")

    public UploadDocResponse upload(UploadDocRequest request) {

        try {

            UploadDocResponse uploadStackResponse = service.uploadDocumento(request);
            if (!uploadStackResponse.sucesso()) {
                return new UploadDocResponse(
                    false,
                    "Falha ao enviar documento",
                    null
                );
            }

            return new UploadDocResponse(
                true,
                "Documento enviado com sucesso",
                null
            );

        } catch (Exception e) {
            return new UploadDocResponse(
                false,
                "Erro ao processar documento: " + e.getMessage(),
                null
            );
        }
    }

}