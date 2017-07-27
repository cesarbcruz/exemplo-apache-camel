package br.com.caelum.camel;

import java.text.SimpleDateFormat;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.thoughtworks.xstream.XStream;

public class RotaHttpPollingNegociacoes {

	public static void main(String[] args) throws Exception {
		RotaHttpPollingNegociacoes desafio = new RotaHttpPollingNegociacoes();
		//desafio.configurarRotaXMLHttp4ParaArquivoXMLGravadoEmDisco();
		//desafio.configurarRotaTransformarXMLEmObjetoJava();
		desafio.configurarRotaObtemXMLWsGravaMysql();
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
		
		final XStream xStream = new XStream();
		xStream.alias("negociacao", Negociacao.class);
		
		CamelContext context = new DefaultCamelContext();
	    context.addRoutes(new RouteBuilder() {
	        
		        public void configure() throws Exception {
		            from("timer://negociacoes?fixedRate=true&delay=1s&period=360s")
		              .to("http4://argentumws.caelum.com.br/negociacoes")
		              .convertBodyTo(String.class)
		              .unmarshal(new XStreamDataFormat(xStream))
		              .split(body())
		              .log("${body}")
		            .end(); //só deixa explícito que é o fim da rota
		        }

	   });
	        
	   context.start();
	        
	   Thread.sleep(4000);
	}
	
	private void configurarRotaObtemXMLWsGravaMysql() throws Exception {
			
			SimpleRegistry registro = new SimpleRegistry();
			MysqlConnectionPoolDataSource ds = criaDataSource();
			registro.put("mysql", ds);
			CamelContext context = new DefaultCamelContext(registro);//construtor recebe registro	
		
			final XStream xStream = new XStream();
			xStream.alias("negociacao", Negociacao.class);
			
		    context.addRoutes(new RouteBuilder() {
		        
			        public void configure() throws Exception {
			        	from("timer://negociacoes?fixedRate=true&delay=1s&period=360s").
			            to("http4://argentumws.caelum.com.br/negociacoes").
			              convertBodyTo(String.class).
			              unmarshal(new XStreamDataFormat(xStream)).
			              split(body()).
			              process(new Processor() {
			                @Override
			                public void process(Exchange exchange) throws Exception {
			                	System.out.println("process");
			                    Negociacao negociacao = exchange.getIn().getBody(Negociacao.class);
			                    exchange.setProperty("preco", negociacao.getPreco());
			                    exchange.setProperty("quantidade", negociacao.getQuantidade());
			                    String data = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(negociacao.getData().getTime());
			                    exchange.setProperty("data", data);
			                }
			              }).
			              setBody(simple("insert into negociacao(preco, quantidade, data) values (${property.preco}, ${property.quantidade}, '${property.data}')")).
			              log("${body}").
			              delay(1000).
			              to("jdbc:mysql");
			        }
	
		   });
		        
		   context.start();
		        
		   Thread.sleep(6000);
		}
	
	private static MysqlConnectionPoolDataSource criaDataSource() {
	    MysqlConnectionPoolDataSource mysqlDs = new MysqlConnectionPoolDataSource();
	    mysqlDs.setDatabaseName("camel");
	    mysqlDs.setServerName("localhost");
	    mysqlDs.setPort(3306);
	    mysqlDs.setUser("root");
	    mysqlDs.setPassword("pa33Lx$k");
	    return mysqlDs;
	}
}