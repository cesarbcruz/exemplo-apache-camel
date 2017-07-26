package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultCamelContext;


import com.thoughtworks.xstream.XStream;

public class RotaDesafioParte1HTTPPolling {

	public static void main(String[] args) throws Exception {
		RotaDesafioParte1HTTPPolling desafio = new RotaDesafioParte1HTTPPolling();
		//desafio.configurarRotaXMLHttp4ParaArquivoXMLGravadoEmDisco();
		desafio.configurarRotaTransformarXMLEmObjetoJava();
    }
	
	/**
	 * Exemplo de Rota com Timer que obtem o XML através do http4 e grava o arquivo XML em disco 
	 * 
	 * @throws Exception
	 */
	private void configurarRotaXMLHttp4ParaArquivoXMLGravadoEmDisco() throws Exception {
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
	
	private void configurarRotaTransformarXMLEmObjetoJava() throws Exception {
		
		final XStream xstream = new XStream();
		xstream.alias("negociacao", Negociacao.class);
		
		CamelContext context = new DefaultCamelContext();
	        context.addRoutes(new RouteBuilder() {

	        	@Override
	        	public void configure() throws Exception {
	        	    from("timer://negociacoes?fixedRate=true&delay=3s&period=360s")
	        	      .to("http4://argentumws.caelum.com.br/negociacoes")
	        	      .convertBodyTo(String.class)
	        	      .unmarshal(new XStreamDataFormat(xstream))
	        	      .split(body())
	        	      .log("${body}")
	        	    .end(); //só deixa explícito que é o fim da rota
	        	}
	        });

	        context.start();
	        
	        Thread.sleep(2000);
	}
}