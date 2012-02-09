# spring-amqp-remoting

The project is a spring-amqp extension.
1. using AmqpTemplate implements Request/Reply pattern
2. Amqp-based remote service.

## Installation

Clone from GIT and then use Maven(2.2.*):

	$ git clone ...
	$ mvn install

## Usage

```java
@Configuration
public class TestServerConfig {

	@Autowired
	private ConnectionFactory connectionFactory;
	
	@Autowired
	private Queue myQueue;
	
	@Bean
	public TestService testServiceImpl() {
		TestService bean = new TestServiceImpl();
		return bean;
	}
	
	@Bean
	public AmqpInvokerServiceExporter testService() {
		AmqpInvokerServiceExporter bean = new AmqpInvokerServiceExporter();
		bean.setServiceInterface(TestService.class);
		bean.setService(testServiceImpl());
		return bean;
	}
	
	@Bean
	public SimpleMessageListenerContainer testContainer() {
		SimpleMessageListenerContainer bean = new SimpleMessageListenerContainer();
		bean.setConnectionFactory(connectionFactory);
		bean.setMessageListener(testService());
		bean.setQueues(myQueue);
		return bean;
	}
	
}

```java
public class TestClientConfig {
	
	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private Queue queue;
	
	@Bean
	public AmqpInvokerProxyFactoryBean testService() {
		AmqpInvokerProxyFactoryBean bean = new AmqpInvokerProxyFactoryBean();
		bean.setAmqpTemplate(amqpTemplate);
		bean.setServiceInterface(TestService.class);
		bean.setQueue(queue);
		return bean;
	}

}

