package org.springframework.cloud.stream.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.converter.AbstractFromMessageConverter;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.TestSupportBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MimeType;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(CustomMessageConverterTests.TestSource.class)
public class CustomMessageConverterTests {

	@Autowired @Bindings(TestSource.class)
	private Source testSource;

	@Autowired
	private BinderFactory binderFactory;

	@Autowired
	private List<AbstractFromMessageConverter> customMessageConverters;

	@Test
	public void testCustomMessageConverter() throws Exception {
		assertTrue(customMessageConverters.size() == 2);
		assertThat(customMessageConverters, hasItem(isA(FooToBarConverter.class)));
		assertThat(customMessageConverters, hasItem(isA(BarToFooConverter.class)));
		testSource.output().send(MessageBuilder.withPayload(new Foo("hi")).build());
		@SuppressWarnings("unchecked")
		Message<String> received = (Message<String>) ((TestSupportBinder) binderFactory.getBinder(null))
				.messageCollector().forChannel(testSource.output()).poll(1, TimeUnit.SECONDS);
		Assert.assertThat(received, notNullValue());
		assertThat(received.getHeaders().get(MessageHeaders.CONTENT_TYPE).toString(),
				equalTo("test/bar"));
	}

	@EnableBinding(Source.class)
	@EnableAutoConfiguration
	@PropertySource("classpath:/org/springframework/cloud/stream/config/custom/source-channel-configurers.properties")
	@Configuration
	public static class TestSource {

		@Bean
		public AbstractFromMessageConverter fooConverter() {
			return new FooToBarConverter();
		}

		@Bean
		public AbstractFromMessageConverter barConverter() {
			return new BarToFooConverter();
		}
	}

	public static class FooToBarConverter extends AbstractFromMessageConverter {

		public FooToBarConverter() {
			super(MimeType.valueOf("test/bar"));
		}

		@Override
		protected Class<?>[] supportedTargetTypes() {
			return new Class[] {Bar.class};
		}

		@Override
		protected Class<?>[] supportedPayloadTypes() {
			return new Class<?>[] {Foo.class};
		}

		@Override
		public Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
			Object result = null;
			try {
				if (message.getPayload() instanceof Foo) {
					Foo fooPayload = (Foo) message.getPayload();
					result = new Bar(fooPayload.test);
				}
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
				return null;
			}
			return result;
		}
	}

	public static class BarToFooConverter extends AbstractFromMessageConverter {

		public BarToFooConverter() {
			super(MimeType.valueOf("test/foo"));
		}

		@Override
		protected Class<?>[] supportedTargetTypes() {
			return new Class[] {Foo.class};
		}

		@Override
		protected Class<?>[] supportedPayloadTypes() {
			return new Class<?>[] {Bar.class};
		}

		@Override
		public Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
			Object result = null;
			try {
				if (message.getPayload() instanceof Bar) {
					Bar barPayload = (Bar) message.getPayload();
					result = new Foo(barPayload.testing);
				}
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
				return null;
			}
			return result;
		}
	}

	public static class Foo {

		final String test;

		public Foo(String test) {
			this.test = test;
		}

	}

	public static class Bar {

		final String testing;

		public Bar(String testing) {
			this.testing = testing;
		}
	}
}
