package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaDesafioParte1HTTPPolling {

	public static void main(String[] args) throws Exception {
		RotaDesafioParte1HTTPPolling rotaPedidos = new RotaDesafioParte1HTTPPolling();
		rotaPedidos.configurarRotaArquivoXMLParaArquivoJSON();
    }
	
	/**
	 * Exemplo de Rota com Timer que obtem o XML através do http4 e grava o arquivo XML em disco 
	 * 
	 * @throws Exception
	 */
	private void configurarRotaArquivoXMLParaArquivoJSON() throws Exception {
		 CamelContext context = new DefaultCamelContext();
	        context.addRoutes(new RouteBuilder() {

	            @Override
	            public void configure() throws Exception {
	            	from("timer://negociacoes?fixedRate=true&delay=1s&period=360s").
	                to("http4://argentumws.caelum.com.br/negociacoes").
	                    convertBodyTo(String.class).
	                    log("${body}").
	                    setHeader(Exchange.FILE_NAME, constant("negociacoes.xml")).
	                to("file:saida");
	            }
	        });

	        context.start();
	        
	        Thread.sleep(2000);
	}
}