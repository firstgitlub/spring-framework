/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} implementation that allows users to easily configure
 * all the infrastructure beans required to enable annotation-driven transaction
 * demarcation.
 *
 * <p>By default, all proxies are created as JDK proxies. This may cause some
 * problems if you are injecting objects as concrete classes rather than
 * interfaces. To overcome this restriction you can set the
 * '{@code proxy-target-class}' attribute to '{@code true}', which
 * will result in class-based proxies being created.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 2.0
 *
 * 事务的annotation-driven标签的的解析
 *
 * { @link https://wangguoping.blog.csdn.net/article/details/83960279}
 *
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * Parses the {@code <tx:annotation-driven/>} tag. Will
	 * {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary register an AutoProxyCreator}
	 * with the container as necessary.
	 *
	 * <tx:annotation-driven> 一共有四个属性如下：
	 *
	 * mode：指定Spring事务管理框架创建通知 bean 的方式。可用的值有 proxy 和 aspectj。
	 *      前者是默认值，表示通知对象是个JDK代理；后者表示Spring AOP会使用 AspectJ 创建代理
	 * proxy-target-class：如果为 true，Spring将创建子类来代理业务类；如果为 false，则使用基于接口的代理。
	 *     （如果使用子类代理，需要在类路径中添加CGLib.jar类库）
	 * order：如果业务类除事务切面外，还需要织入其他的切面，通过该属性可以控制事务切面在目标连接点的织入顺序。
	 * transaction-manager：指定到现有的 PlatformTransactionManager bean 的引用，通知会使用该引用。
	 *
	 */
	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		registerTransactionalEventListenerFactory(parserContext);

		// 获取mode属性设置
		// 指定Spring事务管理框架创建通知 bean 的方式  默认值是JDK动态代理  需要基于接口
		String mode = element.getAttribute("mode");
		if ("aspectj".equals(mode)) {
			// mode="aspectj"
			registerTransactionAspect(element, parserContext);
			if (ClassUtils.isPresent("javax.transaction.Transactional", getClass().getClassLoader())) {
				registerJtaTransactionAspect(element, parserContext);
			}
		}
		else {
			// mode="proxy"  spring aop 代理用的是动态代理技术，实现方式选择的是CGLIB
			// proxy 是代理模式，仅在外部方法调用才会被代理截获，自身方法调用，即使配置了
			// @Transactional 注解事务也无法生效，此外代理模式也不能应用在非 public 方法上
			AopAutoProxyConfigurer.configureAutoProxyCreator(element, parserContext);
		}
		return null;
	}

	private void registerTransactionAspect(Element element, ParserContext parserContext) {
		String txAspectBeanName = TransactionManagementConfigUtils.TRANSACTION_ASPECT_BEAN_NAME;
		String txAspectClassName = TransactionManagementConfigUtils.TRANSACTION_ASPECT_CLASS_NAME;
		if (!parserContext.getRegistry().containsBeanDefinition(txAspectBeanName)) {
			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(txAspectClassName);
			def.setFactoryMethodName("aspectOf");
			registerTransactionManager(element, def);
			parserContext.registerBeanComponent(new BeanComponentDefinition(def, txAspectBeanName));
		}
	}

	private void registerJtaTransactionAspect(Element element, ParserContext parserContext) {
		String txAspectBeanName = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME;
		String txAspectClassName = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CLASS_NAME;
		if (!parserContext.getRegistry().containsBeanDefinition(txAspectBeanName)) {
			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(txAspectClassName);
			def.setFactoryMethodName("aspectOf");
			registerTransactionManager(element, def);
			parserContext.registerBeanComponent(new BeanComponentDefinition(def, txAspectBeanName));
		}
	}

	private static void registerTransactionManager(Element element, BeanDefinition def) {
		def.getPropertyValues().add("transactionManagerBeanName",
				TxNamespaceHandler.getTransactionManagerName(element));
	}

	private void registerTransactionalEventListenerFactory(ParserContext parserContext) {
		RootBeanDefinition def = new RootBeanDefinition();
		def.setBeanClass(TransactionalEventListenerFactory.class);
		parserContext.registerBeanComponent(new BeanComponentDefinition(def,
				TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME));
	}


	/**
	 * Inner class to just introduce an AOP framework dependency when actually in proxy mode.
	 *
	 * 在代理模式下引入AOP框架依赖的内部类。
	 */
	private static class AopAutoProxyConfigurer {

		public static void configureAutoProxyCreator(Element element, ParserContext parserContext) {

			// 注册代理类（InfrastructureAdvisorAutoProxyCreator类型的bean）
			// 自动代理创建器。事务自定义标签能够调用AOP方法的关键
			AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);

			// TRANSACTION_ADVISOR_BEAN_NAME=org.springframework.transaction.config.internalTransactionAdvisor
			String txAdvisorBeanName = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME;
			if (!parserContext.getRegistry().containsBeanDefinition(txAdvisorBeanName)) {
				Object eleSource = parserContext.extractSource(element);

				// Create the TransactionAttributeSource definition.
				// 先创建了 beanDefinition，后面 applicationContext 会自动实例化
				// =============创建TransactionAttributeSource类型的bean.==============
				// 帮助解析事务注解信息，封装并保存成 TransactionAttribute 等。
				RootBeanDefinition sourceDef = new RootBeanDefinition(
						"org.springframework.transaction.annotation.AnnotationTransactionAttributeSource");
				sourceDef.setSource(eleSource);
				sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// 注册bean，并使用Spring中的定义规则生成beanName，返回beanName
				String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

				// Create the TransactionInterceptor definition.
				// =====================创建TransactionInterceptor类型的bean.=====================
				// 事务拦截器。在AOP的拦截器调用过程中接触过拦截器的概念，
				// Spring会将所有的拦截器封装成 Advisors，最后会调用拦截器的 invoke() 方法进行目标方法增强等
				RootBeanDefinition interceptorDef = new RootBeanDefinition(TransactionInterceptor.class);
				interceptorDef.setSource(eleSource);
				interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				registerTransactionManager(element, interceptorDef);
				interceptorDef.getPropertyValues().add("transactionAttributeSource", new RuntimeBeanReference(sourceName));
				// 注册bean，并使用Spring中的定义规则生成beanName，返回beanName
				String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

				// Create the TransactionAttributeSourceAdvisor definition.
				// ==============创建TransactionAttributeSourceAdvisor类型的bean.================
				// 事务属性增强器。可见其属于增强器 Advisor 类型，其中可以定义切点表达式相关的内容。
				RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryTransactionAttributeSourceAdvisor.class);
				advisorDef.setSource(eleSource);
				advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// 将sourceName的bean注入advisorDef的transactionAttributeSource属性中
				advisorDef.getPropertyValues().add("transactionAttributeSource", new RuntimeBeanReference(sourceName));
				// 将interceptorName的bean注入advisorDef的adviceBeanName属性中
				advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
				// 如果配置了order属性，则加入到bean中
				if (element.hasAttribute("order")) {
					advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
				}

				// 注册TransactionAttributeSourceAdvisor类型的bean
				parserContext.getRegistry().registerBeanDefinition(txAdvisorBeanName, advisorDef);

				/**
				 * 通过上面函数大致可以理清大体脉络，首先获取对类本身进行遍历，如果不能确认是否匹配成功，
				 * 那么就尝试获取类的所有接口，再连通类本身获取他们所有的方法，遍历每一个方法查看是否匹配，
				 * 方法一旦匹配成功便认为这个类适用于当前增强器
				 */
				// 创建CompositeComponentDefinition
				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
				compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, txAdvisorBeanName));
				parserContext.registerComponent(compositeDef);
			}
		}
	}

}
