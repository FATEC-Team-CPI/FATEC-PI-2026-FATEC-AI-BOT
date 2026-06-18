package org.acme.aibot.dto;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UploadDocRequest", description = "Requisição para upload de documento")
public class UploadDocRequest {

    @FormParam("documento")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public FileUpload document;
}