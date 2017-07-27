package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidos {

	public static void main(String[] args) throws Exception {
		RotaPedidos rotaPedidos = new RotaPedidos();
		//rotaPedidos.configurarRotaArquivoXMLParaArquivoJSON();
		//rotaPedidos.configurarRotaArquivoXMLParaWebServiceHTTPPost();
		//rotaPedidos.configurarRotaArquivoXMLParaWebServiceHTTPGET();
		//rotaPedidos.configurarSubRotas();
		//rotaPedidos.configurarSubRotasExecutadasEmParalelo();
		rotaPedidos.configurarSubRotasComSeda();
    }
	
	/**
	 * Exemplo de Rota Camel File Shared Directory
	 * File XML -> File JSON
	 * @throws Exception
	 */
	private void configurarRotaArquivoXMLParaArquivoJSON() throws Exception {
		 CamelContext context = new DefaultCamelContext();
	        context.addRoutes(new RouteBuilder() {

	            @Override
	            public void configure() throws Exception {
	                from("file:pedidos?delay=5s&noop=true").
		                split().
		                	xpath("/pedido/itens/item").
		                filter().
		                	xpath("/item/formato[text()='EBOOK']").
		                log("${exchange.pattern}"). 
		                log("${id} - ${body}").
		                marshal().xmljson().
		                log("${body}").
		                setHeader(Exchange.FILE_NAME, simple("${file:name.noext}-${header.CamelSplitIndex}.json")).
	                to("file:saida");
	            }
	        });

	        context.start();
	        
	        Thread.sleep(2000);
	}
	
	/**
	 * Exemplo de Rota Camel File Shared Directory
	 * File XML -> Web Service HTTP (Body JSON)
	 * HTTP_METHOD POST
	 * @throws Exception
	 */
	private void configurarRotaArquivoXMLParaWebServiceHTTPPost() throws Exception {

	        CamelContext context = new DefaultCamelContext();
	        context.addRoutes(new RouteBuilder() {

	            @Override
	            public void configure() throws Exception {
	                from("file:pedidos?delay=5s&noop=true").
		                split().
		                	xpath("/pedido/itens/item").
		                filter().
		                	xpath("/item/formato[text()='EBOOK']").
		                log("${exchange.pattern}"). 
		                log("${id} - ${body}").
		                marshal().xmljson().
		                log("${body}").
		                setHeader(Exchange.HTTP_METHOD, HttpMethods.POST).
	                to("http4://localhost:8080/webservices/ebook/item");
	            }
	        });

	        context.start();
	        
	        Thread.sleep(2000);
	    
	}
	
	/**
	 * Exemplo de Rota Camel File Shared Directory
	 * File XML -> Web Service HTTP (Body JSON)
	 * HTTP_METHOD GET
	 * Header and Property
	 * @throws Exception
	 */
	private void configurarRotaArquivoXMLParaWebServiceHTTPGET() throws Exception {

	        CamelContext context = new DefaultCamelContext();
	        context.addRoutes(new RouteBuilder() {

	            @Override
	            public void configure() throws Exception {
	                from("file:pedidos?delay=5s&noop=true").
	                	setProperty("pedidoId", xpath("/pedido/id/text()")).
	                	setProperty("clienteId", xpath("/pedido/pagamento/email-titular/text()")).
		                split().
		                	xpath("/pedido/itens/item").
		                filter().
		                	xpath("/item/formato[text()='EBOOK']").
		                setProperty("ebookId", xpath("/item/livro/codigo/text()")).
		                marshal().xmljson().
		                setHeader(Exchange.HTTP_METHOD, HttpMethods.GET).
		                setHeader(Exchange.HTTP_QUERY, simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.clienteId}")).
	                to("http4://localhost:8080/webservices/ebook/item");
	            }
	        });

	        context.start();
	        
	        Thread.sleep(2000);
	    
	}
	
	private void configurarSubRotas() throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("file:pedidos?delay=1s&noop=true").
			    routeId("rota-pedidos").
			    multicast().
			        to("direct:soap").
			        to("direct:http");

				from("direct:soap").
				    routeId("rota-soap").
				    log("chamando servico soap ${body}").
				to("mock:soap");
	
				from("direct:http").
				    routeId("rota-http").
				    setProperty("pedidoId", xpath("/pedido/id/text()")).
				    setProperty("email", xpath("/pedido/pagamento/email-titular/text()")).
				    split().
				        xpath("/pedido/itens/item").
				    filter().
				        xpath("/item/formato[text()='EBOOK']").
				    setProperty("ebookId", xpath("/item/livro/codigo/text()")).
				    setHeader(Exchange.HTTP_QUERY,
				            simple("clienteId=${property.email}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}")).
				to("http4://localhost:8080/webservices/ebook/item");
			}
			
		});

		context.start();
		Thread.sleep(20000);
		context.stop();
	}	
	
	/**
	 * O multicast() possui uma configuração para chamar cada sub-rota em uma Thread separada. <br>
	 * Assim as sub-rotas serão processadas em paralelo.<br>
	 * Devemos ter cuidado com essa opção pois possíveis problemas (exceções) também ocorrem em paralelo que pode complicar a análise do problema.
	 * @throws Exception
	 */
	private void configurarSubRotasExecutadasEmParalelo() throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("file:pedidos?delay=5s&noop=true").
			    multicast().
			        parallelProcessing().
			            timeout(500). //millis
			                to("direct:soap").
			                to("direct:http");

				from("direct:soap").
				    routeId("rota-soap").
				    log("chamando servico soap ${body}").
				to("mock:soap");
	
				from("direct:http").
				    routeId("rota-http").
				    setProperty("pedidoId", xpath("/pedido/id/text()")).
				    setProperty("email", xpath("/pedido/pagamento/email-titular/text()")).
				    split().
				        xpath("/pedido/itens/item").
				    filter().
				        xpath("/item/formato[text()='EBOOK']").
				    setProperty("ebookId", xpath("/item/livro/codigo/text()")).
				    setHeader(Exchange.HTTP_QUERY,
				            simple("clienteId=${property.email}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}")).
				to("http4://localhost:8080/webservices/ebook/item");
			}
			
		});

		context.start();
		Thread.sleep(20000);
		context.stop();
	}	
	
	/**
	 * Enquanto o direct usa o processamento síncrono, o seda usa assíncrono.<br>
	 * É importante mencionar que seda não implementar qualquer tipo de persistência ou a recuperação, <br>
	 * tudo é processado dentro da JVM. Se você precisa de persistência, confiabilidade ou processamento distribuído,<br> 
	 * JMS é a melhor escolha. 
	 * @throws Exception
	 */
	private void configurarSubRotasComSeda() throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("file:pedidos?delay=5s&noop=true").
			    routeId("rota-pedidos").
			    to("seda:soap").
			    to("seda:http");

				from("seda:soap").
				    routeId("rota-soap").
				    log("chamando servico soap ${body}").
				to("mock:soap");
	
				from("seda:http").
				    routeId("rota-http").
				    setProperty("pedidoId", xpath("/pedido/id/text()")).
				    setProperty("email", xpath("/pedido/pagamento/email-titular/text()")).
				    split().
				        xpath("/pedido/itens/item").
				    filter().
				        xpath("/item/formato[text()='EBOOK']").
				    setProperty("ebookId", xpath("/item/livro/codigo/text()")).
				    setHeader(Exchange.HTTP_QUERY,
				            simple("clienteId=${property.email}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}")).
				to("http4://localhost:8080/webservices/ebook/item");
			}
			
		});

		context.start();
		Thread.sleep(20000);
		context.stop();
	}	

}
