package org.springframework.amqp.remoting;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * @author stephansun
 *
 */
public class AmqpInvokerClientInterceptor implements MethodInterceptor, InitializingBean {

	private AmqpTemplate amqpTemplate;
	
	private Queue queue;
	
	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();
	
	private MessageConverter messageConverter = new SimpleMessageConverter();
	
	protected AmqpTemplate getAmqpTemplate() {
		return amqpTemplate;
	}

	public void setAmqpTemplate(AmqpTemplate amqpTemplate) {
		this.amqpTemplate = amqpTemplate;
	}
	
	public void setQueue(Queue queue) {
		this.queue = queue;
	}
	
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}
	
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return this.remoteInvocationFactory.createRemoteInvocation(methodInvocation);
	}
	
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	@Override
	public void afterPropertiesSet() {
		if (getAmqpTemplate() == null) {
			// FIXME
			throw new IllegalArgumentException("Property 'amqpTemplate' is required");
		}
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JMS invoker proxy for queue";
		}
		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result = executeRequest(invocation);
		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in JMS invoker remote service at queue", ex);
			}
		}
	}
	
	// core invoke
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) {
		Message requestMessage = createRequestMessage(invocation);
		Message responseMessage = doExecuteRequest(requestMessage);
		return extractInvocationResult(responseMessage);
	}
	
	protected Message createRequestMessage(RemoteInvocation invocation) {
		return this.messageConverter.toMessage(invocation, null);
	}
	
	protected Message doExecuteRequest(Message requestMessage) {
		// FIXME
		Message responseMessage = amqpTemplate.sendAndReceive("", queue.getName(), requestMessage);
		return responseMessage;
	}
	
	protected RemoteInvocationResult extractInvocationResult(Message responseMessage) {
		Object content = this.messageConverter.fromMessage(responseMessage);
		if (content instanceof RemoteInvocationResult) {
			return (RemoteInvocationResult) content;
		}
		return onInvalidResponse(responseMessage);
	}
	
	protected RemoteInvocationResult onInvalidResponse(Message responseMessage) {
		// FIXME need to transformat exception.
		throw new RuntimeException("Invalid response message: " + responseMessage);
	}
	
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}
	
}
