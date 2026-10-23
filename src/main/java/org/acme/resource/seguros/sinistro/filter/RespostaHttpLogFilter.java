package org.acme.resource.seguros.sinistro.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RespostaHttpLogFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        System.out.println("<<< RESPOSTA HTTP >>> " + req.getMethod() + " "
                + req.getUriInfo().getPath() + " -> status=" + res.getStatus());
    }
}