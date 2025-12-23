package org.codeforamerica.shiba.mnit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponents5ClientFactory;
import org.springframework.ws.transport.http.SimpleHttpComponents5MessageSender;

@Configuration
public class EsbWebServiceTemplateConfiguration {

	@Bean
	WebServiceTemplate filenetWebServiceTemplate(WebServiceTemplateBuilder webServiceTemplateBuilder,
			SSLContextBuilder sslContextBuilder, 
			@Value("${mnit-filenet.username}") String username,
			@Value("${mnit-filenet.password}") String password,
			@Value("${mnit-filenet.jaxb-context-path}") String jaxbContextPath,
			@Value("${mnit-filenet.upload-url}") String uploadUrl,
			@Value("${mnit-filenet.timeout-seconds}") long timeoutSeconds)
			throws KeyManagementException, NoSuchAlgorithmException, IOException, URISyntaxException {

		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setContextPath(jaxbContextPath);
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
		int timeoutMillis = (int) TimeUnit.MILLISECONDS.convert(timeoutSeconds, TimeUnit.SECONDS);
		Timeout timeout = Timeout.ofMilliseconds(timeoutMillis);
		
		var tlsStrategy = new DefaultClientTlsStrategy(
				  sslContextBuilder.build(), 
				  HostnameVerificationPolicy.CLIENT, 
				  NoopHostnameVerifier.INSTANCE);

		PoolingHttpClientConnectionManager poolingConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(tlsStrategy)
				.setDefaultSocketConfig(SocketConfig.custom()
						.setSoTimeout(timeout)
						.build())
				.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
				.setConnPoolPolicy(PoolReusePolicy.LIFO)
				.setDefaultConnectionConfig(ConnectionConfig.custom()
						.setSocketTimeout(timeout)
						.setConnectTimeout(timeout)
						.setTimeToLive(TimeValue.ofMinutes(10))
						.build())
				.build();

		Collection<String> targetPreferredAuthSchemes = Arrays.asList(StandardAuthScheme.BASIC);
		
		HttpRequestInterceptor interceptor = new HttpComponents5ClientFactory.RemoveSoapHeadersInterceptor();

		CloseableHttpClient client = HttpClients.custom()
				.addRequestInterceptorFirst(interceptor)
				.setConnectionManager(poolingConnectionManager)
				.setDefaultHeaders(
						List.of(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth))))
				.setDefaultRequestConfig(RequestConfig.custom()
						.setAuthenticationEnabled(true)
						.setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
						.build())
				.build();

	    SimpleHttpComponents5MessageSender webServiceMessageSender = new SimpleHttpComponents5MessageSender(client);
	    
		return webServiceTemplateBuilder
				.setDefaultUri(uploadUrl)
				.setMarshaller(jaxb2Marshaller)
				.setUnmarshaller(jaxb2Marshaller)
				.messageSenders(webServiceMessageSender)
				.build();
	}

}
