package org.codeforamerica.shiba.configurations;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

  @Value("${client.truststore-password}")
  private String truststorePassword;
  @Value("${client.truststore}")
  private String truststore;
  @Value("${client.connect-timeout}")
  private String connectTimeout;
  @Value("${client.read-timeout}")
  private String readTimeout;
  @Bean
  public RestTemplate restTemplate() 
	        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {

	  // Configure SSL context with trust configs
	    SSLContext sslContext = SSLContextBuilder.create()
	            .loadTrustMaterial(Paths.get(truststore).toFile(), truststorePassword.toCharArray())
	            .build();

		var tlsStrategy = new DefaultClientTlsStrategy(
				  sslContext, 
				  HostnameVerificationPolicy.CLIENT, 
				  NoopHostnameVerifier.INSTANCE);
	    
	    PoolingHttpClientConnectionManagerBuilder connectionManagerbuilder =
				  PoolingHttpClientConnectionManagerBuilder.create();
	    connectionManagerbuilder.setTlsSocketStrategy(tlsStrategy) ;
	    
	    PoolingHttpClientConnectionManager connectionManager = connectionManagerbuilder.build();
	    connectionManager.setDefaultMaxPerRoute(1);
	    connectionManager.setMaxTotal(5);
	    
	    // Build HTTP client with connection manager
	    CloseableHttpClient httpClient = HttpClientBuilder.create()
	            .setConnectionManager(connectionManager)
	            .build();
	    
	    // Create request factory with our custom HTTP client
	    HttpComponentsClientHttpRequestFactory requestFactory = 
	            new HttpComponentsClientHttpRequestFactory(httpClient);
	    
	    requestFactory.setConnectTimeout(Integer.valueOf(connectTimeout));
	    requestFactory.setReadTimeout(Integer.valueOf(readTimeout));
	    
	    return new RestTemplate(requestFactory);
  }
}
