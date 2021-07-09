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

package org.springframework.web.servlet;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Simple extension of {@link javax.servlet.http.HttpServlet} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code servlet} tag in {@code web.xml}) as bean properties.
 *
 * <p>A handy superclass for any type of servlet. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 *
 * <p>This servlet leaves request handling to subclasses, inheriting the default
 * behavior of HttpServlet ({@code doGet}, {@code doPost}, etc).
 *
 * <p>This generic servlet base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept. Simple
 * servlets usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * <p>The {@link FrameworkServlet} class is a more specific servlet base
 * class which loads its own application context. FrameworkServlet serves
 * as direct base class of Spring's full-fledged {@link DispatcherServlet}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConfigurableEnvironment environment;

	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * Subclasses can invoke this method to specify that this property
	 * (which must match a JavaBean property they expose) is mandatory,
	 * and must be supplied as a config parameter. This should be called
	 * from the constructor of a subclass.
	 * <p>This method is only relevant in case of traditional initialization
	 * driven by a ServletConfig instance.
	 * @param property name of the required property
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * Set the {@code Environment} that this servlet runs in.
	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
	 * provided by default.
	 * @throws IllegalArgumentException if environment is not assignable to
	 * {@code ConfigurableEnvironment}
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * Return the {@link Environment} associated with this servlet.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 * <p>Subclasses may override this in order to configure the environment or
	 * specialize the environment type returned.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Map config parameters onto bean properties of this servlet, and
	 * invoke subclass initialization.
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 *
	 *
	 * Servlet 是一个 Java 编写的程序，此程序是基于 HTTP 协议的，
	 * 在服务器端运行的 (如Tomcat)，是按照 Servlet 规范编写的一个 Java 类。
	 * 主要是处理客户端的请求并将其结果发送到客户端。Servlet 的生命周期是由 Servlet 的容器来控制的，
	 * 它可以分为3个阶段：初始化、运行和销毁。
	 *
	 * 初始化：Servlet 容器加载 Servlet 类，把 Servlet 类的 .class 文件中的数据读到内存中。
	 * Servlet 容器创建一个 ServletConfig 对象。ServletConfig 对象包含了 Servlet 的初始化配置信息。
	 * Servlet 容器创建一个 Servlet 对象。Servlet 容器调用 Servlet 对象的 init() 方法进行初始化。
	 *
	 * 运行：当 Servlet 容器接收到一个请求时，
	 * Servlet 容器会针对这个请求创建 servletRequest 和 servletResponse对象，
	 * 然后调用 service() 方法。并把这两个参数传递给 service() 方法。
	 * service() 方法通过 servletRequest 对象获得请求的信息，并处理该请求。
	 * 再通过 servletResponse 对象生成这个请求的响应结果。
	 * 最后销毁 servletRequest 和 servletResponse 对象。
	 * 我们不管这个请求是 post 提交的还是 get 提交的，最终这个请求都会由 service() 方法来处理。
	 *
	 * 销毁阶段：当Web应用被终止时，Servlet 容器会先调用 Servlet 对象的 destrory() 方法，
	 * 然后再销毁 servlet 对象，同时也会销毁与 servlet 对象相关联的 servletConfig 对象。
	 * 我们可以在 destrory() 方法的实现中，
	 * 释放 servlet 所占用的资源，如关闭数据库连接，关闭文件输入输出流等。
	 *
	 */
	/**
	 * DipatcherServlet 的初始化过程主要是通过将当前的 servlet 类型实例转换为 BeanWrapper 类型实例，
	 * 以便使用Spring中提供的注入功能进行对应属性的注入。
	 *
	 * 这些属性如 contextAttribute、contextClass、nameSpace、contextConfigLocation 等，
	 * 都可以在 web.xml 文件中以初始化参数的方式配置在 servlet 的声明中，Spring会保证这些参数被注入到对应的值中
	 *
	 */
	@Override
	public final void init() throws ServletException {

		// Set bean properties from init parameters.
		// 解析web.xml中的 init-param 并封装至 PropertyValues 中，其中就包含了 SpringMVC 的配置文件路径
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				// BeanWrapper 是bean 实例化之前 将 beanDefinition 转换成的 对象
				// 将DispatchServlet类实例(this)转化为一个BeanWrapper,
				// 从而能够以Spring的方式来对init-param的值进行注入
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());

				// 注册自定义属性编辑器，一旦遇到 Resource 类型的属性将会使用 ResourceEditor 进行解析
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));

				// 空实现，留给子类覆盖
				initBeanWrapper(bw);

				// 属性注入
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
		// 留给子类扩展，父类FrameworkServlet重写了
		initServletBean();
	}

	/**
	 * Initialize the BeanWrapper for this HttpServletBean,
	 * possibly with custom editors.
	 * <p>This default implementation is empty.
	 * @param bw the BeanWrapper to initialize
	 * @throws BeansException if thrown by BeanWrapper methods
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * Subclasses may override this to perform custom initialization.
	 * All bean properties of this servlet will have been set before this
	 * method is invoked.
	 * <p>This default implementation is empty.
	 * @throws ServletException if subclass initialization fails
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * Overridden method that simply returns {@code null} when no
	 * ServletConfig set yet.
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * PropertyValues implementation created from ServletConfig init parameters.
	 *
	 * ServletConfigPropertyValues 除了封装属性外还有对属性验证的功能
	 *
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * Create new ServletConfigPropertyValues.
		 * @param config the ServletConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 * we can't accept default values
		 * @throws ServletException if any required properties are missing
		 *
		 * 解析web.xml中的init-param并封装至PropertyValues中，
		 * 其中就包含了SpringMVC的配置文件路径
		 *
		 * 对web.xml中的初始化参数进行封装
		 *
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			/**
			 * 从代码中得知，封装属性主要是对初始化的参数进行封装，
			 * 也就是 servlet 中配置的 <init-param> 中配置的封装。
			 * 这些初始化参数被 servlet 容器已经封装在了 ServletConfig 实例中
			 */

			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			// 获取初始化参数名称
			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				// 通过从 ServletConfig 实例中获取出属性值并将其重新封装成 PropertyValue。
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			// 用户可以通过对requiredProperties参数的初始化来强制验证某些属性的必要性，这样，
			// 在属性封装的过程中，一旦检测到requiredProperties中的属性没有指定初始值，就会抛出异常。
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
