package org.springframework.amqp.remoting;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;

import com.rabbitmq.client.Channel;

/**
 * @author stephansun
 *
 */
public class AmqpInvokerServiceExporter extends RemoteInvocationBasedExporter
		implements ChannelAwareMessageListener, InitializingBean {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private MessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();

	private String encoding = DEFAULT_ENCODING;

	private boolean ignoreInvalidRequests = true;

	private Object proxy;

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter
				: new SimpleMessageConverter());
	}

	public void setIgnoreInvalidRequests(boolean ignoreInvalidRequests) {
		this.ignoreInvalidRequests = ignoreInvalidRequests;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.proxy = getProxyForService();
	}

	@Override
	public void onMessage(Message requestMessage, Channel channel)
			throws Exception {
		System.out.println("onMessage");
		RemoteInvocation invocation = readRemoteInvocation(requestMessage);
		if (invocation != null) {
			RemoteInvocationResult result = invokeAndCreateResult(invocation,
					this.proxy);
			// response result
			System.out.println("result:[" + result.getValue() + "]");

			writeRemoteInvocationResult(requestMessage, channel, result);
		}
	}

	protected RemoteInvocation readRemoteInvocation(Message requestMessage) {
		Object content = this.messageConverter.fromMessage(requestMessage);
		if (content instanceof RemoteInvocation) {
			return (RemoteInvocation) content;
		}
		return onInvalidRequest(requestMessage);
	}

	protected RemoteInvocation onInvalidRequest(Message requestMessage) {
		if (this.ignoreInvalidRequests) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid request message will be discarded: "
						+ requestMessage);
			}
			return null;
		} else {
			// FIXME need to transformat exception.
			throw new RuntimeException("Invalid request message: "
					+ requestMessage);
		}
	}

	protected RemoteInvocationResult invokeAndCreateResult(
			RemoteInvocation invocation, Object targetObject) {
		try {
			Object value = invoke(invocation, targetObject);
			return new RemoteInvocationResult(value);
		} catch (Throwable ex) {
			return new RemoteInvocationResult(ex);
		}
	}

	protected void writeRemoteInvocationResult(Message requestMessage,
			Channel channel, RemoteInvocationResult result) throws IOException {
		Message response = createResponseMessage(result);
		String replyTo = requestMessage.getMessageProperties().getReplyTo();
		System.out.println("replyTo:[" + replyTo + "]");
		channel.basicPublish(
				"",
				replyTo,
				false,
				false,
				this.messagePropertiesConverter.fromMessageProperties(
						response.getMessageProperties(), encoding),
				response.getBody());
	}

	protected Message createResponseMessage(RemoteInvocationResult result) {
		Message response = this.messageConverter.toMessage(result, null);
		return response;
	}

}
